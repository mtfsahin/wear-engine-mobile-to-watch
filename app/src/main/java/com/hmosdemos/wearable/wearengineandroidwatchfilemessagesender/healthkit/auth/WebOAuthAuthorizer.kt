package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.auth

import android.app.Activity
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.HealthKitAuthMethod
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.HealthKitConfig
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.HealthKitScopes

/**
 * Method 1: open Huawei's own authorization page in a Custom Tab.
 *
 * No Huawei SDK is involved. The user signs in with a HUAWEI ID on that page, taps Allow,
 * and Huawei sends the browser to [HealthKitConfig.deepLinkUrl] with `code` and `state`.
 *
 * The deep link must be registered as the callback URL of the app in AppGallery Connect,
 * exactly as it is written here.
 */
class WebOAuthAuthorizer : HealthKitAuthorizer {

    override val method = HealthKitAuthMethod.WEB_OAUTH

    override val tokenRedirectUri: String get() = HealthKitConfig.deepLinkUrl

    override fun start(activity: Activity, state: String): Result<Unit> = runCatching {
        val uri = Uri.parse(HealthKitConfig.AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", HealthKitConfig.clientId)
            .appendQueryParameter("redirect_uri", HealthKitConfig.deepLinkUrl)
            .appendQueryParameter("scope", HealthKitScopes.DEFAULT.joinToString(" "))
            .appendQueryParameter("state", state)
            // offline is what makes Huawei return a refresh token in step 2.
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("display", "touch")
            .build()

        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(activity, uri)
    }
}
