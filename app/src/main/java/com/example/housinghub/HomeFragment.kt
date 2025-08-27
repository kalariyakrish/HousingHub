package com.example.housinghub

import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.example.housinghub.SharedViewModel.Viewmodel.SharedViewModel
import com.example.housinghub.adapters.EnhancedPropertyAdapter
import com.example.housinghub.adapters.PropertyInteractionListener
import com.example.housinghub.databinding.FragmentHomeBinding
import com.example.housinghub.model.Property

class HomeFragment : Fragment(), PropertyInteractionListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var propertyAdapter: EnhancedPropertyAdapter
    private lateinit var sharedViewModel: SharedViewModel
    private val firestore = FirebaseFirestore.getInstance()
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    
    // Current property list for search and filtering functionality
    private var allProperties: List<Property> = emptyList()
    private var filteredProperties: List<Property> = emptyList()
    private var searchJob: Job? = null
    private var currentFilter: String = "All"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize binding and views
        swipeRefreshLayout = _binding?.swipeRefresh
        
        // Initialize the rest of the UI
        setupUI()
        setupSearch()
        setupFilters()
        fetchProperties()
    }

    private fun setupUI() {
        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        // Setup Enhanced RecyclerView with Linear Layout
        propertyAdapter = EnhancedPropertyAdapter(sharedViewModel, this)
        binding.propertyRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = propertyAdapter
            addItemDecoration(PropertyItemDecoration(resources.getDimensionPixelSize(R.dimen.card_margin)))
        }

        // Setup SwipeRefreshLayout
        swipeRefreshLayout?.setOnRefreshListener {
            fetchProperties()
        }

        // Setup refresh button
        binding.btnRefresh?.setOnClickListener {
            fetchProperties()
        }

        // Initial loading state
        showLoading(false)
        showEmptyState(false)
    }

    private fun setupSearch() {
        val textWatcher = object : TextWatcher {
            private var searchJob: Job? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // Debounce typing
                    s?.toString()?.let { query ->
                        filterProperties(query)
                    }
                }
            }
        }
        
        binding.searchEditText.addTextChangedListener(textWatcher)

        // Search button click
        binding.btnSearch?.setOnClickListener {
            val query = binding.searchEditText.text.toString()
            filterProperties(query)
        }
    }

    private fun setupFilters() {
        // Setup filter chips
        binding.chipAll?.setOnClickListener { onFilterSelected("All") }
        binding.chipPG?.setOnClickListener { onFilterSelected("PG") }
        binding.chipFlat?.setOnClickListener { onFilterSelected("Flat") }
        binding.chipRoom?.setOnClickListener { onFilterSelected("Room") }
        binding.chipHouse?.setOnClickListener { onFilterSelected("House") }
    }

    private fun onFilterSelected(filter: String) {
        currentFilter = filter
        updateFilterChips(filter)
        applyFilters()
    }

    private fun updateFilterChips(selectedFilter: String) {
        // Reset all chips
        listOf(
            binding.chipAll,
            binding.chipPG,
            binding.chipFlat,
            binding.chipRoom,
            binding.chipHouse
        ).forEach { chip ->
            chip?.isChecked = false
        }

        // Set selected chip
        when (selectedFilter) {
            "All" -> binding.chipAll?.isChecked = true
            "PG" -> binding.chipPG?.isChecked = true
            "Flat" -> binding.chipFlat?.isChecked = true
            "Room" -> binding.chipRoom?.isChecked = true
            "House" -> binding.chipHouse?.isChecked = true
        }
    }

    private fun applyFilters() {
        val searchQuery = binding.searchEditText.text.toString()
        filterProperties(searchQuery)
    }

    private fun showLoading(show: Boolean) {
        if (!isAdded) return
        _binding?.let { b ->
            b.loadingCard?.visibility = if (show) View.VISIBLE else View.GONE
        }
        if (!show) swipeRefreshLayout?.isRefreshing = false
    }

    private fun showEmptyState(show: Boolean) {
        if (!isAdded) return
        _binding?.let { b ->
            b.emptyState?.visibility = if (show) View.VISIBLE else View.GONE
            b.propertyRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    private fun fetchProperties() {
        showLoading(true)
        showEmptyState(false)
        
        // Properties are stored under: Properties/{ownerEmail}/Available/{propertyId}
        // Use a collectionGroup query on the "Available" subcollections so we get all available properties
        firestore.collectionGroup("Available")
            .get()
            .addOnSuccessListener { documents ->
                val properties = documents.mapNotNull { doc ->
                    doc.toObject(Property::class.java)?.apply {
                        // Document id is the property id
                        id = doc.id
                        // Owner document id is the parent of the collection (Properties/{ownerEmail})
                        val ownerDocId = doc.reference.parent?.parent?.id
                        if (!ownerDocId.isNullOrEmpty()) ownerId = ownerDocId

                        // Backward-compat: some older docs might have `address` instead of `location`.
                        if (location.isEmpty() && address.isNotEmpty()) location = address
                        // If createdAt isn't present, try to fallback to timestamp field if used previously
                        if (createdAt == null && timestamp.isNotEmpty()) {
                            // timestamp stored as string in older docs - ignore conversion here (fallback ordering will handle it)
                        }
                    }
                }

                // Sort by createdAt timestamp if available, otherwise keep original order and then by price
                val sortedProperties = properties.sortedWith(
                    compareByDescending<Property> { it.createdAt?.seconds ?: 0L }
                        .thenByDescending { it.price }
                )

                allProperties = sortedProperties
                applyFilters()
                updatePropertyCount(sortedProperties.size)
                showLoading(false)
                
                if (sortedProperties.isEmpty()) {
                    showEmptyState(true)
                }
            }
            .addOnFailureListener { e ->
                showError("Failed to load properties: ${e.message}")
                showLoading(false)
                showEmptyState(true)
            }
    }

    private fun filterProperties(query: String) {
        var filtered = allProperties

        // Apply type filter
        if (currentFilter != "All") {
            filtered = filtered.filter { property ->
                property.type.contains(currentFilter, ignoreCase = true) ||
                property.propertyType.contains(currentFilter, ignoreCase = true)
            }
        }

        // Apply search query
        if (query.isNotEmpty()) {
            filtered = filtered.filter { property ->
                property.title.contains(query, ignoreCase = true) ||
                property.type.contains(query, ignoreCase = true) ||
                property.location.contains(query, ignoreCase = true) ||
                property.address.contains(query, ignoreCase = true) ||
                property.description.contains(query, ignoreCase = true)
            }
        }

        filteredProperties = filtered
        propertyAdapter.updateData(filtered)
        updatePropertyCount(filtered.size)
        showEmptyState(filtered.isEmpty() && allProperties.isNotEmpty())
    }

    private fun updatePropertyCount(count: Int) {
        _binding?.tvPropertyCount?.text = count.toString()
    }

    private fun showError(message: String) {
        if (isAdded) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    // PropertyInteractionListener implementations
    override fun onPropertyClicked(property: Property) {
        PropertyDetailsActivity.start(requireContext(), property.ownerId, property.id)
    }

    override fun onBookmarkClicked(property: Property, position: Int) {
        property.isBookmarked = !property.isBookmarked
        sharedViewModel.toggleBookmark(property)
        
        val message = if (property.isBookmarked) "Added to bookmarks" else "Removed from bookmarks"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onViewDetailsClicked(property: Property) {
        PropertyDetailsActivity.start(requireContext(), property.ownerId, property.id)
    }

    override fun onContactClicked(property: Property) {
        // TODO: Implement contact functionality
        Toast.makeText(context, "Contact feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        swipeRefreshLayout = null
        _binding = null
    }
}

// Enhanced item decoration for better spacing
class PropertyItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        
        outRect.left = spacing
        outRect.right = spacing
        outRect.bottom = spacing
        
        // Add top margin only for first item
        if (position == 0) {
            outRect.top = spacing
        }
    }
}
