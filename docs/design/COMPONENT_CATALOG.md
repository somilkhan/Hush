# HUSH Component Catalog

Owner: Agent 3. Status: **PLANNED. No component is implemented yet.** Each entry lists its states, the tokens it uses (DESIGN_SYSTEM.md), its accessibility contract (ACCESSIBILITY.md), and what it must not do.

Names are proposed Compose names. They are subject to Agent 2 package review. Components take immutable UI state and emit events; they hold no business logic and never fabricate data.

## Foundations

| Component | Purpose | Tokens | States | Notes |
|---|---|---|---|---|
| `HushTheme` | Provides colour, type, shape, spacing, motion, glass to the tree | all | light, dark, increased contrast | Single entry point. Reads system settings and the in-app Appearance preferences |
| `HushColors` | Role-based colour set | §2 | per theme | No raw hex in any component |
| `HushMotion` | Duration and curve tokens; reduced-motion aware | §7 | normal, reduced | Components ask for a motion spec, not a duration |
| `HushIcons` | Vector icon set | §6 | outlined, filled (selected) | Each icon has a content description parameter; decorative icons pass null |

## Surfaces

| Component | Purpose | States | Notes |
|---|---|---|---|
| `GlassBar` | Top bar and bottom bar material | default, fallback (opaque), over-video (scrim) | Chrome only. Blur max 20dp. Auto-falls back per DESIGN_SYSTEM §5.3 |
| `GlassSurface` | Generic floating glass container for chrome | default, fallback | Max two glass layers on screen; the component reports depth so the tree can enforce it in debug builds |
| `Surface` | Opaque content container | default, raised | No shadow at level 1. Hairline only at level 2 |
| `Scrim` | Dims media behind controls | light, dark | Used for player controls and details backdrop |
| `ListGroup` | Inset grouped rows with rounded container | default | Hairlines inset past leading content |

## Buttons and actions

| Component | Purpose | States | Notes |
|---|---|---|---|
| `PrimaryButton` | The one primary action per region | default, pressed, focused, disabled, loading | Accent fill. Max one per screen region. Loading keeps width and shows a stage label |
| `TonalButton` | Secondary action | default, pressed, focused, disabled | Accent at 12% fill, accent label |
| `TextButton` | Tertiary and inline actions | default, pressed, focused, disabled | Used in dialogs and inline errors only |
| `DestructiveButton` | Remove, delete, clear | default, pressed, focused, disabled | `danger` role. Never the default action in a dialog |
| `IconButton` | Icon-only actions | default, pressed, focused, selected, disabled | 48dp hit area; content description required |
| `FloatingActionBar` | Floating group of 2–3 actions over content | default, fallback | Uses GlassSurface. Used sparingly, e.g. episode multi-select |

## Selection and input

| Component | Purpose | States | Notes |
|---|---|---|---|
| `Chip` | Filter, source, category | default, selected, pressed, focused, disabled | Selected uses accent tint; never colour alone (checkmark or label change) |
| `SegmentedControl` | 2–4 mutually exclusive options | default, selected, focused, disabled | Thumb slides at `standard`. Max 4 segments on compact |
| `Switch` | On/off settings | off, on, focused, disabled | Label is the whole row, not just the switch |
| `RadioRow` | Single choice in a list | unselected, selected, focused | Used for quality and track selection in sheets |
| `Slider` | Volume, brightness, seek | default, dragging, focused, disabled | Track follows the finger 1:1; release seeks |
| `TextField` | Text entry | default, focused, error, disabled, with helper | Visible label; error text beside field |
| `SearchField` | Search entry | default, focused, typing, cleared | Clear button 48dp with description |
| `Stepper` | Small numeric change (skip interval) | default, min, max, focused | Announces new value to TalkBack |

## Media

