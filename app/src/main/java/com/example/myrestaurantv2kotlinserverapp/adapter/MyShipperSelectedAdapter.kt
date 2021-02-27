package com.example.myrestaurantv2kotlinserverapp.adapter

import android.content.Context
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.myrestaurantv2kotlinserverapp.R
import com.example.myrestaurantv2kotlinserverapp.callback.IRecyclerItemClickListener
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutShipperBinding
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutShipperSelectedBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.UpdateShipperEvent
import com.example.myrestaurantv2kotlinserverapp.model.ShipperModel
import org.greenrobot.eventbus.EventBus

class MyShipperSelectedAdapter(
        var context: Context,
        var shipperList: List<ShipperModel>
) : RecyclerView.Adapter<MyShipperSelectedAdapter.MyViewHolder>() {
    lateinit var binding: LayoutShipperSelectedBinding
    private var lastCheckedImageView: ImageView? = null
    private var selectedShipper: ShipperModel? = null

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
        binding = LayoutShipperSelectedBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder) {
            val shipper = shipperList[position]
            binding.txtName.text = shipper.name
            binding.txtPhone.text = shipper.phone

            //Event
            setListener(object : IRecyclerItemClickListener{
                override fun onItemClick(view: View, pos: Int) {
                    if(lastCheckedImageView != null){
                        lastCheckedImageView!!.setImageResource(0)
                        binding.imgChecked.visibility = View.GONE
                        lastCheckedImageView = null
                        selectedShipper = null
                    }else{
                        binding.imgChecked.setImageResource(R.drawable.ic_check_circle_outline_24)
                        binding.imgChecked.visibility = View.VISIBLE
                        lastCheckedImageView = binding.imgChecked
                        selectedShipper = shipper

                    }
                }
            })

        }
    }

    fun getSelectedShipper(): ShipperModel? {
        if(selectedShipper!= null)
            return selectedShipper!!
        else
            return null
    }

    override fun getItemCount(): Int = shipperList.size
}