# HUSH vs NuvioMobile-Enhanced: UI/UX parity audit (Phase 1)

- Audited: 2026-11-26T05:37:36Z (UTC, sandbox clock)
- HUSH: `/home/claude/hush`, branch `fix/root-theme-wrap`, commit `887cdea78a7413ba9a4ce40f6aa0b767ad7df50c`
- Reference: `/home/claude/upstream-nuvio`, commit `7aca5a01896cfdf244f39660dd4d2c7e685b7dbb`
- Method: static code reading only. No build, no run, no screenshots. Every visual claim is UNVERIFIED. Contrast figures are computed from the hex values in `HushColors.kt` (WCAG formula), not measured on device.
- Scope: read-only. Out of scope (SKIP): Party, Discord, Trakt, Live TV.
- Nuvio paths are relative to `composeApp/src/commonMain/kotlin/com/nuvio/app/`. HUSH paths are relative to `app/src/main/kotlin/app/hush/placeholder/` (UI) and `design/src/main/kotlin/app/hush/placeholder/design/` (tokens).
- Nuvio items marked "skimmed" were classified from the file inventory and top-level composable names, not read line by line.

## Classification key

- TAKE: adopt the pattern in HUSH's own style.
- ADAPT: adopt the idea, change it to fit HUSH's contract or brand.
- ALREADY IMPLEMENTED: HUSH has an equivalent. Defects, if any, are listed in the defects section.
- SKIP: out of scope, decorative, or conflicts with the restrained brand or the no-invented-data rule.
- NEEDS INVESTIGATION: depends on a product decision, an unaudited HUSH contract, or an open licence question (ADR-019).

## Inventory and classification

### A. Shell, navigation and chrome

| # | Area | Class | Nuvio | HUSH | Reason |
|---|---|---|---|---|---|
| A1 | Bottom tab bar (4 tabs, icon + label, selected tint) | ALREADY IMPLEMENTED | `core/ui/NavigationBar.kt`, `AppShellComponents.kt` | `ui/common/HushComponents.kt` (`HushNavBar`), `ui/HushApp.kt` | Equivalent tab bar exists with `Role.Tab` and 48dp+ targets. |
| A2 | Floating glass nav bar (content extends under bar) | ADAPT | `core/ui/FloatingNavigationBar.kt`, `core/ui/glass/LiquidGlassNavigationSurfaces.kt` | `design/GlassSurface.kt`, `HushNavBar` | HUSH bar is edge-to-edge and content stops above it (`Column` with `weight(1f)`). Apple guidance wants content under chrome with a scroll edge effect. |
| A3 | Glass blur behind chrome | ADAPT | `core/ui/glass/LiquidGlass.kt` (uses a backdrop blur library) | `design/GlassSurface.kt` | HUSH's `GlassSurface` explicitly does not blur (tint + 1dp rim). Adopting real blur needs a licence check on the backdrop library before adding it. |
| A4 | Top bar with title and back control | ALREADY IMPLEMENTED | `core/ui/Components.kt` (skimmed) | `HushComponents.kt` (`GlassTopBar`, `HushBackButton`) | Title is a heading for TalkBack; insets handled. No scroll-linked behaviour (see A5). |
| A5 | Scroll-linked floating header | ADAPT | `features/details/components/DetailFloatingHeader.kt` | `GlassTopBar` used on Details | HUSH header is static. A fade or condense on scroll is a later polish item. |
| A6 | Login to main gate | ALREADY IMPLEMENTED | `features/auth/AuthScreen.kt` | `ui/HushApp.kt` (`Gate`), `ui/login/LoginScreen.kt` | State-based gate exists; guest path exists. |
| A7 | State-based back handling (player, details, sources, tabs) | ALREADY IMPLEMENTED | `DetailsDestinations.kt`, `DownloadsDestinations.kt`, `SettingsDestinations.kt`, `core/ui/PlatformBackHandler.kt` | `ui/HushApp.kt` (`BackHandler` x3) | Single-level back walks one state up. Nuvio has deeper settings pages (see H-section). |
| A8 | Edge-to-edge system insets | ALREADY IMPLEMENTED | `core/ui/PlatformInsets.kt` | `GlassTopBar` and `HushNavBar` use `windowInsetsPadding` | Status and navigation bar insets applied. UNVERIFIED on device. |
| A9 | Visible focus ring (D-pad, keyboard) | ALREADY IMPLEMENTED | `core/ui/Components.kt` (skimmed) | `HushComponents.kt` (`focusRing`), `PosterCard.kt` | Present on tabs, poster tiles and list rows. Missing on Home continue-watching tiles (see defects). |
| A10 | Tab-level app-shell layout and nav-bar settings sheet | SKIP | `features/settings/NavigationBarSettingsSheet.kt`, `AppScreenTab.kt` | n/a | HUSH has four fixed tabs. Customisable nav is not needed yet. |
| A11 | Player surface (full-screen player, back closes it) | NEEDS INVESTIGATION | `features/player/PlayerScreen.kt` (121 lines) and about 60 `PlayerScreen*.kt` / `Player*.kt` files | `ui/HushApp.kt` calls `HushVideoPlayer` in `core/player` (not audited) | HUSH's player implementation was not read in this phase. |
| A12 | Player controls overlay, gestures and tracks | NEEDS INVESTIGATION | `features/player/PlayerControls.kt`, `PlayerGestureOverlay.kt`, `PlayerOverlays.kt`, `PlayerQualityPanel.kt`, `AudioTrackModal.kt` | `core/player/*` (not audited) | Depends on A11. |
| A13 | Player episode and chapter panels | NEEDS INVESTIGATION | `features/player/PlayerEpisodesPanel.kt`, `PlayerChaptersPanel.kt` | none | Needs a player audit first. |
| A14 | Player Discord, Party and Live TV panels | SKIP | `PlayerScreenRuntimeDiscord.kt`, `PlayerScreenRuntimeParty.kt`, `PlayerLiveChannelsPanel.kt`, `features/party/*`, `features/livetv/*` | none | Named out of scope. |

