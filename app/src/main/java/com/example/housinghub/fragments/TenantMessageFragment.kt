package com.example.housinghub.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.housinghub.ChatActivity
import com.example.housinghub.adapters.ChatListAdapter
import com.example.housinghub.databinding.FragmentMessagesBinding
import com.example.housinghub.managers.ChatManager
import com.example.housinghub.model.Chat
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class TenantMessageFragment : Fragment() {

    companion object {
        private const val TAG = "TenantMessageFragment"
    }

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatListAdapter: ChatListAdapter
    private lateinit var chatManager: ChatManager
    private var chatsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeComponents()
        setupRecyclerView()
        loadChats()
    }

    private fun initializeComponents() {
        chatManager = ChatManager(requireContext())
        
        chatListAdapter = ChatListAdapter(
            onChatClick = { chat -> openChatActivity(chat) },
            isOwnerView = false
        )
    }

    private fun setupRecyclerView() {
        binding.rvChats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatListAdapter
        }
    }

    private fun loadChats() {
        Log.d(TAG, "Loading chats for tenant")
        showLoading(true)
        
        // Start real-time listener for chats
        chatsListener = chatManager.listenToUserChats(
            onChatsUpdated = { chats ->
                Log.d(TAG, "Received ${chats.size} chats")
                showLoading(false)
                if (chats.isEmpty()) {
                    Log.d(TAG, "No chats found, showing empty state")
                    showEmptyState()
                } else {
                    Log.d(TAG, "Showing ${chats.size} chats")
                    showChats(chats)
                }
            },
            onError = { error ->
                Log.e(TAG, "Error loading chats: ${error.message}", error)
                showLoading(false)
                showError("Failed to load chats: ${error.message}")
                showEmptyState()
            }
        )
    }

    private fun showChats(chats: List<Chat>) {
        Log.d(TAG, "Displaying ${chats.size} chats in RecyclerView")
        binding.rvChats.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE
        chatListAdapter.updateChats(chats)
    }

    private fun showEmptyState() {
        Log.d(TAG, "Showing empty state")
        binding.rvChats.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun openChatActivity(chat: Chat) {
        val intent = Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra("chatId", chat.id)
        intent.putExtra("chatName", chat.ownerName)
        intent.putExtra("propertyTitle", chat.propertyTitle)
        intent.putExtra("isOwnerView", false)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatsListener?.remove()
        _binding = null
    }
}
