package com.example.myrestaurantv2kotlinserverapp.callback

import com.example.myrestaurantv2kotlinserverapp.model.CategoryModel

interface ICategoryCallBackListener {
    fun onCategoryLoadSuccess(categoryList: List<CategoryModel>)
    fun onCategoryLoadFailed(message: String)
}