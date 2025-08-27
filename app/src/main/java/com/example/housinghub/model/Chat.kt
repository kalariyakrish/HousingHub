package com.example.housinghub.model

import com.google.firebase.Timestamp
import java.io.Serializable

data class Chat(
    var id: String = "",
    var tenantEmail: String = "",
    var tenantName: String = "",
    var ownerEmail: String = "",
    var ownerName: String = "",
    var propertyId: String = "",
    var propertyTitle: String = "",
    var propertyLocation: String = "",
    var lastMessage: String = "",
    var lastMessageTimestamp: Timestamp? = null,
    var lastMessageSender: String = "", // "tenant" or "owner"
    var unreadCountTenant: Int = 0,
    var unreadCountOwner: Int = 0,
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null,
    var isActive: Boolean = true
) : Serializable {
    
    companion object {
        const val SENDER_TENANT = "tenant"
        const val SENDER_OWNER = "owner"
        const val COLLECTION_CHATS = "chats"
        const val COLLECTION_MESSAGES = "messages"
    }
    
    fun getChatId(): String {
        return "${tenantEmail}_${ownerEmail}_${propertyId}".replace(".", "_")
    }
    
    fun getFormattedLastMessageTime(): String {
        val timestamp = lastMessageTimestamp?.toDate()
        return if (timestamp != null) {
            val now = System.currentTimeMillis()
            val messageTime = timestamp.time
            val diff = now - messageTime
            
            when {
                diff < 60000 -> "Just now" // Less than 1 minute
                diff < 3600000 -> "${diff / 60000}m ago" // Less than 1 hour
                diff < 86400000 -> "${diff / 3600000}h ago" // Less than 1 day
                diff < 604800000 -> "${diff / 86400000}d ago" // Less than 1 week
                else -> android.text.format.DateFormat.format("MMM dd", timestamp).toString()
            }
        } else {
            ""
        }
    }
}
