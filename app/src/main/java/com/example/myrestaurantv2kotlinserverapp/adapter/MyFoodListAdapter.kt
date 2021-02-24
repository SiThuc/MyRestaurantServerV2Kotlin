package com.example.myrestaurantv2kotlinserverapp.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.example.myrestaurantv2kotlinserverapp.SizeAddonEditActivity
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutFoodListItemBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.AddonSizeEditEvent
import com.example.myrestaurantv2kotlinserverapp.evenbus.ShowSizeAddonEvent
import com.example.myrestaurantv2kotlinserverapp.evenbus.ShowDeleteDialog
import com.example.myrestaurantv2kotlinserverapp.evenbus.ShowUpdateDialog
import com.example.myrestaurantv2kotlinserverapp.model.FoodModel
import org.greenrobot.eventbus.EventBus

class MyFoodListAdapter(
    var context: Context,
    var foodList: List<FoodModel>
) : RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder>() {
    private lateinit var binding: LayoutFoodListItemBinding
    private val viewBinderHelper = ViewBinderHelper()
    init {
        viewBinderHelper.setOpenOnlyOne(true)
    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        binding = LayoutFoodListItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder) {
            val food = foodList[position]

            viewBinderHelper.bind(binding.root, food.id)

            Glide.with(context).load(food.image).into(binding.imgFood)
            binding.txtFoodName.text = food.name
            binding.txtFoodPrice.text = StringBuilder("€").append(food.price)

            //Event to delete Food Item
            binding.btnDelete.setOnClickListener {
                EventBus.getDefault().postSticky(ShowDeleteDialog(position))
            }

            //Event to Update Food Item
            binding.btnUpdate.setOnClickListener {
                EventBus.getDefault().postSticky(ShowUpdateDialog(position))
            }

            //Event when click for add or update Size
            binding.btnSize.setOnClickListener {

                val foodModel = getItemAtPosition(position)
                if(foodModel.positionInList != -1)
                    Common.foodSelected = foodList[position]
                else
                    Common.foodSelected = foodModel
                val intent = Intent(context, SizeAddonEditActivity::class.java)
                context.startActivity(intent)
                if(foodModel.positionInList == -1)
                    EventBus.getDefault().postSticky(AddonSizeEditEvent(false, position))
                else
                    EventBus.getDefault().postSticky(AddonSizeEditEvent(false, foodModel.positionInList))
            }

            //Event when click for add or update Addon
            binding.btnAddOn.setOnClickListener {
                val foodModel = getItemAtPosition(position)

                if(foodModel.positionInList != -1)
                    Common.foodSelected = foodList[position]
                else
                    Common.foodSelected = foodModel

                val intent = Intent(context, SizeAddonEditActivity::class.java)
                context.startActivity(intent)
                if(foodModel.positionInList == -1)
                    EventBus.getDefault().postSticky(AddonSizeEditEvent(true, position))
                else
                    EventBus.getDefault().postSticky(AddonSizeEditEvent(true, foodModel.positionInList))
            }
        }
    }

    override fun getItemCount(): Int = foodList.size

    fun getItemAtPosition(position: Int): FoodModel {
        return foodList[position]

    }
}