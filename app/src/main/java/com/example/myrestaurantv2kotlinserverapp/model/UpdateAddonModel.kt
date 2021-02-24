package com.example.myrestaurantv2kotlinserverapp.model

class UpdateAddonModel () {
    var addonModelList: List<AddonModel>? = null

    constructor(addonModelList: List<AddonModel>?):this(){
        this.addonModelList = addonModelList
    }
}