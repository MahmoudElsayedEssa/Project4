package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
// END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed auto close @after method to close Koin after every test

    private lateinit var reminderDataSource: ReminderDataSource
    private lateinit var appContext: Application
    private val binding = DataBindingIdlingResource()

    @Before
    fun init() {
        stopKoin() // stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext, get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext, get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(appContext) }
        }

        // declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }

        // Get our real repository
        reminderDataSource = get()

        // clear the data to start fresh
        runBlocking {
            reminderDataSource.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(binding)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(binding)
    }

    @Test
    fun testErrorEnterTitleSnackBar() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        binding.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())

        val snackBarMessage = appContext.getString(R.string.err_enter_title)
        onView(withText(snackBarMessage)).check(matches(isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun testReminderSavedToastMessage() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        binding.monitorActivity(activityScenario)

        onView(withId(R.id.addReminderFAB)).perform(click())

        onView(withId(R.id.reminderTitle)).perform(ViewActions.replaceText("Title"))
        onView(withId(R.id.reminderDescription)).perform(ViewActions.replaceText("Description"))
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.button)).perform(click())

        onView(withId(R.id.saveReminder)).perform(click())

        onView(withText(R.string.reminder_saved)).inRoot(
            withDecorView(
                not(
                    `is`(
                        getActivity(
                            activityScenario
                        ).window.decorView
                    )
                )
            )
        ).check(matches(isDisplayed()))

        activityScenario.close()
    }

    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity {
        lateinit var activity: Activity
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }
}