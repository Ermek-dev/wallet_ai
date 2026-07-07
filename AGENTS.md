# AGENTS.md — соглашения проекта WalletAI

Единый свод правил для людей и ИИ-агентов, работающих с репозиторием.

## Язык

- Все commit-сообщения, PR-описания и markdown-документация — **на русском**.
- Код, имена переменных и функций, комментарии в коде — **на английском**.
- Тексты интерфейса — на русском (позже через `strings.xml` с локализацией ru/kk/en).

## Стиль коммитов

Conventional Commits на русском: `<тип>(<область>): <описание>`.

**Типы:** `feat`, `fix`, `refactor`, `style`, `docs`, `chore`, `perf`, `test`.

**Области (scopes):**

| Scope | Что покрывает |
|---|---|
| `ui` | Compose-экраны, компоненты, тема, навигация |
| `db` | Room: entities, DAO, миграции |
| `data` | Репозитории, DataStore, мапперы |
| `domain` | Модели и интерфейсы в `shared/domain` |
| `ml` | Классификатор, парсер выписки (`shared/ml`) |
| `voice` | Голосовой ввод (`VoiceInputParser`, `VoiceRecognizerHelper`) |
| `ai` | GeminiService, промпты, советник |
| `analytics` | Экран аналитики, графики |
| `profile` | Профиль, настройки, DataStore-настройки |
| `export` | CSV/PDF-экспорт, ShareHelper |
| `notify` | Уведомления, WorkManager |
| `shared` | Общее по KMP-модулю (сборка, структура) |
| `ios` | iOS-таргет |
| `build` | Gradle, зависимости, версии |
| `config` | Манифест, ProGuard, ресурсы конфигурации |
| `docs` | Документация |
| `repo` | Служебное: .gitignore, настройки агентов, CI |

**Правила:**
- Атомарность: один файл — один коммит, если правки независимы.
- Исключение: связанные правки (новый класс + его регистрация в DI/навигации) — одним коммитом.
- Описание в императиве: «добавить», «исправить», «переписать» — без точки в конце.

**Примеры:**
```
feat(voice): добавить VoiceInputParser для разбора голосовых фраз
fix(export): открывать CSV через FileProvider вместо прямого пути
refactor(data): вынести маппинг entity в EntityMappers
docs(plan): добавить фазу облачной синхронизации
chore(build): удалить неиспользуемые зависимости retrofit и moshi
test(ml): покрыть классификатор кейсами с пустым описанием
```

## Gitflow

| Ветка | Назначение |
|---|---|
| `main` | Только релизы, защищена |
| `develop` | Основная ветка разработки |
| `feature/<имя>` | Фича от `develop`, merge через PR в `develop` |
| `release/<v>` | Подготовка релиза (по необходимости) |
| `hotfix/<v>` | Срочные исправления в `main` (по необходимости) |

Релиз: PR `develop` → `main`, затем тег `v*.*.*` на `main`.

> Текущее состояние: репозиторий на ветке `master`. Переход на схему выше — вручную:
> `git branch -m master main && git checkout -b develop`. Remote и push — только вручную на машине владельца.

## Запреты

- Коммит/push без явного апрува владельца. Force-push куда угодно.
- `git reset --hard`, `git clean` — история и рабочая копия неприкосновенны.
- Чтение/вывод секретов: `.env`, `local.properties`, `*.jks`, пароли keystore.
- `Double` для денежных сумм (только `Money` / `Long` в минорных единицах).
- Изменение схемы Room без `Migration`; `fallbackToDestructiveMigration`.
- Android-зависимости (`Context`, `Log`, `android.*`) в `shared/commonMain`.
- `!!` в Kotlin — использовать `?.let`, `requireNotNull`, `checkNotNull`.
- `Any` / нетипизированные public API.
- Новые библиотеки без обсуждения (каждая зависимость = размер APK + поверхность атаки).
- Логирование финансовых данных пользователя и тел запросов к Gemini в release.

## Стандарты Kotlin

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html); `val` вместо `var` везде, где возможно.
- Состояния экранов — `sealed interface` (`Loading` / `Success` / `Error`), как `AdvisorUiState`.
- Именование: классы и Composable — `PascalCase`; функции/переменные — `camelCase`; пакеты — строчными `kz.abdiyev.walletai.*`.
- ViewModel: один на экран; наружу — только `StateFlow`/иммутабельные модели; доступ к данным — через интерфейсы репозиториев из `domain/repository`, инжект через Koin.
- Room-запросы — только в DAO; DAO — только в репозиториях; UI не знает про Room.
- Composable: параметры `modifier: Modifier = Modifier` последним из обязательных; без бизнес-логики в composition (вычисления — во ViewModel/`remember`).

## Процесс работы над задачей

1. Ветка `feature/<имя>` от `develop`.
2. Для логики — сначала тест (`shared/src/commonTest` или `app/src/test`), потом код.
3. `./gradlew :shared:testDebugUnitTest :app:testDebugUnitTest` — зелёные.
4. Самопроверка по чек-листу `/review-pr` (скилл в `.claude/skills/review-pr`).
5. Показать diff владельцу → апрув → коммит по соглашениям выше.
6. Push и PR — вручную владельцем.

## Документация

- `PLAN.md` — дорожная карта (фазы). Обновлять при изменении планов.
- `docs/ARCHITECTURE.md` — слои, модули, потоки данных. Обновлять при структурных изменениях.
- `LEARNING.md` — учебные заметки владельца (агентам не редактировать без просьбы).
