package com.example.housinghub

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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class LoginPage : AppCompatActivity() {

    private lateinit var loginLauncher: ActivityResultLauncher<Intent>
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        auth = FirebaseAuth.getInstance()

        fun navigateToRoleHome(role: String) {
            if (role == "owner") {
                startActivity(Intent(this, OwnerHomePageActivity::class.java))  // ✅ Fixed class name
            } else {
                startActivity(Intent(this, HomePageActivity::class.java))
            }
            finish()
        }

        fun updateTabUnderline(activeTab: Button, underline: View) {
            activeTab.post {
                val params = underline.layoutParams
                params.width = activeTab.width
                underline.layoutParams = params
                underline.translationX = activeTab.x + (activeTab.width - underline.width)
            }
        }

        fun firebaseAuthWithGoogle(idToken: String) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId == null) {
                            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
                            return@addOnCompleteListener
                        }
                        Firebase.firestore.collection("users").document(userId)
                            .get()
                            .addOnSuccessListener { document ->
                                val role = document.getString("role")?.lowercase()
                                val fullName = document.getString("fullName") ?: "User"
                                val emailFromDb = document.getString("email") ?: "email"
                                val phone = document.getString("phone") ?: "N/A"

                                // ✅ Save user session
                                val sessionManager = UserSessionManager(this)
                                sessionManager.saveUserData(fullName, emailFromDb, phone)

                                if (role != null) {
                                    navigateToRoleHome(role)
                                } else {
                                    Toast.makeText(this, "Role not found", Toast.LENGTH_SHORT).show()
                                }
                            }

                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to fetch user role", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Google Sign-In Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
        val emailInput: EditText = findViewById(R.id.emailInput)
        val passwordInput: EditText = findViewById(R.id.passwordInput)
        val loginButton: Button = findViewById(R.id.loginButton)
        val googleSignInButton: Button = findViewById(R.id.googleSignInButton)

        loginTab.setOnClickListener {
            loginTab.setTextColor(getColor(R.color.black))
            signupTab.setTextColor(getColor(android.R.color.darker_gray))
            updateTabUnderline(loginTab, tabUnderline)
        }

        signupTab.setOnClickListener {
            startActivity(Intent(this, SignUpPage::class.java))
            finish()
        }

        // 🔵 UI only: highlight tenant selection
        tenantButton.setOnClickListener {
            tenantButton.backgroundTintList = getColorStateList(R.color.tenant_selected)
            ownerButton.backgroundTintList = getColorStateList(android.R.color.darker_gray)
            Toast.makeText(this, "UI selected: Tenant (Saved role will be used)", Toast.LENGTH_SHORT).show()
        }

        // 🔵 UI only: highlight owner selection
        ownerButton.setOnClickListener {
            ownerButton.backgroundTintList = getColorStateList(R.color.tenant_selected)
            tenantButton.backgroundTintList = getColorStateList(android.R.color.darker_gray)
            Toast.makeText(this, "UI selected: Owner (Saved role will be used)", Toast.LENGTH_SHORT).show()
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Enter valid email"
                return@setOnClickListener
            }
            if (password.isEmpty() || password.length < 6) {
                passwordInput.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Check both collections to find the user's role
                        val db = Firebase.firestore

                        // First check Owners collection
                        db.collection("Owners")
                            .document(email)
                            .get()
                            .addOnSuccessListener { ownerDoc ->
                                if (ownerDoc.exists()) {
                                    // User is an owner
                                    val sessionManager = UserSessionManager(this)
                                    sessionManager.saveUserData(
                                        ownerDoc.getString("fullName") ?: "",
                                        email,
                                        ownerDoc.getString("mobileNumber") ?: ""
                                    )
                                    navigateToRoleHome("owner")
                                } else {
                                    // If not found in Owners, check Tenants
                                    db.collection("Tenants")
                                        .document(email)
                                        .get()
                                        .addOnSuccessListener { tenantDoc ->
                                            if (tenantDoc.exists()) {
                                                val sessionManager = UserSessionManager(this)
                                                sessionManager.saveUserData(
                                                    tenantDoc.getString("fullName") ?: "",
                                                    email,
                                                    tenantDoc.getString("mobileNumber") ?: ""
                                                )
                                                navigateToRoleHome("tenant")
                                            } else {
                                                Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                                                auth.signOut()
                                            }
                                        }
                                }
                            }
                    } else {
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

        googleSignInButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            loginLauncher.launch(signInIntent)
        }
    }





}
    }