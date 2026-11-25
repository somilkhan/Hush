package app.hush.placeholder.core.data.remote

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

/**
 * Minimal PostgREST client for the sync tables. Publishable key plus the user's access token only.
 * Errors carry the table and HTTP status, never the response body or any token.
 */
internal class PostgrestClient(
    private val baseUrl: String,
    private val publishableKey: String,
    private val http: OkHttpClient,
) {
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** Reads every row of [table] that RLS lets this user see, paging by id. */
    fun selectAll(table: String, select: String, session: AuthorizedSession): List<JSONObject> {
        val out = mutableListOf<JSONObject>()
        var offset = 0
        while (true) {
            val url = endpoint(table).newBuilder()
                .addQueryParameter("select", select)
                .addQueryParameter("order", "id.asc")
                .addQueryParameter("limit", PAGE_SIZE.toString())
                .addQueryParameter("offset", offset.toString())
                .build()
            val page = JSONArray(send(Request.Builder().url(url).get(), table, session))
            for (i in 0 until page.length()) out += page.getJSONObject(i)
            if (page.length() < PAGE_SIZE) return out
            offset += page.length()
        }
    }

    /** Insert-or-update on the unique key named in [onConflict] (comma-separated columns). */
    fun upsert(table: String, onConflict: String, rows: JSONArray, session: AuthorizedSession) {
        val url = endpoint(table).newBuilder()
            .addQueryParameter("on_conflict", onConflict)
            .build()
        val body = rows.toString().toRequestBody(jsonType)
        send(
            Request.Builder()
                .url(url)
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(body),
            table,
            session,
        )
    }

    private fun endpoint(table: String): HttpUrl {
        check(baseUrl.startsWith("https://")) { "Sync requires an https Supabase URL." }
        return "${baseUrl.trimEnd('/')}/rest/v1/$table".toHttpUrl()
    }

    private fun send(builder: Request.Builder, table: String, session: AuthorizedSession): String {
        val request = builder
            .header("apikey", publishableKey)
            .header("Authorization", "Bearer ${session.accessToken}")
            .header("Accept", "application/json")
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw RemoteSyncException(table, response.code)
            return text
        }
    }

    companion object {
        const val PAGE_SIZE = 1000

        /** Supabase timestamptz text, e.g. 2026-11-25T10:00:00.123456+00:00. Throws on bad input so sync aborts. */
        fun parseEpochMs(text: String): Long = OffsetDateTime.parse(text).toInstant().toEpochMilli()
    }
}

class RemoteSyncException(val table: String, val httpCode: Int) :
    IllegalStateException("Supabase $table request failed (HTTP $httpCode)")
