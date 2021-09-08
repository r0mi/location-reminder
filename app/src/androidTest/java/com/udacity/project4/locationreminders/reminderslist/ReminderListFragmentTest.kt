package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.IBinder
import android.view.WindowManager
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.Root
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.base.DataBindingViewHolder
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.asDataItem
import com.udacity.project4.util.atPosition
import com.udacity.project4.util.monitorFragment
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
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


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {

//    TODO: test the navigation of the fragments.
//    TODO: test the displayed data on the UI.
//    TODO: add testing for the error messages.
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // An idling resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    @Before
    fun init() {
        stopKoin() //stop the original app koin
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

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun clickAddReminderFAB_navigateToSaveReminder() {
        // GIVEN - ReminderListFragment with navigation controller set up
        val navController = TestNavHostController(getApplicationContext())

        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        scenario.onFragment { fragment ->
            // Set the graph on the TestNavHostController
            navController.setGraph(R.navigation.nav_graph)

            // Make the NavController available via the findNavController() APIs
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // WHEN - Clicking on the "+" button
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN - Verify that the NavController’s current destination state has changed to saveReminderFragment
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.saveReminderFragment)
    }

    @Test
    fun swipeRightOnReminder_navigateToEditReminder() = runBlocking<Unit> {
        // GIVEN - ReminderListFragment with at least one reminder shown and navigation controller set up
        val reminder = ReminderDTO(
            "Say hi to old friends",
            "Don't forget Jane and John",
            "TalTech",
            59.39604886086625,
            24.671091785402538,
            300.5
        )
        repository.saveReminder(reminder)

        val navController = TestNavHostController(getApplicationContext())

        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)

        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        scenario.onFragment { fragment ->
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // WHEN - Swiping right on the first reminder in the list
        onView(withId(R.id.remindersRecyclerView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<DataBindingViewHolder<ReminderDataItem>>(
                0,
                swipeRight()
            )
        )

        // THEN - Verify that the NavController’s current destination state has changed to saveReminderFragment with the proper reminder argument
        val currentDestinationArgs = navController.backStack.last().arguments
        assertThat(navController.currentDestination?.id).isEqualTo(R.id.saveReminderFragment)
        assertThat(currentDestinationArgs?.get("reminder")).isEqualTo(reminder.asDataItem())
        assertThat(currentDestinationArgs?.get("selectedPOI")).isEqualTo(null)
    }

    @Test
    fun swipeLeftOnReminder_deletesReminder() = runBlocking<Unit> {
        // GIVEN - ReminderListFragment with at least one reminder shown
        val reminder = ReminderDTO(
            "Say hi to old friends",
            "Don't forget Jane and John",
            "TalTech",
            59.39604886086625,
            24.671091785402538,
            300.5
        )
        repository.saveReminder(reminder)

        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)

        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        /*var viewmodel: RemindersListViewModel? = null
        scenario.onFragment { fragment ->
            viewmodel = fragment._viewModel
        }*/

        // WHEN - Swiping left on the first reminder in the list
        onView(withId(R.id.remindersRecyclerView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<DataBindingViewHolder<ReminderDataItem>>(
                0,
                swipeLeft()
            )
        )

        // THEN - The reminder is deleted
        //onView(withText("Reminder Deleted!")).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        //assertThat(viewmodel?.showSnackBarInt?.value).isEqualTo(R.string.reminder_deleted)
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText("Reminder Deleted!")))
    }

    @Test
    fun emptyReminderList_noDataDisplayedInUI() = runBlocking<Unit> {
        // WHEN - ReminderListFragment is displayed with an empty reminder data source
        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)

        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        // THEN - "No data" text is shown on the screen
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun reminderList_displayedInUI() = runBlocking<Unit> {
        // GIVEN - A list of reminders
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
        repository.saveReminder(reminder1)
        repository.saveReminder(reminder2)
        repository.saveReminder(reminder3)

        // WHEN - ReminderListFragment is launched
        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)

        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        // THEN - The respective reminders are displayed on the screen
        onView(withId(R.id.remindersRecyclerView)).check(
            matches(
                atPosition(
                    0, allOf(
                        hasDescendant(withText(reminder1.title)),
                        hasDescendant(withText(reminder1.description)),
                        hasDescendant(withText(reminder1.location)),
                    )
                )
            )
        )
        onView(withId(R.id.remindersRecyclerView)).check(
            matches(
                atPosition(
                    1, allOf(
                        hasDescendant(withText(reminder2.title)),
                        hasDescendant(withText(reminder2.description)),
                        hasDescendant(withText(reminder2.location)),
                    )
                )
            )
        )
        onView(withId(R.id.remindersRecyclerView)).check(
            matches(
                atPosition(
                    2, allOf(
                        hasDescendant(withText(reminder3.title)),
                        hasDescendant(withText(reminder3.description)),
                        hasDescendant(withText(reminder3.location)),
                    )
                )
            )
        )
    }

    @Test
    fun showErrorMessage_snackbarIsShown() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)

        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        scenario.onFragment { fragment ->
            (fragment as ReminderListFragment)._viewModel.showSnackBar.value = "Error message"
        }

        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText("Error message")))
    }

    @Test
    fun showErrorMessage_toastIsShown() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)

        dataBindingIdlingResource.monitorFragment(scenario as FragmentScenario<Fragment>)

        scenario.onFragment { fragment ->
            (fragment as ReminderListFragment)._viewModel.showToast.value = "Error message"
        }
        
        onView(withText("Error message")).inRoot(ToastMatcher())
            .check(matches(isDisplayed()))
    }
}

class ToastMatcher : TypeSafeMatcher<Root>() {

    override fun describeTo(description: Description) {
        description.appendText("is toast")
    }

    override fun matchesSafely(item: Root): Boolean {
        val type: Int? = item.windowLayoutParams?.get()?.type
        if (type == WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW) {
            val windowToken: IBinder = item.decorView.windowToken
            val appToken: IBinder = item.decorView.applicationWindowToken
            if (windowToken === appToken) { // means this window isn't contained by any other windows.
                return true
            }
        }
        return false
    }
}