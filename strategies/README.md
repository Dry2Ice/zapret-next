# Strategy Specs

`strategies/` хранит декларативные профили вместо hardcode в BAT-файлах.

## Формат

Каждый spec включает:

- `specVersion` — семантическая версия формата (`major.minor.patch`).
- `id` — стабильный идентификатор стратегии.
- `priority` — приоритет выбора.
- `filters` — stage-based фильтры (`--filter-*`, host/ipset).
- `desyncParams` — параметры DPI desync.
- `requiredAssets` — бинарные/листовые зависимости.

Схема: `strategies/strategy.schema.json`.

## Инструменты

- Конвертер BAT → Spec: `python3 scripts/convert_bat_to_strategy.py`
- Валидатор: `python3 scripts/validate_strategies.py`
- Mapping BAT→Spec: `strategies/bat-to-spec-map.json`

## Адаптеры

- `WindowsAdapter` — обратная совместимость с legacy winws CLI.
- `AndroidAdapter` — подготовка payload для Android `TrafficEngine`.
