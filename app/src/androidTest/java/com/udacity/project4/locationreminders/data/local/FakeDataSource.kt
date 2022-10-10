package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

// Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    private val remindersList = ArrayList<ReminderDTO>()

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return Result.Success(remindersList)
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersList.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        remindersList.forEach {
            if (it.id == id) {
                return Result.Success(it)
            }
        }

        return Result.Error("there is no reminder item with ID: $id")
    }

    override suspend fun deleteAllReminders() {
        remindersList.clear()
    }
}