| Component | Purpose | States | Notes |
|---|---|---|---|
| `PosterCard` | Title in grids and rows | default, focused, pressed, loading (skeleton), missing-art (typographic placeholder) | 2:3, radius 12. No border, no shadow. Missing art shows the title on `surfaceRaised`, never an invented image |
| `EpisodeRow` | Episode in a list | default, watched, in-progress (bar), focused, pressed, loading | 16:9 still, progress bar 2dp, watched mark is an icon plus text |
| `ContinueWatchingCard` | Resume item | default, focused, pressed | Progress bar and remaining time |
| `MediaRow` | Horizontal list of PosterCards | default, loading, empty (hidden), error (inline retry) | Header with title and optional source label |
| `BackdropHeader` | Title area of details screens | default, loading, no-backdrop | Scrim over backdrop. Backdrop is provider art, never a generated gradient |
| `SourceBadge` | Shows which provider supplied an item | default | Text label, not a coloured icon |

## Player

| Component | Purpose | States | Notes |
|---|---|---|---|
| `PlayerSurface` | Video output container | playing, paused, buffering, error | Black letterbox, no glass over frames |
| `PlayerControls` | Play, seek, episode nav, tracks, back | visible, hidden, locked | Auto-hide after 3s; locked while a control is focused |
| `SeekBar` | Position and buffer with time labels | default, dragging, disabled (live) | Times read aloud as "12 minutes 4 seconds" |
| `ControlCluster` | Glass group of primary playback buttons | default, fallback over video | Uses scrim, not blur, over video |
| `ResolvingCard` | Stream resolution progress | resolving, failed-next-source, none-found | Names the source being tried |
| `NextEpisodeCard` | Auto-next countdown | counting, paused, cancelled | Play now and Cancel both reachable |

## Lists, rows and extensions

| Component | Purpose | States | Notes |
|---|---|---|---|
| `ListRow` | Standard row: leading, title, subtitle, trailing | default, pressed, focused, disabled, selected | Min height 56dp; touch target full row |
| `RepositoryRow` | A repository with URL, update time, count | default, updating, failed, offline-stale | Failed shows reason inline and a details action |
| `ExtensionRow` | An extension with status | ready, degraded, failed, installing (staged), disabled | Status text plus icon; never colour alone |
| `InstallProgress` | Stages: download, verify, load | staged, determinate, failed-at-stage, cancelled | Names the stage; retry and remove on failure |
| `ErrorLogRow` | One recorded failure | default, expanded | Expands to show operation and time; copy action |

## Feedback and status

| Component | Purpose | States | Notes |
|---|---|---|---|
| `Skeleton` | Loading placeholder matching final size | static (default), shimmer (only if motion allowed, one pass) | Never loops indefinitely |
| `EmptyState` | Message, optional single action | default | Icon optional and must carry meaning |
| `ErrorState` | What failed, what to do, retry | default, retrying | Retry shows progress; no raw error text by default |
| `OfflineBanner` | Non-blocking connectivity line | visible, hidden | Names what still works |
| `Snackbar` | Brief confirmation with optional undo | shown, undone, timed-out | Undo for about 5s; not for errors that need action |
| `ProgressBar` | Determinate or indeterminate progress | determinate, indeterminate | 2dp for episode progress, 4dp for install |

## Overlays

| Component | Purpose | States | Notes |
|---|---|---|---|
| `HushBottomSheet` | Track selection, episode list, options | opening, open, dragging, closing, dismissed | Track release uses spring; closes downward; scrim tap and swipe dismiss; close button also present |
| `HushDialog` | Single short task or confirmation | opening, open, closing | Cancel always present; destructive confirm uses danger role |
| `ContextMenu` | Long-press actions | open, closed | Every item also reachable through overflow |
| `Popover` | Small menu anchored to a control | open, closed | Used on expanded layouts; sheet on compact |

## Layout

