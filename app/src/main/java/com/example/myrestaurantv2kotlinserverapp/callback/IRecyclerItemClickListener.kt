package com.example.myrestaurantv2kotlinserverapp.callback

import android.view.View

interface IRecyclerItemClickListener {
    fun onItemClick(view: View, pos: Int)
}