package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.transition.Slide
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.RemindersActivity.Companion.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber

class SaveReminderFragment : BaseFragment() {
    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    private val startIntentSenderForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            checkDeviceLocationSettingsAndAddGeofenceRequest(false)
        }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val permissionsDenied = permissions.entries.any { it.value == false }

            if (permissionsDenied) {
                showIndefiniteSnackbarWithAction(
                    R.string.background_location_permission_rationale,
                    R.string.settings
                ) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            } else {
                checkDeviceLocationSettingsAndAddGeofenceRequest()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = Slide().apply {
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            slideEdge = Gravity.START
        }
        enterTransition = Slide().apply {
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            slideEdge = Gravity.END
        }
        returnTransition = Slide().apply {
            duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
            slideEdge = Gravity.END
        }
        if (SaveReminderFragmentArgs.fromBundle(requireArguments()).selectedPOI == null) {
            // The fragment is created with no argument the first time, so start with a fresh view model
            _viewModel.onClear()
        }
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.postValue(
                NavigationCommand.To(
                    SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment(
                        _viewModel.selectedPOI.value
                    )
                )
            )
        }

        binding.saveReminder.setOnClickListener {
            if (_viewModel.validateEnteredData(getReminderDataItem())) {
                checkPermissionsAndAddGeofenceRequest()
            }
        }

        val selectedPOI = SaveReminderFragmentArgs.fromBundle(requireArguments()).selectedPOI
        if (selectedPOI != null) {
            _viewModel.showSnackBarInt.postValue(
                if (_viewModel.selectedPOI.value == null) {
                    R.string.location_added
                } else {
                    R.string.location_updated
                }
            )
            _viewModel.selectedPOI.postValue(selectedPOI)
            arguments?.clear()
        }
        _viewModel.clearNotPersistedPOIData()

        val reminder = SaveReminderFragmentArgs.fromBundle(requireArguments()).reminder
        reminder?.let {
            _viewModel.reminderTitle.postValue(reminder.title)
            _viewModel.reminderDescription.postValue(reminder.description)
            _viewModel.selectedPOI.postValue(
                PointOfInterest(
                    LatLng(it.latitude!!, it.longitude!!), it.radius!!, it.location!!
                )
            )
            _viewModel.reminderId.postValue(reminder.id)
            arguments?.clear()
        }
    }

    private fun getReminderDataItem(): ReminderDataItem {
        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        val location = _viewModel.selectedPOI.value?.name
        val latitude = _viewModel.selectedPOI.value?.latLng?.latitude
        val longitude = _viewModel.selectedPOI.value?.latLng?.longitude
        val radius = _viewModel.selectedPOI.value?.radius
        return _viewModel.reminderId.value?.let {
            ReminderDataItem(title, description, location, latitude, longitude, radius, it)
        } ?: ReminderDataItem(title, description, location, latitude, longitude, radius)
    }

    private fun checkPermissionsAndAddGeofenceRequest() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndAddGeofenceRequest()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettingsAndAddGeofenceRequest(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    startIntentSenderForResult.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Timber.d("Error getting location settings resolution: %s", sendEx.message)
                }
            } else {
                showIndefiniteSnackbarWithAction(
                    R.string.location_service_rationale_for_save_reminder_fragment,
                    R.string.enable
                ) {
                    checkDeviceLocationSettingsAndAddGeofenceRequest()
                }
            }
        }.addOnCompleteListener {
            if (it.isSuccessful) {
                val reminder = getReminderDataItem()
                _viewModel.validateAndSaveReminder(reminder)
                addGeofence(reminder)
            }
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireActivity(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (runningQOrLater) {
            permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
        }
        requestMultiplePermissions.launch(permissionsArray)
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(reminder: ReminderDataItem) {
        val geofence = Geofence.Builder()
            .setRequestId(reminder.id)
            .setCircularRegion(
                reminder.latitude!!,
                reminder.longitude!!,
                reminder.radius!!.toFloat()
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        // Remove the existing geofence associated with the current reminder
        // If such a geofence exists (should exist only when editing an existing reminder),
        // we will remove it and replace it with an updated version
        geofencingClient.removeGeofences(listOf(reminder.id)).run {
            addOnCompleteListener {
                // I use explicitly RemindersListViewModel to show geofence added/not added snackbar
                // on the RemindersListFragment
                val remindersListViewModel: RemindersListViewModel by sharedViewModel()
                if (foregroundAndBackgroundLocationPermissionApproved()) {
                    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                        addOnSuccessListener {
                            Timber.d("Added Geofence ${geofence.requestId}")
                            remindersListViewModel.showSnackBarInt.postValue(R.string.geofence_added)
                        }
                        addOnFailureListener {
                            remindersListViewModel.showSnackBarInt.postValue(R.string.geofence_not_added)
                            if ((it.message != null)) {
                                Timber.w("Failed to add Geofence: ${it.message.toString()}")
                            }
                        }
                    }
                } else {
                    remindersListViewModel.showSnackBarInt.postValue(R.string.geofence_not_added)
                }
            }
        }
    }
}
