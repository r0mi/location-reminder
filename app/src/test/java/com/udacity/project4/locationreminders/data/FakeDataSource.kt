package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import java.util.*

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    var remindersServiceData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()

    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (!shouldReturnError)
            Result.Success(remindersServiceData.values.toList())
        else
            Result.Error(ERROR_LOADING_REMINDERS)
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersServiceData[reminder.id] = reminder
    }

    override suspend fun deleteReminder(id: String) {
        remindersServiceData.remove(id)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error(ERROR_LOADING_REMINDER)
        }
        remindersServiceData[id]?.let {
            return Result.Success(it)
        }
        return Result.Error(REMINDER_NOT_FOUND)
    }

    override suspend fun deleteAllReminders() {
        remindersServiceData.clear()
    }

    fun addReminders(reminders: List<ReminderDTO>) {
        for (reminder in reminders) {
            remindersServiceData[reminder.id] = reminder
        }
    }

    companion object {
        const val REMINDER_NOT_FOUND = "Reminder not found!"
        const val ERROR_LOADING_REMINDER = "Error loading reminder!"
        const val ERROR_LOADING_REMINDERS = "Error loading reminders!"
    }
}