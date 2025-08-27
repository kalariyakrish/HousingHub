package com.example.housinghub.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.housinghub.R
import com.example.housinghub.adapters.BookingAdapter
import com.example.housinghub.databinding.FragmentTenantBookingsBinding
import com.example.housinghub.managers.BookingManager
import com.example.housinghub.model.Booking
import kotlinx.coroutines.launch

class TenantBookingsFragment : Fragment() {

    private var _binding: FragmentTenantBookingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var bookingAdapter: BookingAdapter
    private lateinit var bookingManager: BookingManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTenantBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeComponents()
        setupRecyclerView()
        loadUserBookings()
    }

    private fun initializeComponents() {
        bookingManager = BookingManager(requireContext())
        
        bookingAdapter = BookingAdapter { booking ->
            // Handle booking item click - could navigate to booking details
            showBookingDetails(booking)
        }
    }

    private fun setupRecyclerView() {
        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
        }
        
        // Handle explore properties button
        binding.btnExploreProperties.setOnClickListener {
            // Navigate back or close this view - for now just show a message
            Toast.makeText(requireContext(), "Navigate to explore properties", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserBookings() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = bookingManager.getTenantBookings()
                
                if (result.isSuccess) {
                    val bookings = result.getOrNull() ?: emptyList()
                    if (bookings.isEmpty()) {
                        showEmptyState()
                    } else {
                        showBookings(bookings)
                    }
                } else {
                    showError("Failed to load bookings")
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

    private fun showBookings(bookings: List<Booking>) {
        binding.rvBookings.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE
        bookingAdapter.updateBookings(bookings)
    }

    private fun showEmptyState() {
        binding.rvBookings.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showBookingDetails(booking: Booking) {
        // For now, just show a toast with booking details
        // Later this could navigate to a detailed booking view
        Toast.makeText(
            requireContext(),
            "Booking: ${booking.propertyTitle}\nStatus: ${booking.bookingStatus}",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
