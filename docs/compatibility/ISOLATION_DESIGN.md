# ISOLATION DESIGN — CloudStream extension runtime for HushStream

Owner: Agent 1 (compatibility). Principal review: Claude (Principal Architect).
Status: **PROPOSED**, for owner sign-off. Decision record: `docs/architecture/SOFTWARE_ARCHITECTURE.md` ADR-023.
Scope: D-020 (HushStream = Nuvio features + CloudStream extension compatibility). M3 legal review waived by owner (D-013).

This document designs how HushStream reads CloudStream-compatible repositories and extensions, and how it
would run third-party extension code, without running any code yet. Nothing here is implemented. Every
claim not checked in this session is tagged **UNVERIFIED**. Facts inherited from `EXTENSION_ARCHITECTURE.md`
keep that document's tags (**[V]** read in source, **[D]** observed in public data, **[I]** inference,
**[NT]** not tested) and were not re-checked here.

---

## 1. Goal and non-goals

**Goal.** Existing CloudStream repositories and extensions keep working in HushStream (handover §0/§6, D-020),
and the code that runs them does not endanger the UI, the user's data, or the device more than necessary (ADR-018 driver 2).

**Non-goals (v1).**
- No copy of upstream source. Behaviour is described, not copied. Upstream type names are needed only where the
  binary interface requires them (see §4).
- No recommendation to load extensions from arbitrary URLs. Repositories are user-added and owner-reviewed (see §7).
- No iOS, no desktop JVM. Extensions are Android dex archives and need an Android runtime.
- No media bytes passing through extension code. Extensions return metadata and stream URLs; the player fetches media.

## 2. How a repository and an extension are read (metadata only)

This section needs no third-party code to run. It is Phase 0 (§9).

### 2.1 Reading order

1. **Repository manifest** (`repo.json`): a JSON object with a name, a description, a manifest version, and a list of
   plugin-list URLs. **[D]** (EXTENSION_ARCHITECTURE §2.1).
2. **Plugin lists** (`plugins.json`): a JSON array. Each entry gives a download URL, a status code, a version integer,
   an API version integer, display metadata, a file size, and a `sha256-<hex>` file hash. **[D]** (§2.2).
3. **Extension archive** (`.cs3`): a ZIP containing a manifest JSON and a dex file. The manifest names the plugin
   class to instantiate. **[D]** (§2.3).

### 2.2 Rules HushStream applies at read time

| Rule | Behaviour | Why |
|---|---|---|
| Unknown fields | Ignored. Parsing is tolerant. | Upstream indexes carry fields HushStream does not use (for example the `jar*` fields). Source is tolerant, so HushStream must be too. **[V]** (§2.4) |
| Transport | `https://` only for `repo.json`, plugin lists and `.cs3`. Plain `http://` is refused at input time. | ADR-018 threat model; upstream accepts `http://` and enables cleartext (§5.1). Open question Q3 (owner). |
| Redirectors | No third-party short-link resolvers. The user enters a full URL. | Upstream resolves short codes through third-party redirectors (§5.1). |
| CDN rewrite | Off. | Third-party fetch path; not needed for correctness. |
| Status code | `0` down, `1` OK, `2` slow, `3` beta-only. Shown to the user. `beta` hidden unless the user opts in. | Matches upstream host semantics per its source comment. **[V]** (§2.2 of the existing doc) |
| `apiVersion` | Parsed and stored. An entry whose API version HushStream does not support is listed as "unsupported" and never installed. | Upstream parses but does not enforce it (§2.5). HushStream should not rely on a binary ABI check alone. |
| `manifestVersion` | Stored. Unknown higher values show a warning, not a refusal. | Same reason as above. |
| File hash | The downloaded file must match `fileHash` (SHA-256) before it is written to its final path. Mismatch means refuse and delete the temp file. | Upstream does this **[D]**. A hash alone does not prove authorship (§6). |
| File size | Checked against `fileSize` and a HushStream maximum (proposed 20 MB). | Defends against oversized downloads. Limit is UNVERIFIED as a value; owner may change it. |
| Storage | App-private, one folder per repository. Temp downloads in the cache folder, moved atomically after the hash check. | Keeps extension files out of shared storage. No storage permission is needed. |
| Logging | Repository host and plugin name are logged. Stream URLs and user data are not. | Privacy driver 1. |

### 2.3 What Phase 0 shows

