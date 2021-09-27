package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.asDTO
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.succeeded
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest : AutoCloseKoinTest() {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    // Use a fake data source to be injected into the view model.
    private lateinit var reminderDataSource: FakeDataSource

    @Before
    fun setupSaveReminderViewModel() {
        // Initialise the repository with no tasks.
        reminderDataSource = FakeDataSource()

        saveReminderViewModel =
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), reminderDataSource)
    }

    @Test
    fun setSelectedPOI_selectedLocationStringIsSet() {
        // Set selected POI
        val reminder = ReminderDataItem(
            "Check the view",
            "On both sides of the bridge",
            "Golden Gate Bridge",
            37.819927,
            -122.478256,
            300.0
        )
        saveReminderViewModel.selectedPOI.value = PointOfInterest(
            LatLng(reminder.latitude!!, reminder.longitude!!),
            reminder.radius!!,
            reminder.location!!
        )

        // Assert that selected location string has been set
        assertThat(
            saveReminderViewModel.reminderLocationStr.getOrAwaitValue(),
            `is`(reminder.location)
        )
    }

    @Test
    fun clearAllPOIData_allPOIsAreDeleted() {
        // Create some POI coordinates
        val poi1 = LatLng(37.819927, -122.478256)
        val poi2 = LatLng(37.808674, -122.409821)
        val poi3 = LatLng(37.795490, -122.394276)

        // Add coordinates to POI list
        saveReminderViewModel.addLatLngIfNotInList(poi1)
        saveReminderViewModel.addLatLngIfNotInList(poi2)
        saveReminderViewModel.addLatLngIfNotInList(poi3)

        // Mark current POIs to be persisted
        saveReminderViewModel.persistPOIData()

        // Delete POIs which have not been marked to be persisted
        saveReminderViewModel.clearNotPersistedPOIData()

        // Assert that all POIs are still in list
        assertThat(
            saveReminderViewModel.listOfLatLngs.getOrAwaitValue(),
            `is`(listOf(poi1, poi2, poi3))
        )

        // Delete all POIs
        saveReminderViewModel.clearAllPOIData()

        // Assert that all POIs have been removed
        assertThat(saveReminderViewModel.listOfLatLngs.getOrAwaitValue(), `is`(emptyList()))
    }

    @Test
    fun persistPOIData_poisArePersisted() {
        // Create some POI coordinates
        val poi1 = LatLng(37.819927, -122.478256)
        val poi2 = LatLng(37.808674, -122.409821)
        val poi3 = LatLng(37.795490, -122.394276)

        // Add coordinates to POI list
        saveReminderViewModel.addLatLngIfNotInList(poi1)
        saveReminderViewModel.addLatLngIfNotInList(poi2)
        saveReminderViewModel.addLatLngIfNotInList(poi3)

        // Delete POIs which have not been marked to be persisted
        saveReminderViewModel.clearNotPersistedPOIData()

        // Assert that all POIs have been removed
        assertThat(saveReminderViewModel.listOfLatLngs.getOrAwaitValue(), `is`(emptyList()))

        // Add the same coordinates to POI list again
        saveReminderViewModel.addLatLngIfNotInList(poi1)
        saveReminderViewModel.addLatLngIfNotInList(poi2)
        saveReminderViewModel.addLatLngIfNotInList(poi3)

        // Mark current POIs to be persisted
        saveReminderViewModel.persistPOIData()

        // Delete POIs which have not been marked to be persisted
        saveReminderViewModel.clearNotPersistedPOIData()

        // Assert that all POIs have been persisted
        assertThat(
            saveReminderViewModel.listOfLatLngs.getOrAwaitValue(),
            `is`(listOf(poi1, poi2, poi3))
        )
    }

    @Test
    fun clearNotPersistedPOIData_notPersistedLatLngsDeleted() {
        // Create some POI coordinates
        val poi1 = LatLng(37.819927, -122.478256)
        val poi2 = LatLng(37.808674, -122.409821)
        val poi3 = LatLng(37.795490, -122.394276)
        val poi4 = LatLng(37.788151, -122.407570)

        // Add coordinates to POI list
        saveReminderViewModel.addLatLngIfNotInList(poi1)
        saveReminderViewModel.addLatLngIfNotInList(poi2)

        // Mark current POIs to be persisted
        saveReminderViewModel.persistPOIData()

        // Add additional coordinates to POI list
        saveReminderViewModel.addLatLngIfNotInList(poi3)
        saveReminderViewModel.addLatLngIfNotInList(poi4)

        // Delete POIs which have not been marked to be persisted
        saveReminderViewModel.clearNotPersistedPOIData()

        // Assert that all but persisted POIs have been removed
        assertThat(saveReminderViewModel.listOfLatLngs.getOrAwaitValue(), `is`(listOf(poi1, poi2)))
    }

    @Test
    fun clearNotPersistedPOIData_allLatLngsDeleted() {
        // Create some POI coordinates
        val poi1 = LatLng(37.819927, -122.478256)
        val poi2 = LatLng(37.808674, -122.409821)
        val poi3 = LatLng(37.795490, -122.394276)

        // Add coordinates to POI list
        saveReminderViewModel.addLatLngIfNotCloseElseDelete(poi1)
        saveReminderViewModel.addLatLngIfNotCloseElseDelete(poi2)
        saveReminderViewModel.addLatLngIfNotCloseElseDelete(poi3)

        // Delete POIs which have not been marked to be persisted
        saveReminderViewModel.clearNotPersistedPOIData()

        // Assert that all POIs have been removed
        assertThat(saveReminderViewModel.listOfLatLngs.getOrAwaitValue(), `is`(emptyList()))
    }

    @Test
    fun addLatLngIfNotCloseElseDelete_closeLatLngDeleted() {
        // Create some POI coordinates
        val poi1 = LatLng(37.819927, -122.478256)
        val poi2 = LatLng(37.808674, -122.409821)
        val poi3 = LatLng(37.795490, -122.394276)
        // Create a POI close to poi2
        val poi4 = LatLng(37.808717, -122.409536)

        // Add coordinates to POI list
        saveReminderViewModel.addLatLngIfNotCloseElseDelete(poi1)
        saveReminderViewModel.addLatLngIfNotCloseElseDelete(poi2)
        saveReminderViewModel.addLatLngIfNotCloseElseDelete(poi3)
        // Try to add coordinates close to poi2 (should delete poi2)
        saveReminderViewModel.addLatLngIfNotCloseElseDelete(poi4)

        // Assert that POI list contains all but the close coordinates
        assertThat(saveReminderViewModel.listOfLatLngs.getOrAwaitValue(), `is`(listOf(poi1, poi3)))
    }

    @Test
    fun addLatLngIfNotCloseElseDelete_allLatLngsAdded() {
        // Create some POI coordinates
        val poi1 = LatLng(37.819927, -122.478256)
        val poi2 = LatLng(37.808674, -122.409821)
        val poi3 = LatLng(37.795490, -122.394276)

        // Add coordinates to POI list
        saveReminderViewModel.addLatLngIfNotCloseElseDelete(poi1)
        saveReminderViewModel.addLatLngIfNotCloseElseDelete(poi2)
        saveReminderViewModel.addLatLngIfNotCloseElseDelete(poi3)

        // Assert that all POIs have been added
        assertThat(
            saveReminderViewModel.listOfLatLngs.getOrAwaitValue(),
            `is`(listOf(poi1, poi2, poi3))
        )
    }

    @Test
    fun addLatLngIfNotInList_duplicateLatLngNotAdded() {
        // Create some POI coordinates
        val poi1 = LatLng(37.819927, -122.478256)
        val poi2 = LatLng(37.808674, -122.409821)
        val poi3 = LatLng(37.795490, -122.394276)

        // Add coordinates to POI list
        saveReminderViewModel.addLatLngIfNotInList(poi3)
        saveReminderViewModel.addLatLngIfNotInList(poi1)
        saveReminderViewModel.addLatLngIfNotInList(poi2)
        // Try to add duplicate coordinates
        saveReminderViewModel.addLatLngIfNotInList(poi2)
        saveReminderViewModel.addLatLngIfNotInList(poi3)
        saveReminderViewModel.addLatLngIfNotInList(poi1)

        // Assert that all POIs but the duplicates have been added
        assertThat(
            saveReminderViewModel.listOfLatLngs.getOrAwaitValue(),
            `is`(listOf(poi3, poi1, poi2))
        )
    }

    @Test
    fun addLatLngIfNotInList_allLatLngsAdded() {
        // Create some POI coordinates
        val poi1 = LatLng(37.819927, -122.478256)
        val poi2 = LatLng(37.808674, -122.409821)
        val poi3 = LatLng(37.795490, -122.394276)

        // Add coordinates to POI list
        saveReminderViewModel.addLatLngIfNotInList(poi1)
        saveReminderViewModel.addLatLngIfNotInList(poi2)
        saveReminderViewModel.addLatLngIfNotInList(poi3)

        // Assert that all POIs have been added
        assertThat(
            saveReminderViewModel.listOfLatLngs.getOrAwaitValue(),
            `is`(listOf(poi1, poi2, poi3))
        )
    }

    @Test
    fun onClear_valuesCleared() {
        // Create a valid reminder and update viewmodel live data
        val reminder = ReminderDataItem(
            "Check the view",
            "On both sides of the bridge",
            "Golden Gate Bridge",
            37.819927,
            -122.478256,
            300.0
        )
        saveReminderViewModel.reminderTitle.value = reminder.title
        saveReminderViewModel.reminderDescription.value = reminder.description
        saveReminderViewModel.selectedPOI.value = PointOfInterest(
            LatLng(reminder.latitude!!, reminder.longitude!!),
            reminder.radius!!,
            reminder.location!!
        )
        saveReminderViewModel.reminderId.value = reminder.id
        saveReminderViewModel.addLatLngIfNotCloseElseDelete(
            LatLng(
                reminder.latitude!!,
                reminder.longitude!!
            )
        )

        // Clear viewmodel data
        saveReminderViewModel.onClear()

        // Assert that viewmodel data was cleared
        assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.reminderDescription.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.selectedPOI.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.reminderId.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.listOfLatLngs.getOrAwaitValue(), `is`(arrayListOf()))
    }

    @Test
    fun validateAndSaveReminder_reminderIsNotSavedAndSnackbarIsShownIfTitleMissing() =
        mainCoroutineRule.runBlockingTest {
            lateinit var returnedReminder: Result<ReminderDTO>
            // Create a reminder with null title
            var reminder = ReminderDataItem(
                null,
                "On both sides of the bridge",
                "Golden Gate Bridge",
                null,
                null,
                null
            )

            // Then validate and save the reminder & get it from the data source
            saveReminderViewModel.validateAndSaveReminder(reminder)
            returnedReminder = reminderDataSource.getReminder(reminder.id)

            // Assert that snackbar is shown and reminder has not been saved
            assertThat(
                saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
                `is`(R.string.err_enter_title)
            )
            assertThat(returnedReminder.succeeded, `is`(false))

            // Create a reminder with empty title
            reminder = ReminderDataItem(
                "",
                "On both sides of the bridge",
                "Golden Gate Bridge",
                null,
                null,
                null
            )

            // Then validate and save the reminder & get it from the data source
            saveReminderViewModel.validateAndSaveReminder(reminder)
            returnedReminder = reminderDataSource.getReminder(reminder.id)

            // Assert that snackbar is shown and reminder has not been saved
            assertThat(
                saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
                `is`(R.string.err_enter_title)
            )
            assertThat(returnedReminder.succeeded, `is`(false))
        }

    @Test
    fun validateAndSaveReminder_reminderIsNotSavedAndSnackbarIsShownIfLocationMissing() =
        mainCoroutineRule.runBlockingTest {
            lateinit var returnedReminder: Result<ReminderDTO>
            // Create a reminder with null location
            var reminder = ReminderDataItem(
                "Check the view",
                "On both sides of the bridge",
                null,
                null,
                null,
                null
            )

            // Then validate and save the reminder & get it from the data source
            saveReminderViewModel.validateAndSaveReminder(reminder)
            returnedReminder = reminderDataSource.getReminder(reminder.id)

            // Assert that snackbar is shown and reminder has not been saved
            assertThat(
                saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
                `is`(R.string.err_select_location)
            )
            assertThat(returnedReminder.succeeded, `is`(false))

            // Create a reminder with empty location
            reminder = ReminderDataItem(
                "Check the view",
                "On both sides of the bridge",
                "",
                null,
                null,
                null
            )

            // Then validate and save the reminder & get it from the data source
            saveReminderViewModel.validateAndSaveReminder(reminder)
            returnedReminder = reminderDataSource.getReminder(reminder.id)

            // Assert that snackbar is shown and reminder has not been saved
            assertThat(
                saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
                `is`(R.string.err_select_location)
            )
            assertThat(returnedReminder.succeeded, `is`(false))
        }

    @Test
    fun validateAndSaveReminder_reminderIsSaved() = mainCoroutineRule.runBlockingTest {
        lateinit var returnedReminder: Result<ReminderDTO>
        // Create a valid reminder
        val reminder = ReminderDataItem(
            "Check the view",
            "On both sides of the bridge",
            "Golden Gate Bridge",
            37.819927,
            -122.478256,
            300.0
        )

        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // Validate and save the reminder
        saveReminderViewModel.validateAndSaveReminder(reminder)

        // Then assert that the progress indicator is shown.
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        returnedReminder = reminderDataSource.getReminder(reminder.id)

        // Then assert that the progress indicator is hidden, toast is shown, back navigation command issued and reminder was saved
        val app: Application = ApplicationProvider.getApplicationContext()
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(
            saveReminderViewModel.showToast.getOrAwaitValue(),
            `is`(app.getString(R.string.reminder_saved))
        )
        assertThat(
            saveReminderViewModel.navigationCommand.getOrAwaitValue(),
            `is`(NavigationCommand.Back)
        )
        assertThat(returnedReminder.succeeded, `is`(true))
        assertThat((returnedReminder as Result.Success<ReminderDTO>).data, `is`(reminder.asDTO()))
    }

    @Test
    fun saveReminder_loadingShowedToastShowedAndNavigatedBack() {
        // Create a valid reminder
        val reminder = ReminderDataItem(
            "Check the view",
            "On both sides of the bridge",
            "Golden Gate Bridge",
            37.819927,
            -122.478256,
            300.0
        )

        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // Save the reminder to data source.
        saveReminderViewModel.saveReminder(reminder)
        saveReminderViewModel.reminderTitle.value = reminder.title
        saveReminderViewModel.reminderDescription.value = reminder.description
        saveReminderViewModel.selectedPOI.value = PointOfInterest(
            LatLng(reminder.latitude!!, reminder.longitude!!),
            reminder.radius!!,
            reminder.location!!
        )
        saveReminderViewModel.reminderId.value = reminder.id
        saveReminderViewModel.addLatLngIfNotCloseElseDelete(
            LatLng(
                reminder.latitude!!,
                reminder.longitude!!
            )
        )

        // Then assert that the progress indicator is shown.
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        // Then assert that the progress indicator is hidden, toast is shown and back navigation command issued
        val app: Application = ApplicationProvider.getApplicationContext()
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(
            saveReminderViewModel.showToast.getOrAwaitValue(),
            `is`(app.getString(R.string.reminder_saved))
        )
        assertThat(
            saveReminderViewModel.navigationCommand.getOrAwaitValue(),
            `is`(NavigationCommand.Back)
        )

        //Assert that onClear was called
        assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.reminderDescription.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.selectedPOI.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.reminderId.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.listOfLatLngs.getOrAwaitValue(), `is`(arrayListOf()))
    }

    @Test
    fun validateEnteredData_returnsFalseAndShowsSnackbarIfTitleMissing() {
        // Create a reminder with null title
        var reminder = ReminderDataItem(
            null,
            "On both sides of the bridge",
            "Golden Gate Bridge",
            null,
            null,
            null
        )

        // And validate the reminder
        var isValid = saveReminderViewModel.validateEnteredData(reminder)

        // Assert that reminder is invalid and snackbar is shown
        assertThat(isValid, `is`(false))
        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_enter_title)
        )

        // Create a reminder with empty title
        reminder = ReminderDataItem(
            "",
            "On both sides of the bridge",
            "Golden Gate Bridge",
            null,
            null,
            null
        )

        // And validate the reminder
        isValid = saveReminderViewModel.validateEnteredData(reminder)

        // Assert that reminder is invalid and snackbar is shown
        assertThat(isValid, `is`(false))
        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_enter_title)
        )
    }

    @Test
    fun validateEnteredData_returnsFalseAndShowsSnackbarIfLocationMissing() {
        // Create a reminder with null location
        var reminder = ReminderDataItem(
            "Check the view",
            "On both sides of the bridge",
            null,
            null,
            null,
            null
        )

        // And validate the reminder
        var isValid = saveReminderViewModel.validateEnteredData(reminder)

        // Assert that reminder is invalid and snackbar is shown
        assertThat(isValid, `is`(false))
        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_select_location)
        )

        // Create a reminder with empty location
        reminder =
            ReminderDataItem("Check the view", "On both sides of the bridge", "", null, null, null)

        // And validate the reminder
        isValid = saveReminderViewModel.validateEnteredData(reminder)

        // Assert that reminder is invalid and snackbar is shown
        assertThat(isValid, `is`(false))
        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_select_location)
        )
    }

    @Test
    fun validateEnteredData_returnsTrue() {
        // Create a valid reminder
        val reminder = ReminderDataItem(
            "Check the view",
            "On both sides of the bridge",
            "Golden Gate Bridge",
            null,
            null,
            null
        )

        // And validate the reminder
        val isValid = saveReminderViewModel.validateEnteredData(reminder)

        // Assert that reminder is invalid and snackbar is shown
        assertThat(isValid, `is`(true))
    }

}