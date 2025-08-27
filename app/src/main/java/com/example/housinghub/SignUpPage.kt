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
    private lateinit var mobileNumberInput: EditText

    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_page)

        auth = FirebaseAuth.getInstance()
        mobileNumberInput = findViewById(R.id.mobileNumberInput)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize Google Sign-In launcher
        signupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Toast.makeText(this, getString(R.string.google_sign_in_failed, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Initialize views
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

        // Tab navigation
        signupTab.setOnClickListener {
            signupTab.setTextColor(getColor(R.color.black))
            loginTab.setTextColor(getColor(android.R.color.darker_gray))
            updateTabUnderline(signupTab, tabUnderline)
        }

        loginTab.setOnClickListener {
            startActivity(Intent(this, LoginPage::class.java))
            finish()
        }

        // Role selection
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

        // Email/Password Sign-Up
        createAccountButton.setOnClickListener {
            val fullName = fullNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()
            val mobile = mobileNumberInput.text.toString().trim()

            // Input validation
            if (fullName.isEmpty()) {
                fullNameInput.error = getString(R.string.full_name_required)
                return@setOnClickListener
            }
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = getString(R.string.invalid_email)
                return@setOnClickListener
            }
            if (password.isEmpty() || password.length < 6) {
                passwordInput.error = getString(R.string.password_too_short)
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                confirmPasswordInput.error = getString(R.string.passwords_do_not_match)
                return@setOnClickListener
            }
            if (mobile.isEmpty() || mobile.length < 10) {
                mobileNumberInput.error = getString(R.string.invalid_mobile_number)
                return@setOnClickListener
            }

            // Create user with email and password
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId == null) {
                            Toast.makeText(this, getString(R.string.failed_to_get_user_id), Toast.LENGTH_SHORT).show()
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

                        val db = Firebase.firestore
                        val collection = if (selectedRole == "owner") "Owners" else "Tenants"

                        // Save user data
                        db.collection(collection)
                            .document(email)
                            .set(userMap, SetOptions.merge())
                            .addOnSuccessListener {
                                // Save to session
                                val sessionManager = UserSessionManager(this)
                                sessionManager.saveUserData(fullName, email, mobile)

                                // Handle owner-specific Firestore setup
                                if (selectedRole == "owner") {
                                    setupOwnerProperties(email)
                                } else {
                                    navigateToRoleHome(selectedRole)
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, getString(R.string.failed_to_create_user, e.message), Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, getString(R.string.sign_up_failed, task.exception?.message), Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Google Sign-In
        googleSignInButton.setOnClickListener {
            if (selectedRole.isEmpty()) {
                Toast.makeText(this, getString(R.string.select_role), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val signInIntent = googleSignInClient.signInIntent
            signupLauncher.launch(signInIntent)
        }
    }

    private fun setupOwnerProperties(email: String) {
        val db = Firebase.firestore
        db.collection("Properties")
            .document(email)
            .set(mapOf("createdAt" to com.google.firebase.Timestamp.now()))
            .addOnSuccessListener {
                val dummy = mapOf("dummy" to true)
                // Create Available collection
                db.collection("Properties").document(email)
                    .collection("Documents")
                    .document("init")
                    .set(dummy)
                    .addOnSuccessListener {
                        // Create property collection
                        db.collection("Properties").document(email)
                            .collection("documents")
                            .document("init")
                            .set(dummy)
                            .addOnSuccessListener {
                                navigateToRoleHome("owner")
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, getString(R.string.failed_to_save_user_data, e.message), Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, getString(R.string.failed_to_save_user_data, e.message), Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.failed_to_save_user_data, e.message), Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToRoleHome(role: String) {
        val intent = if (role.equals("owner", ignoreCase = true)) {
            Intent(this, OwnerHomePageActivity::class.java)
        } else {
            Intent(this, HomePageActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun updateTabUnderline(activeTab: Button, tabUnderline: View) {
        activeTab.post {
            val params = tabUnderline.layoutParams
            params.width = activeTab.width
            tabUnderline.layoutParams = params
            tabUnderline.animate()
                .translationX(activeTab.x)
                .setDuration(200)
                .start()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        if (idToken == null) {
            Toast.makeText(this, getString(R.string.google_sign_in_failed, "Invalid token"), Toast.LENGTH_SHORT).show()
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user == null) {
                        Toast.makeText(this, getString(R.string.failed_to_get_user_id), Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    val userEmail = user.email ?: return@addOnCompleteListener
                    val userMap = mapOf(
                        "fullName" to (user.displayName ?: ""),
                        "email" to userEmail,
                        "mobileNumber" to (user.phoneNumber ?: ""),
                        "role" to selectedRole,
                        "documentUploaded" to false,
                        "createdAt" to System.currentTimeMillis()
                        )

                    val collection = if (selectedRole == "owner") "Owners" else "Tenants"

                    // Save user data
                    Firebase.firestore
                        .collection(collection)
                        .document(userEmail)
                        .set(userMap, SetOptions.merge())
                        .addOnSuccessListener {

                            data class HousingItem(
                                // ... other fields
                                val price: String, // Incorrect type
                            )

                            // BEFORE
                            data class Property(
                                val address: String? = null,
                                val price: String? = null, // Problem: Expecting String
                                val bedrooms: Int? = null,
                            )
                            val sessionManager = UserSessionManager(this)
                            sessionManager.saveUserData(user.displayName ?: "", userEmail, user.phoneNumber ?: "")

                            // Handle owner-specific setup
                            if (selectedRole == "owner") {
                                setupOwnerProperties(userEmail)
                            } else {
                                navigateToRoleHome(selectedRole)
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, getString(R.string.failed_to_save_user_data, e.message), Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, getString(R.string.google_sign_in_failed, task.exception?.message), Toast.LENGTH_LONG).show()
                }
            }
    }
}