- Repository name, description, status, version, authors, and language for each extension.
- No execution. Install is allowed only after consent (§7). A listing does not run code.

## 3. Extension API surface and what HushStream must provide

### 3.1 Behaviour the extension expects (described, not copied)

From the surveyed extensions **[D]** (`EXTENSION_ARCHITECTURE.md` §3, five extensions, hashes verified, no execution):

| Behaviour | Expected by extension | Notes |
|---|---|---|
| Plugin entry | A plugin class is instantiated and given a host context to register providers and extractors. | Lifecycle in §4 of the existing doc **[V]**. |
| Provider registration | The extension registers one or more providers, each with a name, base URL, language, supported media types, and optional main-page sections. | **[D]** |
| Search | Given a query and page, returns a list of search results (title, URL, poster, type). | **[D]** names present; behaviour **[V]** from source only. |
| Details | Given a media URL, returns a load result with episodes (where applicable), plot, rating, and cast. | **[V]** source only. |
| Stream resolution | Given an episode data string and callbacks, calls back with stream links (URL, referer, headers, quality, container type) and subtitle tracks. Returns a success flag. | Overload not observed in the five extensions (**[NT]**), shape taken from source. |
| Extraction | Extractor objects resolve embedded players to stream links. | **[D]** registration present. |
| Network | The extension makes HTTP requests through a host-provided client with a request helper, and parses HTML or JSON with host-provided libraries. | **[D]** the extensions link against these helpers. |
| Errors | An extension signals "no data" with a dedicated error type, and any other failure as a thrown exception. | Maps to `ProviderError` (§3.3). |

### 3.2 What HushStream must provide (host-side obligations)

HushStream owes the extension a runtime that behaves like the surface above, but HushStream decides how it is built:

1. A **shim** that provides the names and shapes the binary requires (§4), implemented over HushStream code.
2. A **network client** owned by HushStream. The shim's request helper forwards to it. Timeouts, host logging,
   and the allowlist policy (§5.3) live there.
3. **Parsing helpers** that match the versions the binary was built against. The pinned versions must be recorded in
   `DEPENDENCY_MAP.md` before implementation (**UNVERIFIED** here; the map exists in the repo but was not re-read this session).
4. **Result translation** into HushStream's own port types (`ProviderPort`, `StreamEvent`, `ProviderError`, defined in
   `EXTENSION_ARCHITECTURE.md` §6.1 and owned by Agent 2). No upstream type reaches the UI.

### 3.3 Error mapping (summary, already designed in §6.1 of the existing doc)

| Extension-side event | HushStream `ProviderError` |
|---|---|
| "no data" error or empty result | `NoData` |
| Thrown exception inside the extension | `Broken` (message truncated, stack not shown to the user) |
| Call exceeds its timeout | `Timeout` |
| Status `0` or host unreachable | `Unavailable` |
| Hash or signature failure at install | `Untrusted` |

## 4. The binary interface problem (where the design must be honest)

Extensions are compiled against upstream type names in a fixed package namespace. The runtime must provide those names
with compatible shapes, or the extension fails to link. There is no negotiation (**[V]**, existing doc §3.4).

Consequences for this design:

- HushStream's shim must use **the names the binary requires**. This is a compatibility requirement, not a copy of
  upstream code. The shim is written from the behaviour in §3.1 and from the observed shapes, by HushStream authors.
- The shim lives in its own module (proposed `compat/cloudstream-abi`), so the licence status of that module can be
  reviewed in isolation and the UI module cannot depend on it (Gradle visibility).
- The shim must not contain any upstream file, copied method body, or copied comment. A reviewer checks this by diff
  before merge (see §10, checklist).
- **Legal status is open.** M3 external review is waived by the owner (D-013). The owner carries the risk. This is
  recorded in ADR-023 and must be re-read by the owner before the shim is merged. HushStream does not give legal advice.
- A consequence the owner should know: running a third-party extension means HushStream runs code written by someone
  else, in a process HushStream controls. The design below contains that risk; it does not remove it.

## 5. Process and sandbox options

Options are listed from least to most isolated. Each is judged against the three drivers that matter here: crash
isolation, data isolation, and cost.

### 5.1 Option A — Metadata only (no execution)

