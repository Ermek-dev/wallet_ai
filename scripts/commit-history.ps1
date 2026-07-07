# WalletAI — атомарные коммиты по AGENTS.md
# Запускать из корня репозитория после git init.
# Push и remote — только вручную владельцем.

$ErrorActionPreference = "Stop"

if (-not (Test-Path ".git")) {
    git init
    git branch -m main
}

function Commit-Group($message, $paths) {
    git add @paths
    git status --short
    git commit -m $message
}

# 1. Базовый каркас
Commit-Group "chore(repo): базовый gitignore, gradle wrapper и настройки проекта" @(
    ".gitignore", "gradle/", "gradlew", "gradlew.bat",
    "gradle.properties", "settings.gradle.kts", "build.gradle.kts"
)

# 2. KMP shared
Commit-Group "build(shared): добавить KMP-модуль shared и версии зависимостей" @(
    "shared/", "gradle/libs.versions.toml"
)

# 3. Domain + data
Commit-Group "feat(domain): модели Money, Transaction, репозитории и мапперы" @(
    "app/src/main/java/kz/abdiyev/walletai/domain/",
    "app/src/main/java/kz/abdiyev/walletai/data/"
)

# 4. ML + AI + services
Commit-Group "feat(ml): классификатор, парсеры, GeminiService и утилиты" @(
    "app/src/main/java/kz/abdiyev/walletai/service/",
    "app/src/main/java/kz/abdiyev/walletai/util/",
    "app/src/main/java/kz/abdiyev/walletai/notification/"
)

# 5. UI
Commit-Group "feat(ui): экраны Dashboard, Analytics, Advisor, Profile и навигация" @(
    "app/src/main/java/kz/abdiyev/walletai/ui/",
    "app/src/main/java/kz/abdiyev/walletai/MainActivity.kt",
    "app/src/main/java/kz/abdiyev/walletai/WalletAiApplication.kt",
    "app/src/main/java/kz/abdiyev/walletai/di/",
    "app/src/main/res/",
    "app/src/main/AndroidManifest.xml",
    "app/build.gradle.kts"
)

# 6. Тесты
Commit-Group "test(shared): unit-тесты Money, PeriodFilter, парсеров и классификатора" @(
    "shared/src/commonTest/"
)
Commit-Group "test(app): unit-тесты мапперов, ViewModel и instrumented Room/Compose" @(
    "app/src/test/", "app/src/androidTest/"
)

# 7. Документация и workflow
Commit-Group "docs: PLAN, AGENTS, CLAUDE, ARCHITECTURE, LEARNING и workflow Claude Code" @(
    "PLAN.md", "AGENTS.md", "CLAUDE.md", "LEARNING.md", "docs/", ".claude/", "scripts/"
)

Write-Host ""
Write-Host "Готово. Дальше вручную:"
Write-Host "  git remote add origin <url>"
Write-Host "  git push -u origin main"
Write-Host "  git checkout -b develop"