### B. Home

| # | Area | Class | Nuvio | HUSH | Reason |
|---|---|---|---|---|---|
| B1 | Home hero with primary action | ADAPT | `features/home/HomeScreen.kt` (2137 lines), `components/HomeHeroSection.kt` | `ui/home/HomeScreen.kt`, `ui/common/HeroBanner.kt` | HUSH hero works but has no artwork (contract), and its "Play" button opens Details instead of playing (see defects). |
| B2 | Continue watching row | ALREADY IMPLEMENTED | `components/HomeContinueWatchingSection.kt`, `core/ui/ContinueWatchingText.kt` | `ui/home/HomeScreen.kt` (`ContinueWatchingRow`) | Row with 3dp progress bar and resume. No time-left text. |
| B3 | Continue watching long-press action sheet | TAKE | `core/ui/ContinueWatchingActionSheet.kt` | none | Remove from history or go to details. Local data only, so it fits HUSH. |
| B4 | Catalogue rows per addon catalogue | ADAPT | `components/HomeCatalogSection.kt`, `HomeCatalogParser.kt`, `HomeCatalogSettingsRepository.kt` | HUSH has one "More titles" rail | HUSH's contract returns one featured list. Multi-row catalogues need the contract to grow. |
| B5 | Collection rows on Home | SKIP | `components/HomeCollectionRowSection.kt`, `features/collection/*` | none | Depends on the Collections sync model (see K-section). |
| B6 | Loading skeleton | ALREADY IMPLEMENTED | `components/HomeSkeletonLoading.kt`, `core/ui/Skeleton.kt`, `Shimmer.kt` | `HushComponents.kt` (`LoadingRows`, static) | HUSH deliberately has no shimmer. That matches the Floor (no looping motion). |
| B7 | Empty, error and retry states | ALREADY IMPLEMENTED | `components/HomeStateCards.kt` | `HushComponents.kt` (`StateMessage`) | Empty, error and retry all present. |
| B8 | Offline card | TAKE | `core/ui/NetworkOfflineCard.kt` | none | HUSH has no offline state. Required by the Floor (offline is a normal state). |
| B9 | Home scroll position restore | NEEDS INVESTIGATION | `features/home/HomeScrollPosition.kt` | none | Low priority. Needs a check that it does not fight the tab state. |
| B10 | Hero stretch overscroll | NEEDS INVESTIGATION | `core/ui/HeroStretchOverscroll.kt` | none | Motion effect. Check against Reduce Motion before any adoption. |
| B11 | Dynamic colour from artwork behind hero | SKIP | `core/ui/DynamicArtworkBackground.kt`, `ArtworkDominantColor.kt`, `BackdropFocus.kt` | none | HUSH has no artwork contract. Revisit once artwork exists. |
| B12 | Ambient and mesh backgrounds | SKIP | `core/ui/background/AppBackground.kt`, `core/ui/ProfileMeshBackground.kt`, `core/ui/AshSwarmRenderer.kt` | none | Decorative. Conflicts with the restrained brand direction. |

