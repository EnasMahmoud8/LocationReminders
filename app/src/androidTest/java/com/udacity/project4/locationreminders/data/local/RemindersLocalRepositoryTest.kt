package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runBlockingTest
import org.junit.*
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private lateinit var remindersDatabase: RemindersDatabase

    private val item1 = ReminderDTO("Reminder1", "Description1", "Location1", 12.0, 5.0, "1")
    private val item2 = ReminderDTO("Reminder2", "Description2", "location2", 22.0, 17.0, "2")


    @Before
    fun setup() {
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        remindersLocalRepository = RemindersLocalRepository(
            remindersDatabase.reminderDao(),
            Dispatchers.Main
        )
    }

    @After
    fun tearDown() {
        remindersDatabase.close()
    }


    @Test
    fun saveReminder_checkData() {
        runBlockingTest {
            remindersLocalRepository.saveReminder(item1)
            val reminderSaved = remindersLocalRepository.getReminder(item1.id) as Result.Success
            Assert.assertEquals(reminderSaved.data.title, item1.title)
            Assert.assertEquals(reminderSaved.data.description, item1.description)
            Assert.assertEquals(reminderSaved.data.location, item1.location)
            Assert.assertEquals(reminderSaved.data.latitude, item1.latitude)
            Assert.assertEquals(reminderSaved.data.longitude, item1.longitude)
        }
    }

    @Test
    fun loadReminders_2Reminders() {
        runBlockingTest {
            remindersLocalRepository.deleteAllReminders()
            remindersLocalRepository.saveReminder(item1)
            remindersLocalRepository.saveReminder(item2)
            val reminderSaved = remindersLocalRepository.getReminders() as Result.Success
            Assert.assertEquals(reminderSaved.data.size, 2)
        }
    }

    @Test
    fun loadReminders_returnError() {
        runBlockingTest {
            remindersLocalRepository.deleteAllReminders()
            val responseResult = remindersLocalRepository.getReminders() as Result.Error
            Assert.assertEquals(responseResult.message, "Reminder not found!")
        }
    }

}