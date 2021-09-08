package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.succeeded
import com.udacity.project4.util.MainCoroutineRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var localDataSource: ReminderDataSource
    private lateinit var database: RemindersDatabase

    @Before
    fun setup() {
        // Using an in-memory database for testing, because it doesn't survive killing the process.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        localDataSource =
            RemindersLocalRepository(
                database.reminderDao(),
                Dispatchers.Main
            )
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveReminder_retrievesReminder() = mainCoroutineRule.runBlockingTest {
        // GIVEN - A new reminder is saved in the database.
        val newReminder = ReminderDTO(
            "Say hi to old friends",
            "Don't forget Jane and John",
            "TalTech",
            59.39604886086625,
            24.671091785402538,
            300.5
        )
        localDataSource.saveReminder(newReminder)

        // WHEN - Reminder is retrieved by ID.
        val result = localDataSource.getReminder(newReminder.id)

        // THEN - Same reminder is returned.
        assertThat(result.succeeded, `is`(true))
        result as Result.Success
        assertThat(result.data.title, `is`(newReminder.title))
        assertThat(result.data.description, `is`(newReminder.description))
        assertThat(result.data.location, `is`(newReminder.location))
        assertThat(result.data.latitude, `is`(newReminder.latitude))
        assertThat(result.data.longitude, `is`(newReminder.longitude))
        assertThat(result.data.radius, `is`(newReminder.radius))
    }

    @Test
    fun updateReminder_retrievedReminderIsUpdated() = mainCoroutineRule.runBlockingTest {
        // Save a new reminder in the database.
        val newReminder = ReminderDTO("Say hi", null, null, null, null, null)
        localDataSource.saveReminder(newReminder)

        // Update the reminder
        val updatedReminder = ReminderDTO(
            "Say hi to old friends",
            "Don't forget Jane and John",
            "TalTech",
            59.39604886086625,
            24.671091785402538,
            300.5,
            newReminder.id
        )
        localDataSource.saveReminder(updatedReminder)
        val result = localDataSource.getReminder(newReminder.id)

        // THEN - Same reminder is returned.
        assertThat(result.succeeded, `is`(true))
        result as Result.Success
        assertThat(result.data.title, `is`(updatedReminder.title))
        assertThat(result.data.description, `is`(updatedReminder.description))
        assertThat(result.data.location, `is`(updatedReminder.location))
        assertThat(result.data.latitude, `is`(updatedReminder.latitude))
        assertThat(result.data.longitude, `is`(updatedReminder.longitude))
        assertThat(result.data.radius, `is`(updatedReminder.radius))
    }

    @Test
    fun deleteReminder_retrievesErrorResult() = mainCoroutineRule.runBlockingTest {
        // Save a new reminder in the database.
        val newReminder = ReminderDTO("Say hi", null, null, null, null, null)
        localDataSource.saveReminder(newReminder)

        // Delete the reminder and try to retrieve it
        localDataSource.deleteReminder(newReminder.id)
        val result = localDataSource.getReminder(newReminder.id)

        // Error result is returned
        assertThat(result.succeeded, `is`(false))
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
        assertThat(result.statusCode, `is`(nullValue()))
    }

    @Test
    fun getNonExistentReminder_retrievesErrorResult() = mainCoroutineRule.runBlockingTest {
        // Create a new reminder, but do not save it to database
        val newReminder = ReminderDTO(null, null, null, null, null, null)

        // Try to retrieve that reminder by it ID.
        val result = localDataSource.getReminder(newReminder.id)

        // Error result is returned.
        assertThat(result.succeeded, `is`(false))
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
        assertThat(result.statusCode, `is`(nullValue()))
    }

    @Test
    fun getRemindersFromEmptyDataSource_retrievesEmptyListOfReminders() =
        mainCoroutineRule.runBlockingTest {
            // Do not add anything to the data source

            // Try to retrieve all reminders
            val result = localDataSource.getReminders()

            // Result contains no reminders.
            assertThat(result.succeeded, `is`(true))
            result as Result.Success
            assertThat(result.data.size, `is`(0))
            assertThat(result.data, `is`(listOf()))
        }

    @Test
    fun saveMultipleReminders_retrievesAllSavedReminders() = mainCoroutineRule.runBlockingTest {
        // Save multiple reminders.
        val reminder1 = ReminderDTO(
            "Say hi to old friends",
            "Don't forget Jane and John",
            "TalTech",
            59.39604886086625,
            24.671091785402538,
            300.5
        )
        val reminder2 = ReminderDTO(
            "Say hello to new friends",
            "Including Jenny and Johnny",
            "TalTech Library",
            59.39706415258735,
            24.671131154003252,
            199.0
        )
        val reminder3 = ReminderDTO(
            "Greet current friends",
            "But not Jenkins",
            "TalTech Sports Hall",
            59.39370506890589,
            24.677392134314477,
            101.0
        )
        localDataSource.saveReminder(reminder1)
        localDataSource.saveReminder(reminder2)
        localDataSource.saveReminder(reminder3)
        val reminders = listOf(reminder1, reminder2, reminder3)

        // Try to retrieve all reminders
        val result = localDataSource.getReminders()

        // Result contains no reminders.
        assertThat(result.succeeded, `is`(true))
        result as Result.Success
        assertThat(result.data.size, `is`(reminders.size))
        assertThat(result.data, `is`(reminders))
    }

    @Test
    fun deleteAllReminders_retrievesEmptyListOfReminders() = mainCoroutineRule.runBlockingTest {
        // Save multiple reminders and then delete all reminders
        val reminder1 = ReminderDTO(
            "Say hi to old friends",
            "Don't forget Jane and John",
            "TalTech",
            59.39604886086625,
            24.671091785402538,
            300.5
        )
        val reminder2 = ReminderDTO(
            "Say hello to new friends",
            "Including Jenny and Johnny",
            "TalTech Library",
            59.39706415258735,
            24.671131154003252,
            199.0
        )
        val reminder3 = ReminderDTO(
            "Greet current friends",
            "But not Jenkins",
            "TalTech Sports Hall",
            59.39370506890589,
            24.677392134314477,
            101.0
        )
        localDataSource.saveReminder(reminder1)
        localDataSource.saveReminder(reminder2)
        localDataSource.saveReminder(reminder3)
        localDataSource.deleteAllReminders()

        // Try to retrieve all reminders
        val result = localDataSource.getReminders()

        // Result contains no reminders.
        assertThat(result.succeeded, `is`(true))
        result as Result.Success
        assertThat(result.data.size, `is`(0))
        assertThat(result.data, `is`(listOf()))
    }

    @Test
    fun deleteAReminder_retrievedListOfRemindersDoesNotContainDeletedReminder() =
        mainCoroutineRule.runBlockingTest {
            // Save multiple reminders and then delete all reminders
            val reminder1 = ReminderDTO(
                "Say hi to old friends",
                "Don't forget Jane and John",
                "TalTech",
                59.39604886086625,
                24.671091785402538,
                300.5
            )
            val reminder2 = ReminderDTO(
                "Say hello to new friends",
                "Including Jenny and Johnny",
                "TalTech Library",
                59.39706415258735,
                24.671131154003252,
                199.0
            )
            val reminder3 = ReminderDTO(
                "Greet current friends",
                "But not Jenkins",
                "TalTech Sports Hall",
                59.39370506890589,
                24.677392134314477,
                101.0
            )
            localDataSource.saveReminder(reminder1)
            localDataSource.saveReminder(reminder2)
            localDataSource.saveReminder(reminder3)
            val reminders = listOf(reminder1, reminder3)

            // Delete one reminder and retrieve all reminders
            localDataSource.deleteReminder(reminder2.id)
            val result = localDataSource.getReminders()

            // Result contains all but the deleted reminder.
            assertThat(result.succeeded, `is`(true))
            result as Result.Success
            assertThat(result.data.size, `is`(reminders.size))
            assertThat(result.data, `is`(reminders))
        }

}