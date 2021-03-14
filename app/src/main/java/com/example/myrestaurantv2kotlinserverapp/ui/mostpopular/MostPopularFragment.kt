package com.example.myrestaurantv2kotlinserverapp.ui.mostpopular

import android.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myrestaurantv2kotlinserverapp.R
import com.example.myrestaurantv2kotlinserverapp.adapter.MyBestDealsAdapter
import com.example.myrestaurantv2kotlinserverapp.adapter.MyMostPopularAdapter
import com.example.myrestaurantv2kotlinserverapp.databinding.FragmentBestDealsBinding
import com.example.myrestaurantv2kotlinserverapp.databinding.FragmentMostPopularBinding
import com.example.myrestaurantv2kotlinserverapp.model.BestDealsModel
import com.example.myrestaurantv2kotlinserverapp.ui.bestdeals.BestDealsViewModel
import dmax.dialog.SpotsDialog

class MostPopularFragment : Fragment() {
    private lateinit var viewModel: MostPopularViewModel
    private lateinit var binding: FragmentMostPopularBinding
    private lateinit var dialog: AlertDialog
    private var layoutAnimationController: LayoutAnimationController? = null
    private var adapter: MyMostPopularAdapter? = null



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

}