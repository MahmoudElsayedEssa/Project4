package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

@RequiresApi(Build.VERSION_CODES.M)
class SaveReminderFragment : BaseFragment() {

    companion object {
        private const val TAG = "SaveReminderFragment"
        private const val GEOFENCE_RADIUS_IN_METERS = 100f
    }

    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private lateinit var reminderDataItem: ReminderDataItem
    private lateinit var activityResultLauncherPermissions: ActivityResultLauncher<Array<String>>
    private lateinit var activityResultLauncherLocation: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_save_reminder, container, false
        )

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        activityResultLauncherPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                if (result.all { r -> r.value!! }) {
                    //granted
                    checkPermissionsAndStartGeofencing()
                    Log.d(TAG, "Permission Granted")
                } else {
                    //not granted
                    Snackbar.make(
                        binding.fragmentSaveReminder,
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_LONG
                    ).setAction(R.string.settings) {
                        // Displays App settings screen.
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }.show()
                }
            }

        activityResultLauncherLocation =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    startGeoFence(reminderDataItem)
                }
            }

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {

            _viewModel.navigationCommand.value = NavigationCommand.To(
                SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
            )
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminderDataItem = ReminderDataItem(title, description, location, latitude, longitude)

            if (_viewModel.validateEnteredData(reminderDataItem)) {
                checkPermissionsAndStartGeofencing()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _viewModel.onClear()
    }

    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundLocationPermissionApproved() && backgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            if (!foregroundLocationPermissionApproved()) {
                requestForegroundLocationPermissions()
            }
            if (!backgroundLocationPermissionApproved()) {
                requestBackgroundLocationPermission()
            }
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    activityResultLauncherLocation.launch(intentSenderRequest)

                    /*  exception.startResolutionForResult(requireActivity(),
                          REQUEST_TURN_DEVICE_LOCATION_ON)*/
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.fragmentSaveReminder,
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                startGeoFence(reminderDataItem)
            }
        }
    }

    private fun foregroundLocationPermissionApproved(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun backgroundLocationPermissionApproved(): Boolean {
        return if (runningQOrLater) {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestForegroundLocationPermissions() {
        when {
            foregroundLocationPermissionApproved() -> {
                return
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Snackbar.make(
                    binding.fragmentSaveReminder,
                    R.string.permission_denied_explanation,
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.settings) {
                    // Displays App settings screen.
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
            }
            else -> {
                activityResultLauncherPermissions.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundLocationPermission() {
        if (backgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
            return
        }
        if (runningQOrLater) {
            activityResultLauncherPermissions.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))

        } else {
            return
        }
    }

    private fun startGeoFence(reminderDataItem: ReminderDataItem) {

        val geofence = Geofence.Builder().setRequestId(reminderDataItem.id).setCircularRegion(
                reminderDataItem.latitude!!, reminderDataItem.longitude!!, GEOFENCE_RADIUS_IN_METERS
            ).setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER).build()

        val geofencingRequest =
            GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence).build()


        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        // Use FLAG_UPDATE_CURRENT so that you get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        val geofencePendingIntent = PendingIntent.getBroadcast(
            requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )


        //check for permission
        if (ActivityCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val geofencingClient = LocationServices.getGeofencingClient(requireActivity())

            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {

                addOnSuccessListener {
                    Toast.makeText(
                        requireActivity(), R.string.geofences_added, Toast.LENGTH_SHORT
                    ).show()

                    _viewModel.saveReminder(reminderDataItem)
                    Log.e(TAG, geofence.requestId)
                }

                addOnFailureListener {
                    Toast.makeText(
                        requireActivity(), R.string.geofences_not_added, Toast.LENGTH_SHORT
                    ).show()
                    if ((it.message != null)) {
                        Log.w(TAG, it.message!!)
                    }
                }
            }


        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                SelectLocationFragment.REQUEST_LOCATION_PERMISSION
            )
        }
    }


}