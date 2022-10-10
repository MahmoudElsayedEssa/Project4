package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.rule.MainCoroutineRule
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.KoinComponent

import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class SaveReminderViewModelTest: KoinComponent{

    private lateinit var fakeReminderDataSource: FakeDataSource
    private lateinit var saveReminderViewModel: SaveReminderViewModel


    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()


    @Before
    fun setup() {
        //make fake data source and the view model
        fakeReminderDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeReminderDataSource)

    }


    @Test
    fun shouldReturnError () = runTest  {
        val result = saveReminderViewModel.validateEnteredData(createIncompleteReminderDataItem())
        MatcherAssert.assertThat(result, CoreMatchers.`is`(false))
    }

    private fun createIncompleteReminderDataItem(): ReminderDataItem {
        return ReminderDataItem(
            "",
            "description",
            "location",
            55.00,
            33.00)
    }

    @Test
    fun check_loading() = runTest {

        mainCoroutineRule.pauseDispatcher()
        saveReminderViewModel.saveReminder(createFakeReminderDataItem())

        MatcherAssert.assertThat(saveReminderViewModel.showLoading.value, CoreMatchers.`is`(true))

        mainCoroutineRule.resumeDispatcher()

        MatcherAssert.assertThat(saveReminderViewModel.showLoading.value, CoreMatchers.`is`(false))
    }

    private fun createFakeReminderDataItem(): ReminderDataItem {
        return ReminderDataItem(
            "title",
            "description",
            "location",
            55.00,
            33.00)
    }
}