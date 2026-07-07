# WalletAI — обучающий трек

Отдельный документ для теории. Проходи тему **перед** соответствующей фазой в [PLAN.md](PLAN.md), затем сразу закрепляй на коде.

**Темп:** 2–5 часов в неделю. Не гонись за скоростью — важнее понять и протестировать каждую фазу.

---

## L1. Инструменты и анатомия проекта → перед Фазой 0

**Цель:** уметь открыть проект, собрать APK и найти главные файлы.

### Что изучить
- **Android Studio**: Project view, Logcat, Run (▶), эмулятор vs реальный телефон (режим разработчика + USB-отладка).
- **Gradle**: `build.gradle.kts` (модуля `app`) — зависимости; `settings.gradle.kts` — список модулей.
- **Структура `app/src/main/`**:
  - `AndroidManifest.xml` — точка входа, разрешения, FileProvider.
  - `java/.../` — Kotlin-код.
  - `res/values/strings.xml` — строки UI.
- **Git**: `git status`, `git commit` — точка отката перед каждой фазой.

### Практика (Фаза 0)
1. Открыть проект в Android Studio.
2. Запустить на эмуляторе или телефоне.
3. Найти: `MainActivity.kt`, `FinanceViewModel` / ViewModels, `AppDatabase.kt`.
4. `git init` + первый коммит.

### Ресурсы
- [Developer Android — первое приложение](https://developer.android.com/codelabs/basic-android-kotlin-compose-first-program)
- [Gradle для Android (обзор)](https://developer.android.com/build)

---

## L2. Kotlin за выходные → перед Фазой 1

**Цель:** читать и писать простой Kotlin в проекте.

### Темы
| Тема | Зачем в WalletAI |
|------|------------------|
| `val` / `var` | состояние UI и модели |
| `data class` | `TransactionEntity`, `Money` |
| `enum class` | `TransactionType`, `Period` |
| Null-safety (`?`, `?.`, `?:`) | категория может отсутствовать |
| Функции и лямбды | `onClick = { }` в Compose |
| `when` | выбор таба, тип транзакции |
| `List`, `map`, `filter` | фильтрация транзакций |

### Практика (Фаза 1)
- Переименовать строки «Листок ИИ» → «WalletAI».
- Понять, как `when (selectedTab)` переключает экраны.

### Ресурсы
- [Kotlin Koans](https://play.kotlinlang.org/koans) (первые 10 уроков)
- [Kotlin docs — basics](https://kotlinlang.org/docs/basic-syntax.html)

---

## L3. Jetpack Compose → перед Фазами 1–2

**Цель:** понимать, как UI рисуется и обновляется.

### Темы
- `@Composable` — функция UI.
- **Состояние**: `remember`, `mutableStateOf`, `collectAsStateWithLifecycle()`.
- **Рекомпозиция** — почему фильтрация должна быть во ViewModel, а не в UI.
- `LazyColumn` + `items` — списки транзакций.
- Material 3: `Scaffold`, `NavigationBar`, `AlertDialog`, `DatePickerDialog`.

### Практика
- После ребренда — изменить заголовок TopBar и увидеть результат без перезапуска (hot reload / rerun).
- Добавить диалог с выбором даты — связать `remember` с ViewModel.

### Ресурсы
- [Compose pathway](https://developer.android.com/jetpack/compose/tutorial)
- [State in Compose](https://developer.android.com/jetpack/compose/state)

---

## L4. Room + Flow + Coroutines → перед Фазой 2

**Цель:** локальная БД как единственный источник правды (offline-first).

### Темы
- **Room**: `@Entity`, `@Dao`, `@Database`, `@Query`, `@Insert`.
- **Flow** — DAO отдаёт поток; UI подписывается через `stateIn` / `collectAsState`.
- **Coroutines**: `viewModelScope.launch`, `Dispatchers.IO` для БД.
- **Migration** — при смене схемы данные не стирать (`fallbackToDestructiveMigration` — только для прототипа).
- **DataStore** — настройки (валюта, тема), не транзакции.

### Практика (Фаза 2)
- Понять миграцию `Double` → `Long` (тиын): почему `0.1 + 0.2 ≠ 0.3`.
- Единый `Money.format()` вместо `String.format` по всему коду.

### Ресурсы
- [Room codelab](https://developer.android.com/codelabs/basic-android-kotlin-training-sqlite-room)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)

---

## L5. Архитектура → перед Фазой 5

**Цель:** разделить UI, бизнес-логику и данные; подготовка к KMP.

### Темы
```
ui/       — Compose + ViewModels (тонкие)
domain/   — модели, use-cases, интерфейсы репозиториев (чистый Kotlin)
data/     — Room, DataStore, GeminiService, реализации репозиториев
```
- **Repository** — ViewModel не знает про Room/OkHttp.
- **Koin DI** — проще Hilt для новичка и работает в KMP.
- **Enum вместо строк** — `"INCOME"` → `TransactionType.INCOME`.
- **Unit-тесты** — классификатор, парсер, форматтер без Android.

### Правило для KMP
Не импортировать `android.*`, `Context`, `Log` в `domain/` и парсерах — только в `ui/` и `data/`.

### Ресурсы
- [Guide to app architecture](https://developer.android.com/topic/architecture)
- [Koin Android](https://insert-koin.io/docs/reference/koin-android/start)

---

## L6. Kotlin Multiplatform (обзор) → перед Фазой 6

**Цель:** понять, что переносится в `shared/`, а что остаётся платформенным.

### Переносится в commonMain
- `Money`, `Currency`, enum'ы
- Naive Bayes, парсер выписки, голосовой парсер
- Интерфейсы репозиториев

### Замены при iOS
| Android сейчас | KMP |
|----------------|-----|
| Room | Room KMP / SQLDelight |
| OkHttp | Ktor Client |
| org.json | kotlinx.serialization |
| DataStore | DataStore KMP |

### UI
**Compose Multiplatform** — те же экраны на iOS (короткий путь, т.к. UI уже на Compose).

### Ресурсы
- [Kotlin Multiplatform docs](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)

---

## Чек-лист «готов к следующей фазе»

| После фазы | Ты должен уметь |
|------------|-----------------|
| 0 | Запустить app, сделать commit |
| 1 | Найти и изменить строку в UI + `strings.xml` |
| 2 | Объяснить, почему сумма хранится в Long |
| 4 | Поделиться CSV через Share |
| 3 | Добавить категорию и увидеть в списке |
| 5 | Найти Repository и написать unit-тест |
| 7 | Добавить трату голосом (2 тапа) |
| 8 | Открыть историю с поиском |
| 6 | Показать модуль `shared/` с commonMain |
