# Архитектура WalletAI

> Живой документ: обновлять при структурных изменениях (см. AGENTS.md → Документация).

## Обзор

WalletAI — офлайн-first приложение учёта личных финансов с локальным ML и ИИ-советником.
Единственный источник правды — локальная БД **Room (SQLite на устройстве)**. Сервера и аккаунта нет;
сеть используется только для запроса советов к Gemini API (уходит агрегированная сводка, не сырые данные).

## Модули

```
walletai/
├── app/        Android-приложение (Compose UI, ViewModels, Room, Koin, сервисы)
└── shared/     Kotlin Multiplatform: чистая бизнес-логика без Android-классов
    ├── commonMain/   domain-модели, ML, утилиты  ← сюда кладём всю новую логику
    ├── commonTest/   unit-тесты общей логики
    └── iosMain/      задел под iOS (пока placeholder)
```

## Слои (внутри app/) и поток данных

```
UI (Compose-экраны)  ──собирает StateFlow──  ViewModel (по одному на экран)
        │                                          │ через интерфейсы
        │                                          ▼
        │                              domain/repository (интерфейсы)
        │                                          │ Koin DI (di/AppModule.kt)
        │                                          ▼
        │                              data/repository (реализации)
        │                               │        │         │
        │                               ▼        ▼         ▼
        │                             Room   DataStore   GeminiService (OkHttp)
        └── события пользователя ──────────────────────────────────────►
```

- **UI** (`ui/screens`, `ui/components`): 4 экрана — Dashboard (учёт), Analytics, Advisor (ИИ), Profile. Навигация — табы в `MainActivity`.
- **ViewModel** (`ui/viewmodel`): `DashboardViewModel`, `AnalyticsViewModel`, `AdvisorViewModel`, `ProfileViewModel`. Наружу — только `StateFlow`; внутрь — только интерфейсы репозиториев.
- **domain/repository** (интерфейсы): `TransactionRepository`, `CategoryRepository`, `SettingsRepository`, `AdviceRepository`, `MerchantRuleRepository`.
- **data**: `database/` (Room: Entities, Daos, `DatabaseMigrations`), `preferences/SettingsDataStore` (валюта, имя, настройки), `repository/` (реализации), `mapper/EntityMappers` (Entity ↔ domain-модель).
- **DI**: Koin, единый модуль `di/AppModule.kt`, старт в `WalletAiApplication`.

## Общая логика (shared/commonMain)

| Пакет | Содержимое |
|---|---|
| `domain/model` | `Money` (Long, минорные единицы), `Currency`, `TransactionType`, `Period`, `TransactionData`, `CategoryData` |
| `ml` | `TransactionClassifier` (Naive Bayes), `BankStatementParser`, `VoiceInputParser` |
| `util` | `MoneyFormatter`, `PeriodFilter` |

Правило: код здесь не знает про Android (`Context`, `Log`, `android.*` запрещены) — это гарантия переносимости на iOS.

## Ключевые потоки

**Добавление транзакции (вручную):** DashboardScreen → диалог `TransactionDialog` → `DashboardViewModel.addTransaction` → `TransactionRepository` → Room DAO → Flow автоматически обновляет список на экране.

**Голосовой ввод:** микрофон → `VoiceRecognizerHelper` (системный SpeechRecognizer, on-device) → текст → `VoiceInputParser` (сумма/тип/описание) → `MerchantRuleRepository` (точные правила «мерчант → категория» от исправлений пользователя) → если правила нет — `TransactionClassifier` → предпросмотр → подтверждение → Room.

**ИИ-совет:** AdvisorScreen → `AdvisorViewModel` → `AdviceRepository` → `GeminiService` (агрегированная сводка → Gemini API) → при ошибке/нет ключа — локальный fallback-отчёт.

**Экспорт:** `Exporter` (CSV с BOM / PDF через Canvas) → файл в app-specific storage → `ShareHelper` (FileProvider + Share Intent).

**Уведомления:** `ReminderScheduler` + `ReminderWorker` (WorkManager) — локальные напоминания, без push-серверов.

## Данные

- БД: Room, миграции — только через `DatabaseMigrations.kt` (destructive-миграции запрещены).
- Деньги: `Long` в минорных единицах (тиын/копейки) внутри `Money`; форматирование — `MoneyFormatter`.
- Настройки: Jetpack DataStore (валюта по умолчанию KZT ₸ и др.).
- Секреты: `GEMINI_API_KEY` из `.env` через Secrets Gradle Plugin → `BuildConfig`; `.env` вне git.

## Решения и их причины (ADR-кратко)

| Решение | Почему |
|---|---|
| Room, а не Supabase/сервер | Финансы — приватные данные; офлайн-first быстрее и безопаснее; нет затрат на инфраструктуру. Supabase — опция для будущей синхронизации (PLAN.md, Фаза 9), не замена локальной БД. |
| Koin, а не Hilt | Проще для входа, работает в KMP (iOS). |
| Naive Bayes свой, а не TFLite | Крошечный, обучается на устройстве на данных пользователя, ноль зависимостей, переносим в commonMain. |
| SpeechRecognizer, а не облачный STT | Бесплатно, on-device, аудио не покидает устройство. |
| Compose Multiplatform как путь на iOS | UI уже на Compose — перенос дешевле, чем переписывание на SwiftUI. |
