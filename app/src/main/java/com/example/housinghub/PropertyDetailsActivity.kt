package com.example.housinghub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearSnapHelper
import android.widget.ImageView
import android.widget.ImageButton
import com.example.housinghub.model.Property
import com.google.firebase.firestore.FirebaseFirestore
import com.example.housinghub.adapters.ImageSliderAdapter
import com.google.android.material.appbar.MaterialToolbar

class PropertyDetailsActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_OWNER = "extra_owner"
        private const val EXTRA_PROPERTY_ID = "extra_property_id"

        fun start(context: Context, ownerId: String, propertyId: String) {
            val intent = Intent(context, PropertyDetailsActivity::class.java)
            intent.putExtra(EXTRA_OWNER, ownerId)
            intent.putExtra(EXTRA_PROPERTY_ID, propertyId)
            context.startActivity(intent)
        }
    }

    private var property: Property? = null

    // Views
    private lateinit var recyclerImages: RecyclerView
    private lateinit var btnMessage: Button
    private lateinit var btnBook: Button
    private lateinit var tvTitle: TextView
    private lateinit var tvType: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvPrice: TextView
    private lateinit var layoutBeds: LinearLayout
    private lateinit var layoutBaths: LinearLayout
    private lateinit var tvBedrooms: TextView
    private lateinit var tvBathrooms: TextView
    private lateinit var tvDescription: TextView
    private lateinit var layoutVideos: androidx.cardview.widget.CardView
    private lateinit var tvVideoLink: TextView
    private lateinit var ivVideoPreview: ImageView
    private lateinit var btnPlayVideo: ImageButton

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_property_details)

    // init views
    val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
    recyclerImages = findViewById(R.id.recyclerImages)
    btnMessage = findViewById(R.id.btnMessage)
    btnBook = findViewById(R.id.btnBook)
    tvTitle = findViewById(R.id.tvTitle)
    tvType = findViewById(R.id.tvType)
    tvAddress = findViewById(R.id.tvAddress)
    tvPrice = findViewById(R.id.tvPrice)
    layoutBeds = findViewById(R.id.layoutBeds)
    layoutBaths = findViewById(R.id.layoutBaths)
    tvBedrooms = findViewById(R.id.tvBedrooms)
    tvBathrooms = findViewById(R.id.tvBathrooms)
    tvDescription = findViewById(R.id.tvDescription)
    layoutVideos = findViewById(R.id.layoutVideos)
    tvVideoLink = findViewById(R.id.tvVideoLink)
    ivVideoPreview = findViewById(R.id.ivVideoPreview)
    btnPlayVideo = findViewById(R.id.btnPlayVideo)
    val progressOverlay = findViewById<View>(R.id.progressOverlay)

    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    toolbar.setNavigationOnClickListener { onBackPressed() }

        val ownerId = intent.getStringExtra(EXTRA_OWNER)
        val propertyId = intent.getStringExtra(EXTRA_PROPERTY_ID)

        if (ownerId.isNullOrEmpty() || propertyId.isNullOrEmpty()) {
            Toast.makeText(this, "Property identifier missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch property document from Firestore
        progressOverlay.visibility = View.VISIBLE
        firestore.collection("Properties")
            .document(ownerId)
            .collection("Available")
            .document(propertyId)
            .get()
            .addOnSuccessListener { doc ->
                progressOverlay.visibility = View.GONE
                val p = doc.toObject(Property::class.java)
                if (p != null) {
                    p.id = doc.id
                    p.ownerId = ownerId
                    bindProperty(p)
                    toolbar.title = p.title
                } else {
                    Toast.makeText(this, "Property not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressOverlay.visibility = View.GONE
                Toast.makeText(this, "Failed to load property: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }

        btnMessage.setOnClickListener {
            Toast.makeText(this, "Message clicked", Toast.LENGTH_SHORT).show()
        }

        btnBook.setOnClickListener {
            Toast.makeText(this, "Book clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindProperty(p: Property) {
        tvTitle.text = p.title
        tvType.text = p.type
        tvAddress.text = p.address
        tvPrice.text = "\u20b9${p.price}"

        val bedrooms = try {
            Regex("(\\d+)").find(p.type)?.value
        } catch (e: Exception) { null }

        if (!bedrooms.isNullOrEmpty()) {
            tvBedrooms.text = bedrooms
            layoutBeds.visibility = View.VISIBLE
        } else {
            layoutBeds.visibility = View.GONE
        }

        val bathrooms = try {
            Regex("(\\d+)\\s*bath", RegexOption.IGNORE_CASE).find(p.description)?.groupValues?.get(1)
        } catch (e: Exception) { null }

        if (!bathrooms.isNullOrEmpty()) {
            tvBathrooms.text = bathrooms
            layoutBaths.visibility = View.VISIBLE
        } else {
            layoutBaths.visibility = View.GONE
        }

        tvDescription.text = p.description

        val imageAdapter = ImageSliderAdapter(
            onImageClick = { _ -> /* optional full-screen */ },
            onDeleteClick = { /* no-op in details */ }
        )

        recyclerImages.apply {
            layoutManager = LinearLayoutManager(this@PropertyDetailsActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = imageAdapter
        }
        // Add snap helper for nicer paging feel
        LinearSnapHelper().attachToRecyclerView(recyclerImages)

        imageAdapter.submitList(p.images)

        if (!p.videos.isNullOrEmpty()) {
            layoutVideos.visibility = View.VISIBLE
            val videoUrl = p.videos[0]
            // show a thumbnail in ivVideoPreview if available (first image as fallback)
            if (!p.images.isNullOrEmpty()) {
                val thumb = p.images[0]
                com.bumptech.glide.Glide.with(this).load(thumb).centerCrop().into(ivVideoPreview)
            }

            btnPlayVideo.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse(videoUrl), "video/*")
                startActivity(intent)
            }
        } else {
            layoutVideos.visibility = View.GONE
        }
    }
}
