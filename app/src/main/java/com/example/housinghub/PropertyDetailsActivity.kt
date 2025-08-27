package com.example.housinghub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.housinghub.adapters.PropertyImageCarouselAdapter
import com.example.housinghub.model.Property
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide

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

    // Enhanced Views
    private lateinit var recyclerImages: RecyclerView
    private lateinit var tvImageCount: TextView
    private lateinit var fabBookmark: FloatingActionButton
    private lateinit var btnMessage: MaterialButton
    private lateinit var btnBook: MaterialButton
    private lateinit var tvTitle: TextView
    private lateinit var tvType: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvPrice: TextView
    private lateinit var chipAvailability: Chip
    private lateinit var layoutBeds: LinearLayout
    private lateinit var layoutBaths: LinearLayout
    private lateinit var layoutArea: LinearLayout
    private lateinit var tvBedrooms: TextView
    private lateinit var tvBathrooms: TextView
    private lateinit var tvArea: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvOwnerInitials: TextView
    private lateinit var tvOwnerName: TextView
    private lateinit var tvOwnerType: TextView
    private lateinit var btnCallOwner: ImageButton
    private lateinit var layoutVideos: androidx.cardview.widget.CardView
    private lateinit var tvVideoLink: TextView
    private lateinit var ivVideoPreview: ImageView
    private lateinit var btnPlayVideo: ImageButton

    private val firestore = FirebaseFirestore.getInstance()
    private var currentImagePosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_property_details)

        initializeViews()
        setupToolbar()
        setupClickListeners()
        loadPropertyData()
    }

    private fun initializeViews() {
        recyclerImages = findViewById(R.id.recyclerImages)
        tvImageCount = findViewById(R.id.tvImageCount)
        fabBookmark = findViewById(R.id.fabBookmark)
        btnMessage = findViewById(R.id.btnMessage)
        btnBook = findViewById(R.id.btnBook)
        tvTitle = findViewById(R.id.tvTitle)
        tvType = findViewById(R.id.tvType)
        tvAddress = findViewById(R.id.tvAddress)
        tvPrice = findViewById(R.id.tvPrice)
        chipAvailability = findViewById(R.id.chipAvailability)
        layoutBeds = findViewById(R.id.layoutBeds)
        layoutBaths = findViewById(R.id.layoutBaths)
        layoutArea = findViewById(R.id.layoutArea)
        tvBedrooms = findViewById(R.id.tvBedrooms)
        tvBathrooms = findViewById(R.id.tvBathrooms)
        tvArea = findViewById(R.id.tvArea)
        tvDescription = findViewById(R.id.tvDescription)
        tvOwnerInitials = findViewById(R.id.tvOwnerInitials)
        tvOwnerName = findViewById(R.id.tvOwnerName)
        tvOwnerType = findViewById(R.id.tvOwnerType)
        btnCallOwner = findViewById(R.id.btnCallOwner)
        layoutVideos = findViewById(R.id.layoutVideos)
        tvVideoLink = findViewById(R.id.tvVideoLink)
        ivVideoPreview = findViewById(R.id.ivVideoPreview)
        btnPlayVideo = findViewById(R.id.btnPlayVideo)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupClickListeners() {
        fabBookmark.setOnClickListener {
            toggleBookmark()
        }

        btnMessage.setOnClickListener {
            openMessageScreen()
        }

        btnBook.setOnClickListener {
            bookProperty()
        }

        btnCallOwner.setOnClickListener {
            callOwner()
        }
    }

    private fun loadPropertyData() {
        val ownerId = intent.getStringExtra(EXTRA_OWNER)
        val propertyId = intent.getStringExtra(EXTRA_PROPERTY_ID)

        if (ownerId.isNullOrEmpty() || propertyId.isNullOrEmpty()) {
            Toast.makeText(this, "Property identifier missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showLoading(true)
        
        firestore.collection("Properties")
            .document(ownerId)
            .collection("Available")
            .document(propertyId)
            .get()
            .addOnSuccessListener { doc ->
                showLoading(false)
                val p = doc.toObject(Property::class.java)
                if (p != null) {
                    p.id = doc.id
                    p.ownerId = ownerId
                    this.property = p
                    bindProperty(p)
                    loadOwnerInfo(ownerId)
                } else {
                    Toast.makeText(this, "Property not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Failed to load property: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun bindProperty(p: Property) {
        // Basic property information
        tvTitle.text = p.title
        tvType.text = p.type
        tvAddress.text = if (p.location.isNotEmpty()) p.location else p.address
        tvPrice.text = "₹${String.format("%.0f", p.price)}"

        // Availability status
        if (p.isAvailable) {
            chipAvailability.text = "Available"
            chipAvailability.setChipBackgroundColorResource(R.color.primary_green)
        } else {
            chipAvailability.text = "Not Available"
            chipAvailability.setChipBackgroundColorResource(R.color.red_500)
        }

        // Property features
        setupPropertyFeatures(p)

        // Description
        tvDescription.text = if (p.description.isNotEmpty()) p.description else "No description available"

        // Setup image carousel
        setupImageCarousel(p.images)

        // Setup video section
        setupVideoSection(p.videos)

        // Bookmark status
        updateBookmarkIcon(p.isBookmarked)
    }

    private fun setupPropertyFeatures(p: Property) {
        // Use actual bedroom count from property data
        if (p.bedrooms > 0) {
            tvBedrooms.text = p.bedrooms.toString()
            layoutBeds.visibility = View.VISIBLE
        } else {
            layoutBeds.visibility = View.GONE
        }

        // Use actual bathroom count from property data
        if (p.bathrooms > 0) {
            tvBathrooms.text = p.bathrooms.toString()
            layoutBaths.visibility = View.VISIBLE
        } else {
            layoutBaths.visibility = View.GONE
        }

        // Hide area section since we're focusing on bedrooms/bathrooms
        layoutArea.visibility = View.GONE
    }

    private fun setupImageCarousel(images: List<String>) {
        if (images.isEmpty()) {
            tvImageCount.text = "0/0"
            return
        }

        val imageAdapter = PropertyImageCarouselAdapter { position ->
            // Handle image click - could open full screen gallery
            Toast.makeText(this, "Image ${position + 1} clicked", Toast.LENGTH_SHORT).show()
        }

        recyclerImages.apply {
            layoutManager = LinearLayoutManager(this@PropertyDetailsActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = imageAdapter
        }

        // Add snap helper for better scrolling experience
        PagerSnapHelper().attachToRecyclerView(recyclerImages)

        // Add scroll listener to update image count
        recyclerImages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val position = layoutManager.findFirstVisibleItemPosition()
                if (position >= 0) {
                    currentImagePosition = position
                    tvImageCount.text = "${position + 1}/${images.size}"
                }
            }
        })

        imageAdapter.submitList(images)
        tvImageCount.text = "1/${images.size}"
    }

    private fun setupVideoSection(videos: List<String>) {
        if (videos.isNullOrEmpty()) {
            layoutVideos.visibility = View.GONE
            return
        }

        layoutVideos.visibility = View.VISIBLE
        val videoUrl = videos[0]

        // Generate video thumbnail
        generateVideoThumbnail(videoUrl)

        btnPlayVideo.setOnClickListener {
            playVideo(videoUrl)
        }
    }

    private fun generateVideoThumbnail(videoUrl: String) {
        try {
            // Use Glide to load video thumbnail
            Glide.with(this)
                .asBitmap()
                .load(videoUrl)
                .centerCrop()
                .error(R.drawable.ic_video_placeholder) // Fallback icon if thumbnail generation fails
                .into(ivVideoPreview)
        } catch (e: Exception) {
            // If video thumbnail fails, show a video placeholder icon
            ivVideoPreview.setImageResource(R.drawable.ic_video_placeholder)
        }
    }

    private fun loadOwnerInfo(ownerId: String) {
        firestore.collection("Owners")
            .document(ownerId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val ownerName = doc.getString("fullName") ?: "Property Owner"
                    val ownerInitials = getInitials(ownerName)
                    
                    tvOwnerName.text = ownerName
                    tvOwnerInitials.text = ownerInitials
                    tvOwnerType.text = "Property Owner"
                } else {
                    // Fallback to email-based name
                    val emailName = ownerId.substringBefore("@").replace(".", " ").split(" ")
                        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }
                    tvOwnerName.text = emailName
                    tvOwnerInitials.text = getInitials(emailName)
                    tvOwnerType.text = "Property Owner"
                }
            }
            .addOnFailureListener {
                tvOwnerName.text = "Property Owner"
                tvOwnerInitials.text = "PO"
                tvOwnerType.text = "Property Owner"
            }
    }

    private fun getInitials(name: String): String {
        return name.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifEmpty { "PO" }
    }

    private fun toggleBookmark() {
        property?.let { p ->
            p.isBookmarked = !p.isBookmarked
            updateBookmarkIcon(p.isBookmarked)
            
            val message = if (p.isBookmarked) "Added to bookmarks" else "Removed from bookmarks"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBookmarkIcon(isBookmarked: Boolean) {
        val iconRes = if (isBookmarked) {
            android.R.drawable.btn_star_big_on
        } else {
            android.R.drawable.btn_star_big_off
        }
        fabBookmark.setImageResource(iconRes)
    }

    private fun openMessageScreen() {
        property?.let { p ->
            Toast.makeText(this, "Opening chat with ${p.ownerId}", Toast.LENGTH_SHORT).show()
            // TODO: Implement actual messaging functionality
        }
    }

    private fun bookProperty() {
        property?.let { p ->
            if (p.isAvailable) {
                // Navigate to booking form with individual property data instead of full object
                val intent = Intent(this, BookingFormActivity::class.java)
                intent.putExtra("propertyId", p.id)
                intent.putExtra("ownerId", p.ownerId)
                intent.putExtra("propertyTitle", p.title)
                intent.putExtra("propertyPrice", p.price)
                intent.putExtra("propertyAddress", p.address.ifEmpty { p.location })
                startActivity(intent)
            } else {
                Toast.makeText(this, "This property is not available for booking", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun callOwner() {
        property?.let { p ->
            Toast.makeText(this, "Calling owner for ${p.title}", Toast.LENGTH_SHORT).show()
            // TODO: Implement actual calling functionality
            // val intent = Intent(Intent.ACTION_CALL).apply {
            //     data = Uri.parse("tel:$ownerPhoneNumber")
            // }
            // startActivity(intent)
        }
    }

    private fun playVideo(videoUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(videoUrl), "video/*")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot play video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(show: Boolean) {
        findViewById<View>(R.id.progressOverlay).visibility = if (show) View.VISIBLE else View.GONE
    }
}
