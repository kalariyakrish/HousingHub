package com.example.housinghub.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.housinghub.databinding.ItemOwnerBookingBinding
import com.example.housinghub.model.Booking
import java.text.SimpleDateFormat
import java.util.*

class OwnerBookingAdapter(
    private val onApproveClick: (Booking) -> Unit,
    private val onRejectClick: (Booking) -> Unit,
    private val onBookingClick: (Booking) -> Unit
) : RecyclerView.Adapter<OwnerBookingAdapter.OwnerBookingViewHolder>() {

    private val bookings = mutableListOf<Booking>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    inner class OwnerBookingViewHolder(private val binding: ItemOwnerBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.tvPropertyTitle.text = booking.propertyTitle
            binding.tvPropertyLocation.text = booking.propertyLocation
            binding.tvBookingId.text = "ID: ${booking.id.take(8).uppercase()}"
            
            // Tenant information
            binding.tvTenantName.text = booking.tenantName
            binding.tvTenantPhone.text = booking.tenantPhone
            binding.tvTenantEmail.text = booking.tenantEmail
            
            // Status
            binding.tvBookingStatus.text = booking.bookingStatus.uppercase()
            binding.tvBookingStatus.setBackgroundColor(getStatusColor(booking.bookingStatus))
            
            // Dates
            binding.tvBookingDates.text = booking.getFormattedDateRange()
            
            // Amount
            binding.tvAmountPaid.text = "₹${String.format("%.0f", booking.amountPaid)} paid"
            
            // Duration and occupants
            val durationText = when (booking.durationMonths) {
                1 -> "1 Month"
                6 -> "6 Months"
                12 -> "1 Year"
                else -> "${booking.durationMonths} Months"
            }
            binding.tvDurationOccupants.text = "$durationText • ${booking.numberOfOccupants} occupants"

            // Special notes
            if (booking.specialNotes.isNotBlank()) {
                binding.tvSpecialNotes.text = "Notes: ${booking.specialNotes}"
                binding.tvSpecialNotes.visibility = View.VISIBLE
            } else {
                binding.tvSpecialNotes.visibility = View.GONE
            }

            // Show/hide action buttons based on status
            val isPending = booking.bookingStatus == Booking.STATUS_CONFIRMED
            binding.btnApprove.visibility = if (isPending) View.VISIBLE else View.GONE
            binding.btnReject.visibility = if (isPending) View.VISIBLE else View.GONE
            binding.actionButtonsLayout.visibility = if (isPending) View.VISIBLE else View.GONE

            // Button click listeners
            binding.btnApprove.setOnClickListener {
                onApproveClick(booking)
            }

            binding.btnReject.setOnClickListener {
                onRejectClick(booking)
            }

            binding.root.setOnClickListener {
                onBookingClick(booking)
            }
        }

        private fun getStatusColor(status: String): Int {
            return when (status.lowercase()) {
                "confirmed" -> 0xFFFF9800.toInt() // Orange - needs approval
                "approved" -> 0xFF4CAF50.toInt() // Green
                "rejected" -> 0xFFF44336.toInt() // Red
                "cancelled" -> 0xFF757575.toInt() // Gray
                "active" -> 0xFF2196F3.toInt() // Blue
                "completed" -> 0xFF9C27B0.toInt() // Purple
                else -> 0xFF757575.toInt() // Gray
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OwnerBookingViewHolder {
        val binding = ItemOwnerBookingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OwnerBookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OwnerBookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<Booking>) {
        bookings.clear()
        bookings.addAll(newBookings)
        notifyDataSetChanged()
    }

    fun removeBooking(bookingId: String) {
        val position = bookings.indexOfFirst { it.id == bookingId }
        if (position != -1) {
            bookings.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateBookingStatus(bookingId: String, newStatus: String) {
        val position = bookings.indexOfFirst { it.id == bookingId }
        if (position != -1) {
            bookings[position].bookingStatus = newStatus
            notifyItemChanged(position)
        }
    }
}
