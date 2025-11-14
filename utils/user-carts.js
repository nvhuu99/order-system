import { fail, sleep } from 'k6'
import http from "k6/http"

import { randomInt, parseJsonReponse } from './test-common.js'


const CONTENT_TYPE_HEADER = { headers: { "Content-Type": "application/json" } }

export const userCartsUtil = {

  /* Required properties */
  testId: null,
  inventoryUtil: null,
  inventoryAddr: "",
  shopAddr: "",

  /* Optional properties */
  totalUsers: 10,
  cartMaxItems: 10,
  cartItemMaxQty: 10,
  summaryWaitForSyncSeconds: 10,
  verbose: true,

  init(properties) {
    Object.assign(this, properties)
  },

  logTemplate(message) {
    return `vu: ${__VU} - ${message}`
  },

  verboseLog(message) {
    if (this.verbose == 'true') {
      console.log(this.logTemplate(message))
    } 
  },


  simulateUsersUpdateShoppingCarts() {
    var userId = `VU_${__VU}_${this.testId}`
    var cart = this.loadUserCart(userId)
    var products = this.inventoryUtil.listRandomProducts(randomInt(1, this.cartMaxItems))
    var productIds = products.map(p => p['id'])
    var reservations = []

    for (var cartProductId in cart['items']) {
      if (! productIds.includes(cartProductId)) {
        reservations.push({
          'productId': cartProductId,
          'quantity': 0,
        })
      }
    }

    if (reservations.length > 0) {
      this.verboseLog(`remove products from cart - total: ` + reservations.length)
    }

    for (var i = 0; i < productIds.length; ++i) {
      reservations.push({
        'productId': productIds[i],
        'quantity': randomInt(1, this.cartItemMaxQty),
      })
    }

    this.verboseLog(`put cart products - total: ` + productIds.length)

    for (var i = 0; i < reservations.length; ++i) {
      var body = { 'userId': userId, 'entries': reservations }
      var response = http.put(`${this.shopAddr}/api/v1/carts/${userId}`, JSON.stringify(body), CONTENT_TYPE_HEADER)
      var responseBody = parseJsonReponse(response)
      if (response.status != 200) {
        fail(this.logTemplate('fail to put cart products: ' + JSON.stringify(responseBody)))
      }
      this.verboseLog('put cart products successfully')
    }
  },

  loadUserCart(userId) {
    var response = http.get(`${this.shopAddr}/api/v1/carts/${userId}`)
    var responseBody = parseJsonReponse(response)
    if (response.status != 200) {
      fail(this.logTemplate('fail to load user cart: ' + JSON.stringify(responseBody)))
    }
    var cart = responseBody['data']
    this.verboseLog(`load user cart successfully - ${JSON.stringify(cart)}`)
    return cart
  },

  tryValidateAllUserCarts() {
    var ids = [];
    for (var i = 1; i <= this.totalUsers; ++i) {
      ids.push(`VU_${i}_${this.testId}`)
    }
    var validations = {}
    var wait = this.summaryWaitForSyncSeconds
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
  },

  /**
   * Check user cart items returned from `ShopApi` match the `product_reservations` records
   * 
   * @param {*} id 
   * @returns when all items are valid, return `null`. Otherwise, return the validations
   */
  validateUserCart(id) {
    var reservations = this.inventoryUtil.listReservationsByUserId(id)
    var cart = userCartsUtil.loadUserCart(id)
    var validations = []

    for (var prodId in cart['items']) {

      var resv = reservations[prodId]
      if (! resv) {
        validations.push(`cart.items does not contain reservation of product_id ${prodId}`)
        continue
      }
      
      var message = ""
      var now = new Date()
      var expiresAt = new Date(resv['expiresAt'])
      var { desiredAmount, reservedAmount, reservationStatus } = cart['items'][prodId]

      if (reservedAmount != resv['reservedAmount']) {
        message = `cart.items[*].reservedAmount (${reservedAmount}) and reservations[*].reservedAmount (${resv['reservedAmount']}) value mismatch`
      } else if (desiredAmount != resv['desiredAmount']) {
        message = `cart.items[*].desiredAmount (${desiredAmount}) and reservations[*].desiredAmount (${resv['desiredAmount']}) value mismatch`
      } else if (reservationStatus != resv['status']) {
        message = `cart.items[*].reservationStatus (${reservationStatus}) and reservations[*].reservationStatus (${resv['status']}) value mismatch`
      } else if (expiresAt > now && reservationStatus == 'EXPIRED') {
        message = "wrong status. Reservation has not yet expired but it's status is \"EXPIRED\""
      } else if (desiredAmount == 0) {
        message = "zero amount reservation is not removed"
      } else if (reservedAmount == desiredAmount && reservationStatus != 'OK') {
        message = "wrong status. Expected \"OK\""
      } else if (reservedAmount < desiredAmount && reservationStatus != 'INSUFFICIENT_STOCK') {
        message = "wrong status. Expected \"INSUFFICIENT_STOCK\""
      } else if (reservedAmount > desiredAmount) {
        message = `invalid amount. reserved_amount (${reservedAmount}) is currently greater than desired_amount (${desiredAmount})`
      }

      if (message != "") {
        validations.push(Object.assign(resv, { message }))
      }
    }

    return validations.length == 0 ? null : validations
  }
}