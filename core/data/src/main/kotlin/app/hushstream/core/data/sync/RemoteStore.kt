package app.hushstream.core.data.sync

import app.hushstream.core.data.remote.AuthorizedSession

/** Sync-facing remote table for one row type. Implemented by the Supabase adapters in remote/. */
interface RemoteStore<T> {
    suspend fun fetchAll(session: AuthorizedSession): List<T>
    suspend fun upsertAll(session: AuthorizedSession, rows: List<T>)
}
