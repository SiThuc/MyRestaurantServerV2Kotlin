package com.example.myrestaurantv2kotlinserverapp.ui.bestdeals

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myrestaurantv2kotlinserverapp.callback.IBestDealsCallbackListener
import com.example.myrestaurantv2kotlinserverapp.callback.ICategoryCallBackListener
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.model.BestDealModel
import com.example.myrestaurantv2kotlinserverapp.model.CategoryModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BestDealsViewModel : ViewModel(), IBestDealsCallbackListener {
    private var bestDealsListMutable: MutableLiveData<List<BestDealModel>>? = null
    private var messageError: MutableLiveData<String> = MutableLiveData()

    var listener: IBestDealsCallbackListener = this

    fun getBestDealsList(): MutableLiveData<List<BestDealModel>> {
        if (bestDealsListMutable == null) {
            bestDealsListMutable = MutableLiveData()
            loadBestDeals()
        }

        return bestDealsListMutable!!
    }

    private fun loadBestDeals() {
        val tempList = ArrayList<BestDealModel>()
        val bestDealRef = FirebaseDatabase.getInstance().getReference(Common.BEST_DEALS)
        bestDealRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for(item in snapshot.children){
                    val model = item.getValue<BestDealModel>(BestDealModel::class.java)
                    model!!.menu_id = item.key!!
                    tempList.add(model)
                }
                listener.onBestDealsListLoadSuccess(tempList)
            }

            override fun onCancelled(error: DatabaseError) {
                listener.onBestDealsListLoadFailed(error.message)
            }
        })

    }


    override fun onBestDealsListLoadSuccess(bestDealsList: List<BestDealModel>) {
        bestDealsListMutable!!.value = bestDealsList
    }

    override fun onBestDealsListLoadFailed(message: String) {
        messageError.value = message
    }
}