package com.example.myrestaurantv2kotlinserverapp.ui.category

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myrestaurantv2kotlinserverapp.callback.ICategoryCallBackListener
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.evenbus.ErrorEvent
import com.example.myrestaurantv2kotlinserverapp.evenbus.SuccessUpdateCategoryEvent
import com.example.myrestaurantv2kotlinserverapp.model.CategoryModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.greenrobot.eventbus.EventBus

class CategoryViewModel : ViewModel(), ICategoryCallBackListener {
    private var categoryListMutable: MutableLiveData<List<CategoryModel>>? = null
    private var messageError: MutableLiveData<String> = MutableLiveData()

    var categoryCallBackListener: ICategoryCallBackListener = this


    fun getCategoryList(): LiveData<List<CategoryModel>> {
        if (categoryListMutable == null) {
            categoryListMutable = MutableLiveData()
            messageError = MutableLiveData()
            loadCategoryList()
        }
        return categoryListMutable!!
    }

    fun getMessageError(): MutableLiveData<String> {
        return messageError!!
    }

    fun loadCategoryList() {
        var tempList = ArrayList<CategoryModel>()

        var categoryRef = FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REF)

        categoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (item in snapshot.children) {
                    val model = item.getValue(CategoryModel::class.java)
                    model!!.menu_id = item.key
                    tempList.add(model)
                }
                categoryCallBackListener.onCategoryLoadSuccess(tempList)
            }

            override fun onCancelled(error: DatabaseError) {
                categoryCallBackListener.onCategoryLoadFailed(error.message)
            }
        })
    }

    fun updateCategory(updateData: Map<String, Any>) {
        FirebaseDatabase.getInstance()
                .getReference(Common.CATEGORY_REF)
                .child(Common.categorySelected!!.menu_id!!)
                .updateChildren(updateData)
                .addOnFailureListener { e ->
                    EventBus.getDefault().postSticky(ErrorEvent(e))
                }.addOnCompleteListener {
                    loadCategoryList()
                    EventBus.getDefault().postSticky(SuccessUpdateCategoryEvent(true))
                }
    }

    override fun onCategoryLoadSuccess(categoryList: List<CategoryModel>) {
        categoryListMutable!!.value = categoryList
    }

    override fun onCategoryLoadFailed(message: String) {
        messageError!!.value = message
    }

}