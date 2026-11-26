# Playback parity audit (Phase 1)

- **Date:** 2026-11-26, 05:37 UTC
- **HUSH commit audited:** `887cdea` on branch `fix/root-theme-wrap` (read-only; no code changed)
- **Reference:** NuvioMobile-Enhanced `7aca5a018` (`/home/claude/upstream-nuvio`)
- **Runtime status:** all runtime claims are UNVERIFIED. This sandbox has no Android SDK and no device. Findings come from code reading only.

## HUSH player surface (what exists today)

- `core/player/src/main/kotlin/app/hush/placeholder/core/player/HushVideoPlayer.kt` (229 lines). Single URL, Media3 `ExoPlayer` with `PlayerView(useController = false)`.
- Overlay: tap to toggle, 3 s auto-hide while playing, Back / Play-Pause / -10 s / +10 s / seek slider with time text.
- Poll every 500 ms for state. `onProgress` every 5 s and on dispose. `ON_PAUSE` pauses and saves; `ON_RESUME` resumes if it was playing.
- Caller: `app/src/main/kotlin/app/hush/placeholder/ui/HushApp.kt` (lines ~116-175). Saves history on positive positions. Resume uses `startMs` from history (automatic, no prompt).
- Source choice: `ui/sources/SourcesScreen.kt`, then `startPlaying(PlayRequest(url, ...))`. One URL per play. No fallback.
- `app/src/main/AndroidManifest.xml`: no `supportsPictureInPicture`, no foreground service, no media session.
- No `Player.Listener`, no `setAudioAttributes`, no track selection, no speed, no resize control, no gestures beyond tap.

## Nuvio reference (what it does)

- Gestures: `composeApp/.../commonMain/.../features/player/PlayerScreenRuntimeGestureActions.kt` (double-tap seek, horizontal seek preview, brightness and volume swipes, pinch zoom, resize-mode cycle, speed picker, hold-to-speed, lock controls, keyboard shortcuts).
- Errors and reopen: `PlayerEngine.android.kt` `onPlayerError` (line ~1034), `handleSourceError`, decoder fallback; `PlaybackStallResume.kt` (reopen at position once on closed stream); `PlayerSourceFallbackRun.kt` (`resolveWithSourceFallback`, bounded attempts, tried-key set); `PlayerScreenRuntimeSourceFallback.kt`.
- Audio focus: `androidMain/.../PlayerAudioFocus.kt` (GAIN / LOSS / LOSS_TRANSIENT).
- PiP: `androidMain/.../PlayerPictureInPictureManager.android.kt` (auto-enter, aspect ratio from video size, pause on exit).
- Background and notification: `PlayerNowPlayingService.android.kt` (foreground service), `PlayerNowPlayingController.android.kt` (511 lines).
- Tracks and subtitles: `PlayerEngine.kt` interface (`getAudioTracks`, `selectAudioTrack`, `getSubtitleTracks`, `selectSubtitleTrack`, `setSubtitleUri`, `applySubtitleStyle`, `setSubtitleDelayMs`, `setAudioDelayMs`), `SubtitleModal.kt`, `AudioTrackModal.kt`, `StyledSubtitle.kt`.
- Speed: `setPlaybackSpeed` in `PlayerEngine.android.kt` (line ~1499).
- Progress: `PlaybackProgressPersistIntervalMs = 60_000L` (`PlayerScreenSupport.kt`), resume prompt (`features/watchprogress/ResumePromptRepository.kt`).
- Skip intro: `features/player/skip/` (external segment providers).
- Out of scope in Nuvio's player: Cast, Discord, watch party, autosync, libass, FFmpeg audio downmix, trailers, parental guide, external player handoff.

## Classification

Counts (48 rows): **TAKE 10, ADAPT 15, ALREADY IMPLEMENTED 8, SKIP 9, NEEDS INVESTIGATION 6.**

