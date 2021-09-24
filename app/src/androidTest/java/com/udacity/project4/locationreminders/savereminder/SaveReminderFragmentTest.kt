package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.succeeded
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.ToastMatcher.Companion.onToast
import com.udacity.project4.util.asDataItem
import com.udacity.project4.util.monitorFragment
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class SaveReminderFragmentTest : AutoCloseKoinTest() {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    // An idling resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun init() {
        stopKoin() //stop the original app koin
        appContext = ApplicationProvider.getApplicationContext()
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
            single { RemindersLocalRepository(get(), Dispatchers.Main) as ReminderDataSource }
            single {
                Room.inMemoryDatabaseBuilder(appContext, RemindersDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
                    .reminderDao()
            }
        }

        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
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
    fun newReminder_emptyUiDisplayed() {
        // WHEN - Save reminder fragment is launched without arguments
        val bundle = SaveReminderFragmentArgs().toBundle()
        val scenario = launchFragmentInContainer<SaveReminderFragment>(bundle, R.style.AppTheme)
        @Suppress("UNCHECKED_CAST")
        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        // THEN - Reminder details fields are displayed empty on the screen
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderTitle)).check(matches(withText("")))
        onView(withId(R.id.reminderDescription)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderDescription)).check(matches(withText("")))
        onView(withId(R.id.selectedLocation)).check(matches(withText("")))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        onView(withId(R.id.saveReminder)).check(matches(isDisplayed()))
    }

    @Test
    fun newReminderWithPoi_displayedInUi() {
        // GIVEN - A point of interest
        val poi = PointOfInterest(LatLng(0.0, 0.0), 100.0, "test")

        // WHEN - Save reminder fragment is launched with the POI as argument
        val bundle = SaveReminderFragmentArgs(selectedPOI = poi).toBundle()
        val scenario = launchFragmentInContainer<SaveReminderFragment>(bundle, R.style.AppTheme)
        @Suppress("UNCHECKED_CAST")
        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        // THEN - Proper reminder location together with snackbar is displayed
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderTitle)).check(matches(withText("")))
        onView(withId(R.id.reminderDescription)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderDescription)).check(matches(withText("")))
        onView(withId(R.id.selectedLocation)).check(matches(withText(poi.name)))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        onView(withId(R.id.saveReminder)).check(matches(isDisplayed()))
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.location_added)))
    }

    @Test
    fun newReminderWithUpdatedPoi_displayedInUi() {
        // GIVEN - A point of interest
        val poi1 = PointOfInterest(LatLng(0.0, 0.0), 100.0, "test")
        val poi2 = PointOfInterest(LatLng(0.0, 0.0), 100.0, "test2")

        // WHEN - Save reminder fragment is launched with the POI as argument
        val bundle = SaveReminderFragmentArgs(selectedPOI = poi2).toBundle()
        val scenario = launchFragmentInContainer(bundle, R.style.AppTheme) {
            SaveReminderFragment().also { fragment ->
                fragment._viewModel.selectedPOI.value = poi1
            }
        }
        @Suppress("UNCHECKED_CAST")
        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        // THEN - Proper reminder location together with snackbar is displayed
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderTitle)).check(matches(withText("")))
        onView(withId(R.id.reminderDescription)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderDescription)).check(matches(withText("")))
        onView(withId(R.id.selectedLocation)).check(matches(withText(poi2.name)))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        onView(withId(R.id.saveReminder)).check(matches(isDisplayed()))
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.location_updated)))
    }

    @Test
    fun existingReminder_displayedInUi() {
        // GIVEN - A reminder
        val reminder = ReminderDataItem(
            "Say hi to old friends",
            "Don't forget Jane and John",
            "TalTech",
            59.39604886086625,
            24.671091785402538,
            300.5
        )

        // WHEN - Save reminder fragment is launched with the reminder as argument
        val bundle = SaveReminderFragmentArgs(reminder = reminder).toBundle()
        val scenario = launchFragmentInContainer<SaveReminderFragment>(bundle, R.style.AppTheme)
        @Suppress("UNCHECKED_CAST")
        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        // THEN - Reminder info is displayed
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderTitle)).check(matches(withText(reminder.title)))
        onView(withId(R.id.reminderDescription)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderDescription)).check(matches(withText(reminder.description)))
        onView(withId(R.id.selectedLocation)).check(matches(withText(reminder.location)))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        onView(withId(R.id.saveReminder)).check(matches(isDisplayed()))
    }

    @Test
    fun saveReminderWithoutTitle_errorSnackbarDisplayed() {
        // GIVEN - A reminder fragment with a reminder without a title
        val reminder = ReminderDataItem(
            null,
            "Don't forget Jane and John",
            "TalTech",
            59.39604886086625,
            24.671091785402538,
            300.5
        )

        val bundle = SaveReminderFragmentArgs(reminder = reminder).toBundle()
        val scenario = launchFragmentInContainer<SaveReminderFragment>(bundle, R.style.AppTheme)
        @Suppress("UNCHECKED_CAST")
        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        // WHEN - Save FAB is clicked
        onView(withId(R.id.saveReminder)).perform(click())

        // THEN - Reminder is not saved and corresponding error snackbar is shown
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_enter_title)))
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderTitle)).check(matches(withText("")))
        onView(withId(R.id.reminderDescription)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderDescription)).check(matches(withText(reminder.description)))
        onView(withId(R.id.selectedLocation)).check(matches(withText(reminder.location)))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        onView(withId(R.id.saveReminder)).check(matches(isDisplayed()))

    }

    @Test
    fun saveReminderWithoutLocation_errorSnackbarDisplayed() {
        // GIVEN - A reminder fragment with a reminder without a location
        val reminder = ReminderDataItem(
            "Say hi to old friends",
            "Don't forget Jane and John",
            "",
            59.39604886086625,
            24.671091785402538,
            300.5
        )

        val bundle = SaveReminderFragmentArgs(reminder = reminder).toBundle()
        val scenario = launchFragmentInContainer<SaveReminderFragment>(bundle, R.style.AppTheme)
        @Suppress("UNCHECKED_CAST")
        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        // WHEN - Save FAB is clicked
        onView(withId(R.id.saveReminder)).perform(click())

        // THEN - Reminder is not saved and corresponding error snackbar is shown
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_select_location)))
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderTitle)).check(matches(withText(reminder.title)))
        onView(withId(R.id.reminderDescription)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderDescription)).check(matches(withText(reminder.description)))
        onView(withId(R.id.selectedLocation)).check(matches(withText("")))
        onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        onView(withId(R.id.saveReminder)).check(matches(isDisplayed()))

    }

    @Test
    fun saveReminder_reminderSavedAndNavigatedBack(): Unit = runBlocking {
        // GIVEN - A reminder fragment with a valid reminder and navigation controller set up
        val navController = mock(NavController::class.java)
        val reminder = ReminderDataItem(
            "Say hi to old friends",
            "Don't forget Jane and John",
            "TalTech",
            59.39604886086625,
            24.671091785402538,
            300.5
        )

        val bundle = SaveReminderFragmentArgs(reminder = reminder).toBundle()
        val scenario = launchFragmentInContainer<SaveReminderFragment>(bundle, R.style.AppTheme)

        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), navController)
        }
        @Suppress("UNCHECKED_CAST")
        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        // WHEN - Save FAB is clicked
        onView(withId(R.id.saveReminder)).perform(click())

        // THEN - Reminder is saved, corresponding toast is shown and back navigation is executed
        val allResults = repository.getReminders()
        val reminderDTO = repository.getReminder(reminder.id)
        assertThat(allResults.succeeded).isTrue
        assertThat((allResults as Result.Success).data.size).isEqualTo(1)
        assertThat(reminderDTO.succeeded).isTrue
        assertThat((reminderDTO as Result.Success).data.asDataItem()).isEqualTo(reminder)
        onToast(R.string.reminder_saved).check(matches(isDisplayed()))
        verify(navController).popBackStack()
    }

    @Test
    fun selectLocationWithEmptyPoi_navigateToSelectLocationFragment() {
        // GIVEN - A reminder fragment with a valid reminder and navigation controller set up
        val navController = TestNavHostController(appContext)
        val bundle = SaveReminderFragmentArgs().toBundle()
        val scenario = launchFragmentInContainer<SaveReminderFragment>(bundle, R.style.AppTheme)
        @Suppress("UNCHECKED_CAST")
        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        scenario.onFragment { fragment ->
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.saveReminderFragment)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // WHEN - Select location text view is clicked
        onView(withId(R.id.selectLocation)).perform(click())

        // THEN - Reminder is saved, corresponding toast is shown and back navigation is executed
        val currentDestinationArgs = navController.backStack.last().arguments
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.selectLocationFragment)
        assertThat(currentDestinationArgs?.get("currentPOI")).isEqualTo(null)
    }


    @Test
    fun selectLocation_navigateToSelectLocationFragment() {
        // GIVEN - A reminder fragment with a valid reminder and navigation controller set up
        val navController = TestNavHostController(appContext)
        val reminder = ReminderDataItem(
            "Say hi to old friends",
            "Don't forget Jane and John",
            "TalTech",
            59.39604886086625,
            24.671091785402538,
            300.5
        )

        val bundle = SaveReminderFragmentArgs(reminder = reminder).toBundle()
        val scenario = launchFragmentInContainer<SaveReminderFragment>(bundle, R.style.AppTheme)
        @Suppress("UNCHECKED_CAST")
        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        scenario.onFragment { fragment ->
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.saveReminderFragment)
            Navigation.setViewNavController(fragment.requireView(), navController)
            fragment.activity
        }

        // WHEN - Select location text view is clicked
        onView(withId(R.id.selectLocation)).perform(click())

        // THEN - Reminder is saved, corresponding toast is shown and back navigation is executed
        val currentDestinationArgs = navController.backStack.last().arguments
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.selectLocationFragment)
        assertThat(currentDestinationArgs?.get("currentPOI")).isEqualTo(
            PointOfInterest(
                LatLng(
                    reminder.latitude!!,
                    reminder.longitude!!
                ), reminder.radius!!, reminder.location!!
            )
        )
    }
}