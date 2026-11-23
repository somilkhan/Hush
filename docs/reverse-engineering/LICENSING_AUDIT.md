# LICENSING AUDIT — CloudStream reference and dependencies

Owner: Agent 1. Reviewer: Claude (per MASTER_HANDOVER §3).
Audit date: 2026-11-24. Reference revision: CloudStream `a28e6468f54cfe420feebd205d3d21d08ed0cde8` (2026-11-22).

Status legend: **[V]** verified by reading the cited file in this session. **[D]** verified against public data (raw GitHub file, fetched this session). **[I]** inference, not verified. **[NV]** not verified in this session.

This document is an engineering audit, not legal advice. Any "reuse" decision above the level of "reimplement from documented behaviour" needs a qualified lawyer's review before code is copied.

---

## 1. Verdict in one paragraph

CloudStream is licensed under the **GNU GPL v3.0** [V: `LICENSE` at repo root, header "GNU GENERAL PUBLIC LICENSE Version 3, 29 June 2007"; the file is byte-identical to the GPL-3.0 text served by other GPL-3 projects checked below]. Copying or adapting any CloudStream source file into HUSH would make that file a derivative work under GPL-3, and any HUSH binary that contains it would have to be distributed under GPL-3 with corresponding source. HUSH's stated direction (independent, not GPL-bound) therefore means **no CloudStream source may be copied or ported**. Clean-room reimplementation from documented behaviour is the default route. The one hard problem is that existing `.cs3` extensions link at runtime against CloudStream's API class names (`com.lagradost.cloudstream3.*`, see `EXTENSION_ARCHITECTURE.md` §3). Providing those names inside HUSH is an API-compatibility question with its own legal exposure and **must be decided by Claude and the user before Agent 2 builds the compatibility layer**.

---

## 2. CloudStream repository (the reference)

| Item | Finding | Status | Evidence |
|---|---|---|---|
| Licence | GPL-3.0 | [V] | `LICENSE` (35,149 bytes) |
| Per-file headers | Only 7 of 521 `.kt` files under `library/`, `app/`, `shared/` mention Copyright / GPL / SPDX / License in their first 15 lines. Most files rely on the root LICENSE. | [V] | grep over `library/ app/ shared/` at a28e6468 |
| Copyright holder | Not stated in the root LICENSE (it is the FSF template). Per-file copyright notes are sparse, so authorship is unknown. | [V] | `LICENSE`, file headers |
| Extension repo licence | `recloudstream/extensions` repo: README read; **its LICENSE file was not fetched**. Licence of individual community extensions is **[NV]**. | [NV] | `https://raw.githubusercontent.com/recloudstream/extensions/master/README.md` |
| Plugin-system provenance | The extensions README says: "The gradle plugin and the whole plugin system is **heavily** based on Aliucord". Code-level lineage was **not** diffed. | [D] claim, [NV] lineage | `recloudstream/extensions` README |

### 2.1 Aliucord (provenance of the plugin system)

- Aliucord licence: **Open Software License (OSL) v3.0** [D: `https://raw.githubusercontent.com/Aliucord/Aliucord/master/LICENSE`, begins "Open Software License ("OSL") v. 3.0"].
- Implication: if CloudStream's plugin loader was derived from Aliucord code, that derivation carries OSL-3.0 obligations on the derived code, separate from GPL-3. Whether it was derived, or only inspired by, is **[NV]**.
- Action: before HUSH borrows the plugin-loader *design*, Agent 1 must diff CloudStream's `PluginManager` / `BasePlugin` against Aliucord's loader to establish whether any expression was copied. Ideas and public behaviour are not protected by copyright; code and distinctive expression are.

---

## 3. Dependency licences

Method: the Maven Central POMs could **not** be fetched. `repo1.maven.org` returned HTTP 403 from the agent proxy (`curl: CONNECT tunnel failed, response 403`), which the proxy README says to report, not route around. Licences below are therefore taken from the **upstream project's LICENSE file on GitHub**, fetched this session. The version named may differ from the upstream branch fetched; that is stated per row.

### 3.1 Dependencies that CloudStream's host runtime requires for extensions (highest priority)

