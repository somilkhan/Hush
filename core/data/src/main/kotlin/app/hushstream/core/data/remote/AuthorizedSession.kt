package app.hushstream.core.data.remote

/** Signed-in user and a bearer token for PostgREST calls. Never log [accessToken]. */
class AuthorizedSession(val userId: String, val accessToken: String) {
    override fun toString(): String = "AuthorizedSession(userId=$userId, accessToken=<redacted>)"
}
