package com.faisal.strategygame.data

import com.faisal.strategygame.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

class GameServer(private val baseUrl: String = BuildConfig.SERVER_BASE_URL) {
    fun health(): Result<String> = runCatching {
        val connection = URL("${baseUrl}api/v1/health").openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.setRequestProperty("Accept", "application/json")
        try {
            check(connection.responseCode in 200..299) { "Server returned ${connection.responseCode}" }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
