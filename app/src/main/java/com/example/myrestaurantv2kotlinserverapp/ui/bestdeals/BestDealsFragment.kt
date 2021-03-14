package com.example.myrestaurantv2kotlinserverapp.ui.bestdeals

import android.app.AlertDialog
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
import com.example.myrestaurantv2kotlinserverapp.R
import com.example.myrestaurantv2kotlinserverapp.adapter.MyBestDealsAdapter
import com.example.myrestaurantv2kotlinserverapp.adapter.MyCategoryAdapter
import com.example.myrestaurantv2kotlinserverapp.databinding.FragmentBestDealsBinding
import com.example.myrestaurantv2kotlinserverapp.databinding.FragmentCategoryBinding
import com.example.myrestaurantv2kotlinserverapp.ui.category.CategoryViewModel
import dmax.dialog.SpotsDialog

class BestDealsFragment : Fragment() {
    private lateinit var viewModel: BestDealsViewModel

    private lateinit var binding: FragmentBestDealsBinding
    private lateinit var dialog: AlertDialog
    private var layoutAnimationController: LayoutAnimationController? = null
    private var adapter: MyBestDealsAdapter? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentBestDealsBinding.inflate(inflater, container, false)
        initView(binding)

        viewModel = ViewModelProvider(this).get(BestDealsViewModel::class.java)

        viewModel.getBestDealsList().observe(viewLifecycleOwner, Observer {
            dialog.dismiss()
            adapter = MyBestDealsAdapter(requireContext(), it)
            binding.recyclerBestDeals.adapter = adapter
            binding.recyclerBestDeals.layoutAnimation = layoutAnimationController
        })
        return binding.root
    }

    private fun initView(binding: FragmentBestDealsBinding) {
        dialog = SpotsDialog.Builder().setContext(context)
                .setCancelable(false)
                .build()

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)

        binding.recyclerBestDeals.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(context)
        binding.recyclerBestDeals.layoutManager = layoutManager
        binding.recyclerBestDeals.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))

    }


}