package com.example.myrestaurantv2kotlinserverapp.ui.shipper

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
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
import com.example.myrestaurantv2kotlinserverapp.model.FoodModel
import com.example.myrestaurantv2kotlinserverapp.model.ShipperModel
import com.example.myrestaurantv2kotlinserverapp.ui.dialogs.BottomSheetOrderFragment
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

    private var shipperList: ArrayList<ShipperModel>? = null

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
            shipperList = (it as ArrayList<ShipperModel>?)!!
            adapter = MyShipperAdapter(requireContext(), shipperList!!)
            binding.recyclerShipper.adapter = adapter
            binding.recyclerShipper.layoutAnimation = layoutAnimation

        })

        return binding.root
    }

    private fun initViews() {
        setHasOptionsMenu(true)
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
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                }
                .addOnSuccessListener { _ ->
                    Toast.makeText(requireContext(), "Update state to " + event.checked.toString() + " successfully!", Toast.LENGTH_SHORT).show()
                }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.shipper_list, menu)

        //Create search view
        val menuItem = menu.findItem(R.id.action_search_shipper)
        val searchManager = requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menuItem.actionView as androidx.appcompat.widget.SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))

        //Event
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                startSearchShipper(query!!)
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
            viewModel.loadShipperList()

        }
    }

    private fun startSearchShipper(query: String) {
        val result: MutableList<ShipperModel> = ArrayList()
        for (shipper in shipperList!!) {
            if (shipper.phone!!.toLowerCase().contains(query.toLowerCase())) {
                result.add(shipper)
            }
        }

        //Update search result
        viewModel.getShipperList()!!.value = result

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