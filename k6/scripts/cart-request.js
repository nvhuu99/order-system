import http from "k6/http"
import { check } from "k6"

export const options = {
  scenarios: {
    cart_test: {
      executor: "constant-arrival-rate",
      rate: 100,
      timeUnit: "1s",
      duration: "1h",
      preAllocatedVUs: 10,
      maxVUs: 50,
    },
  },
  thresholds: {
    http_req_failed: [
      { threshold: "rate<0.1", abortOnFail: true },
    ],
  },
}

const cartVersions = {}
const cartLocks = {}
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
  const userId = `usr${randomInt(1, 1000)}`
  if (cartLocks[userId]) {
    return
  } else {
    cartLocks[userId] = 1
  }

  const cartVer = (cartVersions[userId] ?? 0) + 1;
  const product = products[randomInt(0, products.length - 1)]
  const action = actions[randomInt(0, actions.length - 1)]
  const qty = action === "QTY_CHANGE" ? randomInt(1, 5) : 0
  const body = JSON.stringify({
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

  const url = `http://${__ENV.CART_SVC_HOST}:${__ENV.CART_SVC_API_PORT}/api/v1/carts/${userId}`
  const res = http.put(url, body, {
    headers: { "Content-Type": "application/json" },
  })
  const success = check(res, {
    "status is 200": (r) => r.status === 200,
  })

  if (success) {
    cartVersions[userId] = cartVer
  }
  cartLocks[userId] = 0
}

function randomInt(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min }
