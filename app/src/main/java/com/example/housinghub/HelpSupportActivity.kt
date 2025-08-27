package com.example.housinghub.owner

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.housinghub.R

class HelpSupportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_support)

        // You can add click listeners or logic here
        Toast.makeText(this, "Help & Support Loaded", Toast.LENGTH_SHORT).show()
    }
}
