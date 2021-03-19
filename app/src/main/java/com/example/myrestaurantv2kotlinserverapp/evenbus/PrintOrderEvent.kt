package com.example.myrestaurantv2kotlinserverapp.evenbus

import com.example.myrestaurantv2kotlinserverapp.model.OrderModel

class PrintOrderEvent(var path: String, var order: OrderModel) {

}
