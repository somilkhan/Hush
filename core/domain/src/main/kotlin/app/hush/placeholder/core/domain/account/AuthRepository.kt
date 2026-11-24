package app.hush.placeholder.core.domain.account

/** Signed-in user. Tokens are held by the data layer, never shown in UI. */
data class Session(
    val userId: String,
    val email: String,
    val expiresAtEpochSec: Long,
)

/** Email and password accounts. Implementation lives in :core:data (Supabase adapter). */
interface AuthRepository {
    suspend fun currentSession(): Session?
    suspend fun signIn(email: String, password: String): Result<Session>
    suspend fun signUp(email: String, password: String): Result<Session?>
    suspend fun signOut()
}