| Dependency (version in CloudStream) | Upstream LICENSE fetched | Licence | Status | Reuse consequence for HUSH |
|---|---|---|---|---|
| NiceHttp `com.github.Blatzar:NiceHttp` 0.4.18 | `Blatzar/NiceHttp` master `LICENSE` | **GPL-3.0** (byte-identical to CloudStream LICENSE) | [D]; branch is master not the 0.4.18 tag | **Copyleft.** Extensions call nicehttp classes directly (`com/lagradost/nicehttp/Requests`, `ResponseParser`, `NiceResponse`, verified in dex). HUSH must either provide an equivalent API (clean-room) or accept GPL-3 for the combined binary. |
| jsoup 1.22.1 | `jhy/jsoup` master `LICENSE` | MIT (Copyright 2009-2026 Jonathan Hedley) | [D] | Permissive. Extensions reference `org/jsoup/select/Elements`. |
| ksoup 0.2.6 (jsoup port) | `fleeksoft/ksoup` master `LICENSE` | MIT (keeps Jonathan Hedley's notice) | [D] | Permissive. Keep notice. |
| jackson-module-kotlin 2.13.1 | `FasterXML/jackson-module-kotlin` branch `2.13` `LICENSE` | Apache-2.0 | [D] | Permissive. Extensions reference Jackson `ObjectMapper`, `JsonMapper`, `TypeReference`. |
| Rhino 1.8.1 (`org.mozilla:rhino`) | `mozilla/rhino` master `LICENSE.txt` | **MPL-2.0** for the majority of Rhino (file says so explicitly) | [D] | File-level copyleft: modifications to Rhino files must be shared under MPL-2.0. Usable in a closed combined work. |
| OkHttp (via Coil / media3) | Not fetched individually | [NV] (OkHttp is Apache-2.0 upstream per general knowledge) | [NV] | Extensions reference `okhttp3/Interceptor`. Confirm before relying. |
| Kotlin stdlib / kotlinx.coroutines / kotlinx.serialization / kotlinx-datetime | `JetBrains/kotlin`, `Kotlin/kotlinx.coroutines`, `Kotlin/kotlinx.serialization`, `Kotlin/kotlinx-datetime` | Apache-2.0 | [D] | Permissive. |

### 3.2 Other CloudStream dependencies (not required by extensions)

| Dependency (version) | Upstream LICENSE fetched | Licence | Status | Reuse consequence |
|---|---|---|---|---|
| NewPipe Extractor v0.26.3 (trailers) | `TeamNewPipe/NewPipeExtractor` dev `LICENSE` | **GPL-3.0** (byte-identical to CloudStream LICENSE) | [D] | Copyleft. Avoid unless HUSH is GPL-3. |
| nextlib-media3ext / nextlib-mediainfo 1.9.3-0.12.0 (player) | `anilbeesetti/nextlib` master `LICENSE` | **GPL-3.0** (byte-identical) | [D] | Copyleft. Player-extension code: do not depend on it without a decision. |
| juniversalchardet 2.5.0 (subtitle decoding) | `albfernandez/juniversalchardet` master `LICENSE` | Tri-licence: **MPL-1.1 OR GPL-2.0-or-later OR LGPL-2.1-or-later** (the library may be used under any one) | [D] | Choose MPL-1.1 or LGPL-2.1 for HUSH; do not choose GPL. |
| fuzzywuzzy 1.4.0 (`me.xdrop`) | `xdrop/fuzzywuzzy` master `LICENSE` | GPL-2.0 text. The "or later" wording appears only in the FSF boilerplate appendix. The author's actual grant must be checked in a source header. | [D] text; grant **[NV]** | Do not use. GPL-2 and GPL-3 do not combine cleanly unless "or later" is granted. |
| Shimmer 0.5.0 (`facebookarchive/shimmer-android`) | `facebookarchive/shimmer-android` master `LICENSE` | BSD (Meta copyright, redistribution conditions) | [D] | Permissive; archived upstream. Prefer a Compose-native skeleton. |
| conscrypt-android 2.5.2 | `google/conscrypt` master `LICENSE` | Apache-2.0 | [D] | Permissive. |
| Coil 3.3.0 | `coil-kt/coil` main `LICENSE.txt` | Apache-2.0 | [D] | Permissive. |
| media3 1.9.3 (androidx.media3) | `androidx/media` release `LICENSE` | Apache-2.0 | [D] | Permissive. |
| zipline 1.27.0 | `cashapp/zipline` master `LICENSE` | Apache-2.0 | [D] | Permissive. |
| cryptography-kotlin 0.6.0 | `whyoleg/cryptography-kotlin` main `LICENSE` | Apache-2.0 | [D] | Permissive. |
| gson 2.11.0 | `google/gson` main `LICENSE` | Apache-2.0 | [D] | Permissive. |
| qrcode-kotlin 4.5.0 | `g0dkar/qrcode-kotlin` master `LICENSE` | MIT | [D] | Permissive. |
| AndroidX (activity, appcompat, lifecycle, navigation, work, material, etc.) | `androidx/androidx` `androidx-main` `LICENSE.txt` | Apache-2.0 | [D] for the repo LICENSE; per-artifact POMs [NV] | Permissive. |
| Compose Multiplatform 1.12.0 | `JetBrains/compose-multiplatform` `LICENSE.txt` | Apache-2.0 | [D] | Permissive. |
| anime-db 1.0.2, color-picker-android (`recloudstream` JitPack fork), overlappingpanels 0.1.5, previewseekbar-media3 1.1.1.0, torrentserver, safefile 0.0.8, video 1.0.0 | Not fetched | **[NV]** | [NV] | Each needs its licence confirmed (POM or repo) before HUSH uses it. The JitPack `recloudstream` forks are the highest risk because their licence is not stated in the repo's `libs.versions.toml`. |

Note on Aliucord-derived code and extension SDK: see §2.1.

---

## 4. What this means for reuse (decision table)

| Option | Legal exposure | Engineering effect | Recommendation |
|---|---|---|---|
| Copy CloudStream Kotlin source into HUSH | High. Whole derivative becomes GPL-3 with source. | Fast | **Reject** (conflicts with the independence requirement in the handover). |
| Clean-room reimplementation of behaviour, documented process, no source opened while writing a given component | Medium-low. Needs a documented clean-room process. | Slower | **Default.** Requires a `CLEAN_ROOM_LOG` written before each component. (Not yet created; proposed.) |
| Bundle CloudStream `library` module (the API) as-is | High. The API module is GPL-3. | Fastest compatibility | Only if the user accepts GPL-3 for HUSH. Decision required. |
| Re-expose CloudStream API names via a shim | Medium-high. API names and signatures are needed for binary compatibility. Legal treatment of API surfaces varies by jurisdiction and has not been assessed here. | Required for `.cs3` to run at all | **Escalate.** See `EXTENSION_ARCHITECTURE.md` §3 and decision D-A below. |
| Use nicehttp, NewPipe, nextlib as dependencies | GPL-3 | Easy | Reject unless HUSH is GPL-3. nicehttp is needed for extensions, so the same shim question applies. |
| Use MIT / Apache-2.0 / BSD dependencies (jsoup, Jackson, Coil, media3, Compose) | Low (keep notices) | Easy | Allowed, with notices in a `NOTICE` file. |

**Decision D-A (escalate to Claude and the user):** Extensions cannot run without a host that exposes `com.lagradost.cloudstream3.*` and `com.lagradost.nicehttp.*` class names (verified, `EXTENSION_ARCHITECTURE.md` §3). The options are (1) clean-room shim under those names, with legal review; (2) adopt GPL-3 for HUSH and use CloudStream's API module; (3) run extensions in a separate CloudStream-ABI companion component that carries the GPL-3 obligations only for itself. This session does not recommend one without legal input.

---

## 5. Obligations checklist (for any component HUSH does adopt)

- Keep all copyright and licence headers when a file is adopted (handover §6).
- Record the origin of every adopted file in `docs/reverse-engineering/COMPONENT_REUSE_MATRIX.md`.
- Ship a `NOTICE` / third-party licence screen listing every MIT, Apache-2.0, BSD and MPL dependency.
- Do not bundle any community extension or default repository without checking its licence (see §2 "Extension repo licence").
- Trademark: "CloudStream" must not appear in HUSH branding or store listing. HUSH itself is not legally cleared (`docs/DECISION_LOG.md` D-001).
- Non-licence risk for counsel: the product's purpose (playing streams from third-party providers) raises copyright and terms-of-service questions that are outside this audit.

---

## 6. Open items

1. Clone `recloudstream/extensions` and read its LICENSE (NV).
2. Diff CloudStream plugin loader against Aliucord to settle derivation (§2.1).
3. Obtain Maven POMs for the [NV] rows once the proxy allows `repo1.maven.org`, or via an approved route.
4. Confirm the fuzzywuzzy "or later" grant from a source header.
5. Legal review of decision D-A.

## 7. Sources (fetched this session)

- CloudStream `LICENSE`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `library/build.gradle.kts` at a28e6468 (local clone in the session scratchpad).
- `https://raw.githubusercontent.com/Blatzar/NiceHttp/master/LICENSE`
- `https://raw.githubusercontent.com/jhy/jsoup/master/LICENSE`
- `https://raw.githubusercontent.com/fleeksoft/ksoup/master/LICENSE`
- `https://raw.githubusercontent.com/FasterXML/jackson-module-kotlin/2.13/LICENSE`
- `https://raw.githubusercontent.com/mozilla/rhino/master/LICENSE.txt`
- `https://raw.githubusercontent.com/albfernandez/juniversalchardet/master/LICENSE`
- `https://raw.githubusercontent.com/xdrop/fuzzywuzzy/master/LICENSE`
- `https://raw.githubusercontent.com/TeamNewPipe/NewPipeExtractor/dev/LICENSE`
- `https://raw.githubusercontent.com/anilbeesetti/nextlib/master/LICENSE`
- `https://raw.githubusercontent.com/Aliucord/Aliucord/master/LICENSE`
- `https://raw.githubusercontent.com/recloudstream/extensions/master/README.md`
- Other Apache-2.0 / MIT / BSD files listed in §3 from the cited upstream repositories.
