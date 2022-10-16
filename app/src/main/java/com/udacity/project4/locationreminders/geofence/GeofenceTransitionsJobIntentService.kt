package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.errorMessage
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        private const val JOB_ID = 573
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context, GeofenceTransitionsJobIntentService::class.java, JOB_ID, intent
            )
            Log.i("TAG", "enqueueWork:context:$context ,,,intent:$intent ")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onHandleWork(intent: Intent) {

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent.hasError()) {
            Log.e("TAG", geofencingEvent.errorCode.toString())
            return
        }

        if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.v("TAG", getString(R.string.geofence_entered))
            sendNotification(geofencingEvent.triggeringGeofences)
        }

    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendNotification(triggeringGeofences: List<Geofence>) {

        for (triggeringGeofence in triggeringGeofences) {
            val requestId = triggeringGeofence.requestId

            //Get the local repository instance
            //val remindersLocalRepository: RemindersLocalRepository by inject()
            val remindersLocalRepository: ReminderDataSource by inject()
            // Interaction to the repository has to be through a coroutine scope
            CoroutineScope(coroutineContext).launch(SupervisorJob()) {
                //get the reminder with the request id
                val result = remindersLocalRepository.getReminder(requestId)
                if (result is Result.Success<ReminderDTO>) {
                    val reminderDTO = result.data
                    //send a notification to the user with the reminder details
                    sendNotification(
                        this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                            reminderDTO.title,
                            reminderDTO.description,
                            reminderDTO.location,
                            reminderDTO.latitude,
                            reminderDTO.longitude,
                            reminderDTO.id
                        )
                    )
                }
            }
        }
    }

}