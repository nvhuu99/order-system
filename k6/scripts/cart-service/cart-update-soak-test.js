import http from "k6/http"
import { check, sleep } from "k6"

const requestRate = __ENV.REQUESTS_PER_SECOND ?? 100
const errorRate = __ENV.ERROR_RATE ?? 0.2

export const options = {
  scenarios: {
    cart_test: {
      executor: "constant-arrival-rate",
      rate: requestRate,
      timeUnit: "1s",
      duration: __ENV.DURATION ?? "10m",
      preAllocatedVUs: requestRate,
      maxVUs: requestRate * 2,
    },
  },
}

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
  if (Math.random() < errorRate) {
      cartVer = -1; // simulate error
  }
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

  var url = `http://${__ENV.CART_SVC_HOST}:${__ENV.CART_SVC_API_PORT}/api/v1/carts/${userId}`
  var res = http.put(url, body, { headers: { "Content-Type": "application/json" } })
  var success = check(res, {
    "status is 200": (r) => r.status === 200,
  })

  if (success) {
    cartVersions[userId] = cartVer
  }
}

function randomInt(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min }