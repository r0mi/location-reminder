package com.udacity.project4.locationreminders.savereminder

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.transition.Slide
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

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
//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
            _viewModel.validateAndSaveReminder(reminder)
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
}
