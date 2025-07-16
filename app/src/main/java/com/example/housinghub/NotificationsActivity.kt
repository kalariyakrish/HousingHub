package com.example.housinghub.owner

import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.housinghub.R

class NotificationsActivity : AppCompatActivity() {

    private lateinit var switchAppNotifications: Switch
    private lateinit var switchEmailNotifications: Switch
    private lateinit var switchSMSNotifications: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        switchAppNotifications = findViewById(R.id.switchAppNotifications)
        switchEmailNotifications = findViewById(R.id.switchEmailNotifications)
        switchSMSNotifications = findViewById(R.id.switchSMSNotifications)

        val listener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            val type = when (buttonView.id) {
                R.id.switchAppNotifications -> "App"
                R.id.switchEmailNotifications -> "Email"
                R.id.switchSMSNotifications -> "SMS"
                else -> "Unknown"
            }
            Toast.makeText(this, "$type notifications ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        switchAppNotifications.setOnCheckedChangeListener(listener)
        switchEmailNotifications.setOnCheckedChangeListener(listener)
        switchSMSNotifications.setOnCheckedChangeListener(listener)
    }
}
