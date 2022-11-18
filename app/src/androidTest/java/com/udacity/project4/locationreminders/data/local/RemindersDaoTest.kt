package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {
    lateinit var remindersDatabase: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val item1 = ReminderDTO("Reminder1", "Description1", "Location1", 12.0, 5.0, "1")
    private val item2 = ReminderDTO("Reminder2", "Description2", "location2", 22.0, 17.0, "2")


    @Before
    fun setUp() {
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() {
        remindersDatabase.close()
    }

    @Test
    fun insertReminders_deleteAllReminders() {
        runBlockingTest {
            remindersDatabase.reminderDao().saveReminder(item1)
            remindersDatabase.reminderDao().saveReminder(item2)
            remindersDatabase.reminderDao().deleteAllReminders()

            assertEquals(remindersDatabase.reminderDao().getReminders().size, 0)
        }
    }

    @Test
    fun insertReminders_deleteReminderById() {
        runBlockingTest {
            remindersDatabase.reminderDao().saveReminder(item1)
            remindersDatabase.reminderDao().saveReminder(item2)
            remindersDatabase.reminderDao().delete(item1.id)
            val remindersList = remindersDatabase.reminderDao().getReminders()
            assertEquals(remindersList.size, 2)
            assertEquals(remindersList[0].id, item2.id)
        }
    }

    @Test
    fun saveReminder_checkData() {
        runBlockingTest {
            remindersDatabase.reminderDao().saveReminder(item1)
            val reminderSaved = remindersDatabase.reminderDao().getReminderById("1") as ReminderDTO
            assertEquals(reminderSaved.title, item1.title)
            assertEquals(reminderSaved.description, item1.description)
            assertEquals(reminderSaved.location, item1.location)
            assertEquals(reminderSaved.latitude, item1.latitude)
            assertEquals(reminderSaved.longitude, item1.longitude)
        }
    }
}