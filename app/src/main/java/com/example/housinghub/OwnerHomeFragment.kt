package com.example.housinghub

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.housinghub.databinding.FragmentOwnerHomeBinding
import com.example.housinghub.OwnerAddPropertyActivity
import com.example.housinghub.owner.OwnerChatActivity
import com.example.housinghub.owner.OwnerManagePropertyActivity

class OwnerHomeFragment : Fragment() {

    private var _binding: FragmentOwnerHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var tvTotalProperties: TextView
    private lateinit var tvPendingRequests: TextView
    private lateinit var tvViews: TextView
    private lateinit var btnManageProperty: Button
    private lateinit var btnChats: Button
    private lateinit var ivNotification: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOwnerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        tvTotalProperties = binding.tvTotalProperties
        tvPendingRequests = binding.tvPendingRequests
        tvViews = binding.tvViews
        btnManageProperty = binding.btnManageProperty
        btnChats = binding.btnChats
        ivNotification = binding.ivNotification

        binding.tvGreeting.text = "Welcome, Krish 👋"
        tvTotalProperties.text = "12"
        tvPendingRequests.text = "3"
        tvViews.text = "245"

        binding.btnAddProperty.setOnClickListener {
            val intent = Intent(requireContext(), OwnerAddPropertyActivity::class.java)
            startActivity(intent)
        }

        btnManageProperty.setOnClickListener {
            val intent = Intent(requireContext(), OwnerManagePropertyActivity::class.java)
            startActivity(intent)
        }

        btnChats.setOnClickListener {
            val intent = Intent(requireContext(), OwnerChatActivity::class.java)
            startActivity(intent)
        }

        ivNotification.setOnClickListener {
            // Optional: Implement notification logic here
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
