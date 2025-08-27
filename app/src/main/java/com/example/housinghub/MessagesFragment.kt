package com.example.housinghub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.housinghub.databinding.FragmentMessagesBinding

class MessageFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        messageAdapter = MessageAdapter(getDummyMessages())
        notificationAdapter = NotificationAdapter(getDummyNotifications())

        binding.messageRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.messageRecyclerView.adapter = messageAdapter

        binding.tabMessages.setOnClickListener {
            setTab(true)
        }

        binding.tabNotifications.setOnClickListener {
            setTab(false)
        }
    }

    private fun setTab(showMessages: Boolean) {
        binding.tabMessages.setBackgroundResource(
            if (showMessages) R.drawable.tab_selected else R.drawable.tab_unselected
        )
        binding.tabNotifications.setBackgroundResource(
            if (!showMessages) R.drawable.tab_selected else R.drawable.tab_unselected
        )

        binding.messageRecyclerView.adapter = if (showMessages) messageAdapter else notificationAdapter
    }

    private fun getDummyMessages(): List<String> {
        return listOf("Owner: Hello, when will you move?", "Tenant: I’ll visit tomorrow", "Owner: Let me know your budget")
    }

    private fun getDummyNotifications(): List<String> {
        return listOf("Booking confirmed", "New PG listed near you", "Reminder: Rent due tomorrow")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
