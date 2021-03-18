package com.example.myrestaurantv2kotlinserverapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myrestaurantv2kotlinserverapp.callback.IRecyclerItemClickListener
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.databinding.ActivityChatListBinding
import com.example.myrestaurantv2kotlinserverapp.model.ChatInfoModel
import com.example.myrestaurantv2kotlinserverapp.viewholder.ChatListViewHolder
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query

class ChatListActivity : AppCompatActivity() {
    lateinit var binding: ActivityChatListBinding

    lateinit var database: FirebaseDatabase
    lateinit var chatRef: DatabaseReference

    lateinit var adapter: FirebaseRecyclerAdapter<ChatInfoModel, ChatListViewHolder>
    lateinit var options: FirebaseRecyclerOptions<ChatInfoModel>

    lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        loadChatList()
    }

    override fun onStart() {
        super.onStart()
        if(adapter != null) adapter.startListening()
    }

    override fun onResume() {
        super.onResume()
        if(adapter != null) adapter.startListening()
    }

    override fun onStop() {
        if(adapter != null) adapter.stopListening()
        super.onStop()
    }

    private fun loadChatList() {
        adapter = object: FirebaseRecyclerAdapter<ChatInfoModel, ChatListViewHolder>(options){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
                return ChatListViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_message_list_item, parent, false))
            }

            override fun onBindViewHolder(
                holder: ChatListViewHolder,
                position: Int,
                model: ChatInfoModel
            ) {
                holder.txtEmail.text = model.createName
                holder.txtChatMessage.text = model.lastMessage

                holder.setListener(object : IRecyclerItemClickListener {
                    override fun onItemClick(view: View, pos: Int) {
                        //Later
                        Toast.makeText(this@ChatListActivity, model.lastMessage, Toast.LENGTH_SHORT).show()
                    }
                })
            }

        }
        binding.recyclerChatList.adapter = adapter
    }

    private fun initViews() {

        database = FirebaseDatabase.getInstance()
        chatRef = database.getReference(Common.CHAT_REF)

        val query: Query = chatRef
        options = FirebaseRecyclerOptions.Builder<ChatInfoModel>()
            .setQuery(query, ChatInfoModel::class.java)
            .build()

        layoutManager = LinearLayoutManager(this)
        binding.recyclerChatList.layoutManager = layoutManager
        binding.recyclerChatList.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))

        binding.toolbar.title = Common.currentServerUser!!.name
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

    }

}