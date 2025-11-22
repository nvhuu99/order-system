import { fail, sleep } from 'k6'
import http from "k6/http"

import { randomInt, parseJsonReponse } from './common.js'


const CONTENT_TYPE_HEADER = { headers: { "Content-Type": "application/json" } }

export const inventoryUtil = {

  /* Required properties */
  testId: "",
  inventoryAddr: "",
  shopAddr: "",

  /* Optional properties */
  productsTotal: 100,
  maxProductStock: 100,
  verbose: true,
  summaryWaitForSyncSeconds: 10,


  logTemplate(message) {
    return `vu: ${__VU} - ${message}`
  },

  verboseLog(message) {
    if (this.verbose == 'true') {
      console.log(this.logTemplate(message))
    } 
  },


  init(properties) {
    Object.assign(this, properties)
  },

  seedProducts() {
    var count = this.productsTotal
    while (count) {
      var body = {
        name: `test_${this.testId}_product_${count}`,
        price: randomInt(1, 10),
        stock: randomInt(1, this.maxProductStock),
        reservationsExpireAfterSeconds: 3600,
      }
      var response = http.post(`${this.inventoryAddr}/api/v1/admin/products`, JSON.stringify(body), CONTENT_TYPE_HEADER)
      var responseBody = parseJsonReponse(response)
      if (response.status != 201) {
        fail(this.logTemplate("failed to seed products: " + JSON.stringify(responseBody)))
      }
      count--
    }
    this.verboseLog("seed products successfully")
  },

  deleteProducts() {
    var limit = 150
    var totalPages = Math.ceil(this.productsTotal / limit)
    for (var page = 1; page <= totalPages; ++page) {
      var products = this.listProducts(page, limit)
      for (var p = 0; p < products.length; ++p) {
        var response = http.del(`${this.inventoryAddr}/api/v1/admin/products/${products[p]['id']}`)
        var responseBody = parseJsonReponse(response)
        if (response.status != 200) {
          fail(this.logTemplate(`fail to delete products: ${JSON.stringify(responseBody)}`))
        }
      }
    }
    this.verboseLog(`delete products successfully`)
  },

  listRandomProducts(limit) {
    var totalPages = Math.ceil(this.productsTotal / limit)
    var page = randomInt(1, totalPages)
    return this.listProducts(page, limit)
  },
  
  listProducts(page, limit) {
    var body = { 
      nameSearch: `test_${this.testId}_product_`, 
      page,
      limit
    }
    var response = http.post(`${this.inventoryAddr}/api/v1/admin/products/list`, JSON.stringify(body), CONTENT_TYPE_HEADER)
    var responseBody = parseJsonReponse(response)
    if (response.status != 200) {
      fail(this.logTemplate(`fail to list products: ${JSON.stringify(responseBody)}`))
    }
    var products = responseBody['data']
    this.verboseLog(`list products successfully - page: ${page} - limit: ${limit} - total: ${products.length}`)
    return products
  },

  getProduct(id) {
    var response = http.get(`${this.inventoryAddr}/api/v1/admin/products/${id}`)
    var responseBody = parseJsonReponse(response)
    if (response.status != 200) {
      fail(this.logTemplate(`fail to get product - id ${id}: ${JSON.stringify(responseBody)}`))
    }
    var product = responseBody['data']
    this.verboseLog(`get product successfully: ${JSON.stringify(product)}}`)
    return product
  },

  listReservationsByUserId(id) {
    return this.listReservations({ userId: id })
  },

  listReservationsByProductId(id) {
    return this.listReservations({ productId: id })
  },

  listReservations(params) {
    var response = http.post(`${this.inventoryAddr}/api/v1/admin/product-reservations/list`, JSON.stringify(params), CONTENT_TYPE_HEADER)
    var responseBody = parseJsonReponse(response)
    if (response.status != 200) {
      fail(this.logTemplate(`fail to get product_reservations - ${JSON.stringify(responseBody)}`))
    }
    var reservations = responseBody['data']
    this.verboseLog(`get product_reservations successfully - total: ${reservations.length}`)
    return reservations
  },

  getProductAvailability(id) {
    var response = http.get(`${this.inventoryAddr}/api/v1/admin/product-availabilities/${id}`, CONTENT_TYPE_HEADER)
    var responseBody = parseJsonReponse(response)
    if (response.status != 200) {
      fail(this.logTemplate(`fail to get product_availability - id ${id}: ${JSON.stringify(responseBody)}`))
    }
    var data = responseBody['data']
    this.verboseLog(`get product_availability successfully: ${JSON.stringify(data)}}`)
    return data
  },

  aggregateTotalSuccessReservationRequestByHosts() {
    var limit = 100
    var totalPages = Math.ceil(this.productsTotal / limit)
    var page = 1
    var totalGroups = {}
    var sumTotals = 0
    while (page <= totalPages) {
      var products = inventoryUtil.listProducts(page, limit)
      var ids = products.map(p => p['id'])
      for (var i = 0; i < ids.length; ++i) {
        var response = http.get(`${this.inventoryAddr}/api/v1/admin/product-reservations/reservation-requests-handled-total/${ids[i]}`, CONTENT_TYPE_HEADER)
        var responseBody = parseJsonReponse(response)
        if (response.status != 200) {
          fail(this.logTemplate(`fail to get reservation requests handled total - id ${ids[i]}: ${JSON.stringify(responseBody)}`))
        }
        this.verboseLog(`get reservation requests handled total success - id ${ids[i]} - total: ${responseBody['data']['handledTotal']}`)
        if (! totalGroups[this.inventoryAddr]) {
          totalGroups[this.inventoryAddr] = 0
        }
        totalGroups[this.inventoryAddr] += responseBody["data"]["handledTotal"]
        sumTotals += responseBody["data"]["handledTotal"]
      }
      page++
    }

    return { totalGroups, sumTotals }
  },

  tryValidateAllProductAvailabilities() {
    var limit = 100
    var totalPages = Math.ceil(this.productsTotal / limit)
    var page = 1
    var validations = {}
    while (page <= totalPages) {
      var products = inventoryUtil.listProducts(page, limit)
      var ids = products.map(p => p['id'])
      for (var i = 0; i < ids.length; ++i) {
        var wait = this.summaryWaitForSyncSeconds
        while (true) {
          validations[ids[i]] = inventoryUtil.validateProductAvailability(ids[i])
          if (wait > 0 && validations[ids[i]] != null) {
            sleep(1)
            wait--
            continue
          }
          break
        }
      }
      page++
    }
    
    return validations
  },

  /**
   * Check if `product_reservations, product_availability, product` are in-sync
   * 
   * @param {*} id 
   * @returns when in-sync, return `null`. Otherwise, return the validations
   */
  validateProductAvailability(prodId) {
    var reservations = this.listReservationsByProductId(prodId)
    var availability = this.getProductAvailability(prodId)
    var product = this.getProduct(prodId)

    var accumulation = { 'reservedAmount': 0, 'desiredAmount': 0}
    var validations = []
    for (var i = 0; i < reservations.length; ++i) {
      accumulation.reservedAmount += reservations[i]['reservedAmount']
      accumulation.desiredAmount += reservations[i]['desiredAmount']
    }

    if (availability.stock != product.stock) {
      validations.push(`product_availability.stock (${availability.stock}) is not equal to product.stock (${product.stock})`)
    }
    if (availability.reservedAmount != accumulation.reservedAmount) {
      validations.push(`product_availability.reserved_amount (${availability.reservedAmount}) is not equal to the accumulation of product_reservations.reserved_amount (${accumulation.reservedAmount})`)
    }

    return validations.length == 0 ? null : validations
  },
}
