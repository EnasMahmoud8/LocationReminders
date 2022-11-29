package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderlist.ReminderDataItem
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.*
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest  {

    private lateinit var saveReminder: SaveReminderViewModel
    private lateinit var dataSource: FakeDataSource

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        stopKoin()
        dataSource = FakeDataSource()
        saveReminder =
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), dataSource)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun getShowLoading_statues() {
        mainCoroutineRule.runBlockingTest {
            mainCoroutineRule.pauseDispatcher()
            val item1 = ReminderDataItem("Reminder1", "Description1", "Location1", 12.0, 5.0, "1")
            saveReminder.saveReminder(item1)
            Assert.assertEquals(saveReminder.showLoading.getOrAwaitValue(), true)
            mainCoroutineRule.resumeDispatcher()
            Assert.assertEquals(saveReminder.showLoading.getOrAwaitValue(), false)
        }
    }

    @Test
    fun saveReminder_checkData() {
        mainCoroutineRule.runBlockingTest {
            val item1 = ReminderDataItem("Reminder1", "Description1", "Location1", 12.0, 5.0, "1")
            saveReminder.saveReminder(item1)
            val reminderSaved = dataSource.getReminder("1") as Result.Success
            assertEquals(reminderSaved.data.title, item1.title)
            assertEquals(reminderSaved.data.description, item1.description)
            assertEquals(reminderSaved.data.location, item1.location)
            assertEquals(reminderSaved.data.latitude, item1.latitude)
            assertEquals(reminderSaved.data.longitude, item1.longitude)
        }
    }

    @Test
    fun validateEnteredData_emptyTitle_updateSnackBar() {
        val item1 = ReminderDataItem("", "Description1", "Location1", 12.0, 5.0, "1")
        assertEquals(saveReminder.validateEnteredData(item1),false)
        assertEquals(saveReminder.showSnackBarInt.getOrAwaitValue(),R.string.err_enter_title )
    }

    @Test
    fun validateEnteredData_emptyLocation_updateSnackBar() {
        val item1 = ReminderDataItem("Reminder1", "Description1", "", 12.0, 5.0, "1")
        assertEquals(saveReminder.validateEnteredData(item1),false)
        assertEquals(saveReminder.showSnackBarInt.getOrAwaitValue(),R.string.err_select_location )
    }

}