package com.example.housinghub

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.housinghub.OwnerHomePageActivity
import com.example.housinghub.utils.UserSessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class SignUpPage : AppCompatActivity() {

    private lateinit var signupLauncher: ActivityResultLauncher<Intent>
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private var selectedRole = "tenant" // default role
    private lateinit var mobileNumberInput: EditText // Add mobile number field

    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_page)

        auth = FirebaseAuth.getInstance()
        mobileNumberInput = findViewById(R.id.mobileNumberInput) // Initialize mobile number field

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        signupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Toast.makeText(this, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val tenantButton: Button = findViewById(R.id.tenantButton)
        val ownerButton: Button = findViewById(R.id.ownerButton)
        val loginTab: Button = findViewById(R.id.loginTab)
        val signupTab: Button = findViewById(R.id.signupTab)
        val tabUnderline: View = findViewById(R.id.tabUnderline)
        val fullNameInput: EditText = findViewById(R.id.fullNameInput)
        val emailInput: EditText = findViewById(R.id.emailInput)
        val passwordInput: EditText = findViewById(R.id.passwordInput)
        val confirmPasswordInput: EditText = findViewById(R.id.confirmPasswordInput)
        val createAccountButton: Button = findViewById(R.id.createAccountButton)
        val googleSignInButton: Button = findViewById(R.id.googleSignInButton)

        signupTab.setOnClickListener {
            signupTab.setTextColor(getColor(R.color.black))
            loginTab.setTextColor(getColor(android.R.color.darker_gray))
            updateTabUnderline(signupTab, tabUnderline)
        }

        loginTab.setOnClickListener {
            startActivity(Intent(this, LoginPage::class.java))
            finish()
        }

        tenantButton.setOnClickListener {
            selectedRole = "tenant"
            tenantButton.backgroundTintList = getColorStateList(R.color.tenant_selected)
            ownerButton.backgroundTintList = getColorStateList(android.R.color.darker_gray)
        }

        ownerButton.setOnClickListener {
            selectedRole = "owner"
            ownerButton.backgroundTintList = getColorStateList(R.color.tenant_selected)
            tenantButton.backgroundTintList = getColorStateList(android.R.color.darker_gray)
        }

        createAccountButton.setOnClickListener {
            val fullName = fullNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()
            val mobile = mobileNumberInput.text.toString().trim() // Get mobile number

            if (fullName.isEmpty()) {
                fullNameInput.error = "Full Name is required"
                return@setOnClickListener
            }
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Enter valid email"
                return@setOnClickListener
            }
            if (password.isEmpty() || password.length < 6) {
                passwordInput.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                confirmPasswordInput.error = "Passwords do not match"
                return@setOnClickListener
            }
            if (mobile.isEmpty() || mobile.length < 10) {
                mobileNumberInput.error = "Enter valid mobile number"
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId == null) {
                            Toast.makeText(this, "Failed to get user ID", Toast.LENGTH_SHORT).show()
                            return@addOnCompleteListener
                        }

                        val userMap = mapOf(
                            "fullName" to fullName,
                            "email" to email,
                            "mobileNumber" to mobile,
                            "role" to selectedRole,
                            "documentUploaded" to false,
                            "createdAt" to System.currentTimeMillis()
                        )

                        // Use different collection based on role
                        val collectionName = if (selectedRole == "owner") "Owners" else "Tenants"
                        
                        // Use email as document ID
                        Firebase.firestore.collection(collectionName)
                            .document(email)
                            .set(userMap)
                            .addOnSuccessListener {
                                // Save to session
                                val sessionManager = UserSessionManager(this)
                                sessionManager.saveUserData(fullName, email, mobile)
                                navigateToRoleHome(selectedRole)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        googleSignInButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            signupLauncher.launch(signInIntent)
        }
    }

    private fun navigateToRoleHome(role: String) {
        if (role.lowercase() == "owner") {
            startActivity(Intent(this, OwnerHomePageActivity ::class.java))
        } else {
            startActivity(Intent(this, HomePageActivity::class.java))
        }
        finish()
    }

    private fun updateTabUnderline(activeTab: Button, underline: View) {
        activeTab.post {
            val params = underline.layoutParams
            params.width = activeTab.width
            underline.layoutParams = params
            underline.translationX = activeTab.x + (activeTab.width - underline.width)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userEmail = auth.currentUser?.email ?: return@addOnCompleteListener

                    val userMap = mapOf(
                        "role" to selectedRole,
                        "email" to userEmail,
                        "fullName" to (auth.currentUser?.displayName ?: ""),
                        "mobileNumber" to "", // Initialize empty mobile number for Google sign-in
                        "documentUploaded" to false,
                        "createdAt" to System.currentTimeMillis()
                    )

                    // Use different collection based on role
                    val collectionName = if (selectedRole == "owner") "Owners" else "Tenants"

                    // Use email as document ID
                    Firebase.firestore
                        .collection(collectionName)
                        .document(userEmail)
                        .set(userMap, SetOptions.merge())
                        .addOnSuccessListener {
                            // Save to session
                            val sessionManager = UserSessionManager(this)
                            sessionManager.saveUserData(
                                auth.currentUser?.displayName ?: "",
                                userEmail,
                                ""  // Mobile number will be empty initially for Google sign-in
                            )
                            navigateToRoleHome(selectedRole)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Google Sign-In Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
