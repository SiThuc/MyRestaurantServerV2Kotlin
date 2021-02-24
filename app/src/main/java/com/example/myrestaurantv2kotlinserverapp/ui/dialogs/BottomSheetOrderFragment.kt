package com.example.myrestaurantv2kotlinserverapp.ui.dialogs

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myrestaurantv2kotlinserverapp.R
import com.example.myrestaurantv2kotlinserverapp.databinding.FragmentBottomSheetOrderBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.LoadOrderEvent
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.greenrobot.eventbus.EventBus

class BottomSheetOrderFragment : BottomSheetDialogFragment() {

    lateinit var binding: FragmentBottomSheetOrderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentBottomSheetOrderBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    private fun initViews() {
        binding.placedFilter.setOnClickListener {
            EventBus.getDefault().postSticky(LoadOrderEvent(0))
            dismiss()
        }
        binding.shippingFilter.setOnClickListener {
            EventBus.getDefault().postSticky(LoadOrderEvent(1))
            dismiss()
        }
        binding.shippedFilter.setOnClickListener {
            EventBus.getDefault().postSticky(LoadOrderEvent(2))
            dismiss()
        }
        binding.cancelledFilter.setOnClickListener {
            EventBus.getDefault().postSticky(LoadOrderEvent(-1))
            dismiss()
        }
    }

    companion object {
        val instance: BottomSheetOrderFragment? = null
            get() = field ?: BottomSheetOrderFragment()
    }
}