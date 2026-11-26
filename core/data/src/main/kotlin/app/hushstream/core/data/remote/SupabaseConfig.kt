package app.hushstream.core.data.remote

import app.hushstream.core.data.BuildConfig

/** Public Supabase settings from local.properties. Empty when not configured. Publishable key only. */
object SupabaseConfig {
    val url: String = BuildConfig.SUPABASE_URL
    val publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY
}
