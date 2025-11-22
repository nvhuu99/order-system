import http from "k6/http"
import { check, sleep, fail } from "k6"
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.1/index.js"

import { inventoryUtil } from "../utils/inventory.js"
import { userCartsUtil } from "../utils/user-carts.js"
import { randomInt } from "../utils/common.js"


const {
  TOTAL_USERS,
  SEED_PRODUCTS_TOTAL,
  MAX_PRODUCT_STOCK,
  CART_ITEM_MAX_QTY,
  CART_MAX_ITEMS,
  SUMMARY_WAIT_FOR_SYNC_SECONDS,
  INVENTORY_API_ADDR,
  SHOP_API_ADDR,  
  DURATION,
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
    summaryWaitForSyncSeconds: SUMMARY_WAIT_FOR_SYNC_SECONDS,
    inventoryAddr: INVENTORY_API_ADDR,
    shopAddr: SHOP_API_ADDR,
    verbose: VERBOSE,
  }

  inventoryUtil.init(configs)
  inventoryUtil.seedProducts()

  return { configs } 
}


export const options = {
  stages: [
    { duration: '5m', target: TOTAL_USERS },
    { duration: DURATION, target: TOTAL_USERS },
    { duration: '5m', target: 0 },
  ],
};

export default function(setupData) {
  var configs = setupData.configs
  inventoryUtil.init(configs)
  userCartsUtil.init(Object.assign(configs, { inventoryUtil }))
  userCartsUtil.simulateUsersUpdateShoppingCarts()
  sleep(randomInt(1, 5));
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
