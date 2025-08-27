package com.example.housinghub

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.housinghub.databinding.ActivityBookingConfirmationBinding
import com.example.housinghub.model.Booking
import java.text.SimpleDateFormat
import java.util.*

class BookingConfirmationActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_BOOKING = "extra_booking"
        
        fun start(context: Context, booking: Booking) {
            val intent = Intent(context, BookingConfirmationActivity::class.java)
            intent.putExtra(EXTRA_BOOKING, booking)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityBookingConfirmationBinding
    private lateinit var booking: Booking
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get booking data from intent extras (new approach)
        val bookingId = intent.getStringExtra("bookingId")
        val propertyTitle = intent.getStringExtra("propertyTitle")
        val paymentId = intent.getStringExtra("paymentId")
        val amountPaid = intent.getDoubleExtra("amountPaid", 0.0)

        if (bookingId.isNullOrEmpty() || propertyTitle.isNullOrEmpty()) {
            // Try old approach for backward compatibility
            booking = intent.getSerializableExtra(EXTRA_BOOKING) as? Booking
                ?: run {
                    Toast.makeText(this, "Booking data not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
            setupUI()
        } else {
            // Create minimal booking object from intent data
            setupUIFromIntentData(bookingId, propertyTitle, paymentId, amountPaid)
        }

        setupClickListeners()
    }

    private fun setupUIFromIntentData(bookingId: String, propertyTitle: String, paymentId: String?, amountPaid: Double) {
        // Booking details
        binding.tvBookingId.text = bookingId.take(8).uppercase()
        binding.tvPropertyName.text = propertyTitle
        
        // Payment details
        binding.tvPaymentId.text = paymentId ?: "N/A"
        binding.tvAmountPaid.text = "₹${String.format("%.0f", amountPaid)}"
        
        // Set placeholders for data we don't have
        binding.tvLocation.text = "Please check booking details"
        binding.tvCheckInDate.text = "As selected"
        binding.tvDuration.text = "As selected"
        binding.tvOccupants.text = "As selected"
        binding.tvRemainingAmount.text = "To be paid on check-in"
    }

    private fun setupUI() {
        // Booking details
        binding.tvBookingId.text = booking.id.take(8).uppercase() // Show first 8 characters
        binding.tvPropertyName.text = booking.propertyTitle
        binding.tvLocation.text = booking.propertyLocation
        
        // Dates
        binding.tvCheckInDate.text = booking.startDate?.let { 
            dateFormat.format(it.toDate()) 
        } ?: "Not set"
        
        // Duration
        binding.tvDuration.text = when (booking.durationMonths) {
            1 -> "1 Month"
            6 -> "6 Months"
            12 -> "1 Year"
            else -> "${booking.durationMonths} Months"
        }
        
        binding.tvOccupants.text = booking.numberOfOccupants.toString()

        // Payment details
        binding.tvPaymentId.text = booking.paymentId
        binding.tvAmountPaid.text = "₹${String.format("%.0f", booking.amountPaid)}"
        
        val remainingAmount = booking.totalAmount - booking.amountPaid
        binding.tvRemainingAmount.text = "₹${String.format("%.0f", remainingAmount)}"
    }

    private fun setupClickListeners() {
        binding.btnContactOwner.setOnClickListener {
            contactOwner()
        }

        binding.btnViewBookings.setOnClickListener {
            // Navigate to bookings list (you can implement this)
            Toast.makeText(this, "My Bookings feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnBackToHome.setOnClickListener {
            navigateToHome()
        }
    }

    private fun contactOwner() {
        // Check if we have booking object with owner phone
        if (::booking.isInitialized && booking.ownerPhone.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${booking.ownerPhone}")
            }
            startActivity(intent)
        } else {
            // If we don't have owner phone, show a message
            Toast.makeText(this, "Please contact property owner through property details", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomePageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        navigateToHome()
    }
}
