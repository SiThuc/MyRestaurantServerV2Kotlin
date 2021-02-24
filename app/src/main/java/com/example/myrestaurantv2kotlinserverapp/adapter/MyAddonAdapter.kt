package com.example.myrestaurantv2kotlinserverapp.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myrestaurantv2kotlinserverapp.callback.IRecyclerItemClickListener
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutSizeAddonItemBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.SelectAddonModel
import com.example.myrestaurantv2kotlinserverapp.evenbus.SelectSizeModel
import com.example.myrestaurantv2kotlinserverapp.model.UpdateAddonModel
import com.example.myrestaurantv2kotlinserverapp.model.AddonModel
import com.example.myrestaurantv2kotlinserverapp.model.SizeModel
import com.example.myrestaurantv2kotlinserverapp.model.UpdateSizeModel
import org.greenrobot.eventbus.EventBus

class MyAddonAdapter(
    var context: Context,
    var addonModelList: MutableList<AddonModel>
    ) : RecyclerView.Adapter<MyAddonAdapter.MyViewHolder>() {
    var editPos: Int
    private var updatedAddonModel: UpdateAddonModel

    init {
        editPos = -1
        updatedAddonModel = UpdateAddonModel()
    }

    lateinit var binding: LayoutSizeAddonItemBinding

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener {
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
        binding = LayoutSizeAddonItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder) {
            val addon = addonModelList[position]
            binding.txtName.text = addon.name
            binding.txtPrice.text = addon.price.toString()

            setListener(object : IRecyclerItemClickListener {
                override fun onItemClick(view: View, pos: Int) {
                    editPos = position
                    EventBus.getDefault().postSticky(SelectAddonModel(addon))
                }
            })

            binding.imgDelete.setOnClickListener {
                if (addonModelList.size > 0) {
                    addonModelList.removeAt(position)
                    notifyItemRemoved(position)
                }
                updatedAddonModel.addonModelList = addonModelList
                EventBus.getDefault().postSticky(updatedAddonModel)

            }
        }
    }

    override fun getItemCount(): Int {
        if (addonModelList.isEmpty())
            return 0
        else
            return addonModelList.size
    }

    fun addNewAddon(addonModel: AddonModel) {
        Log.d("EVENTBUS", addonModel.toString())
        addonModelList.add(addonModel)
        notifyItemInserted(addonModelList.size - 1)
        Log.d("EVENTBUS", "After Add Clicked")
        updatedAddonModel.addonModelList = addonModelList
        EventBus.getDefault().postSticky(updatedAddonModel)
    }

    fun editAddon(addonModel: AddonModel) {
        Log.d("EVENTBUS", "After Edit clicked")
        addonModelList[editPos] = addonModel
        notifyItemChanged(editPos)
        updatedAddonModel.addonModelList = addonModelList
        EventBus.getDefault().postSticky(updatedAddonModel)
    }
}
