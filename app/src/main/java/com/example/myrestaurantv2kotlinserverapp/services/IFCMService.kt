package com.example.myrestaurantv2kotlinserverapp.services

import com.example.myrestaurantv2kotlinserverapp.model.FCMResponse
import com.example.myrestaurantv2kotlinserverapp.model.FCMSendData
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface IFCMService {
    @Headers(
            "Content-Type:application/json",
            "Authorization:key=AAAANa9JOo4:APA91bHEwIzqGMslqfSQNm_33s0TRANaCHY3UBRjoXYdhWMPjSkKZjohZ_rfEpG2CJMW3cAmV5mrt06S4NEUgwRGBnRxY1xcI4Ii6bV27AcaBxdO36DeMHYaj9cyAdMJkAE9-vmzUBmX"
    )

    @POST("fcm/send")
    fun sendNotification(@Body body: FCMSendData): Observable<FCMResponse>
}