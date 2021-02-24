package com.example.myrestaurantv2kotlinserverapp.evenbus

import com.example.myrestaurantv2kotlinserverapp.model.OrderModel

class CallCustomerEvent(var orderModel: OrderModel)