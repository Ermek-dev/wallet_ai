# WalletAI — правила для Claude Code

Приложение учёта финансов: Kotlin, Jetpack Compose, Room, Koin, KMP-модуль `shared/`, Gemini API.
Полные соглашения (коммиты, gitflow, стиль кода) — в **AGENTS.md**. Архитектура — в **docs/ARCHITECTURE.md**. Дорожная карта — в **PLAN.md**.

## Жёсткие правила

1. **Не коммитить без явного апрува пользователя.** Показать diff и предложенное сообщение коммита, дождаться подтверждения.
2. **`git push`, создание remote, теги релизов — только пользователь вручную.** Никогда не пушить самостоятельно.
3. **Сначала тесты.** Для новой логики в `shared/` (парсеры, классификатор, форматтеры, фильтры) — сначала тест в `shared/src/commonTest/`, потом реализация. После любых правок логики: `./gradlew :shared:testDebugUnitTest :app:testDebugUnitTest`.
4. **Секреты неприкосновенны:** не читать и не логировать `.env`, `local.properties`, `*.jks`. Не хардкодить ключи в код — только `BuildConfig` из `.env`.
5. **Не ломать данные пользователя:** любое изменение схемы Room — только через `Migration` в `DatabaseMigrations.kt` + bump версии. `fallbackToDestructiveMigration` запрещён.
6. **Деньги — только `Long` (минорные единицы) через `Money`.** Никаких `Double` для сумм.
7. **Бизнес-логика — в `shared/commonMain`, чистый Kotlin.** Никаких Android-классов (`Context`, `Log`, `android.*`) в `shared/`.
8. **Язык:** код, имена, комментарии в коде — английский; коммиты, PR, markdown-документация, UI-тексты — русский.

## Команды

```
./gradlew :app:assembleDebug            # сборка debug APK
./gradlew :shared:testDebugUnitTest     # тесты общей логики (KMP)
./gradlew :app:testDebugUnitTest        # тесты Android-модуля
./gradlew lint                          # статический анализ
```

## Структура

- `app/` — Android: UI (Compose), ViewModels, Room, DI (Koin), сервисы
- `shared/` — KMP: domain-модели, ML (классификатор, парсеры), утилиты
- `test/` — временная папка со скиллами из другого проекта (не часть кода, не собирается)
