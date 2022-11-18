package com.udacity.project4.locationreminders

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding

class AuthenticationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthenticationBinding
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_authentication
        )
        binding.lifecycleOwner = this
        binding.loginBtn.setOnClickListener {
            signin()

        }
    }

    private fun signin() {
        if(FirebaseAuth.getInstance().currentUser!= null){
            startReminderApp()
        }
        else{
            val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build(),
            )
            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setTheme(R.style.FirebaseUITheme)
                .setLogo(R.drawable.map)
                .build()
            signInLauncher.launch(signInIntent)
        }
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            val user = FirebaseAuth.getInstance().currentUser
            Toast.makeText(this, "Successfully signed in ${user!!.displayName}", Toast.LENGTH_SHORT)
                .show()
            startReminderApp()
        } else {
            Log.i("TAG", "Sign in unsuccessful ${response?.error?.errorCode}")
            Toast.makeText(this, "Sign in unsuccessful", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startReminderApp() {
        startActivity(Intent(this, RemindersActivity::class.java))
        finish()
    }

    public override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if(currentUser != null){
            startReminderApp()
        }
    }
}