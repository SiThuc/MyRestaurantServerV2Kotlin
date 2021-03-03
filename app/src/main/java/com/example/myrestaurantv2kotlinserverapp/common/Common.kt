package com.example.myrestaurantv2kotlinserverapp.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
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
import com.example.myrestaurantv2kotlinserverapp.model.CategoryModel
import com.example.myrestaurantv2kotlinserverapp.model.FoodModel
import com.example.myrestaurantv2kotlinserverapp.model.ServerUserModel
import com.example.myrestaurantv2kotlinserverapp.model.TokenModel
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

    fun updateToken(context: Context, token: String, isServerToken: Boolean, isShipperToken: Boolean) {
        FirebaseDatabase.getInstance()
            .getReference(TOKEN_REF)
            .child(currentServerUser!!.uid!!)
            .setValue(TokenModel(currentServerUser!!.phone!!, token, isServerToken, isShipperToken))
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