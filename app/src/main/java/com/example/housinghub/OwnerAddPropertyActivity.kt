package com.example.housinghub

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.cloudinary.android.MediaManager
import com.example.housinghub.adapters.ImageSliderAdapter
import com.example.housinghub.adapters.VideoPreviewAdapter
import com.example.housinghub.databinding.ActivityOwnerAddPropertyBinding
import com.example.housinghub.owner.MapLocationPickerActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// Data class to hold media URLs
data class MediaUrls(val images: MutableList<String>, val videos: MutableList<String>)

class OwnerAddPropertyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerAddPropertyBinding
    private var currentStep = 1
    private var totalSteps = 4 // Removed preview step
    private val selectedImages = mutableListOf<Uri>()
    private val selectedVideos = mutableListOf<Uri>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var map: GoogleMap? = null
    private var currentLocation: LatLng? = null
    private lateinit var imageSliderAdapter: ImageSliderAdapter
    private lateinit var videoPreviewAdapter: VideoPreviewAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up the VideoPreviewAdapter resources
        videoPreviewAdapter.onDestroy()
        // No need to set binding to null as it's not nullable
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerAddPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize fusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Initialize adapters
        imageSliderAdapter = ImageSliderAdapter(
            onImageClick = { position ->
                // Handle image click (view full image)
                val imageUri = selectedImages[position]
                viewFullImage(imageUri)
            },
            onDeleteClick = { position ->
                // Handle delete click
                if (position < selectedImages.size) {
                    selectedImages.removeAt(position)
                    updateImagePreview()
                }
            }
        )
        videoPreviewAdapter = VideoPreviewAdapter(
            this,
            onVideoClick = { position ->
                // Handle video click (play video)
                if (position < selectedVideos.size) {
                    val videoUri = selectedVideos[position]
                    playVideo(videoUri)
                }
            },
            onDeleteClick = { position ->
                // Handle delete click
                if (position < selectedVideos.size) {
                    selectedVideos.removeAt(position)
                    updateVideoPreview()
                }
            }
        )
        
        // Setup recycler views
        binding.rvPhotos.layoutManager = GridLayoutManager(this, 3)
        binding.rvPhotos.adapter = imageSliderAdapter
        
        binding.rvVideos.layoutManager = LinearLayoutManager(this)
        binding.rvVideos.adapter = videoPreviewAdapter
        
        // Setup UI components
        setupStepNavigation()
        setupMap()
        setupAddressAutocomplete()
        initCloudinary()
        
        // Setup button click listeners
        setupButtonListeners()
        
        // Initialize ViewFlipper to show first step
        binding.viewFlipper.displayedChild = 0
        
        // Update UI for first step
        updateStepUI()
    }

    // ImagePicker and VideoPicker
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        uris?.let {
            selectedImages.addAll(it)
            updateImagePreview()
        }
    }

    private fun updateImagePreview() {
        // Show or hide the image preview section based on whether images are selected
        if (selectedImages.isEmpty()) {
            binding.tvNoPhotos.visibility = android.view.View.VISIBLE
            binding.rvPhotos.visibility = android.view.View.GONE
            binding.tvPhotoCount.visibility = android.view.View.GONE
        } else {
            binding.tvNoPhotos.visibility = android.view.View.GONE
            binding.rvPhotos.visibility = android.view.View.VISIBLE
            binding.tvPhotoCount.visibility = android.view.View.VISIBLE
            binding.tvPhotoCount.text = "${selectedImages.size} photos selected"
            
            // Convert Uri objects to strings and update the adapter
            val imageUriStrings = selectedImages.map { it.toString() }
            imageSliderAdapter.submitList(imageUriStrings)
        }
    }

    private val videoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        uris?.let {
            selectedVideos.addAll(it)
            updateVideoPreview()
        }
    }

    private fun updateVideoPreview() {
        // Show or hide the video preview section based on whether videos are selected
        if (selectedVideos.isEmpty()) {
            binding.tvNoVideos.visibility = android.view.View.VISIBLE
            binding.rvVideos.visibility = android.view.View.GONE
            binding.tvVideoCount.visibility = android.view.View.GONE
        } else {
            binding.tvNoVideos.visibility = android.view.View.GONE
            binding.rvVideos.visibility = android.view.View.VISIBLE
            binding.tvVideoCount.visibility = android.view.View.VISIBLE
            binding.tvVideoCount.text = "${selectedVideos.size} videos selected"
            
            // Update the adapter with the new list of videos
            videoPreviewAdapter.submitList(selectedVideos.toList())
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                getCurrentLocation()
            }
            else -> {
                showErrorDialog("Location permission is required to get current location")
            }
        }
    }

    private fun setupMap() {
        try {
            val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
            if (mapFragment == null) {
                // Log error or show message
                android.util.Log.e("OwnerAddPropertyActivity", "Map fragment not found")
                return
            }
            
            mapFragment.getMapAsync { googleMap ->
                map = googleMap
                map?.uiSettings?.apply {
                    isZoomControlsEnabled = true
                    isCompassEnabled = true
                }
                // Set default location (e.g., city center) or last known location
                currentLocation?.let { location ->
                    updateMapLocation(location)
                } ?: run {
                    // Set a default location if no current location
                    val defaultLocation = LatLng(0.0, 0.0)
                    updateMapLocation(defaultLocation)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OwnerAddPropertyActivity", "Error setting up map: ${e.message}")
        }
    }

    private fun setupAddressAutocomplete() {
        var searchJob: Job? = null
        
        binding.etAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                s?.toString()?.let { address ->
                    if (address.length >= 3) {
                        searchJob = lifecycleScope.launch {
                            try {
                                delay(1000) // Debounce for 1 second
                                val location = getLocationFromAddress(address)
                                withContext(Dispatchers.Main) {
                                    updateMapLocation(location)
                                }
                            } catch (e: Exception) {
                                // Handle error quietly
                            }
                        }
                    }
                }
            }
        })
    }

    private suspend fun getLocationFromAddress(address: String): LatLng {
        return withContext(Dispatchers.IO) {
            val geocoder = android.location.Geocoder(this@OwnerAddPropertyActivity)
            val addresses = geocoder.getFromLocationName(address, 1)
            addresses?.firstOrNull()?.let { address ->
                LatLng(address.latitude, address.longitude)
            } ?: throw Exception("Location not found")
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun updateMapLocation(location: LatLng) {
        currentLocation = location
        map?.apply {
            clear()
            addMarker(MarkerOptions().position(location))
            animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
        // Update the location status text to show latitude and longitude
        val locationText = "Lat: ${String.format("%.6f", location.latitude)}, Long: ${String.format("%.6f", location.longitude)}"
        binding.tvLocationStatus.text = locationText
    }

    private fun initCloudinary() {
        val config = hashMapOf(
            "cloud_name" to "your_cloud_name",
            "api_key" to "your_api_key",
            "api_secret" to "your_api_secret"
        )
        try {
            MediaManager.init(this, config)
        } catch (e: IllegalStateException) {
            // Already initialized
        }
    }

    private fun showLoadingDialog(): androidx.appcompat.app.AlertDialog {
        return MaterialAlertDialogBuilder(this)
            .setTitle(R.string.adding_property)
            .setMessage(R.string.please_wait)
            .setCancelable(false)
            .create()
            .apply { show() }
    }

    private fun showSuccessDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.success)
            .setMessage(R.string.property_added_success)
            .setPositiveButton(R.string.ok) { _, _ -> finish() }
            .show()
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.error)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            else -> {
                locationPermissionRequest.launch(arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }
    }

    private fun getCurrentLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        updateMapLocation(latLng)
                        // Get address from location and update the address field
                        getAddressFromLocation(latLng)
                    } ?: showErrorDialog("Could not get current location")
                }
                .addOnFailureListener { e ->
                    showErrorDialog("Error getting location: ${e.message}")
                }
        } catch (e: SecurityException) {
            showErrorDialog("Location permission not granted")
        }
    }
    
    private fun getAddressFromLocation(latLng: LatLng) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    withContext(Dispatchers.Main) {
                        addresses?.firstOrNull()?.let { address ->
                            val addressText = address.getAddressLine(0)
                            binding.etAddress.setText(addressText)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showErrorDialog("Could not get address for the location")
                    }
                }
            }
        } catch (e: Exception) {
            showErrorDialog("Error getting address: ${e.message}")
        }
    }

    private fun startMapPicker() {
        val intent = Intent(this, MapLocationPickerActivity::class.java)
        currentLocation?.let {
            intent.putExtra("latitude", it.latitude)
            intent.putExtra("longitude", it.longitude)
        }
        startActivity(intent)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
        private const val MAP_PICKER_REQUEST = 1002
    }

    data class MediaUrls(
        val images: MutableList<String>,
        val videos: MutableList<String>
    )
    
    private fun setupStepNavigation() {
        // Update progress indicator and step text
        updateStepUI()
        
        // Next button click listener
        binding.btnNext.setOnClickListener {
            if (currentStep < totalSteps) {
                currentStep++
                binding.viewFlipper.showNext()
                updateStepUI()
            } else {
                // Submit property
                validateAndSubmitProperty()
            }
        }
        
        // Previous button click listener
        binding.btnPrevious.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                binding.viewFlipper.showPrevious()
                updateStepUI()
            } else {
                finish()
            }
        }
        
        // Back button in toolbar click listener
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun updateStepUI() {
        // Update progress indicator
        val progress = ((currentStep.toFloat() / totalSteps) * 100).toInt()
        binding.progressIndicator.progress = progress
        
        // Update step text
        binding.tvStepIndicator.text = "Step $currentStep/$totalSteps"
        
        // Update button text for last step
        if (currentStep == totalSteps) {
            binding.btnNext.text = "Submit"
        } else {
            binding.btnNext.text = "Next"
        }
        
        // Hide previous button on first step
        binding.btnPrevious.visibility = if (currentStep == 1) View.GONE else View.VISIBLE
        
        // If we're on step 2 (location), try to get current location and address
        if (currentStep == 2) {
            // Check if we already have a location, if not try to get it
            if (currentLocation == null) {
                checkLocationPermission()
            }
        }
    }
    
    private fun setupButtonListeners() {
        // Add images button
        binding.btnAddPhotos.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        
        // Add videos button
        binding.btnAddVideos.setOnClickListener {
            videoPickerLauncher.launch("video/*")
        }
        
        // Get current location button
        binding.btnCurrentLocation.setOnClickListener {
            checkLocationPermission()
        }
        
        // Pick on map button
        binding.btnPickLocation.setOnClickListener {
            startMapPicker()
        }
    }
    
    private fun validateAndSubmitProperty() {
        // Validate all fields
        if (!validateBasicInfo() || !validateLocation() || !validateDetails() || !validateMedia()) {
            return
        }
        
        // Show loading dialog
        val loadingDialog = showLoadingDialog()
        
        // Upload media files and get URLs
        uploadMediaFiles { mediaUrls ->
            // Create property object
            val property = createPropertyObject(mediaUrls)
            
            // Save to Firestore
            savePropertyToFirestore(property, loadingDialog)
        }
    }
    
    private fun validateBasicInfo(): Boolean {
        // Validate property title
        if (binding.etPropertyTitle.text.isNullOrBlank()) {
            binding.tilPropertyTitle.error = "Property title is required"
            binding.viewFlipper.displayedChild = 0 // Go to step 1
            currentStep = 1
            updateStepUI()
            return false
        }
        
        // Validate property type
        if (binding.etPropertyType.text.isNullOrBlank()) {
            binding.tilPropertyType.error = "Property type is required"
            binding.viewFlipper.displayedChild = 0 // Go to step 1
            currentStep = 1
            updateStepUI()
            return false
        }
        
        // Validate price
        if (binding.etPrice.text.isNullOrBlank()) {
            binding.tilPrice.error = "Price is required"
            binding.viewFlipper.displayedChild = 0 // Go to step 1
            currentStep = 1
            updateStepUI()
            return false
        }
        
        return true
    }
    
    private fun validateLocation(): Boolean {
        // Validate address
        if (binding.etAddress.text.isNullOrBlank()) {
            binding.viewFlipper.displayedChild = 1 // Go to step 2
            currentStep = 2
            updateStepUI()
            return false
        }
        
        // Validate map location
        if (currentLocation == null) {
            binding.viewFlipper.displayedChild = 1 // Go to step 2
            currentStep = 2
            updateStepUI()
            return false
        }
        
        return true
    }
    
    private fun validateDetails(): Boolean {
        // Add validation for details step
        return true
    }
    
    private fun validateMedia(): Boolean {
        // Validate at least one image
        if (selectedImages.isEmpty()) {
            binding.viewFlipper.displayedChild = 3 // Go to step 4
            currentStep = 4
            updateStepUI()
            return false
        }
        
        return true
    }
    
    private fun uploadMediaFiles(callback: (MediaUrls) -> Unit) {
        // For now, just return empty lists
        // In a real app, you would upload the files to Cloudinary or Firebase Storage
        callback(MediaUrls(mutableListOf(), mutableListOf()))
    }
    
    private fun createPropertyObject(mediaUrls: MediaUrls): Map<String, Any> {
        return mapOf(
            "title" to binding.etPropertyTitle.text.toString(),
            "type" to binding.etPropertyType.text.toString(),
            "price" to binding.etPrice.text.toString().toDouble(),
            "address" to binding.etAddress.text.toString(),
            "latitude" to (currentLocation?.latitude ?: 0.0),
            "longitude" to (currentLocation?.longitude ?: 0.0),
            "images" to mediaUrls.images,
            "videos" to mediaUrls.videos,
            "ownerId" to (auth.currentUser?.uid ?: ""),
            "createdAt" to com.google.firebase.Timestamp.now()
        )
    }
    
    private fun savePropertyToFirestore(property: Map<String, Any>, loadingDialog: androidx.appcompat.app.AlertDialog) {
        db.collection("properties")
            .add(property)
            .addOnSuccessListener {
                loadingDialog.dismiss()
                showSuccessDialog()
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                showErrorDialog("Error adding property: ${e.message}")
            }
    }
    
    // Dialog methods are defined elsewhere in the file
    
    private fun playVideo(videoUri: Uri) {
        try {
            // Create an intent to play the video
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(videoUri, "video/*")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            showErrorDialog("Could not play video: ${e.message}")
        }
    }
    
    private fun viewFullImage(imageUri: Uri) {
        try {
            // Create an intent to view the full image
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(imageUri, "image/*")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            showErrorDialog("Could not view image: ${e.message}")
        }
    }
}
