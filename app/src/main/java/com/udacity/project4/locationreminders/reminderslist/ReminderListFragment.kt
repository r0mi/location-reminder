package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.transition.Fade
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationFragment
import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentRemindersBinding
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setTitle
import com.udacity.project4.utils.setup
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ReminderListFragment : BaseFragment() {
    // use Koin to retrieve the ViewModel instance
    override val _viewModel: RemindersListViewModel by sharedViewModel()
    private val authenticationViewModel: AuthenticationViewModel by viewModel()
    private lateinit var binding: FragmentRemindersBinding
    private lateinit var geofencingClient: GeofencingClient

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

        try {
            // Wrap in try/catch because instrumented test cannot initialize navigation
            // controller before onViewCreated call
            val navController = findNavController()
            val currentBackStackEntry = navController.currentBackStackEntry!!
            val savedStateHandle = currentBackStackEntry.savedStateHandle
            savedStateHandle.getLiveData<Boolean>(AuthenticationFragment.LOGIN_SUCCESSFUL)
                .observe(currentBackStackEntry, { success ->
                    if (!success) {
                        Timber.w("User chose not to log in, so close app")
                        ActivityCompat.finishAffinity(requireActivity())
                    } else {
                        _viewModel.showSnackBarInt.value = R.string.auth_sign_in_successful
                    }
                })
        } catch (e: IllegalStateException) {
            Timber.e(e)
        }

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
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
        authenticationViewModel.authenticationState.observe(
            viewLifecycleOwner,
            { authenticationState ->
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
        // load the reminders list on the ui
        _viewModel.loadReminders()
    }

    private fun navigateToAddReminder() {
        // use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                ReminderListFragmentDirections.toSaveReminder()
            )
        )
    }

    private fun navigateToEditReminder(reminder: ReminderDataItem) {
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                ReminderListFragmentDirections.toSaveReminder(reminder = reminder)
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

        // setup the recycler view using the extension function
        binding.remindersRecyclerView.setup(adapter, binding.refreshLayout) { reminder, direction ->
            if (direction == ItemTouchHelper.LEFT) { // delete
                _viewModel.deleteReminder(reminder)
                deleteGeofence(reminder)
            } else if (direction == ItemTouchHelper.RIGHT) { // edit
                navigateToEditReminder(reminder)
            }
        }
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
        // display logout as menu item
        inflater.inflate(R.menu.main_menu, menu)
    }

    private fun deleteGeofence(reminder: ReminderDataItem) {
        geofencingClient.removeGeofences(listOf(reminder.id)).run {
            addOnSuccessListener {
                Timber.d("Geofence ${reminder.id} deleted")
            }
            addOnFailureListener {
                _viewModel.showErrorMessage.value = getString(R.string.geofence_not_deleted)
                if ((it.message != null)) {
                    Timber.w("Failed to delete Geofence: ${it.message.toString()}")
                }
            }
        }
    }

}
