package com.example.myrestaurantv2kotlinserverapp.callback

import com.example.myrestaurantv2kotlinserverapp.model.BestDealsModel

interface IBestDealsCallbackListener {
    fun onBestDealsListLoadSuccess(bestDealsList: List<BestDealsModel>)
    fun onBestDealsListLoadFailed(message: String)
}