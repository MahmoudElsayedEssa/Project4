package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private var marker: Marker? = null

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 1010
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.map_options, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.normal_map -> {
                        map.mapType = GoogleMap.MAP_TYPE_NORMAL
                        true
                    }
                    R.id.hybrid_map -> {
                        map.mapType = GoogleMap.MAP_TYPE_HYBRID
                        true
                    }
                    R.id.satellite_map -> {
                        map.mapType = GoogleMap.MAP_TYPE_SATELLITE
                        true
                    }
                    R.id.terrain_map -> {
                        map.mapType = GoogleMap.MAP_TYPE_TERRAIN
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.button.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    private fun onLocationSelected() {

        marker?.let {
            _viewModel.reminderSelectedLocationStr.value = it.title
            _viewModel.latitude.value = it.position.latitude
            _viewModel.longitude.value = it.position.longitude
        }

        findNavController().popBackStack()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.apply {
            setOnMapLongClickListener { latLng ->
                addMapMarker(latLng)
                marker!!.showInfoWindow()
            }

            setOnPoiClickListener { poi ->
                addPoiMarker(poi)
                marker!!.showInfoWindow()
            }

            setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(), R.raw.map_style
                )
            )
        }

//        permissionSetup()
        askForPermissionAndGetCurrentLocation()

    }

    private fun askForPermissionAndGetCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            enableMapMyLocation()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    // onRequestPermissionsResult() is deprecated and this the new way
    //https://stackoverflow.com/questions/66551781/android-onrequestpermissionsresult-is-deprecated-are-there-any-alternatives
//    private fun permissionSetup() {
//        val permission = ContextCompat.checkSelfPermission(
//            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
//        )
//
//        if (permission != PackageManager.PERMISSION_GRANTED) {
//            permissionsResultCallback.launch(Manifest.permission.ACCESS_FINE_LOCATION)
//        } else {
//            println("Permission isGranted")
//        }
//    }
//
//    private val permissionsResultCallback = registerForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) {
//        when (it) {
//            true -> {
//                println("Permission has been granted by user")
//                enableMapMyLocation()
//            }
//            false -> {
//                _viewModel.showSnackBarInt.value = R.string.permission_denied_explanation
//            }
//        }
//    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMapMyLocation()
            }
        }
    }


    private fun addPoiMarker(poi: PointOfInterest) {
        marker?.remove()
        marker = map.addMarker(
            MarkerOptions().position(poi.latLng).title(poi.name)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )
    }

    private fun addMapMarker(latLng: LatLng) {
        val snippet = String.format(
            Locale.getDefault(), "Lat: %1$.5f, Long: %2$.5f", latLng.latitude, latLng.longitude
        )

        marker?.remove()
        marker = map.addMarker(
            MarkerOptions().position(latLng).title(getString(R.string.dropped_pin)).snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
    }

    @SuppressLint("MissingPermission")
    private fun enableMapMyLocation() {
        map.isMyLocationEnabled = true
        addLastLocationCallback()
    }

    @SuppressLint("MissingPermission")
    private fun addLastLocationCallback() {

        val fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())
        val lastLocationTask = fusedLocationProviderClient.lastLocation

        lastLocationTask.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(location.latitude, location.longitude)
                map.apply {
                    moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            latLng, 15f
                        )
                    )
                    addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(getString(R.string.my_current_location))
                    )
                }

            }
        }

    }

}
