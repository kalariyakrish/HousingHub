package com.example.housinghub

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
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
    private val totalSteps = 4

    // Cloudinary configuration constants
    private companion object {
        const val CLOUDINARY_CLOUD_NAME = "dp82wqtwj"
        const val CLOUDINARY_API_KEY = "276822923496737"
        const val CLOUDINARY_API_SECRET = "jeVfRjGfTXn2OXW1rA-8i2c8qqg"
        private const val MAP_PICKER_REQUEST = 1002
    }
    private val selectedImages = mutableListOf<Uri>()
    private val selectedVideos = mutableListOf<Uri>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var map: GoogleMap? = null
    private var currentLocation: LatLng? = null
    private lateinit var imageSliderAdapter: ImageSliderAdapter
    private lateinit var videoPreviewAdapter: VideoPreviewAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerAddPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize fusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize adapters
        imageSliderAdapter = ImageSliderAdapter(
            onImageClick = { position ->
                if (position < selectedImages.size) {
                    viewFullImage(selectedImages[position])
                }
            },
            onDeleteClick = { position ->
                if (position < selectedImages.size) {
                    selectedImages.removeAt(position)
                    updateImagePreview()
                }
            }
        )
        videoPreviewAdapter = VideoPreviewAdapter(
            this,
            onVideoClick = { position ->
                if (position < selectedVideos.size) {
                    playVideo(selectedVideos[position])
                }
            },
            onDeleteClick = { position ->
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

        // Test Cloudinary connection (already initialized in App.kt)
        testCloudinaryConnection()

        // Setup button click listeners
        setupButtonListeners()

        // Initialize ViewFlipper to show first step
        binding.viewFlipper.displayedChild = 0

        // Update UI for first step
        updateStepUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoPreviewAdapter.onDestroy()
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        uris?.let {
            selectedImages.addAll(it)
            updateImagePreview()
        }
    }

    private fun updateImagePreview() {
        if (selectedImages.isEmpty()) {
            binding.tvNoPhotos.visibility = View.VISIBLE
            binding.rvPhotos.visibility = View.GONE
            binding.tvPhotoCount.visibility = View.GONE
        } else {
            binding.tvNoPhotos.visibility = View.GONE
            binding.rvPhotos.visibility = View.VISIBLE
            binding.tvPhotoCount.visibility = View.VISIBLE
            binding.tvPhotoCount.text = "${selectedImages.size} photos selected"
            imageSliderAdapter.submitList(selectedImages.map { it.toString() })
        }
    }

    private val videoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        uris?.let {
            selectedVideos.addAll(it)
            updateVideoPreview()
        }
    }

    private fun updateVideoPreview() {
        if (selectedVideos.isEmpty()) {
            binding.tvNoVideos.visibility = View.VISIBLE
            binding.rvVideos.visibility = View.GONE
            binding.tvVideoCount.visibility = View.GONE
        } else {
            binding.tvNoVideos.visibility = View.GONE
            binding.rvVideos.visibility = View.VISIBLE
            binding.tvVideoCount.visibility = View.VISIBLE
            binding.tvVideoCount.text = "${selectedVideos.size} videos selected"
            videoPreviewAdapter.submitList(selectedVideos.toList())
        }
    }

    @SuppressLint("MissingPermission")
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                getCurrentLocation()
                map?.isMyLocationEnabled = true
            }
            else -> {
                showErrorDialog(getString(R.string.location_permission_required))
            }
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun setupMap() {
        try {
            val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
            if (mapFragment == null) {
                android.util.Log.e("OwnerAddPropertyActivity", "Map fragment not found")
                showErrorDialog(getString(R.string.error, "Map fragment not found"))
                return
            }

            mapFragment.getMapAsync { googleMap ->
                map = googleMap
                map?.uiSettings?.apply {
                    isZoomControlsEnabled = true
                    isCompassEnabled = true
                }

                // Set map click listener
                map?.setOnMapClickListener { latLng ->
                    currentLocation = latLng
                    updateMapLocation(latLng)
                }

                // Check for location permission and enable My Location layer
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    map?.isMyLocationEnabled = true
                    getCurrentLocation()
                } else {
                    checkLocationPermission()
                }

                // Set initial location
                currentLocation?.let { location ->
                    updateMapLocation(location)
                } ?: run {
                    val defaultLocation = LatLng(0.0, 0.0)
                    updateMapLocation(defaultLocation)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OwnerAddPropertyActivity", "Error setting up map: ${e.message}")
            showErrorDialog(getString(R.string.error, e.message))
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
                                delay(1000)
                                val location = getLocationFromAddress(address)
                                withContext(Dispatchers.Main) {
                                    updateMapLocation(location)
                                    currentLocation = location
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("OwnerAddPropertyActivity", "Error in address autocomplete: ${e.message}")
                            }
                        }
                    }
                }
            }
        })
    }

    private suspend fun getLocationFromAddress(address: String): LatLng {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@OwnerAddPropertyActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(address, 1)
                addresses?.firstOrNull()?.let { address ->
                    LatLng(address.latitude, address.longitude)
                } ?: throw Exception("Location not found")
            } catch (e: Exception) {
                throw Exception("Error geocoding address: ${e.message}")
            }
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
        val locationText = "Lat: ${String.format("%.6f", location.latitude)}, Long: ${String.format("%.6f", location.longitude)}"
        binding.tvLocationStatus.text = locationText

        // Update address field if empty
        if (binding.etAddress.text.isNullOrEmpty()) {
            getAddressFromLocation(location)
        }
    }

    private fun testCloudinaryConnection() {
        try {
            val mediaManager = MediaManager.get()
            Log.d("OwnerAddProperty", "MediaManager instance: ${mediaManager != null}")
            Log.d("OwnerAddProperty", "Cloudinary config check passed")
        } catch (e: Exception) {
            Log.e("OwnerAddProperty", "Cloudinary connection test failed: ${e.message}")
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

    private fun showSuccessWithWarningDialog(warningMessage: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.success)
            .setMessage("Property added successfully.\n\nNote: $warningMessage")
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
                map?.isMyLocationEnabled = true
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
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
                    } ?: showErrorDialog(getString(R.string.could_not_get_location))
                }
                .addOnFailureListener { e ->
                    showErrorDialog(getString(R.string.error_getting_location, e.message))
                }
        } catch (e: SecurityException) {
            showErrorDialog(getString(R.string.location_permission_required))
        }
    }

    private fun getAddressFromLocation(latLng: LatLng) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@OwnerAddPropertyActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                withContext(Dispatchers.Main) {
                    addresses?.firstOrNull()?.let { address ->
                        val addressText = address.getAddressLine(0)
                        binding.etAddress.setText(addressText)
                    } ?: showErrorDialog(getString(R.string.could_not_get_address))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorDialog(getString(R.string.error_getting_address, e.message))
                }
            }
        }
    }

    private fun startMapPicker() {
        val intent = Intent(this, MapLocationPickerActivity::class.java)
        currentLocation?.let {
            intent.putExtra("latitude", it.latitude)
            intent.putExtra("longitude", it.longitude)
        }
        startActivityForResult(intent, MAP_PICKER_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MAP_PICKER_REQUEST && resultCode == RESULT_OK) {
            val latitude = data?.getDoubleExtra("latitude", 0.0) ?: return
            val longitude = data.getDoubleExtra("longitude", 0.0)
            val address = data.getStringExtra("address") ?: ""

            currentLocation = LatLng(latitude, longitude)
            binding.etAddress.setText(address)
            updateMapLocation(currentLocation!!)
        }
    }

    private fun setupStepNavigation() {
        updateStepUI()

        binding.btnNext.setOnClickListener {
            if (currentStep < totalSteps) {
                currentStep++
                binding.viewFlipper.showNext()
                updateStepUI()
            } else {
                validateAndSubmitProperty()
            }
        }

        binding.btnPrevious.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                binding.viewFlipper.showPrevious()
                updateStepUI()
            } else {
                finish()
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun updateStepUI() {
        val progress = ((currentStep.toFloat() / totalSteps) * 100).toInt()
        binding.progressIndicator.progress = progress
        binding.tvStepIndicator.text = "Step $currentStep/$totalSteps"

        binding.btnNext.text = if (currentStep == totalSteps) getString(R.string.ok) else "Next"
        binding.btnPrevious.visibility = if (currentStep == 1) View.GONE else View.VISIBLE

        if (currentStep == 2 && currentLocation == null) {
            checkLocationPermission()
        }
    }

    private fun setupButtonListeners() {
        binding.btnAddPhotos.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnAddVideos.setOnClickListener {
            videoPickerLauncher.launch("video/*")
        }

        binding.btnCurrentLocation.setOnClickListener {
            checkLocationPermission()
        }

        binding.btnPickLocation.setOnClickListener {
            startMapPicker()
        }

        binding.etAddress.setOnClickListener {
            checkLocationPermission()
        }
    }

    private fun validateAndSubmitProperty() {
        if (!validateBasicInfo() || !validateLocation() || !validateDetails() || !validateMedia() || !validateMediaFiles()) {
            return
        }

        val loadingDialog = showLoadingDialog()

        uploadMediaFiles { mediaUrls ->
            val property = createPropertyObject(mediaUrls)
            savePropertyToFirestore(property, loadingDialog)
        }
    }

    private fun validateBasicInfo(): Boolean {
        binding.tilPropertyTitle.error = null
        binding.tilPropertyType.error = null
        binding.tilPrice.error = null

        if (binding.etPropertyTitle.text.isNullOrBlank()) {
            binding.tilPropertyTitle.error = getString(R.string.property_title_required)
            binding.viewFlipper.displayedChild = 0
            currentStep = 1
            updateStepUI()
            return false
        }

        if (binding.etPropertyType.text.isNullOrBlank()) {
            binding.tilPropertyType.error = getString(R.string.property_type_required)
            binding.viewFlipper.displayedChild = 0
            currentStep = 1
            updateStepUI()
            return false
        }

        if (binding.etPrice.text.isNullOrBlank()) {
            binding.tilPrice.error = getString(R.string.price_required)
            binding.viewFlipper.displayedChild = 0
            currentStep = 1
            updateStepUI()
            return false
        }

        try {
            binding.etPrice.text.toString().toDouble()
        } catch (e: NumberFormatException) {
            binding.tilPrice.error = getString(R.string.invalid_price_format)
            binding.viewFlipper.displayedChild = 0
            currentStep = 1
            updateStepUI()
            return false
        }

        return true
    }

    private fun validateLocation(): Boolean {
        if (binding.etAddress.text.isNullOrBlank()) {
            showErrorDialog(getString(R.string.address_required))
            binding.viewFlipper.displayedChild = 1
            currentStep = 2
            updateStepUI()
            return false
        }

        if (currentLocation == null) {
            showErrorDialog(getString(R.string.select_location))
            binding.viewFlipper.displayedChild = 1
            currentStep = 2
            updateStepUI()
            return false
        }

        return true
    }

    private fun validateDetails(): Boolean {
        return true // Add validation logic for details step if needed
    }

    private fun validateMedia(): Boolean {
        if (selectedImages.isEmpty()) {
            showErrorDialog(getString(R.string.image_required))
            binding.viewFlipper.displayedChild = 3
            currentStep = 4
            updateStepUI()
            return false
        }
        return true
    }

    private fun initCloudinaryFallback() {
        Log.d("OwnerAddProperty", "Attempting backup Cloudinary initialization")
        val config = hashMapOf(
            "cloud_name" to "dp82wqtwj"
        )
        try {
            MediaManager.init(this, config)
            Log.d("OwnerAddProperty", "Backup Cloudinary initialization successful")
        } catch (e: IllegalStateException) {
            Log.w("OwnerAddProperty", "Cloudinary already initialized during fallback: ${e.message}")
        } catch (e: Exception) {
            Log.e("OwnerAddProperty", "Backup Cloudinary initialization failed: ${e.message}")
        }
    }

    private fun validateMediaFiles(): Boolean {
        val maxImageSize = 10 * 1024 * 1024 // 10MB for images
        val maxVideoSize = 50 * 1024 * 1024 // 50MB for videos

        for (uri in selectedImages) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val size = inputStream?.available() ?: 0
                inputStream?.close()

                if (size > maxImageSize) {
                    showErrorDialog("Image file is too large (max 10MB). Please select a smaller image.")
                    return false
                }
            } catch (e: Exception) {
                Log.w("OwnerAddProperty", "Could not check image file size: ${e.message}")
            }
        }

        for (uri in selectedVideos) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val size = inputStream?.available() ?: 0
                inputStream?.close()

                if (size > maxVideoSize) {
                    showErrorDialog("Video file is too large (max 50MB). Please select a smaller video.")
                    return false
                }
            } catch (e: Exception) {
                Log.w("OwnerAddProperty", "Could not check video file size: ${e.message}")
            }
        }

        return true
    }

    private fun uploadMediaFiles(callback: (MediaUrls) -> Unit) {
        val imageUrls = mutableListOf<String>()
        val videoUrls = mutableListOf<String>()

        var totalFiles = selectedImages.size + selectedVideos.size
        var uploadedFiles = 0
        var hasError = false

        Log.d("OwnerAddProperty", "Total media files to process: $totalFiles")
        Log.d("OwnerAddProperty", "Images: ${selectedImages.size}, Videos: ${selectedVideos.size}")
        Log.d("OwnerAddProperty", "Using Cloudinary unsigned upload with preset: HousingHub")

        if (totalFiles == 0) {
            // No media files to upload
            Log.d("OwnerAddProperty", "No media files to upload, proceeding without media")
            callback(MediaUrls(imageUrls, videoUrls))
            return
        }

        // Add timeout mechanism to prevent infinite loading (increased for large files)
        val timeoutJob = lifecycleScope.launch {
            delay(60000) // 60 seconds timeout for large media files
            if (uploadedFiles < totalFiles && !hasError) {
                hasError = true
                Log.e("OwnerAddProperty", "Upload timeout - proceeding with uploaded files")
                Log.d("OwnerAddProperty", "Uploaded: $uploadedFiles/$totalFiles files")
                runOnUiThread {
                    callback(MediaUrls(imageUrls, videoUrls))
                }
            }
        }

        Log.d("OwnerAddProperty", "Starting upload of $totalFiles files to Cloudinary")

        // Check if MediaManager is initialized, if not, try to initialize it
        try {
            val mediaManager = MediaManager.get()
            if (mediaManager == null) {
                Log.e("OwnerAddProperty", "MediaManager not initialized - attempting backup initialization")
                initCloudinaryFallback()
            } else {
                Log.d("OwnerAddProperty", "MediaManager is properly initialized")
            }
        } catch (e: Exception) {
            Log.e("OwnerAddProperty", "MediaManager initialization check failed: ${e.message}")
            Log.i("OwnerAddProperty", "Attempting backup initialization")
            initCloudinaryFallback()
        }

        // Upload images
        selectedImages.forEachIndexed { index, uri ->
            Log.d("OwnerAddProperty", "Processing image $index: $uri")
            try {
                // Try simple unsigned upload first
                MediaManager.get().upload(uri)
                    .unsigned("HousingHub") // Use unsigned upload preset
                    .option("resource_type", "image")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            Log.d("OwnerAddProperty", "Image upload started: $requestId")
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            val progress = (bytes * 100 / totalBytes).toInt()
                            Log.d("OwnerAddProperty", "Image upload progress: $progress%")
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            if (hasError) return // Ignore if already timed out

                            val imageUrl = resultData["secure_url"] as? String
                            if (imageUrl != null) {
                                imageUrls.add(imageUrl)
                                Log.d("OwnerAddProperty", "Image uploaded successfully: $imageUrl")
                            }
                            uploadedFiles++

                            if (uploadedFiles == totalFiles) {
                                timeoutJob.cancel()
                                Log.d("OwnerAddProperty", "All files uploaded successfully")
                                runOnUiThread {
                                    callback(MediaUrls(imageUrls, videoUrls))
                                }
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            if (hasError) return // Ignore if already timed out

                            Log.e("OwnerAddProperty", "Image upload failed: ${error.description}")
                            Log.e("OwnerAddProperty", "Error code: ${error.code}")
                            uploadedFiles++

                            if (uploadedFiles == totalFiles) {
                                timeoutJob.cancel()
                                runOnUiThread {
                                    callback(MediaUrls(imageUrls, videoUrls))
                                }
                            }
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            Log.w("OwnerAddProperty", "Image upload rescheduled: ${error.description}")
                        }
                    })
                    .dispatch()
            } catch (e: Exception) {
                Log.e("OwnerAddProperty", "Error starting image upload", e)
                uploadedFiles++
                if (uploadedFiles == totalFiles && !hasError) {
                    timeoutJob.cancel()
                    runOnUiThread {
                        callback(MediaUrls(imageUrls, videoUrls))
                    }
                }
            }
        }

        // Upload videos
        selectedVideos.forEachIndexed { index, uri ->
            Log.d("OwnerAddProperty", "Processing video $index: $uri")
            try {
                // Try simple unsigned upload first
                MediaManager.get().upload(uri)
                    .unsigned("HousingHub") // Use unsigned upload preset
                    .option("resource_type", "video")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            Log.d("OwnerAddProperty", "Video upload started: $requestId")
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            val progress = (bytes * 100 / totalBytes).toInt()
                            Log.d("OwnerAddProperty", "Video upload progress: $progress%")
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            if (hasError) return // Ignore if already timed out

                            val videoUrl = resultData["secure_url"] as? String
                            if (videoUrl != null) {
                                videoUrls.add(videoUrl)
                                Log.d("OwnerAddProperty", "Video uploaded successfully: $videoUrl")
                            }
                            uploadedFiles++

                            if (uploadedFiles == totalFiles) {
                                timeoutJob.cancel()
                                Log.d("OwnerAddProperty", "All files uploaded successfully")
                                runOnUiThread {
                                    callback(MediaUrls(imageUrls, videoUrls))
                                }
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            if (hasError) return // Ignore if already timed out

                            Log.e("OwnerAddProperty", "Video upload failed: ${error.description}")
                            Log.e("OwnerAddProperty", "Error code: ${error.code}")
                            uploadedFiles++

                            if (uploadedFiles == totalFiles) {
                                timeoutJob.cancel()
                                runOnUiThread {
                                    callback(MediaUrls(imageUrls, videoUrls))
                                }
                            }
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            Log.w("OwnerAddProperty", "Video upload rescheduled: ${error.description}")
                        }
                    })
                    .dispatch()
            } catch (e: Exception) {
                Log.e("OwnerAddProperty", "Error starting video upload", e)
                uploadedFiles++
                if (uploadedFiles == totalFiles && !hasError) {
                    timeoutJob.cancel()
                    runOnUiThread {
                        callback(MediaUrls(imageUrls, videoUrls))
                    }
                }
            }
        }
    }

    private fun createPropertyObject(mediaUrls: MediaUrls): Map<String, Any> {
        return mapOf(
            "title" to binding.etPropertyTitle.text.toString(),
            "type" to binding.etPropertyType.text.toString(),
            "price" to binding.etPrice.text.toString().toDouble(),
            "address" to binding.etAddress.text.toString(),
            "latitude" to (currentLocation?.latitude ?: 0.0),
            "longitude" to (currentLocation?.longitude ?: 0.0),
            "bedrooms" to (binding.etBedrooms.text.toString().toIntOrNull() ?: 0),
            "bathrooms" to (binding.etBathrooms.text.toString().toIntOrNull() ?: 0),
            "description" to binding.etDescription.text.toString(),
            "images" to mediaUrls.images,
            "videos" to mediaUrls.videos,
            "ownerId" to (auth.currentUser?.uid ?: ""),
            "createdAt" to com.google.firebase.Timestamp.now(),
            "isAvailable" to true
        )
    }

    @SuppressLint("StringFormatInvalid")
    private fun savePropertyToFirestore(property: Map<String, Any>, loadingDialog: androidx.appcompat.app.AlertDialog) {
        val ownerEmail = auth.currentUser?.email ?: run {
            loadingDialog.dismiss()
            showErrorDialog(getString(R.string.user_not_authenticated))
            return
        }

        val propertyId = db.collection("Properties")
            .document(ownerEmail)
            .collection("Available")
            .document().id

        val propertyWithId = property + mapOf("id" to propertyId)

        db.collection("Properties")
            .document(ownerEmail)
            .set(mapOf("email" to ownerEmail))
            .addOnSuccessListener {
                db.collection("Properties")
                    .document(ownerEmail)
                    .collection("Available")
                    .document(propertyId)
                    .set(propertyWithId)
                    .addOnSuccessListener {
                        loadingDialog.dismiss()

                        // Check if media was supposed to be uploaded but failed
                        val expectedMediaCount = selectedImages.size + selectedVideos.size
                        val actualMediaCount = (propertyWithId["images"] as? List<*>)?.size
                            ?: 0 + (propertyWithId["videos"] as? List<*>)?.size!!
                            ?: 0

                        if (expectedMediaCount > 0 && actualMediaCount < expectedMediaCount) {
                            val missedCount = expectedMediaCount - actualMediaCount
                            showSuccessWithWarningDialog("Some media files ($missedCount) failed to upload. You can edit the property later to add them.")
                        } else {
                            showSuccessDialog()
                        }
                    }
                    .addOnFailureListener { e ->
                        loadingDialog.dismiss()
                        showErrorDialog(getString(R.string.error_adding_property, e.message))
                    }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                showErrorDialog(getString(R.string.error_creating_owner_document, e.message))
            }
    }

    private fun playVideo(videoUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(videoUri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            showErrorDialog(getString(R.string.could_not_play_video, e.message))
        }
    }

    private fun viewFullImage(imageUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(imageUri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            showErrorDialog(getString(R.string.could_not_view_image, e.message))
        }
    }
}