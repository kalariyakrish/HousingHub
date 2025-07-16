package com.example.housinghub.owner

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.housinghub.R

class OwnerManagePropertyActivity : AppCompatActivity() {

    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner_manage_property)

        listView = findViewById(R.id.listProperties)

        // Sample data
        val propertyList = listOf("Room 1 - ₹5000", "Room 2 - ₹6500")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, propertyList)

        listView.adapter = adapter
    }
}
