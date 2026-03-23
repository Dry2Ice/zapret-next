# Android app architecture (domain/data/presentation)

## Use-cases

1. `install/start/stop profile`
2. `diagnostics`
3. `update lists`
4. `check status`

## Layers

- `domain/`: бизнес-модели, repository contracts, use-cases.
- `data/`: реализации repository, интеграция с `TrafficEngine`, legacy list import/export, Android diagnostics checks.
- `presentation/`: MVVM/MVI-контракт и Quick Start экран.

## Structured diagnostics and status

Диагностика представляется как `DiagnosticReport` + `DiagnosticCheckResult`:

- `id`, `severity`, `status`, `details`, `remediation`.

Статус сервиса представляется как `ServiceStatus` (running/profile/uptime/bytes), без текстового потока.

## Quick Start and remediation

Quick Start показывает человекочитаемый summary и JSON-путь отчёта.
Для исправляемых проблем доступны one-tap remediation deeplink actions в системные настройки.

## Legacy compatibility

`LegacyListsRepository` сохраняет обратную совместимость с текущими `lists/*.txt`:

- export: копирование `.txt` из `lists/` в backup;
- import: восстановление `.txt` из backup обратно в `lists/`.
