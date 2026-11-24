package app.hush.placeholder.core.data.remote

import android.content.Context
import app.hush.placeholder.core.domain.account.AuthRepository
import app.hush.placeholder.core.domain.account.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Email and password accounts against Supabase Auth (GoTrue REST).
 * Uses only the publishable key. Never the service role key.
 *
 * Known gap: session tokens are stored in private SharedPreferences, which is not encrypted.
 * Move to encrypted storage before any public release (see DECISION_LOG D-010).
 */
class SupabaseAuthRepository(
    context: Context,
    private val baseUrl: String,
    private val publishableKey: String,
    private val http: OkHttpClient = OkHttpClient(),
) : AuthRepository {

    private val prefs = context.getSharedPreferences("hush_auth", Context.MODE_PRIVATE)
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private val configured: Boolean get() = baseUrl.isNotBlank() && publishableKey.isNotBlank()

    override suspend fun currentSession(): Session? = withContext(Dispatchers.IO) {
        val stored = readStored() ?: return@withContext null
        if (stored.expiresAtEpochSec - nowSec() > 60) return@withContext stored.toSession()
        refresh(stored.refreshToken).getOrNull()
    }

    override suspend fun signIn(email: String, password: String): Result<Session> = withContext(Dispatchers.IO) {
        if (!configured) return@withContext notConfigured()
        val body = JSONObject().put("email", email).put("password", password)
        post("/auth/v1/token?grant_type=password", body).mapCatching { json ->
            storeSession(json) ?: error("Sign-in returned no session")
        }
    }

    override suspend fun signUp(email: String, password: String): Result<Session?> = withContext(Dispatchers.IO) {
        if (!configured) return@withContext notConfigured()
        val body = JSONObject().put("email", email).put("password", password)
        // With email confirmation on, Supabase returns no session. Caller must tell the user to confirm.
        post("/auth/v1/signup", body).map { json -> storeSession(json) }
    }

    override suspend fun signOut() = withContext(Dispatchers.IO) {
        val stored = readStored()
        if (configured && stored != null) {
            // Best effort. Local state is cleared even if the server call fails.
            post("/auth/v1/logout", JSONObject(), bearer = stored.accessToken)
        }
        prefs.edit().clear().apply()
    }

    private fun refresh(refreshToken: String): Result<Session> {
        val body = JSONObject().put("refresh_token", refreshToken)
        return post("/auth/v1/token?grant_type=refresh_token", body).mapCatching { json ->
            storeSession(json) ?: error("Refresh returned no session")
        }
    }

    private fun post(path: String, body: JSONObject, bearer: String? = null): Result<JSONObject> = runCatching {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + path)
            .header("apikey", publishableKey)
            .header("Authorization", "Bearer ${bearer ?: publishableKey}")
            .post(body.toString().toRequestBody(jsonType))
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (!response.isSuccessful) {
                val message = json.optString("error_description")
                    .ifBlank { json.optString("msg") }
                    .ifBlank { json.optString("message") }
                    .ifBlank { "Request failed (${response.code})" }
                error(message)
            }
            json
        }
    }

    /** Stores tokens when present. Returns null when the response carries no session (signup awaiting confirmation). */
    private fun storeSession(json: JSONObject): Session? {
        val access = json.optString("access_token")
        if (access.isBlank()) return null
        val user = json.getJSONObject("user")
        val stored = StoredSession(
            accessToken = access,
            refreshToken = json.optString("refresh_token"),
            expiresAtEpochSec = json.optLong("expires_at", nowSec() + json.optLong("expires_in", 3600)),
            userId = user.getString("id"),
            email = user.optString("email"),
        )
        prefs.edit()
            .putString("access", stored.accessToken)
            .putString("refresh", stored.refreshToken)
            .putLong("expires", stored.expiresAtEpochSec)
            .putString("user_id", stored.userId)
            .putString("email", stored.email)
            .apply()
        return stored.toSession()
    }

    private fun readStored(): StoredSession? {
        val access = prefs.getString("access", null) ?: return null
        val userId = prefs.getString("user_id", null) ?: return null
        return StoredSession(
            accessToken = access,
            refreshToken = prefs.getString("refresh", "").orEmpty(),
            expiresAtEpochSec = prefs.getLong("expires", 0L),
            userId = userId,
            email = prefs.getString("email", "").orEmpty(),
        )
    }

    private fun notConfigured(): Result<Nothing> =
        Result.failure(IllegalStateException("Sync is not configured in this build."))

    private fun nowSec(): Long = System.currentTimeMillis() / 1000

    private data class StoredSession(
        val accessToken: String,
        val refreshToken: String,
        val expiresAtEpochSec: Long,
        val userId: String,
        val email: String,
    ) {
        fun toSession() = Session(userId = userId, email = email, expiresAtEpochSec = expiresAtEpochSec)
    }
}
