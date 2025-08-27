package com.example.housinghub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.housinghub.OwnerAddPropertyActivity
import com.example.housinghub.databinding.FragmentOwnerHomeBinding
import com.example.housinghub.owner.OwnerChatActivity
import com.example.housinghub.OwnerManagePropertyActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OwnerHomeFragment : Fragment() {

    private var _binding: FragmentOwnerHomeBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOwnerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get current user and fetch their name
        fetchUserNameAndUpdateUI()

        setupClickListeners()
        loadStatistics()
    }

    private fun fetchUserNameAndUpdateUI() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // First try to get from Firestore owners collection
            db.collection("owners")
                .document(currentUser.email ?: "")
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: document.getString("fullName")
                        if (!name.isNullOrEmpty()) {
                            binding.tvGreeting.text = "Welcome, $name "
                        } else {
                            // Fallback to display email if name is not found
                            val displayName = currentUser.displayName ?: currentUser.email?.substringBefore("@")
                            binding.tvGreeting.text = "Welcome, $displayName "
                        }
                    } else {
                        // If document doesn't exist, use Firebase Auth display name or email
                        val displayName = currentUser.displayName ?: currentUser.email?.substringBefore("@")
                        binding.tvGreeting.text = "Welcome, $displayName "
                    }
                }
                .addOnFailureListener {
                    // In case of any error, use Firebase Auth display name or email
                    val displayName = currentUser.displayName ?: currentUser.email?.substringBefore("@")
                    binding.tvGreeting.text = "Welcome, $displayName "
                }
        }
    }

    private fun setupClickListeners() {
        binding.btnAddProperty.setOnClickListener {
            startActivity(Intent(requireContext(), OwnerAddPropertyActivity::class.java))
        }

        binding.btnManageProperty.setOnClickListener {
            startActivity(Intent(requireContext(), OwnerManagePropertyActivity::class.java))
        }

        binding.btnChats.setOnClickListener {
            startActivity(Intent(requireContext(), OwnerChatActivity::class.java))
        }
    }

    private fun loadStatistics() {
        val currentUser = auth.currentUser?.email ?: return

        // Get total properties count
        db.collection("Properties")
            .whereEqualTo("ownerId", currentUser)
            .get()
            .addOnSuccessListener { result ->
                binding.tvTotalProperties.text = result.size().toString()
            }

        // Get pending requests count
        db.collection("requests")
            .whereEqualTo("ownerId", currentUser)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                binding.tvPendingRequests.text = result.size().toString()
            }

        // Get total views
        db.collection("propertyViews")
            .whereEqualTo("ownerId", currentUser)
            .get()
            .addOnSuccessListener { result ->
                binding.tvViews.text = result.size().toString()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
