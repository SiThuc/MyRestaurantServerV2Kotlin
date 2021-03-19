package com.example.myrestaurantv2kotlinserverapp.adapter

import android.content.Context
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myrestaurantv2kotlinserverapp.database.CartItem
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutOrderDetailItemBinding
import com.example.myrestaurantv2kotlinserverapp.model.AddonModel
import com.example.myrestaurantv2kotlinserverapp.model.SizeModel
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

class MyOrderDetailAdapter(var context: Context,
                           var cartItemList: MutableList<CartItem>) : RecyclerView.Adapter<MyOrderDetailAdapter.MyViewHolder>() {
    lateinit var binding: LayoutOrderDetailItemBinding
    val gson: Gson = Gson()

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        binding = LayoutOrderDetailItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder) {
            val cartItem = cartItemList[position]
            Glide.with(context).load(cartItem.foodImage).into(binding.imgFoodImage)
            binding.txtFoodName.text = cartItem.foodName
            binding.txtFoodQuantity.text = StringBuilder("Quantity: ").append(cartItem.foodQuantity).toString()

            //Fix Crash
            if(cartItem.foodSize == "Default"){
                binding.txtSize.text = StringBuilder("Size: Default")
            }else{
                val sizeModel = gson.fromJson<SizeModel>(cartItem.foodSize, SizeModel::class.java)
                binding.txtSize.text = StringBuilder("Size: ").append(sizeModel.name)
            }

/*            var sizeModel:SizeModel?=null
            try {
                sizeModel = gson.fromJson(cartItem.foodSize, object : TypeToken<SizeModel?>() {}.type)
            }catch (exception: JsonSyntaxException){
                Toast.makeText(context, "Json Exception: "+exception.message, Toast.LENGTH_SHORT).show()
            }
            if (sizeModel != null)
                binding.txtSize.text = StringBuilder("Size: ").append(sizeModel.name)*/

            if (cartItem.foodAddon != "Default") {
                val addonModels: List<AddonModel> = gson.fromJson(cartItem.foodAddon, object : TypeToken<List<AddonModel?>?>() {}.type)
                val addonString = StringBuilder()
                if (addonModels != null) {
                    for (addonModel in addonModels)
                        addonString.append(addonModel.name).append(",")
                    addonString.delete(addonString.length - 1, addonString.length) // Remove last ","
                    binding.txtFoodAddOn.text = StringBuilder("Addon: ").append(addonString)
                }
            } else
                binding.txtFoodAddOn.text = StringBuilder("Addon. Default")
        }
    }

    override fun getItemCount(): Int = cartItemList.size
}