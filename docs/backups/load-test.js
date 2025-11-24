import http from "k6/http"
import { check, sleep, fail } from "k6"
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.1/index.js"

import { inventoryUtil } from "./utils/inventory.js"
import { userCartsUtil } from "./utils/user-carts.js"
import { randomInt } from "./utils/common.js"


const {
  TOTAL_USERS,
  TOTAL_REQUESTS_PER_USER,
  SEED_PRODUCTS_TOTAL,
  MAX_PRODUCT_STOCK,
  CART_ITEM_MAX_QTY,
  CART_MAX_ITEMS,
  INVENTORY_API_ADDR,
  SHOP_API_ADDR,  
  MAX_DURATION,
  PRUNE,
  VERBOSE,
} = __ENV

export function setup() {
  var configs = {
    testId: `${Date.now()}_${Math.floor(Math.random() * 100000)}`,
    totalUsers: TOTAL_USERS,
    productsTotal: SEED_PRODUCTS_TOTAL,
    maxProductStock: MAX_PRODUCT_STOCK,
    cartItemMaxQty: CART_ITEM_MAX_QTY,
    cartMaxItems: CART_MAX_ITEMS,
    inventoryAddr: INVENTORY_API_ADDR,
    shopAddr: SHOP_API_ADDR,
    verbose: VERBOSE,
  }

  inventoryUtil.init(configs)
  inventoryUtil.seedProducts()

  return { configs } 
}


export const options = {
  scenarios: {
    main: {
      executor: "per-vu-iterations",
      vus: TOTAL_USERS,
      iterations: TOTAL_REQUESTS_PER_USER,
      maxDuration: MAX_DURATION,
    },
  },
};

export default function(setupData) {
  var configs = setupData.configs
  inventoryUtil.init(configs)
  userCartsUtil.init(Object.assign(configs, { inventoryUtil }))
  userCartsUtil.simulateUsersUpdateShoppingCarts()
  sleep(randomInt(0, 2));
}


export function handleSummary(data) {

  var configs = data['setup_data']['configs']
  inventoryUtil.init(configs)
  userCartsUtil.init(Object.assign(configs, { inventoryUtil }))

  var cartValidations = userCartsUtil.tryValidateAllUserCarts()
  var availabilitiesValidations = inventoryUtil.tryValidateAllProductAvailabilities()

  console.log(JSON.stringify(cartValidations))
  console.log(JSON.stringify(availabilitiesValidations))

  if (PRUNE == "true") {
    inventoryUtil.init(setupData["configs"])
    inventoryUtil.deleteProducts()
  }
}
