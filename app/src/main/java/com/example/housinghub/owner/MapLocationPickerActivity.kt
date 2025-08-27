package com.example.housinghub.owner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.housinghub.R
import com.example.housinghub.databinding.ActivityMapLocationPickerBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class MapLocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMapLocationPickerBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedLocation: LatLng? = null
    private var selectedAddress: String = ""

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnConfirmLocation.setOnClickListener {
            selectedLocation?.let { location ->
                val intent = Intent().apply {
                    putExtra("latitude", location.latitude)
                    putExtra("longitude", location.longitude)
                    putExtra("address", selectedAddress)
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
            } ?: Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isScrollGesturesEnabled = true
            isMapToolbarEnabled = true
        }

        // Set up map click listener
        map.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            updateMapMarker()
            getAddressFromLocation(latLng)
        }
        
        // Set up camera move listener to update address
        map.setOnCameraIdleListener {
            map.cameraPosition.target.let { latLng ->
                selectedLocation = latLng
                getAddressFromLocation(latLng)
            }
        }

        // Check and request location permission if needed
        if (checkLocationPermission()) {
            setupMap()
        } else {
            requestLocationPermission()
        }

        // If we have initial location from intent, use it
        val initialLat = intent.getDoubleExtra("latitude", 0.0)
        val initialLng = intent.getDoubleExtra("longitude", 0.0)
        if (initialLat != 0.0 && initialLng != 0.0) {
            val initialLocation = LatLng(initialLat, initialLng)
            selectedLocation = initialLocation
            updateMapMarker()
            getAddressFromLocation(initialLocation)
        }
    }

    private fun setupMap() {
        try {
            map.isMyLocationEnabled = true
            getCurrentLocation()
        } catch (e: SecurityException) {
            requestLocationPermission()
        }
    }

    private fun getCurrentLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    if (selectedLocation == null) {
                        selectedLocation = currentLatLng
                        updateMapMarker()
                        getAddressFromLocation(currentLatLng)
                    }
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        } catch (e: SecurityException) {
            showToast("Location permission not granted")
        }
    }

    private fun updateMapMarker() {
        map.clear()
        selectedLocation?.let { location ->
            map.addMarker(MarkerOptions()
                .position(location)
                .title("Selected Location")
            )
            map.animateCamera(CameraUpdateFactory.newLatLng(location))
        }
    }

    private fun getAddressFromLocation(latLng: LatLng) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            
            addresses?.firstOrNull()?.let { address ->
                val addressText = address.getAddressLine(0)
                binding.tvSelectedAddress.text = addressText
                selectedAddress = addressText
                // Update map camera
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Could not get address for selected location")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Handle confirm button click
    private fun setupClickListeners() {
        binding.btnConfirmLocation.setOnClickListener {
            selectedLocation?.let { location ->
                val resultIntent = Intent().apply {
                    putExtra("latitude", location.latitude)
                    putExtra("longitude", location.longitude)
                    putExtra("address", selectedAddress)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } ?: showToast("Please select a location first")
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupMap()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
