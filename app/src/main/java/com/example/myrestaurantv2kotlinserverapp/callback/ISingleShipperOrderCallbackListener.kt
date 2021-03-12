package com.example.myrestaurantv2kotlinserverapp.callback

import com.example.myrestaurantv2kotlinserverapp.model.ShipperOrderModel

interface ISingleShipperOrderCallbackListener {
    fun onSingleShipperOrderSuccess(shipperOrderModel: ShipperOrderModel)
}