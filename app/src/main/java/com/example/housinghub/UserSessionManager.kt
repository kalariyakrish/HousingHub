package com.example.housinghub.utils

import android.content.Context
import android.content.SharedPreferences

class UserSessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)

    fun saveUserData(name: String, email: String, phone: String, userType: String = "tenant") {
        prefs.edit().apply {
            putString("name", name)
            putString("email", email)
            putString("phone", phone)
            putString("userType", userType)
            apply()
        }
    }

    fun saveUserName(name: String) {
        prefs.edit().putString("name", name).apply()
    }

    fun getFullName(): String = prefs.getString("name", "") ?: ""
    fun getEmail(): String = prefs.getString("email", "") ?: ""
    fun getPhone(): String = prefs.getString("phone", "") ?: ""
    fun getUserType(): String = prefs.getString("userType", "tenant") ?: "tenant"

    fun clearUserSession() {
        prefs.edit().clear().apply()
    }

    fun clearSession() {
        clearUserSession()
    }
}
