# WalletAI

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![KMP](https://img.shields.io/badge/Kotlin%20Multiplatform-blueviolet)

Личное Android-приложение учёта доходов и расходов: Kotlin, Jetpack Compose, Room, KMP-модуль `shared/`, Groq API (советник) с кэшем и fallback на Gemini.

## Скриншоты

### Учёт и операции

<table>
  <tr>
    <td align="center"><img src="docs/screenshots/dashboard.png" width="220"/><br/>Главный экран</td>
    <td align="center"><img src="docs/screenshots/edit.png" width="220"/><br/>Добавление операции</td>
    <td align="center"><img src="docs/screenshots/import.png" width="220"/><br/>Импорт выписки</td>
  </tr>
</table>

### Аналитика

<table>
  <tr>
    <td align="center"><img src="docs/screenshots/analytics-expense.png" width="220"/><br/>Расходы</td>
    <td align="center"><img src="docs/screenshots/analytics-income.png" width="220"/><br/>Доходы</td>
  </tr>
</table>

### ИИ-советник и профиль

<table>
  <tr>
    <td align="center"><img src="docs/screenshots/advisor.png" width="220"/><br/>ИИ-советник</td>
    <td align="center"><img src="docs/screenshots/profile.png" width="220"/><br/>Профиль и настройки</td>
  </tr>
</table>

<!-- Когда доснимешь тёмную тему:
### Светлая и тёмная тема
<table><tr>
  <td align="center"><img src="docs/screenshots/dashboard-light.png" width="220"/><br/>Светлая</td>
  <td align="center"><img src="docs/screenshots/dashboard-dark.png" width="220"/><br/>Тёмная</td>
</tr></table>
-->

## Возможности

- Учёт операций, категории, undo удаления, семантика доход/расход
- Голосовой ввод (системный SpeechRecognizer) и импорт выписки (текст / PDF)
- Аналитика: донат-диаграмма, рейтинг категорий, календарные периоды, экспорт CSV/PDF
- ИИ-советник (Groq) с кэшем 24 ч и офлайн-fallback
- Профиль: валюта, тема, настраиваемое вечернее напоминание (WorkManager)

## Требования

- Android Studio (Koala+)
- JDK 11+
- Файл `.env` в корне проекта (см. `.env.example`)

```env
GROQ_API_KEY=ваш_ключ_groq
GEMINI_API_KEY=опционально_для_fallback
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
app/       — Android UI, Room, DI (Koin), WorkManager, PDFBox
shared/    — KMP: domain, ML, AI (Ktor/Groq), unit-тесты
docs/      — ARCHITECTURE.md, PLAN.md
```

## Документация

- [PLAN.md](PLAN.md) — дорожная карта
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — слои и потоки данных
- [AGENTS.md](AGENTS.md) — соглашения для разработки

## Gitflow

Ветка `develop` → feature-ветки → PR. Push и remote — вручную владельцем репозитория.
