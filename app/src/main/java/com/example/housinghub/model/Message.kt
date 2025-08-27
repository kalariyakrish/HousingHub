package com.example.housinghub.model

import com.google.firebase.Timestamp
import java.io.Serializable

data class Message(
    var id: String = "",
    var chatId: String = "",
    var senderEmail: String = "",
    var senderName: String = "",
    var senderType: String = "", // "tenant" or "owner"
    var messageText: String = "",
    var messageType: String = MESSAGE_TYPE_TEXT, // text, property_link, system
    var timestamp: Timestamp? = null,
    var isRead: Boolean = false,
    var propertyData: PropertyMessageData? = null
) : Serializable {
    
    companion object {
        const val MESSAGE_TYPE_TEXT = "text"
        const val MESSAGE_TYPE_PROPERTY_LINK = "property_link"
        const val MESSAGE_TYPE_SYSTEM = "system"
    }
    
    fun getFormattedTime(): String {
        val messageTime = timestamp?.toDate()
        return if (messageTime != null) {
            android.text.format.DateFormat.format("HH:mm", messageTime).toString()
        } else {
            ""
        }
    }
    
    fun getFormattedDate(): String {
        val messageTime = timestamp?.toDate()
        return if (messageTime != null) {
            android.text.format.DateFormat.format("MMM dd, yyyy", messageTime).toString()
        } else {
            ""
        }
    }
}

data class PropertyMessageData(
    var propertyId: String = "",
    var propertyTitle: String = "",
    var propertyLocation: String = "",
    var propertyPrice: Double = 0.0,
    var propertyImageUrl: String = ""
) : Serializable
