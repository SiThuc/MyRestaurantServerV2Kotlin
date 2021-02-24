package com.example.myrestaurantv2kotlinserverapp.ui.foodlist

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.myrestaurantv2kotlinserverapp.R
import com.example.myrestaurantv2kotlinserverapp.adapter.MyFoodListAdapter
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.databinding.FragmentFoodListBinding
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutUpdateFoodBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.MenuItemBack
import com.example.myrestaurantv2kotlinserverapp.evenbus.ShowDeleteDialog
import com.example.myrestaurantv2kotlinserverapp.evenbus.ShowUpdateDialog
import com.example.myrestaurantv2kotlinserverapp.model.FoodModel
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.ArrayList


class FoodListFragment : Fragment() {

    private val PICK_IMAGE_REQUEST: Int = 2406
    private lateinit var foodListViewModel: FoodListViewModel
    private var adapter: MyFoodListAdapter? = null
    private var layoutAnimationController: LayoutAnimationController? = null
    private lateinit var binding: FragmentFoodListBinding
    private lateinit var updateFoodBinding: LayoutUpdateFoodBinding
    private var imageUri: Uri? = null
    private lateinit var dialog: android.app.AlertDialog
    private var storage: FirebaseStorage = FirebaseStorage.getInstance()
    private var storageReference: StorageReference

    init {
        storageReference = storage.reference
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.food_list, menu)

        //Create search view
        val menuItem = menu.findItem(R.id.action_search)

        val searchManager = requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager

        val searchView = menuItem.actionView as androidx.appcompat.widget.SearchView

        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))

        //Event
        searchView.setOnQueryTextListener(object: androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                startSearchFood(query!!)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

        })

        //Clear text when click to Clear Button
        val closeButton = searchView.findViewById<View>(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener {
            val ed = searchView.findViewById<View>(R.id.search_src_text) as EditText
            //Clear Text
            ed.setText("")
            //Clear query
            searchView.setQuery("", false)
            //Collapse the action View
            searchView.onActionViewCollapsed()
            //Collapse the search view
            menuItem.collapseActionView()
            //Restore result to original
            foodListViewModel.foodListMutableLiveData!!.value = Common.categorySelected!!.foods

        }
    }

    private fun startSearchFood(s: String) {
        val resultFood: MutableList<FoodModel> = ArrayList()
        for(i in Common.categorySelected!!.foods!!.indices){
            val foodModel = Common.categorySelected!!.foods!![i]
            if(foodModel.name!!.toLowerCase().contains(s.toLowerCase())){

                //Here we will save index of search result item
                foodModel.positionInList = i
                resultFood.add(foodModel)
            }
        }

        //Update search result
        foodListViewModel.foodListMutableLiveData!!.value = resultFood

    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        foodListViewModel = ViewModelProvider(this).get(FoodListViewModel::class.java)
        binding = FragmentFoodListBinding.inflate(inflater, container, false)

        initView()

        foodListViewModel.getFoodList().observe(viewLifecycleOwner, Observer {
            adapter = MyFoodListAdapter(requireContext(), it)
            binding.recyclerFood.adapter = adapter
            binding.recyclerFood.layoutAnimation = layoutAnimationController
        })

        return binding.root
    }



    private fun initView() {
        setHasOptionsMenu(true) // Set option menu

        dialog = SpotsDialog.Builder().setContext(requireContext()).setCancelable(false).build()

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)
        binding.recyclerFood.setHasFixedSize(true)
        binding.recyclerFood.layoutManager = LinearLayoutManager(context)

        (activity as AppCompatActivity).supportActionBar!!.title = Common.categorySelected!!.name
    }

    //Listening the deleteFoodEvent from ModelView and show dialog
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onShowDeleteDialog(event: ShowDeleteDialog) {
        if (event != null) {
            val position = event.position
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("DELETE")
                    .setMessage("Do you want to delete this food?")
                    .setNegativeButton("CANCEL") { dialogInterface, which -> dialogInterface.dismiss() }
                    .setPositiveButton("DELETE") { dialogInterface, which ->
                        val foodModel = adapter!!.getItemAtPosition(position)
                        if(foodModel.positionInList == -1)
                            Common.categorySelected!!.foods!!.removeAt(position)
                        else
                            Common.categorySelected!!.foods!!.removeAt(foodModel.positionInList)
                        foodListViewModel.updateFoodListByDeleting(position)
                    }
            val deleteDialog = builder.create()
            deleteDialog.show()
            EventBus.getDefault().removeStickyEvent(event)
        }
    }


    //This function to listen when having a request to show Update Dialog
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onShowUpdateDialog(event: ShowUpdateDialog) {
        if (event != null) {
            val foodModel = adapter!!.getItemAtPosition(event.pos)
            if(foodModel.positionInList == -1)
                showUpdateDialog(event.pos, foodModel)
            else
                showUpdateDialog(foodModel.positionInList, foodModel)

            EventBus.getDefault().removeStickyEvent(event)
        }
    }

    private fun showUpdateDialog(pos: Int, foodModel: FoodModel) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Update")
        builder.setMessage("Please fill information")

        updateFoodBinding = LayoutUpdateFoodBinding.inflate(LayoutInflater.from(context))

        //Set Data
        var food = foodModel
        updateFoodBinding.edtFoodName.setText(food!!.name)
        updateFoodBinding.edtFoodPrice.setText(food!!.price.toString())
        updateFoodBinding.edtFoodDescription.setText(food.description)
        Glide.with(requireContext()).load(food.image).into(updateFoodBinding.imgFoodImage)

        //Set Event when user click on image to change the image of the food
        updateFoodBinding.imgFoodImage.setOnClickListener {
            var intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
        }


        builder.setNegativeButton("CANCEL") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }.setPositiveButton("UPDATE") { dialogInterface, _ ->
            food.name = updateFoodBinding.edtFoodName.text.toString()
            food.description = updateFoodBinding.edtFoodDescription.text.toString()
            if (TextUtils.isEmpty(updateFoodBinding.edtFoodPrice.text))
                food.price = 0
            else
                food.price = updateFoodBinding.edtFoodPrice.text.toString().toLong()

            if (imageUri != null) {
                dialog.setMessage("Uploading...")
                dialog.show()

                val uniqueName: String = UUID.randomUUID().toString()
                val imageFolder: StorageReference = storageReference.child("image/" + uniqueName)
                imageFolder.putFile(imageUri!!)
                        .addOnFailureListener { e ->
                            dialog.dismiss()
                            Toast.makeText(requireContext(), "Error here: " + e.message, Toast.LENGTH_SHORT).show()
                        }
                        .addOnCompleteListener { task ->
                            dialog.dismiss()
                            imageFolder.downloadUrl.addOnSuccessListener { uri ->
                                food.image = uri.toString()
                                Common.categorySelected!!.foods!![pos] = food
                                foodListViewModel.updateFoodListToFirebase(false, Common.categorySelected!!.foods!!)
                            }
                        }
                        .addOnProgressListener { taskSnapshot ->
                            var progress = (100 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                            dialog.setMessage(StringBuilder("Uploading: ").append(progress).append("%"))
                        }
                imageUri = null
            } else {
                Common.categorySelected!!.foods!![pos] = food
                foodListViewModel.updateFoodListToFirebase(false, Common.categorySelected!!.foods!!)
            }
        }

        builder.setView(updateFoodBinding.root)
        var updateDialog = builder.create()
        updateDialog.show()

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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                imageUri = data.data!!
                updateFoodBinding.imgFoodImage.setImageURI(imageUri)
            }
        }
    }
}
