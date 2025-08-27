package com.example.housinghub

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.housinghub.adapters.MessageAdapter
import com.example.housinghub.databinding.ActivityChatBinding
import com.example.housinghub.managers.ChatManager
import com.example.housinghub.model.Chat
import com.example.housinghub.model.Message
import com.example.housinghub.utils.UserSessionManager
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChatActivity"
    }

    private lateinit var binding: ActivityChatBinding
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var chatManager: ChatManager
    private lateinit var userSessionManager: UserSessionManager
    
    private var chatId: String = ""
    private var chatName: String = ""
    private var propertyTitle: String = ""
    private var isOwnerView: Boolean = false
    private var messagesListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        getIntentData()
        setupRecyclerView()
        setupClickListeners()
        loadMessages()
    }

    private fun initializeComponents() {
        chatManager = ChatManager(this)
        userSessionManager = UserSessionManager(this)
        
        val currentUserEmail = userSessionManager.getEmail()
        messageAdapter = MessageAdapter(
            currentUserEmail = currentUserEmail,
            onPropertyClick = { propertyId -> 
                // Navigate to property details
                Toast.makeText(this, "Navigate to property: $propertyId", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun getIntentData() {
        chatId = intent.getStringExtra("chatId") ?: ""
        chatName = intent.getStringExtra("chatName") ?: ""
        propertyTitle = intent.getStringExtra("propertyTitle") ?: ""
        isOwnerView = intent.getBooleanExtra("isOwnerView", false)
        
        if (chatId.isEmpty()) {
            Toast.makeText(this, "Invalid chat", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Update UI
        binding.tvChatName.text = chatName
        binding.tvPropertyTitle.text = propertyTitle
    }

    private fun setupRecyclerView() {
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }
        
        binding.ivSend.setOnClickListener {
            sendMessage()
        }
        
        binding.etMessage.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }

    private fun loadMessages() {
        Log.d(TAG, "Loading messages for chat: $chatId")
        showLoading(true)
        
        // Mark messages as read when entering chat
        lifecycleScope.launch {
            chatManager.markMessagesAsRead(chatId)
        }
        
        // Start real-time listener for messages
        messagesListener = chatManager.listenToChatMessages(
            chatId = chatId,
            onMessagesUpdated = { messages ->
                Log.d(TAG, "Received ${messages.size} messages for chat: $chatId")
                showLoading(false)
                showMessages(messages)
                
                // Auto-scroll to bottom for new messages
                if (messages.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }
            },
            onError = { error ->
                Log.e(TAG, "Error loading messages: ${error.message}", error)
                showLoading(false)
                showError("Failed to load messages: ${error.message}")
            }
        )
    }

    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim()
        if (messageText.isEmpty()) return
        
        Log.d(TAG, "Sending message: $messageText to chat: $chatId")
        binding.etMessage.setText("")
        
        lifecycleScope.launch {
            try {
                val senderType = if (isOwnerView) Chat.SENDER_OWNER else Chat.SENDER_TENANT
                Log.d(TAG, "Sender type: $senderType")
                val result = chatManager.sendMessage(chatId, messageText, senderType)
                
                if (result.isFailure) {
                    Log.e(TAG, "Failed to send message: ${result.exceptionOrNull()?.message}")
                    showError("Failed to send message: ${result.exceptionOrNull()?.message}")
                } else {
                    Log.d(TAG, "Message sent successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message: ${e.message}", e)
                showError("Error sending message: ${e.message}")
            }
        }
    }

    private fun showMessages(messages: List<Message>) {
        messageAdapter.updateMessages(messages)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
    }
}
