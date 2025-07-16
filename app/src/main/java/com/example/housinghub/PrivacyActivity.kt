package com.example.housinghub.owner

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.housinghub.R

class PrivacyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        // You can handle click or settings change events here
        Toast.makeText(this, "Privacy Settings Loaded", Toast.LENGTH_SHORT).show()
    }
}
