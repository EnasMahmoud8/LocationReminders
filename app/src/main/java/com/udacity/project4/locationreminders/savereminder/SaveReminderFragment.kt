package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderlist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.androidx.scope.BuildConfig

class SaveReminderFragment : BaseFragment() {
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_BACKGROUND_LOCATION, false) ->{
                checkDeviceLocationSettingsAndStartGeofence()
                return@registerForActivityResult
            }
            else -> {
                showSnackBar()
            }
        }
    }

    private val locationSettingLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                checkDeviceLocationSettingsAndStartGeofence()
            } else {
                showSettingSnackBar()
            }
        }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private lateinit var geofencingClient: GeofencingClient
    lateinit var reminderDataItem: ReminderDataItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.addReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value
            reminderDataItem = ReminderDataItem(
                title, description, location,
                latitude, longitude
            )

            if (_viewModel.validateEnteredData(reminderDataItem)) {
                checkPermissionsAndStartGeofencing()
            }
        }
    }

    private fun checkPermissionsAndStartGeofencing() {
        if (runningQOrLater) {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        }
        else{
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
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

                addGeofenceForReminder()

        }
    }

    private fun showSettingSnackBar() {
        Snackbar.make(
            binding.activityMapsMain,
            R.string.location_required_error, Snackbar.LENGTH_LONG
        ).setAction(android.R.string.ok) {
            checkDeviceLocationSettingsAndStartGeofence()
        }.show()
    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceForReminder() {
        val currentGeofenceData = reminderDataItem

        val geofence = Geofence.Builder()
            .setRequestId(currentGeofenceData.id)
            .setCircularRegion(
                currentGeofenceData.latitude!!,
                currentGeofenceData.longitude!!,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        Log.i("TAG", "addGeofenceForReminder:lat ${currentGeofenceData.latitude}")
        Log.i("TAG", "addGeofenceForReminder:long ${currentGeofenceData.longitude}")
        Log.i("TAG", "addGeofenceForReminder:backPer ${PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )}")
        Log.i("TAG", "addGeofenceForReminder:forePer ${(
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))}")

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()


        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                Toast.makeText(
                    context, R.string.reminder_saved,
                    Toast.LENGTH_SHORT
                )
                    .show()
                Log.e("Add Geofence", geofence.requestId)
                _viewModel.validateAndSaveReminder(reminderDataItem)
            }
            addOnFailureListener {
                Toast.makeText(
                    context, R.string.geofences_not_added,
                    Toast.LENGTH_SHORT
                ).show()
                if ((it.message != null)) {
                    Log.w("TAG", it.message!!)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            if (resultCode == Activity.RESULT_OK) {
                addGeofenceForReminder()
            } else{
                checkDeviceLocationSettingsAndStartGeofence(false)
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d("TAG", "onRequestPermissionResult")

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            showSnackBar()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
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

    companion object {
        internal const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        internal const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        internal const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        internal const val LOCATION_PERMISSION_INDEX = 0
        internal const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
        internal const val ACTION_GEOFENCE_EVENT =
            "SaveReminder.reminder.action.ACTION_GEOFENCE_EVENT"
        internal const val GEOFENCE_RADIUS_IN_METERS = 100f
        internal const val GEOFENCE_EXPIRATION_IN_MILLISECONDS = Geofence.NEVER_EXPIRE
    }

}