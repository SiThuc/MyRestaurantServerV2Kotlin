package com.example.myrestaurantv2kotlinserverapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myrestaurantv2kotlinserverapp.callback.ILoadTimeFromFirebaseCallback
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.databinding.ActivityChatDetailBinding
import com.example.myrestaurantv2kotlinserverapp.model.ChatInfoModel
import com.example.myrestaurantv2kotlinserverapp.model.ChatMessageModel
import com.example.myrestaurantv2kotlinserverapp.viewholder.ChatPictureViewHolder
import com.example.myrestaurantv2kotlinserverapp.viewholder.ChatTextViewHolder
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class ChatDetailActivity : AppCompatActivity(), ILoadTimeFromFirebaseCallback {
    private val MY_CAMERA_REQUEST_CODE = 1100
    private val MY_RESULT_LOAD_IMAGE = 7272

    private lateinit var binding: ActivityChatDetailBinding

    private var roomId: String? = null
    private var chatSender: String? = null

    var database: FirebaseDatabase? = null
    var chatRef: DatabaseReference? = null
    var offsetRef: DatabaseReference? = null
    var listener: ILoadTimeFromFirebaseCallback? = null

    lateinit var adapter: FirebaseRecyclerAdapter<ChatMessageModel, RecyclerView.ViewHolder>
    lateinit var options: FirebaseRecyclerOptions<ChatMessageModel>

    var fileUri: Uri? = null
    var storageReference: StorageReference? = null
    var layoutManager: LinearLayoutManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        loadChatContent()
    }

    override fun onStart() {
        super.onStart()
        if (adapter != null) adapter.startListening()
    }

    override fun onResume() {
        super.onResume()
        if (adapter != null) adapter.startListening()
    }

    override fun onStop() {
        if (adapter != null) adapter.stopListening()
        super.onStop()
    }

    private fun loadChatContent() {
        adapter =
            object : FirebaseRecyclerAdapter<ChatMessageModel, RecyclerView.ViewHolder>(options) {

                override fun getItemViewType(position: Int): Int {
                    return if (adapter.getItem(position).isPicture) 1 else 0
                }

                override fun onCreateViewHolder(
                    viewGroup: ViewGroup,
                    viewType: Int
                ): RecyclerView.ViewHolder {
                    val view: View
                    return if (viewType == 0) {
                        view = LayoutInflater.from(viewGroup.context)
                            .inflate(R.layout.layout_message_text, viewGroup, false)
                        ChatTextViewHolder(view)
                    } else {
                        view = LayoutInflater.from(viewGroup.context)
                            .inflate(R.layout.layout_message_picture, viewGroup, false)
                        ChatPictureViewHolder(view)
                    }
                }

                override fun onBindViewHolder(
                    holder: RecyclerView.ViewHolder,
                    position: Int,
                    model: ChatMessageModel
                ) {
                    if (holder is ChatTextViewHolder) {
                        val chatTextViewHolder = holder as ChatTextViewHolder
                        chatTextViewHolder.txt_email!!.text = model.name
                        chatTextViewHolder.txt_chat_message!!.text = model.content
                        chatTextViewHolder.txt_time!!.text =
                            DateUtils.getRelativeTimeSpanString(
                                model.timeStamp!!,
                                Calendar.getInstance().timeInMillis, 0
                            ).toString()
                    } else {
                        val chatPictureViewHolder = holder as ChatPictureViewHolder
                        chatPictureViewHolder.txt_email!!.text = model.name
                        chatPictureViewHolder.txt_chat_message!!.text = model.content
                        chatPictureViewHolder.txt_time!!.text =
                            DateUtils.getRelativeTimeSpanString(
                                model.timeStamp!!,
                                Calendar.getInstance().timeInMillis, 0
                            ).toString()

                        Glide.with(this@ChatDetailActivity)
                            .load(model.pictureLink)
                            .into(chatPictureViewHolder.img_preview!!)

                    }
                }


            }

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val friendlyMessageCount = adapter.itemCount
                val lastVisiblePosition = layoutManager!!.findLastCompletelyVisibleItemPosition()
                if (lastVisiblePosition == -1 ||
                    positionStart >= friendlyMessageCount - 1 &&
                    lastVisiblePosition == positionStart - 1
                ) {
                    binding.recyclerChat.scrollToPosition(positionStart)
                }
            }
        })

        binding.recyclerChat.adapter = adapter
    }

    private fun initViews() {
        roomId = intent.getStringExtra(Common.KEY_CHAT_ROOM_ID)
        chatSender = intent.getStringExtra(Common.KEY_CHAT_SENDER)

        listener = this
        database = FirebaseDatabase.getInstance()
        chatRef = database!!.getReference(Common.CHAT_REF)

        offsetRef = database!!.getReference(".info/serverTimeOffset")

        val query = chatRef!!.child(roomId!!)
            .child(Common.CHAT_DETAIL_REF)

        options = FirebaseRecyclerOptions.Builder<ChatMessageModel>()
            .setQuery(query, ChatMessageModel::class.java)
            .build()

        layoutManager = LinearLayoutManager(this)
        binding.recyclerChat.layoutManager = layoutManager

        binding.toolbar.title = chatSender
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        //Event
        binding.imgImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/"
            startActivityForResult(intent, MY_RESULT_LOAD_IMAGE)
        }

        binding.imgCamera.setOnClickListener {
            Log.d("DEBUG", "Begin To startCamera")
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//            val builder = StrictMode.VmPolicy.Builder()
//            StrictMode.setVmPolicy(builder.build())
//
//            fileUri = getOutputMediaFileUri()
//            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)

            startActivityForResult(intent, MY_CAMERA_REQUEST_CODE)

        }

        binding.imgSend.setOnClickListener {
            offsetRef!!.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val offset = snapshot.getValue(Long::class.java)
                    val estimatedServerTimeInMs = System.currentTimeMillis().plus(offset!!)
                    listener!!.onLoadOnlyTimeSuccess(estimatedServerTimeInMs)
                }

                override fun onCancelled(error: DatabaseError) {
                    listener!!.onLoadTimeFailed(error.message)
                }
            })
        }

    }

    private fun getOutputMediaFileUri(): Uri? {
        return Uri.fromFile(getOutputMediaFile())

    }

    private fun getOutputMediaFile(): File? {
        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "MyRestaurant"
        )
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.exists())
                return null
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

        return File(
            StringBuilder(mediaStorageDir.path)
                .append(File.separator)
                .append("IMG_")
                .append(timeStamp)
                .append("_")
                .append(Random().nextInt()).toString()
        )
    }

    override fun onLoadOnlyTimeSuccess(estimatedTimeMs: Long) {
        val chatMessageModel = ChatMessageModel()
        chatMessageModel.name = Common.currentServerUser!!.name
        chatMessageModel.content = binding.edtChat.text.toString()
        chatMessageModel.timeStamp = estimatedTimeMs

        if (fileUri == null) {
            chatMessageModel.isPicture = false
            submitChatToFirebase(chatMessageModel, chatMessageModel.isPicture, estimatedTimeMs)
        } else {
            uploadPicture(fileUri!!, chatMessageModel, estimatedTimeMs)
        }
    }

    private fun uploadPicture(
        fileUri: Uri,
        chatMessageModel: ChatMessageModel,
        estimatedTimeInMs: Long
    ) {
        if (fileUri != null) {
            val dialog = AlertDialog.Builder(this@ChatDetailActivity)
                .setCancelable(false)
                .setMessage("Please wait")
                .create()
            dialog.show()
            val fileName = Common.getFileName(contentResolver, fileUri)
            val path = StringBuilder(Common.currentServerUser!!.uid)
                .append("/")
                .append(fileName)
                .toString()

            storageReference = FirebaseStorage.getInstance().getReference(path)
            val uploadTask = storageReference!!.putFile(fileUri)

            uploadTask.continueWithTask { task1 ->
                if (!task1.isSuccessful)
                    Toast.makeText(this@ChatDetailActivity, "Failed to upload", Toast.LENGTH_SHORT).show()
                storageReference!!.downloadUrl

            }.addOnFailureListener { t ->
                dialog.dismiss()
                Toast.makeText(this@ChatDetailActivity, t.message, Toast.LENGTH_SHORT).show()
            }.addOnCompleteListener { task2 ->
                if (task2.isSuccessful) {
                    val url = task2.result.toString()
                    dialog.dismiss()
                    chatMessageModel.isPicture = true
                    chatMessageModel.pictureLink = url
                    submitChatToFirebase(
                        chatMessageModel,
                        chatMessageModel.isPicture,
                        estimatedTimeInMs
                    )
                }
            }
        } else {
            Toast.makeText(this@ChatDetailActivity, "Image is empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitChatToFirebase(
        chatMessageModel: ChatMessageModel,
        isPicture: Boolean,
        estimatedTimeInMs: Long
    ) {
        chatRef!!.child(roomId!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        appendChat(chatMessageModel, isPicture, estimatedTimeInMs)
                    } else {
                        createChat(chatMessageModel, isPicture, estimatedTimeInMs)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatDetailActivity, error.message, Toast.LENGTH_SHORT).show()
                }

            })
    }

    private fun appendChat(
        chatMessageModel: ChatMessageModel,
        picture: Boolean,
        estimatedTimeInMs: Long
    ) {
        val updateData = HashMap<String, Any>()
        updateData["lastUpdate"] = estimatedTimeInMs
        if (picture)
            updateData["lastUpdate"] = "<Image>"
        else
            updateData["lastMessage"] = chatMessageModel.content!!

        chatRef!!.child(roomId!!)
            .updateChildren(updateData)
            .addOnFailureListener { e ->
                Toast.makeText(
                    this@ChatDetailActivity,
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnCompleteListener { task2 ->
                if (task2.isSuccessful) {
                    chatRef!!.child(roomId!!)
                        .child(Common.CHAT_DETAIL_REF)
                        .push()
                        .setValue(chatMessageModel)
                        .addOnFailureListener { e ->
                            Toast.makeText(this@ChatDetailActivity, e.message, Toast.LENGTH_SHORT).show()
                        }
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                binding.edtChat.setText("")
                                binding.edtChat.requestFocus()

                                if (adapter != null) {
                                    adapter.notifyDataSetChanged()
                                    if (picture) {
                                        fileUri = null
                                        binding.imgPreview.visibility = View.GONE
                                    }
                                }
                            }
                        }

                }
            }
    }

    private fun createChat(
        chatMessageModel: ChatMessageModel,
        picture: Boolean,
        estimatedTimeInMs: Long
    ) {
        val chatInfoModel = ChatInfoModel()
        chatInfoModel.createName = chatMessageModel.name
        if (picture)
            chatInfoModel.lastMessage = "<Image>"
        else
            chatInfoModel.lastMessage = chatMessageModel.content

        chatInfoModel.lastUpdate = estimatedTimeInMs
        chatInfoModel.createDate = estimatedTimeInMs

        chatRef!!.child(roomId!!)
            .setValue(chatInfoModel)
            .addOnFailureListener { e ->
                Toast.makeText(
                    this@ChatDetailActivity,
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnCompleteListener { task2 ->
                if (task2.isSuccessful) {
                    chatRef!!.child(roomId!!)
                        .child(Common.CHAT_DETAIL_REF)
                        .push()
                        .setValue(chatMessageModel)
                        .addOnFailureListener { e ->
                            Toast.makeText(this@ChatDetailActivity, e.message, Toast.LENGTH_SHORT).show()
                        }
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                binding.edtChat.setText("")
                                binding.edtChat.requestFocus()

                                if (adapter != null) {
                                    adapter.notifyDataSetChanged()
                                    if (picture) {
                                        fileUri = null
                                        binding.imgPreview.visibility = View.GONE
                                    }
                                }
                            }
                        }
                }
            }
    }


    override fun onLoadTimeFailed(message: String) {
        Toast.makeText(this@ChatDetailActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun rotateBitmap(bitmap: Bitmap, i: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(i.toFloat())
        return Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                var bitmap: Bitmap?
                var ei: ExifInterface?=null
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(contentResolver, fileUri)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        ei = ExifInterface(contentResolver.openInputStream(fileUri!!)!!)
                    }
                    val orientation = ei!!.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED
                    )

                    var rotateBitmap: Bitmap
                    rotateBitmap = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90)
                        ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180)
                        ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270)
                        else -> bitmap
                    }
                    binding.imgPreview.visibility = View.VISIBLE
                    binding.imgPreview.setImageBitmap(rotateBitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } else if (requestCode == MY_RESULT_LOAD_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    val imageUri = data!!.data
                    val inputStream = contentResolver.openInputStream(imageUri!!)
                    val selectedImage = BitmapFactory.decodeStream(inputStream)
                    binding.imgPreview.visibility = View.VISIBLE
                    fileUri = imageUri
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }

            } else {
                Toast.makeText(this@ChatDetailActivity, "You have not select image", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}