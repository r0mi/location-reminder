package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.transition.Slide
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.RemindersActivity.Companion.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
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
//            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(
                    SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment(
                        _viewModel.selectedPOI.value
                    )
                )
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.selectedPOI.value?.name
            val latitude = _viewModel.selectedPOI.value?.latLng?.latitude
            val longitude = _viewModel.selectedPOI.value?.latLng?.longitude
            val radius = _viewModel.selectedPOI.value?.radius
            val reminder = _viewModel.reminderId.value?.let {
                ReminderDataItem(title, description, location, latitude, longitude, radius, it)
            } ?: ReminderDataItem(title, description, location, latitude, longitude, radius)
            _viewModel.validateAndSaveReminder(reminder)
            if (_viewModel.validateEnteredData(reminder)) {
                addGeofence(reminder)
            }
        }

        val selectedPOI = SaveReminderFragmentArgs.fromBundle(requireArguments()).selectedPOI
        if (selectedPOI != null) {
            _viewModel.showSnackBarInt.value = if (_viewModel.selectedPOI.value == null) {
                R.string.location_added
            } else {
                R.string.location_updated
            }
            _viewModel.selectedPOI.value = selectedPOI
            arguments?.clear()
        }
        _viewModel.clearNotPersistedPOIData()

        val reminder = SaveReminderFragmentArgs.fromBundle(requireArguments()).reminder
        reminder?.let {
            _viewModel.reminderTitle.value = reminder.title
            _viewModel.reminderDescription.value = reminder.description
            _viewModel.selectedPOI.value = PointOfInterest(
                LatLng(it.latitude!!, it.longitude!!), it.radius!!, it.location!!
            )
            _viewModel.reminderId.value = reminder.id
            arguments?.clear()
        }
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(reminder: ReminderDataItem) {
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

        geofencingClient.removeGeofences(listOf(reminder.id)).run {
            addOnCompleteListener {
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                    addOnSuccessListener {
                        Timber.d("Added Geofence ${geofence.requestId}")
                    }
                    addOnFailureListener {
                        _viewModel.showErrorMessage.value = getString(R.string.geofence_not_added)
                        if ((it.message != null)) {
                            Timber.w("Failed to add Geofence: ${it.message.toString()}")
                        }
                    }
                }
            }
        }
    }
}
