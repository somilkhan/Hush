# HUSH Screen Inventory

Owner: Agent 3. Status: **DESIGN ONLY. No screen in this document is implemented or verified.**

Every screen must define four non-happy states before it is built: **loading, empty, offline, error**. Data comes only from Agent 2's application contracts. Where a contract does not exist yet, the screen is marked `BLOCKED-ON-CONTRACT` and must not be built with fake data (CLAUDE.md rule 4).

Priority: **P0** blocks the first usable journey (startup, extension install, search, details, episodes, playback). **P1** core. **P2** supporting. **P3** advanced or deferred.

Journey order from MASTER_HANDOVER §5 Phase 4 governs build order: startup → repository install → extension load → discovery → search → details → episodes → streams → playback → subtitles → progress → downloads → library → settings.

## 1. Discovery

| Screen | Priority | Must show | Loading | Empty | Offline | Error |
|---|---|---|---|---|---|---|
| Home | P0 | Continue watching (if any), then provider-supplied rows in provider order | Skeleton rows, 3 per screen, static tone | "Add a repository to see titles" with one primary action to Extensions | Cached rows shown with a quiet "Offline, showing saved" line | Per-row error with retry; other rows stay usable |
| Personalised recommendations | P2 | Rows derived from history and favourites | As Home | Explains what generates recommendations; no invented picks | Cached rows | Row retry; no fallback picks |
| Trending | P2 | Provider trending rows, labelled with the source provider | Skeleton | "This source has no trending list" (not a failure) | Cached | Retry |
| Categories | P2 | Categories declared by the source, as a grid of labelled tiles | Skeleton tiles | "No categories from this source" | Cached | Retry |
| Search | P0 | Query field, recent queries, source filter chips | Keyboard-first; no loading until a query is submitted | Recent queries, or "Search across your installed sources" | Search runs against providers; offline message names it | Field-level error text for invalid query; provider errors per source |
| Search results | P0 | Grouped by source, then by type (movie, series, anime) | Progressive: each source streams in as it returns | "No results from N sources" with a list of which sources were tried | Results from cache if any, else offline state | Per-source failure chip with retry, other sources keep results |

## 2. Media

| Screen | Priority | Must show | Loading | Empty | Offline | Error |
|---|---|---|---|---|---|---|
| Movie details | P0 | Backdrop (scrimmed), title, year, runtime, synopsis, primary Play, secondary Favourite/Bookmark | Title and poster skeleton; text appears in place | Missing synopsis shown as "No description from this source" | Saved metadata if previously opened | Retry; Play stays disabled with reason |
| Anime details | P1 | Same structure as movie plus episode count and season grouping where the source supplies it | As movie | As movie | As movie | As movie |
| Series details | P0 | Title, seasons summary, first-episode Play, Continue button when progress exists | As movie | "No episodes from this source" | Saved | Retry per season, not whole screen |
| Seasons | P1 | Season list, episode count, watched progress per season | List skeleton | "This series has no seasons listed" | Saved | Retry |
| Episodes | P0 | Episode rows: still (16:9), number, title, runtime, progress bar, watched mark | Row skeletons | "No episodes listed for this season" | Saved list | Row-level retry; list remains |
| Related content | P2 | Horizontal row from the source, labelled | Skeleton | Row hidden entirely (no empty row) | Hidden | Hidden; no error shown for optional content |

## 3. Player

The player is a full-bleed surface. Chrome appears on tap and hides after 3 seconds of no interaction. No glass sits over video frames without a scrim (see DESIGN_SYSTEM §5.3).

| Screen or dialog | Priority | Must show | Loading | Empty | Offline | Error |
|---|---|---|---|---|---|---|
| Player surface and controls | P0 | Play/pause, seek bar with time, episode previous/next, subtitle and audio buttons, back | "Finding streams" with source name and a cancel action | "No streams from any source" with per-source reasons | Local downloads play; network streams show offline state before attempt | Stream failure offers next source automatically, then a clear message with source choice |
| Episode navigation | P0 | Previous and next episode, episode list sheet | Sheet skeleton | "No other episodes" | Saved | Retry |
| Subtitle selection | P0 | Track list, Off, source subtitles, local file if supported | "Looking for subtitles" | "No subtitles from this source" (state, not a failure) | Downloaded subtitles only | Track fails: fall back to Off with a message |
| Audio and video selection | P1 | Audio tracks, quality levels, with language labels | Sheet skeleton | "Only one track available" | As stream | Fall back to default track |
| Player settings | P1 | Playback speed, subtitle size, skip interval, decoder choice | None (local) | Not applicable | Not applicable | Reset to defaults action |
| Player dialogs | P1 | Confirm exit during download, stream error details | Not applicable | Not applicable | Explains what is offline | Plain-language error with a retry |

