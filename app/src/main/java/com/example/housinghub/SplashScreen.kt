package com.example.housinghub

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp // ✅ Import Firebase

@SuppressLint("CustomSplashScreen")
@Suppress("DEPRECATION")
class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Initialize Firebase SDK
        FirebaseApp.initializeApp(this)

        setContentView(R.layout.activity_splash_screen)

        Log.d("SplashActivity", "SplashActivity launched")
        Toast.makeText(this, "Splash loaded", Toast.LENGTH_SHORT).show()

        // Initialize views
        val logo = findViewById<ImageView>(R.id.logoImage)
        val title = findViewById<TextView>(R.id.appTitle)
        val tagline = findViewById<TextView>(R.id.appTagline)

        // Load and apply animations
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up)

        logo.startAnimation(scaleUp)
        title.startAnimation(fadeIn)
        tagline.startAnimation(fadeIn)

        // Shine animation
        val shine = findViewById<View>(R.id.shineView)
        shine.post {
            val anim = ObjectAnimator.ofFloat(shine, "translationX", 0f, title.width.toFloat())
            anim.duration = 1500
            anim.repeatCount = 0
            anim.start()
        }

        // Navigate to LoginActivity after a 3-second delay
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val intent = Intent(this, LoginPage::class.java)
                startActivity(intent)
                finish()
                Log.d("SplashActivity", "Navigated to LoginPage")
            } catch (e: Exception) {
                Log.e("SplashActivity", "Failed to start LoginPage: ${e.message}")
                Toast.makeText(this, "Error loading login page", Toast.LENGTH_LONG).show()
            }
        }, 3000)
    }
}
