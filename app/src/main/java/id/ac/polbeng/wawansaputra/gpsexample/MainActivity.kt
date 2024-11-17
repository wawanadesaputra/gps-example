package id.ac.polbeng.wawansaputra.gpsexample

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import id.ac.polbeng.wawansaputra.gpsexample.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var trackingLocation: Boolean = false
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvLocation.text = ""
        binding.tvLocation.movementMethod = ScrollingMovementMethod()

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                }
                else -> {
                    binding.btnUpdate.isEnabled = false
                }
            }
        }

        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val builder = LocationSettingsRequest.Builder()
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationRequest = LocationRequest.create().apply {
                interval = TimeUnit.SECONDS.toMillis(20)
                fastestInterval = TimeUnit.SECONDS.toMillis(10)
                maxWaitTime = TimeUnit.SECONDS.toMillis(40)
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    if (locationResult.locations.isNotEmpty()) {
                        val location = locationResult.lastLocation
                        val latitude = location.latitude.toString()
                        val longitude = location.longitude.toString()
                        logResultsToScreen("$latitude, $longitude")
                    }
                }
            }
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this@MainActivity, 100)
                } catch (sendEx: IntentSender.SendIntentException) {
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                logResultsToScreen("${location?.latitude}, ${location?.longitude}")
            }

        binding.btnUpdate.setOnClickListener {
            if (!trackingLocation) {
                startLocationUpdates()
                trackingLocation = true
            } else {
                stopLocationUpdates()
                trackingLocation = false
            }
            updateButtonState(trackingLocation)
        }
    }

    private fun logResultsToScreen(output: String) {
        val outputWithPreviousLogs = "$output\n${binding.tvLocation.text}"
        binding.tvLocation.text = outputWithPreviousLogs
    }

    private fun updateButtonState(trackingLocation: Boolean) {
        if (trackingLocation) {
            binding.btnUpdate.text = getString(R.string.stop_update)
        } else {
            binding.btnUpdate.text = getString(R.string.start_update)
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        Log.d(TAG, "Request Location Updates")
    }

    private fun stopLocationUpdates() {
        val removeTask = fusedLocationClient.removeLocationUpdates(locationCallback)
        removeTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Location Callback removed.")
            } else {
                Log.d(TAG, "Failed to remove Location Callback.")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
}
