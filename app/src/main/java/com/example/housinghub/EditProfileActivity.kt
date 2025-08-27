package com.example.housinghub

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.housinghub.utils.UserSessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditProfileActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilAddress: TextInputLayout
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userSessionManager: UserSessionManager
    
    private var hasChanges = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        
        initializeViews()
        setupToolbar()
        initializeFirebase()
        loadUserData()
        setupTextWatchers()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        tilName = findViewById(R.id.tilName)
        tilEmail = findViewById(R.id.tilEmail)
        tilPhone = findViewById(R.id.tilPhone)
        tilAddress = findViewById(R.id.tilAddress)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Edit Profile"
        }
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        userSessionManager = UserSessionManager(this)
    }
    
    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        val userType = userSessionManager.getUserType()
        val collection = if (userType == "tenant") "tenants" else "owners"
        
        Log.d("EditProfile", "Loading user data for $userType with ID: $userId")
        
        firestore.collection(collection)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d("EditProfile", "User data loaded successfully")
                    val name = document.getString("name") ?: document.getString("fullName") ?: ""
                    val email = document.getString("email") ?: ""
                    val phone = document.getString("phone") ?: document.getString("mobileNumber") ?: ""
                    val address = document.getString("address") ?: ""
                    
                    etName.setText(name)
                    etEmail.setText(email)
                    etPhone.setText(phone)
                    etAddress.setText(address)
                } else {
                    Log.w("EditProfile", "User document does not exist")
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("EditProfile", "Error loading user data", e)
                Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                hasChanges = true
                updateSaveButton()
            }
        }
        
        etName.addTextChangedListener(textWatcher)
        etEmail.addTextChangedListener(textWatcher)
        etPhone.addTextChangedListener(textWatcher)
        etAddress.addTextChangedListener(textWatcher)
    }
    
    private fun updateSaveButton() {
        btnSave.isEnabled = hasChanges && validateFields()
    }
    
    private fun validateFields(): Boolean {
        var isValid = true
        
        // Clear previous errors
        tilName.error = null
        tilEmail.error = null
        tilPhone.error = null
        tilAddress.error = null
        
        // Validate name
        val name = etName.text.toString().trim()
        if (name.isEmpty()) {
            tilName.error = "Name is required"
            isValid = false
        } else if (name.length < 2) {
            tilName.error = "Name must be at least 2 characters"
            isValid = false
        }
        
        // Validate email
        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Please enter a valid email address"
            isValid = false
        }
        
        // Validate phone
        val phone = etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            tilPhone.error = "Phone number is required"
            isValid = false
        } else if (phone.length < 10) {
            tilPhone.error = "Please enter a valid phone number"
            isValid = false
        }
        
        // Address is optional but validate if provided
        val address = etAddress.text.toString().trim()
        if (address.isNotEmpty() && address.length < 5) {
            tilAddress.error = "Please enter a complete address"
            isValid = false
        }
        
        return isValid
    }
    
    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            if (validateFields()) {
                saveProfile()
            }
        }
        
        btnCancel.setOnClickListener {
            onBackPressed()
        }
    }
    
    private fun saveProfile() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userType = userSessionManager.getUserType()
        val collection = if (userType == "tenant") "tenants" else "owners"
        
        val updatedData = mapOf(
            "name" to etName.text.toString().trim(),
            "fullName" to etName.text.toString().trim(), // For backward compatibility
            "email" to etEmail.text.toString().trim(),
            "phone" to etPhone.text.toString().trim(),
            "mobileNumber" to etPhone.text.toString().trim(), // For backward compatibility
            "address" to etAddress.text.toString().trim(),
            "updatedAt" to System.currentTimeMillis()
        )
        
        Log.d("EditProfile", "Saving profile data for $userType with ID: $userId")
        
        btnSave.isEnabled = false
        btnSave.text = "Saving..."
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                firestore.collection(collection)
                    .document(userId)
                    .update(updatedData)
                    .await()
                
                Log.d("EditProfile", "Profile updated successfully")

                // Update UserSessionManager with new data
                userSessionManager.saveUserName(updatedData["name"] as String)

                Toast.makeText(this@EditProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                
                // Set result and finish
                setResult(Activity.RESULT_OK)
                finish()
                
            } catch (e: Exception) {
                Log.e("EditProfile", "Error updating profile", e)
                Toast.makeText(this@EditProfileActivity, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                
                btnSave.isEnabled = true
                btnSave.text = "Save Changes"
            }
        }
    }
    
    override fun onBackPressed() {
        if (hasChanges) {
            // Show confirmation dialog
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Discard Changes?")
                .setMessage("You have unsaved changes. Are you sure you want to discard them?")
                .setPositiveButton("Discard") { _, _ ->
                    super.onBackPressed()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}
