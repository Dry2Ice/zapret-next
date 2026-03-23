#!/usr/bin/env python3
"""Local validator for strategy specs against strategy.schema.json."""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCHEMA_PATH = ROOT / "strategies" / "strategy.schema.json"
SPECS_DIR = ROOT / "strategies" / "specs"


def validate_with_jsonschema(schema: dict, docs: list[tuple[Path, dict]]) -> list[str]:
    try:
        import jsonschema
    except ImportError:
        return ["jsonschema dependency is not installed"]

    validator = jsonschema.Draft202012Validator(schema)
    errors: list[str] = []
    for path, doc in docs:
        for err in validator.iter_errors(doc):
            where = ".".join(str(x) for x in err.path) or "<root>"
            errors.append(f"{path}: {where}: {err.message}")
    return errors


def minimal_validate(schema: dict, docs: list[tuple[Path, dict]]) -> list[str]:
    required = schema["required"]
    errors: list[str] = []
    for path, doc in docs:
        for key in required:
            if key not in doc:
                errors.append(f"{path}: missing required key '{key}'")
        if "specVersion" in doc and not isinstance(doc["specVersion"], str):
            errors.append(f"{path}: specVersion must be string")
        if "priority" in doc and not isinstance(doc["priority"], int):
            errors.append(f"{path}: priority must be integer")
        if "filters" in doc and not isinstance(doc["filters"], dict):
            errors.append(f"{path}: filters must be object")
    return errors


def load_docs() -> tuple[dict, list[tuple[Path, dict]]]:
    schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    docs: list[tuple[Path, dict]] = []
    for file_path in sorted(SPECS_DIR.glob("*.json")):
        docs.append((file_path.relative_to(ROOT), json.loads(file_path.read_text(encoding="utf-8"))))
    return schema, docs


def main() -> int:
    schema, docs = load_docs()
    if not docs:
        print("No strategy specs found", file=sys.stderr)
        return 1

    errors = validate_with_jsonschema(schema, docs)
    if errors and errors[0].startswith("jsonschema dependency"):
        print("[warn] jsonschema not available, running minimal validator")
        errors = minimal_validate(schema, docs)

    if errors:
        print("Validation failed:")
        for err in errors:
            print(f" - {err}")
        return 1

    print(f"Validated {len(docs)} strategy specs successfully")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
