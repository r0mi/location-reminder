package com.udacity.project4.authentication

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.map
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.utils.SingleLiveEvent
import timber.log.Timber

class AuthenticationViewModel(private val app: Application) : BaseViewModel(app) {
    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED
    }

    val launchSignInFlow: SingleLiveEvent<Unit> = SingleLiveEvent()
    val user = FirebaseUserLiveData()
    val authenticationState = user.map { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }

    fun login() {
        launchSignInFlow.value = Unit
    }

    fun logout() {
        AuthUI.getInstance()
            .signOut(app.applicationContext)
            .addOnCompleteListener {
                showSnackBarInt.value = R.string.auth_signed_out
            }
    }

    fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            navigationCommand.value = NavigationCommand.Back
            Timber.i("Successfully signed in user ${FirebaseAuth.getInstance().currentUser?.displayName}!")
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
            if (response == null) {
                showSnackBarInt.value = R.string.auth_sign_in_cancelled
                Timber.i("Sign in cancelled")
            } else {
                showSnackBarInt.value = R.string.auth_sign_in_unsuccessful
                Timber.e("Sign in unsuccessful. Error code ${response.error?.errorCode}, ${response.error.toString()}")
            }
        }
    }
}