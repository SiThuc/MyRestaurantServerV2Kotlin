package com.example.myrestaurantv2kotlinserverapp.ui

import android.app.AlertDialog
import android.widget.Button
import android.widget.RadioButton
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myrestaurantv2kotlinserverapp.callback.IShipperLoadCallbackListener
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.model.OrderModel
import com.example.myrestaurantv2kotlinserverapp.model.ShipperModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class ShipperViewModel : ViewModel(), IShipperLoadCallbackListener {
    private var messageError: MutableLiveData<String>? = null
    private var shipperMutableList: MutableLiveData<List<ShipperModel>>? = null
    private var shipperListener: IShipperLoadCallbackListener = this

    fun getMessageError(): MutableLiveData<String> {
        return messageError!!
    }

    fun getShipperList(): MutableLiveData<List<ShipperModel>> {
        if (shipperMutableList == null) {
            shipperMutableList = MutableLiveData()
            loadShipperList()
        }
        return shipperMutableList!!
    }

    private fun loadShipperList() {
        val tempList = ArrayList<ShipperModel>()
        val shipperRef = FirebaseDatabase.getInstance().getReference(Common.SHIPPER_REF)
        shipperRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (shipperSnapShot in snapshot.children) {
                    val shipperModel = shipperSnapShot.getValue(ShipperModel::class.java)
                    shipperModel!!.key = shipperSnapShot.key!!
                    tempList.add(shipperModel)
                }
                shipperListener.onShipperLoadSuccess(tempList)
            }

            override fun onCancelled(error: DatabaseError) {
                shipperListener.onShipperLoadFailed(error.message)
            }
        })
    }

    override fun onShipperLoadSuccess(shipperModelList: List<ShipperModel>?) {
        if (shipperModelList != null)
            shipperMutableList!!.value = shipperModelList
    }


    override fun onShipperLoadSuccess(
        pos: Int,
        orderModel: OrderModel?,
        shipperModels: List<ShipperModel?>?,
        dialog: AlertDialog?,
        btn_ok: Button?,
        btn_cancel: Button?,
        rdi_shipping: RadioButton?,
        rdi_shipped: RadioButton?,
        rdi_cancelled: RadioButton?,
        rdi_delete: RadioButton?,
        rdi_restore_placed: RadioButton?
    ) {
        //Do nothing
    }

    override fun onShipperLoadFailed(message: String?) {
        messageError!!.value = message
    }
}