package com.example.myrestaurantv2kotlinserverapp.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutOrderItemBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.CallCustomerEvent
import com.example.myrestaurantv2kotlinserverapp.evenbus.DeleteOrderEvent
import com.example.myrestaurantv2kotlinserverapp.evenbus.UpdateOrderEvent
import com.example.myrestaurantv2kotlinserverapp.model.OrderModel
import org.greenrobot.eventbus.EventBus
import java.lang.String
import java.lang.StringBuilder
import java.text.SimpleDateFormat

class MyOrderAdapter(
    var context: Context,
    var orderList: MutableList<OrderModel>
) : RecyclerView.Adapter<MyOrderAdapter.MyViewHolder>() {

    lateinit var binding: LayoutOrderItemBinding
    private val viewBinderHelper = ViewBinderHelper()
    private val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

    init {
        viewBinderHelper.setOpenOnlyOne(true)
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        binding = LayoutOrderItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder) {
            val order = orderList[position]
            viewBinderHelper.bind(binding.root, order.key)

            Glide.with(context).load(order.cartItemList!![0].foodImage).into(binding.imgFoodImage)
            binding.txtOrderNumber.text = StringBuilder("No.:").append(order.key)
            Common.setSpanStringColor(
                "Order date: ",
                simpleDateFormat.format(order.createDate),
                binding.txtTime,
                Color.parseColor("#333639")
            )
            Common.setSpanStringColor(
                "Order status ", Common.convertStatusToString(order.orderStatus),
                binding.txtOrderStatus, Color.parseColor("#00579A")
            )
            Common.setSpanStringColor(
                "Name ", order.userName,
                binding.txtName, Color.parseColor("#00574B")
            )
            Common.setSpanStringColor(
                "Num of items ",
                if (order.cartItemList == null) "0" else String.valueOf(order.cartItemList!!.size),
                binding.txtNumItem,
                Color.parseColor("#4B647D")
            )

            /*-------------Events----------*/
            //Call
            binding.btnCall.setOnClickListener {
                EventBus.getDefault().postSticky(CallCustomerEvent(order))
            }

            //Delete
            binding.btnDelete.setOnClickListener {
                EventBus.getDefault().postSticky(DeleteOrderEvent(order))
            }

            //Update
            binding.btnUpdate.setOnClickListener {
                EventBus.getDefault().postSticky(UpdateOrderEvent(order, position))
            }
        }
    }

    override fun getItemCount(): Int = orderList.size

    fun removeItem(order: OrderModel){
        orderList.remove(order)
    }
}