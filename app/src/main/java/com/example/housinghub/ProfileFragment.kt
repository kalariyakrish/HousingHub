// ProfileFragment.kt
package com.example.housinghub.ui.profile

import android.app.AlertDialog
import android.content.Context
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

class ProfileFragment : Fragment() {

    private lateinit var profileInitials: TextView
    private lateinit var profileName: TextView
    private lateinit var profileDetails: TextView
    private lateinit var uploadButton: Button
    private lateinit var agreementButton: Button
    private lateinit var editButton: Button
    private lateinit var logoutButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        profileInitials = view.findViewById(R.id.profileImageInitials)
        profileName = view.findViewById(R.id.profileName)
        profileDetails = view.findViewById(R.id.profileDetails)
        uploadButton = view.findViewById(R.id.uploadDocumentsBtn)
        agreementButton = view.findViewById(R.id.generateAgreementBtn)
        editButton = view.findViewById(R.id.editProfileBtn)
        logoutButton = view.findViewById(R.id.logoutBtn)

        setUserData()

        uploadButton.setOnClickListener {
            Toast.makeText(requireContext(), "Upload Documents clicked", Toast.LENGTH_SHORT).show()
        }

        agreementButton.setOnClickListener {
            Toast.makeText(requireContext(), "Generate Agreement clicked", Toast.LENGTH_SHORT).show()
        }

        editButton.setOnClickListener {
            Toast.makeText(requireContext(), "Edit Profile clicked", Toast.LENGTH_SHORT).show()
        }

        logoutButton.setOnClickListener {
            confirmLogout()
        }

        return view
    }

    private fun setUserData() {
        val sessionManager = UserSessionManager(requireContext())
        val fullName = sessionManager.getFullName()
        val email = sessionManager.getEmail()
        val mobile = sessionManager.getPhone()

        profileName.text = fullName
        profileDetails.text = "$email\n+91 $mobile"

        profileInitials.text = fullName
            .split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE).edit().clear().apply()
                Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(), LoginPage::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}