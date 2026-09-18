package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Finds the Activity behind a Compose context.
 *
 * Both authorization methods need an Activity: the Custom Tab is opened from one, and the
 * login-free SDK starts its screen with startActivityForResult.
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
