# HUSH — Execution Plan (v1, 2026-11-24)

## 1. Identity
- Working name: HUSH (not legally cleared; trademark and store search pending).
- Repo: `somilkhan/Hush` (public), default branch `main`.
- Licence: GPL-3.0 (`LICENSE`, DECISION_LOG D-007).
- Goal: new independent Android app (Kotlin + Jetpack Compose), Apple-inspired subtle Liquid Glass, compatible with existing CloudStream extensions.
- Not a CloudStream fork or reskin.
- Source brief: `MASTER_HANDOVER.md` (Claude Project). If it conflicts with this repo, ask before changing.

## 2. Verified state
- `main` in sync with origin. Bootstrap docs, Gradle skeleton (placeholder namespace `app.hush.placeholder`), design specs and compat specs are in.
- Build: UNVERIFIED (no Android SDK, no Maven/Gradle access from the current environment).
- Tests: none. Implemented features: none.

## 3. Owners and boundaries
| Owner | Owns | Must not touch |
|---|---|---|
| Agent 1 (RE + compat) | `docs/reverse-engineering/`, `docs/compatibility/` | `app/`, `core/`, `design/` |
| Agent 2 (architecture) | `docs/architecture/`, Gradle root, `app/`, `core/` | RE and design docs |
| Agent 3 (design) | `docs/design/`, `design/` tokens (after Agent 2 confirms path) | RE, architecture, business logic |
| Claude (orchestrator) | Project docs (`CLAUDE.md`, `docs/PROJECT_STATUS.md`, `docs/DECISION_LOG.md`, `docs/PROJECT_MEMORY.md`), reviews, merges | Overwriting agent work |

## 4. Git rules
1. One branch per milestone, not per small fix.
2. One commit per logical milestone. Batch docs. No commit per file.
3. Message format: `type(scope): summary`, body lists changes, one trailer block only.
4. Merge to `main` only after review (section 6), with `--no-ff`.
5. Never force-push `main`. Never rewrite published history.
6. Before pushing: `git ls-remote --heads origin <branch>` and compare with local HEAD. If they match, the work is pushed even if a hook says otherwise.
7. Hook says "unpushed" while the SHAs match: run `git update-ref refs/remotes/origin/<branch> <sha>`. Do not push empty or duplicate commits to satisfy it.

## 5. Milestones (in order, gated)
- **M0 Bootstrap (done):** docs, skeleton, design specs, compat specs, LICENSE.
- **M1 Build proof (blocked on environment):** Android SDK and network to Google Maven and Gradle distribution needed. Generate the Gradle wrapper, run `./gradlew :app:assembleDebug`, record exact output in `docs/PROJECT_STATUS.md`. Pass → "BUILD VERIFIED". Fail → record root cause; never claim success.
- **M2 Contracts agreed (docs only):** Agent 1 answers Agent 2's questions in `INTEGRATION_CONTRACTS.md` (search vs details split, episodes, stream resolution, subtitles, timeouts, minSdk). Agent 3 answers paging, search progress, player picker, reduced-transparency storage. Gate: Claude signs off in `DECISION_LOG.md`. No implementation before this.
- **M3 Compat legal review (external):** Question for a lawyer: does implementing CloudStream's extension API class names in a GPL-3 app raise licence or attribution issues? Also extension sandboxing and user risk. Gate: written answer in `docs/compatibility/LEGAL_REVIEW.md`. Until then the compat layer stays as spec only.
- **M4 Foundation code:** domain models, data layer, DI container, navigation shell, domain unit tests.
- **M5 Design system code:** tokens, theme, glass component with fallbacks. Screenshot check on emulator or device.
- **M6 Vertical slice:** repo add, extension load, search, details, episodes, stream, playback. Only after the M3 gate.
- **M7 Quality and release:** tests, accessibility (TalkBack, font scale, contrast), performance profile, attribution file.
  - Add `NOTICE` with copyright holder line, GPL-3.0 reference and the source of the screen structure (NuvioMobile-Enhanced, owner's fork). Required before any public release (D-009).

## 6. Verification and review
- No feature is "done" without evidence: build output, test output, or device/emulator run. Name which one.
- Agent reports are checked against actual files and commits before acceptance.
- CloudStream behaviour claims cite a file path and symbol in the reference repo. Inference is labelled.
- Compatibility rows are PASS/FAIL with the extension version used. "NOT TESTED" stays NOT TESTED.

## 7. Pre-commit checklist
- [ ] Right branch (`git branch --show-current`)
- [ ] Only the owner's files changed (`git status`)
- [ ] No CloudStream source copied in (grep `com.lagradost` in new code; any port has an origin note)
- [ ] Placeholder namespace not shipped in a release
- [ ] Docs status matches reality
- [ ] `LICENSE` present and unchanged
- [ ] One trailer block only

## 8. Open decisions (owner)
1. Final package namespace and applicationId (D-002), after trademark and store-name check.
2. Copyright holder line for source headers and NOTICE.
3. Whether `MASTER_HANDOVER.md` stays as the Claude Project authority or moves into the repo.
4. Who does the M3 legal review.
5. Minimum Android SDK (provisional 26).

## 9. Never
- Invent APIs that differ from CloudStream's real contracts without a documented decision.
- Fake data or screens and call them functional.
- Run untrusted extension code outside a documented security analysis.
- Put secrets, tokens or credentials in the repo.
- Rename repo, package or brand without a DECISION_LOG entry.
