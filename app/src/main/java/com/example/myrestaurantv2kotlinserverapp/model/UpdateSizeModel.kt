package com.example.myrestaurantv2kotlinserverapp.model

class UpdateSizeModel() {
    var sizeModelList: List<SizeModel>? = null

    constructor(sizeModelList: List<SizeModel>?):this(){
        this.sizeModelList = sizeModelList
    }
}