package com.udacity.project4.util

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

fun ReminderDTO.asDataItem(): ReminderDataItem {
    return ReminderDataItem(
        title,
        description,
        location,
        latitude,
        longitude,
        radius,
        id
    )
}