| # | Feature | Class | Reason | HUSH | Nuvio |
|---|---|---|---|---|---|
| 1 | Play / pause | ALREADY IMPLEMENTED | Text button wired to `play()`/`pause()`. Labels only, no icons. | `HushVideoPlayer.kt` L186-193 | `PlayerScreenRuntimeGestureActions.kt` `togglePlayback` |
| 2 | Seek -10 s / +10 s buttons | ALREADY IMPLEMENTED | Clamped to `[0, duration]`. | `HushVideoPlayer.kt` L140-145, L187-197 | `seekBy`, `skipStepMs` |
| 3 | Seek slider | ALREADY IMPLEMENTED (with defect risk) | Seeks on every `onValueChange` while dragging, and the thumb snaps back to the 500 ms poll value. Track-phase rule from `gestures` is broken. Device behaviour UNVERIFIED. | `HushVideoPlayer.kt` L206-215 | `PlayerTimeline.kt` |
| 4 | Overlay show / hide and auto-hide | ALREADY IMPLEMENTED | Tap toggles, 3 s auto-hide while playing. Tap target covers the full area. | `HushVideoPlayer.kt` L109-114, L155-162 | `PlayerControls.kt` |
| 5 | Overlay fade motion | ADAPT | Fade is opacity (compositor-friendly), so the motion is cheap. But it uses default Compose timing, not HUSH motion tokens, and the 500 ms poll recomposes the whole overlay. Read values through lambdas or `derivedStateOf`. | `HushVideoPlayer.kt` L164-169 | `PlayerScreenRuntimeUi.kt` |
| 6 | Double-tap seek (left/right) | TAKE | Standard phone gesture. Nuvio has a seek amount and an overlay indicator. | missing | `handleDoubleTapSeek` (L265) |
| 7 | Horizontal drag seek preview | ADAPT | Useful. Must be restrained, with a single preview label, not a per-frame seek. | missing | `showHorizontalSeekPreview` (L112) |
| 8 | Brightness and volume vertical swipe | ADAPT | Needs `Window.attributes.screenBrightness` for brightness. Nuvio changes player volume; HUSH should decide between that and the system stream. | missing | `showBrightnessFeedback` (L136), `changeVolumeBy` (L171) |
| 9 | Pinch zoom | ADAPT | Compose `detectTransformGestures` plus `PlayerView` scale. Low priority. | missing | `pinchVideoZoom` (L330) |
| 10 | Lock controls | SKIP | Low value for v1 single-phone use. Revisit if needed. | missing | `lockPlayerControls` (L60) |
| 11 | Keyboard shortcuts | SKIP | Phone-first. Revisit for TV or desktop targets only. | missing | `PlayerKeyboardShortcuts.kt` |
| 12 | Resize / aspect-ratio cycle | TAKE | Cheap: `PlayerView.resizeMode` cycle (fit, fill, zoom). HUSH has no control at all. | `PlayerView` default only | `cycleResizeMode` (L306), `PlayerAspectRatio` |
| 13 | Playback speed (picker, cycle) | TAKE | `player.setPlaybackSpeed`. HUSH has none. | missing | `setPlaybackSpeedFromPicker` (L345), `cyclePlaybackSpeed` (L353) |
| 14 | Hold-to-2x speed | TAKE | Standard long-press gesture; cheap once speed exists. | missing | `activateHoldToSpeed` (L365) |
| 15 | Audio track selection | TAKE | HUSH relies on ExoPlayer defaults. Needs a track list and `TrackSelectionOverride`. Language preference is also needed. | missing | `getAudioTracks`, `selectAudioTrack`, `applyAudioLanguagePreferences` |
| 16 | Subtitle track selection (embedded) | TAKE | `PlayerView` renders captions if a default track exists, but the user cannot choose or turn them off. | missing | `getSubtitleTracks`, `selectSubtitleTrack`, `SubtitleModal.kt` |
| 17 | Addon / sidecar subtitles (`setSubtitleUri`) | ADAPT | Depends on the addon subtitle contract and on the CloudStream extension and provider boundary (ADR-018). Do not run extension code to fetch subtitles without a security analysis (rule 5). | missing | `setSubtitleUri`, `AddonSubtitleLoader.kt` |
| 18 | Subtitle styling | ADAPT | Use Media3 `CaptionStyleCompat`. Good place for HUSH glass styling. | missing | `applySubtitleStyle`, `SubtitleAppearanceSettings.kt`, `StyledSubtitle.kt` |
| 19 | Subtitle delay | NEEDS INVESTIGATION | Nuvio has `setSubtitleDelayMs` on its Android engine. Media3 has no standard API. Needs a custom cue shift. | missing | `setSubtitleDelayMs` |
| 20 | Audio delay | NEEDS INVESTIGATION | Nuvio flags `supportsAudioDelay`. The Android path depends on its own renderer. Not standard in Media3. | missing | `setAudioDelayMs`, `PlayerScreenRuntimeAudioDelay.kt` |
| 21 | Audio amplification and centre mix | SKIP | Needs the FFmpeg audio extension (`FfmpegAudioRenderer`). Licence and build cost. Not v1. | missing | `FfmpegAudioDownmix`, `PlayerScreenRuntimeAudioAmplification.kt` |
| 22 | ASS / libass subtitle rendering | SKIP | Heavy native dependency and licence review needed. Not v1. | missing | `PlayerLibassCompat.kt`, `PlayerEngine.android.kt` |
| 23 | Subtitle autosync | SKIP | Large subsystem (`features/autosync`, about 8 files). Not v1. | missing | `features/autosync/` |
| 24 | Resume position on open | ALREADY IMPLEMENTED | Auto-resume from `history.positionMs` through `startMs`. | `HushApp.kt` L117, L195; `HushVideoPlayer.kt` L80 | `PlayerScreenRuntimePlaybackActions.kt` |
| 25 | Resume prompt ("continue at mm:ss or start over") | ADAPT | HUSH resumes silently. Prompt is a UX decision for the owner and the UI team. | missing | `features/watchprogress/ResumePromptRepository.kt` |
| 26 | Progress persistence cadence | ADAPT | HUSH writes every 5 s (`PROGRESS_INTERVAL_MS`). Nuvio writes every 60 s. Keep the 5 s if Room writes are cheap, but measure. The save runs on `recordScope`; confirm that scope outlives the composable (see NEEDS INVESTIGATION). | `HushVideoPlayer.kt` L46, L102-105; `HushApp.kt` L149-166 | `PlaybackProgressPersistIntervalMs` (60 s) |
| 27 | Completed threshold | ALREADY IMPLEMENTED | `COMPLETE_FRACTION` marks completion. | `HushApp.kt` L162 | next-episode rules |
| 28 | Error state (playback failure) | TAKE | **Gap.** No `Player.Listener`. A failed URL shows a black screen with controls and no message or retry. Top-priority gap. | missing | `PlayerEngine.android.kt` `onPlayerError` (L1034), `retry()` |
| 29 | Buffering indicator | TAKE | **Gap.** No `STATE_BUFFERING` handling. The user sees nothing during a stall. | missing | `PlayerLoadingStatus.kt`, engine `STATE_BUFFERING` |
| 30 | Decoder fallback on error | NEEDS INVESTIGATION | Nuvio retries with `EXTENSION_RENDERER_MODE_PREFER`. Media3 default renderers differ. Needs a device test. | missing | `PlayerEngine.android.kt` L1004-1021 |
| 31 | Stream failover (try next source) | ADAPT | **Gap.** One URL per play. Failover should use `MediaRepository.streams` candidates with a bounded attempt count and a tried-key set. Must respect ADR-018 (per-call provider isolation). CloudStream extension compatibility applies (D-013 waives only the legal API-name review, not the security analysis). | `ui/details/PlayRequest.kt`; `HushApp.kt` | `PlayerSourceFallbackRun.kt`, `PlayerScreenRuntimeSourceFallback.kt` |
| 32 | Reopen at position after a stall | ADAPT | Stall reopen (closed stream or out-of-range read) once per session is a good rule. Depends on #31 and #28. | missing | `PlaybackStallResume.kt` |
| 33 | Audio focus | TAKE | **Gap.** `ExoPlayer.Builder` is built without `setAudioAttributes(..., handleAudioFocus = true)`. HUSH does not hold focus, so another app's audio plays over the video and nothing pauses on a call. Fix is a builder change. | `HushVideoPlayer.kt` L79-83 | `PlayerAudioFocus.kt` |
| 34 | Pause on headphone unplug | ALREADY IMPLEMENTED | Media3 handles becoming-noisy by default when the builder keeps its defaults. Confirm in the Media3 version pinned by the build. UNVERIFIED on device. | `HushVideoPlayer.kt` L79 | Nuvio uses the same builder |
| 35 | Picture-in-picture | TAKE | **Gap.** No PiP support declared (`supportsPictureInPicture` and `onUserLeaveHint`). Activity config also needs `configChanges` handling. | `AndroidManifest.xml`; `MainActivity.kt` | `PlayerPictureInPictureManager.android.kt` |
| 36 | Background audio and lock-screen controls | ADAPT | HUSH pauses on every `ON_PAUSE`, so background audio is off and there are no media controls. Owner must decide policy first. If background audio is on, a foreground service with `mediaPlayback` type and a media session are needed (Android 14+ rules). | `HushVideoPlayer.kt` L116-132 | `PlayerNowPlayingService.android.kt`, `PlayerNowPlayingController.android.kt` |
| 37 | Lifecycle (foreground) | ALREADY IMPLEMENTED | Pause and save on `ON_PAUSE`, resume on `ON_RESUME` if it was playing. Release on dispose. | `HushVideoPlayer.kt` L116-132, L85-89 | `PlayerScreenRuntimeEffects.kt` |
| 38 | Keep screen on during playback | NEEDS INVESTIGATION | Not set in HUSH. Check whether `PlayerView` keeps the screen awake on the pinned Media3 version, or set `keepScreenOn` explicitly. | missing | not verified in Nuvio read |
| 39 | Next episode and auto-play | ADAPT | `episodeId` is passed through `PlayRequest`, but no next-episode flow exists. Needs episode data from `MediaRepository`. | `PlayRequest.kt` | `PlayerNextEpisodeAutoPlay.kt`, `PlayerNextEpisodeRules.kt` |
| 40 | Skip intro / outro segments | SKIP | External segment providers (AniSkip, MAL, Kitsu, etc.). Privacy, licence and provider isolation review first. Not v1. | missing | `features/player/skip/` |
| 41 | Chapters | NEEDS INVESTIGATION | Nuvio reads chapters through its own reader. Media3 support depends on container. Low priority. | missing | `PlayerChapterReader.kt`, `PlayerChaptersPanel.kt` |
| 42 | Media info panel | ADAPT | Low priority. Read `Format` from the player. | missing | `PlayerMediaInfo.kt` |
| 43 | Sources panel during playback | ADAPT | HUSH picks a source before playback. An in-player source switch is needed for #31. | `SourcesScreen.kt` | `PlayerSourcesPanel.kt` |
| 44 | Offline downloads and offline playback | NEEDS INVESTIGATION | Blocked on ADR-019 (owner legal question). | `DECISION_LOG.md` (ADR-019 PROPOSED) | `features/downloads/` |
| 45 | Cast | SKIP | Google Cast dependency. Not v1. | missing | `PlayerCastBridge.android.kt` |
| 46 | External player handoff | SKIP | Not v1. Cheap later if wanted. | missing | `ExternalPlayerLaunchCoordinator.kt` |
| 47 | Watch party, Discord presence, trailers, parental guide | SKIP | Out of scope for HUSH v1 playback. | missing | `PlayerScreenRuntimeParty.kt`, `PlayerScreenRuntimeDiscord.kt`, `TrailerPlayer.kt`, `ParentalGuideOverlay.kt` |
| 48 | Liquid Glass control surfaces | ADAPT | HUSH already has `GlassSurface` and `HushMotion` in `design/`. Use them for the overlay instead of plain black alpha. Check the reduced-glass path. | `design/GlassSurface.kt`, `design/HushMotion.kt`, `HushVideoPlayer.kt` L170 | `PlayerLiquidGlass.kt` |

