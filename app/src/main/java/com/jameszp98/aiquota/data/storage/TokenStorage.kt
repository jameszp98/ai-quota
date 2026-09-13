package com.jameszp98.aiquota.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/**
 * Secure storage for authentication tokens using EncryptedSharedPreferences.
 * Tokens are encrypted at rest using Android Keystore-backed encryption.
 *
 * IMPORTANT: Never log token values. They provide access to user accounts.
 */
class TokenStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _cursorTokenState = MutableStateFlow(loadCursorTokenState())
    val cursorTokenState: Flow<CursorTokenState> = _cursorTokenState

    private fun loadCursorTokenState(): CursorTokenState {
        val bearer = encryptedPrefs.getString(KEY_CURSOR_BEARER, null)
        val session = encryptedPrefs.getString(KEY_CURSOR_SESSION, null)
        return when {
            bearer != null -> CursorTokenState.HasBearerToken
            session != null -> CursorTokenState.HasSessionToken
            else -> CursorTokenState.NoToken
        }
    }

    // Cursor tokens
    suspend fun saveCursorBearerToken(token: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit()
            .putString(KEY_CURSOR_BEARER, token.trim())
            .remove(KEY_CURSOR_SESSION)
            .apply()
        _cursorTokenState.value = CursorTokenState.HasBearerToken
    }

    suspend fun saveCursorSessionToken(token: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit()
            .putString(KEY_CURSOR_SESSION, token.trim())
            .remove(KEY_CURSOR_BEARER)
            .apply()
        _cursorTokenState.value = CursorTokenState.HasSessionToken
    }

    suspend fun getCursorBearerToken(): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_CURSOR_BEARER, null)
    }

    suspend fun getCursorSessionToken(): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_CURSOR_SESSION, null)
    }

    suspend fun clearCursorTokens() = withContext(Dispatchers.IO) {
        encryptedPrefs.edit()
            .remove(KEY_CURSOR_BEARER)
            .remove(KEY_CURSOR_SESSION)
            .apply()
        _cursorTokenState.value = CursorTokenState.NoToken
    }

    companion object {
        private const val PREFS_FILE_NAME = "ai_quota_secure_prefs"
        private const val KEY_CURSOR_BEARER = "cursor_bearer_token"
        private const val KEY_CURSOR_SESSION = "cursor_session_token"
    }
}

sealed class CursorTokenState {
    data object NoToken : CursorTokenState()
    data object HasBearerToken : CursorTokenState()
    data object HasSessionToken : CursorTokenState()
}
