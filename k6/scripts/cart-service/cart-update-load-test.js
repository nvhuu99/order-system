import http from "k6/http"
import { check, sleep } from "k6"
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js'

const cartServiceHosts = (__ENV.CART_SVC_INSTANCES ?? "cart-service").split(",")
const totalUsers = __ENV.TOTAL_VUS ?? 10
const totalRequestPerUser = __ENV.TOTAL_REQUESTS_PER_VU ?? 10
const maxDuration = __ENV.MAX_DURATION ?? '60s'
const hostname = __ENV.HOSTNAME ?? "cart-update-load-test"
const cartVersions = {}
const actions = ["QTY_CHANGE", "DROP_ITEM"]
const products = [
  { id: "P001", name: "Laptop" },
  { id: "P002", name: "Smartphone" },
  { id: "P003", name: "Headphones" },
  { id: "P004", name: "Keyboard" },
  { id: "P005", name: "Mouse" },
  { id: "P006", name: "Monitor" },
  { id: "P007", name: "USB-C Cable" },
  { id: "P008", name: "External HDD" },
  { id: "P009", name: "Printer" },
  { id: "P010", name: "Tablet" },
]

export const options = {
  scenarios: {
    send_cart_requests: {
      executor: "per-vu-iterations",
      vus: totalUsers,
      iterations: totalRequestPerUser,
      maxDuration: maxDuration,
    },
  },
};

export default function () {

  var userId = `VU_${__VU}_${hostname}`
  var cartVer = (cartVersions[userId] ?? 0) + 1;
  var product = products[randomInt(0, products.length - 1)]
  var action = actions[randomInt(0, actions.length - 1)]
  var qty = action === "QTY_CHANGE" ? randomInt(1, 5) : 0
  var body = JSON.stringify({
    userId: userId,
    versionNumber: cartVer,
    entries: [
      {
        productId: product.id,
        productName: product.name,
        qtyAdjustment: qty,
        action: action,
      },
    ],
  })

  var url = `http://${__ENV.SHOP_SVC_HOST}:${__ENV.SHOP_SVC_API_PORT}/api/v1/carts/${userId}`
  var res = http.put(url, body, { headers: { "Content-Type": "application/json" } })
  var success = check(res, {
    "status is 200": (r) => r.status === 200,
  })

  if (success) {
    cartVersions[userId] = cartVer
  }
}

export function handleSummary(data) {

  var observedCartVersions = {}
  var totalRequestsHandledPerHost = {}
  var failedCarts = {}

  var waited = 0
  var maxRetries = 30
  var isSucceeded = false
  var successTotals = 0
  
  while (maxRetries--) {

    for (var i = 0; i < cartServiceHosts.length; ++i) {
      var host = cartServiceHosts[i].trim()
      var url = `http://${host}:${__ENV.CART_SVC_API_PORT}/api/v1/carts/cart-update-request-handler/summary`
      var res = http.get(url, { headers: { "Content-Type": "application/json" } })
      var summary = res.json()

      if (res.status != 200) {
        continue
      }

      for (const userId in summary["observed_cart_versions"]) {
        if (userId.endsWith(hostname)) {
          observedCartVersions[userId] = Math.max(summary["observed_cart_versions"][userId], (observedCartVersions[userId] ?? 0)) 
        }
      }

      
      totalRequestsHandledPerHost[host] = 0
      for (const userId in summary["requests_handled_total"]) {
        if (userId.endsWith(hostname)) {
          totalRequestsHandledPerHost[host] = summary["requests_handled_total"][userId] + totalRequestsHandledPerHost[host]
        }
      }
    }

    for (const userId in observedCartVersions) {
      if (observedCartVersions[userId] == totalRequestPerUser) {
        successTotals++
      } else {
        failedCarts[userId] = observedCartVersions[userId]
      }
    }

    if (successTotals == totalUsers) {
      isSucceeded = true
      break
    }

    successTotals = 0

    sleep(1)
    waited += 1
  }
  
  var customContent = ""
  customContent += "█ Requests Summary:\n\n"
  customContent += `    Total success carts......:  ${ successTotals }\n`
  customContent += `    Total failed carts.......:  ${ totalUsers - successTotals }\n`
  customContent += `    Result...................:  ${ isSucceeded ? "Success" : "Failed" }\n`
  customContent += "\n█ Estimated duration:\n\n"
  customContent += `${
      isSucceeded 
        ? "    Workload handled completely after ~" + Math.round(waited + (data.state.testRunDurationMs/1000)) + " seconds\n"
        : "    Workload aborted after ~" + Math.round(waited + (data.state.testRunDurationMs/1000)) + " seconds\n"
  }`
  customContent += "\n█ Requests handled by host:\n\n"
  for (const host in totalRequestsHandledPerHost) {
    customContent += "    " + host.padEnd(30, ".") + ":  " + totalRequestsHandledPerHost[host] + "\n"
  }
  customContent += "\n█ Carts failed:\n\n"
  if (! isSucceeded) {
    for (const userId in failedCarts) {
      customContent += "    " + userId.padEnd(30, ".") + ":  " + failedCarts[userId] + "\n"
    }
  } else {
    customContent += "    None\n"
  }

  return {
    stdout: textSummary(data) + "\n\n" + customContent + "\n",
  }
}

function randomInt(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min }

