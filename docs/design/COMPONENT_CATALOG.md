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
