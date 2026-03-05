# Проверка целостности и валидация списков

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
