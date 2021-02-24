package com.example.myrestaurantv2kotlinserverapp.model

class TokenModel(){
    var uid: String? = null
    var token: String? = null
    constructor(uid: String, token: String) : this() {
        this.uid = uid
        this.token = token
    }
}