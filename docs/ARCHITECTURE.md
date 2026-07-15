# Архитектура WalletAI

> Живой документ: обновлять при структурных изменениях (см. AGENTS.md → Документация).

## Обзор

WalletAI — офлайн-first приложение учёта личных финансов с локальным ML и ИИ-советником.
Единственный источник правды — локальная БД **Room (SQLite на устройстве)**. Сервера и аккаунта нет;
сеть используется для запроса советов к **Groq API** (основной провайдер) и опционально Gemini (fallback).
На сервер уходит агрегированная сводка, не сырые транзакции.

## Модули

```
walletai/
├── app/        Android-приложение (Compose UI, ViewModels, Room, Koin, сервисы)
└── shared/     Kotlin Multiplatform: чистая бизнес-логика без Android-классов
    ├── commonMain/   domain-модели, ML, AI, утилиты  ← сюда кладём всю новую логику
    ├── commonTest/   unit-тесты общей логики
    └── iosMain/      задел под iOS (пока placeholder)
```

## Слои (внутри app/) и поток данных

```
UI (Compose-экраны)  ──собирает StateFlow──  ViewModel (по одному на экран)
        │                                          │ через интерфейсы
        │                                          ▼
        │                              domain/repository (интерфейсы)
        │                                          │ Koin DI (di/*Module.kt)
        │                                          ▼
        │                              data/repository (реализации)
        │                               │        │         │
        │                               ▼        ▼         ▼
        │                             Room   DataStore   AdviceAiClient (Ktor)
        └── события пользователя ──────────────────────────────────────►
```

- **UI** (`ui/screens`, `ui/components`): основные табы — Учёт (Dashboard), Аналитика, ИИ (Advisor), Профиль; отдельные маршруты — История, Импорт (`WalletAiRoutes` + Navigation Compose в `MainActivity`).
- **ViewModel** (`ui/viewmodel`): `DashboardViewModel`, `AnalyticsViewModel` (`AnalyticsUiState`), `AdvisorViewModel`, `ProfileViewModel`, `ImportViewModel` (`ImportUiState`), `HistoryViewModel`. Наружу — `StateFlow`; внутрь — интерфейсы репозиториев.
- **domain/repository** (интерфейсы): `TransactionRepository`, `CategoryRepository`, `SettingsRepository`, `AdviceRepository`, `MerchantRuleRepository`, `MaintenanceRepository`.
- **data**: `database/` (Room), `preferences/SettingsDataStore` + `AdviceCacheDataStore`, `repository/`, `mapper/EntityMappers`.
- **DI**: Koin — модульная сборка `databaseModule`, `repositoryModule`, `aiModule`, `viewModelModule`; старт в `WalletAiApplication` + `ReminderBootstrap` при запуске.

## Общая логика (shared/commonMain)

| Пакет | Содержимое |
|---|---|
| `domain/model` | `Money`, `Currency`, `TransactionType`, `Period`, `Settings`, `TransactionData`, `CategoryData` |
| `ml` | `TransactionClassifier`, `BankStatementParser`, `VoiceInputParser`, парсеры Kaspi/Freedom |
| `ai` | `AdviceAiClient`, `GroqClient`, `AiErrorMapper`, `AdviceCachePolicy` |
| `util` | `MoneyFormatter`, `PeriodFilter`, `CalendarMillis`, `DescriptionNormalizer`, `ReminderDelayCalculator` |
| `ui` | `TiltMath`, `TiltOffset` — математика наклона для UI-эффектов (без Android) |

Правило: код здесь не знает про Android (`Context`, `Log`, `android.*` запрещены).

## Ключевые потоки

**Учёт и периоды:** `PeriodFilter` + `CalendarMillis` (календарная неделя/месяц/3 месяца) → `DashboardUiState.filteredTransactions`; на главном экране превью последних 7 операций.

**Аналитика:** `AnalyticsViewModel` агрегирует `AnalyticsUiState` (донат, top-8 категорий + «Прочее»); экспорт CSV/PDF — только за выбранный период.

**Голосовой ввод:** микрофон → `VoiceRecognizerHelper` (системный SpeechRecognizer; **может использовать облако Google** — disclosure в UI) → `VoiceInputParser` → merchant rules → классификатор → предпросмотр → Room. При нулевой сумме — событие и ручное редактирование.

**Импорт выписки:** SAF/PDFBox (`PdfTextExtractor`, off-main) → `BankStatementParser` → `ImportUiState` (Idle/Parsing/Preview/Saving/Error) → предпросмотр с редактированием → дедупликация через `DescriptionNormalizer` → Room.

**ИИ-совет:** AdvisorScreen → `AdvisorViewModel` → `AdviceRepository` → `GroqClient` (Ktor) → кэш memory + DataStore (`AdviceCachePolicy`, TTL 24 ч) → при ошибке — локальный fallback.

**Экспорт:** `Exporter` → `ShareHelper` (FileProvider).

**Уведомления:** `ReminderScheduler` + `ReminderWorker` (WorkManager, приблизительное время без exact alarm); `notificationHour` + `notificationMinute` в Settings; перепланирование при старте (`ReminderBootstrap`).

**Динамическое свечение (tilt glow):** `TiltSensorMonitor` (Android, `SensorManager`) → `TiltMath` в shared → `StateFlow<TiltOffset>` → `LocalTiltOffset` + `Modifier.tiltGlow` на карточках/FAB/NavigationBar; тумблер `Settings.dynamicEffects`, отключение при `ANIMATOR_DURATION_SCALE == 0`.

## Данные

- БД: Room, миграции — только через `DatabaseMigrations.kt`.
- Деньги: `Long` в минорных единицах внутри `Money`.
- Настройки: DataStore (`Settings`, `notificationMinute` — новые ключи с defaults, Room-миграция не нужна).
- Секреты: `GROQ_API_KEY` / `GEMINI_API_KEY` из `.env` → `BuildConfig`; `.env` вне git.

## Решения и их причины (ADR-кратко)

| Решение | Почему |
|---|---|
| Room, а не Supabase/сервер | Финансы — приватные данные; офлайн-first. |
| Koin | KMP-совместимый DI. |
| Naive Bayes свой | Обучается на устройстве, ноль тяжёлых ML-зависимостей. |
| Groq + кэш | Стабильнее квоты, чем прямой Gemini при разработке. |
| WorkManager для напоминаний | Без `SCHEDULE_EXACT_ALARM`; Android может доставить чуть позже. |
| SpeechRecognizer | Бесплатно из коробки; пользователю показываем, что возможна облачная обработка Google. |