### C. Poster and cards

| # | Area | Class | Nuvio | HUSH | Reason |
|---|---|---|---|---|---|
| C1 | Poster card (2:3 tile, title, kind and year) | ADAPT | `components/HomePosterCard.kt`, `core/ui/PosterCardStyleCompose.kt`, `PosterCardDimensions.kt` | `ui/common/PosterCard.kt` | HUSH tile is a tonal block with an initial. Honest, but no artwork. Accepted until the contract provides images. |
| C2 | Poster grid | ALREADY IMPLEMENTED | `components/PosterGrid.kt` | `Adaptive(150.dp)` `LazyVerticalGrid` in Home, Library, Search | Equivalent exists. |
| C3 | Poster long-press action sheet | TAKE | `core/ui/PosterActionSheet.kt`, `PosterZoomActionOverlay.kt` | none | Standard mobile pattern. Start with add/remove from library and open details. |
| C4 | Card depth and lift effects | SKIP | `core/ui/CardDepthEffect.kt`, `PosterLift.kt`, `CardDepthStyleRepository.kt` | none | Decorative 3D. Not part of the brand direction. |
| C5 | Poster open motion | SKIP | `core/ui/PosterOpenMotion.kt` | none | Shared-element motion is a later question. Do not add motion before the Floor's Reduce Motion path exists. |
| C6 | Custom poster editor | SKIP | `core/poster/CustomPosterScreen.kt` | none | Needs artwork and storage decisions. |

### D. Details

