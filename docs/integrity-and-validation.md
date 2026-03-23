# Проверка целостности, валидация списков и Android diagnostics report

## 1) Проверка целостности бинарей

- Манифест: `bin/checksums.sha256`.
- Скрипт: `scripts/verify-integrity.ps1`.

Пример запуска:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-integrity.ps1
```

Скрипт проверяет SHA256 для файлов, перечисленных в манифесте, и завершает работу с кодом `1` при несовпадении.

## 2) Валидация списков

- Скрипт: `scripts/validate-lists.ps1`.
- Проверяются:
  - формат доменов в `list-*.txt`,
  - формат IPv4/CIDR в `ipset-*.txt`,
  - дубликаты внутри каждого файла,
  - отсутствие обязательных файлов.

Примеры запуска:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\validate-lists.ps1
```

Строгий режим (ошибка при обнаружении проблем):

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\validate-lists.ps1 -Strict
```

## 3) Android diagnostics report format

Android-диагностика использует единый JSON-формат `integrity-validation-report/v1` и сохраняется в app-specific storage:

- Путь: `<app files dir>/diagnostics/report-<timestamp>.json`
- Корневые поля:
  - `schema` — строка формата (`integrity-validation-report/v1`)
  - `generatedAt` — ISO-8601 timestamp
  - `checks` — массив проверок

Структура элемента `checks[]`:

- `id` — machine-readable идентификатор проверки
- `severity` — `INFO | WARNING | ERROR`
- `status` — `PASS | WARN | FAIL | SKIPPED`
- `details` — человекочитаемое описание
- `remediation` — `null` или объект:
  - `label` — текст действия
  - `deepLink` — deeplink для one-tap remediation (настройки/экран приложения)

Обязательные Android checks:

1. `vpn_permission`
2. `battery_optimization`
3. `private_dns`
4. `network_reachability`
5. `profile_validity`
