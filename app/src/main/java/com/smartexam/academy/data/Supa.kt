package com.smartexam.academy.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartexam.academy.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Supabase values are injected at build time (gradle -P or env). Empty = not configured. */
object Config {
    val url: String = BuildConfig.SUPABASE_URL.trimEnd('/')
    val anonKey: String = BuildConfig.SUPABASE_ANON_KEY
    val isConfigured: Boolean get() = url.startsWith("http") && anonKey.isNotBlank()
}

class ApiException(val code: Int, message: String) : IOException(message)

/** Token storage — encrypted when the device supports it, plain private prefs otherwise. */
class Session(ctx: Context) {
    private val prefs: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            "sea_session",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            ctx,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        ctx.getSharedPreferences("sea_session_plain", Context.MODE_PRIVATE)
    }

    var accessToken: String?
        get() = prefs.getString("access", null)
        set(v) = prefs.edit().putString("access", v).apply()
    var refreshToken: String?
        get() = prefs.getString("refresh", null)
        set(v) = prefs.edit().putString("refresh", v).apply()
    var expiresAt: Long
        get() = prefs.getLong("exp", 0L)
        set(v) = prefs.edit().putLong("exp", v).apply()
    var userId: String?
        get() = prefs.getString("uid", null)
        set(v) = prefs.edit().putString("uid", v).apply()
    var email: String?
        get() = prefs.getString("email", null)
        set(v) = prefs.edit().putString("email", v).apply()

    val isLoggedIn: Boolean get() = !refreshToken.isNullOrBlank() && !userId.isNullOrBlank()

    fun save(j: JSONObject) {
        accessToken = j.getString("access_token")
        refreshToken = j.optString("refresh_token", refreshToken ?: "")
        val expiresIn = j.optLong("expires_in", 3600L)
        expiresAt = System.currentTimeMillis() / 1000 + expiresIn
        j.optJSONObject("user")?.let { u ->
            userId = u.optString("id", userId ?: "")
            email = u.optString("email", email ?: "")
        }
    }

    fun clear() = prefs.edit().clear().apply()
}

/** Minimal GoTrue + PostgREST client on OkHttp. */
const val REDIRECT = "smartexam://login-callback"

class Supa(ctx: Context) {
    val session = Session(ctx.applicationContext)
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val json = "application/json; charset=utf-8".toMediaType()
    private val refreshLock = Mutex()

    // ───────── Auth (email OTP) ─────────

    suspend fun sendOtp(email: String) {
        val body = JSONObject().put("email", email.trim()).put("create_user", true)
        raw("POST", "/auth/v1/otp?redirect_to=" + java.net.URLEncoder.encode(REDIRECT, "UTF-8"), body.toString(), auth = false)
    }

    /**
     * Handles the sign-in link from the email (smartexam://login-callback…).
     * Supports tokens in the fragment (#access_token=…&refresh_token=…), ?token_hash=…&type=…,
     * and reports ?error_description=… back as an exception.
     */
    suspend fun handleCallback(uri: android.net.Uri) {
        val frag = uri.encodedFragment.orEmpty().split("&").mapNotNull {
            val i = it.indexOf('='); if (i > 0) it.substring(0, i) to android.net.Uri.decode(it.substring(i + 1)) else null
        }.toMap()
        fun p(k: String) = frag[k] ?: uri.getQueryParameter(k)
        p("error_description")?.let { throw ApiException(400, it.replace('+', ' ')) }
        val access = p("access_token")
        if (access != null) {
            session.save(JSONObject().put("access_token", access).put("refresh_token", p("refresh_token") ?: "")
                .put("expires_in", (p("expires_in") ?: "3600").toLongOrNull() ?: 3600L))
            val user = JSONObject(raw("GET", "/auth/v1/user", null, auth = true))
            session.userId = user.optString("id"); session.email = user.optString("email")
            return
        }
        val hash = p("token_hash")
        if (hash != null) {
            val type = p("type") ?: "magiclink"
            val res = JSONObject(raw("POST", "/auth/v1/verify",
                JSONObject().put("type", if (type == "signup") "email" else type).put("token_hash", hash).toString(), auth = false))
            session.save(res); return
        }
        throw ApiException(400, "लॉग इन लिंक अधूरा है। ईमेल का नया लिंक या OTP इस्तेमाल करें।")
    }

