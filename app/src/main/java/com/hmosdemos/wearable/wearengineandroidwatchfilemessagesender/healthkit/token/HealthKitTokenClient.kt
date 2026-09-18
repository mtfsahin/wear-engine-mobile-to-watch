package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.token

import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.HealthKitConfig
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.net.HttpJson

/**
 * Steps 2 and 4 of the flow: turn an authorization code into tokens, and get a new access
 * token when the old one ends.
 *
 * These two calls are the same for both authorization methods.
 *
 * The client secret is needed here. In a production app this class belongs on your own
 * backend: the app sends it the code, the backend keeps the secret and the tokens.
 */
internal class HealthKitTokenClient(
    private val http: HttpJson = HttpJson(),
) {

    /** Step 2. The code is valid for 5 minutes and can be used once. */
    suspend fun exchangeCode(code: String, redirectUri: String?): Result<TokenResponse> =
        runCatching {
            val form = buildMap {
                put("grant_type", "authorization_code")
                put("code", code)
                put("client_id", HealthKitConfig.clientId)
                put("client_secret", HealthKitConfig.clientSecret)
                if (!redirectUri.isNullOrBlank()) put("redirect_uri", redirectUri)
            }
            TokenResponse.from(http.postForm(HealthKitConfig.TOKEN_URL, form))
        }

    /** Step 4. The refresh token is valid for 180 days. */
    suspend fun refresh(refreshToken: String): Result<TokenResponse> = runCatching {
        val form = mapOf(
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken,
            "client_id" to HealthKitConfig.clientId,
            "client_secret" to HealthKitConfig.clientSecret,
        )
        TokenResponse.from(http.postForm(HealthKitConfig.TOKEN_URL, form))
    }
}
