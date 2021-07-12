package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import androidx.transition.Fade
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationFragment
import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentRemindersBinding
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setTitle
import com.udacity.project4.utils.setup
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ReminderListFragment : BaseFragment() {
    //use Koin to retrieve the ViewModel instance
    override val _viewModel: RemindersListViewModel by viewModel()
    private val authenticationViewModel: AuthenticationViewModel by viewModel()
    private lateinit var binding: FragmentRemindersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = Fade().apply {
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        }
        enterTransition = Fade().apply {
            duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        }
        returnTransition = Fade().apply {
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        }

        val navController = findNavController()
        val currentBackStackEntry = navController.currentBackStackEntry!!
        val savedStateHandle = currentBackStackEntry.savedStateHandle
        savedStateHandle.getLiveData<Boolean>(AuthenticationFragment.LOGIN_SUCCESSFUL)
            .observe(currentBackStackEntry, { success ->
                if (!success) {
                    Timber.w("User chose not to log in, so close app")
                    ActivityCompat.finishAffinity(requireActivity())
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_reminders, container, false
            )
        binding.viewModel = _viewModel

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(false)
        setTitle(getString(R.string.app_name))

        binding.refreshLayout.setOnRefreshListener { _viewModel.loadReminders() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = findNavController()
        authenticationViewModel.authenticationState.observe(viewLifecycleOwner, { authenticationState ->
            when (authenticationState) {
                AuthenticationViewModel.AuthenticationState.UNAUTHENTICATED -> {
                    Timber.d("No user logged in, go to authentication")
                    navigateToAuthentication()
                }
                else -> {
                    Timber.d("Logged in as user ${authenticationViewModel.user.value?.displayName}")
                }
            }
        })
        binding.lifecycleOwner = this
        setupRecyclerView()
        binding.addReminderFAB.setOnClickListener {
            navigateToAddReminder()
        }
    }

    override fun onResume() {
        super.onResume()
        //load the reminders list on the ui
        _viewModel.loadReminders()
    }

    private fun navigateToAddReminder() {
        //use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                ReminderListFragmentDirections.toSaveReminder()
            )
        )
    }

    private fun navigateToAuthentication() {
        _viewModel.navigationCommand.postValue(
            NavigationCommand.ToId(
                R.id.authenticationFragment
            )
        )
    }

    private fun setupRecyclerView() {
        val adapter = RemindersListAdapter {
        }

//        setup the recycler view using the extension function
        binding.remindersRecyclerView.setup(adapter)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                authenticationViewModel.logout()
            }
        }
        return super.onOptionsItemSelected(item)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
//        display logout as menu item
        inflater.inflate(R.menu.main_menu, menu)
    }

}
