package com.example.myrestaurantv2kotlinserverapp.model

import com.example.myrestaurantv2kotlinserverapp.database.CartItem

class OrderModel(
        var key: String? = null,
        var userId: String? = null,
        var userName: String? = null,
        var userPhone: String? = null,
        var shippingAddress: String? = null,
        var comment: String? = null,
        var transactionId: String? = null,
        var lat: Double = 0.0,
        var lng: Double = 0.0,
        var totalPayment: Double = 0.0,
        var finalPayment: Double = 0.0,
        var isCod: Boolean = false,
        var discount: Int = 0,
        var cartItemList: List<CartItem>? = null,
        var createDate: Long = 0,
        var orderNumber: String? = null,
        var orderStatus: Int = 0
)