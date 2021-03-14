package com.example.myrestaurantv2kotlinserverapp.callback

import com.example.myrestaurantv2kotlinserverapp.model.BestDealModel

interface IBestDealsCallbackListener {
    fun onBestDealsListLoadSuccess(bestDealsList: List<BestDealModel>)
    fun onBestDealsListLoadFailed(message: String)
}