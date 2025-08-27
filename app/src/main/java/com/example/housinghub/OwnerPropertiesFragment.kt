package com.example.housinghub.owner

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.housinghub.R
import com.example.housinghub.adapters.OwnerBookingAdapter
import com.example.housinghub.databinding.FragmentOwnerPropertiesBinding
import com.example.housinghub.managers.BookingManager
import com.example.housinghub.model.Booking
import kotlinx.coroutines.launch

class OwnerPropertiesFragment : Fragment() {

    private var _binding: FragmentOwnerPropertiesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var ownerBookingAdapter: OwnerBookingAdapter
    private lateinit var bookingManager: BookingManager
    private var allBookings = listOf<Booking>()
    private var currentFilter = "all"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOwnerPropertiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeComponents()
        setupRecyclerView()
        setupFilterChips()
        loadOwnerBookings()
    }

    private fun initializeComponents() {
        bookingManager = BookingManager(requireContext())
        
        ownerBookingAdapter = OwnerBookingAdapter(
            onApproveClick = { booking -> showApproveDialog(booking) },
            onRejectClick = { booking -> showRejectDialog(booking) },
            onBookingClick = { booking -> showBookingDetails(booking) }
        )
    }

    private fun setupRecyclerView() {
        binding.rvOwnerBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ownerBookingAdapter
        }
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener { filterBookings("all") }
        binding.chipPending.setOnClickListener { filterBookings("pending") }
        binding.chipApproved.setOnClickListener { filterBookings("approved") }
        binding.chipRejected.setOnClickListener { filterBookings("rejected") }
    }

    private fun loadOwnerBookings() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = bookingManager.getOwnerBookings()
                
                if (result.isSuccess) {
                    allBookings = result.getOrNull() ?: emptyList()
                    if (allBookings.isEmpty()) {
                        showEmptyState()
                    } else {
                        filterBookings(currentFilter)
                    }
                } else {
                    showError("Failed to load bookings: ${result.exceptionOrNull()?.message}")
                    showEmptyState()
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
                showEmptyState()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun filterBookings(filter: String) {
        currentFilter = filter
        
        val filteredBookings = when (filter) {
            "pending" -> allBookings.filter { it.bookingStatus == Booking.STATUS_CONFIRMED }
            "approved" -> allBookings.filter { it.bookingStatus == Booking.STATUS_APPROVED }
            "rejected" -> allBookings.filter { it.bookingStatus == Booking.STATUS_REJECTED }
            else -> allBookings
        }
        
        if (filteredBookings.isEmpty()) {
            showEmptyStateForFilter(filter)
        } else {
            showBookings(filteredBookings)
        }
    }

    private fun showApproveDialog(booking: Booking) {
        AlertDialog.Builder(requireContext())
            .setTitle("Approve Booking")
            .setMessage("Are you sure you want to approve this booking request from ${booking.tenantName}?")
            .setPositiveButton("Approve") { _, _ ->
                approveBooking(booking)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRejectDialog(booking: Booking) {
        val editText = EditText(requireContext()).apply {
            hint = "Reason for rejection (optional)"
            setPadding(16, 16, 16, 16)
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Reject Booking")
            .setMessage("Are you sure you want to reject this booking request from ${booking.tenantName}?")
            .setView(editText)
            .setPositiveButton("Reject") { _, _ ->
                val reason = editText.text.toString().ifEmpty { "No reason provided" }
                rejectBooking(booking, reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun approveBooking(booking: Booking) {
        lifecycleScope.launch {
            try {
                val result = bookingManager.approveBooking(booking.id)
                
                if (result.isSuccess) {
                    showSuccess("Booking approved successfully!")
                    ownerBookingAdapter.updateBookingStatus(booking.id, Booking.STATUS_APPROVED)
                    
                    // Update local list
                    allBookings = allBookings.map { 
                        if (it.id == booking.id) it.copy(bookingStatus = Booking.STATUS_APPROVED) 
                        else it 
                    }
                    
                    // Refresh current filter
                    filterBookings(currentFilter)
                } else {
                    showError("Failed to approve booking: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showError("Error approving booking: ${e.message}")
            }
        }
    }

    private fun rejectBooking(booking: Booking, reason: String) {
        lifecycleScope.launch {
            try {
                val result = bookingManager.rejectBooking(booking.id, reason)
                
                if (result.isSuccess) {
                    showSuccess("Booking rejected successfully!")
                    ownerBookingAdapter.updateBookingStatus(booking.id, Booking.STATUS_REJECTED)
                    
                    // Update local list
                    allBookings = allBookings.map { 
                        if (it.id == booking.id) it.copy(bookingStatus = Booking.STATUS_REJECTED) 
                        else it 
                    }
                    
                    // Refresh current filter
                    filterBookings(currentFilter)
                } else {
                    showError("Failed to reject booking: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                showError("Error rejecting booking: ${e.message}")
            }
        }
    }

    private fun showBookings(bookings: List<Booking>) {
        binding.rvOwnerBookings.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE
        ownerBookingAdapter.updateBookings(bookings)
    }

    private fun showEmptyState() {
        binding.rvOwnerBookings.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.tvEmptyTitle.text = "No Booking Requests"
        binding.tvEmptyMessage.text = "When tenants book your properties,\ntheir requests will appear here for approval."
    }

    private fun showEmptyStateForFilter(filter: String) {
        binding.rvOwnerBookings.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
        
        when (filter) {
            "pending" -> {
                binding.tvEmptyTitle.text = "No Pending Requests"
                binding.tvEmptyMessage.text = "All booking requests have been\nprocessed or no new requests yet."
            }
            "approved" -> {
                binding.tvEmptyTitle.text = "No Approved Bookings"
                binding.tvEmptyMessage.text = "You haven't approved any\nbooking requests yet."
            }
            "rejected" -> {
                binding.tvEmptyTitle.text = "No Rejected Bookings"
                binding.tvEmptyMessage.text = "You haven't rejected any\nbooking requests yet."
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showBookingDetails(booking: Booking) {
        val details = buildString {
            append("Property: ${booking.propertyTitle}\n")
            append("Location: ${booking.propertyLocation}\n")
            append("Tenant: ${booking.tenantName}\n")
            append("Phone: ${booking.tenantPhone}\n")
            append("Email: ${booking.tenantEmail}\n")
            append("Duration: ${booking.durationMonths} months\n")
            append("Occupants: ${booking.numberOfOccupants}\n")
            append("Dates: ${booking.getFormattedDateRange()}\n")
            append("Amount Paid: ₹${String.format("%.0f", booking.amountPaid)}\n")
            append("Status: ${booking.bookingStatus.uppercase()}")
            
            if (booking.specialNotes.isNotBlank()) {
                append("\n\nSpecial Notes:\n${booking.specialNotes}")
            }
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Booking Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
