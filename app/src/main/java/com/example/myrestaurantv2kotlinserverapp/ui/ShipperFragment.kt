package com.example.myrestaurantv2kotlinserverapp.ui

import android.app.AlertDialog
import android.os.Bundle
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
import com.example.myrestaurantv2kotlinserverapp.R
import com.example.myrestaurantv2kotlinserverapp.adapter.MyShipperAdapter
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.databinding.FragmentShipperBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.ChangeMenuClick
import com.example.myrestaurantv2kotlinserverapp.evenbus.UpdateShipperEvent
import com.google.firebase.database.FirebaseDatabase
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class ShipperFragment : Fragment() {
    private lateinit var viewModel: ShipperViewModel
    private lateinit var binding: FragmentShipperBinding
    private lateinit var dialog: AlertDialog
    private var adapter: MyShipperAdapter? = null
    private lateinit var layoutAnimation: LayoutAnimationController

    fun newInstance(): ShipperFragment {
        return ShipperFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentShipperBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ShipperViewModel::class.java)
        initViews()

        viewModel.getShipperList().observe(viewLifecycleOwner, Observer {
            dialog.dismiss()
            adapter = MyShipperAdapter(requireContext(), it)
            binding.recyclerShipper.adapter = adapter
            binding.recyclerShipper.layoutAnimation = layoutAnimation

        })

        return binding.root
    }

    private fun initViews() {
        dialog = SpotsDialog.Builder().setContext(context).setCancelable(false).build()
        layoutAnimation = AnimationUtils.loadLayoutAnimation(
            context,
            R.anim.layout_item_from_left
        )

        val layoutManager = LinearLayoutManager(context)
        binding.recyclerShipper.layoutManager = layoutManager
        binding.recyclerShipper.addItemDecoration(
            DividerItemDecoration(
                context,
                layoutManager.orientation
            )
        )
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onUpdateShipperActive(event: UpdateShipperEvent) {
        val updateData: MutableMap<String, Any> = HashMap()
        updateData["active"] = event.checked  // Get state of button, not of shipper
        FirebaseDatabase.getInstance()
            .getReference(Common.SHIPPER_REF)
            .child(event.shipper.key!!)
            .updateChildren(updateData)
            .addOnFailureListener { e: Exception ->
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener { aVoid: Void? ->
                Toast.makeText(requireContext(), "Update state to " + event.checked.toString() + " successfully!", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onStop() {
        if (EventBus.getDefault().hasSubscriberForEvent(UpdateShipperEvent::class.java))
            EventBus.getDefault().removeStickyEvent(UpdateShipperEvent::class.java)
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(ChangeMenuClick(true))
        super.onDestroy()
    }

}