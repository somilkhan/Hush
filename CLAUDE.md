# HUSH — CLAUDE.md (primary instruction file)

## Project identity
- Working brand name: **HUSH** (chosen working name, NOT legally cleared).
- Repository: `somilkhan/Hush` (not yet attached in this session; GitHub sign-in pending).
- Package/namespace: NOT decided yet. Do not rename the package or repo blindly; first audit naming implications (see DECISION_LOG D-002).
- Source of truth for the original brief: `MASTER_HANDOVER.md` (attached to the Claude Project; read with `project_read`).

## Objective
A NEW, independent Android media application (Kotlin + Jetpack Compose) with an original Apple-inspired, subtle Liquid Glass identity.
- Not a CloudStream fork, not a reskin. CloudStream is a reference for capabilities and reusable (licence-checked) engineering only.
- Existing CloudStream repositories and compatible extensions MUST continue to work (non-negotiable, per handover §0/§6).

## Brand direction (decided)
- Premium, minimal, polished; Apple-inspired, original identity.
- Subtle Liquid Glass: restrained blur, controlled translucency, subtle highlights.
- Clean typography, restrained colour, distinctive app icon.
- Avoid generic AI-generated branding and typical streaming-app aesthetics.

## Non-negotiable rules
1. Independent application identity, package namespace and navigation.
2. CloudStream code reuse only after licence audit (CloudStream is GPL-family; verify LICENSE and dependency licences before porting any code; preserve notices; record origins).
3. Never claim a feature works without verification evidence (build, test, or real-device run — state which).
4. Never fake data to make UI look complete; mark prototypes clearly.
5. Do not execute untrusted extension code with unrestricted privileges without a security analysis.
6. Do not overwrite another agent's work; respect ownership boundaries.

## Agents (exactly three)
- **Agent 1 — Reverse engineering & CloudStream compatibility.** Owns `docs/reverse-engineering/` and `docs/compatibility/`.
- **Agent 2 — Kotlin architecture & core application.** Owns `docs/architecture/` and core/app modules.
- **Agent 3 — Compose UI/UX & design system.** Owns `docs/design/` and feature UI modules.
- **Claude (Principal Engineer)** — orchestration, integration, review, project memory.

## Documentation rules
- Read `PROJECT_STATUS.md` and relevant docs before editing.
- Update `PROJECT_MEMORY.md`, `DECISION_LOG.md`, `PROJECT_STATUS.md`, `TASK_BOARD.md` at each milestone.
- Significant decisions go to `DECISION_LOG.md` in the format defined there.

## Build & test
- Not yet established. Verify JDK, Android SDK and Gradle availability before claiming builds work (see PROJECT_STATUS.md).

## Design-work preference (user)
- For UI/animation work, load the relevant skill first (ui-ux-pro-max, ui-styling, design-system, apple-design, mobile-design, gestures, animation-performance, etc.).
- Design corrections from the user must be recorded as rules via `learn-taste`.

## Current status
See `docs/PROJECT_STATUS.md`.
