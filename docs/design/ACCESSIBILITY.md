# HUSH Accessibility

Owner: Agent 3. Status: **DRAFT v0.1. Contrast figures are hand-computed from the hex values in DESIGN_SYSTEM §2 and must be re-checked with a contrast tool before merge.** Nothing here is verified on device.

## 1. Targets

- **Minimum touch target: 48 × 48dp** for every interactive element. Visual size may be smaller; the hit area is padded.
- Spacing between adjacent targets: at least 8dp.
- Destructive actions are never adjacent to their most common neighbour without at least 16dp separation.
- Player seek bar: 48dp tall hit area, thumb 20dp.

## 2. Contrast

WCAG 2.x AA thresholds: **4.5:1** for text under 18sp (and under 14sp bold), **3:1** for large text, icons, focus rings and control boundaries.

Ratios below were computed by hand from the palette. Treat them as provisional until checked by tool.

| Pair | Light | Dark | Requirement | Status |
|---|---|---|---|---|
| `label` on `canvas` | ≈ 17.5:1 | ≈ 17:1 | 4.5 | Pass (provisional) |
| `labelSecondary` on `canvas` | ≈ 4.9:1 | ≈ 6.2:1 | 4.5 | Pass (provisional) |
| `labelTertiary` on `canvas` | ≈ 3.8:1 | ≈ 3.5:1 | 4.5 for text | **Fail for text.** Use only for decorative or repeated hints with a label elsewhere |
| `accent` on `surface` (text link) | ≈ 6.3:1 | ≈ 9:1 | 4.5 | Pass (provisional) |
| `accentOn` on `accent` (button label) | ≈ 6.3:1 | ≈ 9:1 | 4.5 | Pass (provisional) |
| `danger` on `surface` | ≈ 6.4:1 | ≈ 6.8:1 | 4.5 | Pass (provisional) |
| `warning` on `surface` | ≈ 5.2:1 | ≈ 9:1 | 4.5 | Pass (provisional) |
| `success` on `surface` | ≈ 5.9:1 | ≈ 9:1 | 4.5 | Pass (provisional) |
| Focus ring `accent` on `canvas` | ≈ 6.3:1 | ≈ 9.7:1 | 3:1 | Pass (provisional) |

Rules:
- `labelTertiary` is not used for any text a user must read to act.
- Glass text: the tint alpha floor (72% light, 60% dark) is what keeps text readable over changing content. Below the floor, the component must switch to fallback.
- Text over media always sits on a scrim. Minimum scrim: 45% light, 60% dark, and the text must pass 4.5:1 against the scrimmed result on the brightest test frame of the artwork.
- **Colour is never the only carrier of meaning.** Status uses text plus an icon. Selection uses a checkmark or label change as well as tint. Errors use text beside the field.

## 3. Increased contrast

- Activated by the system setting and by an in-app option.
- Label and separator tokens strengthen: `labelSecondary` moves to the `label` value at 80% opacity equivalent; separators go to 30%.
- Focus rings become 3dp at 100%.
- Glass tint rises to the top of its range and rim highlight is removed in favour of a solid hairline.

## 4. Text scaling

- All text uses sp and follows the system font scale.
- Layouts must work at **200% font scale** without clipping, truncating essential text, or overlapping.
- Essential text may wrap; decorative text may truncate only with a full-text alternative for screen readers.
- Chips and tabs wrap or scroll horizontally, never cut off.
- Poster titles clamp to two lines at large scale; full title shown in the details screen and in the content description.
- Test matrix: 100%, 130%, 200% font scale, compact and expanded width.

## 5. Reduced motion and reduced transparency

- Reduced motion is honoured from the system "remove animations" setting and from an in-app toggle in Appearance.
- Under reduced motion: fades replace spatial motion; no springs; no shared-element travel; no parallax; no looping shimmer; skeletons are static.
- Reduced transparency (in-app toggle; also used when the system asks for less effect): glass uses the opaque fallback from DESIGN_SYSTEM §5.3.
- No information is conveyed only by motion. A completed install, a finished download, a new episode are all announced in text.
- Auto-playing content does not exist in HUSH, so no pause control is needed for it; any future auto-play must offer pause.

## 6. Screen readers (TalkBack)

### 6.1 General rules

