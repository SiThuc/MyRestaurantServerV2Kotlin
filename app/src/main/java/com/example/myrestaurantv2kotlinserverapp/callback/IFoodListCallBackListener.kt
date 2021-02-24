package com.example.myrestaurantv2kotlinserverapp.callback
import com.example.myrestaurantv2kotlinserverapp.model.FoodModel

interface IFoodListCallBackListener {
    fun onFoodListLoadSuccess(foodList: List<FoodModel>)
    fun onFoodListLoadFailed(message: String)
}