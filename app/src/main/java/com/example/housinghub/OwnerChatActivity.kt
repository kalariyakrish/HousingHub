package com.example.housinghub.owner

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.housinghub.R

class OwnerChatActivity : AppCompatActivity() {

    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner_chat)

        listView = findViewById(R.id.listChats)

        // Sample chat list
        val chats = listOf("Tenant A", "Tenant B", "Tenant C")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, chats)

        listView.adapter = adapter
    }
}
