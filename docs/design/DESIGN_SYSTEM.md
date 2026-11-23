# HUSH Design System

Owner: Agent 3 (Compose UI/UX). Status: **DRAFT v0.1, design only, no code.**
Brand name is a working name, not legally cleared (see DECISION_LOG D-001).

## 1. Brand foundation

**Thesis:** HUSH is a quiet room for watching. The interface recedes so the content, and the user's choice, come forward.

**Personality, in four words:** calm, precise, unhurried, confident.

**What HUSH is not:** a streaming-service dashboard, a Material You colour-wash, a neon "cinema" theme, a poster wall with glowing borders.

**Signature element:** one continuous glass control bar that floats over content and is the only place glass appears in the content-free chrome. Everything else is opaque, quiet and typographic.

**Platform posture:** Android-native behaviour (system back, predictive back, TalkBack, system font scale, Material navigation idioms where they are the expected behaviour). Identity lives in colour, type, spacing, glass, motion personality and the app icon, not in custom navigation.

## 2. Colour

Restrained: one neutral ramp plus one accent. No gradients in chrome. Hex values are design intent; each is verified against its surface in ACCESSIBILITY.md.

### 2.1 Roles (light / dark)

| Role | Light | Dark | Use |
|---|---|---|---|
| `canvas` | `#F4F4F2` | `#0B0C0E` | Screen background, receding layer |
| `surface` | `#FFFFFF` | `#15171A` | Raised content (cards, sheets) |
| `surfaceRaised` | `#FFFFFF` + 1dp hairline | `#1D2025` | Dialogs, menus, elevated sheets |
| `label` | `#141518` | `#F2F3F5` | Primary text and icons |
| `labelSecondary` | `#5C6068` | `#9BA1AA` | Metadata, captions |
| `labelTertiary` | `#7E838B` | `#6B717A` | Placeholders, disabled hints (not for essential text) |
| `separator` | `#141518` at 10% | `#FFFFFF` at 10% | Hairlines, 1px / 0.5dp |
| `accent` | `#0E6B66` | `#5FD0C6` | The one accent: primary action, selection, focus |
| `accentOn` | `#FFFFFF` | `#0B0C0E` | Text/icon on accent fill |
| `success` | `#2F6B3A` | `#7FD19A` | Installed, ready |
| `warning` | `#8A5A00` | `#E8B65C` | Degraded source, needs attention |
| `danger` | `#A3262B` | `#F08A8D` | Errors, destructive actions |
| `scrim` | `#000000` at 45% | `#000000` at 60% | Over media, behind sheets |
| `glassTint` | `#FFFFFF` at 72% | `#1A1D22` at 60% | Glass fill (see §5) |

Accent discipline: the accent appears on at most one primary action per screen, on the active selection, and on focus rings. Never on body text on the canvas, never as a decorative wash.

Dark mode is not an inversion. Dark uses a near-black canvas and a visibly lighter `surface`, so layers stay distinct. Pure `#000000` is reserved for the player letterbox.

### 2.2 Light and dark palettes are authored separately

Each role is defined twice. Colour tokens are never derived by inverting or by changing opacity alone. Increased-contrast variants raise label and separator contrast; they do not add new hues.

### 2.3 Forbidden

- Purple-to-blue or any gradient hero. Gradients only inside poster artwork supplied by the provider.
- Glow, neon outlines, coloured drop shadows.
- Per-category or per-provider accent colours. One accent, always.
- Dynamic Material You wallpaper colour as the brand. It may be offered as an opt-in appearance setting later, not as the default.

## 3. Typography

Typeface: **Inter** (SIL Open Font License 1.1, verify licence file before bundling). One family for display and body, using optical-size and weight axes. Bundle the variable font; do not depend on a remote font load. Fallback: system default sans.

System font is used for system-owned surfaces only (notifications, system dialogs).

### 3.1 Scale (sp)

Modelled on a conventional mobile type ladder, set by HUSH's own values. Not copied from any platform.

| Token | Size / line | Weight | Tracking | Use |
|---|---|---|---|---|
| `displayLarge` | 34 / 40 | SemiBold 600 | -0.4 | Hero title on details, empty-state headline |
| `headlineLarge` | 28 / 34 | SemiBold 600 | -0.3 | Screen title on large-title screens |
| `headlineMedium` | 22 / 28 | SemiBold 600 | -0.2 | Section header, dialog title |
| `titleMedium` | 17 / 22 | SemiBold 600 | -0.1 | Card title, list row title |
| `bodyLarge` | 17 / 24 | Regular 400 | 0 | Synopsis, reading text |
| `bodyMedium` | 15 / 22 | Regular 400 | 0 | Default body, list subtitle |
| `labelLarge` | 15 / 20 | Medium 500 | 0 | Button text |
| `labelMedium` | 13 / 18 | Medium 500 | 0 | Chips, segmented control |
| `caption` | 12 / 16 | Regular 400 | +0.1 | Metadata. Floor: 12sp |

