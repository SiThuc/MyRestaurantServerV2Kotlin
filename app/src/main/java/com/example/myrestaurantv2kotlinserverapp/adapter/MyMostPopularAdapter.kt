package com.example.myrestaurantv2kotlinserverapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutCategoryItemBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.*
import com.example.myrestaurantv2kotlinserverapp.model.BestDealsModel
import com.example.myrestaurantv2kotlinserverapp.model.MostPopularModel
import org.greenrobot.eventbus.EventBus


class MyMostPopularAdapter(
    var context: Context,
    var mostPopularList: List<MostPopularModel>
) : RecyclerView.Adapter<MyMostPopularAdapter.MyViewHolder>() {

    lateinit var binding: LayoutCategoryItemBinding
    private val viewBinderHelper = ViewBinderHelper()

    init {
        viewBinderHelper.setOpenOnlyOne(true)
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        binding = LayoutCategoryItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder) {
            with(mostPopularList[position]) {
                Glide.with(context).load(this.image).into(binding.categoryImage)
                binding.categoryName.text = this.name

                viewBinderHelper.bind(binding.root, this.menu_id)

                binding.btnUpdate.setOnClickListener {
                    Common.mostPopularSelected = this
                    EventBus.getDefault().postSticky(UpdateMostPopularEvent(true))
                }

                binding.btnDelete.setOnClickListener {
                    Common.mostPopularSelected = this
                    EventBus.getDefault().postSticky(DeleteMostPopularEvent(true))
                }

            }

        }
    }

    override fun getItemCount(): Int = mostPopularList.size

}