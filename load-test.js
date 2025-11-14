import http from "k6/http"
import { check, sleep, fail } from "k6"
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.1/index.js"

import { inventoryUtil } from "./utils/inventory.js"
import { userCartsUtil } from "./utils/user-carts.js"
import { randomInt } from "./utils/test-common.js"


const TOTAL_USERS = 3
const TOTAL_REQUESTS_PER_USER = 3
const MAX_DURATION = '300s'
const PRUNE = true
const TEST_ID = 1
const SEED_PRODUCTS_TOTAL = 10
const SUMMARY_RESERVATIONS_SYNC_WAIT_SECONDS = 10


export function setup() {
  
  var configs = {
    testId: TEST_ID ?? `${Date.now()}_${Math.floor(Math.random() * 100000)}`,
    verbose: false,
    inventoryAddr: "http://inventory-service:8082",
    shopAddr: "http://shop-service:8080",
    productsTotal: 10,
    cartMaxItems: 2,
    cartItemMaxQty: 10,
    seedProductsTotal: SEED_PRODUCTS_TOTAL,
  }

  inventoryUtil.init(configs)
  inventoryUtil.seedProducts(configs.seedProductsTotal)

  return { configs } 
}

export function teardown(setupData) {
  if (PRUNE) {
    inventoryUtil.init(setupData["configs"])
    inventoryUtil.deleteProducts()
  }
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
  userCartsUtil.simulateUserUpdateShoppingCart(`VU_${__VU}_${configs.testId}`)
  sleep(randomInt(0, 2));
}


export function handleSummary(data) {

  var configs = data['setup_data']['configs']
  inventoryUtil.init(configs)
  userCartsUtil.init(Object.assign(configs, { inventoryUtil }))

  var cartValidations = tryValidateAllUserCarts(configs)
  var availabilitiesValidations = tryValidateAllProductAvailabilities(configs)

  console.log(JSON.stringify(cartValidations))
  console.log(JSON.stringify(availabilitiesValidations))
}


function tryValidateAllUserCarts(configs) {
  var ids = [];
  for (var i = 1; i <= TOTAL_USERS; ++i) {
    ids.push(`VU_${i}_${configs.testId}`)
  }
  var validations = {}
  var wait = SUMMARY_RESERVATIONS_SYNC_WAIT_SECONDS
  for (var i = 0; i < ids.length; ++i) {
    while (true) {
      validations[ids[i]] = userCartsUtil.validateUserCart(ids[i])
      if (wait-- && validations[ids[i]] != null) {
        sleep(1)
        continue
      }
      break
    }
  }
  return validations
}

function tryValidateAllProductAvailabilities(configs) {
  var limit = 100
  var totalPages = Math.ceil(SEED_PRODUCTS_TOTAL / limit)
  var page = 1
  var validations = {}
  while (page++ <= totalPages) {
    var products = inventoryUtil.listProducts(page, limit)
    var ids = products.map(p => p['id'])
    var wait = SUMMARY_RESERVATIONS_SYNC_WAIT_SECONDS
    for (var i = 0; i < ids.length; ++i) {
      while (true) {
        validations[ids[i]] = inventoryUtil.validateProductAvailability(ids[i])
        if (wait-- && validations[ids[i]] != null) {
          sleep(1)
          continue
        }
        break
      }
    }
  }
  
  return validations
}
