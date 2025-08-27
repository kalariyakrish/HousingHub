package com.example.housinghub

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.housinghub.SharedViewModel.Viewmodel.SharedViewModel
import com.example.housinghub.databinding.FragmentSearchBinding
import com.example.housinghub.model.Property
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SearchFragment : Fragment(), BookmarkClickListener {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var propertyAdapter: PropertyAdapter
    private lateinit var sharedViewModel: SharedViewModel
    private val firestore = FirebaseFirestore.getInstance()
    private var allProperties = mutableListOf<Property>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        propertyAdapter = PropertyAdapter(sharedViewModel, this)

        binding.searchRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.searchRecyclerView.adapter = propertyAdapter

        fetchPropertiesFromFirestore()

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                val filtered = allProperties.filter {
                    it.title.contains(query, true) || it.location.contains(query, true)
                }
                propertyAdapter.updateData(filtered)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun fetchPropertiesFromFirestore() {
        binding.progressBar.visibility = View.VISIBLE
        
        // Fetch all properties from the Properties collection
        firestore.collection("Properties")
            .get()
            .addOnSuccessListener { documents ->
                val fetchedProperties = documents.mapNotNull { doc ->
                    doc.toObject(Property::class.java)?.apply {
                        id = doc.id  // Set the document ID as property ID
                    }
                }
                allProperties = fetchedProperties.sortedByDescending { it.timestamp }.toMutableList()
                propertyAdapter.updateData(allProperties)
                binding.progressBar.visibility = View.GONE
                
                // Show/hide empty state
                if (allProperties.isEmpty()) {
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.searchRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateText.visibility = View.GONE
                    binding.searchRecyclerView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.emptyStateText.apply {
                    text = "Error loading properties: ${e.message}"
                    visibility = View.VISIBLE
                }
                binding.searchRecyclerView.visibility = View.GONE
            }
    }

    override fun onBookmarkClicked(property: Property, position: Int) {
        property.isBookmarked = !property.isBookmarked
        sharedViewModel.toggleBookmark(property)
        propertyAdapter.notifyItemChanged(position)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
