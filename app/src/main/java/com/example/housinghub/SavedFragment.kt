package com.example.housinghub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.housinghub.SharedViewModel.Viewmodel.SharedViewModel
import com.example.housinghub.adapters.EnhancedPropertyAdapter
import com.example.housinghub.adapters.PropertyInteractionListener
import com.example.housinghub.databinding.FragmentSavedBinding
import com.example.housinghub.model.Property

class SavedFragment : Fragment(), PropertyInteractionListener {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var propertyAdapter: EnhancedPropertyAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupRefreshLayout()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupViewModel() {
        sharedViewModel = ViewModelProvider(requireActivity(), ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application))[SharedViewModel::class.java]
    }

    private fun setupRecyclerView() {
        propertyAdapter = EnhancedPropertyAdapter(sharedViewModel, this)
        binding.savedRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = propertyAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupRefreshLayout() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshBookmarks()
        }
        
        binding.swipeRefreshLayout.setColorSchemeColors(
            requireContext().getColor(R.color.primary_blue)
        )
    }

    private fun setupClickListeners() {
        binding.refreshButton.setOnClickListener {
            refreshBookmarks()
        }

        binding.explorePropertiesButton.setOnClickListener {
            // Navigate to home tab
            val homePageActivity = requireActivity() as? HomePageActivity
            homePageActivity?.binding?.bottomNavigation?.selectedItemId = R.id.nav_home
        }
    }

    private fun observeViewModel() {
        // Observe bookmarked properties
        sharedViewModel.bookmarkedProperties.observe(viewLifecycleOwner) { savedList ->
            updateUI(savedList)
        }

        // Observe loading state
        sharedViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        // Observe error messages
        sharedViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                sharedViewModel.clearErrorMessage()
            }
        }
    }

    private fun updateUI(savedList: List<Property>) {
        if (savedList.isEmpty()) {
            showEmptyState()
        } else {
            showPropertiesList(savedList)
        }
        
        // Update count text
        val countText = when (savedList.size) {
            0 -> "No properties saved"
            1 -> "1 property saved"
            else -> "${savedList.size} properties saved"
        }
        binding.savedCountText.text = countText
    }

    private fun showEmptyState() {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.swipeRefreshLayout.visibility = View.GONE
    }

    private fun showPropertiesList(properties: List<Property>) {
        binding.emptyStateLayout.visibility = View.GONE
        binding.swipeRefreshLayout.visibility = View.VISIBLE
        propertyAdapter.updateData(properties)
    }

    private fun refreshBookmarks() {
        sharedViewModel.loadBookmarkedProperties()
    }

    // PropertyInteractionListener implementations
    override fun onPropertyClicked(property: Property) {
        PropertyDetailsActivity.start(requireContext(), property.ownerId, property.id)
    }

    override fun onBookmarkClicked(property: Property, position: Int) {
        sharedViewModel.toggleBookmark(property) { isBookmarked ->
            val message = if (isBookmarked) {
                "Added to bookmarks"
            } else {
                "Removed from bookmarks"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewDetailsClicked(property: Property) {
        PropertyDetailsActivity.start(requireContext(), property.ownerId, property.id)
    }

    override fun onContactClicked(property: Property) {
        // TODO: Implement contact functionality
        Toast.makeText(requireContext(), "Contact feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh bookmarks when fragment becomes visible
        refreshBookmarks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
