package com.example.myrestaurantv2kotlinserverapp.ui.orders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myrestaurantv2kotlinserverapp.callback.IOrderCallBackListener
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.model.OrderModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*
import kotlin.collections.ArrayList

class OrderViewModel : ViewModel(), IOrderCallBackListener {
    private val orderListLiveData = MutableLiveData<List<OrderModel>>()
    val messageError = MutableLiveData<String>()

    private val orderLoadListener: IOrderCallBackListener

    init {
        orderLoadListener = this
    }

    fun getOrderList(): MutableLiveData<List<OrderModel>>{
        loadOrder(0)
        return orderListLiveData
    }

     fun loadOrder(status: Int) {
        val tempList: MutableList<OrderModel> = ArrayList()
        val orderRef = FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
                .orderByChild("orderStatus")
                .equalTo(status.toDouble())
        orderRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for(item in snapshot.children){
                    val order = item.getValue(OrderModel::class.java)
                    order!!.key = item.key
                    tempList.add(order)
                }
                orderLoadListener.onOrderLoadSuccess(tempList)
            }

            override fun onCancelled(error: DatabaseError) {
                orderLoadListener.onOrderLoadFailed(error.message)
            }
        })
    }

    override fun onOrderLoadSuccess(orderList: List<OrderModel>) {
        if(orderList.isNotEmpty())
            Collections.sort(orderList){t1, t2 ->
                if(t1.createDate < t2.createDate)  return@sort -1
                if(t1.createDate == t2.createDate) 0 else 1
            }
            orderListLiveData.value = orderList
    }

    override fun onOrderLoadFailed(message: String) {
        messageError.value = message
    }


}