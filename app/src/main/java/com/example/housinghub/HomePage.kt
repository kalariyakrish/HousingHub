package com.example.housinghub

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.housinghub.databinding.ActivityHomePageBinding
import com.example.housinghub.ui.profile.ProfileFragment

class HomePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomePageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set default selected item
        binding.bottomNavigation.selectedItemId = R.id.nav_home

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }


        // Bottom Navigation Item Selection
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_search -> SearchFragment()
                R.id.nav_saved -> SavedFragment()
                R.id.nav_messages -> MessageFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Any) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment as Fragment)
            .commit()
    }
}
