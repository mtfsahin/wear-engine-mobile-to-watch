package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit

import android.net.Uri
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.BuildConfig

/**
 * The two ways an app can obtain a Health Kit Cloud authorization code.
 *
 * Both are official and fully supported by Huawei, and both end with the same
 * authorization code, the same token exchange and the same REST API. They differ only in
 * how the consent screen is shown.
 */
enum class HealthKitAuthMethod(val label: String) {
    /** Method 1: the Huawei consent page is opened in a browser / Custom Tab. No SDK. */
    WEB_OAUTH("Web OAuth"),

    /** Method 2: the Health Service Kit login-free authorization SDK shows the screen. */
    LOGIN_FREE_SDK("Login-free SDK"),
}

/** The two solutions offered inside the login-free authorization SDK (Method 2). */
enum class LoginFreeSolution(val label: String) {
    /** Solution 1: HUAWEI ID screen, Huawei Health screen or an HTML5 page, per device. */
    AUTO("Solution 1"),

    /** Solution 2: always the Huawei Health app, which also asks for cloud sync. */
    HEALTH_APP("Solution 2"),
}

/**
 * Configuration for the Huawei Health Kit Cloud integration.
 *
 * Nothing here is hard coded: every value comes from `local.properties` through
 * `BuildConfig` (see app/build.gradle.kts and local.properties.example), so no client id
 * or secret is committed to git.
 */
object HealthKitConfig {

    /** App ID from AppGallery Connect. Used as `client_id` and as the SDK `clientId`. */
    val clientId: String = BuildConfig.HEALTH_KIT_CLIENT_ID

    /**
     * App secret from AppGallery Connect, used for the token exchange.
     *
     * A production app should keep this on its own backend and never ship it in an APK.
     * This demo exchanges the code on device to stay a single module; see the README.
     */
    val clientSecret: String = BuildConfig.HEALTH_KIT_CLIENT_SECRET

    /**
     * The app's own deep link. Method 1 registers it as the OAuth redirect URI; Method 2
     * registers it in AppGallery Connect as the Scheme URL.
     */
    val deepLinkUrl: String = BuildConfig.HEALTH_KIT_DEEP_LINK

    /** Scheme Secret that AppGallery Connect issues for the Scheme URL (Method 2 only). */
    val schemeSecret: String = BuildConfig.HEALTH_KIT_SCHEME_SECRET

    /** Auth method the app starts with. Change it in local.properties or in the UI. */
    val defaultAuthMethod: HealthKitAuthMethod =
        runCatching { HealthKitAuthMethod.valueOf(BuildConfig.HEALTH_KIT_AUTH_METHOD) }
            .getOrDefault(HealthKitAuthMethod.WEB_OAUTH)

    /**
     * Callback URL that Huawei hosts for login-free authorization. It is entered as-is in
     * the AppGallery Connect "Callback URL" field and is not a page you host.
     *
     * Huawei's login-free pages do not document the token request, so it is not confirmed
     * that this value must be sent as `redirect_uri` in step 2. OAuth 2.0 requires the
     * same redirect_uri in both steps, so this is the most likely value. Test it against
     * your own app before you go live, and set [SEND_REDIRECT_URI_FOR_LOGIN_FREE] to
     * false if Huawei rejects the request.
     */
    const val LOGIN_FREE_CALLBACK_URL =
        "https://h5hosting.dbankcdn.com/cch5/healthkit/oauth-h5/oauth-callback.html"

    /** Whether the token request for Method 2 includes `redirect_uri`. See above. */
    const val SEND_REDIRECT_URI_FOR_LOGIN_FREE = true

    const val AUTHORIZE_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/authorize"
    const val TOKEN_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/token"
    const val ACTIVITY_RECORDS_URL =
        "https://health-api.cloud.huawei.com/healthkit/v2/activityRecords"

    /** Days of history the "fetch workouts" action asks for. */
    const val DEFAULT_LOOKBACK_DAYS = 7L

    /** Health Kit Cloud rejects any activityRecords range longer than this. */
    const val MAX_QUERY_RANGE_DAYS = 31L

    /** False when local.properties is missing the values this method needs. */
    fun isConfigured(method: HealthKitAuthMethod): Boolean = when (method) {
        HealthKitAuthMethod.WEB_OAUTH ->
            clientId.isNotBlank() && clientSecret.isNotBlank() && deepLinkUrl.isNotBlank()

        HealthKitAuthMethod.LOGIN_FREE_SDK ->
            clientId.isNotBlank() && deepLinkUrl.isNotBlank() && schemeSecret.isNotBlank()
    }

    /** What is missing, for a message the user can act on. */
    fun missingConfigMessage(method: HealthKitAuthMethod): String {
        val missing = buildList {
            if (clientId.isBlank()) add("healthkit.clientId")
            if (deepLinkUrl.isBlank()) add("healthkit.deepLink")
            if (method == HealthKitAuthMethod.WEB_OAUTH && clientSecret.isBlank()) {
                add("healthkit.clientSecret")
            }
            if (method == HealthKitAuthMethod.LOGIN_FREE_SDK && schemeSecret.isBlank()) {
                add("healthkit.schemeSecret")
            }
        }
        return "Add ${missing.joinToString(", ")} to local.properties (see local.properties.example)."
    }

    /** True when [uri] is this app's own deep link, whatever produced it. */
    fun isDeepLink(uri: Uri?): Boolean {
        if (uri == null) return false
        val expected = Uri.parse(deepLinkUrl)
        return uri.scheme.equals(expected.scheme, ignoreCase = true) &&
            uri.host.equals(expected.host, ignoreCase = true)
    }
}

/**
 * OAuth scopes for "Activity records" data. Each read scope also has a `.write` version;
 * this app only reads, so only the read scopes are requested.
 */
object HealthKitScopes {
    const val OPENID = "openid"
    const val ACTIVITY_RECORD_READ = "https://www.huawei.com/healthkit/activityrecord.read"
    const val ACTIVITY_READ = "https://www.huawei.com/healthkit/activity.read"
    const val LOCATION_READ = "https://www.huawei.com/healthkit/location.read"

    /** Smallest set that returns workout records with their summary values. */
    val DEFAULT = listOf(OPENID, ACTIVITY_RECORD_READ, ACTIVITY_READ)
}
