package com.example.housinghub

import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.example.housinghub.SharedViewModel.Viewmodel.SharedViewModel
import com.example.housinghub.databinding.FragmentHomeBinding
import com.example.housinghub.model.Property

class HomeFragment : Fragment(), BookmarkClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var propertyAdapter: PropertyAdapter
    private lateinit var sharedViewModel: SharedViewModel
    private val firestore = FirebaseFirestore.getInstance()
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    
    // Current property list for search functionality
    private var currentPropertyList: List<Property> = emptyList()
    private var searchJob: Job? = null

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
        fetchProperties()
    }

    private fun setupUI() {
        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        // Setup RecyclerView
        propertyAdapter = PropertyAdapter(sharedViewModel, this) { property ->
            // Open details screen by ownerId and property id
            PropertyDetailsActivity.start(requireContext(), property.ownerId, property.id)
        }
        binding.propertyRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2) // 2 columns grid
            adapter = propertyAdapter
            addItemDecoration(GridSpacingItemDecoration(2, resources.getDimensionPixelSize(R.dimen.grid_spacing), true))
        }

        // Setup SwipeRefreshLayout
        swipeRefreshLayout?.setOnRefreshListener {
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
    }

    private fun showLoading(show: Boolean) {
        if (!isAdded) return
        _binding?.let { b ->
            b.loadingProgress.visibility = if (show) View.VISIBLE else View.GONE
        }
        if (!show) swipeRefreshLayout?.isRefreshing = false
    }

    private fun showEmptyState(show: Boolean) {
        if (!isAdded) return
        _binding?.let { b ->
            b.emptyState.visibility = if (show) View.VISIBLE else View.GONE
            b.propertyRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    private fun fetchProperties() {
        showLoading(true)
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

                propertyAdapter.updateData(sortedProperties)
                showEmptyState(sortedProperties.isEmpty())
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showError("Failed to load properties: ${e.message}")
                showLoading(false)
                showEmptyState(true)
            }
    }

    private fun filterProperties(query: String) {
    val currentList = propertyAdapter.items
        if (query.isEmpty()) {
            propertyAdapter.updateData(currentList)
            return
        }

        val filteredList = currentList.filter { property ->
            property.title.contains(query, ignoreCase = true) ||
            property.type.contains(query, ignoreCase = true) ||
            property.location.contains(query, ignoreCase = true) ||
            property.address.contains(query, ignoreCase = true)
        }
        
        propertyAdapter.updateData(filteredList)
        showEmptyState(filteredList.isEmpty())
    }

    // Extension property for adapter
    private val PropertyAdapter.items: List<Property>
        get() = try {
            this.items
        } catch (e: Exception) {
            emptyList()
        }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onBookmarkClicked(property: Property, position: Int) {
        property.isBookmarked = !property.isBookmarked
        sharedViewModel.toggleBookmark(property)
        propertyAdapter.notifyItemChanged(position)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    swipeRefreshLayout = null
    _binding = null
    }
}

// Add this class for grid spacing
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount
            if (position < spanCount) outRect.top = spacing
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) outRect.top = spacing
        }
    }
}