| # | Area | Class | Nuvio | HUSH | Reason |
|---|---|---|---|---|---|
| D1 | Details page (hero, title, meta, synopsis, actions) | ADAPT | `features/details/MetaDetailsScreen.kt` (2782 lines), `components/DetailHero.kt`, `DetailMetaInfo.kt`, `DetailActionButtons.kt` | `ui/details/DetailsScreen.kt` | HUSH shows kind, year, synopsis and save. Missing runtime, rating, genres (depends on the MediaItem contract, not audited). |
| D2 | Expandable description | TAKE | `components/ExpandableDescription.kt` | `DetailsScreen.kt` shows the full synopsis | Long synopses push episodes off screen. Clamp with a "More" control. |
| D3 | Save to library toggle | ALREADY IMPLEMENTED | `components/DetailActionButtons.kt`, `features/library/LibraryListManagementButton.kt` | `DetailsScreen.kt` (`SaveToggle`) | Single saved list. Multi-list is NEEDS INVESTIGATION (see E4). |
| D4 | Season selector | ADAPT | `components/DetailSeriesContent.kt`, `DetailEpisodeNavigator.kt`, `SeasonViewMode.kt` | `DetailsScreen.kt` (`SeasonRow`, button row) | Works. Lacks Nuvio's episode navigator and view modes. |
| D5 | Episode rows with per-episode action | ADAPT | `features/details/components/EpisodeWatchedActionSheet.kt`, `EpisodeAvailabilityBadge.kt` | `DetailsScreen.kt` (episode `Row` with Play) | Add a watched/unwatched action sheet. Availability badge only if the contract supplies it. |
| D6 | Cast and people | NEEDS INVESTIGATION | `components/DetailCastSection.kt`, `features/details/PersonDetailScreen.kt`, `PersonDetail.kt` | none | Depends on whether `MediaItem` carries cast. Not verified in this phase. |
| D7 | Trailers | SKIP | `components/DetailTrailersSection.kt`, `HeroTrailerPlayerSurface.kt`, `TrailerPlayerPopup.kt` | none | No trailer source in HUSH's contract; large player surface. |
| D8 | Comments | SKIP | `components/DetailCommentsSection.kt`, `CommentDetailSheet.kt` | none | Needs a backend. Not in HUSH scope. |
| D9 | Episode ratings and OMDb | SKIP | `features/details/OmdbEpisodeRatingsService.kt`, `components/DetailEpisodeRatingMap.kt`, `features/ratings/UserRatingSheet.kt` | none | Third-party API key and terms. Out of scope for the UI phase. |
| D10 | Production and additional info | NEEDS INVESTIGATION | `components/DetailProductionSection.kt`, `DetailAdditionalInfoSection.kt` | none | Depends on contract fields. |
| D11 | Tablet detail hero | NEEDS INVESTIGATION | `components/TabletDetailHero.kt` | none | HUSH has no tablet layout pass yet (mobile-design: adapt for expanded width). |
| D12 | Stream picker before play | TAKE | `features/streams/StreamsScreen.kt` (1635 lines), `StreamActionsSheet` | none (`DetailsScreen` plays `streams().firstOrNull()`) | HUSH silently picks the first stream. Users need to choose and see why a stream failed. |
| D13 | Stream loading state | ADAPT | `features/streams/StreamLoadingScreen.kt` (`StreamLoadingScreen.kt` at root) | none | Show progress while streams resolve, and an empty-streams message. |
| D14 | Batch download from details | NEEDS INVESTIGATION | `features/details/components/BatchDownloadDialog.kt` | none | Blocked on ADR-019 (downloads licence). |
| D15 | Person and TMDB entity browse | SKIP | `features/details/TmdbEntityBrowseScreen.kt` | none | TMDB dependency. |

### E. Library

| # | Area | Class | Nuvio | HUSH | Reason |
|---|---|---|---|---|---|
| E1 | Saved library grid | ADAPT | `features/library/LibraryScreen.kt` (3611 lines), `LibraryListRows.kt`, `LibrarySavedContent.kt` | `ui/library/LibraryScreen.kt` (`SavedTile`) | HUSH grid works. No sort, filter or list segmentation. |
| E2 | Loading state | ADAPT | Nuvio uses skeleton rows in its library | `LibraryScreen.kt` shows bare text "Loading your library" | Replace with `LoadingRows` for consistency with Home. |
| E3 | Library sort and display options | NEEDS INVESTIGATION | `features/library/LibraryDisplaySettings.kt`, `LibrarySortEffect.kt` | none | Product decision on what to sort by. |
| E4 | Multiple library lists and list management | NEEDS INVESTIGATION | `features/library/LibraryListManagementDialog.kt`, `LibraryListEditor.kt`, `LibraryListManagementController.kt` | none (one saved list) | Product decision. HUSH's current single list is simpler and honest. |
| E5 | Downloaded content inside library | NEEDS INVESTIGATION | `features/library/LibraryDownloadedContent.kt`, `LibraryDownloadsButton.kt` | none | Blocked on ADR-019. |
| E6 | Release calendar panel | SKIP | `features/library/LibraryReleaseScheduleStorage.kt`, `LibraryReleaseCalendarPanel` in `LibraryScreen.kt` | none | Needs release-date data that HUSH's contract does not have. |
| E7 | Library empty state | ALREADY IMPLEMENTED | Nuvio empty state in `LibraryScreen.kt` | `StateMessage` "Nothing saved yet" with guidance | Copy tells the user how to fill it. |

