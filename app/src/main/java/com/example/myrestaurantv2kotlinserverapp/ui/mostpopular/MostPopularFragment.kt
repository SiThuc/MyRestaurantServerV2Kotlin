package com.example.myrestaurantv2kotlinserverapp.ui.mostpopular

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.myrestaurantv2kotlinserverapp.R
import com.example.myrestaurantv2kotlinserverapp.adapter.MyBestDealsAdapter
import com.example.myrestaurantv2kotlinserverapp.adapter.MyMostPopularAdapter
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.databinding.FragmentBestDealsBinding
import com.example.myrestaurantv2kotlinserverapp.databinding.FragmentMostPopularBinding
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutUpdateCategoryBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.*
import com.example.myrestaurantv2kotlinserverapp.model.BestDealsModel
import com.example.myrestaurantv2kotlinserverapp.ui.bestdeals.BestDealsViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.HashMap

class MostPopularFragment : Fragment() {
    private lateinit var viewModel: MostPopularViewModel
    private lateinit var binding: FragmentMostPopularBinding
    private lateinit var dialog: AlertDialog
    private var layoutAnimationController: LayoutAnimationController? = null
    private var adapter: MyMostPopularAdapter? = null

    private var imageUri: Uri? = null
    private var storage: FirebaseStorage = FirebaseStorage.getInstance()
    private var storageReference: StorageReference
    private lateinit var updateMostPopularBinding: LayoutUpdateCategoryBinding

    companion object {
        const val PICK_IMAGE_REQUEST: Int = 1234
    }

    init {
        storageReference = storage.reference
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentMostPopularBinding.inflate(inflater, container, false)
        initViews()
        viewModel = ViewModelProvider(this).get(MostPopularViewModel::class.java)

        viewModel.getMostPopularList().observe(viewLifecycleOwner, Observer {
            dialog.dismiss()

            adapter = MyMostPopularAdapter(requireContext(), it)
            binding.recyclerMostPopular.adapter = adapter
            binding.recyclerMostPopular.layoutAnimation = layoutAnimationController
        })

        return binding.root
    }

    private fun initViews() {
        dialog = SpotsDialog.Builder().setContext(context)
                .setCancelable(false)
                .build()

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)

        binding.recyclerMostPopular.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(context)
        binding.recyclerMostPopular.layoutManager = layoutManager
        binding.recyclerMostPopular.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))
    }

    //EventBus which listens when user click Update Button on MostPopular
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onUpdateMostPopular(event: UpdateMostPopularEvent) {
        if(event.isUpdate)
            showUpdateDialog()
        EventBus.getDefault().removeStickyEvent(event)
    }

    private fun showUpdateDialog() {
        var builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Update Best Deals")
        builder.setMessage("Please fill information")

        updateMostPopularBinding = LayoutUpdateCategoryBinding.inflate(LayoutInflater.from(context))

        //Set data
        updateMostPopularBinding.edtCategoryName.setText(Common.mostPopularSelected!!.name)
        Glide.with(requireContext()).load(Common.mostPopularSelected!!.image)
                .into(updateMostPopularBinding.imgCategory)

        //Set Events
        updateMostPopularBinding.imgCategory.setOnClickListener {
            var intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select picture"), PICK_IMAGE_REQUEST)
        }


        builder!!.setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
        builder!!.setPositiveButton("UPDATE") { dialogInterface, _ ->
            val updateData = HashMap<String, Any>()
            updateData!!["name"] = updateMostPopularBinding.edtCategoryName.text.toString()
            if (imageUri != null){
                dialog.setMessage("Uploading....")
                dialog.show()

                val imageName = UUID.randomUUID().toString()
                val imageFolder = storageReference.child("images/$imageName")
                imageFolder.putFile(imageUri!!)
                        .addOnFailureListener{e ->
                            dialog.dismiss()
                            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                        }
                        .addOnProgressListener { taskSnapshot ->
                            val progress = Math.round(100.0 * taskSnapshot.bytesTransferred/taskSnapshot.totalByteCount).toDouble()
                            dialog.setMessage("Uploaded $progress%")
                        }
                        .addOnSuccessListener { taskSnapshot ->
                            imageFolder.downloadUrl.addOnSuccessListener { uri ->
                                dialogInterface.dismiss()
                                dialog.dismiss()
                                updateData["image"] = uri.toString()
                                updateMostPopular(updateData)
                            }
                        }
            } else{
                updateMostPopular(updateData)
            }
        }

        builder!!.setView(updateMostPopularBinding.root)
        val updateDialog = builder!!.create()
        updateDialog.show()
    }

    private fun updateMostPopular(updateData: java.util.HashMap<String, Any>) {
        FirebaseDatabase.getInstance()
                .getReference(Common.MOST_POPULAR)
                .child(Common.mostPopularSelected!!.menu_id)
                .updateChildren(updateData)
                .addOnFailureListener { e -> Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show() }
                .addOnCompleteListener { task ->
                    viewModel.loadMostPopulars()
                    EventBus.getDefault().postSticky(ToastEvent(true, true))
                }
    }

    //EventBus which listens when user click Update Button on BestDeals
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onDeleteMostPopularEvent(event: DeleteMostPopularEvent) {
        if(event.isDelete)
            showDeleteDialog()
        EventBus.getDefault().removeStickyEvent(event)
    }

    private fun showDeleteDialog() {
        var builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Delete Most Popular")
        builder.setMessage("Do you want really to delete this Item")

        builder.setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
        builder!!.setPositiveButton("DELETE") { dialogInterface, _ -> deleteMostPopular()}

        val deleteDialog = builder.create()
        deleteDialog.show()

    }

    private fun deleteMostPopular() {
        FirebaseDatabase.getInstance()
                .getReference(Common.MOST_POPULAR)
                .child(Common.mostPopularSelected!!.menu_id)
                .removeValue()
                .addOnFailureListener { e -> Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show() }
                .addOnCompleteListener { task ->
                    viewModel.loadMostPopulars()
                    Common.mostPopularSelected = null
                    EventBus.getDefault().postSticky(ToastEvent(false, true))
                }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){
            if(data != null && data.data != null){
                imageUri = data.data
                updateMostPopularBinding.imgCategory.setImageURI(imageUri)
            }
        }
    }


    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().postSticky(MenuItemBack())
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)
        super.onStop()
    }

}