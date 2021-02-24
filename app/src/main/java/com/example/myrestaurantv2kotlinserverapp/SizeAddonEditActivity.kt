package com.example.myrestaurantv2kotlinserverapp

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myrestaurantv2kotlinserverapp.adapter.MyAddonAdapter
import com.example.myrestaurantv2kotlinserverapp.adapter.MySizeAdapter
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.databinding.ActivitySizeAddonEditBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.AddonSizeEditEvent
import com.example.myrestaurantv2kotlinserverapp.evenbus.SelectAddonModel
import com.example.myrestaurantv2kotlinserverapp.evenbus.SelectSizeModel
import com.example.myrestaurantv2kotlinserverapp.model.AddonModel
import com.example.myrestaurantv2kotlinserverapp.model.SizeModel
import com.example.myrestaurantv2kotlinserverapp.model.UpdateAddonModel
import com.example.myrestaurantv2kotlinserverapp.model.UpdateSizeModel
import com.google.firebase.database.FirebaseDatabase
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class SizeAddonEditActivity : AppCompatActivity() {
    private var needSave: Boolean = false
    lateinit var binding: ActivitySizeAddonEditBinding
    private lateinit var sizeAdapter: MySizeAdapter
    private lateinit var addonAdapter: MyAddonAdapter
    private var foodEditPosition = -1
    private var isAddon: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySizeAddonEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() {
        setSupportActionBar(binding.toolBar)
        supportActionBar!!.title = "Edit/Add Size and Addon"

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        binding.recyclerAddonSize.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerAddonSize.layoutManager = layoutManager
        binding.recyclerAddonSize.addItemDecoration(
                DividerItemDecoration(
                        this,
                        layoutManager.orientation
                )
        )

        //Event when create new Size/Addon
        binding.btnCreate.setOnClickListener {
            if (!isAddon) { //Size
                if (sizeAdapter != null) {
                    var sizeModel = SizeModel(
                            binding.edtName.text.toString(),
                            binding.edtPrice.text.toString().toLong()
                    )
                    sizeAdapter!!.addNewSize(sizeModel)
                }

            } else { //Addon
                if (addonAdapter != null) {
                    var addonModel = AddonModel(
                            binding.edtName.text.toString(),
                            binding.edtPrice.text.toString().toLong()
                    )
                    addonAdapter!!.addNewAddon(addonModel)
                }

            }
        }

        //Event when click to edit SIze/Addon
        binding.btnEdit.setOnClickListener {
            if (!isAddon) { //Size
                if (sizeAdapter != null) {
                    val sizeModel = SizeModel(
                            binding.edtName.text.toString(),
                            binding.edtPrice.text.toString().toLong()
                    )
                    Log.d("EVENTBUS", "Before edit Adapter")
                    sizeAdapter.editSize(sizeModel)
                }
            } else { //Addon
                if (addonAdapter != null) {
                    val addonModel = AddonModel(
                            binding.edtName.text.toString(),
                            binding.edtPrice.text.toString().toLong()
                    )
                    Log.d("EVENTBUS", "Before edit Adapter")
                    addonAdapter.editAddon(addonModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.edtName.setText("")
        binding.edtPrice.setText("")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_size_addon, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> saveData()
            android.R.id.home -> {
                if (needSave) {
                    val builder = AlertDialog.Builder(this)
                            .setTitle("Cancel?")
                            .setMessage("Do you really want to close without saving?")
                            .setNegativeButton(
                                    "CANCEL"
                            ) { dialogInterface, _ -> dialogInterface.dismiss() }
                            .setPositiveButton("OK") { dialogInterface, i ->
                                needSave = false
                                closeActivity()
                            }
                    val dialog = builder.create()
                    dialog.show()
                } else {
                    closeActivity()
                }

            }
        }
        return true
    }

    private fun saveData() {
        if (foodEditPosition != -1) {
            Common.categorySelected!!.foods!![foodEditPosition] = Common.foodSelected!!
            var updateData: MutableMap<String, Any> = HashMap()
            updateData["foods"] = Common.categorySelected!!.foods!!

            FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REF)
                    .child(Common.categorySelected!!.menu_id!!)
                    .updateChildren(updateData)
                    .addOnFailureListener { e ->
                        Toast.makeText(
                                this@SizeAddonEditActivity,
                                e.message,
                                Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                    this@SizeAddonEditActivity,
                                    "Reload Success",
                                    Toast.LENGTH_SHORT
                            ).show()
                            needSave = false
                            binding.edtName.setText("")
                            binding.edtPrice.setText("0")
                        }
                    }
        }
    }

    private fun closeActivity() {
        binding.edtName.setText("")
        binding.edtPrice.setText("0")
        finish()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onAddonSizeReceive(event: AddonSizeEditEvent) {
        if (!event.addon) {
            if (Common.foodSelected!!.size != null) {
                sizeAdapter = MySizeAdapter(this, Common.foodSelected!!.size.toMutableList())
                foodEditPosition = event.pos //Save food edit to update
                binding.recyclerAddonSize.adapter = sizeAdapter
                isAddon = event.addon
            }
        }else{
            if (Common.foodSelected!!.addon != null) {
                addonAdapter = MyAddonAdapter(this, Common.foodSelected!!.addon.toMutableList())
                foodEditPosition = event.pos //Save food edit to update
                binding.recyclerAddonSize.adapter = addonAdapter
                isAddon = event.addon
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSizeModelUpdate(event: UpdateSizeModel) {
        if (event.sizeModelList != null) { //Size
            needSave = true
            Common.foodSelected!!.size = event.sizeModelList!! // Update
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSelectSizeEvent(event: SelectSizeModel) {
        if (event.sizeModel != null) { //Size
            binding.edtName.setText(event.sizeModel.name)
            binding.edtPrice.setText(event.sizeModel.price.toString())
            binding.btnEdit.isEnabled = true
        } else
            binding.btnEdit.isEnabled = true
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onAddonModelUpdate(event: UpdateAddonModel) {
        if (event.addonModelList != null) { //Addon
            needSave = true
            Log.d("TESTADDON", event.addonModelList.toString())
            Common.foodSelected!!.addon = event.addonModelList!! // Update
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSelectAddonEvent(event: SelectAddonModel) {
        if (event.addonModel != null) { //Addon
            binding.edtName.setText(event.addonModel.name)
            binding.edtPrice.setText(event.addonModel.price.toString())
            binding.btnEdit.isEnabled = true
        } else
            binding.btnEdit.isEnabled = true
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().removeStickyEvent(UpdateSizeModel::class.java)
        super.onStop()
    }
}