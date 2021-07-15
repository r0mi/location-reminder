package com.udacity.project4.locationreminders.savereminder

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.transition.Slide
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
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
            val description = _viewModel.reminderDescription
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude
            val longitude = _viewModel.longitude.value
            val radius = _viewModel.radius.value

//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
            // Make sure to clear the view model after saving the reminder and navigating away, as it's a single view model.
            //_viewModel.onClear()
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
    }
}
