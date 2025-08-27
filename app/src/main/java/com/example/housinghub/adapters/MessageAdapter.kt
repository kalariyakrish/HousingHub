package com.example.housinghub.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.housinghub.databinding.ItemMessageReceivedBinding
import com.example.housinghub.databinding.ItemMessageSentBinding
import com.example.housinghub.databinding.ItemMessagePropertyBinding
import com.example.housinghub.model.Message

class MessageAdapter(
    private val currentUserEmail: String,
    private val onPropertyClick: (String) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<Message>()
    
    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_PROPERTY = 3
    }

    inner class SentMessageViewHolder(private val binding: ItemMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.tvMessage.text = message.messageText
            binding.tvTime.text = message.getFormattedTime()
            
            // Read status
            binding.ivReadStatus.visibility = if (message.isRead) View.VISIBLE else View.GONE
        }
    }

    inner class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.tvMessage.text = message.messageText
            binding.tvTime.text = message.getFormattedTime()
            binding.tvSenderName.text = message.senderName
            
            // Show sender name for group chats or system messages
            binding.tvSenderName.visibility = if (message.senderType == "system") View.VISIBLE else View.GONE
        }
    }

    inner class PropertyMessageViewHolder(private val binding: ItemMessagePropertyBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.tvTime.text = message.getFormattedTime()
            
            message.propertyData?.let { property ->
                binding.tvPropertyTitle.text = property.propertyTitle
                binding.tvPropertyLocation.text = property.propertyLocation
                binding.tvPropertyPrice.text = "₹${String.format("%.0f", property.propertyPrice)}/month"
                
                // Handle property image if available
                if (property.propertyImageUrl.isNotEmpty()) {
                    // Load image with Glide
                    binding.ivPropertyImage.visibility = View.VISIBLE
                    // You can implement Glide loading here
                } else {
                    binding.ivPropertyImage.visibility = View.GONE
                }
                
                binding.root.setOnClickListener {
                    onPropertyClick(property.propertyId)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.messageType == Message.MESSAGE_TYPE_PROPERTY_LINK -> VIEW_TYPE_PROPERTY
            message.senderEmail == currentUserEmail -> VIEW_TYPE_SENT
            else -> VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ReceivedMessageViewHolder(binding)
            }
            VIEW_TYPE_PROPERTY -> {
                val binding = ItemMessagePropertyBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                PropertyMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
            is PropertyMessageViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
}
