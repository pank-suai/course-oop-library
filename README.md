# Библиотечная система для курса ООП

Настольное приложение для JVM на Kotlin и Compose Desktop. Система предназначена для учёта читателей, филиалов, изданий и выдач. Данные сохраняются в локальной SQLite-базе через Room 3 и BundledSQLiteDriver.

## Архитектура

```text
Compose Desktop UI
    ↓ MVVM (StateFlow состояния)
LibraryService / RoomLibraryService
    ↓ доменные интерфейсы репозиториев
Room-репозитории → LibraryDao → SQLite
```

Иерархия предметной области:

```text
LibraryEntity
├── Person
│   ├── Reader
│   └── Librarian
└── Publication
    ├── Book
    └── Periodical

LibraryBranch и Loan также реализуют LibraryEntity.
```

Loan является ассоциативной сущностью связи многие-ко-многим между читателями и изданиями. Librarian принадлежит филиалу. Все связи Room используют внешние ключи и индексы.

## Пользовательский интерфейс

В главном окне доступны четыре русские вкладки:

- Читатели: добавление, поиск по имени, просмотр и удаление читателей;
- Издания: добавление книг и периодических изданий, поиск по названию или автору, просмотр остатка и удаление;
- Выдачи: выбор читателя и издания по именам и названиям, выдача экземпляра и возврат активной выдачи;
- Архитектура: просмотр иерархии классов, связи M:N и паттернов, явное добавление демонстрационных данных и транзакционная очистка.

Технические идентификаторы генерируются внутри приложения и не вводятся в формы. ISBN является необязательным библиографическим полем. При активных или исторических выдачах приложение защищает читателя и издание от удаления.

## Паттерны проектирования

- Repository: интерфейсы репозиториев не зависят от Room; реализации выполняют отображение записей;
- MVVM: Compose-экраны наблюдают значения StateFlow из ViewModel;
- Strategy: LoanPolicy задаёт правила выдачи и сроки, зависящие от типа издания;
- Factory: LibraryEntityFactory создаёт корректные сущности и получает UUID через IdGenerator;
- Observer: потоки Flow из DAO преобразуются в StateFlow для UI;
- State: LoanState описывает активную, просроченную и возвращённую выдачу;
- Facade: RoomLibraryService координирует проверку, атомарные выдачу и возврат, защищённое удаление, повторно безопасную загрузку и очистку.

## Сохранение данных

Используются Room 3.0.0, KSP 2.3.11 и androidx.sqlite:sqlite-bundled:2.7.0. Экспортированная схема хранится в `core/schemas/persistence.LibraryDatabase/1.json`. Версия схемы равна 1, деструктивные миграции не используются.

Путь базы данных выбирается по операционной системе:

- macOS: `~/Library/Application Support/CourseOopLibrary/library.db`;
- Linux: `~/.local/share/course-oop-library/library.db`;
- Windows: `%LOCALAPPDATA%/CourseOopLibrary/library.db`.

Выдача выполняет атомарную проверку статуса ACTIVE, лимита активных выдач читателя и доступного экземпляра. Возврат проверяет дату, переводит запись в RETURNED и увеличивает остаток ровно один раз. Просроченные записи переводятся из ACTIVE в OVERDUE при чтении потока выдач.

## Сборка и тестирование

Для сборки нужен JDK с поддержкой байткода Java 17. Локальный toolchain JDK 17 не требуется.

```text
./gradlew clean test build --no-daemon
./gradlew :core:jvmTest
./gradlew :app:jvmTest
./gradlew :core:jvmTest --tests application.ControlScenarioTest
./gradlew :docs:compileTypst --no-daemon
typst compile --root . docs/index.typ docs/report.pdf
typst compile --root . presentation/main.typ presentation/course-oop-library.pdf
./gradlew :app:run
```

Финальный проверенный набор содержит 35 успешно пройденных тестов: 17 в core и 18 в app; ошибки, ошибки выполнения и пропуски отсутствуют. Проверены доменная модель, запись и повторное открытие Room-базы, преобразование записей, M:N-связь, атомарные выдача и возврат, лимит активных выдач, обновление метаданных с сохранением остатка, строгий инвариант ACTIVE, сохранённое и периодическое обновление просрочки, независимое восстановление operation/refresh ошибок, корректное распространение отмены корутин без зависшего busy-state, проверка времени возврата, защищённое удаление, повторно безопасная загрузка, транзакционная очистка и жизненный цикл ViewModel.

## Артефакты курсовой работы

- `docs/report.pdf` — пояснительная записка формата A4 на 51 странице без титульного листа; итоговая сборка и визуальная проверка зафиксированы в `artifacts/report-build.txt`;
- `docs/index.typ` и `docs/lib/gost.typ` — исходник отчёта и форматирование на основе ГОСТ-шаблона предыдущей курсовой работы;
- `docs/requirements-matrix.md` — матрица требований и фактических доказательств;
- `docs/images` — десять авторитетных `.puml` и сгенерированных PlantUML `.svg`, а также снимки интерфейса;
- `presentation/main.typ` — единственный исходник Typst/Polylux для защиты;
- `presentation/lib/guap-template.typ` и `presentation/lib/images` — адаптированный шаблон ГУАП и официальные изображения;
- `presentation/diagrams` — крупношрифтовые PlantUML-представления домена и Room для защиты;
- `presentation/course-oop-library.pdf` — финальная презентация на 15 слайдов 16:9;
- `artifacts/test-results.txt`, `artifacts/build-results.txt`, `artifacts/report-build.txt`, `artifacts/presentation-build.txt` — фактические результаты проверок и сборок.

## Диаграммы

В каталоге `docs/images` находятся пары исходник и результат для каждой технической диаграммы:

- `oop_entities.puml` и `oop_entities.svg`;
- `pattern_repository.puml` и `pattern_repository.svg`;
- `pattern_mvvm.puml` и `pattern_mvvm.svg`;
- `pattern_strategy.puml` и `pattern_strategy.svg`;
- `pattern_factory.puml` и `pattern_factory.svg`;
- `pattern_observer.puml` и `pattern_observer.svg`;
- `pattern_state.puml` и `pattern_state.svg`;
- `pattern_facade.puml` и `pattern_facade.svg`;
- `architecture_oop.puml` и `architecture_oop.svg`;
- `room_schema.puml` и `room_schema.svg`.

Проверка выполняется командами `plantuml -checkonly docs/images/*.puml` и `plantuml -tsvg -charset UTF-8 docs/images/*.puml`. Все десять SVG успешно разбираются XML-парсером; контактный лист визуальной проверки сохранён в `artifacts/diagram-qa/contact-sheet.png`.

## Структура проекта

- `core/src/commonMain/kotlin/domain` — иерархия предметной области, состояния, политика и фабрика;
- `core/src/commonMain/kotlin/persistence` — Room-записи, DAO, база данных, репозитории и преобразователи;
- `core/src/commonMain/kotlin/application` — application service;
- `core/src/jvmMain/kotlin/persistence` — путь базы данных и JVM builder для операционной системы;
- `app/src/commonMain/kotlin/di` — явный контейнер приложения;
- `app/src/commonMain/kotlin/ui` — Compose UI и ViewModel;
- `artifacts` — фактические результаты TDD, сборки и визуальной проверки.