    suspend fun verifyOtp(email: String, token: String) {
        val body = JSONObject().put("type", "email").put("email", email.trim()).put("token", token.trim())
        val res = JSONObject(raw("POST", "/auth/v1/verify", body.toString(), auth = false))
        session.save(res)
        if (session.email.isNullOrBlank()) session.email = email.trim()
    }

    private suspend fun refresh(): Boolean = refreshLock.withLock {
        val rt = session.refreshToken ?: return false
        return try {
            val res = JSONObject(
                raw("POST", "/auth/v1/token?grant_type=refresh_token",
                    JSONObject().put("refresh_token", rt).toString(), auth = false)
            )
            session.save(res); true
        } catch (e: ApiException) {
            if (e.code == 400 || e.code == 401) session.clear()
            false
        }
    }

    private suspend fun ensureFresh() {
        if (session.refreshToken.isNullOrBlank()) return
        val now = System.currentTimeMillis() / 1000
        if (session.accessToken.isNullOrBlank() || session.expiresAt - now < 60) refresh()
    }

    suspend fun logout() {
        try { raw("POST", "/auth/v1/logout", "{}", auth = true) } catch (_: Exception) {}
        session.clear()
    }

    // ───────── PostgREST ─────────

    suspend fun get(pathAndQuery: String): JSONArray = JSONArray(rest("GET", pathAndQuery, null))

    suspend fun insert(table: String, row: JSONObject, upsert: Boolean = false): JSONArray {
        val prefer = if (upsert) "return=representation,resolution=merge-duplicates" else "return=representation"
        return JSONArray(rest("POST", table, row.toString(), prefer))
    }

    suspend fun rpc(fn: String, args: JSONObject): String = rest("POST", "rpc/$fn", args.toString())

    private suspend fun rest(method: String, path: String, body: String?, prefer: String? = null): String {
        ensureFresh()
        return try {
            raw(method, "/rest/v1/$path", body, auth = true, prefer = prefer)
        } catch (e: ApiException) {
            if (e.code == 401 && refresh()) raw(method, "/rest/v1/$path", body, auth = true, prefer = prefer) else throw e
        }
    }

    private suspend fun raw(
        method: String, path: String, body: String?, auth: Boolean, prefer: String? = null
    ): String = withContext(Dispatchers.IO) {
        if (!Config.isConfigured) throw ApiException(0, "Supabase कॉन्फ़िगर नहीं है")
        val b = Request.Builder().url(Config.url + path)
            .header("apikey", Config.anonKey)
            .header("Accept", "application/json")
        // Publishable keys go only in `apikey`; Authorization carries a user JWT when signed in.
        val token = if (auth) session.accessToken?.takeIf { it.isNotBlank() } else null
        if (token != null) b.header("Authorization", "Bearer $token")
        if (prefer != null) b.header("Prefer", prefer)
        when (method) {
            "GET" -> b.get()
            else -> b.method(method, (body ?: "{}").toRequestBody(json))
        }
        http.newCall(b.build()).execute().use { r ->
            val text = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw ApiException(r.code, readableError(r.code, text))
            text.ifBlank { "[]" }
        }
    }

    private fun readableError(code: Int, text: String): String {
        val msg = try {
            val j = JSONObject(text)
            j.optString("msg").ifBlank { j.optString("error_description") }
                .ifBlank { j.optString("message") }.ifBlank { j.optString("error") }
        } catch (_: Exception) { "" }
        return when {
            code == 429 -> "बहुत ज़्यादा कोशिशें हुईं। थोड़ी देर बाद फिर कोशिश करें।"
            msg.contains("expired", true) || msg.contains("invalid", true) && msg.contains("otp", true) ->
                "OTP ग़लत है या उसकी समय-सीमा ख़त्म हो गई।"
            msg.isNotBlank() -> msg
            else -> "सर्वर त्रुटि ($code)"
        }
    }

    /** Blocking download for images; call off the main thread. */
    fun fetchBytes(url: String): ByteArray? = try {
        http.newCall(Request.Builder().url(url).build()).execute().use { if (it.isSuccessful) it.body?.bytes() else null }
    } catch (_: Exception) { null }
}
