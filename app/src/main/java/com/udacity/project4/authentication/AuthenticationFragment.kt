package com.udacity.project4.authentication

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.transition.Slide
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentAuthenticationBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class AuthenticationFragment : BaseFragment() {
    companion object {
        const val LOGIN_SUCCESSFUL: String = "LOGIN_SUCCESSFUL"
    }

    override val _viewModel: AuthenticationViewModel by viewModel()
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var binding: FragmentAuthenticationBinding
    private lateinit var navController: NavController

    private val signInLauncher =
        registerForActivityResult(FirebaseAuthUIActivityResultContract()) { res ->
            _viewModel.onSignInResult(res)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = Slide().apply {
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            slideEdge = Gravity.BOTTOM
        }
        enterTransition = Slide().apply {
            duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
            slideEdge = Gravity.BOTTOM
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_authentication,
            container,
            false
        )
        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = findNavController()

        savedStateHandle = navController.previousBackStackEntry!!.savedStateHandle
        savedStateHandle.set(LOGIN_SUCCESSFUL, false)

        _viewModel.launchSignInFlow.observe(viewLifecycleOwner, {
            launchSignInFlow()
        })

        _viewModel.authenticationState.observe(viewLifecycleOwner, { authenticationState ->
            when (authenticationState) {
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {
                    savedStateHandle.set(LOGIN_SUCCESSFUL, true)
                }
                else -> Timber.d("Authentication state that doesn't require any UI change $authenticationState")
            }
        })

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Timber.w("User chose not to log in, so close app")
                    ActivityCompat.finishAffinity(requireActivity())
                }
            })
    }

    private fun launchSignInFlow() {
        // Give users the option to sign in / register with their email or Google account. If users
        // choose to register with their email, they will need to create a password as well.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setLogo(R.drawable.map)
            .setTheme(R.style.LoginTheme)
            .setIsSmartLockEnabled(false)
            .build()
        signInLauncher.launch(signInIntent)
    }
}