### F. Search

| # | Area | Class | Nuvio | HUSH | Reason |
|---|---|---|---|---|---|
| F1 | Search field and results grid | ALREADY IMPLEMENTED | `features/search/SearchScreen.kt` (581 lines) | `ui/search/SearchScreen.kt`, `SearchViewModel.kt` | Field, submit, loading, empty, error and retry all present. |
| F2 | Recent searches | TAKE | `features/search/SearchRecentSection` (in `SearchScreen.kt`), `SearchHistoryRepository.kt`, `SearchHistoryStorage.kt` | none | Local-only history fits HUSH's guest and device-local model. |
| F3 | Discover browse on search | NEEDS INVESTIGATION | `features/search/SearchDiscoverContent.kt`, `DiscoverSelectionStorage.kt` | none | Depends on catalogue sources. |
| F4 | Clear-query control and focus on open | ADAPT | Nuvio search field (skimmed) | `SearchScreen.kt` `OutlinedTextField` with no trailing clear and no focus request | Small usability gap. |

### G. Addons and sources

| # | Area | Class | Nuvio | HUSH | Reason |
|---|---|---|---|---|---|
| G1 | Addon manager (add by manifest, list, remove) | ALREADY IMPLEMENTED | `features/addons/AddonsScreen.kt` (669 lines), `settings/AddonsSettingsPage.kt` | `ui/sources/SourcesScreen.kt` | Core flow present. Defects in the defects section (remove without confirm, contrast). |
| G2 | Addon reorder, enable and disable | NEEDS INVESTIGATION | `features/addons/AddonRepository.kt` (skimmed) | none | Product decision. |
| G3 | Tracking addon consent | NEEDS INVESTIGATION | `features/addons/TrackingAddonConsentDialog.kt` | none | Tracking is not in the named out-of-scope list. Owner scope call needed. |
| G4 | Debrid, cloud library and torrent (P2P) consent | NEEDS INVESTIGATION | `features/debrid/*`, `features/cloud/*`, `features/p2p/P2pConsentDialog.kt` | none | Not in the named out-of-scope list. Also a security and licence question (Floor rule 5). |

### H. Settings

