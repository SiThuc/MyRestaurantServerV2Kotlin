package com.example.myrestaurantv2kotlinserverapp.database

class CartItem(
        var foodId: String = "",
        var foodName: String? = null,
        var foodImage: String? = null,
        var foodPrice: Double? = null,
        var foodQuantity: Int? = null,
        var foodAddon: String = "",
        var foodSize: String = "",
        var userPhone: String? = null,
        var foodExtraPrice: Double? = 0.0,
        var uid: String = "")
