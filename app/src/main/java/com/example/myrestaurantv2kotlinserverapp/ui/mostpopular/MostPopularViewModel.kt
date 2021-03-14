package com.example.myrestaurantv2kotlinserverapp.ui.mostpopular

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myrestaurantv2kotlinserverapp.callback.IMostPopularCallbackListener
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.model.MostPopularModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MostPopularViewModel : ViewModel(), IMostPopularCallbackListener {
    private var mostPopularListMutable: MutableLiveData<List<MostPopularModel>>? = null
    private var messageError: MutableLiveData<String> = MutableLiveData()

    var listener: IMostPopularCallbackListener = this

    fun getMostPopularList(): MutableLiveData<List<MostPopularModel>> {
        if (mostPopularListMutable == null) {
            mostPopularListMutable = MutableLiveData()
            loadMostPopulars()
        }

        return mostPopularListMutable!!
    }

    fun loadMostPopulars() {
        val tempList = ArrayList<MostPopularModel>()
        val mostPopularRef = FirebaseDatabase.getInstance()
            .getReference(Common.MOST_POPULAR)
        mostPopularRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for(item in snapshot.children){
                    val model = item.getValue<MostPopularModel>(MostPopularModel::class.java)
                    model!!.menu_id = item.key!!
                    tempList.add(model)
                }
                listener.onMostPopularListLoadSuccess(tempList)
            }

            override fun onCancelled(error: DatabaseError) {
                listener.onMostPopularListLoadFailed(error.message)
            }
        })

    }


    override fun onMostPopularListLoadSuccess(mostPopularList: List<MostPopularModel>) {
        mostPopularListMutable!!.value = mostPopularList
    }

    override fun onMostPopularListLoadFailed(message: String) {
        messageError.value = message
    }
}