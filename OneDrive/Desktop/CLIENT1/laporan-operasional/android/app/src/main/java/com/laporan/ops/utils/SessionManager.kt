package com.laporan.ops.utils

import android.content.Context
import android.content.SharedPreferences
import com.laporan.ops.api.RetrofitClient
import com.laporan.ops.model.User

class SessionManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME    = "laporan_ops_prefs"
        private const val KEY_TOKEN    = "token"
        private const val KEY_USER_ID  = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_NAME     = "name"
        private const val KEY_ROLE     = "role"

        @Volatile private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    fun saveSession(token: String, user: User) {
        prefs.edit().apply {
            putString(KEY_TOKEN,    token)
            putInt(KEY_USER_ID,     user.id)
            putString(KEY_USERNAME, user.username)
            putString(KEY_NAME,     user.name)
            putString(KEY_ROLE,     user.role)
            apply()
        }
        RetrofitClient.setToken(token)
    }

    fun getToken():    String? = prefs.getString(KEY_TOKEN,    null)
    fun getUserId():   Int     = prefs.getInt(KEY_USER_ID,     -1)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getName():     String? = prefs.getString(KEY_NAME,     null)
    fun getRole():     String? = prefs.getString(KEY_ROLE,     null)
    fun isLoggedIn():  Boolean = getToken() != null

    fun initToken() { getToken()?.let { RetrofitClient.setToken(it) } }

    fun clearSession() {
        prefs.edit().clear().apply()
        RetrofitClient.setToken(null)
    }
}
