package com.example.myrestaurantv2kotlinserverapp.ui.foodlist

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myrestaurantv2kotlinserverapp.callback.IFoodListCallBackListener
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.evenbus.*
import com.example.myrestaurantv2kotlinserverapp.model.FoodModel
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.greenrobot.eventbus.EventBus


class FoodListViewModel : ViewModel(), IFoodListCallBackListener {
    var foodListMutableLiveData: MutableLiveData<List<FoodModel>>? = null
    private var messageError: MutableLiveData<String> = MutableLiveData()

    var foodListCallBackListener: IFoodListCallBackListener = this


    fun getFoodList(): MutableLiveData<List<FoodModel>> {
        if (foodListMutableLiveData == null) {
            foodListMutableLiveData = MutableLiveData()
            messageError = MutableLiveData()
            loadFoodList()
        }
        return foodListMutableLiveData!!
    }

    private fun loadFoodList() {
        val foodListRef = FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REF)
                .child(Common.categorySelected!!.menu_id!!)
                .child("foods")

        var tempList = ArrayList<FoodModel>()

        foodListRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (item in snapshot.children) {
                    val model = item.getValue(FoodModel::class.java)
                    model!!.key = item.key
                    tempList.add(model)
                }
                foodListCallBackListener.onFoodListLoadSuccess(tempList)
            }

            override fun onCancelled(error: DatabaseError) {
                foodListCallBackListener.onFoodListLoadFailed(error.message)
            }
        })
    }

    //Listen Event when user confirm deleting this food
    fun updateFoodListByDeleting(position: Int) {
        Common.categorySelected!!.foods!!.removeAt(position)
        updateFoodListToFirebase(true, Common.categorySelected!!.foods!!)
    }

    fun updateFoodListToFirebase(isDelete: Boolean, foodList: List<FoodModel>) {
        val updateData = HashMap<String, Any>()
        updateData["foods"] = foodList

        FirebaseDatabase.getInstance()
                .getReference(Common.CATEGORY_REF)
                .child(Common.categorySelected!!.menu_id!!)
                .updateChildren(updateData)
                .addOnFailureListener { e: Exception ->
                    EventBus.getDefault().postSticky(ErrorEvent(e))
                }
                .addOnCompleteListener { task: Task<Void?> ->
                    if (task.isSuccessful) {
                        reloadFoodList()
                        if (isDelete)
                            EventBus.getDefault().postSticky(SuccessEvent(true, true))
                        else
                            EventBus.getDefault().postSticky(SuccessEvent(true, false))
                    }
                }
    }

    private fun reloadFoodList() {
        loadFoodList()
    }

    override fun onFoodListLoadSuccess(foodList: List<FoodModel>) {
        foodListMutableLiveData!!.value = foodList
    }

    override fun onFoodListLoadFailed(message: String) {
        messageError.value = message
    }

}