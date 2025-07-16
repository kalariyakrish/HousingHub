package com.example.housinghub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.housinghub.SharedViewModel.Viewmodel.SharedViewModel
import com.example.housinghub.databinding.FragmentSavedBinding


class SavedFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var propertyAdapter: PropertyAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        propertyAdapter = PropertyAdapter()
        binding.savedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.savedRecyclerView.adapter = propertyAdapter

        sharedViewModel.bookmarkedProperties.observe(viewLifecycleOwner, Observer { savedList ->
            if (savedList.isEmpty()) {
                binding.emptyText.visibility = View.VISIBLE
                binding.savedRecyclerView.visibility = View.GONE
            } else {
                binding.emptyText.visibility = View.GONE
                binding.savedRecyclerView.visibility = View.VISIBLE
                propertyAdapter.updateData(savedList)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
