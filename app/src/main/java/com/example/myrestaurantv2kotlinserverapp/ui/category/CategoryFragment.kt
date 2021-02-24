package com.example.myrestaurantv2kotlinserverapp.ui.category

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.myrestaurantv2kotlinserverapp.R
import com.example.myrestaurantv2kotlinserverapp.adapter.MyCategoryAdapter
import com.example.myrestaurantv2kotlinserverapp.databinding.FragmentCategoryBinding
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutUpdateCategoryBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.LoadCategory
import com.example.myrestaurantv2kotlinserverapp.evenbus.MenuItemBack
import com.example.myrestaurantv2kotlinserverapp.evenbus.ShowUpdateCategoryDialog
import com.example.myrestaurantv2kotlinserverapp.model.CategoryModel
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.HashMap

class CategoryFragment : Fragment() {
    private lateinit var viewModel: CategoryViewModel
    private lateinit var binding: FragmentCategoryBinding
    private lateinit var updateCategoryBinding: LayoutUpdateCategoryBinding
    private lateinit var dialog: AlertDialog
    private var layoutAnimationController: LayoutAnimationController? = null
    private var adapter: MyCategoryAdapter? = null
    private var imageUri: Uri? = null
    private var storage: FirebaseStorage = FirebaseStorage.getInstance()
    private var storageReference: StorageReference
    private var updateData: HashMap<String, Any>? = null

    init {
        storageReference = storage.reference
    }


    companion object {
        const val PICK_IMAGE_REQUEST: Int = 1234
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        binding = FragmentCategoryBinding.inflate(inflater, container, false)
        initView(binding)

        return binding.root
    }

    private fun initView(binding: FragmentCategoryBinding) {

        dialog = SpotsDialog.Builder().setContext(context)
                .setCancelable(false)
                .build()

        layoutAnimationController =
                AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)

        binding.recyclerMenu.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(context)
        binding.recyclerMenu.layoutManager = layoutManager
        binding.recyclerMenu.addItemDecoration(
                DividerItemDecoration(
                        context,
                        layoutManager.orientation
                )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(CategoryViewModel::class.java)

        viewModel.getMessageError().observe(viewLifecycleOwner, Observer {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        })

        viewModel.getCategoryList().observe(viewLifecycleOwner, Observer {
            dialog.dismiss()
            adapter = MyCategoryAdapter(requireContext(), it)
            binding.recyclerMenu.adapter = adapter
            binding.recyclerMenu.layoutAnimation = layoutAnimationController
        })
    }


    //Listen for Show Update Category Dialog
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onShowUpdateCategoryDialog(event: ShowUpdateCategoryDialog) {
        if (event.category != null) {
            val category = event.category
            showDialogUpdate(category)
            EventBus.getDefault().removeStickyEvent(event)
        }
    }

    //EventBus which listens when user click on the Cart Icon in FoodListFragment
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCategoryLoad(event: LoadCategory) {
        if (event.isLoad) {
            viewModel.loadCategoryList()
        }
    }

    private fun showDialogUpdate(category: CategoryModel) {
        var builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Update Category")
        builder.setMessage("Please fill information")

        updateCategoryBinding = LayoutUpdateCategoryBinding.inflate(LayoutInflater.from(context))

        //Set data
        updateCategoryBinding.edtCategoryName.setText(category.name)
        Glide.with(requireContext()).load(category.image)
                .into(updateCategoryBinding.imgCategory)

        //Set Events
        updateCategoryBinding.imgCategory.setOnClickListener {
            var intent = Intent()
            intent.setType("image/*")
            intent.setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent, "Select picture"), PICK_IMAGE_REQUEST)
        }

        builder!!.setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
        builder!!.setPositiveButton("UPDATE") { dialogInterface, _ ->
            updateData = HashMap<String, Any>()
            updateData!!["name"] = updateCategoryBinding.edtCategoryName.text.toString()
            updateData!!["foods"] = category.foods!!

            if (imageUri != null)
                uploadImageAndUpdateDataToStorage()
            else{
                updateData!!["image"] = category.image!!
                viewModel.updateCategory(updateData!!)
            }
        }

        builder!!.setView(updateCategoryBinding.root)
        val updateDialog = builder!!.create()
        updateDialog.show()
    }

    private fun uploadImageAndUpdateDataToStorage() {
        dialog.setMessage("Uploading....")
        dialog.show()

        val uniqueName = UUID.randomUUID().toString()
        val imageFolder = storageReference.child("image/" + uniqueName)

        imageFolder.putFile(imageUri!!)
                .addOnFailureListener { e ->
                    dialog.dismiss()
                    Toast.makeText(context, "Error here: " + e.message, Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener { task ->
                    dialog.dismiss()
                    imageFolder.downloadUrl.addOnSuccessListener { uri ->
                        updateData!!["image"] = uri.toString()
                        viewModel.updateCategory(updateData!!)
                    }
                }
                .addOnProgressListener { taskSnapShot ->
                    var progress = (100 * taskSnapShot.bytesTransferred / taskSnapShot.totalByteCount)
                    dialog.setMessage(StringBuilder("Uploading: ").append(progress).append("%"))
                }
    }

    //This function process when the Intent send back a Image Uri
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.data != null) {
                imageUri = data.data!!
                updateCategoryBinding.imgCategory.setImageURI(imageUri)
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