package com.udacity.project4.locationreminders.selectlocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var mMap: GoogleMap
    private var poiMarker: Marker? = null

    private val locationSettingLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                checkDeviceLocationSettings()
            } else {
                showSettingSnackBar()
            }
        }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                checkDeviceLocationSettings()
                return@registerForActivityResult
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                checkDeviceLocationSettings()
                return@registerForActivityResult
            }
            else -> {
                showSnackBar()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveBtn.setOnClickListener {
            if (poiMarker != null) {
                onLocationSelected()
            }
        }

        return binding.root
    }

    private fun onLocationSelected() {
        _viewModel.latitude.value = poiMarker!!.position.latitude
        _viewModel.longitude.value = poiMarker!!.position.longitude
        _viewModel.reminderSelectedLocationStr.value = poiMarker!!.title
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setMapLongClick(mMap)
        setPoiClick(mMap)
        setMapStyle(mMap)

        requestLocationPermissions()
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener {
            map.clear()
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                it.latitude,
                it.longitude
            )

            poiMarker = map.addMarker(
                MarkerOptions()
                    .position(it).title("Dropped Pin").snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            map.animateCamera(CameraUpdateFactory.newLatLng(it))
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.clear()
            poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            poiMarker?.showInfoWindow()
            map.animateCamera(CameraUpdateFactory.newLatLng(poi.latLng))
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e("TAG", "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("TAG", "Can't find style. Error: ", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationPermissions() {

        var permissionsArray = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        locationPermissionRequest.launch(permissionsArray)
    }

    private fun showSnackBar() {
        Snackbar.make(
            binding.activityMapsMain,
            R.string.permission_denied_explanation,
            Snackbar.LENGTH_LONG
        )
            .setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
    }

    private fun showSettingSnackBar() {
        Snackbar.make(
            binding.activityMapsMain,
            R.string.location_required_error, Snackbar.LENGTH_LONG
        ).setAction(android.R.string.ok) {
            checkDeviceLocationSettings()
        }.show()
    }


    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
         val locationRequest = LocationRequest.create()

        val builder = LocationSettingsRequest.Builder().setAlwaysShow(true)
            .addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())

        val locationSettingsResponseTask: Task<LocationSettingsResponse> =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    locationSettingLauncher?.launch(
                        IntentSenderRequest.Builder(exception.resolution.intentSender).build()
                    )

                } catch (sendEx: IntentSender.SendIntentException) {
                    showSettingSnackBar()                }
            } else {
                showSettingSnackBar()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {

                getUserLocation()
        }
    }


    @SuppressLint("MissingPermission")
    fun getUserLocation() {
        mMap.isMyLocationEnabled = true
        LocationServices.getFusedLocationProviderClient(requireActivity())
            .getCurrentLocation(
                CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build(), null
            )
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                    mMap.addMarker(
                        MarkerOptions().position(userLocation)
                            .title("My Location")
                    )?.showInfoWindow()
                }
            }
    }

}