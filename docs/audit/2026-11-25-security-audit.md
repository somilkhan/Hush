# HUSH security and code audit, 2026-11-25

Branch: feature/app-vertical-slice. Audit only. No app code, database, commits or pushes changed.
Supabase project: mrpagrxdyqooyfeybwqn (ap-south-1). Read-only queries and advisors only.

## Findings

| ID | Severity | File or object | What is wrong | Evidence | Fix | Status |
|---|---|---|---|---|---|---|
| F1 | medium | core/data SupabaseAuthRepository.kt (`hush_auth` prefs) | Access and refresh tokens stored in plain SharedPreferences, not encrypted. The code's own comment flags this. | `prefs.edit().putString("refresh", ...)`; comment "not encrypted" | Use Keystore-backed storage (EncryptedSharedPreferences or Tink). minSdk 26 supports it. | VERIFIED |
| F2 | medium | app/src/main/AndroidManifest.xml | No `android:allowBackup` or data-extraction rules. Backups can copy the token file. | `<application android:label="HUSH" android:supportsRtl="true" ...>` contains no allowBackup | Set `android:allowBackup="false"` or add dataExtractionRules that exclude `hush_auth`. | VERIFIED (absence in manifest) |
| F3 | low | SupabaseAuthRepository.kt `currentSession()` | Any refresh failure returns null but keeps stored tokens. An expired or revoked refresh token stays on disk. A network error also looks like a sign-out. | `refresh(stored.refreshToken).getOrNull()` with no clear on failure | Clear stored session only on 400/401 from refresh. Keep it on network errors. | VERIFIED (code path); runtime NOT TESTED |
| F4 | low | SupabaseAuthRepository.kt, BuildConfig | No check that the URL is https. A non-https `supabase.url` in local.properties would send passwords and tokens in clear. | `baseUrl.trimEnd('/') + path`; no scheme check | Refuse non-https base URL in `configured`. | VERIFIED |
| F5 | low | core/player HushVideoPlayer.kt | ExoPlayer is built and `prepare()`d inside `remember(url)`, which runs during composition. If composition is discarded before `DisposableEffect` registers, the player leaks. `PlayerView` is not detached on dispose. | `remember(url) { ExoPlayer.Builder(context).build().apply { ... prepare() } }` | Create in a `DisposableEffect` keyed on url, and release there. | INFERENCE (not run) |
| F6 | info | SupabaseAuthRepository.kt `post()` | Server error text (`error_description`, `msg`) is shown to the user. GoTrue messages are user-facing and contain no stack traces or keys. | `error(message)` | No change needed. Keep messages generic if the server text ever changes. | VERIFIED |

## Items that are fine (with evidence)

- **RLS on every public table.** list_tables: `rls_enabled: true` for library_items, watch_history, addons, plugins.
- **Owner-only policies, authenticated role only.** pg_policy: `own library rows cmd=* roles=authenticated`, and the same shape for the other three. Each has `USING` and `WITH CHECK` on `(select auth.uid()) = user_id`.
- **Spoofed user_id is blocked.** The `WITH CHECK` clause rejects any insert or update whose user_id is not the caller. Logic check from the migration; not run against a live client (see below).
- **anon has no grants.** role_table_grants for all four tables lists only postgres, authenticated and service_role. The migration also runs `revoke all ... from anon`.
- **Cascade delete from auth.users.** Each user_id FK is `on delete cascade` (list_tables foreign_key_constraints).
- **Indexes.** Unique (user_id, ...) keys and `(user_id, updated_at desc)` indexes exist on all four tables (pg_indexes).
- **Supabase security advisor.** `get_advisors(type=security)` returned `lints: []`.
- **No service-role or secret key in the repo.** grep for `service_role`, `sb_secret`, `sb_publishable`, and `eyJ...` JWT-shaped strings found no hits in the working tree. History search (`git log -p -G`) found no key values. Only the word "password" as a parameter name, and a licence sentence.
- **local.properties is git-ignored and untracked.** `.gitignore:4: local.properties`; `git ls-files` shows no match.
- **Publishable key only in the client.** The adapter sends `apikey` and `Authorization: Bearer <publishable key>`. The key is compiled in via BuildConfig, which is expected for a publishable key.
- **Domain models hold no tokens.** `Session` contains only userId, email and expiresAtEpochSec.
- **signOut clears local state.** It calls `prefs.edit().clear()` even if the server call fails.
- **Cleartext disabled by default.** No `usesCleartextTraffic` in the manifest, and no http:// literals in code.
- **UI null handling.** Stream URLs come from `firstOrNull()?.url` and are checked with `?.let` and `enabled = url != null` (HeroBanner.kt:38, DetailsScreen.kt:122). No `!!` found in app/ui.
- **Player releases on dispose.** `DisposableEffect(player) { onDispose { player.release() } }` in HushVideoPlayer.kt.
- **M7 release items.** `LICENSE` exists with the GPL-3 header. `NOTICE` exists (436 bytes). The NOTICE content was not read.
- **Placeholder namespace is still in code (open).** Namespace `app.hush.placeholder.core.data`. The M7 checklist item "placeholder namespace not shipped" stays open, as D-002 requires.

## Not checked, and why

- **Runtime auth behaviour, NOT TESTED.** The sandbox has no HTTP path to supabase.co from the audit, and no device run. Refresh, expiry and signup-confirmation behaviour are from code reading only.
- **Live RLS spoof test, NOT TESTED.** Confirming that a client insert with another user_id fails needs an authenticated test client. Only the policy text was checked.
- **FORCE RLS, NOT CHECKED.** My catalog query failed on a type error, and the retry did not select relforcerowsecurity. Table owner is postgres, so RLS is bypassed only for that role.
- **Performance and health advisors, NOT RUN.** Only the security advisor was requested.
- **NOTICE content, NOT READ.** Only existence and size were checked.
- **Compose UI, partial.** Pattern grep only, not a full read of every screen.
- **Build, lint and tests, NOT RUN.** No Gradle run in this audit.
- **Git history, partial.** Pattern search only, not a full secret scanner such as gitleaks.
