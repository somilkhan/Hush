# Backend and networking parity audit (Nuvio vs HUSH)

- Date: 2026-11-26
- Time: 05:37 UTC (sandbox clock)
- HUSH commit audited: 887cdea (branch fix/root-theme-wrap)
- Reference: NuvioMobile-Enhanced at /home/claude/upstream-nuvio, commit 7aca5a018
- Scope: read-only. No code, migrations, data or git state changed.
- Runtime status: UNVERIFIED. The sandbox cannot reach supabase.co from the shell. Remote facts below come from read-only Supabase MCP calls (list_migrations, list_tables, get_advisors), which are project-side evidence, not device runs.

## Evidence used

- Remote (read-only MCP, project mrpagrxdyqooyfeybwqn):
  - list_migrations: only `20261124234157 init_sync_schema`. The watch_history title migration is absent.
  - list_tables (public): library_items, watch_history, addons, plugins. All RLS enabled. All tables have 0 rows. watch_history has no `title` column.
  - get_advisors security: no lints.
  - get_advisors performance: 2 INFO `unused_index` (library_items_user_updated_idx, watch_history_user_updated_idx). Expected with 0 rows. health advisor not run.
- Local: supabase/migrations/20261125000000_init_sync_schema.sql, 20261125120000_watch_history_title.sql.
- Local: core/data/.../remote/{SupabaseAuthRepository, SecureTokenStore, SupabaseConfig, AuthorizedSession, PostgrestClient, RemoteLibraryRepository, RemoteWatchHistoryRepository}.kt
- Local: core/data/.../sync/{SyncEngine, SyncMerge, RemoteStore}.kt; core/domain/.../sync/SyncModels.kt; core/domain/.../account/AuthRepository.kt
- Local call sites: app/.../ui/settings/SettingsScreen.kt:41,73; app/.../ui/HushApp.kt:64.
- Nuvio: features/library/LibrarySyncReconciler.kt; features/library/sync/SupabaseLibrarySyncAdapter.kt; features/watching/sync/SupabaseProgressSyncAdapter.kt; core/auth/*.kt; features/addons/AddonHttpClient.kt.

## Known open items, confirmed or refuted

| Item | Status | Evidence |
|---|---|---|
| Sync not wired to app start | CONFIRMED | SyncEngine is constructed nowhere outside tests. `syncNow()` has no production caller. |
| Tombstones for deletes missing | CONFIRMED | SyncEngine.kt class doc says deletions are not synced. No deleted/tombstone column in either migration. No delete RPC. `LibraryRepository.remove` (SyncModels.kt) never reaches the remote. |
| Auth refresh has no mutex | CONFIRMED | `currentSession()` and `authorizedSession()` each call `refresh()` with no lock. Both can run at once. GoTrue rotates refresh tokens, so the loser gets 400, hits `AuthRejectedException`, and `store.clear()` signs the user out. Runtime UNVERIFIED. |
| Redirect policy unrestricted | CONFIRMED | `OkHttpClient()` is created with defaults in SupabaseAuthRepository, RemoteLibraryRepository and RemoteWatchHistoryRepository. OkHttp follows redirects by default, including https to http. No `followRedirects(false)` and no host check. |
| Sign-out must preserve guest data, history, downloads | ALREADY IMPLEMENTED (backend path) | `signOut()` calls `/auth/v1/logout`, then `store.clear()`, which only wipes the `hush_secure` prefs (tokens). Nothing else in the backend path clears local stores. Settings calls `auth.signOut()` only. Re-check when local-store clearing is added. |
| watch_history title migration applied remotely | REFUTED as "applied" | Remote has no `title` column and no migration entry. |

## Findings beyond the known list

1. **High, HUSH bug if sync is enabled:** RemoteWatchHistoryRepository selects `title`. With the migration unapplied, PostgREST returns an error on every history fetch. SyncEngine.syncHistory throws, RemoteSyncException(watch_history) is returned, and history sync never completes. Fix: owner approves applying the migration before sync is wired.
2. **Medium, migration version mismatch:** local files are 20261125000000 and 20261125120000. Remote has 20261124234157 init_sync_schema. The init file header claims it was applied on 2026-11-25, but the remote version stamp is earlier. A future `supabase db push` may see drift. Reconcile the version stamps before the next migration.
3. **Medium, merge key ignores media_type:** SyncMerge keys library rows by `mediaId` only (SyncEngine.kt). The DB unique key is (user_id, media_type, media_id). The same id as movie and series would merge into one row and drop the other.
4. **Medium, offset paging over a random key:** PostgrestClient.selectAll pages with `offset` and `order=id.asc`. `id` is a random uuid, so rows inserted mid-run shift pages and can be skipped or duplicated. Use keyset paging (`id=gt.<last>`).
5. **Medium, response bodies unbounded:** SupabaseAuthRepository.post and PostgrestClient.send call `response.body?.string()` with no size cap. Add a bounded read.
6. **Medium, clock source:** the client sends `updated_at` from the device clock. The DB default now() is bypassed on upsert, so device clock skew decides the winner. The schema has no trigger that sets updated_at server-side.
7. **Low, token writes are not atomic:** `storeSession` writes five prefs keys separately. A crash mid-write can pair a new access token with an old refresh token. `SecureTokenStore.get` also returns null silently on decrypt failure, which reads as signed out.
8. **Low, sync apply race:** `applyLocal` writes remote winners without a conditional check, so a local edit made during a sync run can be overwritten.
9. **Info:** no `Log.*` calls in core or app, so no token logging was found. RemoteSyncException carries table and HTTP code only. No service_role key in client code.
10. **Info:** the `addons` and `plugins` tables have no client code yet.
11. **Info:** Nuvio's library and watch-progress sync go through `sync_*` RPCs with delete events, pending-key protection, origin client id and delta cursors. HUSH uses direct table upserts. Direct upserts are simpler but have no delete path.

## Classification

### TAKE (2)

| ID | Item | Nuvio reference | HUSH target |
|---|---|---|---|
| T1 | Delete propagation (tombstones or delete events, via a `deleted_at` column or an RPC) | features/library/sync/SupabaseLibrarySyncAdapter.kt:83-93 (`deleteItems` via `sync_delete_library_items`). features/watching/sync/SupabaseProgressSyncAdapter.kt:115 | SyncEngine.kt, SyncMerge.kt, new migration (owner approval). Prefer soft delete via UPDATE, which RLS already covers. |
| T2 | Pending local upsert and delete keys must survive a remote snapshot | features/library/LibrarySyncReconciler.kt `reconcileLibrarySnapshot` | SyncEngine.kt, plus a pending-key store |

### ADAPT (9)

| ID | Item | Nuvio reference | HUSH target |
|---|---|---|---|
| A1 | Mutex around refresh, and one shared token path | core/auth/DeviceLinkAuthRepository.kt (refresh flows) | SupabaseAuthRepository.kt `currentSession`, `authorizedSession`, `refresh` |
| A2 | Redirect policy: `followRedirects(false)` or `followSslRedirects(false)`, and reject non-https | features/addons/AddonHttpClient.kt (uses `followRedirects` default true, so this is a deliberate HUSH tightening) | SupabaseAuthRepository.kt:23, RemoteLibraryRepository.kt:15, RemoteWatchHistoryRepository.kt:14 |
| A3 | Keyset paging instead of offset | features/library/sync/LibrarySyncPaging.kt | PostgrestClient.kt `selectAll` |
| A4 | Bounded body reads (cap bytes) | none found in Nuvio; ADAPT from HUSH's own `readUtf8` use | SupabaseAuthRepository.kt `post`, PostgrestClient.kt `send` |
| A5 | Merge key includes media_type | features/library/LibrarySyncReconciler.kt `libraryItemKey(id, type)` | SyncEngine.kt:58 key lambda |
| A6 | Server-side updated_at (trigger) or server time on write | Nuvio sets timestamps server-side through RPCs | new migration (owner approval); RemoteLibraryRepository.kt, RemoteWatchHistoryRepository.kt |
| A7 | Origin client id on writes, to skip own echoes | `putSyncOriginClientId` in SupabaseProgressSyncAdapter.kt | later, with delta sync |
| A8 | Single atomic write for the token blob | none | SupabaseAuthRepository.kt `storeSession`, SecureTokenStore.kt |
| A9 | Delta cursor with event ids (replaces full fetch) | features/library/LibrarySyncReconciler.kt `reconcileLibraryDelta` | later; needs the T1 event log |

### ALREADY IMPLEMENTED (11)

| ID | Item | Where |
|---|---|---|
| I1 | Owner RLS policies with USING and WITH CHECK, using `(select auth.uid())` | init_sync_schema.sql policy block. Remote advisors clean. |
| I2 | anon revoked, authenticated granted only what is needed | init_sync_schema.sql revoke and grant block |
| I3 | Publishable key only in client; no service_role | SupabaseConfig.kt, SupabaseAuthRepository.kt header |
| I4 | https-only guards on auth and PostgREST calls | SupabaseAuthRepository.kt `configured`; PostgrestClient.kt `endpoint` |
| I5 | No token logging; error messages carry no body | No Log calls in core or app; PostgrestClient.kt RemoteSyncException |
| I6 | Sign-out clears tokens and leaves local data alone | SupabaseAuthRepository.kt:76-84; SettingsScreen.kt:73 |
| I7 | Tokens encrypted with AES-GCM in Keystore | SecureTokenStore.kt |
| I8 | Transient network errors keep the session | SupabaseAuthRepository.kt `currentSession` (AuthRejectedException only) |
| I9 | Sync is serialised by a mutex | SyncEngine.kt `mutex.withLock` |
| I10 | Conflict rule defined and unit-tested | SyncMerge.kt; core/data/src/test/.../SyncMergeTest.kt |
| I11 | Upsert conflict targets match the DB unique keys | RemoteLibraryRepository.kt ON_CONFLICT; RemoteWatchHistoryRepository.kt ON_CONFLICT vs init_sync_schema.sql |

### SKIP (4)

| ID | Item | Reason |
|---|---|---|
| S1 | Trakt, Simkl, MDBList, AniList, WeTrakr sync | Outside the Supabase backend scope |
| S2 | Membership and entitlement remote sources | Not in HUSH scope |
| S3 | Device-link auth (TV pairing) | Not planned in HUSH's current milestones |
| S4 | supabase-kt client dependency | HUSH uses hand-rolled OkHttp PostgREST and GoTrue. Keep it that way for now. |

### NEEDS INVESTIGATION (4)

| ID | Item | Question |
|---|---|---|
| N1 | addons and plugins tables | Is a client planned? If not, drop them from the remote schema. |
| N2 | Cleartext traffic | Check the manifest for `usesCleartextTraffic` and any network security config. Not checked in this pass. |
| N3 | applyLocal race (finding 8) | Needs a conditional write design. Needs a test. |
| N4 | Nuvio sync_* RPC definitions | Check for SECURITY DEFINER and the exposed schema before copying the RPC pattern. |

## Advisor findings

- Security: 0 lints.
- Performance: 2 INFO unused indexes (library_items_user_updated_idx, watch_history_user_updated_idx). Expected with 0 rows. Revisit after real traffic.
- Health: not run.

## Top 3 risks

1. Enabling sync before applying the watch_history title migration breaks history sync on every run (finding 1).
2. Concurrent token refresh can sign the user out, because refresh tokens rotate and there is no mutex (A1, confirmed by code).
3. Missing delete propagation (T1) plus the media_type merge-key bug (A5) can resurrect or drop rows once sync runs.

## Suggested next steps (for owner decision, not done here)

- Owner approval to apply the watch_history migration, and to reconcile the migration version stamp.
- Fix A1 (refresh mutex) and A2 (redirect policy) before wiring sync to app start.
- Design T1 (soft delete column, owner-approved migration) and A6 (server updated_at) together.
- Device test after the root theme fix is merged. Runtime claims above stay UNVERIFIED until then.
