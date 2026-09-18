package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.auth

import android.net.Uri

/**
 * Reads the authorization code out of the deep link.
 *
 * Both methods answer on the same deep link, and both can answer with an error instead of
 * a code, so both go through this one parser.
 */
object AuthorizationRedirect {

    fun parseCode(uri: Uri, expectedState: String?): Result<String> {
        // Method 1 reports a refusal as error=access_denied, the SDK as errorCode.
        val error = uri.getQueryParameter("error") ?: uri.getQueryParameter("errorCode")
        if (!error.isNullOrBlank()) {
            val description = uri.getQueryParameter("error_description")
                ?: uri.getQueryParameter("errorMessage")
            val suffix = if (description.isNullOrBlank()) "" else ": $description"
            return Result.failure(AuthorizationDeniedException("$error$suffix"))
        }

        val code = uri.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            return Result.failure(
                AuthorizationDeniedException("The deep link carried no authorization code.")
            )
        }

        val returnedState = uri.getQueryParameter("state")
        if (!expectedState.isNullOrBlank() && returnedState != expectedState) {
            return Result.failure(
                AuthorizationDeniedException("The state value did not match the request.")
            )
        }

        return Result.success(code)
    }
}

/** The user said no, or Huawei refused the request. */
class AuthorizationDeniedException(message: String) : Exception(message)