- **What:** Phase 0 only. The app reads indexes and lists extensions. Nothing runs.
- **Isolation:** Total for code. Data risk limited to the index files.
- **Compatibility:** None. No provider works, so no stream can be found.
- **Cost:** Low.
- **Use:** The first shipped step. It is not a compatibility layer.

### 5.2 Option B — In-process dex loading (how upstream works)

- **What:** Load each `.cs3` with a class loader whose parent is the app's own class loader. Call the plugin in the app process, with timeouts and exception catching around each call.
- **Isolation:** None for data or privilege. The extension can read any app class, any app-private file (including the
  Room database and tokens), and use every permission the app holds. Upstream does this **[V]** (existing doc §5.1).
- **Crash isolation:** Exceptions are caught. A loop that never returns, a native crash, or memory exhaustion takes the
  app down. Timeouts can abandon a thread but cannot stop it.
- **Compatibility:** Highest and simplest.
- **Cost:** Low.
- **Verdict:** **Rejected** for any release. It conflicts with ADR-018 driver 2 (isolation) and the sign-out data policy (D-016).
  It is acceptable only for HushStream-authored test code during a spike (§9, Phase 2).

### 5.3 Option C — Separate process in the same app (android:process)

- **What:** Run the extension runtime in a second process of the same APK, reached by a bound service over a narrow
  binder interface that carries only data (HushStream-owned data classes, never the upstream classes).
- **Crash and hang isolation:** Good. A crash or stall kills only the runtime process; the UI sees a dead service and
  restarts it. Timeouts can kill the process rather than abandon a thread.
- **Data isolation:** **None beyond process boundary.** The process runs with the same Linux user as the app, so it can
  read the same app-private files and use the same permissions (**UNVERIFIED** for the exact Android version range, but this
  is the standard behaviour of same-app processes). Option C is a failure boundary, not a security boundary.
- **Cost:** Medium. Two processes to start and watch. Binder marshalling. Shim must also load in that process.
- **Verdict:** A **fallback** if Option D fails its spike. Better than B. Not enough alone for untrusted repositories.

### 5.4 Option D — Isolated process (android:isolatedProcess) with host-mediated I/O  **(recommended)**

- **What:** Run the runtime in a process marked as isolated. Android gives it its own unique Linux user, no app-data
  access, and no permissions. The host sends the `.cs3` as a file descriptor over binder. The extension's network
  requests go to the host through a request proxy; the host applies policy, timeouts and logging, and returns
  responses. Media bytes never pass through the runtime; the runtime returns only metadata and URLs.
- **Data isolation:** Strong. The runtime cannot read HushStream's private files or database. It cannot hold tokens.
  It cannot read storage. It has no direct network path if the platform denies it (**UNVERIFIED**, see §8, item 1).
- **Crash and hang isolation:** Same as C, plus the process is cheaper to recycle.
- **Network:** **UNVERIFIED.** Isolated processes may have no network access. If so, the host proxy is mandatory, not optional.
  The design assumes the proxy, because it gives policy, logging and a single timeout path either way.
- **Dex loading:** **UNVERIFIED.** The runtime needs to load a dex it cannot open by path. Plausible path: the host passes an
  open descriptor, and the runtime loads the dex from `/proc/self/fd` or an equivalent in-memory loader. The spike must prove this.
- **Cost:** High. Proxy adds latency to every HTTP call (scraping is many small calls). Shim must be proxy-aware. Body size limits needed.
- **Verdict:** **Recommended target.** Only option that gives data isolation without a second APK. Gated by the Phase 2 spike.

### 5.5 Option E — Separate companion APK

- **What:** Ship the extension runtime as its own app with its own package name, data directory, and signature, and
  communicate with HushStream over a signature-protected bound service.
- **Isolation:** Strongest available without a separate device user: its own private data, its own permissions, and it
  can be given only INTERNET.
- **Cost:** Highest. Two APKs to sideload (ADR-020), version pairing, two update paths, and a larger attack surface to explain to the user.
- **Verdict:** **Reserve.** Revisit if Option D's spike fails on data isolation, or if the owner adds untrusted public repositories.

### 5.6 Summary

| Option | Crash isolation | Data isolation | Compat effort | Cost | Verdict |
|---|---|---|---|---|---|
| A Metadata only | n/a | n/a | none | low | Phase 0, not compat |
| B In-process | weak | none | low | low | Rejected for release |
| C Same-app process | good | none | medium | medium | Fallback if D fails |
| **D Isolated process + host I/O** | **good** | **strong (UNVERIFIED)** | **medium-high** | **high** | **Recommended** |
| E Companion APK | good | strongest | high | highest | Reserve |

