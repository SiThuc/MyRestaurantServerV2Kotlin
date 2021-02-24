package com.example.myrestaurantv2kotlinserverapp.callback

import com.example.myrestaurantv2kotlinserverapp.model.OrderModel

interface IOrderCallBackListener {
    fun onOrderLoadSuccess(orderList: List<OrderModel>)
    fun onOrderLoadFailed(message: String)
}