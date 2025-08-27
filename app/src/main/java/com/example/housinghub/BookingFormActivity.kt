package com.example.housinghub

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.housinghub.R
import com.example.housinghub.databinding.ActivityBookingFormBinding
import com.example.housinghub.managers.BookingManager
import com.example.housinghub.model.Booking
import com.example.housinghub.model.Property
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class BookingFormActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var binding: ActivityBookingFormBinding
    private lateinit var bookingManager: BookingManager
    private lateinit var property: Property
    private var selectedStartDate: Date? = null
    private var selectedDurationMonths = 1
    private lateinit var booking: Booking
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize components
        bookingManager = BookingManager(this)
        
        // Get property data from intent extras instead of serialized object
        val propertyId = intent.getStringExtra("propertyId")
        val ownerId = intent.getStringExtra("ownerId")
        val propertyTitle = intent.getStringExtra("propertyTitle")
        val propertyPrice = intent.getDoubleExtra("propertyPrice", 0.0)
        val propertyAddress = intent.getStringExtra("propertyAddress")

        if (propertyId.isNullOrEmpty() || ownerId.isNullOrEmpty() || propertyTitle.isNullOrEmpty()) {
            Toast.makeText(this, "Property details not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Create a minimal Property object from the passed data
        property = Property(
            id = propertyId,
            ownerId = ownerId,
            title = propertyTitle,
            price = propertyPrice,
            address = propertyAddress ?: "",
            location = propertyAddress ?: ""
        )

        setupUI()
    }

    private fun setupUI() {
        // Setup date picker
        binding.etCheckInDate.setOnClickListener {
            showDatePicker()
        }

        // Setup duration chips
        binding.chipGroupDuration.setOnCheckedChangeListener { _, checkedId ->
            selectedDurationMonths = when (checkedId) {
                R.id.chip1Month -> 1
                R.id.chip6Months -> 6
                R.id.chip12Months -> 12
                else -> 1
            }
            updatePaymentSummary()
        }

        // Setup payment button
        binding.btnProceedPayment.setOnClickListener {
            processBooking()
        }

        // Setup cancel button
        binding.btnCancel?.setOnClickListener {
            finish()
        }

        // Set property details
        binding.tvPropertyTitle.text = property.title
        binding.tvPropertyLocation.text = property.address.ifEmpty { property.location }
        binding.tvMonthlyRent.text = "₹${String.format("%.0f", property.price)}"

        // Load property image from Firestore
        loadPropertyImage()

        // Initialize payment summary
        updatePaymentSummary()
    }

    private fun loadPropertyImage() {
        // Load full property details from Firestore to get image URLs
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("Properties")
            .document(property.ownerId)
            .collection("Available")
            .document(property.id)
            .get()
            .addOnSuccessListener { doc ->
                val fullProperty = doc.toObject(Property::class.java)
                fullProperty?.let { p ->
                    // Update our property object with full details including images
                    property = p.copy(
                        id = property.id,
                        ownerId = property.ownerId,
                        title = property.title,
                        price = property.price,
                        address = property.address,
                        location = property.location
                    )
                    
                    // Load first image if available
                    if (p.images.isNotEmpty()) {
                        Glide.with(this)
                            .load(p.images[0])
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.placeholder_image)
                            .centerCrop()
                            .into(binding.ivPropertyImage)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("BookingFormActivity", "Failed to load property image: ${e.message}")
                // Keep placeholder image
            }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                selectedStartDate = calendar.time
                binding.etCheckInDate.setText(dateFormat.format(selectedStartDate!!))
                updatePaymentSummary()
            },
            year, month, day
        )

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun updatePaymentSummary() {
        val totalAmount = property.price * selectedDurationMonths
        val advanceAmount = property.price * Booking.ADVANCE_PERCENTAGE
        val remainingAmount = totalAmount - advanceAmount

        binding.tvTotalAmount.text = "₹${String.format("%.0f", totalAmount)}"
        binding.tvAdvanceAmount.text = "₹${String.format("%.0f", advanceAmount)}"
        
        // Update duration summary
        val durationText = when (selectedDurationMonths) {
            1 -> "1 Month"
            6 -> "6 Months" 
            12 -> "1 Year"
            else -> "$selectedDurationMonths Months"
        }
        binding.tvDurationSummary.text = durationText
    }

    private fun processBooking() {
        // Validate inputs
        if (selectedStartDate == null) {
            Toast.makeText(this, "Please select check-in date", Toast.LENGTH_SHORT).show()
            return
        }

        val numberOfOccupants = binding.etOccupants.text.toString().toIntOrNull() ?: 1
        val specialNotes = binding.etSpecialNotes.text.toString()

        if (!binding.cbTerms.isChecked) {
            Toast.makeText(this, "Please accept terms and conditions", Toast.LENGTH_SHORT).show()
            return
        }

        // Create booking
        lifecycleScope.launch {
            try {
                val result = bookingManager.createBooking(
                    property = property,
                    startDate = selectedStartDate!!,
                    durationMonths = selectedDurationMonths,
                    numberOfOccupants = numberOfOccupants,
                    specialNotes = specialNotes
                )

                if (result.isSuccess) {
                    booking = result.getOrNull()!!
                    
                    // Save booking to Firestore immediately with pending status
                    val saveResult = bookingManager.saveBooking(booking)
                    if (saveResult.isSuccess) {
                        Log.d("BookingFormActivity", "Booking saved successfully to Firestore")
                        startRazorpayPayment()
                    } else {
                        Log.e("BookingFormActivity", "Failed to save booking: ${saveResult.exceptionOrNull()?.message}")
                        Toast.makeText(
                            this@BookingFormActivity,
                            "Failed to save booking: ${saveResult.exceptionOrNull()?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@BookingFormActivity,
                        "Failed to create booking: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@BookingFormActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startRazorpayPayment() {
        val checkout = Checkout()
        
        try {
            // Set Razorpay key ID
            checkout.setKeyID("rzp_test_A6DrVXpkyXHyww") // Use hardcoded key for now
            
            val options = JSONObject().apply {
                put("name", "HousingHub")
                put("description", "Advance payment for ${property.title}")
                put("currency", "INR")
                put("amount", (booking.calculateAdvanceAmount() * 100).toInt()) // Amount in paise
                put("prefill", JSONObject().apply {
                    put("email", booking.tenantEmail)
                    put("contact", booking.tenantPhone)
                })
                put("theme", JSONObject().apply {
                    put("color", "#1E88E5")
                })
            }

            Log.d("BookingFormActivity", "Starting Razorpay payment for amount: ${booking.calculateAdvanceAmount()}")
            checkout.open(this, options)
            
        } catch (e: Exception) {
            Log.e("BookingFormActivity", "Error in payment setup: ${e.message}", e)
            Toast.makeText(this, "Error in payment setup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentSuccess(paymentId: String?) {
        Log.d("BookingFormActivity", "Payment Success: $paymentId")
        
        if (paymentId != null) {
            // Update booking with payment details
            lifecycleScope.launch {
                try {
                    val updateResult = bookingManager.updateBookingPayment(
                        bookingId = booking.id,
                        paymentId = paymentId,
                        paymentSignature = "", // Not available in success callback
                        razorpayOrderId = "", // Not using orders in this simple implementation
                        amountPaid = booking.calculateAdvanceAmount()
                    )

                    if (updateResult.isSuccess) {
                        Log.d("BookingFormActivity", "Booking payment updated successfully")
                        // Navigate to confirmation screen
                        val intent = Intent(this@BookingFormActivity, BookingConfirmationActivity::class.java)
                        intent.putExtra("bookingId", booking.id)
                        intent.putExtra("propertyTitle", booking.propertyTitle)
                        intent.putExtra("paymentId", paymentId)
                        intent.putExtra("amountPaid", booking.calculateAdvanceAmount())
                        startActivity(intent)
                        finish()
                    } else {
                        Log.e("BookingFormActivity", "Failed to update payment: ${updateResult.exceptionOrNull()?.message}")
                        Toast.makeText(
                            this@BookingFormActivity,
                            "Payment successful but failed to update booking. Please contact support.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("BookingFormActivity", "Error processing payment success: ${e.message}", e)
                    Toast.makeText(
                        this@BookingFormActivity,
                        "Payment successful but error updating: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            Toast.makeText(this, "Payment ID not received", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentError(code: Int, response: String?) {
        Log.e("BookingFormActivity", "Payment Error: $code, $response")
        Toast.makeText(this, "Payment failed: $response", Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
