package com.example.housinghub.owner

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.housinghub.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var saveButton: Button

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        nameEditText = findViewById(R.id.editName)
        phoneEditText = findViewById(R.id.editPhone)
        saveButton = findViewById(R.id.saveButton)

        loadUserData()

        saveButton.setOnClickListener {
            val fullName = nameEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()

            if (fullName.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phone.isEmpty() || phone.length != 10 || !phone.all { it.isDigit() }) {
                Toast.makeText(this, "Please enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            val userMap = mapOf(
                "fullName" to fullName,
                "mobileNumber" to phone
            )

            db.collection("users").document(userId)
                .update(userMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    nameEditText.setText(document.getString("fullName") ?: "")
                    phoneEditText.setText(document.getString("mobileNumber") ?: "")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
