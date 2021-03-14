package com.example.myrestaurantv2kotlinserverapp.callback

import com.example.myrestaurantv2kotlinserverapp.model.MostPopularModel

interface IMostPopularCallbackListener {
    fun onMostPopularListLoadSuccess(mostPopularList: List<MostPopularModel>)
    fun onMostPopularListLoadFailed(message: String)
}