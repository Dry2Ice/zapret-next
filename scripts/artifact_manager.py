#!/usr/bin/env python3
"""Artifact manager: independent channel updates with manifest verification, staging and rollback."""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from dataclasses import dataclass
from pathlib import Path
from typing import Dict

ROOT = Path(__file__).resolve().parents[1]
MANIFEST_PATH = ROOT / "artifacts" / "manifests" / "artifact-manifest.json"
STAGING_ROOT = ROOT / ".staging"
ROLLBACK_ROOT = ROOT / ".rollback"

CHANNEL_PATHS = {
    "lists": ROOT / "lists",
    "strategies": ROOT / "strategies",
    "assets": ROOT / "bin",
}


@dataclass
class ChannelMeta:
    version: str
    sha256: str
    last_updated: str


def hash_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def hash_dir(path: Path) -> str:
    h = hashlib.sha256()
    if not path.exists():
        return ""
    for file in sorted(p for p in path.rglob("*") if p.is_file()):
        h.update(str(file.relative_to(path)).encode("utf-8"))
        h.update(hash_file(file).encode("utf-8"))
    return h.hexdigest()


def compatibility_ok(app_version: str, spec_version: str) -> bool:
    # Policy: major app version must equal major spec version.
    return app_version.split(".", 1)[0] == spec_version.split(".", 1)[0]


def load_manifest(path: Path = MANIFEST_PATH) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def signature_payload(manifest: dict) -> str:
    channels = manifest["channels"]
    ordered = {
        key: {
            "version": channels[key]["version"],
            "sha256": channels[key]["sha256"],
        }
        for key in sorted(channels)
    }
    base = {
        "manifestVersion": manifest["manifestVersion"],
        "appVersion": manifest["appVersion"],
        "specVersion": manifest["specVersion"],
        "channels": ordered,
    }
    return json.dumps(base, separators=(",", ":"), ensure_ascii=False)


def signature_value(manifest: dict) -> str:
    return hashlib.sha256(signature_payload(manifest).encode("utf-8")).hexdigest()


def verify_manifest(manifest: dict) -> list[str]:
    errors: list[str] = []
    if not compatibility_ok(manifest["appVersion"], manifest["specVersion"]):
        errors.append("appVersion/specVersion compatibility check failed")

    for channel, path in CHANNEL_PATHS.items():
        expected = manifest["channels"][channel]["sha256"]
        actual = hash_dir(path)
        if expected != actual:
            errors.append(f"{channel} hash mismatch: expected={expected} actual={actual}")

    expected_signature = manifest["signature"]["value"]
    actual_signature = signature_value(manifest)
    if expected_signature != actual_signature:
        errors.append("manifest signature mismatch")

    return errors


def generate_manifest(app_version: str, spec_version: str) -> dict:
    channels: Dict[str, dict] = {}
    for name, path in CHANNEL_PATHS.items():
        channels[name] = {
            "version": "1.0.0",
            "sha256": hash_dir(path),
            "lastUpdated": "generated",
        }

    manifest = {
        "manifestVersion": "1.0.0",
        "appVersion": app_version,
        "specVersion": spec_version,
        "channels": channels,
        "signature": {
            "algorithm": "sha256",
            "value": "",
        },
    }
    manifest["signature"]["value"] = signature_value(manifest)
    return manifest


def stage_update(channel: str, source_dir: Path) -> Path:
    target = STAGING_ROOT / channel
    if target.exists():
        shutil.rmtree(target)
    shutil.copytree(source_dir, target)
    return target


def apply_staged(channel: str) -> None:
    staged = STAGING_ROOT / channel
    if not staged.exists():
        raise FileNotFoundError(f"staged channel not found: {channel}")

    live = CHANNEL_PATHS[channel]
    backup = ROLLBACK_ROOT / channel
    backup.parent.mkdir(parents=True, exist_ok=True)

    if backup.exists():
        shutil.rmtree(backup)
    if live.exists():
        shutil.copytree(live, backup)
        shutil.rmtree(live)

    shutil.copytree(staged, live)


def rollback(channel: str) -> None:
    live = CHANNEL_PATHS[channel]
    backup = ROLLBACK_ROOT / channel
    if not backup.exists():
        raise FileNotFoundError(f"no rollback snapshot for {channel}")

    if live.exists():
        shutil.rmtree(live)
    shutil.copytree(backup, live)


def cmd_generate(args: argparse.Namespace) -> None:
    manifest = generate_manifest(args.app_version, args.spec_version)
    MANIFEST_PATH.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST_PATH.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Manifest written to {MANIFEST_PATH}")


def cmd_verify(_: argparse.Namespace) -> None:
    manifest = load_manifest()
    errors = verify_manifest(manifest)
    if errors:
        print("Manifest verification failed:")
        for e in errors:
            print(f" - {e}")
        raise SystemExit(1)
    print("Manifest verification passed")


def cmd_stage(args: argparse.Namespace) -> None:
    target = stage_update(args.channel, Path(args.source).resolve())
    print(f"Staged {args.channel} -> {target}")


def cmd_apply(args: argparse.Namespace) -> None:
    apply_staged(args.channel)
    print(f"Applied staged update for {args.channel}")


def cmd_rollback(args: argparse.Namespace) -> None:
    rollback(args.channel)
    print(f"Rolled back {args.channel}")


def parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description="Artifact manager")
    sub = p.add_subparsers(dest="cmd", required=True)

    g = sub.add_parser("generate-manifest")
    g.add_argument("--app-version", default="1.0.0")
    g.add_argument("--spec-version", default="1.0.0")
    g.set_defaults(func=cmd_generate)

    v = sub.add_parser("verify")
    v.set_defaults(func=cmd_verify)

    s = sub.add_parser("stage")
    s.add_argument("channel", choices=sorted(CHANNEL_PATHS))
    s.add_argument("source")
    s.set_defaults(func=cmd_stage)

    a = sub.add_parser("apply")
    a.add_argument("channel", choices=sorted(CHANNEL_PATHS))
    a.set_defaults(func=cmd_apply)

    r = sub.add_parser("rollback")
    r.add_argument("channel", choices=sorted(CHANNEL_PATHS))
    r.set_defaults(func=cmd_rollback)

    return p


def main() -> None:
    p = parser()
    args = p.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
