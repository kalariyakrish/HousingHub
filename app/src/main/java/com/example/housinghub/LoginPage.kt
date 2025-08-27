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
    private var selectedRole: String? = null // Track role for Google Sign-In

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize Google Sign-In launcher
        loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
        val emailInput: EditText = findViewById(R.id.emailInput)
        val passwordInput: EditText = findViewById(R.id.passwordInput)
        val loginButton: Button = findViewById(R.id.loginButton)
        val googleSignInButton: Button = findViewById(R.id.googleSignInButton)

        // Tab navigation
        loginTab.setOnClickListener {
            loginTab.setTextColor(getColor(R.color.black))
            signupTab.setTextColor(getColor(android.R.color.darker_gray))
            updateTabUnderline(loginTab, tabUnderline)
        }

        signupTab.setOnClickListener {
            startActivity(Intent(this, SignUpPage::class.java))
            finish()
        }

        // Role selection for UI and Google Sign-In
        tenantButton.setOnClickListener {
            selectedRole = "tenant"
            tenantButton.backgroundTintList = getColorStateList(R.color.tenant_selected)
            ownerButton.backgroundTintList = getColorStateList(android.R.color.darker_gray)
            Toast.makeText(this, getString(R.string.tenant_selected), Toast.LENGTH_SHORT).show()
        }

        ownerButton.setOnClickListener {
            selectedRole = "owner"
            ownerButton.backgroundTintList = getColorStateList(R.color.tenant_selected)
            tenantButton.backgroundTintList = getColorStateList(android.R.color.darker_gray)
            Toast.makeText(this, getString(R.string.owner_selected), Toast.LENGTH_SHORT).show()
        }

        // Email/Password Login
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = getString(R.string.invalid_email)
                return@setOnClickListener
            }
            if (password.isEmpty() || password.length < 6) {
                passwordInput.error = getString(R.string.password_too_short)
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val db = Firebase.firestore

                        // Check Owners collection
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
                                        ownerDoc.getString("mobileNumber") ?: "",
                                        "owner"
                                    )
                                    navigateToRoleHome("owner")
                                } else {
                                    // Check Tenants collection
                                    db.collection("Tenants")
                                        .document(email)
                                        .get()
                                        .addOnSuccessListener { tenantDoc ->
                                            if (tenantDoc.exists()) {
                                                // User is a tenant
                                                val sessionManager = UserSessionManager(this)
                                                sessionManager.saveUserData(
                                                    tenantDoc.getString("fullName") ?: "",
                                                    email,
                                                    tenantDoc.getString("mobileNumber") ?: "",
                                                    "tenant"
                                                )
                                                navigateToRoleHome("tenant")
                                            } else {
                                                Toast.makeText(this, getString(R.string.user_data_not_found), Toast.LENGTH_SHORT).show()
                                                auth.signOut()
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this, getString(R.string.failed_to_fetch_user_role, e.message), Toast.LENGTH_SHORT).show()
                                            auth.signOut()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, getString(R.string.failed_to_fetch_user_role, e.message), Toast.LENGTH_SHORT).show()
                                auth.signOut()
                            }
                    } else {
                        Toast.makeText(this, getString(R.string.login_failed, task.exception?.message), Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Google Sign-In
        googleSignInButton.setOnClickListener {
            if (selectedRole == null) {
                Toast.makeText(this, getString(R.string.select_role), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val signInIntent = googleSignInClient.signInIntent
            loginLauncher.launch(signInIntent)
        }
    }

    private fun navigateToRoleHome(role: String) {
        try {
            val intent = if (role.equals("owner", ignoreCase = true)) {
                Intent(this, OwnerHomePageActivity::class.java)
            } else {
                Intent(this, HomePageActivity::class.java)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.navigation_error, e.message), Toast.LENGTH_LONG).show()
        }
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
                        Toast.makeText(this, getString(R.string.user_id_not_found), Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    val email = user.email ?: run {
                        Toast.makeText(this, getString(R.string.user_data_not_found), Toast.LENGTH_SHORT).show()
                        auth.signOut()
                        return@addOnCompleteListener
                    }

                    val db = Firebase.firestore
                    val collection = if (selectedRole == "owner") "Owners" else "Tenants"

                    db.collection(collection)
                        .document(email)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val sessionManager = UserSessionManager(this)
                                sessionManager.saveUserData(
                                    document.getString("fullName") ?: user.displayName ?: "",
                                    email,
                                    document.getString("mobileNumber") ?: user.phoneNumber ?: "",
                                    selectedRole!!
                                )
                                navigateToRoleHome(selectedRole!!)
                            } else {
                                Toast.makeText(this, getString(R.string.user_data_not_found), Toast.LENGTH_SHORT).show()
                                auth.signOut()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, getString(R.string.failed_to_fetch_user_role, e.message), Toast.LENGTH_SHORT).show()
                            auth.signOut()
                        }
                } else {
                    Toast.makeText(this, getString(R.string.google_sign_in_failed, task.exception?.message), Toast.LENGTH_LONG).show()
                }
            }
    }
}