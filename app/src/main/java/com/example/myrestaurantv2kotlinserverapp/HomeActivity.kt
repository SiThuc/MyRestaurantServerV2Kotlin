package com.example.myrestaurantv2kotlinserverapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.Menu
import android.view.View
import android.widget.ImageView
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
import com.andremion.counterfab.CounterFab
import com.example.myrestaurantv2kotlinserverapp.adapter.PdfDocumentAdapter
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.database.CartItem
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutNewsSystemBinding
import com.example.myrestaurantv2kotlinserverapp.evenbus.*
import com.example.myrestaurantv2kotlinserverapp.model.FCMResponse
import com.example.myrestaurantv2kotlinserverapp.model.FCMSendData
import com.example.myrestaurantv2kotlinserverapp.model.OrderModel
import com.example.myrestaurantv2kotlinserverapp.services.IFCMService
import com.example.myrestaurantv2kotlinserverapp.services.RetrofitFCMClient
import com.example.myrestaurantv2kotlinserverapp.utils.PDFUltils
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itextpdf.text.*
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfWriter
import io.reactivex.Observable.fromIterable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


class HomeActivity : AppCompatActivity() {

    private var menuClick = -1
    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var drawer: DrawerLayout
    lateinit var navView: NavigationView
    lateinit var navController: NavController

    private var img_upload: ImageView? = null
    private val compositeDisposable = CompositeDisposable()
    private lateinit var ifcmService: IFCMService
    private var imgUri: Uri? = null
    private lateinit var storage: FirebaseStorage
    private var storageReference: StorageReference? = null
    private val PICK_IMAGE_REQUEST = 1234
    private lateinit var dialogBinding: LayoutNewsSystemBinding

    private lateinit var dialog: android.app.AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fabChat = findViewById<CounterFab>(R.id.fab_chat)
        fabChat.setOnClickListener {
            startActivity(Intent(this, ChatListActivity::class.java))
        }

        init()