- Every interactive element has a role, a name and a state. Name describes purpose, not appearance ("Play Episode 3, The Long Night", not "Play button").
- Icon-only controls require a content description. Decorative icons are hidden from TalkBack.
- Grouped content uses semantic merging so a poster and its title read as one item.
- Focus order follows visual order. Top bar, then content, then bottom bar. Modal surfaces trap focus and return it to the trigger when closed.
- Headings are marked as headings so users can jump between sections.
- Live regions: search progress ("Results from 3 of 5 sources"), install stage, download completion. Polite announcements only; no assertive interruptions except errors that block the user.

### 6.2 Component-specific

| Component | TalkBack contract |
|---|---|
| PosterCard | Merged label: "<Title>, <year>, <type>, from <source>". State: watched or in progress described in the label |
| EpisodeRow | "Episode 3, <title>, <runtime>, 42 percent watched". Swipe actions have named custom actions: "Mark watched" |
| SeekBar | Slider semantics. Value as time: "12 minutes 4 seconds of 45 minutes". Adjustable with increment and decrement of 10 seconds |
| Play/Pause | Name changes with state: "Pause" while playing, "Play" while paused |
| Skip buttons | "Skip forward 10 seconds" with the actual interval |
| Subtitle and audio selection | Sheet title announced on open; each option announces name, language, and selected state |
| Switch | Label is the whole row; state announced as on or off |
| Chip (filter) | Role checkbox or toggle with selected state |
| SegmentedControl | Radio group semantics with a group name |
| ExtensionRow | Name, version, status in words ("Failed to load, last error: manifest") |
| InstallProgress | Stage announced on change: "Verifying extension, step 2 of 3" |
| Snackbar undo | Undo is focusable and announced; it stays for at least 5s and is not auto-dismissed while focused |
| ErrorState | Title announced as heading; retry button named "Retry loading <what>" |
| Skeleton | Hidden from TalkBack; the region announces "Loading <what>" once |
| Empty state | Announced as text; its action is a labelled button |
| Dialog | Title announced on open; focus lands on the first meaningful control, not the destructive one |

### 6.3 Player specifics

- Player controls are reachable with TalkBack even while the video is playing; showing controls on focus is required.
- Controls hide after 3s, but do not hide while TalkBack focus is inside them.
- The current subtitle and audio track are announced when changed.
- Buffering is announced once as "Buffering" and not repeated every second.

## 7. Keyboard, D-pad and external input

- Every action reachable by touch is reachable by keyboard or D-pad, including sheets, the player and the seek bar.
- Focus ring is always visible on focused elements (DESIGN_SYSTEM §5.4).
  - Implemented in `ui/common/HushComponents.kt` as `Modifier.focusRing(focused, shape, accent)`: a 2dp accent border, drawn only while the element has focus. Applied to `PosterCard`, `MediaListItem` and the bottom-bar tabs. UNVERIFIED on device; CI compile only.
  - Open: 3dp at 100% font scale (DESIGN_SYSTEM §5.4) is not yet wired to a scale value.
- Loading rows are static skeletons with no shimmer, so reduced motion has nothing to disable in the search and details screens.
- Player shortcuts for external keyboards: Space play/pause, arrows seek 10 seconds, Shift+arrows 60 seconds, F fullscreen, M mute, C subtitles.
- D-pad navigation on Android TV is out of scope for this phase; no screen should block it structurally.

## 8. Captions and media

- Subtitle support is first-class. The default subtitle size is at least 16sp equivalent and is adjustable up to the largest supported size.
- Subtitle text has a background or outline that passes 4.5:1 against any frame.
- HUSH does not add audio descriptions. Provider streams that supply them expose them as an audio track with a language or "descriptive" label.

## 9. Colour-blind and visual safety

- Status never depends on red and green alone.
- Progress bars use the accent plus a text value.
- No flashing content. HUSH has no flashing UI. Provider media is not altered.

## 10. Testing checklist (to run before a screen is marked done)

- [ ] Every target at least 48dp (Layout Inspector or accessibility scanner).
- [ ] Contrast checked with a tool for every new text or icon pair.
- [ ] 200% font scale: no clipping or overlap.
- [ ] TalkBack pass: every element named, focus order correct, no unlabelled controls.
- [ ] Reduced motion on: no springs, loops or travel.
- [ ] Reduced transparency on: glass fallback renders; text still passes.
- [ ] Keyboard or D-pad reaches every action.
- [ ] Results recorded in PROJECT_STATUS with the device or emulator used. Emulator results are not reported as real-device results.

## 11. Known gaps

- Contrast values are provisional (see note at top).
- Real-device TalkBack and font-scale testing has not been performed; no device exists in this session.
- Localisation and right-to-left layout are not yet specified. Compose supports RTL; components must mirror where meaning depends on direction (seek, back arrow, progress).
