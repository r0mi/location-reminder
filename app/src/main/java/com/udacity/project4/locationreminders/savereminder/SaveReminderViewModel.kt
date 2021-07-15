package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment.Companion.DELETE_POI_RADIUS_IN_METERS
import com.udacity.project4.utils.*
import kotlinx.coroutines.launch

class SaveReminderViewModel(val app: Application, val dataSource: ReminderDataSource) :
    BaseViewModel(app) {
    val reminderTitle = MutableLiveData<String>()
    val reminderDescription = MutableLiveData<String>()
    val selectedPOI = MutableLiveData<PointOfInterest>()
    val reminderSelectedLocationStr = Transformations.map(selectedPOI) { it?.name }

    private val _listOfLatLngs = MutableLiveData<MutableList<Pair<LatLng, Boolean>>>()

    val listOfLatLngs: LiveData<List<LatLng>> = Transformations.map(_listOfLatLngs) {
        it?.let {
            it.map { pair ->
                pair.first
            }
        } ?: arrayListOf()
    }

    fun clearAllPOIData() {
        _listOfLatLngs.clear()
    }

    fun persistPOIData() {
        _listOfLatLngs.value?.let {
            it.forEachIndexed { index, pair ->
                _listOfLatLngs.value?.set(index, Pair(pair.first, true))
            }
        }
    }

    fun clearNotPersistedPOIData() {
        _listOfLatLngs.value?.filter { it.second }?.let {
            _listOfLatLngs.value = it.toMutableList()
        }
    }

    fun addLatLngIfNotClose(latLng: LatLng) {
        val closePoint =
            _listOfLatLngs.value?.firstOrNull { it.first.distanceTo(latLng) <= DELETE_POI_RADIUS_IN_METERS }
        if (closePoint != null) {
            _listOfLatLngs -= closePoint
        } else {
            _listOfLatLngs += Pair(latLng, false)
        }
    }

    fun addLatLngIfNotInList(latLng: LatLng) {
        _listOfLatLngs.addIfNotExists(Pair(latLng, false))
    }

    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun onClear() {
        reminderTitle.value = null
        reminderDescription.value = null
        selectedPOI.value = null
        _listOfLatLngs.clear()
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder(reminderData: ReminderDataItem) {
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
        }
    }

    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.radius,
                    reminderData.id
                )
            )
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            navigationCommand.value = NavigationCommand.Back
            onClear()
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }
        return true
    }
}