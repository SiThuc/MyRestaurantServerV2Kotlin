package com.example.myrestaurantv2kotlinserverapp.viewholder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myrestaurantv2kotlinserverapp.R
import com.example.myrestaurantv2kotlinserverapp.callback.IRecyclerItemClickListener

class ChatListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    lateinit var txtEmail: TextView
    lateinit var txtChatMessage: TextView

    private var listener: IRecyclerItemClickListener?=null

    fun setListener(listener: IRecyclerItemClickListener){
        this.listener = listener
    }

    init {
        txtEmail = itemView.findViewById(R.id.txt_email) as TextView
        txtChatMessage = itemView.findViewById(R.id.txt_chat_message) as TextView
        itemView.setOnClickListener { view -> listener!!.onItemClick(view, adapterPosition) }
    }
}