Rules:
- Weights below Regular are not used for text. Light weights fail at small sizes on dark surfaces.
- Minimum 12sp anywhere. Nothing essential below 13sp.
- Sizes are sp so they follow the user's font-scale setting. Layouts must reflow at 200%.
- Numerals in durations, ratings and episode counts use tabular figures.
- Sentence case everywhere. No all-caps letter-spaced micro-labels.

## 4. Spacing, shape and layout

### 4.1 Spacing (dp), 4-point base

`2 · 4 · 8 · 12 · 16 · 20 · 24 · 32 · 40 · 48 · 64`

- Screen horizontal gutter: **16dp** compact, **24dp** medium and expanded.
- Between list rows: 0 (rows divided by inset hairline). Between sections: 32dp.
- Touch target minimum 48dp; visual size may be smaller with a padded hit area.

### 4.2 Corner radii (dp)

Nested radii follow one rule: an inner radius is the outer radius minus the padding between them, floored at 8. This is HUSH's convention, not a published platform rule.

| Token | Radius | Use |
|---|---|---|
| `radiusXS` | 6 | Tags, inline badges |
| `radiusS` | 8 | Chips, small controls |
| `radiusM` | 12 | Buttons, text fields, poster thumbnails |
| `radiusL` | 20 | Cards, sheets (top corners), dialogs |
| `radiusXL` | 28 | Floating glass bars and capsules |
| `radiusFull` | 50% | Icon buttons, switches, avatars |

No other radii. Mixing 10, 14 or 18 is a defect.

### 4.3 Layout grid

- Compact (<600dp): single column, 2–3 poster columns in grids.
- Medium (600–839dp): list-detail where it helps, poster grids 4–5 columns.
- Expanded (≥840dp): navigation rail, list-detail, player keeps 16:9 without cropping.
- Posters: 2:3 ratio. Episode stills: 16:9. Avatars and logos: 1:1.

## 5. Elevation, glass and borders

### 5.1 Elevation

Hierarchy comes from tone, hairlines and shadow in that order. Most surfaces have **no shadow**.

| Level | Treatment | Use |
|---|---|---|
| 0 | `canvas` | Screen background |
| 1 | `surface`, no shadow | Cards, list groups |
| 2 | `surfaceRaised` + hairline, shadow 0 dp 8 dp 24 dp at 12% (light) / 40% (dark) | Menus, popovers |
| 3 | Glass (§5.2) | Floating functional chrome only |
| Modal | `scrim` + `surfaceRaised` sheet | Sheets, dialogs |

Shadows are soft, neutral and never coloured.

### 5.2 Glass material rules

Glass is **chrome, never content**. It may appear only on:

- Top app bar and bottom navigation over scrolling content.
- Floating player control cluster (over a scrim, never over bright frames without the scrim).
- Sheet grabber region and floating action bar.

Glass never appears on: poster cards, episode rows, text blocks, settings groups, the player video surface, or any surface that carries body text over changing imagery.

**Strict limits**

| Property | Limit | Notes |
|---|---|---|
| Blur radius | **max 20dp**, default 16dp | Above 20dp costs too much on mid-range devices and blurs away the content hierarchy |
| Tint alpha, light | 72% min, 85% max | Below 72% text over bright content fails contrast |
| Tint alpha, dark | 60% min, 80% max | |
| Animated blur | **never** | Blur is set once per surface; motion uses transform and alpha only |
| Glass layers on screen | max 2 at once | A third layer is a design defect |
| Rim highlight | 1dp, top edge, white at 18% light / 12% dark | Gives the "bent light" edge without a border |
| Saturation boost | none beyond system default | |

**Implementation intent (for Agent 2 review):** use a platform backdrop-blur effect where available (Android 12+ render effects). A Compose recipe such as Haze is a candidate library; it is NOT adopted until Agent 2 approves the dependency and Agent 1 confirms its licence. No dependency is added by this document.

### 5.3 Fallbacks when blur is not cheap

Glass degrades to an opaque surface with the same tint and rim. Visual identity survives; the blur does not.

Fallback triggers:
1. Android below 12 (no render effect).
2. Low-performance flag reported by the device or set by the user in Settings > Appearance > Reduce effects.
3. Glass over video playback (the frame changes every 40ms; blur is disabled, scrim is used instead).
4. Reduce transparency preference (in-app toggle, and honour system "remove animations" for motion).
5. Memory pressure or a frame-time budget miss reported by Agent 2 profiling.

Fallback look: `surfaceRaised` at 96% opacity plus the same rim and hairline. The screen must read identically in structure.

### 5.4 Borders

- Hairline separators: 1px (0.5dp feel), `separator` token only.
- No solid grey borders around cards or buttons. Buttons are filled or tonal; secondary controls use a hairline only when they sit on the canvas.
- Focus ring: 2dp `accent` at 70%, offset 2dp. Always visible for keyboard and D-pad.

## 6. Iconography

