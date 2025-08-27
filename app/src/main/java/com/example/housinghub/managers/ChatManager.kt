package com.example.housinghub.managers

import android.content.Context
import android.util.Log
import com.example.housinghub.model.Chat
import com.example.housinghub.model.Message
import com.example.housinghub.model.PropertyMessageData
import com.example.housinghub.utils.UserSessionManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ChatManager(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val userSessionManager = UserSessionManager(context)
    
    companion object {
        private const val TAG = "ChatManager"
        private const val COLLECTION_CHATS = "chats"
        private const val COLLECTION_MESSAGES = "messages"
    }
    
    /**
     * Create or get existing chat between tenant and owner for a property
     */
    suspend fun createOrGetChat(
        tenantEmail: String,
        tenantName: String,
        ownerEmail: String,
        ownerName: String,
        propertyId: String,
        propertyTitle: String,
        propertyLocation: String
    ): Result<Chat> {
        return try {
            val chatId = "${tenantEmail}_${ownerEmail}_${propertyId}".replace(".", "_")
            
            // Check if chat already exists
            val existingChatDoc = firestore.collection(COLLECTION_CHATS)
                .document(chatId)
                .get()
                .await()
            
            if (existingChatDoc.exists()) {
                val existingChat = existingChatDoc.toObject(Chat::class.java)?.apply {
                    id = existingChatDoc.id
                }
                Log.d(TAG, "Found existing chat: $chatId")
                Result.success(existingChat!!)
            } else {
                // Create new chat
                val newChat = Chat(
                    id = chatId,
                    tenantEmail = tenantEmail,
                    tenantName = tenantName,
                    ownerEmail = ownerEmail,
                    ownerName = ownerName,
                    propertyId = propertyId,
                    propertyTitle = propertyTitle,
                    propertyLocation = propertyLocation,
                    lastMessage = "Chat started",
                    lastMessageTimestamp = Timestamp.now(),
                    lastMessageSender = Chat.SENDER_TENANT,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
                
                firestore.collection(COLLECTION_CHATS)
                    .document(chatId)
                    .set(newChat)
                    .await()
                
                // Send system message about property
                sendPropertyLinkMessage(newChat, propertyId, propertyTitle, propertyLocation, 0.0, "")
                
                Log.d(TAG, "Created new chat: $chatId")
                Result.success(newChat)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating/getting chat: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Send a text message
     */
    suspend fun sendMessage(
        chatId: String,
        messageText: String,
        senderType: String
    ): Result<Message> {
        return try {
            val userEmail = userSessionManager.getEmail()
            val userName = userSessionManager.getFullName()
            
            val messageId = firestore.collection(COLLECTION_CHATS)
                .document(chatId)
                .collection(COLLECTION_MESSAGES)
                .document().id
            
            val message = Message(
                id = messageId,
                chatId = chatId,
                senderEmail = userEmail,
                senderName = userName,
                senderType = senderType,
                messageText = messageText,
                messageType = Message.MESSAGE_TYPE_TEXT,
                timestamp = Timestamp.now(),
                isRead = false
            )
            
            // Save message
            firestore.collection(COLLECTION_CHATS)
                .document(chatId)
                .collection(COLLECTION_MESSAGES)
                .document(messageId)
                .set(message)
                .await()
            
            // Update chat with last message
            updateChatLastMessage(chatId, messageText, senderType)
            
            Log.d(TAG, "Message sent successfully: $messageId")
            Result.success(message)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Send property link message
     */
    private suspend fun sendPropertyLinkMessage(
        chat: Chat,
        propertyId: String,
        propertyTitle: String,
        propertyLocation: String,
        propertyPrice: Double,
        propertyImageUrl: String
    ): Result<Message> {
        return try {
            val messageId = firestore.collection(COLLECTION_CHATS)
                .document(chat.id)
                .collection(COLLECTION_MESSAGES)
                .document().id
            
            val propertyData = PropertyMessageData(
                propertyId = propertyId,
                propertyTitle = propertyTitle,
                propertyLocation = propertyLocation,
                propertyPrice = propertyPrice,
                propertyImageUrl = propertyImageUrl
            )
            
            val message = Message(
                id = messageId,
                chatId = chat.id,
                senderEmail = "system",
                senderName = "HousingHub",
                senderType = "system",
                messageText = "Property: $propertyTitle",
                messageType = Message.MESSAGE_TYPE_PROPERTY_LINK,
                timestamp = Timestamp.now(),
                isRead = false,
                propertyData = propertyData
            )
            
            firestore.collection(COLLECTION_CHATS)
                .document(chat.id)
                .collection(COLLECTION_MESSAGES)
                .document(messageId)
                .set(message)
                .await()
            
            Log.d(TAG, "Property link message sent: $messageId")
            Result.success(message)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending property link message: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update chat's last message info
     */
    private suspend fun updateChatLastMessage(
        chatId: String,
        lastMessage: String,
        senderType: String
    ) {
        try {
            val unreadField = if (senderType == Chat.SENDER_TENANT) "unreadCountOwner" else "unreadCountTenant"
            
            val updates = mapOf(
                "lastMessage" to lastMessage,
                "lastMessageTimestamp" to Timestamp.now(),
                "lastMessageSender" to senderType,
                "updatedAt" to Timestamp.now(),
                unreadField to com.google.firebase.firestore.FieldValue.increment(1)
            )
            
            firestore.collection(COLLECTION_CHATS)
                .document(chatId)
                .update(updates)
                .await()
                
        } catch (e: Exception) {
            Log.e(TAG, "Error updating chat last message: ${e.message}", e)
        }
    }
    
    /**
     * Get chats for current user
     */
    suspend fun getUserChats(): Result<List<Chat>> {
        return try {
            val userEmail = userSessionManager.getEmail()
            val userType = userSessionManager.getUserType()
            
            if (userEmail.isEmpty()) {
                return Result.failure(Exception("User not logged in"))
            }
            
            val query = if (userType == "tenant") {
                firestore.collection(COLLECTION_CHATS)
                    .whereEqualTo("tenantEmail", userEmail)
            } else {
                firestore.collection(COLLECTION_CHATS)
                    .whereEqualTo("ownerEmail", userEmail)
            }
            
            val querySnapshot = query
                .get()
                .await()
            
            val chats = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Chat::class.java)?.apply {
                    id = doc.id
                }
            }.sortedByDescending { it.updatedAt?.toDate()?.time ?: 0 }
            
            Log.d(TAG, "Retrieved ${chats.size} chats for user: $userEmail")
            Result.success(chats)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving chats: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get messages for a chat
     */
    suspend fun getChatMessages(chatId: String): Result<List<Message>> {
        return try {
            val querySnapshot = firestore.collection(COLLECTION_CHATS)
                .document(chatId)
                .collection(COLLECTION_MESSAGES)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val messages = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Message::class.java)?.apply {
                    id = doc.id
                }
            }
            
            Log.d(TAG, "Retrieved ${messages.size} messages for chat: $chatId")
            Result.success(messages)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving messages: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Listen to chat messages in real-time
     */
    fun listenToChatMessages(
        chatId: String,
        onMessagesUpdated: (List<Message>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        Log.d(TAG, "Setting up message listener for chat: $chatId")
        
        return firestore.collection(COLLECTION_CHATS)
            .document(chatId)
            .collection(COLLECTION_MESSAGES)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to messages: ${error.message}", error)
                    onError(error)
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Message::class.java)?.apply {
                            id = doc.id
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message document: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                Log.d(TAG, "Message listener update: found ${messages.size} messages for chat $chatId")
                onMessagesUpdated(messages)
            }
    }
    
    /**
     * Listen to user chats in real-time
     */
    fun listenToUserChats(
        onChatsUpdated: (List<Chat>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration? {
        val userEmail = userSessionManager.getEmail()
        val userType = userSessionManager.getUserType()
        
        Log.d(TAG, "Setting up chat listener for user: $userEmail, type: $userType")
        
        if (userEmail.isEmpty()) {
            Log.e(TAG, "User email is empty")
            onError(Exception("User not logged in"))
            return null
        }
        
        val query = if (userType == "tenant") {
            firestore.collection(COLLECTION_CHATS)
                .whereEqualTo("tenantEmail", userEmail)
        } else {
            firestore.collection(COLLECTION_CHATS)
                .whereEqualTo("ownerEmail", userEmail)
        }
        
        return query
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to chats: ${error.message}", error)
                    onError(error)
                    return@addSnapshotListener
                }
                
                val chats = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Chat::class.java)?.apply {
                            id = doc.id
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing chat document: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                // Sort by updatedAt in memory to avoid index requirement
                val sortedChats = chats.sortedByDescending { it.updatedAt?.toDate()?.time ?: 0 }
                
                Log.d(TAG, "Chat listener update: found ${sortedChats.size} chats for $userEmail")
                onChatsUpdated(sortedChats)
            }
    }
    
    /**
     * Mark messages as read
     */
    suspend fun markMessagesAsRead(chatId: String): Result<Unit> {
        return try {
            val userEmail = userSessionManager.getEmail()
            val userType = userSessionManager.getUserType()
            
            // Mark unread messages as read
            val messagesSnapshot = firestore.collection(COLLECTION_CHATS)
                .document(chatId)
                .collection(COLLECTION_MESSAGES)
                .whereEqualTo("isRead", false)
                .whereNotEqualTo("senderEmail", userEmail)
                .get()
                .await()
            
            val batch = firestore.batch()
            
            messagesSnapshot.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            
            // Reset unread count for current user
            val unreadField = if (userType == "tenant") "unreadCountTenant" else "unreadCountOwner"
            batch.update(
                firestore.collection(COLLECTION_CHATS).document(chatId),
                unreadField, 0
            )
            
            batch.commit().await()
            
            Log.d(TAG, "Messages marked as read for chat: $chatId")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read: ${e.message}", e)
            Result.failure(e)
        }
    }
}
