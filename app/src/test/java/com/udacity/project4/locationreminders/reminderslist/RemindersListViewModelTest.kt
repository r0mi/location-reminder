package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.asDataItem
import com.udacity.project4.locationreminders.asDataItemList
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
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

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest : AutoCloseKoinTest() {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val fakeReminders = listOf(
        ReminderDTO("Check the view", "On both sides of the bridge", "Golden Gate Bridge", null, null, null),
        ReminderDTO("Check the size", "Measure all sides", "Union Square", null, null, null),
        ReminderDTO("Try some seafood", "Lobster is a must", "Pier 39", null, null, null)
    )

    // Subject under test
    private lateinit var remindersListViewModel: RemindersListViewModel

    // Use a fake data source to be injected into the view model.
    private lateinit var reminderDataSource: FakeDataSource

    @Before
    fun setupReminderListViewModel() {
        // Initialise the repository with no tasks.
        reminderDataSource = FakeDataSource()
        reminderDataSource.addReminders(fakeReminders)

        remindersListViewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), reminderDataSource)
    }

    @Test
    fun loadReminders_loadingShowedAndRemindersLoaded() {
        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // Load the reminders in the view model.
        remindersListViewModel.loadReminders()

        // Then assert that the progress indicator is shown.
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))
        assertThat(remindersListViewModel.isRefreshing.getOrAwaitValue(), `is`(nullValue()))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        // Then assert that the progress indicators are hidden and correct data has been loaded
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(remindersListViewModel.isRefreshing.getOrAwaitValue(), `is`(false))
        assertThat(
            remindersListViewModel.remindersList.getOrAwaitValue(),
            `is`(fakeReminders.asDataItemList())
        )
    }

    @Test
    fun loadReminders_noDataShown() {
        runBlockingTest {
            reminderDataSource.deleteAllReminders()
        }

        // Load the reminders in the view model.
        remindersListViewModel.loadReminders()

        // Then assert that no data is shown
        assertThat(remindersListViewModel.showNoData.value, `is`(true))
    }


    @Test
    fun deleteReminder_loadingShowedReminderDeletedAndSnackbarShown() {
        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // Load the reminders in the view model.
        remindersListViewModel.deleteReminder(fakeReminders[0].asDataItem())

        // Then assert that the progress indicator is shown.
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        // Then assert that the progress indicator is hidden, the reminder is deleted and snackbar is shown
        val updatedRemidnersList = fakeReminders.toMutableList()
        updatedRemidnersList.removeFirst()
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(
            remindersListViewModel.remindersList.getOrAwaitValue(),
            `is`(updatedRemidnersList.asDataItemList())
        )
        assertThat(
            remindersListViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.reminder_deleted)
        )
    }
}