package app.hush.placeholder.core.data.remote

import app.hush.placeholder.core.data.sync.RemoteStore
import app.hush.placeholder.core.domain.sync.WatchEntry
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Playback positions in public.watch_history. Needs migration 20261125120000 (title column). */
class RemoteWatchHistoryRepository(
    baseUrl: String,
    publishableKey: String,
    http: OkHttpClient = OkHttpClient(),
) : RemoteStore<WatchEntry> {

    private val client = PostgrestClient(baseUrl, publishableKey, http)

    override suspend fun fetchAll(session: AuthorizedSession): List<WatchEntry> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            client.selectAll(TABLE, SELECT, session).map { o ->
                WatchEntry(
                    mediaId = o.getString("media_id"),
                    episodeId = o.getString("episode_id"),
                    positionMs = o.getLong("position_ms"),
                    durationMs = o.getLong("duration_ms"),
                    completed = o.getBoolean("completed"),
                    updatedAtEpochMs = PostgrestClient.parseEpochMs(o.getString("updated_at")),
                    title = o.optString("title"),
                )
            }
        }

    override suspend fun upsertAll(session: AuthorizedSession, rows: List<WatchEntry>) =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            rows.chunked(BATCH).forEach { batch ->
                val array = JSONArray()
                batch.forEach { e ->
                    array.put(
                        JSONObject()
                            .put("user_id", session.userId)
                            .put("media_id", e.mediaId)
                            .put("episode_id", e.episodeId)
                            .put("position_ms", e.positionMs)
                            .put("duration_ms", e.durationMs)
                            .put("completed", e.completed)
                            .put("title", e.title)
                            .put("updated_at", Instant.ofEpochMilli(e.updatedAtEpochMs).toString()),
                    )
                }
                client.upsert(TABLE, ON_CONFLICT, array, session)
            }
        }

    private companion object {
        const val TABLE = "watch_history"
        const val SELECT = "media_id,episode_id,position_ms,duration_ms,completed,title,updated_at"
        const val ON_CONFLICT = "user_id,media_id,episode_id"
        const val BATCH = 500
    }
}
