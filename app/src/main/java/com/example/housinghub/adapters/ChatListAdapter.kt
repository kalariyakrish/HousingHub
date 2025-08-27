package com.example.housinghub.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.housinghub.databinding.ItemChatBinding
import com.example.housinghub.model.Chat

class ChatListAdapter(
    private val onChatClick: (Chat) -> Unit,
    private val isOwnerView: Boolean = false
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    companion object {
        private const val TAG = "ChatListAdapter"
    }

    private val chats = mutableListOf<Chat>()

    inner class ChatViewHolder(private val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: Chat) {
            // Show the other party's name based on current user type
            binding.tvChatName.text = if (isOwnerView) {
                chat.tenantName
            } else {
                chat.ownerName
            }
            
            // Property info
            binding.tvPropertyTitle.text = chat.propertyTitle
            binding.tvPropertyLocation.text = chat.propertyLocation
            
            // Last message
            binding.tvLastMessage.text = chat.lastMessage
            binding.tvMessageTime.text = chat.getFormattedLastMessageTime()
            
            // Unread count
            val unreadCount = if (isOwnerView) chat.unreadCountOwner else chat.unreadCountTenant
            if (unreadCount > 0) {
                binding.tvUnreadCount.visibility = View.VISIBLE
                binding.tvUnreadCount.text = if (unreadCount > 99) "99+" else unreadCount.toString()
                binding.tvLastMessage.setTypeface(binding.tvLastMessage.typeface, android.graphics.Typeface.BOLD)
            } else {
                binding.tvUnreadCount.visibility = View.GONE
                binding.tvLastMessage.setTypeface(binding.tvLastMessage.typeface, android.graphics.Typeface.NORMAL)
            }
            
            // Online indicator (can be enhanced later)
            binding.ivOnlineIndicator.visibility = View.GONE
            
            binding.root.setOnClickListener {
                onChatClick(chat)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int = chats.size

    fun updateChats(newChats: List<Chat>) {
        Log.d(TAG, "Updating adapter with ${newChats.size} chats")
        chats.clear()
        chats.addAll(newChats)
        notifyDataSetChanged()
        
        // Log some details about the chats
        newChats.forEachIndexed { index, chat ->
            Log.d(TAG, "Chat $index: ${chat.id}, last message: ${chat.lastMessage}")
        }
    }

    fun updateChatUnreadCount(chatId: String, newUnreadCount: Int) {
        val position = chats.indexOfFirst { it.id == chatId }
        if (position != -1) {
            if (isOwnerView) {
                chats[position].unreadCountOwner = newUnreadCount
            } else {
                chats[position].unreadCountTenant = newUnreadCount
            }
            notifyItemChanged(position)
        }
    }
}
