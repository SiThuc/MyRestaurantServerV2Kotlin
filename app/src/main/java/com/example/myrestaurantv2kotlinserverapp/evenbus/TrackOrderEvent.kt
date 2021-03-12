package com.example.myrestaurantv2kotlinserverapp.evenbus

import com.example.myrestaurantv2kotlinserverapp.model.OrderModel

class TrackOrderEvent(var order: OrderModel, var position: Int)