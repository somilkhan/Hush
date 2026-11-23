# COMPATIBILITY MATRIX — CloudStream extensions and repositories

Owner: Agent 1. Reviewer: Agent 2 (per handover §3).
Reference: CloudStream `a28e6468`. Index and extensions fetched 2026-11-24.

## Status legend

| Code | Meaning |
|---|---|
| **NOT TESTED** | Nothing run. This is the status of every operation in HUSH today. |
| **STATIC** | Verified by reading source or bytes. Not run. Does **not** prove compatibility. |
| **DATA-VERIFIED** | Verified against public data (hashes, manifests). Not loaded by any app. |
| **PASS / FAIL (device)** | Executed on a named Android device or emulator. None exists yet. |

**Current verdict: no operation is verified as compatible.** The only evidence is static or data-level.

## 1. Environment for testing

| Item | State | Evidence |
|---|---|---|
| Java | Present | `java -version` (Picked up JAVA_TOOL_OPTIONS line printed) |
| Gradle | 8.14.3 present | `gradle -v` |
| Android SDK | **Not installed** (`ANDROID_HOME` unset) | `echo $ANDROID_HOME` |
| adb / emulator | **Not present** | `which adb emulator` |
| Maven Central | **Blocked** from this session (HTTP 403 through agent proxy) | `curl` to `repo1.maven.org` |
| GitHub raw / clone | Reachable | clone and raw fetches succeeded |
| Android device | None | N/A |

Consequence: **no Android build and no extension execution is possible in this session.** Any PASS entry needs an emulator or device and an Android SDK.

## 2. Test matrix (from MASTER_HANDOVER §2 Agent 1 D)

| # | Operation | Required evidence | Status | Evidence so far | Blocker |
|---|---|---|---|---|---|
| 1 | Repository installation (add URL, parse `repo.json`) | App adds repo and lists it | **NOT TESTED** | `repo.json` fetched and format checked (DATA-VERIFIED); code path read (STATIC) | No app build |
| 2 | Extension discovery (`plugins.json` parse) | App lists the 5 entries | **NOT TESTED** | Entries parsed by hand; all fields present; unknown `jar*` fields ignored per source (STATIC) | No app build |
| 3 | Extension installation (download, hash check, store) | `.cs3` saved at expected path; hash check enforced | **NOT TESTED** | All five `fileHash` values **match** the downloaded file (DATA-VERIFIED, `sha256sum`) | No app build |
| 4 | Extension loading (`PathClassLoader`, manifest, `pluginClassName`) | Plugin class instantiated | **NOT TESTED** | Manifest of Dailymotion read; `pluginClassName` present (DATA-VERIFIED); loader read (STATIC) | Needs ABI host |
| 5 | Provider registration (`registerMainAPI`) | Provider appears in list | **NOT TESTED** | Extension references `registerMainAPI` (DATA-VERIFIED, dex strings) | Needs ABI host |
| 6 | Search | Non-empty results from a live provider | **NOT TESTED** | Search override present in dex (DATA-VERIFIED) | Needs ABI host and network |
| 7 | Media details (`load`) | `LoadResponse` with title and episode list | **NOT TESTED** | `load` override present (DATA-VERIFIED) | Needs ABI host and network |
| 8 | Episode loading | Episode `data` values returned | **NOT TESTED** | `Episode` referenced by extensions (DATA-VERIFIED) | Needs ABI host and network |
| 9 | Stream extraction (`loadLinks`) | At least one `ExtractorLink` callback | **NOT TESTED** | **`loadLinks` not observed in any of the 5 extensions** (DATA-VERIFIED). Shape from source (STATIC). | No sample extension implements it |
| 10 | Subtitle extraction | `SubtitleFile` callback fired | **NOT TESTED** | `SubtitleFile` referenced in 5 of 5 (DATA-VERIFIED) but no subtitle-producing run | Needs ABI host, `loadLinks` sample |
| 11 | Failure handling (throw in `load`, `search`, `loadLinks`; missing class; hash mismatch; HTTP repo) | Caught, surfaced, app stays up | **NOT TESTED** | Caught-exception paths read (STATIC); hash mismatch throws (STATIC) | No app build |

