package com.example.myrestaurantv2kotlinserverapp.callback

import android.app.AlertDialog
import android.widget.Button
import android.widget.RadioButton
import com.example.myrestaurantv2kotlinserverapp.model.OrderModel
import com.example.myrestaurantv2kotlinserverapp.model.ShipperModel


interface IShipperLoadCallbackListener {
    fun onShipperLoadSuccess(shipperModelList: List<ShipperModel>?)

//    fun onShipperLoadSuccess(
//        pos: Int,
//        orderModel: OrderModel?,
//        shipperModels: List<ShipperModel>?,
//        dialog: AlertDialog?,
//        btn_ok: Button?,
//        btn_cancel: Button?,
//        rdi_shipping: RadioButton?,
//        rdi_shipped: RadioButton?,
//        rdi_cancelled: RadioButton?,
//        rdi_delete: RadioButton?,
//        rdi_restore_placed: RadioButton?
//    )

    fun onShipperLoadFailed(message: String?)
}