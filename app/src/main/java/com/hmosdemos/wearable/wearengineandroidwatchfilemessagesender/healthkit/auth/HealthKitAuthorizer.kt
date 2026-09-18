package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.auth

import android.app.Activity
import android.net.Uri
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.HealthKitAuthMethod
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.HealthKitConfig

/**
 * One way of performing step 1 of the flow: show the consent screen and bring back an
 * authorization code.
 *
 * Both implementations end the same way, with the code arriving on this app's deep link,
 * so everything after step 1 is shared.
 */
interface HealthKitAuthorizer {

    /** Which of the two documented methods this authorizer implements. */
    val method: HealthKitAuthMethod

    /**
     * Shows the consent screen. [state] is a random string that Huawei returns unchanged
     * with the code, so the app can detect a response it did not ask for.
     */
    fun start(activity: Activity, state: String): Result<Unit>

    /** True when [uri] is the deep link this authorizer is waiting for. */
    fun ownsRedirect(uri: Uri): Boolean = HealthKitConfig.isDeepLink(uri)

    /** The `redirect_uri` value to send in the token request, or null to send none. */
    val tokenRedirectUri: String?
}
