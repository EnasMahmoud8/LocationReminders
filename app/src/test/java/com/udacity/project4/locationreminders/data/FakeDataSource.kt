package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeDataSource (private var reminders: MutableList<ReminderDTO>? = mutableListOf()) :
    ReminderDataSource {

    private var returnSuccess = true

    fun setReturnSuccess(value: Boolean){
        returnSuccess = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if(returnSuccess){
            Result.Success(ArrayList(reminders))
        } else
            Result.Error("Reminder not found!")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders!!.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return if(returnSuccess){
            val reminder = reminders!!.find { it.id == id }
            return if(reminder != null){
                Result.Success(reminder)
            }
            else
                Result.Error("Reminder not found!")
        } else
            Result.Error("Reminder not found!")
    }

    override suspend fun deleteAllReminders() {
        reminders!!.clear()
    }
}