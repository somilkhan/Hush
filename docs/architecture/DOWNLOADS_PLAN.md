# Downloads — slice plan (D-018)

**Date:** 2026-11-26 · **Source:** NuvioMobile-Enhanced `7aca5a0` (GPL-3.0, owner's fork) · **Status:** PLAN ONLY. No downloads code is built yet.

Owner decision (D-015 item 6): follow what Nuvio does for downloads; no separate lawyer review.

## Attribution rule (GPL-3.0)
Every ported file carries a header: `Adapted from NuvioMobile-Enhanced (GPL-3.0), commit 7aca5a0, path <original path>.` Keep the GPL-3.0 notice. Do not copy `com.nuvio` identifiers into new code; rename the package to `app.hushstream`.

## Slices (Nuvio line counts)

| # | Slice | Nuvio files (lines) | Class | Depends on |
|---|---|---|---|---|
| S1 | Domain model and queue policy | `DownloadsModels` (197), `DownloadQueuePolicy` (56), `DownloadDisplay` (49), `DownloadStatusTexts` (41), `DownloadSpeeds` (38) | ADAPT | Room (D-017) |
| S2 | Storage and settings | `AndroidDownloadStore` (92), `DownloadsStorage.android` (67), `DownloadsSettingsStorage.android` (68), `DownloadsSettingsRepository` (101), `DownloadsStorageStats.android` (27) | ADAPT | S1 |
| S3 | Transfer engine | `AndroidDownloadTransfer` (119), `DownloadsPlatformDownloader.android` (170), `DownloadsTransferWorker` (39), `DownloadsTransferJobService` (71), `AndroidDownloadScheduler` (381), `DownloadsRepository` (842) | ADAPT · NEEDS INVESTIGATION (WorkManager vs JobService) | S1, S2, network guard |
| S4 | Network guard | `DownloadNetworkGuard` (11 + 50) | ADAPT | none |
| S5 | Subtitles and artwork | `DownloadSubtitles` (109), `DownloadArtwork` (110), `DownloadSubtitleStorage.android` (41) | ADAPT | S3 |
| S6 | Download UI | `DownloadsScreen` (500), `DownloadRows` (359), `DownloadComponents` (229), `DownloadActionSheet` (164), `DownloadDeleteConfirmation` (66), `DownloadsQueueDialog` (747), `DeviceStorageBar` (183) | ADAPT (restyle to HUSH glass tokens) | S1–S3 |
| S7 | Batch downloads | `BatchDownloadCoordinator` (476), `BatchDownloadQueuer` (153), `BatchDownloadDialog` (653) | ADAPT | S3 |
| S8 | Offline metadata and Library integration | `OfflineShowMetadata` (340), `LibraryDownloadedContent` (460), `LibraryDownloadsButton` (97), `EpisodeDownloadIndex` (42), `DownloadWatchStatus` (37) | ADAPT | S3, S6 |
| S9 | Notifications, export, live status | `DownloadsNotificationActionReceiver` (44), `AndroidDownloadExport` (142), `AndroidDownloadSink` (72), `DownloadsLiveStatusPlatform.android` (300) | ADAPT · NEEDS INVESTIGATION (notification permission, Android 13+) | S3 |
| S10 | Settings page | `DownloadsSettingsPage` (190 + 39), `DownloadsSettingsScreen` (33) | ADAPT | S2 |

Total about 8,200 lines across 71 files.

## Order and gates
1. S1 then S2 (no network; unit-testable pure logic first).
2. S4 then S3 (transfer needs the network guard). Decide WorkManager vs JobService before coding S3 (NEEDS INVESTIGATION).
3. S5, S6, S7, S8, S9, S10 in that order.

Each slice: commit on the feature branch, CI green, DECISION_LOG entry with the CI run ID.

## Open items
- S3: WorkManager adds a dependency. Decide before coding.
- S9: Android 13+ notification permission flow. Needs owner check on minSdk 24 behaviour.
- Device testing for all slices is gated by ADR-022 (owner's device testing around 70% completion).
