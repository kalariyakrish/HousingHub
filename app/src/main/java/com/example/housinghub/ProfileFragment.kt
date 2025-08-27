// ProfileFragment.kt
package com.example.housinghub

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.housinghub.utils.UserSessionManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileFragment : Fragment() {

    private lateinit var userSessionManager: UserSessionManager
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Profile header views
    private lateinit var profileImageInitials: TextView
    private lateinit var profileName: TextView
    private lateinit var profileDetails: TextView
    private lateinit var tvMemberSince: TextView

    // Stats views
    private lateinit var tvBookingsCount: TextView
    private lateinit var tvSavedCount: TextView
    private lateinit var tvChatsCount: TextView

    // Menu items
    private lateinit var layoutEditProfile: LinearLayout
    private lateinit var layoutMyBookings: LinearLayout
    private lateinit var layoutDocuments: LinearLayout
    private lateinit var layoutSettings: LinearLayout
    private lateinit var logoutBtn: MaterialButton

    // Backward compatibility views
    private lateinit var uploadButton: Button
    private lateinit var agreementButton: Button
    private lateinit var editButton: Button
    private lateinit var logoutButton: Button

    // Activity result launcher for edit profile
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Profile was updated, refresh the data
            Log.d("ProfileFragment", "Profile updated, refreshing data")
            loadUserData()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        initializeFirebase()
        loadUserData()
        loadUserStats()
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        // Profile header
        profileImageInitials = view.findViewById(R.id.profileImageInitials)
        profileName = view.findViewById(R.id.profileName)
        profileDetails = view.findViewById(R.id.profileDetails)
        
        // Try to find new UI elements (may not exist in old layout)
        try {
            tvMemberSince = view.findViewById(R.id.tvMemberSince)
            tvBookingsCount = view.findViewById(R.id.tvBookingsCount)
            tvSavedCount = view.findViewById(R.id.tvSavedCount)
            tvChatsCount = view.findViewById(R.id.tvChatsCount)
            layoutEditProfile = view.findViewById(R.id.layoutEditProfile)
            layoutSettings = view.findViewById(R.id.layoutSettings)
            logoutBtn = view.findViewById(R.id.logoutBtn)
        } catch (e: Exception) {
            Log.w("ProfileFragment", "New UI elements not found, using backward compatibility")
        }

        // Backward compatibility - find old button elements
        try {
            uploadButton = view.findViewById(R.id.uploadDocumentsBtn)
            agreementButton = view.findViewById(R.id.generateAgreementBtn)
            editButton = view.findViewById(R.id.editProfileBtn)
            logoutButton = view.findViewById(R.id.logoutBtn)
        } catch (e: Exception) {
            Log.w("ProfileFragment", "Old UI elements not found")
        }
    }

    private fun initializeFirebase() {
        userSessionManager = UserSessionManager(requireContext())
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("ProfileFragment", "User not authenticated")
            setUserDataFromSession()
            return
        }

        val userType = userSessionManager.getUserType()
        val collection = if (userType == "tenant") "tenants" else "owners"

        Log.d("ProfileFragment", "Loading user data for $userType with ID: $userId")

        firestore.collection(collection)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d("ProfileFragment", "User data loaded successfully")
                    val name = document.getString("name") ?: document.getString("fullName") ?: "User"
                    val email = document.getString("email") ?: ""
                    val phone = document.getString("phone") ?: document.getString("mobileNumber") ?: ""
                    val createdAt = document.getLong("createdAt")

                    setUserData(name, email, phone, createdAt)
                } else {
                    Log.w("ProfileFragment", "User document does not exist, using session data")
                    setUserDataFromSession()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error loading user data", e)
                setUserDataFromSession()
            }
    }

    private fun setUserDataFromSession() {
        val fullName = userSessionManager.getFullName()
        val email = userSessionManager.getEmail()
        val mobile = userSessionManager.getPhone()
        setUserData(fullName, email, mobile, null)
    }

    private fun setUserData(name: String, email: String, phone: String, createdAt: Long?) {
        // Set initials
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString()?.uppercase() }.take(2).joinToString("")
        profileImageInitials.text = initials.ifEmpty { "U" }

        // Set name
        profileName.text = name

        // Set details
        val details = buildString {
            if (email.isNotEmpty()) appendLine(email)
            if (phone.isNotEmpty()) {
                if (phone.startsWith("+91")) {
                    append(phone)
                } else {
                    append("+91 $phone")
                }
            }
        }
        profileDetails.text = details

        // Set member since (if view exists)
        try {
            createdAt?.let { timestamp ->
                val monthYear = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(timestamp))
                tvMemberSince.text = "Member since $monthYear"
            } ?: run {
                tvMemberSince.text = "Welcome to HousingHub"
            }
        } catch (e: Exception) {
            // View doesn't exist in old layout
        }
    }

    private fun loadUserStats() {
        val userId = auth.currentUser?.uid ?: return
        val userType = userSessionManager.getUserType()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Only load stats if views exist
                if (::tvBookingsCount.isInitialized) {
                    // Load bookings count
                    val bookingsCount = if (userType == "tenant") {
                        firestore.collection("bookings")
                            .whereEqualTo("tenantId", userId)
                            .get()
                            .await()
                            .size()
                    } else {
                        firestore.collection("bookings")
                            .whereEqualTo("ownerId", userId)
                            .get()
                            .await()
                            .size()
                    }
                    tvBookingsCount.text = bookingsCount.toString()

                    // Load chats count
                    val chatsCount = firestore.collection("chats")
                        .whereEqualTo("${userType}Id", userId)
                        .get()
                        .await()
                        .size()
                    tvChatsCount.text = chatsCount.toString()

                    // Load saved/favorites count (for tenants)
                    if (userType == "tenant") {
                        val savedCount = firestore.collection("favorites")
                            .whereEqualTo("tenantId", userId)
                            .get()
                            .await()
                            .size()
                        tvSavedCount.text = savedCount.toString()
                    } else {
                        // For owners, show properties count
                        val propertiesCount = firestore.collection("properties")
                            .whereEqualTo("ownerId", userId)
                            .get()
                            .await()
                            .size()
                        tvSavedCount.text = propertiesCount.toString()
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error loading user stats", e)
            }
        }
    }

    private fun setupClickListeners() {
        // New UI click listeners
        try {
            layoutEditProfile.setOnClickListener {
                Log.d("ProfileFragment", "Edit Profile clicked")
                val intent = Intent(requireContext(), EditProfileActivity::class.java)
                editProfileLauncher.launch(intent)
            }

            layoutMyBookings.setOnClickListener {
                Toast.makeText(requireContext(), "My Bookings - Coming soon", Toast.LENGTH_SHORT).show()
            }

            layoutDocuments.setOnClickListener {
                Toast.makeText(requireContext(), "Documents - Coming soon", Toast.LENGTH_SHORT).show()
            }

            layoutSettings.setOnClickListener {
                Toast.makeText(requireContext(), "Settings - Coming soon", Toast.LENGTH_SHORT).show()
            }

            logoutBtn.setOnClickListener {
                confirmLogout()
            }
        } catch (e: Exception) {
            Log.w("ProfileFragment", "New UI elements not available, using old buttons")
        }

        // Backward compatibility - old UI click listeners
        try {
            uploadButton.setOnClickListener {
                Toast.makeText(requireContext(), "Upload Documents clicked", Toast.LENGTH_SHORT).show()
            }

            agreementButton.setOnClickListener {
                Toast.makeText(requireContext(), "Generate Agreement clicked", Toast.LENGTH_SHORT).show()
            }

            editButton.setOnClickListener {
                Log.d("ProfileFragment", "Edit Profile (old) clicked")
                val intent = Intent(requireContext(), EditProfileActivity::class.java)
                editProfileLauncher.launch(intent)
            }

            logoutButton.setOnClickListener {
                confirmLogout()
            }
        } catch (e: Exception) {
            Log.w("ProfileFragment", "Old UI elements not available")
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        Log.d("ProfileFragment", "Performing logout")
        
        // Clear user session
        userSessionManager.clearSession()
        
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut()
        
        // Clear shared preferences
        requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE).edit().clear().apply()
        
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        
        // Navigate to LoginPage
        val intent = Intent(requireContext(), LoginPage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        loadUserData()
        loadUserStats()
    }
}