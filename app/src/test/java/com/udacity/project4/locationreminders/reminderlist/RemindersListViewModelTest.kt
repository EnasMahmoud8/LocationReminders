package com.udacity.project4.locationreminders.reminderlist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert
import org.hamcrest.core.Is
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class RemindersListViewModelTest {

    private lateinit var remindersList: RemindersListViewModel
    private lateinit var dataSource: FakeDataSource

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        stopKoin()
        dataSource = FakeDataSource()
        remindersList =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
    }

    @After
    fun tearDown() {
        runBlockingTest { dataSource.deleteAllReminders() }

    }

    @Test
    fun getShowLoading_statues() {
        mainCoroutineRule.runBlockingTest {
            mainCoroutineRule.pauseDispatcher()
            dataSource.deleteAllReminders()
            val item1 = ReminderDTO("Reminder1", "Description1", "Location1", 12.0, 5.0, "1")
            dataSource.saveReminder(item1)
            remindersList.loadReminders()
            assertEquals(remindersList.showLoading.getOrAwaitValue(), true )
            mainCoroutineRule.resumeDispatcher()
            assertEquals(remindersList.showLoading.getOrAwaitValue(), false)
        }
    }

    @Test
    fun getRemindersList_emptyReminders_showNoData() {
        mainCoroutineRule.runBlockingTest {
            dataSource.deleteAllReminders()
            remindersList.loadReminders()
            assertEquals(remindersList.remindersList.getOrAwaitValue().size, 0)
            assertEquals(remindersList.showNoData.getOrAwaitValue(), true)
        }
    }

    @Test
    fun getRemindersList_returnError() {
        mainCoroutineRule.runBlockingTest {
            dataSource.setReturnSuccess(false)
            remindersList.loadReminders()
            MatcherAssert.assertThat(
                remindersList.showSnackBar.getOrAwaitValue(),
                Is.`is`("Error return reminders")
            )
        }
    }

    @Test
    fun loadReminders_2Reminders() {
        val item1 = ReminderDTO("Reminder1", "Description1", "Location1", 12.0, 5.0, "1")
        val item2 = ReminderDTO("Reminder2", "Description2", "location2", 22.0, 17.0, "2")

        mainCoroutineRule.runBlockingTest {
            dataSource.deleteAllReminders()
            dataSource.saveReminder(item1)
            dataSource.saveReminder(item2)

            remindersList.loadReminders()
            assertEquals(remindersList.remindersList.getOrAwaitValue().size, 2)
//            assertEquals(remindersList.showNoData.getOrAwaitValue(), false)
        }
    }
}