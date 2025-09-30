import http from "k6/http"
import { check, textSummary } from "k6"

export const options = {
  scenarios: {
    ramp_phase_1: {
      executor: "constant-arrival-rate",
      rate: 5,
      timeUnit: "1s",
      duration: "15s",
      preAllocatedVUs: 10,
      maxVUs: 20,
      startTime: "0s",
    },
    ramp_phase_2: {
      executor: "constant-arrival-rate",
      rate: 10,
      timeUnit: "1s",
      duration: "5m",
      preAllocatedVUs: 50,
      maxVUs: 100,
      startTime: "15s",
    },
  },
};

const timestamp = Date.now()
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

export default function () {

  var userId = `VU_${__VU}_${timestamp}`
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
  var res = http.put(url, body, {
    headers: { "Content-Type": "application/json" },
  })
  var success = check(res, {
    "status is 200": (r) => r.status === 200,
  })

  if (success) {
    cartVersions[userId] = cartVer
  }
}

export function handleSummary(data) {

  var successTotals = 0;
  for (const userId in cartVersions) {
      var url = `http://${__ENV.SHOP_SVC_HOST}:${__ENV.SHOP_SVC_API_PORT}/api/v1/carts/${userId}`
      var res = http.get(url, { headers: { "Content-Type": "application/json" } })
      var cartData = res.json()
      if (res.status == 200 && cartVersions[userId] == cartData.versionNumber) {
        successTotals++
      }
  }

  var failed = Object.keys(cartVersions).length - successTotals
  var defaultSummary = textSummary(data, { indent: " ", enableColors: true })
  const customContent = `

  █ Cart Service Result
  
    Total success carts......:  ${successTotals}
    Total failed carts.......:  ${Object.keys(cartVersions).length - successTotals}
    Result...................:  ${ failed == 0 ? "Success" : "Failed" }

  `

  return {
    stdout: defaultSummary + customContent,
    exitCode: failed ? 1 : 0
  }
}

function randomInt(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min }
