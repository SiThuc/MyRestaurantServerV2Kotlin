package com.example.myrestaurantv2kotlinserverapp.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.example.myrestaurantv2kotlinserverapp.callback.IRecyclerItemClickListener
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutCategoryItemBinding
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutUpdateCategoryBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.*
import com.example.myrestaurantv2kotlinserverapp.model.CategoryModel
import com.google.android.gms.tasks.Task
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.HashMap


class MyCategoryAdapter(
    var context: Context,
    var categoryList: List<CategoryModel>
) : RecyclerView.Adapter<MyCategoryAdapter.MyViewHolder>() {

    lateinit var binding: LayoutCategoryItemBinding
    private val viewBinderHelper = ViewBinderHelper()
    init {
        viewBinderHelper.setOpenOnlyOne(true)
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        binding = LayoutCategoryItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        with(holder) {
            with(categoryList[position]) {
                Glide.with(context).load(this.image).into(binding.categoryImage)
                binding.categoryName.text = this.name

                viewBinderHelper.bind(binding.root, this.menu_id)

                binding.categoryImage.setOnClickListener {
                    Common.categorySelected = this
                    EventBus.getDefault().postSticky(CategoryClick(true, this))
                    Log.d("ActionEvent", "Event is sent")
                }

                //When user click on the button to Update
                binding.btnUpdate.setOnClickListener {
                    //Display the Update Dialog
                    Common.categorySelected = this
                    EventBus.getDefault().postSticky(ShowUpdateCategoryDialog(this))
                }
            }

        }
    }

    override fun getItemCount(): Int = categoryList.size

}