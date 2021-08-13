package com.udacity.project4.authentication

import android.os.Looper.getMainLooper
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.PointOfInterest
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.robolectric.Shadows.shadowOf

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class AuthenticationViewModelTest : AutoCloseKoinTest() {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Subject under test
    private lateinit var authenticationViewModel: AuthenticationViewModel

    @Before
    fun setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
        authenticationViewModel = AuthenticationViewModel(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun login_launchSignInFlowEventEmitted() {
        // Start login process
        authenticationViewModel.login()

        // Assert that launchSignInFlow has been set
        assertThat(authenticationViewModel.launchSignInFlow.getOrAwaitValue(), `is`(Unit))
    }

    @Test
    fun logout_accountLoggedOut() {
        // Don't know how to test, respective question about testing AuthUI was deleted
        // from knowledge base as being off-topic
    }

    @Test
    fun onSignInResult_success() {
        val result = FirebaseAuthUIAuthenticationResult(AppCompatActivity.RESULT_OK, null)
        authenticationViewModel.onSignInResult(result)

        // Assert that back navigation command is issued
        assertThat(authenticationViewModel.navigationCommand.getOrAwaitValue(), `is`(NavigationCommand.Back))
    }

    @Test
    fun onSignInResult_cancel() {
        val result = FirebaseAuthUIAuthenticationResult(AppCompatActivity.RESULT_CANCELED, null)

        authenticationViewModel.onSignInResult(result)

        // Assert that sign in cancelled snackbar is shown
        assertThat(authenticationViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.auth_sign_in_cancelled))
    }

    @Test
    fun onSignInResult_unsuccessful() {
        val mockedIdpResponse = mock(IdpResponse::class.java)
        val result = FirebaseAuthUIAuthenticationResult(AppCompatActivity.RESULT_CANCELED, mockedIdpResponse)

        authenticationViewModel.onSignInResult(result)

        // Assert that sign in unsuccessful snackbar is shown
        assertThat(authenticationViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.auth_sign_in_unsuccessful))
    }

}