| Component | Purpose | Notes |
|---|---|---|
| `HushScaffold` | Screen frame with top bar, content, bottom bar | Handles insets: status bar, navigation bar, keyboard, display cutouts |
| `HushTopBar` | Title, back, actions | Large-title variant on Home and Library; glass in chrome role |
| `HushNavBar` | Bottom navigation, four destinations | Selected item tints icon and label only |
| `HushNavRail` | Expanded navigation | Same destinations as NavBar |
| `PosterGrid` | Adaptive grid of PosterCards | Columns from width class; 2:3 cells |

## Rules every component follows

1. No raw colour, size or duration values; tokens only.
2. Every state in its table has a visual and an accessibility description.
3. No component fabricates content. Missing data shows the component's empty or error state.
4. Glass components enforce the limits in DESIGN_SYSTEM §5.2 and switch to fallback automatically.
5. Tap targets are at least 48dp, even when the visual is smaller.
6. Components with motion read the reduced-motion setting from `HushMotion`; they do not check system settings themselves.
7. Previews exist for each state in light and dark, and at 200% font scale, before a component is marked done.

---

## M2 answers (Agent 3, docs only)

Status: PROPOSAL for Agent 2 review. No code. All values in brackets are placeholders to be set by the owner of the contract. No data is shown in any example; states are named, not populated.

### Q1. Paging: per-provider or merged cursors?

**Answer: per-provider cursors. The UI never merges cursors.**

Reasons: `Page.nextCursor` is opaque (INTEGRATION_CONTRACTS §1), so the app cannot combine cursors from different providers. A single merged cursor would also force a slow provider to hold back every other provider's rows. Home rows are already provider-ordered (SCREEN_INVENTORY §1), so each row maps to one provider and one cursor.

Implied state shape (per row, owned by the feature layer, not by the component):

```
sealed interface RowState {
  data object Loading : RowState                 // first page in flight: skeleton, fixed height
  data class Loaded(
    val items: List<MediaSummary>,
    val hasMore: Boolean,
    val loadingMore: Boolean,
    val moreError: Boolean                       // append failed: inline retry at row end
  ) : RowState
  data object Empty : RowState                   // provider returned zero items: row hidden on Home
  data class Failed(val retryable: Boolean) : RowState   // first page failed: inline retry
  data object Unsupported : RowState             // AppError.Unsupported: row hidden, not an error
  data object Offline : RowState                 // cached items shown with "Offline, showing saved"
}
```

Per screen, the feature holds `Map<ProviderId, RowState>` plus the cursor per provider. Rules:
- Load more is requested when the user is within [3] items of the row end (horizontal rows) or within [1] screen of list end (grids).
- A row's failure never changes other rows.
- Appended items go to the end. Visible items are never re-sorted (INTERACTION_GUIDELINES §6).
- Cursors are kept in memory per screen session. Restoring them after process death is out of scope for M2.

Open for Agent 2: does `sections()` return an empty `nextCursor` for "end" and `null` for "not paged"? Both map to `hasMore = false`, so either works if the answer is consistent.

### Q2. Search: per-provider progress or merged list?

**Answer: per-provider groups in a fixed slot order, with a progress summary above them.** Not a single merged list.

Reasons: a merged list re-sorts as results arrive, which breaks reading. Fixed slots let the user see progress and failures per source, which matches SCREEN_INVENTORY §1.

Slot rules:
- Each provider in the query gets one slot when `Started(provider)` arrives. Slots are ordered by the user's provider order (Extensions screen order), not by arrival time.
- Results fill their slot in place. Nothing moves once shown.
- A slot is one of: `Searching` (skeleton rows at the final row height), `Results(n)` (grouped by kind: movie, series, anime), `NoResults` ("No results from this source"), `Failed(reason)` (chip with retry), `Cancelled` (shown only if the user started a new query; never shown as a failure).

Summary line above the slots (one line, not a banner):
- While running: "Searching [k] of [n] sources"
- On finish: "[m] results from [s] sources. [f] failed." Failed count is a tappable chip that scrolls to the first failed slot.
- With zero results and zero failures: "No results from [n] sources" plus the list of sources tried, as SCREEN_INVENTORY §1 requires.