## 4. Library

| Screen | Priority | Must show | Loading | Empty | Offline | Error |
|---|---|---|---|---|---|---|
| Continue watching | P1 | Items with progress bar and remaining time | Skeleton | Hidden on Home until there is an item; Library shows the empty state below | Saved | Retry |
| Watch history | P1 | Grouped by day, with remove and clear-all | Skeleton | "Nothing watched yet. Titles you play appear here." | Saved | Retry; remove failure shows undo-able error |
| Favourites | P1 | Poster grid, sort by title or added date | Grid skeleton | "Favourite titles to keep them here." | Saved | Retry |
| Bookmarks | P2 | Titles and episodes saved for later, with optional note | List skeleton | "Bookmark a title or episode to find it later." | Saved | Retry |
| Downloads | P1 | Queue, completed, storage used, pause and remove | Skeleton | "Nothing downloaded. Download from any episode." | Completed items play; queue paused with reason | Per-item error with retry; storage-full message with action |

## 5. Extensions

| Screen | Priority | Must show | Loading | Empty | Offline | Error |
|---|---|---|---|---|---|---|
| Repository management | P0 | Added repositories with URL, last update, extension count; add by URL | Row skeleton; add shows progress | "Add a CloudStream-compatible repository to start." | Last-known list; update action disabled with reason | Manifest error: what failed, and copy-details action |
| Installed extensions | P0 | Extension rows: name, version, language, status (ready, degraded, failed) | Skeleton | "No extensions installed. Browse a repository." | Installed extensions keep working where they are local | Per-extension status with a detail link |
| Extension details | P1 | Metadata, supported operations as reported by the extension, version, source repository, disable and remove | Skeleton | Not applicable | Local details shown | Shows last error with what it failed on |
| Extension installation | P0 | Progress with stage (download, verify, load), cancel, result | Determinate where Agent 2 reports it, indeterminate otherwise | Not applicable | Install blocked with reason before starting | Stage-specific error and retry; no silent partial installs |
| Extension errors | P1 | Log of recent failures per extension, by operation | "Loading log" | "No errors recorded" | Local log | Log unreadable: message and clear-log action |

Note: compatibility behaviour (what each extension supports) is owned by Agent 1. This screen only renders the status Agent 2 exposes.

## 6. Settings

| Screen | Priority | Must show | Loading | Empty | Offline | Error |
|---|---|---|---|---|---|---|
| Settings root | P0 | Groups: Appearance, Playback, Downloads, Extensions, Advanced, About | None | Not applicable | Not applicable | Not applicable |
| Appearance | P0 | Theme (system, light, dark), reduce effects (glass fallback), reduce motion override, text note | None | Not applicable | Not applicable | Not applicable |
| Playback | P1 | Default quality, skip interval, subtitle style, autoplay next episode | None | Not applicable | Not applicable | Reset action |
| Downloads | P1 | Storage location, parallel downloads, Wi-Fi only | None | Not applicable | Not applicable | Storage-unavailable message |
| Extensions | P1 | Update schedule, auto-update toggle, link to repositories | None | Not applicable | Not applicable | Not applicable |
| Advanced | P2 | Diagnostics export, cache clear, developer options hidden by default | Export progress | Not applicable | Not applicable | Export failure message |
| About | P2 | Version, licence notices, open-source attributions (required by LICENSING_AUDIT) | None | Not applicable | Not applicable | Not applicable |

## 7. Supporting UI

| Component | Priority | Notes |
|---|---|---|
| Dialogs | P0 | Single short task, obvious exit, Cancel always present |
| Bottom sheets | P0 | Used for track selection and episode list; dismiss by swipe and by scrim |
| Context menus | P1 | Long-press on posters and rows; every action also reachable without long-press |
| Snackbar with undo | P1 | Used for remove, clear and mark-watched actions |
| Offline banner | P1 | One line, not blocking, names what still works |
| Empty states | P0 | One message, one primary action, no illustration unless it carries meaning |
| Error states | P0 | What failed, what the user can do, retry action. No raw exception text on the main surface |
| Loading skeletons | P0 | Match final layout dimensions to avoid layout shift |

## 8. Build order for Agent 3

1. Design tokens and theme (DESIGN_SYSTEM) once Agent 2 confirms the `design/` path.
2. Shared components (COMPONENT_CATALOG), including glass fallbacks.
3. P0 journey screens: Extensions repository and install, Home, Search and results, Details, Episodes, Player, Settings root and Appearance.
4. P1 then P2 screens.

Each screen is reviewed against this inventory before it is marked done. A screen with unimplemented states is not complete.
