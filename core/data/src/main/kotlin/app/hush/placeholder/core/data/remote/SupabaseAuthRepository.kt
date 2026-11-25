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
 * Uses only the publishable key, never the service role key. Requires an https base URL.
 * Tokens are stored encrypted with a Keystore key (SecureTokenStore).
 */
class SupabaseAuthRepository(
    context: Context,
    private val baseUrl: String,
    private val publishableKey: String,
    private val http: OkHttpClient = OkHttpClient(),
) : AuthRepository {

    private val store = SecureTokenStore(context)
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private val configured: Boolean
        get() = baseUrl.startsWith("https://") && publishableKey.isNotBlank()

    override suspend fun currentSession(): Session? = withContext(Dispatchers.IO) {
        val stored = readStored() ?: return@withContext null
        if (stored.expiresAtEpochSec - nowSec() > 60) return@withContext stored.toSession()
        if (!configured) return@withContext null
        val result = refresh(stored.refreshToken)
        result.getOrNull() ?: run {
            // Only a real rejection (400/401) ends the session. Network errors keep tokens for the next try.
            if (result.exceptionOrNull() is AuthRejectedException) store.clear()
            null
        }
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
        // With email confirmation on, Supabase returns no session. The UI must ask the user to confirm.
        post("/auth/v1/signup", body).map { json -> storeSession(json) }
    }

    override suspend fun signOut() = withContext(Dispatchers.IO) {
        val stored = readStored()
        if (configured && stored != null) {
            // Best effort. Local state is cleared even if the server call fails.
            post("/auth/v1/logout", JSONObject(), bearer = stored.accessToken)
        }
        store.clear()
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
                if (response.code == 400 || response.code == 401) throw AuthRejectedException(message)
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
        store.put("access", stored.accessToken)
        store.put("refresh", stored.refreshToken)
        store.put("expires", stored.expiresAtEpochSec.toString())
        store.put("user_id", stored.userId)
        store.put("email", stored.email)
        return stored.toSession()
    }

    private fun readStored(): StoredSession? {
        val access = store.get("access") ?: return null
        val userId = store.get("user_id") ?: return null
        return StoredSession(
            accessToken = access,
            refreshToken = store.get("refresh").orEmpty(),
            expiresAtEpochSec = store.get("expires")?.toLongOrNull() ?: 0L,
            userId = userId,
            email = store.get("email").orEmpty(),
        )
    }

    private fun notConfigured(): Result<Nothing> =
        Result.failure(IllegalStateException("Sync is not configured in this build."))

    private fun nowSec(): Long = System.currentTimeMillis() / 1000

    /** The server rejected the credentials or refresh token. */
    class AuthRejectedException(message: String) : IllegalStateException(message)

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
