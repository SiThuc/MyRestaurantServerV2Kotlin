package com.example.myrestaurantv2kotlinserverapp.callback

interface ILoadTimeFromFirebaseCallback {
    fun onLoadOnlyTimeSuccess(estimatedTimeMs: Long)
    fun onLoadTimeFailed(message: String)
}
