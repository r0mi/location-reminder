package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.base.DataBindingViewHolder
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.PointOfInterest
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.ToastMatcher.Companion.onToast
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                AuthenticationViewModel(
                    appContext
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()
        saveReminderViewModel = get()
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun test00_cancelLogin() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Clear all the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }

        // Verify: We are on login screen
        val loginButton = onView(allOf(withId(R.id.auth_button), withText("Login"), isDisplayed()))

        // Click on login button
        loginButton.perform(click())

        runBlocking {
            delay(3000) // Provide time for the user to manually cancel the Google Smart Lock screen
        }

        // Cancel login by navigating back
        pressBack()

        // Verify sign in cancelled message is shown
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.auth_sign_in_cancelled)))

        // When using ActivityScenario.launch(), always call close()
        activityScenario.close()
    }

    @Test
    fun test01_successfulLogin() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Verify: We are on login screen
        val loginButton = onView(allOf(withId(R.id.auth_button), withText("Login"), isDisplayed()))

        // Click on login button
        loginButton.perform(click())

        runBlocking {
            delay(3000) // Provide time for the user to manually cancel the Google Smart Lock screen
        }

        // Sign in using e-mail and password
        onView(allOf(withId(R.id.email_button), withText("Sign in with email")))
            .perform(scrollTo(), click())

        runBlocking {
            delay(3000) // Again provide time for the user to manually cancel the Google Smart Lock screen
        }

        // Input the test account e-mail address
        onView(withId(R.id.email))
            .perform(
                scrollTo(),
                replaceText("test@test.com"),
                ViewActions.closeSoftKeyboard()
            )

        // Proceed by clicking "Next"
        onView(allOf(withId(R.id.button_next), withText("Next")))
            .perform(scrollTo(), click())

        runBlocking {
            delay(1000) // Add timeout for network delays
        }

        // Input the test account password
        onView(withId(R.id.password))
            .perform(
                scrollTo(), replaceText("T3st3r"),
                ViewActions.closeSoftKeyboard()
            )

        // Click on "Sign in" button to log in
        onView(allOf(withId(R.id.button_done), withText("Sign in")))
            .perform(scrollTo(), click())

        runBlocking {
            delay(2000) // Add timeout for network delays
        }

        // Verify sign in success message is shown
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.auth_sign_in_successful)))

        // Verify that "Logout" button is visible
        onView(withId(R.id.logout)).check(matches(isDisplayed()))

        // When using ActivityScenario.launch(), always call close()
        activityScenario.close()
    }

    @Test
    fun test02_addReminderWithEmptyTitle() = runBlocking {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Verify: no data is shown
        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        // Click add new task
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Save reminder
        onView(withId(R.id.saveReminder)).perform(click())

        // Verify error message is shown
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))

        // Navigate back
        pressBack()

        // Verify nothing has been saved and no data is shown
        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        // When using ActivityScenario.launch(), always call close()
        activityScenario.close()
    }

    @Test
    fun test03_addReminderWithEmptyLocation() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Verify: no data is shown
        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        // Click add new task
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Type in the title & description
        onView(withId(R.id.reminderTitle)).perform(typeText("Title"))
        onView(withId(R.id.reminderDescription)).perform(typeText("Description"))
        closeSoftKeyboard()

        // Save reminder
        onView(withId(R.id.saveReminder)).perform(click())

        // Verify error message is shown
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_select_location)))

        // Navigate back
        pressBack()

        // Verify nothing has been saved and no data is shown
        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        // When using ActivityScenario.launch(), always call close()
        activityScenario.close()
    }

    @Test
    fun test04_saveEmptyLocation() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click add new task
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Select location
        onView(withId(R.id.selectLocation)).perform(click())

        // Verify snackbar message is shown
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.select_poi)))

        // Wait for the initial snackbar to hide
        runBlocking {
            delay(3000)
        }

        // Save location
        onView(withId(R.id.save_button)).perform(click())

        // Verify snackbar message is shown again
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.select_poi)))

        // When using ActivityScenario.launch(), always call close()
        activityScenario.close()
    }

    @Test
    fun test05_selectLocation() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click add new task
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Click to select location
        onView(withId(R.id.selectLocation)).perform(click())

        // Wait for the map to load
        runBlocking {
            delay(2000)
        }

        // Long click on map to select a POI
        onView(withId(R.id.map)).perform(longClick())

        // Save location
        onView(withId(R.id.save_button)).perform(click())

        // Verify snackbar message is shown
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.location_added)))

        // When using ActivityScenario.launch(), always call close()
        activityScenario.close()
    }

    @Test
    fun test06_addReminder() {
        val reminderTitle = "Title"
        val reminderDescription = "Description"

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Verify: no data is shown
        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        // Click add new task
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Type in the title & description
        onView(withId(R.id.reminderTitle)).perform(typeText(reminderTitle))
        onView(withId(R.id.reminderDescription)).perform(typeText(reminderDescription))
        closeSoftKeyboard()

        // Select location
        onView(withId(R.id.selectLocation)).perform(click())

        // Wait for the map to load
        runBlocking {
            delay(2000)
        }

        // Long click on map to select a POI
        onView(withId(R.id.map)).perform(longClick())

        // Save location
        onView(withId(R.id.save_button)).perform(click())

        // Verify snackbar message is shown
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.location_added)))

        // Get selected location
        val selectedLocation = saveReminderViewModel.reminderLocationStr.value

        // Verify selected location string is displayed
        onView(withText(selectedLocation)).check(matches(isDisplayed()))

        // Save reminder
        onView(withId(R.id.saveReminder)).perform(click())

        // Verify reminder saved toast is displayed
        onToast(R.string.reminder_saved).check(matches(isDisplayed()))

        // Verify added reminder is displayed
        onView(withText(reminderTitle)).check(matches(isDisplayed()))
        onView(withText(reminderDescription)).check(matches(isDisplayed()))
        onView(withText(selectedLocation)).check(matches(isDisplayed()))

        // When using ActivityScenario.launch(), always call close()
        activityScenario.close()
    }

    @Test
    fun test07_addReminderWithoutDescription() {
        val reminderTitle = "Only Title"
        val locationString = "Franklin Dental Group"

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click add new task
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Type in the title
        onView(withId(R.id.reminderTitle)).perform(typeText(reminderTitle))
        closeSoftKeyboard()

        // Add location programmatically
        saveReminderViewModel.selectedPOI.postValue(
            PointOfInterest(
                LatLng(
                    37.8005840,
                    -122.4262300
                ), 300.0, locationString
            )
        )

        // Verify location string is displayed
        onView(withText(locationString)).check(matches(isDisplayed()))

        // Save reminder
        onView(withId(R.id.saveReminder)).perform(click())

        // Verify reminder saved toast is displayed
        onToast(R.string.reminder_saved).check(matches(isDisplayed()))

        // Verify added reminder is displayed
        onView(withText(reminderTitle)).check(matches(isDisplayed()))
        onView(withText(locationString)).check(matches(isDisplayed()))

        // When using ActivityScenario.launch(), always call close()
        activityScenario.close()
    }

    @Test
    fun test08_editReminder() {
        val reminderOldTitle = "Only Title"
        val reminderOldLocationString = "Franklin Dental Group"
        val reminderNewTitle = "New Title"
        val reminderNewDescription = "New Description"

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Verify the reminder to be edited is displayed
        onView(withText(reminderOldTitle)).check(matches(isDisplayed()))
        onView(withText(reminderOldLocationString)).check(matches(isDisplayed()))

        // Swipe right on the specific reminder
        onView(withId(R.id.remindersRecyclerView)).perform(
            RecyclerViewActions.actionOnItem<DataBindingViewHolder<ReminderDataItem>>(
                allOf(
                    hasDescendant(withText(reminderOldTitle)),
                    hasDescendant(withText(reminderOldLocationString))
                ),
                swipeRight()
            )
        )

        // Verify that we opened the edit page for the specific reminder
        onView(withId(R.id.saveReminder)).check(matches(isDisplayed()))
        onView(withText(reminderOldTitle)).check(matches(isDisplayed()))
        onView(withText(reminderOldLocationString)).check(matches(isDisplayed()))

        // Type in the new title & description
        onView(withId(R.id.reminderTitle)).perform(replaceText(reminderNewTitle))
        onView(withId(R.id.reminderDescription)).perform(replaceText(reminderNewDescription))
        closeSoftKeyboard()

        // Select location
        onView(withId(R.id.selectLocation)).perform(click())

        // Wait for the map to load
        runBlocking {
            delay(2000)
        }

        // Open the overflow menu
        onView(allOf(withContentDescription("More options"), isDisplayed())).perform(click())

        // Clear the map
        onView(allOf(withId(R.id.title), withText("Clear Map"), isDisplayed())).perform(click())

        // Move to device current location
        onView(allOf(withContentDescription("My Location"), isDisplayed())).perform(click())

        // Long click on map to select a POI at current location
        onView(withId(R.id.map)).perform(longClick())

        // Save location
        onView(withId(R.id.save_button)).perform(click())

        // Verify snackbar message is shown
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.location_updated)))

        // Get selected location
        val selectedLocation = saveReminderViewModel.reminderLocationStr.value

        // Verify selected location string is displayed
        onView(withText(selectedLocation)).check(matches(isDisplayed()))

        // Save reminder
        onView(withId(R.id.saveReminder)).perform(click())

        // Reminder saved toast
        onToast(R.string.reminder_saved).check(matches(isDisplayed()))

        // Verify updated reminder is displayed
        onView(withText(reminderNewTitle)).check(matches(isDisplayed()))
        onView(withText(reminderNewDescription)).check(matches(isDisplayed()))

        // When using ActivityScenario.launch(), always call close()
        activityScenario.close()
    }

    @Test
    fun test09_deleteReminder() {
        val reminderTitle = "New Title"
        val reminderDescription = "New Description"

        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Verify reminder to be deleted is displayed
        onView(withText(reminderTitle)).check(matches(isDisplayed()))
        onView(withText(reminderDescription)).check(matches(isDisplayed()))

        // Swipe left on the specific reminder
        onView(withId(R.id.remindersRecyclerView)).perform(
            RecyclerViewActions.actionOnItem<DataBindingViewHolder<ReminderDataItem>>(
                allOf(
                    hasDescendant(withText(reminderTitle)),
                    hasDescendant(withText(reminderDescription))
                ),
                swipeLeft()
            )
        )

        // Verify snackbar message is shown
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.reminder_deleted)))

        // Verify deleted reminder is not displayed
        onView(withText(reminderTitle)).check(doesNotExist())
        onView(withText(reminderDescription)).check(doesNotExist())

        // When using ActivityScenario.launch(), always call close()
        activityScenario.close()
    }

    @Test
    fun test10_logout() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Verify: logout menu button is shown
        onView(withId(R.id.logout)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        // Click on logout menu button
        onView(withId(R.id.logout)).perform(click())

        // Verify: We are on login screen
        onView(withId(R.id.auth_button)).check(matches(isDisplayed()))

        // Verify "signed out" message is shown
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.auth_signed_out)))

        // When using ActivityScenario.launch(), always call close()
        activityScenario.close()
    }

}