| # | Area | Class | Nuvio | HUSH | Reason |
|---|---|---|---|---|---|
| H1 | Settings root and sections | ADAPT | `features/settings/SettingsScreen.kt` (1345 lines), `SettingsFullScreenPages.kt` | `ui/settings/SettingsScreen.kt` (Account, Sources, About on one scroll) | HUSH has three sections. Nuvio uses sub-pages per topic. |
| H2 | Appearance (theme, glass, poster style) | ADAPT | `features/settings/AppearanceSettingsPage.kt`, `AppearanceThemePicker.kt`, `core/ui/glass/LiquidGlassEffectSettings.kt`, `PosterCardStyleRepository.kt` | none in UI. `HushTheme(glassMode)` exists in `design/HushTheme.kt` but `HushApp` never passes it | The glass mode enum is unreachable. Add a Settings > Appearance row. |
| H3 | Account and auth (sign in, sign out, server) | ADAPT | `features/settings/AccountSettingsPage.kt`, `features/auth/ServerConnectionDialogs.kt` | `SettingsScreen.kt` Account section | HUSH has sign in, sign out and guest copy. Server selection is out of scope. Sign-out policy is pending (owner decision). |
| H4 | Auth screen | ADAPT | `features/auth/AuthScreen.kt` (1232 lines), `DeviceLinkAuthSection.kt` | `ui/login/LoginScreen.kt` (118 lines) | See defects. Device link is NEEDS INVESTIGATION (auth scope). |
| H5 | Downloads settings | NEEDS INVESTIGATION | `features/settings/DownloadsSettingsScreen.kt`, `features/downloads/DownloadsSettingsRepository.kt` | none | Blocked on ADR-019. |
| H6 | Advanced settings and DNS | SKIP | `features/settings/AdvancedSettingsPage.kt`, `core/network/DnsSettings.kt` | none | Power-user surface. Defer. |
| H7 | About page | ALREADY IMPLEMENTED | `features/settings/AboutSettingsPage.kt` | `SettingsScreen.kt` About section | HUSH's About names the licence and the working name. See defects for copy. |
| H8 | What's new | SKIP | `features/whatsnew/WhatsNewScreen.kt` | none | Needs a release-notes source. |
| H9 | App language | NEEDS INVESTIGATION | `features/settings/AppLanguage.kt` | none (hard-coded English strings) | Depends on whether HUSH adopts string resources. |
| H10 | App icon picker | SKIP | `features/settings/AppIconPicker.kt`, `AppIconOption.kt`, `AppIconResources.kt` | none | Icon identity is an owner decision (brand direction: distinctive icon, not yet designed). |
| H11 | Playback and player toolbar settings | NEEDS INVESTIGATION | `features/settings/PlaybackSettingsDialogs.kt`, `PlayerToolbarSettingsDialog.kt`, `AutoSkipSegmentSelectionDialog.kt` | none | Depends on A11 to A13. |
| H12 | Tracking sign-in sheets (Simkl, AniList, Plex, Silo) | NEEDS INVESTIGATION | `features/settings/SimklSyncInfoDialog.kt`, `features/anilist/*`, `PlexSignInSheet.kt`, `SiloSignInSheet.kt`, `TrackingDeviceCodeSheet.kt` | none | Not in the named out-of-scope list. Owner scope call. |

### I. Profiles

| # | Area | Class | Nuvio | HUSH | Reason |
|---|---|---|---|---|---|
| I1 | Profile selection | NEEDS INVESTIGATION | `features/profiles/ProfileSelectionScreen.kt` (681 lines), `ProfileSwitcherTab.kt` | none | Multi-profile is a product decision. HUSH is single-user. |
| I2 | Profile edit, avatar, PIN and biometric lock | SKIP | `features/profiles/ProfileEditScreen.kt`, `PinEntryDialog.kt`, `ProfileBiometricAuth.kt`, `AvatarPicker.kt` | none | Depends on I1. Crypto and biometric scope too large for the UI phase. |

### J. Downloads

| # | Area | Class | Nuvio | HUSH | Reason |
|---|---|---|---|---|---|
| J1 | Downloads screen and rows | NEEDS INVESTIGATION | `features/downloads/DownloadsScreen.kt` (500 lines), `DownloadRows.kt`, `DeviceStorageBar.kt` | none | Blocked on ADR-019 licence decision. |
| J2 | Download action sheet and queue dialog | NEEDS INVESTIGATION | `features/downloads/DownloadActionSheet.kt`, `DownloadsQueueDialog.kt` | none | Same block as J1. |
| J3 | Destructive delete confirmation | TAKE | `features/downloads/DownloadDeleteConfirmation.kt` | none | Use the same pattern for source removal (see defects). |

### K. Collections

| # | Area | Class | Nuvio | HUSH | Reason |
|---|---|---|---|---|---|
| K1 | Collection management, editor, folder detail | SKIP | `features/collection/CollectionManagementScreen.kt` (421 lines), `CollectionEditorScreen.kt`, `FolderDetailScreen.kt`, `CollectionSyncService.kt` | none | Depends on a remote sync model and JSON preservation. Not in HUSH's current contract. |
| K2 | Catalogue screen (browse one catalogue) | NEEDS INVESTIGATION | `features/catalog/CatalogScreen.kt` (431 lines) | none | Depends on B4 contract. |

