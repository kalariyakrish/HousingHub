package com.example.housinghub

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.housinghub.OwnerHomeFragment
import com.example.housinghub.R
import com.example.housinghub.databinding.ActivityOwnerHomePageBinding
import com.example.housinghub.owner.OwnerMessagesFragment
import com.example.housinghub.owner.OwnerProfileFragment
import com.example.housinghub.owner.OwnerPropertiesFragment

class OwnerHomePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerHomePageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(OwnerHomeFragment())
        }

        // Handle bottom navigation item clicks
        binding.bottomNavigationOwner.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_owner_home -> loadFragment(OwnerHomeFragment())
                R.id.nav_owner_properties -> loadFragment(OwnerPropertiesFragment())
                R.id.nav_owner_messages -> loadFragment(OwnerMessagesFragment())
                R.id.nav_owner_profile -> loadFragment(OwnerProfileFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