- Grid: 24dp, live area 20dp, 2dp stroke equivalent, rounded joins and caps.
- One style only: outlined by default; filled only for the selected state of tab and nav icons.
- Icons match the weight of adjacent `labelLarge` text.
- Custom HUSH icon set, drawn in-house as vector drawables. If a base set is used, it must be under a permissive licence (for example Lucide, ISC) with attribution recorded in LICENSING_AUDIT (Agent 1).
- Icon-only controls always have a content description (see ACCESSIBILITY.md).
- No emoji as icons. No coloured icon badges.

## 7. Motion

Motion is brief, physical and interruptible. It confirms; it never performs.

### 7.1 Durations

| Token | ms | Use |
|---|---|---|
| `instant` | 0 | Toggles' thumb snaps under reduced motion; state changes that must be immediate |
| `micro` | 120 | Press feedback, ripple-equivalent, icon swap |
| `standard` | 200 | Fades, chip selection, small reveals |
| `emphasis` | 320 | Sheet open, dialog open, row expansion |
| `page` | 380 | Screen push and pop (shared element allowed) |
| `ambient` | 0 (no loop) | No looping or attention-seeking motion is permitted |

### 7.2 Curves

- Enter / decelerate: cubic-bezier(0.2, 0.0, 0.0, 1.0) (sum: fast start, soft landing).
- Exit / accelerate: cubic-bezier(0.4, 0.0, 1.0, 1.0).
- Sheet and drag release: spring, damping ratio 0.8, stiffness medium. Seed release velocity from the gesture.
- Gesture track phase: no easing, no spring. Position follows the finger 1:1.

Curves here are design intent, not platform specifications.

### 7.3 Motion rules

- Animate `transform` and `alpha` only. Never animate size, padding or blur.
- Enter and exit share an axis (a sheet that rises from the bottom closes downward).
- Interruptible: a sheet that is closing can be caught and reopened.
- No keyframe animation on anything a gesture can touch.
- Staggered reveals: max 6 items, 24ms stagger, total under 240ms.
- Reduced motion (system "remove animations" = off, or HUSH's own toggle): durations become `instant` or `standard` fades; no springs; no shared-element travel.

## 8. Interaction states

Every interactive component defines all of these. A missing state is a bug.

| State | Visual rule |
|---|---|
| Default | Role colours as §2 |
| Pressed | Overlay 8% of `label` (light) or `accentOn` (dark); scale 0.98 at `micro` for large tappable surfaces |
| Focused | 2dp accent ring at 70%, offset 2dp |
| Selected | Accent for the element, or a tonal fill (accent at 12%) for chips |
| Disabled | Content at 38% opacity, no press feedback, still in focus order with a stated reason for screen readers |
| Loading | Skeleton with static tone (shimmer only when motion is allowed, and never looping forever: one pass, then hold) |
| Error | `danger` text and icon, message beside the field, never colour alone |
| Dragging | Element follows the finger; neighbours move only at release |

## 9. App icon concept (proposal, text only, no assets generated)

**Form:** a single open ring. A stroke of even weight wraps about three quarters of a circle, with rounded caps, and the gap sits at the upper right. The ring is the only shape.

**Colour:** field in `#0B0C0E` (graphite, the dark canvas). Ring in the accent `#5FD0C6` (dark accent). A single 1dp highlight on the top inner edge of the ring, the one nod to glass.

**Why this, and why it is distinct:**
- It is not a play triangle, a film strip, a ticket, a popcorn box, a TV, a letter, or a stacked-card motif. Most streaming and media icons are one of those.
- The gap reads as a door left ajar or a screen dimmed to standby. It suggests quiet rather than spectacle, which is the brand's thesis.
- It works as a progress or loading ring in the app, so the icon and the app's own loading indicator share one shape. That makes the icon a system element rather than a logo pasted on top.

**Constraints:**
- The shape must not imitate any existing product, mark or app icon. Before adoption, run a logo and trademark search for ring-with-gap marks in media and utility categories. HUSH is not legally cleared, and this concept has not been checked.
- Avoid a letter "H" or any "hush" pictogram (finger over lips), which is generic and collides with common imagery.
- Android adaptive icon: foreground layer is the ring alone, kept inside the 66dp safe circle of the 108dp canvas. Background layer is the solid graphite field. Monochrome themed variant for Android 13+ is the ring in one tone.
- No text in the icon. No gradients. No glow.

**Open for review:** whether the accent ring reads as too close to a generic loading spinner at 48px. A test at launcher sizes is required before adoption.

## 10. Anti-generic checklist

Run before a screen ships. Any "yes" to a red item is a rejection.

- [ ] Red: gradient hero, purple/blue gradients, glowing edges, neon.
- [ ] Red: a grid of identical rounded cards with the same shadow and no hierarchy.
- [ ] Red: 01/02/03 or eyebrow labels on content that is not a sequence.
- [ ] Red: a big number over a small label as the hero.
- [ ] Red: emoji or coloured badges used as icons.
- [ ] Red: glass on a content card or on text over video.
- [ ] Amber: more than one accent-coloured element per primary region.
- [ ] Amber: a corner radius outside §4.2.
- [ ] Amber: a font weight below Regular.
- [ ] Amber: any motion that loops without user action.
- [ ] Pass: the structure would still make sense with all colour removed.
