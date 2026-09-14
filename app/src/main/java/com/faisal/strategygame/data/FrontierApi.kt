package com.faisal.strategygame.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URI
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection

const val DEFAULT_GATEWAY = "https://broker-craft-mariah-enormous.trycloudflare.com"
class ApiFailure(val status: Int, message: String) : Exception(message)

fun secureOrigin(raw: String): String {
    val uri = URI(raw.trim().trimEnd('/'))
    require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null &&
        uri.query == null && uri.fragment == null && uri.path.orEmpty().isEmpty()) {
        "أدخل عنوان HTTPS للسيرفر بدون مسار أو بيانات دخول"
    }
    return uri.toASCIIString()
}

class FrontierApi(val origin: String, private val device: String) {
    var token: String = ""
    var clockOffset: Long = 0
        private set

    suspend fun request(path: String, body: JSONObject? = null, key: String? = null): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL("$origin/api/v1$path").openConnection() as HttpsURLConnection
        try {
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.instanceFollowRedirects = false
            connection.requestMethod = if (body == null) "GET" else "POST"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Device-ID", device)
            connection.setRequestProperty("X-Device-Kind", "android")
            connection.setRequestProperty("X-Client-Version", "0.2.0")
            if (token.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer $token")
            if (key != null) connection.setRequestProperty("X-Idempotency-Key", key)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText().take(2_000_000) }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrElse {
                throw ApiFailure(code, "تعذر قراءة استجابة السيرفر ($code). تحقق من رابط الاتصال.")
            }
            if (code !in 200..299) throw ApiFailure(code, json.optString("error", "فشل الطلب ($code)"))
            if (connection.date > 0) clockOffset = connection.date - System.currentTimeMillis()
            json
        } finally { connection.disconnect() }
    }
}

/** Tokens are encrypted with a non-exportable Android Keystore key; passwords are never saved. */
class SessionVault(context: Context) {
    private val prefs = context.getSharedPreferences("frontier_session", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("frontier_session_key", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("frontier_session_key", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun save(value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val data = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("value", Base64.encodeToString(data, Base64.NO_WRAP)).commit())
    }
    fun read(): String? = runCatching {
        val data = Base64.decode(prefs.getString("value", null) ?: return null, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data.copyOfRange(0, 12)))
        }
        String(cipher.doFinal(data.copyOfRange(12, data.size)), Charsets.UTF_8)
    }.getOrNull()
    fun clear() { prefs.edit().clear().commit() }
}

fun JSONObject.rows(key: String): List<JSONObject> = optJSONArray(key)?.let { array ->
    (0 until array.length()).mapNotNull { array.optJSONObject(it) }
}.orEmpty()
fun JSONObject.obj(key: String): JSONObject = optJSONObject(key) ?: JSONObject()
fun json(vararg pairs: Pair<String, Any>): JSONObject = JSONObject().apply { pairs.forEach { put(it.first, it.second) } }
