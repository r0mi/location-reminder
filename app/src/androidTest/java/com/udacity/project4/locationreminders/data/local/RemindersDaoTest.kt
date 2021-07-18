package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminderAndGetById() = runBlockingTest {
        // GIVEN - Insert a reminder.
        val reminder = ReminderDTO(
            "Say hi to old friends",
            "Don't forget Jane and John",
            "TalTech",
            59.39604886086625,
            24.671091785402538,
            300.5
        )
        database.reminderDao().saveReminder(reminder)

        // WHEN - Get the reminder by id from the database.
        val loaded = database.reminderDao().getReminderById(reminder.id)

        // THEN - The loaded data contains the expected values.
        assertThat(loaded as ReminderDTO, Matchers.notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
        assertThat(loaded.radius, `is`(reminder.radius))
    }

    @Test
    fun updateReminderAndGetById() = runBlockingTest {
        // 1. Insert a reminder into the DAO.
        val originalReminder = ReminderDTO(
            "Say hi to old friends",
            "Don't forget Jane and John",
            "TalTech",
            59.39604886086625,
            24.671091785402538,
            300.5
        )
        database.reminderDao().saveReminder(originalReminder)

        // 2. Update the reminder by creating a new reminder with the same ID but different attributes.
        val updatedReminder = ReminderDTO(
            "Say hello to new friends",
            "Including Jenny and Johnny",
            "TalTech Library",
            59.39706415258735,
            24.671131154003252,
            199.0,
            originalReminder.id
        )
        database.reminderDao().saveReminder(updatedReminder)

        // 3. Check that when you get the reminder by its ID, it has the updated values.
        val loaded = database.reminderDao().getReminderById(originalReminder.id)
        assertThat(loaded?.id, `is`(originalReminder.id))
        assertThat(loaded?.title, `is`(updatedReminder.title))
        assertThat(loaded?.description, `is`(updatedReminder.description))
        assertThat(loaded?.location, `is`(updatedReminder.location))
        assertThat(loaded?.latitude, `is`(updatedReminder.latitude))
        assertThat(loaded?.longitude, `is`(updatedReminder.longitude))
        assertThat(loaded?.radius, `is`(updatedReminder.radius))
    }

    @Test
    fun insertReminderThenDeleteByIdAndGetById() = runBlockingTest {
        // Insert a reminder.
        val reminder = ReminderDTO(
            "Say hi to old friends",
            "Don't forget Jane and John",
            "TalTech",
            59.39604886086625,
            24.671091785402538,
            300.5
        )
        database.reminderDao().saveReminder(reminder)

        // Delete the reminder by id from the database.
        database.reminderDao().deleteReminderById(reminder.id)

        // Verify that the loaded data does not contain the saved reminder.
        val loaded = database.reminderDao().getReminderById(reminder.id)
        assertThat(loaded, nullValue())
    }

    @Test
    fun getNonExistentReminderById() = runBlockingTest {
        // Create a new reminder, but do not save it to database
        val newReminder = ReminderDTO(null, null, null, null, null, null)

        // Try to retrieve that reminder by it ID.
        val loaded = database.reminderDao().getReminderById(newReminder.id)

        // Null value is returned
        assertThat(loaded, `is`(nullValue()))
    }

    @Test
    fun getEmptyListOfReminders() = runBlockingTest {
        // GIVEN - an empty database.

        // WHEN - Get all reminders from database.
        val reminders = database.reminderDao().getReminders()

        // THEN - The loaded data contains no reminders.
        assertThat(reminders.size, `is`(0))
        assertThat(reminders, `is`(listOf()))
    }

    @Test
    fun insertMultipleRemindersAndRetrieveAll() = runBlockingTest {
        // Insert multiple reminders.
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
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)

        // Put all reminders to a list
        val reminders = listOf(reminder1, reminder2, reminder3)

        // Check that all reminders loaded from database match the inserted reminders.
        val loaded = database.reminderDao().getReminders()
        assertThat(loaded.size, `is`(reminders.size))
        assertThat(loaded, `is`(reminders))
    }


    @Test
    fun insertMultipleRemindersThenDeleteAndRetrieveAll() = runBlockingTest {
        // Insert multiple reminders.
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
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)

        // Delete all reminders
        database.reminderDao().deleteAllReminders()

        // Check that the loaded data contains no reminders.
        val loaded = database.reminderDao().getReminders()
        assertThat(loaded.size, `is`(0))
        assertThat(loaded, `is`(listOf()))
    }

    @Test
    fun insertMultipleRemindersThenDeleteOneByIdAndRetrieveAll() = runBlockingTest {
        // Insert multiple reminders.
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
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)
        val reminders = listOf(reminder1, reminder3)

        // Delete one reminder by id
        database.reminderDao().deleteReminderById(reminder2.id)

        // Check that the loaded data contains all but the deleted reminder.
        val loaded = database.reminderDao().getReminders()
        assertThat(loaded.size, `is`(reminders.size))
        assertThat(loaded, `is`(reminders))
    }

}