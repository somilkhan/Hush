# Extension metadata parser (S0, core/domain)

Status: **UNVERIFIED.** Written without a build (no Android SDK or Gradle run in this session). Only CI `:core:domain:test` will verify it. Not checked against real CloudStream repositories.

## Scope

A pure-Kotlin parser that reads a repository index and lists extension metadata. HUSH can show extensions before any loader exists.

- Package: `app.hushstream.core.domain.compat` (in `core/domain`).
- Does not fetch URLs, does not load or execute extension code, does not read Android APIs.
- Dependency added: `kotlinx-serialization-json` 1.7.3 (catalog key `kotlinx-serialization-json`), `core/domain` only. Used through the JSON tree API (`JsonObject`, `JsonArray`, `JsonPrimitive`), so no Kotlin serialization compiler plugin is needed. Version UNVERIFIED against Maven.

## Schema

This is a **HUSH schema**, defined from HUSH requirements. It is not CloudStream's format. No CloudStream code was read or copied.

```json
{
  "name": "Repository name",
  "extensions": [
    {
      "internalName": "sample.movies",
      "name": "Sample Movies",
      "version": 3,
      "sourceUrl": "https://example.org/sample-movies.pkg",
      "sha256": "<64 hex chars, optional>",
      "languages": ["en"],
      "types": ["movie", "series"],
      "iconUrl": "https://example.org/icon.png",
      "permissions": ["network"],
      "description": "optional",
      "authors": ["optional"]
    }
  ]
}
```

| Field | Required | Rule |
|---|---|---|
| `name` (index) | yes | non-blank string |
| `extensions` (index) | yes | array |
| `internalName` | yes | 1 to 64 chars from `[A-Za-z0-9_.-]` |
| `name` | yes | non-blank string |
| `version` | yes | integer >= 0 (JSON number, not a string) |
| `sourceUrl` | yes | https URL with a host |
| `sha256` | no | 64 hex chars |
| `iconUrl` | no | https URL |
| `languages`, `types`, `permissions`, `authors` | no | array of non-blank strings; default empty |
| `description` | no | string; default `""` |

`types` values are kept as declared (for example `live`), so the schema does not need a new release when a new type appears.

## Result contract

`RepositoryIndexParser.parse(text, repositoryUrl): RepositoryParseResult`

- `Success(repository, skipped)`: `skippedCount` is `skipped.size`. `skipped` lists each dropped entry with its array index and reason.
- `Failure(reason, message)`: whole-index problems only.

| `FailureReason` | Cause |
|---|---|
| `EMPTY_INPUT` | blank text |
| `INVALID_JSON` | not parseable JSON |
| `SCHEMA` | root not an object, missing or blank `name`, `extensions` not an array |
| `INSECURE_URL` | repository URL is not https (D-013; security audit F4) |

Per-entry problems never fail the whole index. A malformed entry (wrong type, missing field, invalid URL, bad pattern) is skipped and counted. That keeps one bad entry from hiding the rest of a repository.

## Security notes

- Every download URL in the schema (`sourceUrl`, `iconUrl`) must be https. An http URL is treated as malformed and skipped, not silently upgraded.
- The repository URL must be https, or the whole index is refused.
- `sha256` is parsed and kept so the future loader can verify the download. Nothing verifies it yet.
- `permissions` is informational in this slice. It is not enforced. Enforcement belongs to the isolation design (agent/architect) and the security review (ADR-008).

## Open decisions (owner)

1. Real CloudStream repository format: whether HUSH must read the upstream index format directly. If yes, this schema needs an adapter. M3 review was waived (D-013), so no upstream copying happens without a licence check.
2. Whether `sha256` becomes required before any extension is loaded.
3. Whether index entries should carry full metadata (this slice) or only a reference that is fetched separately.

## Tests

`core/domain/src/test/kotlin/app/hushstream/core/domain/compat/RepositoryIndexParserTest.kt`:

- valid index with all fields and defaults
- malformed entries skipped and counted, with correct indices
- empty input (blank and whitespace)
- empty extension list
- non-https repository URL rejected (http, ftp, no scheme)
- invalid JSON
- wrong top-level shapes

Verification: UNVERIFIED (no build run). CI `:core:domain:test` is the evidence to check.
