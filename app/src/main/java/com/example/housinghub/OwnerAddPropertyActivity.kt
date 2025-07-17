package com.example.housinghub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class OwnerAddPropertyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerAddPropertyBinding
    private var currentStep = 1
    private val totalSteps = 5
    private val selectedImages = mutableListOf<Uri>()
    private val selectedVideos = mutableListOf<Uri>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var map: GoogleMap? = null
    private var currentLocation: LatLng? = null
    private lateinit var imageSliderAdapter: ImageSliderAdapter
    private lateinit var videoPreviewAdapter: VideoPreviewAdapter
    private val storage = FirebaseStorage.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        uris?.let { 
            selectedImages.addAll(it)
            updateImagePreview()
        }
    }

    private val videoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        uris?.let {
            selectedVideos.addAll(it)
            updateVideoPreview()
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Permission granted, get current location
                getCurrentLocation()
            }
            else -> {
                showErrorDialog("Location permission is required to get current location")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerAddPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupUI()
        setupMap()
    }

    private fun setupMap() {
        // Initialize map fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            map = googleMap
            // Set default location or last selected location
            currentLocation?.let { location ->
                map?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            }
        }
    }

    private fun setupUI() {
        // Initialize ViewFlipper
        binding.viewFlipper.displayedChild = 0

        // Setup RecyclerViews
        setupImageRecyclerView()
        setupVideoRecyclerView()

        // Update progress and step indicator
        updateProgress()

        // Setup click listeners
        setupClickListeners()
    }

    private fun setupImageRecyclerView() {
        imageSliderAdapter = ImageSliderAdapter { position ->
            showFullScreenImage(position)
        }
        binding.rvPhotos.apply {
            layoutManager = GridLayoutManager(this@OwnerAddPropertyActivity, 3)
            adapter = imageSliderAdapter
        }
    }

    private fun setupVideoRecyclerView() {
        videoPreviewAdapter = VideoPreviewAdapter(this) { position ->
            playVideo(position)
        }
        binding.rvVideos.apply {
            layoutManager = LinearLayoutManager(this@OwnerAddPropertyActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = videoPreviewAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnNext.setOnClickListener {
            if (validateCurrentStep()) {
                if (currentStep < totalSteps) {
                    currentStep++
                    binding.viewFlipper.showNext()
                    updateProgress()
                } else {
                    submitProperty()
                }
            }
        }

        binding.btnPrevious.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                binding.viewFlipper.showPrevious()
                updateProgress()
            }
        }

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

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            1 -> {
                // Basic Info Validation
                when {
                    binding.etPropertyTitle.text.isNullOrBlank() -> {
                        showErrorDialog("Please enter property title")
                        false
                    }
                    binding.etPropertyType.text.isNullOrBlank() -> {
                        showErrorDialog("Please enter property type")
                        false
                    }
                    binding.etPrice.text.isNullOrBlank() -> {
                        showErrorDialog("Please enter property price")
                        false
                    }
                    else -> true
                }
            }
            2 -> {
                // Location Validation
                when {
                    binding.etAddress.text.isNullOrBlank() -> {
                        showErrorDialog("Please enter property address")
                        false
                    }
                    currentLocation == null -> {
                        showErrorDialog("Please select property location")
                        false
                    }
                    else -> true
                }
            }
            3 -> {
                // Details Validation
                when {
                    binding.etBedrooms.text.isNullOrBlank() -> {
                        showErrorDialog("Please enter number of bedrooms")
                        false
                    }
                    binding.etBathrooms.text.isNullOrBlank() -> {
                        showErrorDialog("Please enter number of bathrooms")
                        false
                    }
                    binding.etDescription.text.isNullOrBlank() -> {
                        showErrorDialog("Please enter property description")
                        false
                    }
                    else -> true
                }
            }
            4 -> {
                // Media Validation
                if (selectedImages.isEmpty()) {
                    showErrorDialog("Please add at least one photo")
                    false
                } else true
            }
            5 -> {
                // Final Review
                true
            }
            else -> false
        }
    }

    private fun updateImagePreview() {
        imageSliderAdapter.submitList(selectedImages.map { it.toString() })
        updatePreview()
    }

    private fun updateVideoPreview() {
        videoPreviewAdapter.submitList(selectedVideos)
        updatePreview()
    }

    private fun updatePreview() {
        // Update image preview visibility
        binding.rvPhotos.visibility = if (selectedImages.isEmpty()) View.GONE else View.VISIBLE
        binding.tvNoPhotos.visibility = if (selectedImages.isEmpty()) View.VISIBLE else View.GONE
        binding.tvPhotoCount.apply {
            visibility = if (selectedImages.isEmpty()) View.GONE else View.VISIBLE
            text = getString(R.string.tvPhotoCount, selectedImages.size)
        }
        
        // Update video preview visibility
        binding.rvVideos.visibility = if (selectedVideos.isEmpty()) View.GONE else View.VISIBLE
        binding.tvNoVideos.visibility = if (selectedVideos.isEmpty()) View.VISIBLE else View.GONE
        binding.tvVideoCount.apply {
            visibility = if (selectedVideos.isEmpty()) View.GONE else View.VISIBLE
            text = getString(R.string.tvVideoCount, selectedVideos.size)
        }
    }

    private fun showFullScreenImage(position: Int) {
        val intent = Intent(this, ImageViewerActivity::class.java).apply {
            putStringArrayListExtra("images", ArrayList(selectedImages.map { it.toString() }))
            putExtra("position", position)
        }
        startActivity(intent)
    }

    private fun playVideo(position: Int) {
        val videoUri = selectedVideos[position]
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(videoUri, "video/*")
        }
        startActivity(intent)
    }

    private fun startMapPicker() {
        val intent = Intent(this, MapLocationPickerActivity::class.java)
        currentLocation?.let {
            intent.putExtra("latitude", it.latitude)
            intent.putExtra("longitude", it.longitude)
        }
        startActivityForResult(intent, MAP_PICKER_REQUEST)
    }

    private fun updateProgress() {
        binding.progressIndicator.progress = (currentStep * 100) / totalSteps
        binding.tvStepIndicator.text = "Step $currentStep/$totalSteps"
        binding.btnNext.text = if (currentStep == totalSteps) "Submit" else "Next"
        binding.btnPrevious.visibility = if (currentStep == 1) View.GONE else View.VISIBLE
    }

    private fun submitProperty() {
        val propertyId = UUID.randomUUID().toString()
        val userId = auth.currentUser?.uid ?: run {
            showErrorDialog(getString(R.string.error_not_logged_in))
            return
        }

        // Show loading dialog
        val loadingDialog = showLoadingDialog()

        // Upload media files first
        uploadMediaFiles(propertyId) { mediaUrls ->
            // Create property data
            val propertyData = hashMapOf(
                "id" to propertyId,
                "ownerId" to userId,
                "title" to binding.etPropertyTitle.text.toString(),
                "type" to binding.etPropertyType.text.toString(),
                "price" to (binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0),
                "address" to binding.etAddress.text.toString(),
                "location" to hashMapOf(
                    "latitude" to (currentLocation?.latitude ?: 0.0),
                    "longitude" to (currentLocation?.longitude ?: 0.0)
                ),
                "bedrooms" to (binding.etBedrooms.text.toString().toIntOrNull() ?: 0),
                "bathrooms" to (binding.etBathrooms.text.toString().toIntOrNull() ?: 0),
                "description" to binding.etDescription.text.toString(),
                "images" to mediaUrls.images,
                "videos" to mediaUrls.videos,
                "createdAt" to System.currentTimeMillis(),
                "isAvailable" to true
            )

            // Save to Firestore
            db.collection("properties")
                .document(propertyId)
                .set(propertyData)
                .addOnSuccessListener {
                    loadingDialog.dismiss()
                    showSuccessDialog()
                }
                .addOnFailureListener { e ->
                    loadingDialog.dismiss()
                    showErrorDialog(e.message ?: getString(R.string.error_adding_property))
                }
        }
    }

    private fun uploadMediaFiles(propertyId: String, onComplete: (MediaUrls) -> Unit) {
        val mediaUrls = MediaUrls(mutableListOf(), mutableListOf())
        val totalFiles = selectedImages.size + selectedVideos.size
        if (totalFiles == 0) {
            onComplete(mediaUrls)
            return
        }

        var completedUploads = 0

        fun checkCompletion() {
            completedUploads++
            if (completedUploads == totalFiles) {
                onComplete(mediaUrls)
            }
        }

        // Upload images
        selectedImages.forEach { imageUri ->
            val imageRef = storage.reference.child("properties/$propertyId/images/${UUID.randomUUID()}")
            imageRef.putFile(imageUri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    imageRef.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    mediaUrls.images.add(downloadUri.toString())
                    checkCompletion()
                }
                .addOnFailureListener {
                    checkCompletion()
                }
        }

        // Upload videos
        selectedVideos.forEach { videoUri ->
            val videoRef = storage.reference.child("properties/$propertyId/videos/${UUID.randomUUID()}")
            videoRef.putFile(videoUri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    videoRef.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    mediaUrls.videos.add(downloadUri.toString())
                    checkCompletion()
                }
                .addOnFailureListener {
                    checkCompletion()
                }
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
            .setPositiveButton(R.string.ok) { _, _ ->
                finish()
            }
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
        locationPermissionRequest.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    private fun getCurrentLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        currentLocation = LatLng(it.latitude, it.longitude)
                        binding.tvLocationStatus.text = "Location selected: ${it.latitude}, ${it.longitude}"
                    } ?: showErrorDialog("Could not get current location. Please try again or pick location from map.")
                }
                .addOnFailureListener { e ->
                    showErrorDialog("Error getting location: ${e.message}")
                }
        } catch (e: SecurityException) {
            showErrorDialog("Location permission not granted")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MAP_PICKER_REQUEST && resultCode == RESULT_OK) {
            data?.let { intent ->
                val latitude = intent.getDoubleExtra("latitude", 0.0)
                val longitude = intent.getDoubleExtra("longitude", 0.0)
                currentLocation = LatLng(latitude, longitude)
                binding.tvLocationStatus.text = "Location selected: $latitude, $longitude"
            }
        }
    }

    companion object {
        private const val MAP_PICKER_REQUEST = 100
    }

    data class MediaUrls(
        val images: MutableList<String>,
        val videos: MutableList<String>
    )
}
