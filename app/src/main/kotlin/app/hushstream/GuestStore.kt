package app.hushstream

import android.content.Context

/** Remembers that the user chose "Continue as guest". Local data only, no account. */
class GuestStore(context: Context) {
    private val prefs = context.getSharedPreferences("hush_app", Context.MODE_PRIVATE)

    var isGuest: Boolean
        get() = prefs.getBoolean("guest", false)
        set(value) {
            prefs.edit().putBoolean("guest", value).apply()
        }
}
