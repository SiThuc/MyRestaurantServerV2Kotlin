package com.example.myrestaurantv2kotlinserverapp.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.myrestaurantv2kotlinserverapp.R
import com.example.myrestaurantv2kotlinserverapp.model.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.FirebaseDatabase
import java.util.*


object Common {
    fun setSpanString(welcome: String, name: String?, txtUsername: TextView) {
        val builder = SpannableStringBuilder()
        builder.append(welcome)
        val spannableString = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        spannableString.setSpan(boldSpan, 0, name!!.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.append(spannableString)
        txtUsername.setText(builder, TextView.BufferType.SPANNABLE)
    }

    fun setSpanStringColor(welcome: String, name: String?, textView: TextView, color: Int) {
        val builder = SpannableStringBuilder()
        builder.append(welcome)
        val spannableString = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        spannableString.setSpan(boldSpan, 0, name!!.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(
                ForegroundColorSpan(color),
                0,
                name!!.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.append(spannableString)
        textView.setText(builder, TextView.BufferType.SPANNABLE)

    }

    fun convertStatusToString(orderStatus: Int): String? {
        when (orderStatus) {
            0 -> return "Placed"
            1 -> return "Shipping"
            2 -> return "Shipped"
            -1 -> return "Cancelled"
            else -> return "Error"
        }
    }

    fun updateToken(
            context: Context,
            token: String,
            isServerToken: Boolean,
            isShipperToken: Boolean
    ) {
        if (currentServerUser != null)
            FirebaseDatabase.getInstance()
                    .getReference(TOKEN_REF)
                    .child(currentServerUser!!.uid!!)
                    .setValue(
                            TokenModel(
                                    currentServerUser!!.phone!!,
                                    token,
                                    isServerToken,
                                    isShipperToken
                            )
                    )
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
                    }
    }

    fun createOrderNumber(): String {
        return StringBuilder()
                .append(System.currentTimeMillis())
                .append(Math.abs(Random().nextInt()))
                .toString()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun showNotification(
            context: Context,
            id: Int,
            title: String?,
            content: String?,
            intent: Intent?
    ) {
        Log.d("Notification", "Tittle:$title, Content:$content")

        var pendingIntent: PendingIntent? = null
        if (intent != null) {
            pendingIntent =
                    PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            val NOTIFICATION_CHANNEL_ID = "pham.thuc.myrestaurantv2.server"

            val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notificationChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "My Restaurant V2",
                    NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.description = "My Restaurant V2 Channel"
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)

            notificationManager.createNotificationChannel(notificationChannel)

            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            builder.setContentTitle(title).setContentText(content).setAutoCancel(true)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setLargeIcon(
                            BitmapFactory.decodeResource(
                                    context.resources,
                                    R.drawable.ic_restaurant_24
                            )
                    )

            if (pendingIntent != null)
                builder.setContentIntent(pendingIntent)

            val notification = builder.build()
            notificationManager.notify(id, notification)
        }
        Log.d("Notification", "Ending notification")
    }

    fun getNewOrderTopic(): String {
        return java.lang.StringBuilder("/topics/new_order").toString()
    }

    fun decodePoly(encoded: String): List<LatLng> {
        val poly: MutableList<LatLng> = ArrayList<LatLng>()
        var index = 0
        var len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0

            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0

            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    fun getBearing(begin: LatLng, end: LatLng): Float {
        val lat = Math.abs(begin.latitude - end.latitude)
        val lng = Math.abs(begin.longitude - end.longitude)

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return Math.toDegrees(Math.atan(lng / lat)).toFloat()
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (90 - Math.toDegrees(Math.atan(lng / lat)) + 90).toFloat()
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (Math.toDegrees(Math.atan(lng / lat)) + 180).toFloat()
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (90 - Math.toDegrees(Math.atan(lng / lat)) + 270).toFloat()
        return -1.0f

    }

    fun getNewsTopic(): String {
        return StringBuilder("/topics/news").toString()
    }

    fun generateChatRoomId(uid: String): String {
        if(uid != null)
            return uid
        else
            return StringBuilder("ChatYourSelf_Error_").append(Random().nextInt()).toString()
    }

    fun getFileName(contentResolver: ContentResolver?, fileUri: Uri): Any {
        var result:String? = null
        if(fileUri.scheme == "content"){
            val cursor = contentResolver!!.query(fileUri, null, null, null, null)
            try {
                if(cursor != null && cursor.moveToFirst())
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }finally {
                cursor!!.close()
            }
        }

        if(result == null) {
            result = fileUri.path
            val cut = result!!.lastIndexOf('/')
            if(cut != -1)
                result = result!!.substring(cut + 1)
        }
        return result
    }

    val CHAT_DETAIL_REF: String = "ChatDetail"
    val KEY_CHAT_SENDER: String = "CHAT_SENDER"
    val KEY_CHAT_ROOM_ID: String?="CHAT_ROOM_ID"
    val CHAT_REF: String = "Chats"
    val IMAGE_URL: String ="IMAGE_URL"
    val IS_SEND_IMAGE: String = "IS_SEND_IMAGE"
    var mostPopularSelected: MostPopularModel? = null
    val MOST_POPULAR: String = "MostPopular"
    var bestDealsSelected: BestDealsModel? = null
    val BEST_DEALS: String = "BestDeals"
    var currentOrderSelected: OrderModel? = null
    val SHIPPING_ORDER_REF: String = "ShipperOrders"
    val SHIPPER_REF: String = "Shippers"
    val NOTI_CONTENT: String = "Content"
    val NOTI_TITLE: String = "Title"
    val TOKEN_REF: String = "Tokens"
    val ORDER_REF: String = "Orders"
    var foodSelected: FoodModel? = null
    val CATEGORY_REF: String = "Category"
    val FULL_WIDTH_COLUMN: Int = 1
    val DEFAULT_COLUMN_COUNT: Int = 1
    var categorySelected: CategoryModel? = null
    var currentServerUser: ServerUserModel? = null
    val SERVER_REF: String = "Server"
}