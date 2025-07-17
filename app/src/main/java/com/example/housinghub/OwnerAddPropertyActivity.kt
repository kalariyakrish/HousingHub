package com.example.housinghub

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.housinghub.databinding.ActivityOwnerAddPropertyBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class OwnerAddPropertyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerAddPropertyBinding
    private val firestore = FirebaseFirestore.getInstance()

    private val selectedImageUris = mutableListOf<Uri>()
    private var agreementUri: Uri? = null

    companion object {
        private const val PICK_IMAGES_CODE = 1001
        private const val PICK_AGREEMENT_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerAddPropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initCloudinary()

        binding.btnUploadImages.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_CODE)
        }

        binding.btnUploadAgreement.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
            }
            startActivityForResult(Intent.createChooser(intent, "Select Rent Agreement"), PICK_AGREEMENT_CODE)
        }

        binding.btnSubmit.setOnClickListener {
            if (validateInputs()) {
                uploadImagesToCloudinary()
            }
        }
    }

    private fun initCloudinary() {
        try {
            MediaManager.get()
        } catch (e: Exception) {
            val config = hashMapOf(
                "cloud_name" to "dp82wqtwj", // ✅ Replace with your actual Cloudinary cloud name
                "upload_preset" to "HousingHub", // ✅ Replace with your unsigned preset name
                "timeout" to 60000,
                "connect_timeout" to 60000,
                "read_timeout" to 60000
            )
            MediaManager.init(this, config)
        }
    }

    private fun validateInputs(): Boolean {
        return when {
            binding.etTitle.text.isNullOrBlank() -> {
                showToast("Enter Title"); false
            }
            binding.etLocation.text.isNullOrBlank() -> {
                showToast("Enter Location"); false
            }
            binding.etPrice.text.isNullOrBlank() -> {
                showToast("Enter Price"); false
            }
            binding.etDescription.text.isNullOrBlank() -> {
                showToast("Enter Description"); false
            }
            selectedImageUris.isEmpty() -> {
                showToast("Select at least one image"); false
            }
            agreementUri == null -> {
                showToast("Upload rent agreement"); false
            }
            else -> true
        }
    }

    private fun uploadImagesToCloudinary() {
        val imageUrls = mutableListOf<String>()
        var uploadedCount = 0

        selectedImageUris.forEach { uri ->
            val requestId = UUID.randomUUID().toString()
            MediaManager.get().upload(uri)
                .option("public_id", "property_images/$requestId")
                .unsigned("HousingHub") // ✅ Must match unsigned preset
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {}
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        val url = resultData?.get("secure_url") as? String
                        url?.let { imageUrls.add(it) }
                        uploadedCount++
                        if (uploadedCount == selectedImageUris.size) {
                            uploadAgreementToCloudinary(imageUrls)
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        Log.e("Cloudinary", "Upload Error: ${error?.description}")
                        showToast("Image upload failed: ${error?.description}")
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                }).dispatch()
        }
    }

    private fun uploadAgreementToCloudinary(imageUrls: List<String>) {
        val agreementId = UUID.randomUUID().toString()
        MediaManager.get().upload(agreementUri)
            .option("public_id", "rent_agreements/$agreementId")
            .unsigned("HousingHub") // ✅ Required for unsigned preset
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val agreementUrl = resultData?.get("secure_url") as? String
                    savePropertyToFirestore(imageUrls, agreementUrl)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.e("Cloudinary", "Agreement Upload Error: ${error?.description}")
                    showToast("Agreement upload failed: ${error?.description}")
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
    }

    private fun savePropertyToFirestore(images: List<String>, agreementUrl: String?) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser ?: return

        val propertyId = UUID.randomUUID().toString()
        val property = hashMapOf(
            "id" to propertyId,
            "title" to binding.etTitle.text.toString(),
            "location" to binding.etLocation.text.toString(),
            ("price" to binding.etPrice.text.toString().toIntOrNull() ?: 0) as Pair<Any, Any>,
            "images" to images,
            "ownerId" to currentUser.email,
            "isAvailable" to true,
            "agreement" to (agreementUrl ?: ""),
            "createdAt" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("Properties") // Using capital P as requested
            .document(propertyId)
            .set(property)
            .addOnSuccessListener {
                showToast("Property added successfully")
                finish()
            }
            .addOnFailureListener { e ->
                showToast("Error: ${e.message}")
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGES_CODE -> {
                    selectedImageUris.clear()
                    data?.clipData?.let { clip ->
                        for (i in 0 until clip.itemCount) {
                            selectedImageUris.add(clip.getItemAt(i).uri)
                        }
                    } ?: data?.data?.let { selectedImageUris.add(it) }

                    binding.imagePreviewRecycler.layoutManager =
                        LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                    binding.imagePreviewRecycler.adapter = ImagePreviewAdapter(selectedImageUris)

                    showToast("${selectedImageUris.size} image(s) selected")
                }

                PICK_AGREEMENT_CODE -> {
                    agreementUri = data?.data
                    showToast("Agreement file selected")
                }
            }
        }
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
