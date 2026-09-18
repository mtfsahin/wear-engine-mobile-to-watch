package com.hmosdemos.wearable.wearengineandroidwatchfilemessagesender.healthkit.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * The small amount of HTTP this integration needs: one form POST for the token endpoints
 * and one JSON GET for the REST API. Kept in one place so the callers stay readable.
 *
 * Swap this class for Retrofit or Ktor if the app already uses one of them.
 */
internal class HttpJson(
    private val connectTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
    private val readTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
) {

    suspend fun postForm(url: String, form: Map<String, String>): JSONObject =
        withContext(Dispatchers.IO) {
            val body = form.entries.joinToString("&") { (k, v) -> "${encode(k)}=${encode(v)}" }
            val connection = open(url, "POST").apply {
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            connection.use {
                it.outputStream.use { out -> out.write(body.toByteArray(Charsets.UTF_8)) }
                it.readJson(url)
            }
        }

    suspend fun getJson(url: String, accessToken: String): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = open(url, "GET").apply {
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            connection.use { it.readJson(url) }
        }

    private fun open(url: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
        }

    private fun HttpURLConnection.readJson(url: String): JSONObject {
        val status = responseCode
        val stream = if (status in 200..299) inputStream else errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (status !in 200..299) {
            throw HealthKitHttpException(status, text, url)
        }
        return JSONObject(text)
    }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T =
        try {
            block(this)
        } finally {
            disconnect()
        }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 15_000
    }
}

/** A non 2xx answer from Huawei, with the body kept so the error code stays visible. */
internal class HealthKitHttpException(
    val statusCode: Int,
    val body: String,
    url: String,
) : Exception("HTTP $statusCode from $url: ${body.take(500)}")
