package com.udacity.project4.locationreminders.reminderslist

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeDataSource
import kotlinx.coroutines.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {


    private lateinit var viewModel: RemindersListViewModel
    private lateinit var repo: FakeDataSource

    @Before
    fun setupViewModel() {
        repo = FakeDataSource()
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), repo)
        stopKoin()
        modules()
    }

    private fun modules() {
        val mModule = module {
            single {
                viewModel
            }
        }
        startKoin {
            modules(listOf(mModule))
        }
    }

    @Test
    fun loadReminders() {
        GlobalScope.launch(Dispatchers.IO) {
            val reminderOne =
                ReminderDTO("title", "description", "location", 33.0, 33.0)
            val reminderTwo =
                ReminderDTO("title", "description", "location", 33.0, 33.0)

            repo.saveReminder(reminderOne)
            repo.saveReminder(reminderTwo)

            viewModel.loadReminders()
            val remindersCount = viewModel.remindersList.value?.size
            Assert.assertEquals(remindersCount, 2)
        }
    }
}