package com.example.myrestaurantv2kotlinserverapp.model

class ShipperOrderModel {
    var key: String? = null
    var shipperPhone: String? = null
    var shipperName: String? = null
    var currentLat = 0.0
    var currentLng: Double = 0.0
    var orderModel: OrderModel? = null
    var isStartTrip = false
}
