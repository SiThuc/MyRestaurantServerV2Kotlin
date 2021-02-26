package com.example.myrestaurantv2kotlinserverapp.model

class ShipperModel() {
    var key: String? = null
    var uid: String? = null
    var name: String? = null
    var phone: String? = null
    var isActive: Boolean = false

    constructor(key: String, uid: String, name: String, phone: String, isActive: Boolean) : this() {
        this.key = key
        this.uid = uid
        this.name = name
        this.phone = phone
        this.isActive = isActive
    }
}

