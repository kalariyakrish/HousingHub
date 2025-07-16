package com.example.housinghub

import com.example.housinghub.model.Property

interface BookmarkClickListener {
    fun onBookmarkClicked(property: Property, position: Int)
}
