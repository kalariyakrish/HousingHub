package com.example.housinghub.utils

import android.content.Context
import android.content.SharedPreferences

class UserSessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)

    fun saveUserData(name: String, email: String, phone: String) {
        prefs.edit().apply {
            putString("name", name)
            putString("email", email)
            putString("phone", phone)
            apply()
        }
    }

    fun getFullName(): String = prefs.getString("name", "") ?: ""
    fun getEmail(): String = prefs.getString("email", "") ?: ""
    fun getPhone(): String = prefs.getString("phone", "") ?: ""

    fun clearUserSession() {
        prefs.edit().clear().apply()
    }
}
