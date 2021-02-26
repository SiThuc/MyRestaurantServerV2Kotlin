package com.example.myrestaurantv2kotlinserverapp.evenbus

import com.example.myrestaurantv2kotlinserverapp.model.ShipperModel

class UpdateShipperEvent(var shipper: ShipperModel, var checked: Boolean)