Partial failure:
- A failed source shows an inline chip in its own slot with the source name, the reason in plain language, and "Retry". Other slots keep their results.
- The failure never clears results already shown.
- Offline: if the device has no connection, all slots show the offline state at once instead of one failure per source.

Contract notes for Agent 2:
- `Results` is assumed to arrive once per provider. If paged search exists, it needs a separate event or a cursor; that is an Agent 1 question, not decided here.
- `Finished` is the only terminal event that closes the summary. A stream that ends without `Finished` (for example after cancel) shows the summary as "Stopped".

### Q3. Player: stream picker or automatic selection?

**Answer: automatic selection by default, with a manual source sheet available at any time during playback. No picker is shown before playback.**

Default behaviour:
1. Playback starts from the first `Found` stream from `StreamResolutionPort.streams()`, as the flow already allows.
2. While nothing is found yet, `ResolvingCard` shows "Finding streams" with the name of the source being tried and a Cancel action. It does not show a list.
3. If the chosen stream fails to start within [timeout, owner to set], the player moves to the next found stream. The card reads "Trying another source" and names it. The move happens at most [N] times, owner to set.
4. If all found streams fail, or the resolution finishes with none, the `ResolvingCard` changes to `failed-next-source` or `none-found` (COMPONENT_CATALOG Player table). The "none found" state lists the per-source reasons.

User override path:
- A "Source" control in `PlayerControls` opens `HushBottomSheet` with a `RadioRow` list. Each row shows: source label, quality if known, and a status (playing, available, failed with reason).
- Selecting a row switches source and resumes at the current position. The switch keeps playback position from the old stream. If the new stream has a different duration, the seek bar follows the new duration.
- The sheet is reachable during playback and also from the error state. It is never required to play.
- The sheet lists streams found so far. It keeps listing new ones as they arrive, appended at the end.

Not decided here: the first-stream timeout, the retry count, and whether the last-chosen source is remembered per title. The last one is a per-viewer convenience; if adopted, it is stored in DataStore under the same rules as Q4.

### Q4. Reduced-transparency preference: where does it live?

**Answer: confirm DataStore Preferences, read in `:app` and passed to `HushTheme`. No other store.**

Reasons: it is an app-level setting that must survive restart, is read reactively as a `Flow`, and is not a secret. SharedPreferences adds nothing here. Compose code does not read DataStore directly; `:app` maps the value to a `GlassMode` value and passes it down.

Proposed key and values:
- Key: `ui.glass_mode`, enum string.
- Values: `FOLLOW_SYSTEM` (default), `REDUCED` (user asked), `FULL` (user asked, only when the system asks for nothing).

Effective value, decided in one place (`:app`, never in components):
- If the system asks to reduce transparency or remove animations, the effective mode is `REDUCED` regardless of the stored value. The in-app control shows "On (from system setting)" and is disabled. This rule protects users who need it.
- Otherwise the stored value decides. `FOLLOW_SYSTEM` means the app follows the system, which is "not reduced" in practice.
- A low-performance flag from the device forces `REDUCED` the same way, shown as "On (device)". Owner to confirm the flag source.

Default when the system setting is on: `REDUCED`. Glass renders as the opaque fallback from DESIGN_SYSTEM §5.3: `surfaceRaised` at 96% plus the same rim and hairline. The screen structure does not change.

Startup: until the first DataStore value arrives, render `FOLLOW_SYSTEM`. The read is one small `Flow` value, so the flash is short. If Agent 2 sees a visible flash in testing, the fix is to read it before first composition. This is UNVERIFIED until a device run.

Android note: Android has no single universal "reduce transparency" switch on every version. The reliable signals are the animator duration scale (Remove animations) and the in-app toggle. Owner or Agent 2 to confirm which system signals are available at the minimum SDK (provisional 26; see EXECUTION_PLAN §8).