## Failover and ADR-018 notes

- ADR-018 (provider failure isolation per call) is referenced in `DECISION_LOG.md` L77. The full ADR text is not in the repo docs I read (`docs/architecture/DECISIONS.md` has no ADR-018 heading). Treat its exact rules as UNVERIFIED until read.
- CloudStream extensions produce sources through their own code. Failover must call each candidate through the provider port, one call at a time, with a timeout and no shared state. Extension code runs only after the security analysis required by rule 5.
- The stall-reopen logic (#32) is safe to port in principle, but it should be written fresh against HUSH's own state, not copied (GPL attribution rule applies if any Nuvio file is copied).

## Top 3 playback gaps

1. **No error, buffering or failover handling (#28, #29, #31).** A failed stream gives a silent black screen, and the user must go back and pick another source by hand.
2. **No audio focus and no background, PiP or media-session policy (#33, #35, #36).** Other apps' audio plays over the video, PiP is absent, and the pause-on-any-pause rule removes background audio and lock-screen controls. The owner must set the background-audio policy.
3. **No track, speed or aspect controls and no gestures (#6, #12, #13, #15, #16).** Users cannot change audio or subtitle tracks, speed or framing, and there is no double-tap seek.

## Other observations

- `docs/PROJECT_STATUS.md` says "Implemented features: None (UI, playback, persistence and compat are all unimplemented)". That conflicts with `HushVideoPlayer.kt` and the wiring in `HushApp.kt`. Status doc needs updating (not changed here).
- `recordScope` in `HushApp.kt` is used from `onDispose`-triggered progress callbacks. Its lifetime was not confirmed in this audit (NEEDS INVESTIGATION). If it is cancelled with the composition, the final save on exit could be lost.
- Seek slider: see row #3. Also check that the auto-hide timer does not hide controls mid-drag (the `LaunchedEffect` keys do not restart on drag).

## Verification status

- Code reading only. No build run (no Android SDK in sandbox).
- No device run. Every "device behaviour" statement above is UNVERIFIED.
- No code edited, committed or pushed.
