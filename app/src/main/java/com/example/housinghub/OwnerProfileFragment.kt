package com.example.housinghub.owner

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.housinghub.LoginPage
import com.example.housinghub.R
import com.example.housinghub.utils.UserSessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OwnerProfileFragment : Fragment() {

    private lateinit var profileInitials: TextView
    private lateinit var profileName: TextView
    private lateinit var profileDetails: TextView
    private lateinit var editProfileBtn: Button
    private lateinit var logoutBtn: Button

    private lateinit var userSession: UserSessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_owner_profile, container, false)

        try {
            // Initialize views
            profileInitials = view.findViewById(R.id.profileInitials)
            profileName = view.findViewById(R.id.profileName)
            profileDetails = view.findViewById(R.id.profileDetails)
            editProfileBtn = view.findViewById(R.id.editProfileBtn)
            logoutBtn = view.findViewById(R.id.logoutBtn)

            userSession = UserSessionManager(requireContext())

            // Set default values to prevent null display
            profileName.text = "Loading..."
            profileDetails.text = "Loading..."
            profileInitials.text = "..."

            loadOwnerProfile()

            editProfileBtn.setOnClickListener {
                startActivity(Intent(requireContext(), EditProfileActivity::class.java))
            }

            logoutBtn.setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                userSession.clearUserSession()
                startActivity(Intent(requireContext(), LoginPage::class.java))
                requireActivity().finish()
            }

            // Set menu option click listeners
            setMenuClickListeners(view)

        } catch (e: Exception) {
            Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun loadOwnerProfile() {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.email
            if (userId == null) {
                Toast.makeText(context, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
                return
            }

            FirebaseFirestore.getInstance().collection("Owners")  // Changed from "users" to "Owners"
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val fullName = document.getString("fullName") ?: "Unknown"
                        val email = document.getString("email") ?: ""
                        val mobile = document.getString("mobileNumber") ?: ""

                        profileName.text = fullName
                        profileDetails.text = "$email\n$mobile"  // Removed hard-coded +91
                        profileInitials.text = getInitials(fullName)
                    } else {
                        Toast.makeText(context, "Profile data not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getInitials(name: String): String {
        return try {
            name.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .take(2)
                .joinToString("")
        } catch (e: Exception) {
            "?"
        }
    }

    private fun setMenuClickListeners(view: View) {
        try {
            val optionMap = mapOf(
                R.id.optionAccountSecurity to "Account Security",
                R.id.optionNotifications to "Notifications",
                R.id.optionPrivacy to "Privacy"
            )

            for ((id, title) in optionMap) {
                view.findViewById<TextView>(id)?.setOnClickListener {
                    Toast.makeText(context, "$title clicked", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error setting up menu: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
