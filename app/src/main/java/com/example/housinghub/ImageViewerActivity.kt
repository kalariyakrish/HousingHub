package com.example.housinghub

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.housinghub.databinding.ActivityImageViewerBinding

class ImageViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val images = intent.getStringArrayListExtra("images") ?: arrayListOf()
        val initialPosition = intent.getIntExtra("position", 0)

        setupImageViewer(images, initialPosition)
        setupToolbar()
    }

    private fun setupImageViewer(images: ArrayList<String>, initialPosition: Int) {
        val adapter = ImageSliderAdapter(
            onImageClick = { /* Optional click handling */ },
            onDeleteClick = { /* Optional delete handling */ }
        )
        binding.viewPager.apply {
            this.adapter = adapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
            setCurrentItem(initialPosition, false)
        }
        adapter.submitList(images)

        // Update counter text
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateCounter(position + 1, images.size)
            }
        })
        updateCounter(initialPosition + 1, images.size)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun updateCounter(current: Int, total: Int) {
        binding.tvCounter.text = "$current/$total"
    }
}
