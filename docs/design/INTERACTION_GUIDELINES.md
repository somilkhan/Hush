# HUSH Interaction Guidelines

Owner: Agent 3. Status: **DRAFT v0.1, design only.**

Principle: a gesture is direct manipulation. While the finger is down, the UI is the finger. When it lifts, physics finishes the motion.

## 1. Navigation

- **Primary destinations (4):** Home, Search, Library, Extensions. Settings sits behind the top-bar action, not in the bottom bar.
- Bottom navigation on compact widths; navigation rail on expanded.
- Tab switch keeps each destination's scroll position and back stack.
- **System back** always works: closes sheets first, then leaves the player, then pops the stack, then returns to Home. Predictive back animation is honoured.
- A screen with unsaved input confirms before discarding it.
- Deep links to a title or episode open the detail screen with the stack rebuilt, so back returns somewhere sensible.

## 2. Gestures

### 2.1 Track and release

- **Track phase:** position maps 1:1 to the pointer. No easing, no spring, no duration. Write position directly; no state update per move event.
- **Release phase:** spring seeded with the release velocity. Never a fixed keyframe.
- **Dismiss on distance or velocity.** Passing about 40–50% of travel dismisses, and so does a fast flick over a short distance.
- **Rubber-band at edges:** dragging past a boundary moves about 20% of the distance.
- **Snap-back is quick** when the threshold is not reached.
- **Bounce** only at the end of a drag, 0.1–0.3. A button-driven close has no bounce.
- **Interruptible:** a sheet that is closing can be caught and dragged back.
- **Direction matches entry:** a sheet that rises from the bottom dismisses downward.

### 2.2 Specific gestures

| Gesture | Where | Action | Alternative (required) |
|---|---|---|---|
| Tap | Anywhere | Select, open | Not applicable |
| Long-press | Poster, episode row | Context menu | Overflow button on the same item |
| Horizontal swipe on episode row | Episode list | Mark watched / unwatched (threshold-based, with undo snackbar) | Context menu item |
| Swipe down | Sheet, dialog | Dismiss | Close button, system back |
| Swipe from edge | Any screen | System back | System back is always available |
| Player tap | Video surface | Show or hide controls | Not applicable |
| Player double-tap left or right | Video surface | Seek back or forward by the skip interval (default 10s) | Seek buttons in controls |
| Player vertical drag, left half | Video surface | Brightness | Brightness in player settings; disabled by default to avoid accidental changes |
| Player vertical drag, right half | Video surface | Volume | System volume keys; setting to disable |
| Seek bar drag | Controls | Scrub position, release to seek | Previous/next and skip buttons |

Gesture-driven actions always have a visible control. A gesture is never the only path.

### 2.3 Gesture conflicts

- Horizontal swipe on episode rows must not fight the horizontal scroll of the page. Use a threshold, and lock the axis after the first 8dp of movement.
- Player gestures are disabled while the seek bar or a control is pressed.
- `touch-action` equivalents: vertical scroll lists never capture horizontal drags except on rows that declare a swipe action.

## 3. Feedback and state changes

- Every tap is answered within 100ms: press overlay at `micro`, state change at `standard`.
- Toggles switch with the thumb sliding at `standard`; colour changes with it.
- Progress: determinate when Agent 2 reports a fraction; otherwise indeterminate with a stage label.
- Feedback lives in the interface (inline state, snackbar), not in modal alerts. Modal alerts are reserved for data loss and irreversible actions.
- Long operations (search, stream resolution, install) show what is happening in words ("Checking 4 sources"), not only a spinner.

## 4. Errors and destructive actions

- Errors say what failed, why in one line, and what the user can do. No stack traces on primary surfaces; a "copy details" action exists in Advanced and Extension errors.
- **Destructive actions** (remove repository, delete download, clear history) use the `danger` role and open a confirmation with a clear verb ("Remove repository", not "OK"), plus Cancel.
- Where undo is safe (remove from history, mark watched), act immediately and offer undo in a snackbar for about 5 seconds. Do not combine undo with a confirmation for the same action.
- Failed operations never silently leave partial state. An install that fails at verify is shown as failed with the stage, and the user can retry or remove the remains.

## 5. Player behaviour

- Controls appear on tap and hide after 3 seconds without interaction. They do not hide while a control is focused or a sheet is open.
- Playing: controls hide; pausing: controls stay visible.
- Rotation: player switches to landscape immediately and keeps playback position.
- Picture-in-picture: available where the platform supports it; controls collapse to play/pause.
- Background audio is off by default; when on, a media notification exposes play/pause and next/previous episode.
- Auto-next episode: a 10-second countdown card with Play now and Cancel. Cancel always works.
- Playback position saves every 5 seconds and on pause, exit and background.

## 6. Search

- Query field focuses on entry. Keyboard action is Search.
- Results stream in per source; the user can open a result before all sources finish.
- Source filter chips are multi-select and persist for the session only.
- A failed source shows as a chip with a retry, never blocks other results.

## 7. Forms and input

- Labels stay visible; placeholder text is a hint, not a label.
- Validation runs on blur and on submit; errors appear beside the field with `danger` colour and an icon.
- Repository URL entry validates the format as the user types, and checks the manifest on submit. Checking shows progress and a cancel.
- Keyboard types match the content (URL keyboard for URLs, number for numeric fields).

## 8. Motion behaviour summary

- Motion confirms, never performs. No looping motion without user action.
- Transform and alpha only (DESIGN_SYSTEM §7).
- Reduced motion: fades only, no springs, no shared-element travel, no parallax.
- Shared-element transitions from poster to details are allowed; they must degrade to a fade under reduced motion.

## 9. Loading behaviour

- Skeletons match the final layout size exactly to prevent shift.
- Content that arrives later replaces the skeleton in place; no re-sorting of visible items while the user is reading. New items append or wait behind a "New results" pill.
- Pull-to-refresh on list screens; the indicator stays visible until the request completes or fails.

## 10. Things HUSH does not do

- No hover-only affordances.
- No auto-playing trailers or previews on focus.
- No interstitials, rating prompts or upsells during playback.
- No animated backgrounds or idle motion.
- No gesture that deletes without a visible undo or confirmation.
