package com.hufsteam.shuttletrack.data.remote

import android.content.Context
import android.content.SharedPreferences

object TokenStore {
    private const val PREF_NAME = "hufs_bus_auth"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_EMAIL = "email"
    private const val KEY_ROLE = "role"

    private var preferences: SharedPreferences? = null

    var accessToken: String? = null
        private set
    var email: String? = null
        private set
    var role: String? = null
        private set

    fun initialize(context: Context) {
        if (preferences != null) return
        preferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        accessToken = preferences?.getString(KEY_ACCESS_TOKEN, null)
        email = preferences?.getString(KEY_EMAIL, null)
        role = preferences?.getString(KEY_ROLE, null)
    }

    fun save(token: String?) {
        accessToken = token?.takeIf { it.isNotBlank() }
        preferences?.edit()?.putString(KEY_ACCESS_TOKEN, accessToken)?.apply()
    }

    fun saveSession(token: String?, email: String?, role: String?) {
        accessToken = token?.takeIf { it.isNotBlank() }
        this.email = email?.takeIf { it.isNotBlank() }
        this.role = role?.takeIf { it.isNotBlank() }
        preferences?.edit()
            ?.putString(KEY_ACCESS_TOKEN, accessToken)
            ?.putString(KEY_EMAIL, this.email)
            ?.putString(KEY_ROLE, this.role)
            ?.apply()
    }

    fun clear() {
        accessToken = null
        email = null
        role = null
        preferences?.edit()
            ?.remove(KEY_ACCESS_TOKEN)
            ?.remove(KEY_EMAIL)
            ?.remove(KEY_ROLE)
            ?.apply()
    }
}
