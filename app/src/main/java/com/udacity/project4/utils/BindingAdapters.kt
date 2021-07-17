package com.udacity.project4.utils

import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.udacity.project4.R
import com.udacity.project4.base.BaseRecyclerViewAdapter
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem


object BindingAdapters {

    /**
     * Use binding adapter to set the recycler view data using livedata object
     */
    @Suppress("UNCHECKED_CAST")
    @BindingAdapter("android:liveData")
    @JvmStatic
    fun <T> setRecyclerViewData(recyclerView: RecyclerView, items: LiveData<List<T>>?) {
        items?.value?.let { itemList ->
            (recyclerView.adapter as? BaseRecyclerViewAdapter<T>)?.apply {
                clear()
                addData(itemList)
            }
        }
    }

    /**
     * Use this binding adapter to show and hide the views using boolean variables
     */
    @BindingAdapter("android:fadeVisible")
    @JvmStatic
    fun setFadeVisible(view: View, visible: Boolean? = true) {
        if (view.tag == null) {
            view.tag = true
            view.visibility = if (visible == true) View.VISIBLE else View.GONE
        } else {
            view.animate().cancel()
            if (visible == true) {
                if (view.visibility == View.GONE)
                    view.fadeIn()
            } else {
                if (view.visibility == View.VISIBLE)
                    view.fadeOut()
            }
        }
    }

    /**
     * Use this binding adapter to hide the SwipeRefreshLayout refreshing progress indicator
     */
    @BindingAdapter("android:isRefreshing")
    @JvmStatic
    fun setIsRefreshing(view: SwipeRefreshLayout, isRefreshing: Boolean? = false) {
        isRefreshing?.let {
            view.isRefreshing = it
        }
    }

    /**
     * Use this binding adapter to properly format the reminder for location string
     */
    @BindingAdapter("android:reminderForLocation")
    @JvmStatic
    fun setReminderForLocation(view: TextView, reminder: ReminderDataItem) {
        view.text = view.context.resources.getHtmlSpannedString(
            R.string.reminder_for_location,
            reminder.location!!,
            reminder.latitude!!,
            reminder.longitude!!,
            reminder.radius!!
        )
    }


}