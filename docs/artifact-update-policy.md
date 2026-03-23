# Artifact update policy (lists / strategies / assets)

## Independent channels

Artifacts are managed independently by channel:

- `lists` → `lists/`
- `strategies` → `strategies/`
- `assets` → `bin/`

## Versioned manifest + integrity

Manifest location: `artifacts/manifests/artifact-manifest.json`.

The manifest stores, per channel:

- `version`
- `sha256`
- `lastUpdated`

and global:

- `appVersion`
- `specVersion`
- `signature` (`sha256` over normalized payload)

## Compatibility policy

`appVersion` ↔ `specVersion` policy:

- major versions must match (e.g., `1.x.x` app only accepts `1.x.x` spec).

## Staged update and rollback

`artifact_manager.py` commands:

1. `stage <channel> <source>` — copy candidate artifacts to `.staging/<channel>`.
2. `apply <channel>` — backup live artifacts to `.rollback/<channel>` and atomically switch to staged.
3. `rollback <channel>` — restore previous working state from `.rollback/<channel>`.
4. `verify` — verify manifest hash/signature/compatibility before apply.

## UI provenance/integrity indicator

Quick Start UI surfaces per-channel provenance:

- source channel
- channel version
- last update
- signature state (`ok/invalid`)
- hash prefix
