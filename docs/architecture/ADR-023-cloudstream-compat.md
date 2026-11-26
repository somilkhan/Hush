# ADR-023 — CloudStream extension compatibility: isolated runtime, metadata first

- **Status:** PROPOSED. Needs owner sign-off before any Phase 3 work (see `docs/compatibility/ISOLATION_DESIGN.md` §9 and §11).
- **Date:** 2026-11-26
- **Deciders:** Owner (product and risk), Claude (Principal Architect), Agent 1 (compatibility)
- **Refines:** ADR-018 (provider failure isolation). ADR-018 chose in-process calls with timeouts "now", with process isolation as the upgrade path "before any public release" and before untrusted repositories are added. This ADR sets that upgrade path as a hard gate for the compatibility runtime.
- **Related:** D-013 (M3 legal review waived), D-020 (product scope), D-016 (sign-out data policy), ADR-020 (sideload distribution).

## Context

- D-020 makes CloudStream extension compatibility part of HushStream's product scope. Existing CloudStream repositories and extensions must keep working (handover §0/§6).
- CloudStream extensions are dex archives loaded and run inside the host app's process, with the app's permissions and private data (**[V]**, `docs/compatibility/EXTENSION_ARCHITECTURE.md` §5.1). Extensions are third-party code.
- Extensions are compiled against upstream type names. The runtime must provide those names with compatible shapes, or they fail to link. No negotiation exists (**[V]**).
- Extensions declare no permissions. Their manifest carries no capability list (**[D]**).
- The sandbox has no Android SDK, so nothing in this decision is verified by running code (**UNVERIFIED** for all platform claims in the design).
- The owner waived legal review of the compatibility surface (D-013). The owner carries that risk, and this ADR does not change it.

## Decision

1. **Metadata first.** Repository and plugin-list reading, hash checking, consent and removal ship before any extension code runs (Phase 0). No third-party code runs in Phase 0.
2. **Target runtime: an isolated Android process (`isolatedProcess`) with host-mediated network.** Extensions run in a process with no app-data access and no permissions. Network requests go through a HushStream-owned proxy that applies timeouts, host logging and size limits. Only metadata and stream URLs come back; media bytes are fetched by the player.
3. **Gate.** No third-party extension runs until the Phase 2 spike passes with measured results: dex loading from a descriptor, proxy path, recoverable kill on hang, and per-call cost. If the spike fails, the fallback is a same-app separate process (Option C). That fallback is a failure boundary, not a security boundary, and it must be recorded in `DECISION_LOG.md` with the owner's acceptance before any third-party repository is enabled.
4. **Shim.** The binary-compatibility shim is HushStream-authored, in its own module, implemented from observed behaviour and required shapes. It contains no upstream file, method body or comment. The UI never depends on it.
5. **Permissions.** Extensions get network through the proxy (logged) and nothing else. No access to HushStream data, tokens, storage, intents or background work.
6. **Consent.** Install and every hash-changing update require explicit user consent. Repositories must use HTTPS.
7. **Failure.** Per-call timeouts, process kill on hang, a per-extension circuit breaker, and mapping to HushStream's own `ProviderError` types. A failed repository refresh never removes installed extensions.

## Options considered

| Option | Summary | Result |
|---|---|---|
| A. Metadata only | No execution at all | Accepted as Phase 0. Not compatibility. |
| B. In-process dex loading (as upstream) | Full compatibility, no isolation | **Rejected.** Conflicts with ADR-018 driver 2, D-016 and the privacy driver. |
| C. Same-app separate process | Crash and hang isolation; same Linux user, same data | **Fallback** only, if D fails. Owner must accept. |
| **D. Isolated process with host proxy** | Crash isolation and data isolation, no second APK | **Chosen as target**, gated by the spike. |
| E. Companion APK | Strongest separation, own signature and data | **Reserved.** Revisit if D fails on data isolation or public untrusted repositories are added. Costs two APKs under ADR-020. |

## Consequences

- **Positive.** Extension crashes and hangs cannot take down the UI or reach user data (if the spike confirms the platform behaviour). The trust boundary is explicit and reviewable. Metadata work can proceed while the spike runs.
- **Negative.** Every HTTP call from an extension becomes a proxied call, which adds latency and needs a body size limit. The shim must be proxy-aware. Compatibility depends on a runtime that must load dex from a descriptor, which is unproven.
- **Risk.** If Android denies network or dex loading in an isolated process, Option D may be unworkable and Option C becomes the default, which weakens the data boundary. This is the main open technical risk.
- **Risk.** The shim has a legal status that the owner has accepted (D-013). The owner carries it.
- **Risk.** Running third-party extension code is a security exposure even inside the isolated process. The proxy limits what can be reached, but it does not stop an extension from making malicious requests within its allowed capability.

## Requirements addressed

No SRS exists (`docs/srs.md` missing). Drivers are stated in prose.

- Isolation of untrusted extension code (driver 2, `SOFTWARE_ARCHITECTURE.md` §1).
- Responsiveness: the main thread never waits on extension code (driver 3).
- Privacy and local control: no extension access to HushStream data (driver 1).
- Compatibility with existing CloudStream repositories and extensions (handover §0/§6, D-020).

## Verification

- Repository format, hash match, and surveyed ABI names: **[D]**, inherited from `EXTENSION_ARCHITECTURE.md`, not re-checked this session.
- Upstream runtime behaviour: **[V]** from source, inherited, not re-read this session.
- Platform behaviour of `isolatedProcess` (network, dex from descriptor, same-app process sharing): **UNVERIFIED**. The Phase 2 spike must test each.
- Build: none. No Android SDK in the sandbox. Build evidence is CI `assembleDebug` only, after the branch is pushed.

## Trade-offs

- Choosing D over B costs time before the first working provider. That is accepted to keep untrusted code out of the app's data.
- Choosing metadata first delays visible compatibility. That is accepted because it lets the repository UI and consent flow be built and checked on device earlier.
- A proxy-mediated network adds latency per call. The spike measures it; the owner decides whether it is acceptable.

## Affected modules

- `:core:compat` (owned by Agent 1): runtime, proxy, consent data.
- `compat/cloudstream-abi` (proposed): shim, isolated from UI.
- `:core:domain` (Agent 2): `ProviderPort`, `StreamEvent`, `ProviderError`.
- `:app`: wiring only.
- UI modules: no dependency on `:core:compat` (rule in `ARCHITECTURE.md`).

## Open questions for the owner

1. Approve Option D as target, with Option C as the spike fallback.
2. Confirm the shim legal basis (D-013 waiver; HushStream-authored, no upstream text).
3. Refuse cleartext HTTP for repositories and extensions? Provider streams?
4. v1 trust model: hash and consent, or pinned signatures before any public repository.
5. Which five surveyed extensions, if any, are approved for the first device run (Phase 3).
