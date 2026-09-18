package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.token

import org.json.JSONObject

/** The part of Huawei's token answer that this app uses. */
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long,
) {
    companion object {
        fun from(json: JSONObject): TokenResponse = TokenResponse(
            accessToken = json.getString("access_token"),
            refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
            expiresInSeconds = json.optLong("expires_in", DEFAULT_EXPIRY_SECONDS),
        )

        private const val DEFAULT_EXPIRY_SECONDS = 3600L
    }
}
