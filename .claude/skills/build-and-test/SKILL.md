---
name: build-and-test
description: Сборка и тестирование WalletAI — правильные gradle-таски для app и shared (KMP), запуск в эмуляторе, типовые проблемы сборки. Использовать при любой сборке, прогоне тестов или ошибке gradle.
---

# Сборка и тесты WalletAI

## Основные команды (из корня проекта)

```bash
./gradlew :app:assembleDebug             # debug APK -> app/build/outputs/apk/debug/
./gradlew :shared:testAndroidHostTest      # тесты KMP-логики (commonTest) — самые важные
./gradlew :app:testDebugUnitTest         # unit-тесты Android-модуля
./gradlew lint                           # статический анализ
./gradlew :app:installDebug              # установка на запущенный эмулятор/устройство
```

Запуск на эмуляторе после installDebug:
```bash
adb shell am start -n kz.abdiyev.walletai/.MainActivity
adb logcat -s WalletAI GeminiService DashboardViewModel AndroidRuntime
```

## Порядок при изменении логики

1. Логика живёт в `shared/src/commonMain` — тест пишется в `shared/src/commonTest/kotlin/.../SharedLogicTest.kt` (или новый файл рядом).
2. Тест первым, красный → реализация → `./gradlew :shared:testAndroidHostTest` зелёный.
3. Затем полная сборка `:app:assembleDebug` — убедиться, что Android-модуль не сломан.

## Типовые проблемы

| Симптом | Причина / решение |
|---|---|
| `GEMINI_API_KEY not found` / пустой ключ | Нет файла `.env` в корне. Создать по образцу `.env.example`. Никогда не читать содержимое `.env`. |
| Room: `Cannot verify the data integrity` / краш после изменения Entity | Изменена схема без миграции. Добавить `Migration` в `DatabaseMigrations.kt`, поднять `version` в `AppDatabase`. |
| KSP-ошибки Room после правки Entity/DAO | Сначала читать сообщение KSP полностью — обычно указан точный метод DAO. |
| `SDK location not found` | Отсутствует `local.properties` (генерируется Android Studio). Не создавать вручную с чужими путями. |
| Долгая первая сборка | Норма: gradle качает зависимости. Не прерывать, не менять версии «на удачу». |
| Тесты `commonTest` не видят новый класс | Класс добавлен в `androidMain`/`app` вместо `commonMain` — логика без Android-зависимостей должна лежать в `commonMain`. |

## Что НЕ делать

- Не менять версии AGP/Kotlin/зависимостей ради «починки» без анализа ошибки.
- Не добавлять `fallbackToDestructiveMigration` — это стирает данные пользователя.
- Не отключать lint/тесты, чтобы «прошла сборка».
