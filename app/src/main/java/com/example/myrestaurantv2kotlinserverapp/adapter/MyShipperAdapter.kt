package com.example.myrestaurantv2kotlinserverapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutShipperBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.UpdateShipperEvent
import com.example.myrestaurantv2kotlinserverapp.model.ShipperModel
import org.greenrobot.eventbus.EventBus

class MyShipperAdapter(
    var context: Context,
    var shipperList: List<ShipperModel>
) : RecyclerView.Adapter<MyShipperAdapter.MyViewHolder>() {
    lateinit var binding: LayoutShipperBinding

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        binding = LayoutShipperBinding.inflate(LayoutInflater.from(context), parent, false)

        return MyViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder){
            val shipper = shipperList[position]

            binding.txtName.text = shipper.name
            binding.txtPhone.text = shipper.phone
            binding.btnEnable.isChecked = shipper.isActive

            //Event
            binding.btnEnable.setOnCheckedChangeListener { buttonView, isChecked ->
                EventBus.getDefault().postSticky(UpdateShipperEvent(shipper, isChecked))
            }
        }
    }

    override fun getItemCount(): Int = shipperList.size
}