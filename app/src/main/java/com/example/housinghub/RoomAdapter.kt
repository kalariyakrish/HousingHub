package com.example.housinghub

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.housinghub.databinding.ItemPropertyBinding
import com.example.housinghub.model.Room

class RoomAdapter(private var roomList: List<Room>) :
    RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(val binding: ItemPropertyBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val binding = ItemPropertyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = roomList[position]
        with(holder.binding) {
            titleText.text = room.title
            locationText.text = room.description // or location if you have it
            priceText.text = "₹${room.price}/month"
            Glide.with(propertyImage.context)
                .load(room.imageUrl)
                .into(propertyImage)
        }
    }

    override fun getItemCount(): Int = roomList.size

    fun updateData(newRooms: List<Room>) {
        roomList = newRooms
        notifyDataSetChanged()
    }
}
