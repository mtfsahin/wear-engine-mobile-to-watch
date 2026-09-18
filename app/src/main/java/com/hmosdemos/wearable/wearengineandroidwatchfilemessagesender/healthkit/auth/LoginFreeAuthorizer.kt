package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.auth

import android.app.Activity
import android.content.Intent
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.HealthKitAuthMethod
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.HealthKitConfig
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.HealthKitScopes
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.LoginFreeSolution
import com.huawei.hms.hihealth.HuaweiHiHealthLite

/**
 * Method 2: let the Health Service Kit login-free authorization SDK show the screen.
 *
 * The SDK picks the screen that fits the phone: the HUAWEI ID screen, the Huawei Health
 * app, or a web page that Huawei hosts. When the user is already signed in with a HUAWEI
 * ID, no sign-in step is shown, which is what "login-free" means. The code comes back on
 * this app's own deep link, the same as in Method 1.
 *
 * Setup in AppGallery Connect:
 *  - Callback URL: [HealthKitConfig.LOGIN_FREE_CALLBACK_URL], entered exactly as it is.
 *  - Scheme URL: this app's deep link, which also gives you the Scheme Secret.
 *  - Others: apply for the login-free authorization permission for your regions.
 *
 * This is the only file that touches the Huawei health SDK. If your SDK version places
 * these classes in another package, only the import above has to change.
 */
class LoginFreeAuthorizer(
    private val solution: LoginFreeSolution,
) : HealthKitAuthorizer {

    override val method = HealthKitAuthMethod.LOGIN_FREE_SDK

    override val tokenRedirectUri: String?
        get() = if (HealthKitConfig.SEND_REDIRECT_URI_FOR_LOGIN_FREE) {
            HealthKitConfig.LOGIN_FREE_CALLBACK_URL
        } else {
            null
        }

    override fun start(activity: Activity, state: String): Result<Unit> = runCatching {
        val controller = HuaweiHiHealthLite.getLoginFreeAuthController(activity)
        val scopes = HealthKitScopes.DEFAULT.toTypedArray()

        val intent = when (solution) {
            LoginFreeSolution.AUTO -> controller.requestLoginFreeAuthorizationIntent(
                scopes,
                state,
                HealthKitConfig.clientId,
                HealthKitConfig.deepLinkUrl,
                HealthKitConfig.schemeSecret,
            )

            LoginFreeSolution.HEALTH_APP -> controller.requestAuthorizationWithPrivacyIntent(
                scopes,
                state,
                HealthKitConfig.clientId,
                HealthKitConfig.deepLinkUrl,
                HealthKitConfig.schemeSecret,
            )
        }

        activity.startActivityForResult(intent, REQUEST_CODE)
    }.recoverCatching { error ->
        // A missing SDK shows up as a linkage error, which is not an Exception.
        if (error is LinkageError) {
            throw IllegalStateException(
                "Huawei health SDK not found. Add the dependency and the Huawei Maven " +
                    "repository, or use the web OAuth method.",
                error,
            )
        }
        throw error
    }

    companion object {
        /** Request code for the SDK screen. Any value works; it just has to be stable. */
        const val REQUEST_CODE = 2000

        /**
         * Reads the launch error the SDK reports through `onActivityResult`.
         *
         * This path only reports that the deep link itself could not be opened, which
         * means the Scheme URL in AppGallery Connect and the one in AndroidManifest.xml
         * are not the same. A normal allow or deny arrives on the deep link instead.
         */
        fun readLaunchError(requestCode: Int, data: Intent?): String? {
            if (requestCode != REQUEST_CODE || data == null) return null
            val result = data.getIntExtra(EXTRA_RESULT, 0)
            if (result == 0) return null
            val message = data.getStringExtra(EXTRA_MESSAGE).orEmpty()
            return "Deep link failed ($result): $message. Check that the Scheme URL in " +
                "AppGallery Connect matches the intent filter in AndroidManifest.xml."
        }

        private const val EXTRA_RESULT = "DEEPLINK_RESULT"
        private const val EXTRA_MESSAGE = "DEEPLINK_MSG"
    }
}