## 6. Recommendation

**Option D**, behind the port in §3.2, staged so that no third-party code runs until the spike (Phase 2) has passed.

If the Phase 2 spike shows that the isolated process cannot load a dex from a descriptor, or cannot reach the host
proxy under the chosen policy, **fall back to Option C**, and record in `DECISION_LOG.md` that data isolation is then
not provided. In that case the owner must accept the risk before any third-party repository is enabled. Option B is not
an acceptable fallback.

## 7. Permission model

Extensions have **no declared permissions** today. Their manifest carries no capability list **[D]**. HushStream therefore
enforces permissions on the host side and treats every extension as holding the minimum.

### 7.1 Capabilities

| Capability | Default | How it is granted |
|---|---|---|
| Read own extension folder | Yes | Implicit. Isolated process has no other access anyway. |
| Make HTTP requests through host proxy | Yes, logged | Implicit when the extension is installed. Hosts are logged; a per-extension host report is shown in the extension detail screen. |
| Access HushStream private data (Room, tokens, settings) | **Never** | Not offered. No API exists for it. |
| Storage, contacts, location, camera, microphone | **Never** | Not offered. The runtime has no permissions. |
| Intents, install packages, clipboard | **Never** | Not offered. |
| Background work | Never | Runtime lives only while a user action needs it. |
| Stream resolution callbacks | Yes | Core function. |

### 7.2 Consent flow (install and update)

Shown before the first install, and again on every update that changes the hash:

1. Repository name and URL (HTTPS).
2. Extension name, version, authors, and language.
3. Plain statement: "This extension can make network requests to any website. It cannot read your library, history,
   downloads or account."
4. File hash (short form) and, when the update changed it, the previous hash.
5. Install button. No default-on install from an index.

Owner decision needed on wording of step 3 (§11).

### 7.3 Revocation

- Per extension: disable (keeps files), or remove (deletes files and its cache).
- Per repository: disable (hides its extensions and keeps them installed but inactive), or remove.
- Status `0` (down) disables the provider automatically until the next refresh shows it up again.

## 8. Failure isolation (ADR-018)

| Failure | Detected by | Response |
|---|---|---|
| Call hangs | Per-operation timeout (proposed defaults, not measured; owner may tune, §11 item 7) | Kill runtime process, return `Timeout` for that call. Other providers are unaffected. |
| Extension throws | Runtime catch, sent back as an error record | `Broken` with truncated message. Counted toward the circuit breaker. |
| Runtime crashes or is OOM-killed | Binder death callback | Mark the process dead. Restart lazily on next call. Count as one failure. |
| Repeated failures (proposed: 3 in 10 minutes) | Circuit breaker per extension | Disable that extension until the user re-enables it. Show an "extension error" state (U5 in the existing doc). |
| Repository unreachable | Network error on refresh | Keep last good listing. Show stale marker. Never delete installed extensions on a failed refresh. |
| Hash mismatch on download | Hash check before move | Refuse, delete temp file, `Untrusted`. |
| Proxy request body over limit | Proxy | Refuse with error; the extension gets a failed request, not a crash. |

Thresholds (3 failures, 10 minutes, default timeouts) are proposals. None is measured. **UNVERIFIED.**

Cancellation: leaving a screen cancels in-flight calls and sends a cancel to the runtime. If the runtime does not acknowledge
within a short window, it is killed.

## 9. Phased plan and exit criteria

No phase depends on a later phase's unproven claim. Each exit needs evidence named in the report (CLAUDE.md rule 3).
Build evidence is CI `assembleDebug` only, because the sandbox has no Android SDK (**UNVERIFIED** locally).

