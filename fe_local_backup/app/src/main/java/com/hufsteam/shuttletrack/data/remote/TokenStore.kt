package com.hufsteam.shuttletrack.data.remote

import android.content.Context
import android.content.SharedPreferences

object TokenStore {
    private const val PREF_NAME = "hufs_bus_auth"
    private const val KEY_ACCESS_TOKEN = "access_token"

    private var preferences: SharedPreferences? = null

    var accessToken: String? = null
        private set

    fun initialize(context: Context) {
        if (preferences != null) return
        preferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        accessToken = preferences?.getString(KEY_ACCESS_TOKEN, null)
    }

    fun save(token: String?) {
        accessToken = token?.takeIf { it.isNotBlank() }
        preferences?.edit()?.putString(KEY_ACCESS_TOKEN, accessToken)?.apply()
    }

    fun clear() {
        accessToken = null
        preferences?.edit()?.remove(KEY_ACCESS_TOKEN)?.apply()
    }
}
