package com.example.housinghub

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImagePreviewAdapter(private val imageUris: MutableList<Uri>, 
                         private val onDeleteClick: (Int) -> Unit = {}) :
    RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_preview, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(imageUris[position])
            .into(holder.imageView)
            
        holder.btnDelete.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int = imageUris.size
    
    fun submitList(newList: List<Uri>) {
        imageUris.clear()
        imageUris.addAll(newList)
        notifyDataSetChanged()
    }
}
