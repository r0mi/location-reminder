package com.udacity.project4.locationreminders

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

fun ReminderDataItem.asDTO(): ReminderDTO {
    return ReminderDTO(
        title,
        description,
        location,
        latitude,
        longitude,
        radius,
        id
    )
}

fun List<ReminderDTO>.asDataItemList(): List<ReminderDataItem> {
    val dataList = ArrayList<ReminderDataItem>()
    dataList.addAll(this.map { reminder -> reminder.asDataItem() })
    return dataList
}