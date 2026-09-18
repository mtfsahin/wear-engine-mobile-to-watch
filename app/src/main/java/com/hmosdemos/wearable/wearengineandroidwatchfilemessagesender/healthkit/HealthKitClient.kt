package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.data.models.WorkoutRecord
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.api.ActivityRecordsApi
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.auth.AuthorizationRedirect
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.auth.HealthKitAuthorizer
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.auth.LoginFreeAuthorizer
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.auth.WebOAuthAuthorizer
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.token.HealthKitTokenClient
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.token.TokenStore
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * The one entry point the rest of the app uses for Huawei Health Kit Cloud.
 *
 * It runs the four steps of the flow:
 *  1. [startAuthorization] shows the consent screen with the chosen method.
 *  2. [completeAuthorization] turns the code from the deep link into tokens.
 *  3. [fetchWorkouts] reads the workout records.
 *  4. the access token is refreshed here when it ends, without asking the user again.
 *
 * Only step 1 depends on the method. Everything else is shared, which is why the method
 * can be changed at runtime with [useMethod].
 */
class HealthKitClient(
    context: Context,
    private val tokenStore: TokenStore = TokenStore(context),
    private val activityRecordsApi: ActivityRecordsApi = ActivityRecordsApi(),
) {

    private val tokenClient = HealthKitTokenClient()

    private var authorizer: HealthKitAuthorizer = createAuthorizer(
        HealthKitConfig.defaultAuthMethod,
        LoginFreeSolution.AUTO,
    )

    private var pendingState: String? = null

    /** The method the next authorization will use. */
    var authMethod: HealthKitAuthMethod = HealthKitConfig.defaultAuthMethod
        private set

    /** Which login-free solution is used when the method is the SDK. */
    var loginFreeSolution: LoginFreeSolution = LoginFreeSolution.AUTO
        private set

    /** True once the user has granted permission and tokens are stored. */
    val isAuthorized: Boolean
        get() = tokenStore.hasTokens

    /** Switches the method. It has no effect on tokens that are already stored. */
    fun useMethod(
        method: HealthKitAuthMethod,
        solution: LoginFreeSolution = loginFreeSolution,
    ) {
        authMethod = method
        loginFreeSolution = solution
        authorizer = createAuthorizer(method, solution)
    }

    /** Step 1. The result arrives later on the deep link, in [completeAuthorization]. */
    fun startAuthorization(activity: Activity): Result<Unit> {
        if (!HealthKitConfig.isConfigured(authMethod)) {
            return Result.failure(
                IllegalStateException(HealthKitConfig.missingConfigMessage(authMethod))
            )
        }
        val state = UUID.randomUUID().toString()
        pendingState = state
        return authorizer.start(activity, state)
    }

    /** True when this deep link belongs to the Health Kit flow. */
    fun ownsRedirect(uri: Uri?): Boolean = uri != null && authorizer.ownsRedirect(uri)

    /** Step 2. Reads the code from the deep link and exchanges it for tokens. */
    suspend fun completeAuthorization(uri: Uri): Result<Unit> {
        val code = AuthorizationRedirect.parseCode(uri, pendingState)
            .getOrElse { return Result.failure(it) }
        pendingState = null

        return tokenClient.exchangeCode(code, authorizer.tokenRedirectUri)
            .map { tokenStore.save(it) }
    }

    /**
     * The error the login-free SDK reports when it cannot open the deep link at all.
     * Returns null for every other case, including a normal allow or deny.
     */
    fun readAuthorizationLaunchError(requestCode: Int, data: Intent?): String? =
        LoginFreeAuthorizer.readLaunchError(requestCode, data)

    /** Step 3. Reads the workouts of the last [days] days. */
    suspend fun fetchWorkouts(
        days: Long = HealthKitConfig.DEFAULT_LOOKBACK_DAYS,
        activityTypes: List<Int> = emptyList(),
    ): Result<List<WorkoutRecord>> {
        val safeDays = days.coerceIn(1L, HealthKitConfig.MAX_QUERY_RANGE_DAYS)
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(safeDays)

        val accessToken = accessToken().getOrElse { return Result.failure(it) }
        return activityRecordsApi.fetchWorkouts(accessToken, startTime, endTime, activityTypes)
    }

    /** Step 4. A valid access token, refreshed first when the stored one has ended. */
    suspend fun accessToken(): Result<String> {
        if (tokenStore.hasValidAccessToken()) {
            return Result.success(tokenStore.accessToken.orEmpty())
        }

        val refreshToken = tokenStore.refreshToken
            ?: return Result.failure(IllegalStateException("Connect a Huawei Health account first."))

        return tokenClient.refresh(refreshToken)
            .map { tokens ->
                tokenStore.save(tokens)
                tokens.accessToken
            }
    }

    /** Forgets the tokens. The user has to grant permission again after this. */
    fun signOut() {
        tokenStore.clear()
        pendingState = null
    }

    private fun createAuthorizer(
        method: HealthKitAuthMethod,
        solution: LoginFreeSolution,
    ): HealthKitAuthorizer = when (method) {
        HealthKitAuthMethod.WEB_OAUTH -> WebOAuthAuthorizer()
        HealthKitAuthMethod.LOGIN_FREE_SDK -> LoginFreeAuthorizer(solution)
    }
}
