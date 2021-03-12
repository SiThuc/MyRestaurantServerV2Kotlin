package com.example.myrestaurantv2kotlinserverapp

import android.animation.ValueAnimator
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.myrestaurantv2kotlinserverapp.callback.ISingleShipperOrderCallbackListener
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.model.ShipperOrderModel
import com.example.myrestaurantv2kotlinserverapp.remote.IGoogleAPI
import com.example.myrestaurantv2kotlinserverapp.remote.RetrofitGoogleAPIClient

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.lang.Exception

class TrackingOrderActivity : AppCompatActivity(), OnMapReadyCallback,
    ISingleShipperOrderCallbackListener, ValueEventListener {

    private var isInit: Boolean = false
    private var shippingRef: DatabaseReference?= null
    private var currentShipperOrder: ShipperOrderModel? = null
    private var shipperMarker: Marker? = null
    private var polylineOptions: PolylineOptions? = null
    private var blackPolylineOptions: PolylineOptions? = null
    private var blackPolyline: Polyline? = null
    private var grayPolyline: Polyline? = null
    private var yellowPolyline: Polyline? = null
    private var polylineList: List<LatLng> = ArrayList()

    //Move maker
    private var handler: Handler? = null
    private var index = 0
    private var next: Int = 0
    private var v = 0f
    private var lat = 0.0
    private var lng = 0.0
    private var startPosition = LatLng(0.0, 0.0)
    private var endPosition = LatLng(0.0, 0.0)

    private lateinit var iGoogleAPI: IGoogleAPI
    private val compositeDisposable = CompositeDisposable()

    private lateinit var mMap: GoogleMap
    private var iSingleShipperOrderModelListener: ISingleShipperOrderCallbackListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking_order)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        initViews()
    }

    private fun initViews() {
        iSingleShipperOrderModelListener = this
        iGoogleAPI = RetrofitGoogleAPIClient.instance!!.create(IGoogleAPI::class.java)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this,
                    R.raw.uber_light_with_label
                )
            )
            if (!success)
                Log.d("DEBUG", "Failed to load map style")
        } catch (ex: Resources.NotFoundException) {
            Log.d("DEBUG", "Not found json string for map style")
        }

        checkOrderFromFirebase()
    }

    private fun checkOrderFromFirebase() {
        FirebaseDatabase.getInstance()
            .getReference(Common.SHIPPING_ORDER_REF)
            .child(Common.currentOrderSelected!!.orderNumber!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val shipperOrderModel = snapshot.getValue(ShipperOrderModel::class.java)
                        shipperOrderModel!!.key = snapshot.key
                        iSingleShipperOrderModelListener!!.onSingleShipperOrderSuccess(
                            shipperOrderModel
                        )
                    } else {
                        Toast.makeText(
                            this@TrackingOrderActivity,
                            "Order not found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@TrackingOrderActivity, error.message, Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    override fun onSingleShipperOrderSuccess(shipperOrderModel: ShipperOrderModel) {
        currentShipperOrder = shipperOrderModel
        subscribeShipperMove(currentShipperOrder!!)

        val locationOrder =
            LatLng(shipperOrderModel.orderModel!!.lat, shipperOrderModel.orderModel!!.lng)
        val locationShipper = LatLng(shipperOrderModel.currentLat, shipperOrderModel.currentLng)


        //Add Box
        mMap.addMarker(
            MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.box))
                .title(shipperOrderModel.orderModel!!.userName)
                .snippet(shipperOrderModel!!.orderModel!!.shippingAddress)
                .position(locationOrder)
        )

        //Add Shipper
        if (shipperMarker == null) {
            val height = 80
            val width = 80
            val bitmapDrawable = ContextCompat.getDrawable(
                this@TrackingOrderActivity,
                R.drawable.shippernew
            ) as BitmapDrawable
            val resized = Bitmap.createScaledBitmap(bitmapDrawable.bitmap, width, height, false)

            shipperMarker = mMap.addMarker(
                MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(resized))
                    .title(shipperOrderModel.shipperName)
                    .snippet(shipperOrderModel.shipperPhone)
                    .position(locationShipper)
            )

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 18.0f))
        } else {
            shipperMarker!!.position = locationShipper
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 18.0f))
        }

        //Draw Routes
        val to = StringBuilder().append(shipperOrderModel.orderModel!!.lat)
            .append(",")
            .append(shipperOrderModel.orderModel!!.lng)
            .toString()

        val from = StringBuilder().append(shipperOrderModel.currentLat)
            .append(",")
            .append(shipperOrderModel.currentLng)
            .toString()


        compositeDisposable.add(
            iGoogleAPI!!.getDirections(
                "driving", "less_driving", from, to,
                getString(R.string.google_maps_key)
            )!!
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ s ->
                    try {
                        val jsonObject = JSONObject(s)
                        val jsonArray = jsonObject.getJSONArray("routes")
                        for (i in 0 until jsonArray.length()) {
                            val route = jsonArray.getJSONObject(i)
                            val poly = route.getJSONObject("overview_polyline")
                            val polyline = poly.getString("points")
                            polylineList = Common.decodePoly(polyline)
                        }

                        polylineOptions = PolylineOptions()
                        polylineOptions!!.color(Color.YELLOW)
                        polylineOptions!!.width(12.0f)
                        polylineOptions!!.startCap(SquareCap())
                        polylineOptions!!.endCap(SquareCap())
                        polylineOptions!!.jointType(JointType.ROUND)
                        polylineOptions!!.addAll(polylineList)
                        yellowPolyline = mMap.addPolyline(polylineOptions)
                    } catch (e: Exception) {
                        Log.d("DEBUG", e.message.toString())
                    }

                }, { t ->
                    Toast.makeText(
                        this@TrackingOrderActivity,
                        t.message.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                })
        )
    }

    private fun subscribeShipperMove(currentShipperOrder: ShipperOrderModel) {
        shippingRef = FirebaseDatabase.getInstance()
            .getReference(Common.SHIPPING_ORDER_REF)
            .child(currentShipperOrder.key!!)

        shippingRef!!.addValueEventListener(this)
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        if(snapshot.exists()){
            //Save old position
            val from = StringBuilder().append(currentShipperOrder!!.currentLat).append(",")
                .append(currentShipperOrder!!.currentLng).toString()

            //Update position
            currentShipperOrder = snapshot.getValue(ShipperOrderModel::class.java)

            //Save new position
            val to = StringBuilder().append(currentShipperOrder!!.currentLat).append(",")
                .append(currentShipperOrder!!.currentLng).toString()

            if(isInit)
                moveMarkerAnimation(shipperMarker, from, to)
            else
                isInit = true
        }
    }

    private fun moveMarkerAnimation(shipperMarker: Marker?, from: String, to: String) {
        compositeDisposable.addAll(
            iGoogleAPI.getDirections("driving", "less_driving", from, to, getString(R.string.google_maps_key))!!
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ s ->
                    Log.d("DEBUG", s!!)
                    try {
                        val jsonObject = JSONObject(s)
                        val jsonArray = jsonObject.getJSONArray("routes")
                        for (i in 0 until jsonArray.length()) {
                            val route = jsonArray.getJSONObject(i)
                            val poly = route.getJSONObject("overview_polyline")
                            val polyline = poly.getString("points")

                            polylineList = Common.decodePoly(polyline)
                        }

                        polylineOptions = PolylineOptions()
                        polylineOptions!!.color(Color.GRAY)
                        polylineOptions!!.width(5.0f)
                        polylineOptions!!.startCap(SquareCap())
                        polylineOptions!!.endCap(SquareCap())
                        polylineOptions!!.jointType(JointType.ROUND)
                        polylineOptions!!.addAll(polylineList)
                        grayPolyline = mMap.addPolyline(polylineOptions)

                        blackPolylineOptions = PolylineOptions()
                        blackPolylineOptions!!.color(Color.BLACK)
                        blackPolylineOptions!!.width(5.0f)
                        blackPolylineOptions!!.startCap(SquareCap())
                        blackPolylineOptions!!.endCap(SquareCap())
                        blackPolylineOptions!!.jointType(JointType.ROUND)
                        blackPolylineOptions!!.addAll(polylineList)
                        blackPolyline = mMap.addPolyline(blackPolylineOptions)


                        //Animator
                        val polylineAnimator = ValueAnimator.ofInt(0, 100)
                        polylineAnimator.duration = 2000
                        polylineAnimator.interpolator = LinearInterpolator()
                        polylineAnimator.addUpdateListener { valueAnimator ->
                            val points = grayPolyline!!.points
                            val percentValue =
                                Integer.parseInt(valueAnimator.animatedValue.toString())
                            val size = points.size
                            val newPoints = (size * (percentValue / 100.0f).toInt())
                            val p = points.subList(0, newPoints)
                            blackPolyline!!.points = p
                        }
                        polylineAnimator.start()

                        //Car moving
                        index = -1
                        next = 1
                        val r = object : Runnable {
                            override fun run() {
                                if (index < polylineList.size - 1) {
                                    index++
                                    next = index + 1
                                    startPosition = polylineList[index]
                                    endPosition = polylineList[next]
                                }

                                val valueAnimator = ValueAnimator.ofInt(0, 1)
                                valueAnimator.duration = 1500
                                valueAnimator.interpolator = LinearInterpolator()
                                valueAnimator.addUpdateListener { valueAnimator ->
                                    v = valueAnimator.animatedFraction
                                    lat =
                                        v * endPosition!!.latitude + (1 - v) * startPosition!!.latitude
                                    lng =
                                        v * endPosition!!.longitude + (1 - v) * startPosition!!.longitude

                                    val newPos = LatLng(lat, lng)
                                    shipperMarker!!.position = newPos
                                    shipperMarker.setAnchor(0.5f, 0.5f)
                                    shipperMarker.rotation = Common.getBearing(startPosition!!, newPos)

                                    mMap.moveCamera(CameraUpdateFactory.newLatLng(shipperMarker.position)) //Fixed
                                }

                                valueAnimator.start()
                                if (index < polylineList.size - 2)
                                    handler!!.postDelayed(this, 1500)
                            }

                        }

                        handler = Handler()
                        handler!!.postDelayed(r, 1500)

                    } catch (e: Exception) {
                        Log.d("DEBUG", e.message.toString())
                    }

                }, { throwable ->
                    Toast.makeText(this@TrackingOrderActivity, throwable.message, Toast.LENGTH_SHORT)
                        .show()
                })
        )
    }

    override fun onCancelled(error: DatabaseError) {
        Toast.makeText(this@TrackingOrderActivity, error.message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        shippingRef!!.removeEventListener(this)
        isInit = false
        super.onDestroy()
    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }
}