## 3. Extension inventory (published index, 2026-11-24)

Status values: DATA-VERIFIED means the hash and archive structure were checked. None were loaded.

| internalName | Version (index) | Size (bytes) | Index status | Hash of downloaded `.cs3` | Archive | Host symbols referenced (dex) | Run in HUSH |
|---|---|---|---|---|---|---|---|
| DailymotionProvider | 4 | 11,472 | 1 (OK) | MATCH | manifest.json + classes.dex | CloudStream API set, nicehttp, Jackson, jsoup, OkHttp | NOT TESTED |
| InternetArchiveProvider | 1 | 28,037 | 1 | MATCH | manifest.json + classes.dex | CloudStream API set | NOT TESTED |
| InvidiousProvider | 9 | 13,695 | 1 | MATCH | manifest.json + classes.dex | CloudStream API set | NOT TESTED |
| TwitchProvider | 2 | 15,239 | 1 | MATCH | manifest.json + classes.dex | CloudStream API set | NOT TESTED |
| YoutubeProvider | 1 | 13,152 | 1 | MATCH | manifest.json + classes.dex | CloudStream API set | NOT TESTED |

Source: `https://raw.githubusercontent.com/recloudstream/extensions/builds/plugins.json`, and each `.cs3` at the `url` field. Files kept in the session scratchpad at `/tmp/claude-0/-home-claude/fb3b8b07-ec76-52e6-b25e-290dc1213f89/scratchpad/extensions_repo_probe/all/`.

Not yet covered: any extension that overrides `loadLinks`, any extension with `requiresResources: true`, any extension that uses `getVideoInterceptor`, and any extension from a repository other than the official one.

## 4. Repository inventory

| Repository | URL | Format | Status |
|---|---|---|---|
| Official CloudStream extensions | `https://raw.githubusercontent.com/recloudstream/extensions/master/repo.json` | `manifestVersion` 1 | DATA-VERIFIED (format). NOT TESTED (install). Licence of repo contents **not verified**. |
| Any third-party repository | — | — | Not examined. Short-code forms `cutt.ly`, `py.md` not exercised. |

## 5. Security tests (from EXTENSION_ARCHITECTURE §5.3)

| Test | Status |
|---|---|
| Missing `pluginClassName` class | NOT TESTED |
| Extension throws in `load()` | NOT TESTED |
| Extension throws in `search()` | NOT TESTED |
| Hash mismatch refused | NOT TESTED (logic read, STATIC) |
| Signature mismatch refused | NOT TESTED (no signatures exist in CloudStream) |
| `http://` repository refused | NOT TESTED (CloudStream accepts it; HUSH design proposal only) |
| App-private file access from extension | NOT TESTED |

## 6. What would move a row to PASS

1. Android SDK and build-tools installed (not present now).
2. A HUSH ABI shim that satisfies the names in `EXTENSION_ARCHITECTURE.md` §3 (blocked on decision D-A).
3. An Android emulator or device, with the API level recorded.
4. A test harness that calls each operation in §2 against a named extension and records the result, version, and device.
5. At least one extension that overrides `loadLinks`, so rows 9 and 10 can be tested.

Until 1 to 4 exist, no row may be marked PASS. CloudStream's own `app/.../utils/TestingUtils.kt` (`testHomepage`, `search`, `load`, `loadLinks`) is a useful reference for a harness design; it is not a test run.

## 7. Verified facts vs inference (summary)

| Claim | Basis |
|---|---|
| Extensions require the `com.lagradost.cloudstream3` names at run time | DATA-VERIFIED (dex string tables of 5 of 5) |
| Index hash equals downloaded file hash | DATA-VERIFIED (`sha256sum`, 5 of 5) |
| Extensions can run without a host | **Inference: false**, since they link against host classes |
| A HUSH build with `com.lagradost.cloudstream3` shim will load them | **NOT TESTED** |
| Extension behaviour at run time | **NOT TESTED** |
