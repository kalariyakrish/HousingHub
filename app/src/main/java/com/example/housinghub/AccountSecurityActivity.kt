package com.example.housinghub.owner

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.housinghub.R
import com.google.firebase.auth.FirebaseAuth

class AccountSecurityActivity : AppCompatActivity() {

    private lateinit var changePasswordField: EditText
    private lateinit var updatePasswordButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_security)

        changePasswordField = findViewById(R.id.newPassword)
        updatePasswordButton = findViewById(R.id.updatePasswordButton)
        auth = FirebaseAuth.getInstance()

        updatePasswordButton.setOnClickListener {
            val newPassword = changePasswordField.text.toString().trim()

            if (newPassword.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.currentUser?.updatePassword(newPassword)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
