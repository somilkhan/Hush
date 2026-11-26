# Sync status (as of 2026-11-27)

| Item | Status | Needs |
|---|---|---|
| Merge rule (newer updatedAt wins, tie to remote) | Done, unit tested (SyncMergeTest) | — |
| Tombstone fields on LibraryEntry, WatchEntry | Done (feature/sync-tombstones-trigger) | — |
| Tombstone merge rules | Done, unit tested (SyncTombstoneTest) | — |
| Library tombstone applied locally (removes row) | Done in engine; local repo still hard-deletes, so no local tombstone is produced yet | Room tombstone column (DB v3) |
| History tombstones | Not applied (no remove API on WatchHistoryRepository) | Add remove API |
| Remote tombstone column | NOT DONE | Supabase migration. Owner approval. Not applied. |
| Single sync trigger (SyncCoordinator.onSignedIn) | Done | Call site in app/ (Android Core / app owner) |
| Sync wired at app start | NOT DONE | Call site |
| watch_history.title remote migration | Local file exists, NOT applied remotely | Owner approval |

Verification: unit tests in core/data (CI). Device and Supabase runtime: UNVERIFIED.