### L. Shared components and design system

| # | Area | Class | Nuvio | HUSH | Reason |
|---|---|---|---|---|---|
| L1 | Design tokens (colour, type, spacing, shape, motion) | ALREADY IMPLEMENTED | `core/ui/Tokens.kt`, `TypeScale.kt`, `ThemeColors.kt` | `design/HushColors.kt`, `HushTypography.kt`, `HushSpacing.kt`, `HushShapes.kt`, `HushMotion.kt` | HUSH has a complete, smaller token set. Typography enforces min 12sp and no weight below Regular. |
| L2 | Light and dark theme with system follow | ALREADY IMPLEMENTED | `core/ui/Theme.kt` | `design/HushTheme.kt` | Both exist. Dark mode contrast: see defects. |
| L3 | Bottom sheet, dialog, menu, chip | TAKE | `core/ui/BottomSheet.kt`, `NativeBottomSheet.kt`, `Dialog.kt`, `Menu.kt`, `Chip.kt`, `DropdownChip.kt`, `FloatingPrompt.kt` | none | HUSH has no sheet, dialog, menu or chip primitive. Needed for C3, D5 and J3. Build them in `design/` with HUSH styling. |
| L4 | Loading and progress indicators | ALREADY IMPLEMENTED | `core/ui/LoadingIndicator.kt`, `ProgressBar.kt`, `Slider.kt` | `HushComponents.kt` (`LoadingRows`), progress bar in `HomeScreen.kt` | Static loading rows exist. No slider yet (player-dependent). |
| L5 | Shimmer and skeleton animation | SKIP | `core/ui/Shimmer.kt`, `Skeleton.kt` | none | Looping motion is banned by HUSH's Floor. |
| L6 | Playful jelly tabs and springs | SKIP | `core/ui/jelly/JellyTabs.kt`, `JellyMotion.kt`, `JellySpring.kt`, `JellyDrawing.kt` | none | Conflicts with the restrained brand direction. |
| L7 | Disintegration and ash effects | SKIP | `core/ui/DisintegrationEffect.kt`, `AshSwarmRenderer.kt`, `ScopedDisintegrationTracker.kt` | none | Decorative. |
| L8 | Tracking list picker dialog | NEEDS INVESTIGATION | `core/ui/TrackingListPickerDialog.kt` | none | Depends on tracking scope (H12). |
| L9 | Glass primitive (tint, rim, hairline) | ALREADY IMPLEMENTED | `core/ui/glass/LiquidGlass.kt` (partially) | `design/GlassSurface.kt` | HUSH has the rim, which is what the Apple skill calls the non-negotiable part. Blur is the missing piece (A3). |

## Counts

Counts cover rows A1 to L9.

| Classification | Rows |
|---|---|
| TAKE | 8 |
| ADAPT | 17 |
| ALREADY IMPLEMENTED | 19 |
| SKIP | 21 |
| NEEDS INVESTIGATION | 25 |
| Total | 90 |

## HUSH UI defects (from reading code)

Severity: H = fix before device testing, M = fix in the next UI pass, L = polish.

