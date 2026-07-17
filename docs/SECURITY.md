# Безопасность WalletAI

Документ фиксирует принятые решения по защите финансовых данных на устройстве.

**Последнее обновление:** 2026-07-16

## Облачный бэкап Android

- `android:allowBackup="true"` — пользователь может переносить приложение, но финансовые данные **исключены** из облачного бэкапа и device transfer.
- Исключено: Room БД (`walletai_database*`), DataStore настроек (`walletai_settings`), кэш советника (`walletai_advice_cache`).
- Файлы: `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`.

## Шифрование БД (SQLCipher)

**Решение: не использовать SQLCipher в текущей версии.**

Обоснование:

- Приложение личное, данные не покидают устройство (кроме агрегатов в Gemini/Groq по явному действию пользователя).
- Android Full-Disk Encryption + sandbox приложения достаточны для угрозы «украли телефон с разблокированным экраном» — здесь помогает биометрия (опционально в настройках), а не SQLCipher.
- SQLCipher усложняет миграции Room, увеличивает APK и требует надёжного хранения ключа в Keystore.

Пересмотреть при публикации в Play для широкой аудитории или при добавлении облачной синхронизации (Фаза 9).

## Логирование

- `Timber.plant(DebugTree())` — только в `BuildConfig.DEBUG`.
- В release дерево не сажается; финансовые суммы и тела API-запросов не логируются.

## API-ключи

- Ключи Gemini/Groq — через `BuildConfig` из `.env` / `local.properties` (не в git).
- Для публичного Play: Firebase AI Logic или серверный прокси (см. часть D в `PLAN.md`).

## Сеть

- `android:usesCleartextTraffic="false"` — только HTTPS.
- Таймауты Ktor — единая конфигурация в `shared/.../AdviceHttpClientConfig.kt`.

## Экспорт и компоненты

- `FileProvider` — `android:exported="false"`, grant-uri только для share-intent.
- Перед релизом: прогон `security-auditor` агентом по экспортируемым компонентам.
