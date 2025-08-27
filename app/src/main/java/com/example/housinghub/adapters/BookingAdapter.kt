package com.example.housinghub.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.housinghub.databinding.ItemBookingBinding
import com.example.housinghub.model.Booking
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(
    private val onBookingClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    private val bookings = mutableListOf<Booking>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    inner class BookingViewHolder(private val binding: ItemBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.tvPropertyTitle.text = booking.propertyTitle
            binding.tvPropertyLocation.text = booking.propertyLocation
            binding.tvBookingId.text = "ID: ${booking.id.take(8).uppercase()}"
            
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

            binding.root.setOnClickListener {
                onBookingClick(booking)
            }
        }

        private fun getStatusColor(status: String): Int {
            return when (status.lowercase()) {
                "confirmed" -> 0xFF4CAF50.toInt() // Green
                "pending" -> 0xFFFF9800.toInt() // Orange
                "cancelled" -> 0xFFF44336.toInt() // Red
                "active" -> 0xFF2196F3.toInt() // Blue
                "completed" -> 0xFF9C27B0.toInt() // Purple
                else -> 0xFF757575.toInt() // Gray
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<Booking>) {
        bookings.clear()
        bookings.addAll(newBookings)
        notifyDataSetChanged()
    }
}
