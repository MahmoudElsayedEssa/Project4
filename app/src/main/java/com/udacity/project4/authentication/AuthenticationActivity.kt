package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.observe
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity


class AuthenticationActivity : AppCompatActivity() {

    companion object {
        const val SIGN_IN_REQUEST_CODE = 1001
        const val TAG: String = "AuthenticationActivity"
    }

    private val viewModel by viewModels<LoginViewModel>()

    private var _binding: ActivityAuthenticationBinding? = null
    val binding: ActivityAuthenticationBinding
        get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.authenticationState.observe(this) { authenticationState ->
            when (authenticationState) {
                AuthenticationState.AUTHENTICATED -> {
                    Log.i(
                        TAG,
                        "Authentication state: $authenticationState"
                    )

                    startRemindersActivity()
                }

                else -> {}
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    fun onLoginClick(view: View) {
        launchSignInFlow()
    }

    private fun launchSignInFlow() {

        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        openSomeActivityForResult(providers)
    }

    private fun openSomeActivityForResult(providers: ArrayList<AuthUI.IdpConfig>) {
        val intent =
            AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
        resultLauncher.launch(intent)
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            if (result.resultCode == SIGN_IN_REQUEST_CODE) {

                val response = IdpResponse.fromResultIntent(data)
                if (result.resultCode == Activity.RESULT_OK) {
                    Log.i(
                        TAG,
                        "Successfully signed in user " +
                                "${FirebaseAuth.getInstance().currentUser?.displayName}!"
                    )
                    startRemindersActivity()


                } else {

                    Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
                }
            }

        }

    private fun startRemindersActivity() {
        val intent = Intent(this, RemindersActivity::class.java)
        startActivity(intent)
    }
}
