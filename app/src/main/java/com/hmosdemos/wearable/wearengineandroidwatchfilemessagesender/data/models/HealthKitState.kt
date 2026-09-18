package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.data.models

import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.HealthKitAuthMethod
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.HealthKitConfig
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.LoginFreeSolution

/** UI state of the "Huawei Health Kit, workouts" card. */
data class HealthKitState(
    val authMethod: HealthKitAuthMethod = HealthKitConfig.defaultAuthMethod,
    val loginFreeSolution: LoginFreeSolution = LoginFreeSolution.AUTO,
    val isAuthorized: Boolean = false,
    val isAuthorizing: Boolean = false,
    val isLoadingWorkouts: Boolean = false,
    val workouts: List<WorkoutRecord> = emptyList(),
    val lastSyncedAtMillis: Long? = null,
    val errorMessage: String? = null,
)
