# WalletAI

Личное Android-приложение учёта доходов и расходов: Kotlin, Jetpack Compose, Room, KMP-модуль `shared/`, Gemini API.

## Возможности

- Учёт операций, категории, undo удаления
- Голосовой ввод и импорт выписки (текст / PDF)
- Аналитика расходов и доходов, экспорт CSV/PDF
- ИИ-советник (Gemini) с офлайн-fallback
- Профиль: валюта, тема, уведомления

## Требования

- Android Studio (Koala+)
- JDK 11+
- Файл `.env` в корне проекта (см. `.env.example`)

```env
GEMINI_API_KEY=ваш_ключ
```

## Сборка и тесты

```bash
./gradlew :app:assembleDebug
./gradlew :shared:testAndroidHostTest :app:testDebugUnitTest
./gradlew :app:installDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Структура

```
app/       — Android UI, Room, DI (Koin)
shared/    — KMP: domain, ML, Gemini (Ktor), unit-тесты
docs/      — ARCHITECTURE.md, PLAN.md
```

## Документация

- [PLAN.md](PLAN.md) — дорожная карта
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — слои и потоки данных
- [AGENTS.md](AGENTS.md) — соглашения для разработки

## Gitflow

Ветка `develop` → feature-ветки → PR. Push и remote — вручную владельцем репозитория.