| Phase | Work | Runs third-party code? | Exit criteria | Evidence required |
|---|---|---|---|---|
| **P0 — Metadata** | Read `repo.json`, `plugins.json`; hash-check and store `.cs3` files (no load); list, consent, remove. | No | Owner adds a repository by HTTPS URL; lists the five surveyed extensions; hash check refuses a tampered file; `http://` refused. | Device run (owner); unit tests for parsing and hash, if added; CI build. |
| **P1 — Port and host pipeline** | `ProviderPort` implemented over a HushStream-authored **fake** provider (no third-party code). Timeouts, error mapping, circuit breaker, cancellation. | No (fake only) | Fake provider that hangs is timed out and cancelled; one that throws shows `Broken`; the rest of the UI keeps working. | Device run. CI build. |
| **P2 — Isolation spike** | A HushStream-authored tiny test extension (built by us, no upstream code) run in an isolated process with host proxy. Measure: dex load from a descriptor; network through proxy; kill on hang; memory cost; latency per proxied call. Also check whether the isolated process can open a socket directly. | HushStream test code only | Written spike report in `docs/compatibility/` with measured numbers. Go to P3 only if dex load and proxy both work and a hang is recoverable. Otherwise record the fallback to Option C in `DECISION_LOG.md`. | Device run results. Numbers, not estimates. |
| **P3 — ABI shim, minimal** | Implement only the names and shapes needed by the **five surveyed extensions**, over the proxy. No upstream file copied (diff-checked). | Only after P2 passes, and only with owner-approved pinned extensions | Each of the five extensions installs, registers and returns one search result and one details result on device, against live sites. Failures are recorded per extension, not hidden. | Device run with per-extension results table. Legal note updated. |
| **P4 — Consent and trust** | Consent screen (§7.2), per-extension host report, signature or pinned-key decision (Q4), revocation. | Yes (pinned set) | Every install and update goes through consent. A hash change forces re-consent. | Device run. |
| **P5 — Broader repositories** | Allow user-added repositories beyond the pinned set. | Yes (user-chosen) | Only after the owner accepts the trust model in writing and a security review (QA/Security role) is done. | Security review record. Device run. |
| **Pre-release gate** | Full security review of the runtime, the proxy, and the consent flow. | — | No public release until P5 review is passed. | Review record in `docs/reverse-engineering/` or `docs/audit/`. |

Phase order is a hard rule: P3 cannot start before P2 exit, and P5 cannot start before P4 exit.

## 10. Review checklist (before any merge in P1 to P5)

- [ ] No upstream source file, method body or comment in the diff. Names only where the binary requires them.
- [ ] Shim lives in its own module; UI module does not depend on it.
- [ ] No upstream type appears in any `ui` or `app` package.
- [ ] No storage permission, no `MANAGE_EXTERNAL_STORAGE`, no cleartext config.
- [ ] Every proxied call has a timeout, a host log entry, and a body size limit.
- [ ] No token, library or history data is reachable from the runtime process.
- [ ] Branch check, owner-only files untouched, LICENSE intact (project rules).

## 11. Owner decisions needed

1. **Option D** (isolated process with host proxy) as the target, with Option C as the fallback if the spike fails. (ADR-023)
2. **Shim legal basis** (D-013 waiver stands). Owner confirms the shim is HushStream-authored from behaviour, with no upstream copied text.
3. **Cleartext HTTP.** Refuse `http://` for repositories and extensions (recommended). Provider `http://` streams: allow with a warning, or refuse?
4. **Trust model for v1.** Hash plus consent (recommended for P0 to P3), or signatures with a pinned key (recommended before P5)?
5. **Consent wording** in §7.2 step 3.
6. **Proxy policy.** Should every extension request be logged with its destination host to a local file, or only in memory for the current session?
7. **Default timeouts and circuit breaker thresholds** in §8 (proposals, not measured).
8. **Pinned extension set** for P3: which five extensions are approved for the first device run? (Owner must review them; this design does not endorse any.)

## 12. Verification status

| Claim | Status |
|---|---|
| Repository and plugin list format, hash match for five extensions | **[D]** inherited from `EXTENSION_ARCHITECTURE.md`; not re-checked this session |
| Upstream loading lifecycle and cleartext setting | **[V]** inherited; not re-read from upstream source this session |
| Android isolated process gives no network, no data access | **UNVERIFIED** |
| Dex loading from a file descriptor in an isolated process | **UNVERIFIED** |
| Same-app process shares the app's Linux user and files | **UNVERIFIED** (standard Android behaviour, not tested) |
| Timeouts, circuit breaker thresholds, size limits | **UNVERIFIED** (proposals) |
| Content of `DEPENDENCY_MAP.md` and `LICENSING_AUDIT.md` | **UNVERIFIED** (referenced, not re-read this session) |
| Any run of any extension, any build | **NOT TESTED.** No Android SDK in this sandbox. |
