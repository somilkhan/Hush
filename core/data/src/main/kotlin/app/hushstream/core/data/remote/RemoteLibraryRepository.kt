package app.hushstream.core.data.remote

import app.hushstream.core.data.sync.RemoteStore
import app.hushstream.core.domain.model.MediaKind
import app.hushstream.core.domain.sync.LibraryEntry
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Saved titles in public.library_items. RLS scopes every call to the session's user. */
class RemoteLibraryRepository(
    baseUrl: String,
    publishableKey: String,
    http: OkHttpClient = OkHttpClient(),
) : RemoteStore<LibraryEntry> {

    private val client = PostgrestClient(baseUrl, publishableKey, http)

    override suspend fun fetchAll(session: AuthorizedSession): List<LibraryEntry> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            client.selectAll(TABLE, SELECT, session).map { o ->
                LibraryEntry(
                    mediaId = o.getString("media_id"),
                    kind = if (o.getString("media_type") == "series") MediaKind.SERIES else MediaKind.MOVIE,
                    title = o.getString("title"),
                    posterUrl = if (o.isNull("poster_url")) null else o.getString("poster_url").ifBlank { null },
                    saved = o.getBoolean("saved"),
                    updatedAtEpochMs = PostgrestClient.parseEpochMs(o.getString("updated_at")),
                )
            }
        }

    override suspend fun upsertAll(session: AuthorizedSession, rows: List<LibraryEntry>) =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            rows.chunked(BATCH).forEach { batch ->
                val array = JSONArray()
                batch.forEach { e ->
                    array.put(
                        JSONObject()
                            .put("user_id", session.userId)
                            .put("media_id", e.mediaId)
                            .put("media_type", if (e.kind == MediaKind.SERIES) "series" else "movie")
                            .put("title", e.title)
                            .put("poster_url", e.posterUrl ?: JSONObject.NULL)
                            .put("saved", e.saved)
                            .put("updated_at", Instant.ofEpochMilli(e.updatedAtEpochMs).toString()),
                    )
                }
                client.upsert(TABLE, ON_CONFLICT, array, session)
            }
        }

    private companion object {
        const val TABLE = "library_items"
        const val SELECT = "media_id,media_type,title,poster_url,saved,updated_at"
        const val ON_CONFLICT = "user_id,media_type,media_id"
        const val BATCH = 500
    }
}
