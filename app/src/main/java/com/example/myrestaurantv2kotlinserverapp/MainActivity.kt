package com.example.myrestaurantv2kotlinserverapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import com.example.myrestaurantv2kotlinserverapp.common.Common
import com.example.myrestaurantv2kotlinserverapp.databinding.ActivityMainBinding
import com.example.myrestaurantv2kotlinserverapp.databinding.LayoutRegisterBinding
import com.example.myrestaurantv2kotlinserverapp.model.ServerUserModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import dmax.dialog.SpotsDialog
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private lateinit var dialog: AlertDialog
    private lateinit var serverRef: DatabaseReference
    private lateinit var binding: ActivityMainBinding

    companion object{
        val APP_REQUEST_CODE = 2018
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(listener)
    }

    override fun onStop() {
        firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    private fun init() {
        firebaseAuth = FirebaseAuth.getInstance()
        serverRef = FirebaseDatabase.getInstance().getReference(Common.SERVER_REF)
        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()

        listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if(user != null){
                //If user is already logged
                Toast.makeText(this, "Already Login", Toast.LENGTH_SHORT).show()
                checkUserFromFirebase(user)
            }else{
                //If user is not logged
                phoneLogin()
            }
        }
    }

    private fun phoneLogin() {
        val providers = arrayListOf(AuthUI.IdpConfig.PhoneBuilder().build())

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                APP_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
            } else {
                Toast.makeText(this, "Failed to sign in", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUserFromFirebase(user: FirebaseUser) {
        dialog.show()
        serverRef.child(user.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val serverUser = snapshot.getValue(ServerUserModel::class.java)
                            if (serverUser!!.isActive)
                                goToHomeActivity(serverUser)
                            else {
                                dialog.dismiss()
                                Toast.makeText(this@MainActivity, "You must be allowed from Admin to access this app", Toast.LENGTH_SHORT).show()
                            }

                        } else{
                            dialog.dismiss()
                            showRegisterDialog(user)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
                    }
                })
        dialog.dismiss()
    }

    private fun showRegisterDialog(user: FirebaseUser) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Register User")
        builder.setMessage("Please fill information for registering")

        val dialogBinding = LayoutRegisterBinding.inflate(layoutInflater)
        dialogBinding.edtPhone.setText(user.phoneNumber.toString())

        builder.setView(dialogBinding.root)
        builder.setNegativeButton("CANCEL"){dialogInterface,i -> dialogInterface.dismiss()}
                .setPositiveButton("REGISTER"){dialogInterface,i ->
                    if (TextUtils.isDigitsOnly(dialogBinding.edtName.text.toString())) {
                        Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val serverUserModel = ServerUserModel()
                    serverUserModel.uid = user.uid
                    serverUserModel.name = dialogBinding.edtName.text.toString()
                    serverUserModel.phone = dialogBinding.edtPhone.text.toString()
                    serverUserModel.isActive = false //By default, we set isActive is false

                    dialog.show()

                    serverRef.child(serverUserModel.uid!!)
                            .setValue(serverUserModel)
                            .addOnFailureListener(object : OnFailureListener {
                                override fun onFailure(p0: Exception) {
                                    dialog.dismiss()
                                    Toast.makeText(this@MainActivity, p0.message.toString(), Toast.LENGTH_SHORT).show()
                                }
                            }).addOnCompleteListener{task ->
                                dialog.dismiss()
                                Toast.makeText(this, "Congratulation! Register success! Admin wil check and active you soon", Toast.LENGTH_SHORT).show()
                                goToHomeActivity(serverUserModel)
                            }
                }

        val registerDialog = builder.create()
        registerDialog.show()

    }

    private fun goToHomeActivity(user: ServerUserModel?) {
        dialog.dismiss()
        Common.currentServerUser = user
        startActivity(Intent(this@MainActivity, HomeActivity::class.java))
        finish()

    }
}