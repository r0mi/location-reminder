package com.udacity.project4.locationreminders

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.udacity.project4.R
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : AppCompatActivity() {

    // Lazy inject SaveReminderViewModel that is shared between fragments
    private val remindersListViewModel by viewModel<RemindersListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                findNavController(R.id.nav_host_fragment).popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "project4.locationreminders.RemindersActivity.action.ACTION_GEOFENCE_EVENT"
    }
}
