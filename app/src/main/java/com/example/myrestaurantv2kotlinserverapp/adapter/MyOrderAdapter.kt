package com.example.myrestaurantv2kotlinserverapp.adapter

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.example.myrestaurantv2kotlinserverapp.callback.IRecyclerItemClickListener
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.database.CartItem
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutDialogOrderDetailBinding
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutOrderItemBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.*
import com.example.myrestaurantv2kotlinserverapp.model.OrderModel
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
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

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private var listener: IRecyclerItemClickListener? = null

        fun setListener(listener: IRecyclerItemClickListener) {
            this.listener = listener
        }

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            listener!!.onItemClick(v!!, adapterPosition)
        }

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

            //Show the directions
            binding.btnDirection.setOnClickListener {
                EventBus.getDefault().postSticky(TrackOrderEvent(order, position))
            }

            //Show the directions
            binding.btnPrint.setOnClickListener {
                Dexter.withContext(context)
                    .withPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .withListener(object : PermissionListener {
                        override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                            EventBus.getDefault().postSticky(PrintOrderEvent(
                                StringBuilder(Common.getAppPath(context)).append(Common.FILE_PRINT).toString()
                                ,order))
                        }

                        override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                            Toast.makeText(context, "You should accept this permission to print order", Toast.LENGTH_SHORT).show()
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            p0: PermissionRequest?,
                            p1: PermissionToken?
                        ) {
                        }
                    }).check()
            }

            binding.imgFoodImage.setOnClickListener {
                showDialog(order.cartItemList)
            }

            //Event to show Order Detail
            /*setListener(object : IRecyclerItemClickListener {
                override fun onItemClick(view: View, pos: Int) {
                    Log.d("DEBUG", "Clicked")
                    showDialog(order.cartItemList)
                }

            })*/
        }
    }

    private fun showDialog(cartItemList: List<CartItem>?) {
        val dialogBinding = LayoutDialogOrderDetailBinding.inflate(LayoutInflater.from(context))

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogBinding.root)

        dialogBinding.recyclerOrderDetail.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        dialogBinding.recyclerOrderDetail.layoutManager = layoutManager
        dialogBinding.recyclerOrderDetail.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))
        val adapter = MyOrderDetailAdapter(context, cartItemList!!.toMutableList())
        dialogBinding.recyclerOrderDetail.adapter = adapter

        //Show dialog
        val dialog = builder.create()
        dialog.show()

        //Custom dialog
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setGravity(Gravity.CENTER)

        dialogBinding.btnOk.setOnClickListener { dialog.dismiss() }
    }


    override fun getItemCount(): Int = orderList.size

    fun removeItem(order: OrderModel) {
        orderList.remove(order)
    }

    fun getItemAtPosition(position:Int): OrderModel{
        return orderList[position]
    }
}