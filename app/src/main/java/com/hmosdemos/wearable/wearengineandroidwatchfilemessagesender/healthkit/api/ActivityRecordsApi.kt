package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.api

import android.net.Uri
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.data.models.WorkoutRecord
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.HealthKitConfig
import com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.net.HttpJson
import org.json.JSONArray
import org.json.JSONObject

/**
 * Step 3 of the flow: read workout records.
 *
 * `GET https://health-api.cloud.huawei.com/healthkit/v2/activityRecords` returns the
 * records of one user for one time range. It is the same call for every Huawei watch or
 * band, and also for workouts the user entered in the Huawei Health app by hand.
 */
class ActivityRecordsApi {

    private val http = HttpJson()

    /**
     * Reads every record between [startTimeMillis] and [endTimeMillis]. The range cannot
     * be longer than [HealthKitConfig.MAX_QUERY_RANGE_DAYS] days.
     *
     * [activityTypes] filters by workout type number; an empty list returns all types.
     * When the answer says there is more data, the next page is read with the cursor,
     * up to [MAX_PAGES] pages.
     */
    suspend fun fetchWorkouts(
        accessToken: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        activityTypes: List<Int> = emptyList(),
    ): Result<List<WorkoutRecord>> = runCatching {
        val records = mutableListOf<WorkoutRecord>()
        var cursor: String? = null
        var page = 0

        do {
            val url = buildUrl(startTimeMillis, endTimeMillis, activityTypes, cursor)
            val json = http.getJson(url, accessToken)
            records += readRecords(json)
            cursor = nextCursor(json)
            page++
        } while (cursor != null && page < MAX_PAGES)

        records.sortedByDescending { it.startTime }
    }

    private fun buildUrl(
        startTimeMillis: Long,
        endTimeMillis: Long,
        activityTypes: List<Int>,
        cursor: String?,
    ): String {
        val builder = Uri.parse(HealthKitConfig.ACTIVITY_RECORDS_URL).buildUpon()
        if (cursor != null) {
            // Once there is a cursor, it replaces the time range in the request.
            builder.appendQueryParameter("cursor", cursor)
        } else {
            builder.appendQueryParameter("startTime", startTimeMillis.toString())
            builder.appendQueryParameter("endTime", endTimeMillis.toString())
            activityTypes.forEach { builder.appendQueryParameter("activityType", it.toString()) }
        }
        return builder.build().toString()
    }

    private fun readRecords(json: JSONObject): List<WorkoutRecord> {
        val array = json.optJSONArray("activityRecord") ?: JSONArray()
        return (0 until array.length()).map { parseRecord(array.getJSONObject(it)) }
    }

    private fun nextCursor(json: JSONObject): String? {
        if (!json.optBoolean("hasMoreData", false)) return null
        return json.optString("cursor").takeIf { it.isNotBlank() }
    }

    private fun parseRecord(obj: JSONObject): WorkoutRecord {
        var distanceMeters: Double? = null
        var calorieKcal: Double? = null

        val summary = obj.optJSONObject("activitySummary")?.optJSONArray("dataSummary")
        if (summary != null) {
            for (i in 0 until summary.length()) {
                val entry = summary.getJSONObject(i)
                val typeName = entry.optString("dataTypeName")
                val values = entry.optJSONArray("value") ?: continue
                when {
                    typeName.contains("distance") -> distanceMeters = firstFloatValue(values)
                    typeName.contains("calor") -> calorieKcal = firstFloatValue(values)
                }
            }
        }

        return WorkoutRecord(
            id = obj.optString("id"),
            name = obj.optString("name"),
            startTime = obj.optLong("startTime"),
            endTime = obj.optLong("endTime"),
            activityType = obj.optInt("activityType"),
            activeTimeMillis = obj.optLong("activeTime"),
            timeZone = obj.optString("timeZone"),
            sourceType = if (obj.has("sourceType")) obj.optInt("sourceType") else null,
            distanceMeters = distanceMeters,
            calorieKcal = calorieKcal,
        )
    }

    private fun firstFloatValue(values: JSONArray): Double? {
        if (values.length() == 0) return null
        val first = values.getJSONObject(0)
        return if (first.has("floatValue")) first.optDouble("floatValue") else null
    }

    private companion object {
        /** Stops the loop if the server keeps saying there is more data. */
        const val MAX_PAGES = 5
    }
}
