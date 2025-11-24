import { fail, sleep, check } from 'k6'
import http from "k6/http"

import { randomInt, parseJsonReponse } from './common.js'


const CONTENT_TYPE_HEADER = { headers: { "Content-Type": "application/json" } }

export const userCartsUtil = {

  /* Required properties */
  testId: null,
  inventoryUtil: null,
  shopServiceDNS: "",
  shopServiceAPIPort: 8081,

  /* Optional properties */
  totalUsers: 10,
  cartMaxItems: 10,
  cartItemMaxQty: 10,
  verbose: true,

  cartItemsQtyFromLastRequest: {},

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
        if (! this.cartItemsQtyFromLastRequest[userId]) {
          this.cartItemsQtyFromLastRequest[userId] = {}
        }
        this.cartItemsQtyFromLastRequest[userId][cartProductId] = 0
      }
    }

    if (reservations.length > 0) {
      this.verboseLog(`remove products from cart - total: ` + reservations.length)
    }

    for (var i = 0; i < productIds.length; ++i) {
      var newQty = randomInt(1, this.cartItemMaxQty)
      reservations.push({
        'productId': productIds[i],
        'quantity': newQty,
      })
      if (! this.cartItemsQtyFromLastRequest[userId]) {
        this.cartItemsQtyFromLastRequest[userId] = {}
      }
      this.cartItemsQtyFromLastRequest[userId][productIds[i]] = newQty
    }

    this.verboseLog(`put cart products - total: ` + productIds.length)

    var body = { 'userId': userId, 'entries': reservations }
    var response = http.put(`http://${this.shopServiceDNS}:${this.shopServiceAPIPort}/api/v1/carts/${userId}`, JSON.stringify(body), CONTENT_TYPE_HEADER)
    var responseBody = parseJsonReponse(response)
    if (response.status != 200) {
      fail(this.logTemplate('fail to put cart products: ' + JSON.stringify(responseBody)))
    }
    this.verboseLog('put cart products successfully')

    return reservations
  },

  loadUserCart(userId) {
    var response = http.get(`http://${this.shopServiceDNS}:${this.shopServiceAPIPort}/api/v1/carts/${userId}`)
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
    for (var i = 0; i < ids.length; ++i) {
      while (true) {
        validations[ids[i]] = userCartsUtil.validateUserCart(ids[i])
        if (validations[ids[i]] != null) {
          console.log(this.logTemplate(`retry validating user cart...`))
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

      var resv = null
      for (var i = 0; i < reservations.length; ++i) {
        if (reservations[i]['productId'] == prodId) {
          resv = reservations[i]
          break
        }
      }
      if (! resv) {
        validations.push(`cart.items does not contain reservation of product_id ${prodId}`)
        continue
      }
      
      var message = ""
      var now = new Date()
      var expiresAt = new Date(resv['expiresAt'])
      var { desiredAmount, reservedAmount, reservationStatus } = cart['items'][prodId]

      /* Verify cart.items amounts with product_reservations amounts */
      
      if (reservedAmount != resv['reservedAmount']) {
        message = `cart.items[*].reservedAmount (${reservedAmount}) and reservations[*].reservedAmount (${resv['reservedAmount']}) value mismatch`
      }
      else if (desiredAmount != resv['desiredAmount']) {
        message = `cart.items[*].desiredAmount (${desiredAmount}) and reservations[*].desiredAmount (${resv['desiredAmount']}) value mismatch`
      }

      /* Verify if removed all items with quantity is equal to zero */
      
      else if (desiredAmount == 0) {
        message = "zero amount reservation is not removed"
      }

      /* Verify cart.items statuses with product_reservations statuses */

      else if (reservationStatus != resv['status']) {
        message = `cart.items[*].reservationStatus (${reservationStatus}) and reservations[*].reservationStatus (${resv['status']}) value mismatch`
      }
      else if (expiresAt > now && reservationStatus == 'EXPIRED') {
        message = "wrong status. Reservation has not yet expired but it's status is \"EXPIRED\""
      }
      else if (reservedAmount == desiredAmount && reservationStatus != 'OK') {
        message = "wrong status. Expected \"OK\""
      }
      else if (reservedAmount < desiredAmount && reservationStatus != 'INSUFFICIENT_STOCK') {
        message = "wrong status. Expected \"INSUFFICIENT_STOCK\""
      }

      /* Check if cart.items amounts are valid */

      else if (reservedAmount > desiredAmount) {
        message = `invalid amount. reserved_amount (${reservedAmount}) is currently greater than desired_amount (${desiredAmount})`
      }
      else if (reservedAmount != resv['reservedAmount']) {
        message = `cart.items[*].reservedAmount (${reservedAmount}) and reservations[*].reservedAmount (${resv['reservedAmount']}) value mismatch`
      }

      /* Check if cart.items amounts match the user activities history */

      // else if (reservedAmount != this.cartItemsQtyFromLastRequest[id][prodId]) {
      //   message = `cart.items[*].reservedAmount does not match the amount of the last update request`
      // }

      if (message != "") {
        validations.push(Object.assign(resv, { message }))
      }
    }

    return validations.length == 0 ? null : validations
  }
}