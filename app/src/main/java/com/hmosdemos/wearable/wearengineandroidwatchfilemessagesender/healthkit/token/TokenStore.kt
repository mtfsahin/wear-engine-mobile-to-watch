package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.token

import android.content.Context

/**
 * Keeps the tokens between app starts, so the user grants permission only once.
 *
 * A production app should store these in EncryptedSharedPreferences, or keep them on its
 * own backend and never place them on the device at all.
 */
class TokenStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        private set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        private set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var expiresAtMillis: Long
        get() = prefs.getLong(KEY_EXPIRES_AT, 0L)
        private set(value) = prefs.edit().putLong(KEY_EXPIRES_AT, value).apply()

    /** True once the user has granted permission at least once. */
    val hasTokens: Boolean
        get() = !refreshToken.isNullOrBlank() || !accessToken.isNullOrBlank()

    /** True while the stored access token can still be used. */
    fun hasValidAccessToken(nowMillis: Long = System.currentTimeMillis()): Boolean =
        !accessToken.isNullOrBlank() && nowMillis < expiresAtMillis - EXPIRY_MARGIN_MS

    fun save(tokens: TokenResponse) {
        accessToken = tokens.accessToken
        expiresAtMillis = System.currentTimeMillis() + tokens.expiresInSeconds * 1000
        // A refresh answer often has no new refresh token, so the old one is kept.
        tokens.refreshToken?.takeIf { it.isNotBlank() }?.let { refreshToken = it }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "healthkit_tokens"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"

        /** Refresh a minute early, so a call cannot start with a token that just died. */
        const val EXPIRY_MARGIN_MS = 60_000L
    }
}
