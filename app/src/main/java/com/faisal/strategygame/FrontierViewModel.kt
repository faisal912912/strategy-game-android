package com.faisal.strategygame

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faisal.strategygame.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

data class CityJob(val kind: String, val label: String, val ends: Long, val claimPath: String = "", val claimId: Long = 0) {
    fun encode() = json("kind" to kind, "label" to label, "ends" to ends, "claimPath" to claimPath, "claimId" to claimId)
}
data class Command(val title: String, val path: String, val body: JSONObject, val jobKind: String = "", val key: String = UUID.randomUUID().toString()) {
    fun encode() = json("title" to title, "path" to path, "body" to body, "jobKind" to jobKind, "key" to key)
    companion object {
        fun decode(j: JSONObject) = Command(j.getString("title"), j.getString("path"), j.getJSONObject("body"), j.optString("jobKind"), j.getString("key"))
    }
}
class FrontierViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("frontier_online", 0)
    private val vault = SessionVault(app)
    private val device = prefs.getString("device", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("device", it).commit() }
    var gateway by mutableStateOf(prefs.getString("gateway", DEFAULT_GATEWAY) ?: DEFAULT_GATEWAY); private set
    private var api = FrontierApi(gateway, device)
    private var scopeKey = ""
    var signedIn by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var refreshing by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    var error by mutableStateOf<String?>(null); private set
    var tab by mutableStateOf("city"); private set
    var documents by mutableStateOf<Map<String, JSONObject>>(emptyMap()); private set
    var failedPaths by mutableStateOf<Set<String>>(emptySet()); private set
    var jobs by mutableStateOf<List<CityJob>>(emptyList()); private set
    var pending by mutableStateOf<Command?>(null); private set
    var now by mutableStateOf(System.currentTimeMillis()); private set
    var lastSync by mutableStateOf(0L); private set
    var scanX by mutableStateOf(250); private set
    var scanY by mutableStateOf(250); private set
    val nodePath get() = "/world/v3/resources/scan?radius=100&x=$scanX&y=$scanY"
    val monsterPath get() = "/world/v4/monsters/scan?radius=150&x=$scanX&y=$scanY"
    fun scan(x: Int, y: Int) { scanX = x.coerceIn(0, 499); scanY = y.coerceIn(0, 499); refresh() }
    var reduceMotion by mutableStateOf(prefs.getBoolean("reduceMotion", false)); private set
    val canAct get() = signedIn && !busy && !refreshing && pending == null && lastSync > 0 && failedPaths.isEmpty()
    val me get() = doc("/me")
    fun doc(path: String) = documents[path] ?: JSONObject()
    fun clearMessage() { message = null }
    fun setMotion(value: Boolean) { reduceMotion = value; prefs.edit().putBoolean("reduceMotion", value).apply() }
    fun tick() { now = System.currentTimeMillis() + api.clockOffset }

    init {
        vault.read()?.let { raw ->
            runCatching { JSONObject(raw) }.getOrNull()?.let { saved ->
                if (saved.optString("origin") == gateway) {
                    api.token = saved.optString("token")
                    viewModelScope.launch { establishSession() }
                }
            }
        }
    }
    private fun friendly(e: Exception): String = when {
        e is ApiFailure && e.status == 401 -> "انتهت الجلسة أو بيانات الدخول غير صحيحة. سجّل الدخول مجددًا."
        e is ApiFailure && e.status == 429 -> "طلبات كثيرة. انتظر دقيقة ثم أعد المحاولة."
        e is ApiFailure -> "لم ينفذ الطلب: ${e.message}"
        e is IllegalArgumentException -> e.message ?: "تحقق من البيانات"
        else -> "تعذر الاتصال. تحقق من الإنترنت ورابط السيرفر ثم أعد المحاولة."
    }
    private fun handle(e: Exception) {
        if (e is CancellationException) throw e
        error = friendly(e)
        if (e is ApiFailure && e.status == 401) {
            api.token = ""; vault.clear(); signedIn = false; documents = emptyMap(); lastSync = 0
        }
    }
    fun authenticate(user: String, password: String, rawGateway: String, register: Boolean) {
        if (busy) return
        busy = true; error = null
        viewModelScope.launch {
            try {
                require(user.trim().length in 3..32) { "اسم الحساب من 3 إلى 32 حرفًا" }
                require(password.length in 10..128) { "كلمة المرور من 10 إلى 128 حرفًا" }
                gateway = secureOrigin(rawGateway)
                api = FrontierApi(gateway, device)
                checkRelease()
                val body = json("username" to user.trim(), "password" to password)
                if (register) api.request("/register", body)
                val login = api.request("/login", body)
                api.token = login.getString("token")
                vault.save(json("origin" to gateway, "token" to api.token).toString())
                prefs.edit().putString("gateway", gateway).commit()
                establishSession()
            } catch (e: Exception) { handle(e) } finally { busy = false }
        }
    }
    private suspend fun checkRelease() {
        val health = api.request("/health")
        require(health.optInt("server_id") == 1) { "هذه النسخة مهيأة للمملكة الأولى" }
        val release = api.request("/client/release")
        require(versionSupported(BuildConfig.VERSION_NAME, release.optString("min_client_version", "0.1.0"))) { "يلزم تحديث التطبيق للدخول إلى السيرفر" }
    }
    private suspend fun establishSession() {
        busy = true
        try {
            checkRelease()
            val profile = api.request("/me")
            scopeKey = gateway + ":" + profile.getLong("player_id")
            val saved = runCatching { JSONObject(prefs.getString(scopeKey, "{}")!!) }.getOrDefault(JSONObject())
            jobs = saved.rows("jobs").map { CityJob(it.getString("kind"), it.getString("label"), it.getLong("ends"),it.optString("claimPath"),it.optLong("claimId")) }
            pending = saved.optJSONObject("pending")?.let { Command.decode(it) }
            documents = mapOf("/me" to profile); signedIn = true; error = null; failedPaths = emptySet()
            refreshData()
        } catch (e: Exception) { handle(e) } finally { busy = false }
    }
    fun selectTab(value: String) { tab = value; refresh() }
    fun refresh() {
        if (!signedIn || busy || refreshing) return
        viewModelScope.launch { refreshData() }
    }
    private suspend fun refreshData() {
        if (refreshing) return
        refreshing = true
        val paths = mutableListOf("/me", "/world/v3/gather/marches", "/world/v4/hunt/marches")
        paths += when (tab) {
            "city" -> listOf("/game/buildings", "/city/progression")
            "army" -> listOf("/game/research", "/commanders", "/hospital/v2/status", "/hospital/v2/heal/jobs")
            "world" -> listOf("/world/v2/state", nodePath, monsterPath, "/world/v4/pve/status", "/world/resources", "/world/monsters")
            "missions" -> listOf("/daily/quests", "/progress/v2/daily", "/world/v4/hunt/reports", "/world/v3/gather/reports", "/mail")
            else -> listOf("/alliances/me", "/leaderboards/power", "/inventory")
        }
        try {
            val responses = coroutineScope { paths.map { path -> async {
                try { Triple(path, api.request(path), null) }
                catch (e: Exception) { if (e is CancellationException) throw e; Triple(path, null, e) }
            } }.awaitAll() }
            var next = documents
            val failures = mutableSetOf<String>()
            for ((path, value, failure) in responses) {
                if (value != null) next = next + (path to value)
                else if (path == "/alliances/me" && failure is ApiFailure && failure.status == 404) next = next + (path to JSONObject())
                else { failures += path; if (failure != null) handle(failure) }
            }
            if (signedIn) documents = next
            failedPaths = failures
            if (failures.isEmpty()) { lastSync = System.currentTimeMillis(); error = null }
        } finally { refreshing = false; tick() }
    }
    private fun persist() {
        if (scopeKey.isEmpty()) return
        val data = json("jobs" to JSONArray(jobs.map { it.encode() }))
        pending?.let { data.put("pending", it.encode()) }
        check(prefs.edit().putString(scopeKey, data.toString()).commit())
    }
    fun submit(command: Command) {
        if (!canAct) return
        pending = command
        try { persist() } catch (e: Exception) { pending = null; handle(e); return }
        retryPending()
    }
    fun retryPending() {
        val command = pending ?: return
        if (busy || refreshing || !signedIn) return
        busy = true; error = null
        viewModelScope.launch {
            try {
                val response = api.request(command.path, command.body, command.key)
                if (command.jobKind.isNotEmpty()) {
                    val claimPath = when(command.jobKind) {"gather" -> "/world/gather/claim";"hunt" -> "/world/monsters/claim";else -> ""}
                    jobs = jobs.filterNot { it.kind == command.jobKind } + CityJob(command.jobKind, command.title, instantMillis(response.optString("finishes_at")),claimPath,response.optLong("job_id",response.optLong("hunt_id")))
                }
                if (command.path.endsWith("/claim")) {
                    val kind = when(command.path) { "/game/buildings/claim" -> "build"; "/game/training/claim" -> "train"; "/game/research/claim" -> "research"; else -> "" }
                    jobs = jobs.filterNot { it.kind == kind || it.claimPath == command.path }
                }
                pending = null; persist(); message = "تم: ${command.title}"
                refreshData()
            } catch (e: Exception) {
                // A timeout/5xx can happen AFTER commit. Keep the same body and key for safe replay.
                if (e is ApiFailure && e.status in 400..499 && e.status !in listOf(401, 408, 425, 429)) {
                    if (e.status == 404 && command.path.startsWith("/game/") && command.path.endsWith("/claim")) {
                        val kind = if ("buildings" in command.path) "build" else if ("training" in command.path) "train" else "research"
                        jobs = jobs.filterNot { it.kind == kind }
                    }
                    pending = null; persist()
                }
                handle(e)
            } finally { busy = false }
        }
    }
    fun logout() {
        if (busy || refreshing) return
        busy = true
        viewModelScope.launch {
            try { api.request("/logout", JSONObject()) } catch (_: Exception) { /* Local token is always removed. */ }
            vault.clear(); api.token = ""; signedIn = false; documents = emptyMap(); jobs = emptyList()
            pending = null; scopeKey = ""; lastSync = 0; error = null; busy = false; tab = "city"
        }
    }
}

fun instantMillis(value: String): Long = runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(0)
fun versionSupported(current: String, minimum: String): Boolean {
    fun parts(s: String) = s.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    val a = parts(current); val b = parts(minimum)
    for (i in 0 until maxOf(a.size, b.size)) {
        val difference = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
        if (difference != 0) return difference > 0
    }
    return true
}
