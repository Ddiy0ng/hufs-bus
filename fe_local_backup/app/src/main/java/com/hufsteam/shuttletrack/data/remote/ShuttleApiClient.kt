package com.hufsteam.shuttletrack.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ShuttleApiClient {
    const val BASE_URL: String = "http://13.209.95.60:8080"
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 8_000

    suspend fun getArray(path: String, token: String? = null): JSONArray? = withContext(Dispatchers.IO) {
        runCatching { request("GET", path, token, null)?.let(::JSONArray) }.getOrNull()
    }

    suspend fun getObject(path: String, token: String? = null): JSONObject? = withContext(Dispatchers.IO) {
        runCatching { request("GET", path, token, null)?.let(::JSONObject) }.getOrNull()
    }

    suspend fun postObject(path: String, body: JSONObject, token: String? = null): Boolean = withContext(Dispatchers.IO) {
        runCatching { request("POST", path, token, body.toString()) != null }.getOrDefault(false)
    }

    private fun request(method: String, path: String, token: String?, body: String?): String? {
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val connection = (URL(BASE_URL + normalizedPath).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            token?.takeIf { it.isNotBlank() }?.let { setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) doOutput = true
        }

        return try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }
            }
            val code = connection.responseCode
            if (code !in 200..299) return null
            BufferedReader(connection.inputStream.reader(Charsets.UTF_8)).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
