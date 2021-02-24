package com.example.myrestaurantv2kotlinserverapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.evenbus.*
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class HomeActivity : AppCompatActivity() {

    private var menuClick = -1
    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var drawer: DrawerLayout
    lateinit var navView: NavigationView
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        subscribeToTopic(Common.getNewOrderTopic())

        drawer = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
                setOf(
                        R.id.nav_category, R.id.nav_order, R.id.nav_food_list, R.id.nav_sign_out
                ), drawer
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener { item ->
            item.isChecked = true
            drawer!!.closeDrawers()
            if (item.itemId == R.id.nav_sign_out) {
                signOut()
            } else if (item.itemId == R.id.nav_category) {
                if (menuClick != item.itemId) {
                    navController.popBackStack() //Clear backStack
                    navController.navigate(R.id.nav_category)
                }
            } else if (item.itemId == R.id.nav_order) {
                if (menuClick != item.itemId){
                    navController.popBackStack()
                    navController.navigate(R.id.nav_order)
                }
            }

            menuClick = item.itemId
            true
        }

        //View
        val headerView = navView.getHeaderView(0)
        val txtUsername = headerView.findViewById<View>(R.id.txt_username) as TextView
        Common.setSpanString("Hey", Common.currentServerUser!!.name, txtUsername)

        menuClick = R.id.nav_category  //Default

    }

    private fun subscribeToTopic(newOrderTopic: String) {
        FirebaseMessaging.getInstance()
            .subscribeToTopic(newOrderTopic)
            .addOnFailureListener { message ->
                Toast.makeText(this@HomeActivity, ""+message.message, Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener { task ->
                if(!task.isSuccessful)
                    Toast.makeText(this@HomeActivity, "Subscribe topic failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun signOut() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Sign out")
                .setMessage("Do you really want to sign out")
                .setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
                .setPositiveButton("OK") { _, _ ->
                    Common.foodSelected = null
                    Common.categorySelected = null
                    Common.currentServerUser = null
                    FirebaseAuth.getInstance().signOut()

                    val intent = Intent(this@HomeActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    //This function process when user click on a category in categoryList
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCategoryClick(event: CategoryClick) {
        if (event.isSuccess) {
            navController.navigate(R.id.nav_food_list)
        }
    }

    //This function process when update category successfully
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onUpdateCategorySuccess(event: SuccessUpdateCategoryEvent) {
        if (event.isSuccess)
            Toast.makeText(this, "Update Category Success!", Toast.LENGTH_SHORT).show()
        EventBus.getDefault().removeStickyEvent(event)
    }

    //This function process when user delete the food successfully
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onUpdateFoodSuccess(event: SuccessEvent) {
        if (event.success)
            if (event.isDelete)
                Toast.makeText(this, "Delete Food success!", Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(this, "Update Food success!", Toast.LENGTH_SHORT).show()
        EventBus.getDefault().removeStickyEvent(event)
    }

    //This function process when having error when update data
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onUpdateError(event: ErrorEvent) {
        if (event != null)
            Toast.makeText(this, "Updating Error: " + event.ex.message, Toast.LENGTH_SHORT).show()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onMenuItemBack(event: MenuItemBack) {
        menuClick = -1
        if (supportFragmentManager.backStackEntryCount > 0)
            supportFragmentManager.popBackStack()
    }


    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

}