        drawer = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_category,
                R.id.nav_order,
                R.id.nav_food_list,
                R.id.nav_shipper,
                R.id.nav_sign_out
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
                if (menuClick != item.itemId) {
                    navController.popBackStack()
                    navController.navigate(R.id.nav_order)
                }
            } else if (item.itemId == R.id.nav_shipper) {
                if (menuClick != item.itemId) {
                    navController.popBackStack()
                    navController.navigate(R.id.nav_shipper)
                }
            } else if (item.itemId == R.id.nav_best_deals) {
                if (menuClick != item.itemId) {
                    navController.popBackStack()
                    navController.navigate(R.id.nav_best_deals)
                }
            } else if (item.itemId == R.id.nav_most_popular) {
                if (menuClick != item.itemId) {
                    navController.popBackStack()
                    navController.navigate(R.id.nav_most_popular)
                }
            } else if (item.itemId == R.id.nav_news) {
                showSendNewsDialog()
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

    private fun init() {
        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService::class.java)
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference
        subscribeToTopic(Common.getNewOrderTopic())
        updateToken()

        dialog =
            android.app.AlertDialog.Builder(this).setCancelable(false).setMessage("Please wait...")
                .create()


    }

    private fun showSendNewsDialog() {
        dialogBinding = LayoutNewsSystemBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("News System")
            .setMessage("Send news information to all client")

        //Event
        dialogBinding.rdiNone.setOnClickListener {
            dialogBinding.edtLink.visibility = View.GONE
            dialogBinding.imgUpload.visibility = View.GONE

        }

        dialogBinding.rdiLink.setOnClickListener {
            dialogBinding.edtLink.visibility = View.VISIBLE
            dialogBinding.imgUpload.visibility = View.GONE
        }

        dialogBinding.rdiUpload.setOnClickListener {
            dialogBinding.edtLink.visibility = View.GONE
            dialogBinding.imgUpload.visibility = View.VISIBLE
        }

        dialogBinding.imgUpload.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "Select Picture"),
                PICK_IMAGE_REQUEST
            )
        }

        builder.setView(dialogBinding.root)
        builder.setNegativeButton("CANCEL", { dialogInterface, i -> dialogInterface.dismiss() })
        builder.setPositiveButton("SEND", { dialogInterface, i ->
            if (dialogBinding.rdiNone.isChecked)
                sendNews(
                    dialogBinding.edtTitle.text.toString(),
                    dialogBinding.edtContent.text.toString()
                )
            else if (dialogBinding.rdiLink.isChecked)
                sendNews(
                    dialogBinding.edtTitle.text.toString(),
                    dialogBinding.edtContent.text.toString(),
                    dialogBinding.edtLink.text.toString()
                )
            else if (dialogBinding.rdiUpload.isChecked) {
                if (imgUri != null) {
                    val dialog = AlertDialog.Builder(this).setMessage("Uploading...").create()
                    dialog.show()
                    val fileName = UUID.randomUUID().toString()
                    val newsImage = storageReference!!.child("news/$fileName")
                    newsImage.putFile(imgUri!!)
                        .addOnFailureListener { e ->
                            dialog.dismiss()
                            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                        }
                        .addOnSuccessListener { taskSnapshot ->
                            dialog.dismiss()
                            newsImage.downloadUrl.addOnSuccessListener { uri ->
                                sendNews(
                                    dialogBinding.edtTitle.text.toString(),
                                    dialogBinding.edtContent.text.toString(),
                                    uri.toString()
                                )
                            }
                        }
                        .addOnProgressListener { taskSnapshot ->
                            val progress =
                                Math.round(100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                                    .toDouble()
                            dialog.setMessage(StringBuilder("Uploading: $progress %"))
                        }
                }
            }
        })

        val dialog = builder.create()
        dialog.show()


    }

    private fun sendNews(title: String, content: String, url: String) {
        val notificationData: MutableMap<String, String> = HashMap()
        notificationData[Common.NOTI_TITLE] = title
        notificationData[Common.NOTI_CONTENT] = content
        notificationData[Common.IS_SEND_IMAGE] = "true"
        notificationData[Common.IMAGE_URL] = url

        val fcmSendData = FCMSendData(Common.getNewsTopic(), notificationData)
        val dialog = AlertDialog.Builder(this).setMessage("Waiting...").create()
        dialog.show()

        compositeDisposable.addAll(
            ifcmService.sendNotification(fcmSendData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ t: FCMResponse ->
                    dialog.dismiss()
                    if (t.message_id != 0L)
                        Toast.makeText(this@HomeActivity, "News has been sent", Toast.LENGTH_SHORT)
                            .show()
                    else
                        Toast.makeText(this@HomeActivity, "Send News failed", Toast.LENGTH_SHORT)
                            .show()
                }, {
                    dialog.dismiss()
                    Toast.makeText(this@HomeActivity, it.message, Toast.LENGTH_SHORT).show()
                })
        )


    }

    private fun sendNews(title: String, content: String) {
        val notificationData: MutableMap<String, String> = HashMap()
        notificationData[Common.NOTI_TITLE] = title
        notificationData[Common.NOTI_CONTENT] = content
        notificationData[Common.IS_SEND_IMAGE] = "false"

        val fcmSendData = FCMSendData(Common.getNewsTopic(), notificationData)
        val dialog = AlertDialog.Builder(this).setMessage("Waiting...").create()
        dialog.show()

        compositeDisposable.addAll(
            ifcmService.sendNotification(fcmSendData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ t: FCMResponse ->
                    dialog.dismiss()
                    if (t.message_id != 0L)
                        Toast.makeText(this@HomeActivity, "News has been sent", Toast.LENGTH_SHORT)
                            .show()
                    else
                        Toast.makeText(this@HomeActivity, "Send News failed", Toast.LENGTH_SHORT)
                            .show()
                }, {
                    dialog.dismiss()
                    Toast.makeText(this@HomeActivity, it.message, Toast.LENGTH_SHORT).show()
                })
        )


    }

    private fun updateToken() {
        FirebaseInstanceId.getInstance()
            .instanceId
            .addOnFailureListener { e ->
                Toast.makeText(this@HomeActivity, e.message.toString(), Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener { instanceIdResult ->
                Common.updateToken(this@HomeActivity, instanceIdResult.token, true, false)
            }
    }

    private fun subscribeToTopic(newOrderTopic: String) {
        FirebaseMessaging.getInstance()
            .subscribeToTopic(newOrderTopic)
            .addOnFailureListener { message ->
                Toast.makeText(this@HomeActivity, "" + message.message, Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener { task ->
                if (!task.isSuccessful)
                    Toast.makeText(this@HomeActivity, "Subscribe topic failed", Toast.LENGTH_SHORT)
                        .show()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.data != null) {
                imgUri = data.data
                dialogBinding.imgUpload.setImageURI(imgUri)
            }
        }
    }

    //Listen for event when user click on Print Order
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onPrintOrderEvent(event: PrintOrderEvent) {
        createPDFFile(event.path, event.order)
        EventBus.getDefault().removeStickyEvent(event)
    }

    private fun createPDFFile(path: String, order: OrderModel) {
        dialog.show()
        if (File(path).exists())
            File(path).delete()
        try {
            val document = Document()
            //Save
            PdfWriter.getInstance(document, FileOutputStream(path))

            //Open
            document.open()

            //Setting
            document.pageSize = (PageSize.A4)
            document.addCreationDate()
            document.addAuthor("Pham Thuc")
            document.addCreator(Common.currentServerUser!!.name)

            //font Setting
            val colorAccent = BaseColor(0, 153, 204, 255)
            val fontSize = 20.0f

            //Custom font
            val fontName =
                BaseFont.createFont("assets/fonts/branden_medium.otf", "UTF-8", BaseFont.EMBEDDED)

            //Create titel of document
            val titleFont = Font(fontName, 36.0f, Font.NORMAL, BaseColor.BLACK)
            PDFUltils.addNewItem(document, "Order Details", Element.ALIGN_CENTER, titleFont)

            //Add more
            val orderNumberFont = Font(fontName, fontSize, Font.NORMAL, colorAccent)
            PDFUltils.addNewItem(document, "Order No: ", Element.ALIGN_LEFT, orderNumberFont)

            val orderNumberValueFont = Font(fontName, fontSize, Font.NORMAL, BaseColor.BLACK)
            PDFUltils.addNewItem(document, order.key!!, Element.ALIGN_LEFT, orderNumberValueFont)
            PDFUltils.addLineSeparator(document)

            //Date
            PDFUltils.addNewItem(document, "Order Date: ", Element.ALIGN_LEFT, orderNumberFont)
            PDFUltils.addNewItem(
                document, SimpleDateFormat("dd-MM-yyyy").format(order.createDate),
                Element.ALIGN_LEFT, orderNumberValueFont
            )
            PDFUltils.addLineSeparator(document)

            //Account name
            PDFUltils.addNewItem(document, "Account Name: ", Element.ALIGN_LEFT, orderNumberFont)
            PDFUltils.addNewItem(document, order.key!!, Element.ALIGN_LEFT, orderNumberValueFont)
            PDFUltils.addLineSeparator(document)

            //Product Detail
            PDFUltils.addLineSpace(document)
            PDFUltils.addNewItem(document, " Product Detail: ", Element.ALIGN_LEFT, titleFont)
            PDFUltils.addLineSeparator(document)

            //Using RxJava, fetch image form url and add to PDF
            io.reactivex.Observable.fromIterable(order.cartItemList)
                .flatMap({ cartItem: CartItem ->
                    Common.getBitmapFromUrl(this@HomeActivity, cartItem, document)
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Consumer { cartItem -> //On Next
                    //Each time, we will add detail
                    //Food name
                    PDFUltils.addNewItemWithLeftAndRight(
                        document,
                        cartItem.foodName!!,
                        "(0.0%)",
                        titleFont,
                        orderNumberValueFont
                    )

                    //Food Size and Addon
                    PDFUltils.addNewItemWithLeftAndRight(
                        document,
                        "Size: ",
                        Common.formatSizeJsonToString(cartItem.foodSize)!!,
                        titleFont,
                        orderNumberValueFont

                    )

                    PDFUltils.addNewItemWithLeftAndRight(
                        document,
                        "Addon: ",
                        Common.formatAddonJsonToString(cartItem.foodAddon)!!,
                        titleFont,
                        orderNumberValueFont
                    )


                    //FoodPrice
                    //Format : 1* 30 = 30
                    PDFUltils.addNewItemWithLeftAndRight(
                        document,
                        StringBuilder().append(cartItem.foodQuantity)
                            .append("*")
                            .append(cartItem.foodExtraPrice!! + cartItem.foodPrice!!)
                            .toString(),
                        StringBuilder().append(cartItem.foodQuantity!! * (cartItem.foodExtraPrice!! + cartItem.foodPrice!!))
                            .toString(),
                        titleFont,
                        orderNumberValueFont
                    )

                    //Last separator
                    PDFUltils.addLineSeparator(document)

                }, Consumer { t -> //On Error
                    dialog.dismiss()
                    Toast.makeText(this@HomeActivity, "", Toast.LENGTH_SHORT).show()
                },
                    Action { //On Complete
                        //When all product detail is wrote, we will append Total
                        PDFUltils.addLineSpace(document)
                        PDFUltils.addLineSpace(document)
                        PDFUltils.addNewItemWithLeftAndRight(
                            document,
                            "Total",
                            StringBuilder().append(order.totalPayment).toString(),
                            titleFont,
                            titleFont
                        )

                        //Close
                        document.close()
                        dialog.dismiss()
                        Toast.makeText(this@HomeActivity, "Success", Toast.LENGTH_SHORT).show()
                        printPDF()

                    })


        }catch (e: FileNotFoundException){
            e.printStackTrace()
        }catch (e: IOException){
            e.printStackTrace()
        }catch (e: DocumentException){
            e.printStackTrace()
        }

    }

    private fun printPDF() {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        try {
            val printDocumentAdapter = PdfDocumentAdapter(
                this, StringBuilder(Common.getAppPath(this)).append(Common.FILE_PRINT).toString())
            printManager.print("Document", printDocumentAdapter, PrintAttributes.Builder().build())
        }catch (e: Exception){
            e.printStackTrace()
        }
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