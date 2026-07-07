# WalletAI — установка APK и git-коммиты

## Установка на телефон

```powershell
$env:JAVA_HOME = "C:\Users\abdie\AppData\Local\Programs\Android Studio\jbr"
cd d:\pet_projects\listok-ai
.\gradlew.bat :app:assembleDebug
```

APK: `app\build\outputs\apk\debug\app-debug.apk`

**Вариант A — USB (нужен adb в PATH):**
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

**Вариант B — вручную:** скопировать APK на телефон → открыть → разрешить установку из неизвестных источников.

**Gemini (ИИ):** добавить ключ в `.env`, пересобрать APK.

## Тесты перед коммитом

```powershell
$env:JAVA_HOME = "C:\Users\abdie\AppData\Local\Programs\Android Studio\jbr"
.\gradlew.bat :shared:testAndroidHostTest :app:testDebugUnitTest
```

## Git: чистая история с нуля

> Push и remote — только вручную владельцем (AGENTS.md).

```powershell
cd d:\pet_projects\listok-ai
Remove-Item -Recurse -Force .git   # удалит текущую историю!
git init
git branch -m main
.\scripts\commit-history.ps1       # атомарные коммиты по AGENTS.md
git remote add origin <url>
git push -u origin main
git checkout -b develop
```

После `commit-history.ps1` проверь `git log --oneline` и diff перед push.
