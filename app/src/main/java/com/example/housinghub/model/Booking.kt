package com.example.housinghub.model

import com.google.firebase.Timestamp
import java.io.Serializable

data class Booking(
    var id: String = "",
    var tenantEmail: String = "",
    var tenantName: String = "",
    var tenantPhone: String = "",
    var ownerEmail: String = "",
    var ownerName: String = "",
    var ownerPhone: String = "",
    var propertyId: String = "",
    var propertyTitle: String = "",
    var propertyLocation: String = "",
    var propertyPrice: Double = 0.0,
    var bookingStatus: String = "pending", // pending, confirmed, cancelled
    var paymentId: String = "",
    var paymentSignature: String = "",
    var razorpayOrderId: String = "",
    var amountPaid: Double = 0.0,
    var totalAmount: Double = 0.0,
    var startDate: Timestamp? = null,
    var endDate: Timestamp? = null,
    var durationMonths: Int = 1,
    var numberOfOccupants: Int = 1,
    var specialNotes: String = "",
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null,
    var paymentDate: Timestamp? = null,
    var cancellationReason: String = "",
    var refundAmount: Double = 0.0,
    var securityDeposit: Double = 0.0
) : Serializable {
    
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_CONFIRMED = "confirmed"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_ACTIVE = "active"
        const val STATUS_COMPLETED = "completed"
        
        // Booking advance percentage (20% of monthly rent)
        const val ADVANCE_PERCENTAGE = 0.20
        
        // Security deposit (1 month rent)
        const val SECURITY_DEPOSIT_MONTHS = 1
    }
    
    fun calculateTotalAmount(): Double {
        return propertyPrice * durationMonths
    }
    
    fun calculateAdvanceAmount(): Double {
        return propertyPrice * ADVANCE_PERCENTAGE
    }
    
    fun calculateSecurityDeposit(): Double {
        return propertyPrice * SECURITY_DEPOSIT_MONTHS
    }
    
    fun getFormattedDateRange(): String {
        val start = startDate?.toDate()
        val end = endDate?.toDate()
        return if (start != null && end != null) {
            val startStr = android.text.format.DateFormat.format("MMM dd, yyyy", start)
            val endStr = android.text.format.DateFormat.format("MMM dd, yyyy", end)
            "$startStr - $endStr"
        } else {
            "Date not set"
        }
    }
}
