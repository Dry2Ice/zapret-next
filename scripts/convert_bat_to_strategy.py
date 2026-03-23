#!/usr/bin/env python3
"""Convert legacy general*.bat winws profiles into strategy specs."""
from __future__ import annotations

import json
import re
import shlex
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC_DIR = ROOT / "strategies" / "specs"
MAPPING_FILE = ROOT / "strategies" / "bat-to-spec-map.json"


def slugify(stem: str) -> str:
    value = stem.lower()
    value = re.sub(r"[^a-z0-9]+", "-", value)
    return value.strip("-")


def parse_bat(file_path: Path) -> dict:
    lines = file_path.read_text(encoding="utf-8", errors="ignore").splitlines()
    command_lines: list[str] = []
    capture = False
    for line in lines:
        stripped = line.strip()
        if not capture and stripped.lower().startswith("start ") and "winws.exe" in stripped.lower():
            capture = True
        if capture:
            command_lines.append(stripped.rstrip("^ "))
            if not stripped.endswith("^"):
                break

    if not command_lines:
        raise ValueError(f"No winws command found in {file_path}")

    cmd = " ".join(command_lines)
    cmd = re.sub(r"^start\s+\"[^\"]*\"\s+/min\s+", "", cmd, flags=re.IGNORECASE)

    tokens = shlex.split(cmd, posix=False)
    binary = tokens[0]
    args = tokens[1:]

    sections: list[list[str]] = [[]]
    for arg in args:
        if arg == "--new":
            sections.append([])
        else:
            sections[-1].append(arg)

    wf_tcp = None
    wf_udp = None
    stages = []
    required_assets = set()

    for idx, section in enumerate(sections):
        match: dict[str, str] = {}
        desync_params: dict[str, str] = {}
        stage_assets: set[str] = set()

        for token in section:
            if not token.startswith("--"):
                continue
            key, sep, value = token[2:].partition("=")
            if key == "wf-tcp":
                wf_tcp = value
                continue
            if key == "wf-udp":
                wf_udp = value
                continue

            if key.startswith("filter-") or key.startswith("hostlist") or key.startswith("ipset") or key == "ip-id":
                match[key] = value if sep else "1"
                if value.lower().endswith((".bin\"", ".txt\"", ".bin", ".txt")):
                    stage_assets.add(value.strip('"'))
                continue

            if key.startswith("dpi-desync"):
                desync_params[key] = value if sep else "1"
                if value.lower().endswith((".bin\"", ".txt\"", ".bin", ".txt")):
                    stage_assets.add(value.strip('"'))

        if desync_params or match:
            stages.append(
                {
                    "index": idx,
                    "filters": match,
                    "desyncParams": desync_params,
                    "requiredAssets": sorted(stage_assets),
                }
            )
            required_assets.update(stage_assets)

    strategy_id = slugify(file_path.stem)
    return {
        "specVersion": "1.0.0",
        "id": strategy_id,
        "priority": 100,
        "sourceBat": file_path.name,
        "engine": {
            "binary": binary,
            "wfTcp": wf_tcp,
            "wfUdp": wf_udp,
        },
        "filters": {
            "stages": stages,
        },
        "desyncParams": {
            "stageCount": len(stages),
        },
        "requiredAssets": sorted(required_assets),
    }


def main() -> None:
    SPEC_DIR.mkdir(parents=True, exist_ok=True)
    mapping = {}
    bat_files = sorted(ROOT.glob("general*.bat"), key=lambda p: p.name.lower())
    for bat_file in bat_files:
        spec = parse_bat(bat_file)
        spec_name = f"{spec['id']}.strategy.json"
        (SPEC_DIR / spec_name).write_text(json.dumps(spec, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        mapping[bat_file.name] = f"strategies/specs/{spec_name}"

    MAPPING_FILE.write_text(json.dumps(mapping, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Converted {len(mapping)} BAT profiles")


if __name__ == "__main__":
    main()
