package com.example.myrestaurantv2kotlinserverapp.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myrestaurantv2kotlinserverapp.callback.IRecyclerItemClickListener
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutSizeAddonItemBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.SelectSizeModel
import com.example.myrestaurantv2kotlinserverapp.model.SizeModel
import com.example.myrestaurantv2kotlinserverapp.model.UpdateSizeModel
import org.greenrobot.eventbus.EventBus

class MySizeAdapter(
    var context: Context,
    var sizeModelList: MutableList<SizeModel>
) : RecyclerView.Adapter<MySizeAdapter.MyViewHolder>() {
    var editPos:Int
    private var updatedSizeModel: UpdateSizeModel

    init {
        editPos = -1
        updatedSizeModel = UpdateSizeModel()
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
            val size = sizeModelList[position]
            binding.txtName.text = size.name
            binding.txtPrice.text = size.price.toString()

            setListener(object : IRecyclerItemClickListener {
                override fun onItemClick(view: View, pos: Int) {
                    editPos = position
                    EventBus.getDefault().postSticky(SelectSizeModel(size))
                }
            })

            binding.imgDelete.setOnClickListener {
                if(sizeModelList.size > 0){
                    sizeModelList.removeAt(position)
                    notifyItemRemoved(position)
                }
                updatedSizeModel.sizeModelList = sizeModelList
                EventBus.getDefault().postSticky(updatedSizeModel)

            }
        }
    }

    override fun getItemCount(): Int{
        if(sizeModelList.isEmpty())
            return 0
        else
            return sizeModelList.size
    }

    fun addNewSize(sizeModel: SizeModel) {
        Log.d("EVENTBUS", sizeModel.toString())
        sizeModelList.add(sizeModel)
        notifyItemInserted(sizeModelList.size - 1)
        Log.d("EVENTBUS", "After Add Clicked")
        updatedSizeModel.sizeModelList = sizeModelList
        EventBus.getDefault().postSticky(updatedSizeModel)
    }

    fun editSize(sizeModel: SizeModel) {
        Log.d("EVENTBUS", "After Edit clicked")
        sizeModelList[editPos] = sizeModel
        notifyItemChanged(editPos)
        updatedSizeModel.sizeModelList = sizeModelList
        EventBus.getDefault().postSticky(updatedSizeModel)
    }

}