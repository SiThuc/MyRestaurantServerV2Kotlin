package com.example.myrestaurantv2kotlinserverapp.model

data class CategoryModel(
        var menu_id: String? = null,
        var name: String? = null,
        var image: String? = null,
        var foods: MutableList<FoodModel>? = null)