package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.data.models

import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.WorkoutType

/**
 * One exercise record from `GET /healthkit/v2/activityRecords`.
 *
 * Only the fields this app shows are read. The answer carries more, for example the full
 * `activitySummary.dataSummary` list.
 */
data class WorkoutRecord(
    val id: String,
    val name: String,
    val startTime: Long,
    val endTime: Long,
    val activityType: Int,
    val activeTimeMillis: Long,
    val timeZone: String,
    val sourceType: Int? = null,
    val distanceMeters: Double? = null,
    val calorieKcal: Double? = null,
) {
    val activityTypeName: String
        get() = WorkoutType.displayName(activityType)

    val durationMinutes: Long
        get() = activeTimeMillis / 1000 / 60
}
