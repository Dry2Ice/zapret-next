"""Adapters for translating Strategy Spec to platform-specific runtime configs."""
from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class WindowsAdapter:
    """Legacy adapter producing winws CLI arguments for Windows BAT compatibility."""

    def to_legacy_command(self, spec: dict[str, Any]) -> str:
        engine = spec.get("engine", {})
        binary = engine.get("binary", "winws.exe")
        args: list[str] = [f'"{binary}"']

        if engine.get("wfTcp"):
            args.append(f'--wf-tcp={engine["wfTcp"]}')
        if engine.get("wfUdp"):
            args.append(f'--wf-udp={engine["wfUdp"]}')

        for stage in spec.get("filters", {}).get("stages", []):
            for key, value in stage.get("filters", {}).items():
                args.append(f"--{key}={value}" if value != "1" else f"--{key}")
            for key, value in stage.get("desyncParams", {}).items():
                args.append(f"--{key}={value}" if value != "1" else f"--{key}")
            args.append("--new")

        if args[-1] == "--new":
            args.pop()

        return " ".join(args)


@dataclass(frozen=True)
class AndroidAdapter:
    """Adapter that maps Strategy Spec into TrafficEngine profile payload."""

    def to_engine_profile(self, spec: dict[str, Any]) -> dict[str, Any]:
        stages = spec.get("filters", {}).get("stages", [])
        return {
            "specVersion": spec.get("specVersion"),
            "strategyId": spec.get("id"),
            "priority": spec.get("priority"),
            "requiredAssets": spec.get("requiredAssets", []),
            "pipeline": [
                {
                    "index": stage.get("index"),
                    "filters": stage.get("filters", {}),
                    "desyncParams": stage.get("desyncParams", {}),
                }
                for stage in stages
            ],
        }
