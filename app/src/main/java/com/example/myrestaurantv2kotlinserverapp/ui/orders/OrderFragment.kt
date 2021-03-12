package com.example.myrestaurantv2kotlinserverapp.ui.orders

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.session.MediaSession
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myrestaurantv2kotlinserverapp.R
import com.example.myrestaurantv2kotlinserverapp.adapter.MyOrderAdapter
import com.example.myrestaurantv2kotlinserverapp.adapter.MyShipperSelectedAdapter
import com.example.myrestaurantv2kotlinserverapp.callback.IShipperLoadCallbackListener
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.databinding.FragmentOrderBinding
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutDialogCancelledBinding
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutDialogShippedBinding
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutDialogShippingBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.*
import com.example.myrestaurantv2kotlinserverapp.model.*
import com.example.myrestaurantv2kotlinserverapp.services.IFCMService
import com.example.myrestaurantv2kotlinserverapp.services.RetrofitFCMClient
import com.example.myrestaurantv2kotlinserverapp.ui.dialogs.BottomSheetOrderFragment
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dmax.dialog.SpotsDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class OrderFragment : Fragment(), IShipperLoadCallbackListener {

    private val compositeDisposable = CompositeDisposable()
    lateinit var ifcmService: IFCMService

    private lateinit var orderViewModel: OrderViewModel
    private lateinit var binding: FragmentOrderBinding
    lateinit var layoutAnimationController: LayoutAnimationController
    lateinit var orderAdapter: MyOrderAdapter
    lateinit var dialogLayoutBinding: LayoutDialogShippingBinding

    lateinit var shipperSelectedAdapter: MyShipperSelectedAdapter
    lateinit var shipperLoadCallbackListener: IShipperLoadCallbackListener

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        orderViewModel =
                ViewModelProvider(this).get(OrderViewModel::class.java)
        binding = FragmentOrderBinding.inflate(inflater, container, false)
        initViews()

        orderViewModel.getOrderList().observe(viewLifecycleOwner, Observer {
            orderAdapter = MyOrderAdapter(requireContext(), it.toMutableList())
            binding.recyclerOrder.adapter = orderAdapter
            binding.recyclerOrder.layoutAnimation = layoutAnimationController

            updateTextCounter()
            //binding.txtOrderFilter.text = StringBuilder("Order (").append(it.size).append(")")
        })

        orderViewModel.messageError.observe(viewLifecycleOwner, Observer {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        })

        return binding.root
    }

    private fun initViews() {
        setHasOptionsMenu(true)
        shipperLoadCallbackListener = this
        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService::class.java)

        binding.recyclerOrder.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        binding.recyclerOrder.layoutManager = layoutManager
        binding.recyclerOrder.addItemDecoration(
                DividerItemDecoration(
                        context,
                        layoutManager.orientation
                )
        )
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.order_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_filter) {
            val bottomSheet = BottomSheetOrderFragment.instance
            bottomSheet!!.show(requireActivity().supportFragmentManager, "OrderList")
            return true
        } else
            return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onStop() {
        if (EventBus.getDefault().hasSubscriberForEvent(LoadOrderEvent::class.java))
            EventBus.getDefault().removeStickyEvent(LoadOrderEvent::class.java)

        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(ChangeMenuClick(true))
        super.onDestroy()
    }

    //Listen for event when user clicks on Filter Menu
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onLoadOrder(event: LoadOrderEvent) {
        orderViewModel.loadOrder(event.status)
    }

    //Listen for event when user click on Call Customer button
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCallCustomerEvent(event: CallCustomerEvent) {
        if (event != null) {
            showCallingDialog(event.orderModel)
        }
        EventBus.getDefault().removeStickyEvent(event)
    }

    private fun showCallingDialog(orderModel: OrderModel) {
        Dexter.withContext(requireActivity())
                .withPermission(android.Manifest.permission.CALL_PHONE)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                        val intent = Intent()
                        intent.action = Intent.ACTION_DIAL
                        intent.data = Uri.parse(StringBuilder("tel:").append(orderModel.userPhone).toString())
                        startActivity(intent)

                    }

                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                        Toast.makeText(requireContext(), "You must accept " + p0!!.permissionName, Toast.LENGTH_SHORT).show()
                    }

                    override fun onPermissionRationaleShouldBeShown(
                            p0: PermissionRequest?,
                            p1: PermissionToken?
                    ) {
                    }
                }).check()
    }

    //Listen for event when user click on Delete Order
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onDeleteOrderEvent(event: DeleteOrderEvent) {
        if (event != null) {
            showDeleteAlertDialog(event.order)
        }
        EventBus.getDefault().removeStickyEvent(event)
    }

    private fun showDeleteAlertDialog(order: OrderModel) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("DELETE")
                .setMessage("Do you want to delete this Order?")
                .setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
                .setPositiveButton("DELETE") { dialogInterface, _ ->
                    FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
                            .child(order.key!!)
                            .removeValue()
                            .addOnFailureListener {
                                Toast.makeText(context, it.message.toString(), Toast.LENGTH_SHORT).show()
                            }
                            .addOnSuccessListener {
                                orderAdapter.removeItem(order)
                                orderAdapter.notifyDataSetChanged()
                                updateTextCounter()
                                //binding.txtOrderFilter.text = StringBuilder("Order (").append(orderAdapter.itemCount).append(")")
                                dialogInterface.dismiss()
                                Toast.makeText(context, "Order is deleted successful!", Toast.LENGTH_SHORT).show()
                            }


                }
        val deleteDialog = builder.create()
        deleteDialog.show()

        val btnNegative = deleteDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        btnNegative.setTextColor(Color.LTGRAY)
        val btnPositive = deleteDialog.getButton(DialogInterface.BUTTON_POSITIVE)
        btnPositive.setTextColor(Color.RED)
    }

    //Listen for event when user click on Update Order
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onUpdateOrderEvent(event: UpdateOrderEvent) {
        showUpdateDialog(event.order, event.position)
        EventBus.getDefault().removeStickyEvent(event)
    }

    private fun showUpdateDialog(order: OrderModel, position: Int) {

        var builder: AlertDialog.Builder? = null

        if (order.orderStatus == -1) {
            var dialogLayoutBinding = LayoutDialogCancelledBinding.inflate(layoutInflater)
            builder = AlertDialog.Builder(requireContext()).setView(dialogLayoutBinding.root)
            showCancelledDialog(order, position, dialogLayoutBinding, builder)
        } else if (order.orderStatus == 0) {
            dialogLayoutBinding = LayoutDialogShippingBinding.inflate(layoutInflater)
            loadShipperList()
            builder = AlertDialog.Builder(requireContext(), android.R.style.Theme_Material_Light_NoActionBar).setView(dialogLayoutBinding.root)
            showShippingDialog(order, position, dialogLayoutBinding, builder)

        } else {
            var dialogLayoutBinding = LayoutDialogShippedBinding.inflate(layoutInflater)
            builder = AlertDialog.Builder(requireContext()).setView(dialogLayoutBinding.root)
            showShippedDialog(order, position, dialogLayoutBinding, builder)

        }
    }

    private fun loadShipperList() {
        val tempList = ArrayList<ShipperModel>()
        val shipperRef = FirebaseDatabase.getInstance().getReference(Common.SHIPPER_REF)
        val shipperActive = shipperRef.orderByChild("active").equalTo(true) // Load only shipper active by Server app

        shipperActive.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (shipperSnapshot in snapshot.children) {
                    val shipperModel = shipperSnapshot.getValue(ShipperModel::class.java)
                    shipperModel!!.key = shipperSnapshot.key
                    tempList.add(shipperModel)
                }
                shipperLoadCallbackListener.onShipperLoadSuccess(tempList)
            }

            override fun onCancelled(error: DatabaseError) {
                shipperLoadCallbackListener.onShipperLoadFailed(error.message)
            }
        })
    }

    private fun showCancelledDialog(order: OrderModel, position: Int, dialogLayoutBinding: LayoutDialogCancelledBinding, builder: AlertDialog.Builder?) {
        dialogLayoutBinding.txtStatus.text = StringBuilder("Order Status(")
                .append(Common.convertStatusToString(order.orderStatus))
                .append(")")

        val dialog = builder!!.create()
        dialog.show()

        //Custom Dialog
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setGravity(Gravity.CENTER)
        dialogLayoutBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialogLayoutBinding.btnOk.setOnClickListener {
            if (dialogLayoutBinding.rdiDelete.isChecked) {
                deleteOrder(position, order)
                dialog.dismiss()
            } else if (dialogLayoutBinding.rdiRestorePlaced.isChecked) {
                updateOrder(position, order, 0)
                dialog.dismiss()
            }
        }

    }

    private fun showShippingDialog(order: OrderModel, position: Int, dialogLayoutBinding: LayoutDialogShippingBinding, builder: AlertDialog.Builder?) {
        dialogLayoutBinding.txtStatus.text = StringBuilder("Order Status(")
                .append(Common.convertStatusToString(order.orderStatus))
                .append(")")
        val dialog = builder!!.create()
        dialog.show()

        //Custom Dialog
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setGravity(Gravity.CENTER)
        dialogLayoutBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialogLayoutBinding.btnOk.setOnClickListener {
            if (dialogLayoutBinding.rdiCancelled.isChecked) {
                updateOrder(position, order, -1)
                dialog.dismiss()
            } else if (dialogLayoutBinding.rdiShipping.isChecked) {
                if (shipperSelectedAdapter.getSelectedShipper() == null)
                    Toast.makeText(requireContext(), "Please choose Shipper to continue!", Toast.LENGTH_SHORT).show()
                else {
                    updateOrder(position, order, 1)
                    val shipperModel = shipperSelectedAdapter.getSelectedShipper()
                    createShipperOrder(position, shipperModel, order, dialog, 1)
                    dialog.dismiss()
                }
            }
        }
    }

    private fun createShipperOrder(position: Int, shipperModel: ShipperModel?, order: OrderModel, dialog: AlertDialog, newStatus:Int) {

        val shippingOrder = ShipperOrderModel()
        shippingOrder.shipperPhone = shipperModel!!.phone
        shippingOrder.shipperName = shipperModel!!.name
        shippingOrder.orderModel = order
        shippingOrder.orderModel!!.orderStatus = newStatus
        shippingOrder.isStartTrip = false
        shippingOrder.currentLat = -1.0
        shippingOrder.currentLng = -1.0

        FirebaseDatabase.getInstance()
                .getReference(Common.SHIPPING_ORDER_REF)
                .child(order.key!!)  //Change push() to key()
                .setValue(shippingOrder)
                .addOnFailureListener { e: java.lang.Exception ->
                    dialog.dismiss()
                    Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener { task: Task<Void?> ->
                    if (task.isSuccessful) {
                        dialog.dismiss()

                        //Load token
                        FirebaseDatabase.getInstance()
                                .getReference(Common.TOKEN_REF)
                                .child(shipperModel.key!!)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            val tokenModel = snapshot.getValue(TokenModel::class.java)
                                            val notiData = HashMap<String, String>()
                                            notiData.put(Common.NOTI_TITLE, "You have a new Order to ship")
                                            notiData.put(Common.NOTI_CONTENT, StringBuilder("Order to :").append(order.userPhone).toString())

                                            val sendData = FCMSendData(tokenModel!!.token!!, notiData)
                                            compositeDisposable.add(
                                                    ifcmService.sendNotification(sendData)
                                                            .subscribeOn(Schedulers.io())
                                                            .observeOn(AndroidSchedulers.mainThread())
                                                            .subscribe({ fcmResponse ->
                                                                dialog.dismiss()
                                                                if (fcmResponse.success == 1) {
                                                                    updateOrder(position, order, 1)
                                                                } else
                                                                    Toast.makeText(requireContext(), "Failed to send Notification!Order wasn't updated", Toast.LENGTH_SHORT).show()
                                                            })
                                            )
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        dialog.dismiss()
                                        Toast.makeText(requireContext(), error.message.toString(), Toast.LENGTH_SHORT).show()
                                    }
                                })

                    }
                }
    }

    private fun showShippedDialog(order: OrderModel, position: Int, dialogLayoutBinding: LayoutDialogShippedBinding, builder: AlertDialog.Builder?) {
        dialogLayoutBinding.txtStatus.text = StringBuilder("Order Status(")
                .append(Common.convertStatusToString(order.orderStatus))
                .append(")")
        val dialog = builder!!.create()
        dialog.show()

        //Custom Dialog
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setGravity(Gravity.CENTER)
        dialogLayoutBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialogLayoutBinding.btnOk.setOnClickListener {
            if (dialogLayoutBinding.rdiCancelled.isChecked) {
                updateOrder(position, order, -1)
                dialog.dismiss()
            } else if (dialogLayoutBinding.rdiShipped.isChecked) {
                updateOrder(position, order, 2)
                dialog.dismiss()
            }
        }
    }

    private fun deleteOrder(position: Int, order: OrderModel) {
        if (!TextUtils.isEmpty(order.key)) {
            FirebaseDatabase.getInstance()
                    .getReference(Common.ORDER_REF)
                    .child(order.key!!)
                    .removeValue()
                    .addOnFailureListener { e: java.lang.Exception -> Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show() }
                    .addOnSuccessListener { aVoid: Void? ->
                        orderAdapter.removeItem(order)
                        orderAdapter.notifyItemRemoved(position)
                        updateTextCounter()
                        Toast.makeText(context, "Delete order success", Toast.LENGTH_SHORT).show()
                    }
        } else {
            Toast.makeText(context, "Order number must not be null or empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateOrder(position: Int, order: OrderModel, newStatus: Int) {
        if (!TextUtils.isEmpty(order.key)) {
            val updateData: MutableMap<String, Any> = HashMap()
            updateData["orderStatus"] = newStatus
            FirebaseDatabase.getInstance()
                    .getReference(Common.ORDER_REF)
                    .child(order.key!!)
                    .updateChildren(updateData)
                    .addOnFailureListener { e: Exception -> Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show() }
                    .addOnSuccessListener { aVoid: Void? ->

                        val spDialog = SpotsDialog.Builder().setContext(context).setCancelable(false).build()
                        spDialog.show()

                        //Load token
                        FirebaseDatabase.getInstance()
                                .getReference(Common.TOKEN_REF)
                                .child(order.userId!!)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            val tokenModel = snapshot.getValue(TokenModel::class.java)
                                            val notiData = HashMap<String, String>()
                                            notiData[Common.NOTI_TITLE] = "Your order was updated"
                                            notiData[Common.NOTI_CONTENT] = StringBuilder("Your order")
                                                    .append(order.key!!)
                                                    .append("was updated to")
                                                    .append(Common.convertStatusToString(newStatus)).toString()
                                            val sendData = FCMSendData(tokenModel!!.token!!, notiData)

                                            compositeDisposable.add(
                                                    ifcmService.sendNotification(sendData)
                                                            .subscribeOn(Schedulers.io())
                                                            .observeOn(AndroidSchedulers.mainThread())
                                                            .subscribe({ fcmResponse ->
                                                                spDialog.dismiss()
                                                                if (fcmResponse.success == 1) {

                                                                    Toast.makeText(requireContext(), "Update order successfully", Toast.LENGTH_SHORT).show()

                                                                } else
                                                                    Toast.makeText(requireContext(), "Failed to sen notification", Toast.LENGTH_SHORT).show()
                                                            }, { t ->
                                                                spDialog.dismiss()
                                                                Toast.makeText(requireContext(), t.message, Toast.LENGTH_SHORT).show()
                                                            })
                                            )

                                        } else {
                                            spDialog.dismiss()
                                            Toast.makeText(requireContext(), "Token not found", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        spDialog.dismiss()
                                        Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
                                    }
                                })

                        orderAdapter.removeItem(order)
                        orderAdapter.notifyItemRemoved(position)
                        updateTextCounter()
                    }
        } else {
            Toast.makeText(context, "Order number must not be null or empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTextCounter() {
        binding.txtOrderFilter.text = StringBuilder("Order (").append(orderAdapter.itemCount).append(")")
    }

    override fun onShipperLoadSuccess(shipperList: List<ShipperModel>?) {
        if (dialogLayoutBinding.recyclerShippers != null) {
            dialogLayoutBinding.recyclerShippers.setHasFixedSize(true)
            val layoutManager = LinearLayoutManager(requireContext())
            dialogLayoutBinding.recyclerShippers.layoutManager = layoutManager
            dialogLayoutBinding.recyclerShippers.addItemDecoration(DividerItemDecoration(requireContext(), layoutManager.orientation))

            shipperSelectedAdapter = MyShipperSelectedAdapter(requireContext(), shipperList!!)
            dialogLayoutBinding.recyclerShippers.adapter = shipperSelectedAdapter
        }
    }

    override fun onShipperLoadFailed(message: String?) {
        Toast.makeText(requireContext(), message.toString(), Toast.LENGTH_SHORT).show()
    }
}