1. **Login form cannot scroll and has no IME handling (H).** `ui/login/LoginScreen.kt` uses a `Column(fillMaxSize)` with no `verticalScroll` and no `imePadding`. With the keyboard open and a large font scale, the Create account and Continue as guest buttons can sit under the keyboard with no way to reach them. UNVERIFIED on device.
2. **Login shows raw exception text and no inline validation (H).** Errors come from `it.message ?: "Sign-in failed"` (line ~L70). The Sign in and Create account buttons are disabled below 6 characters with no message explaining why. No `ImeAction.Next` or `Done` on the fields.
3. **Source removal has no confirmation and no destructive role (H).** `ui/sources/SourcesScreen.kt` (the Remove button calls `registry.remove` on tap). Removing an addon changes what Home, Search and Details return, with no undo. Follow the pattern in Nuvio's `DownloadDeleteConfirmation.kt` (J3).
4. **Contrast below 4.5:1 on real text (H).** `labelTertiary` (#7E838B light, #6B717A dark) is 3.46:1 on the light canvas and 3.98:1 on the dark canvas, computed from the hex values. `ui/sources/SourcesScreen.kt` line ~129 uses it for the addon manifest URL at `labelSmall` (real, not decorative). Use `labelSecondary` (5.73:1 on canvas) for real text. The decorative initials in `PosterCard`, `HeroBanner`, `LibraryScreen` and `DetailsScreen` can stay at tertiary.
5. **"Play" on the hero does not play (M).** `ui/common/HeroBanner.kt` calls `onPlay(item.id)`, and `HomeScreen` maps that to `onOpen(hero.id)`, which opens Details. The label promises playback and the action does not deliver it.
6. **Episode Play fails silently (M).** `ui/details/DetailsScreen.kt` episode button does `repository.streams(...).firstOrNull()?.let { onPlay(...) }`. If there are no streams or resolution is slow, nothing happens and nothing is shown. Add a busy state and an empty-streams message (ties to D12, D13).
7. **Glass mode cannot be changed (M).** `HushTheme` accepts `glassMode`, but `HushApp` never passes it and no Settings row exists (H2).
8. **Reduced transparency is tied to animator duration (M).** `design/HushTheme.kt` sets `reduceTransparency` when `ANIMATOR_DURATION_SCALE == 0`. That conflates Reduce Motion with Reduce Transparency. Split the two signals.
9. **Developer copy in the About section (M).** `ui/settings/SettingsScreen.kt` ships "Screen structure is inspired by NuvioMobile-Enhanced. No code, artwork or logos were copied from it." It reads as an internal note. Keep attribution for GPL compliance, but move it to a licence or credits screen and write the About text for users.
10. **Glass does not extend content under the bar (M).** `HushApp.kt` places content in a `weight(1f)` box above `HushNavBar` and `GlassTopBar`. Apple's guidance (non-negotiable 10) and Nuvio's floating bar both let content pass under chrome. Visual result UNVERIFIED.
11. **Continue-watching tiles lack focus ring and have no time-left text (L).** `ui/home/HomeScreen.kt` `ContinueWatchingRow` uses `clickable` without `focusRing`, unlike `PosterCard`.
12. **Library loading is bare text (L).** `ui/library/LibraryScreen.kt` shows "Loading your library" text while Home uses `LoadingRows`.
13. **Search field lacks clear and focus (L).** No trailing clear control, no focus request on open.
14. **Hero, poster and details letters as artwork stand-ins (L, honest).** They are labelled as demo or placeholder through `DemoBadge` and the code comments. Keep until the artwork contract exists. UNVERIFIED visually.

## What already works well

- Honest demo marking (`DemoBadge`) and no invented artwork or data in the UI.
- Accessibility basics: `Role.Tab`, `heading()` on titles, 48dp targets through `HushSpacing.minTouch`, `clearAndSetSemantics` on loading rows.
- Typography rules enforced in code (min 12sp, no weight below Regular) in `HushTypography.kt`.
- Glass rim and an honest GlassSurface comment that it does not blur.
- The root theme wrap (`HushApp` calls `HushTheme` before the gate) is present at `887cdea`. The login background defect from the handover is addressed by code reading. Build and device check UNVERIFIED.

## Next steps (for the owner, not done in this phase)

- Decide scope for tracking, debrid, cloud, P2P, downloads (ADR-019), profiles and collections, since several rows depend on them.
- Confirm whether `HushVideoPlayer` (core/player) gets a separate player audit (rows A11 to A13).
- Device or screenshot pass for every UNVERIFIED claim, once the owner allows it.
