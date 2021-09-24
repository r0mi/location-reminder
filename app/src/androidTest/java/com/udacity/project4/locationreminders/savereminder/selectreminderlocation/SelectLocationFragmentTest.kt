package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.PointOfInterest
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class SelectLocationFragmentTest : AutoCloseKoinTest() {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        stopKoin()

        val appContext: Application = ApplicationProvider.getApplicationContext()
        val myModule = module {
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

        startKoin {
            modules(listOf(myModule))
        }

        val repository: ReminderDataSource = get()

        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Test
    fun saveEmptyLocation_errorSnackbarDisplayedInUi() {
        // GIVEN - A SelectLocationFragment without a selected POI
        val bundle = SelectLocationFragmentArgs(currentPOI = null).toBundle()
        launchFragmentInContainer<SelectLocationFragment>(bundle, R.style.AppTheme)

        // WHEN - Save button is clicked
        onView(withId(R.id.save_button)).perform(click())

        // THEN - Error snackbar is shown
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.select_poi)))
    }

    @Test
    fun saveValidLocation_navigateToSaveReminderFragment() {
        // GIVEN - A SelectLocationFragment with a selected POI
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        val poi = PointOfInterest(
            LatLng(59.39604886086625, 24.671091785402538),
            300.5,
            "TalTech"
        )
        val bundle = SelectLocationFragmentArgs(currentPOI = poi).toBundle()
        val scenario = launchFragmentInContainer<SelectLocationFragment>(bundle, R.style.AppTheme)
        scenario.onFragment { fragment ->
            navController.setGraph(R.navigation.nav_graph)
            navController.setCurrentDestination(R.id.selectLocationFragment)
            Navigation.setViewNavController(fragment.requireView(), navController)
            fragment._viewModel.addLatLngIfNotInList(poi.latLng)
        }

        // WHEN - Save button is clicked
        onView(withId(R.id.save_button)).perform(click())

        // THEN - Verify that the NavController’s current destination state has changed to saveReminderFragment with the proper POI argument
        val currentDestinationArgs = navController.backStack.last().arguments
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.saveReminderFragment)
        assertThat(currentDestinationArgs?.get("selectedPOI")).isEqualTo(poi)
    }
}