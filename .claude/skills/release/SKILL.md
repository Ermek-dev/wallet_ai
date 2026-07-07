---
name: release
description: Процесс подготовки релиза WalletAI — версия, changelog, release-сборка, подпись, тег. Использовать при словах «релиз», «release», «выпустить версию», «подписать APK».
---

# Релиз WalletAI

Аналог «деплоя» для Android-приложения. Публикация в Google Play — отдельный ручной шаг владельца.

## Чек-лист релиза

1. **Ветка**: `release/vX.Y.Z` от `develop` (или прямо `develop` → `main`, пока проект маленький).
2. **Версия** в `app/build.gradle.kts`:
   - `versionCode` +1 (всегда, монотонно — Play не примет повтор);
   - `versionName` = `X.Y.Z` (semver: фичи → minor, фиксы → patch).
3. **Тесты и линт**: `./gradlew :shared:testDebugUnitTest :app:testDebugUnitTest lint` — всё зелёное.
4. **CHANGELOG.md**: раздел `## vX.Y.Z — YYYY-MM-DD`, пункты на русском из коммитов `feat`/`fix` с прошлого релиза (`git log v<prev>..HEAD --oneline`).
5. **Release-сборка**: `./gradlew :app:assembleRelease`.
   - Подпись берёт keystore и пароли из переменных окружения `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` — они настраиваются владельцем, агент их не читает и не выводит.
   - Если включён R8 (`isMinifyEnabled = true`) — сохранить `app/build/outputs/mapping/release/mapping.txt` (расшифровка крашей в Play Console).
6. **Прогон на устройстве**: установить release-APK, проверить смок-сценарии: добавить расход → увидеть в списке и аналитике → экспорт CSV → ИИ-совет (или fallback без ключа).
7. **PR `develop` → `main`** — создаёт и мержит владелец.
8. **Тег**: `git tag vX.Y.Z` на `main` — только владелец вручную, как и push.

## Границы для агента

- Агент готовит: версию, changelog, сборку, смок-чек-лист.
- Агент НЕ делает: push, теги, загрузку в Play Console, операции с keystore.

## Перед первым публичным релизом (однократно)

- Ограничить Gemini API-ключ в Google Cloud Console (package name + SHA-1 + квоты) или перейти на Firebase AI Logic.
- Включить R8: `isMinifyEnabled = true`, `isShrinkResources = true` (см. скилл android-r8, если добавлен).
- Проверить `allowBackup` / `backup_rules.xml` — финансовая БД не должна утекать в открытый бэкап.
- Убедиться, что `applicationId` финальный (`kz.abdiyev.walletai`) — после публикации его не сменить.
