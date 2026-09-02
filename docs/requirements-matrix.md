# Матрица «требование → фактическое доказательство»

Матрица составлена по финальному исходному коду, тестам, отчёту, презентации и сохранённым артефактам. Все пути указаны относительно корня репозитория.

| ID | Требование | Фактический артефакт | Проверяемое доказательство / статус |
|---|---|---|---|
| R01 | Иерархия классов предметной области | `core/src/commonMain/kotlin/domain/LibraryEntity.kt`, `DomainModels.kt`, `docs/images/oop_entities.puml`, `oop_entities.svg` | `LibraryEntity` → `Person` → `Reader`/`Librarian`; `LibraryEntity` → `Publication` → `Book`/`Periodical`; отдельные `LibraryBranch` и `Loan`. Выполнено. |
| R02 | Не менее пяти классов сущностей | `core/src/commonMain/kotlin/domain/DomainModels.kt` | В финальной модели шесть конкретных сущностей: `Reader`, `Librarian`, `Book`, `Periodical`, `LibraryBranch`, `Loan`. Выполнено. |
| R03 | Обязательная многозначная зависимость | `core/src/jvmTest/kotlin/persistence/ManyToManyTest.kt`, `core/src/commonMain/kotlin/persistence/Records.kt`, `docs/images/room_schema.puml` | `Loan` содержит `readerId` и `publicationId`; тест оформляет две выдачи одного издания разным читателям и проверяет две записи. Выполнено. |
| R04 | Использовать и обосновать шаблоны проектирования | `domain/LoanPolicy.kt`, `LibraryEntityFactory.kt`, `persistence/Repositories.kt`, `app/.../LibraryViewModels.kt`, `application/RoomLibraryService.kt` | В отчёте отдельно описаны Repository, MVVM, Strategy, Factory, Observer, State и Facade; для каждого подключена финальная PlantUML-диаграмма. Strategy отражает лимит 5, 21 день для Book и 7 дней для Periodical. Выполнено. |
| R05 | Разработать графическое приложение | `app/src/jvmMain/kotlin/main.kt`, `app/src/commonMain/kotlin/ui/LibraryScreen.kt`, `LibraryViewModels.kt`, `docs/images/screen_*_room.png` | `main` открывает Compose Desktop `Window`; экран содержит русские вкладки «Читатели», «Издания», «Выдачи» и «Архитектура», поиск, создание, удаление, выдачу, возврат, загрузку и очистку. Выполнено сборкой и GUI smoke test. |
| R06 | Результат должен быть Desktop JVM-only | `core/build.gradle.kts`, `app/build.gradle.kts`, `app/src/jvmMain/kotlin/main.kt`, `.github/workflows/ci.yml` | В модулях объявлена цель `jvm()`, есть JVM-точка входа и Compose Desktop; CI выполняет JVM test/build. Выполнено. |
| R07 | Выделить сущностные, управляющие и интерфейсные классы | `core/src/commonMain/kotlin/domain`, `persistence`, `application`; `app/src/commonMain/kotlin/ui`, `di` | Сущности отделены от Room-записей; управляющие классы включают `RoomLibraryService`, репозитории и ViewModel; интерфейсный слой — Compose-компоненты и JVM `main`. Выполнено. |
| R08 | Подготовить диаграммы сущностей, каждого паттерна и общую диаграмму | десять пар `docs/images/*.puml` и `docs/images/*.svg` | Все десять исходников проверены `plantuml -checkonly`, перегенерированы PlantUML 1.2026.7 и успешно разобраны XML-парсером. Выполнено. |
| R09 | Протестировать классы и функциональные сценарии | `core/src/commonTest`, `core/src/jvmTest`, `app/src/commonTest`, `artifacts/test-results.txt` | Итог `./gradlew clean test build --no-daemon`: 35 тестов, 17 core и 18 app, 0 failures, 0 errors, 0 skipped. Выполнено. |
| R10 | Подготовить пояснительную записку установленной структуры | `docs/index.typ`, `docs/lib/gost.typ`, `docs/report.pdf`, `artifacts/report-build.txt` | Отчёт без титульного листа, с заданием, содержанием, введением, разделами 1–4, заключением, источниками и приложением с полными текущими production-листингами. `typst compile` и Gradle-задача успешны; формат на 51 странице. Выполнено. |
| R11 | Подготовить презентацию на 10–15 слайдов | `presentation/main.typ`, `presentation/lib/guap-template.typ`, `presentation/diagrams`, `presentation/course-oop-library.pdf`, `artifacts/presentation-build.txt` | Единственный конвейер Typst/Polylux содержит 15 слайдов формата 16:9. Текст шаблона, карточек и крупношрифтовых PlantUML-схем не меньше 28 pt; снимки показывают реальный UI в штатном масштабе. Все слайды отрендерены и проверены. Выполнено. |
| R12 | Сохранять данные между запусками в SQLite через Room 3 | `core/src/commonMain/kotlin/persistence/LibraryDatabase.kt`, `LibraryDao.kt`, `Records.kt`, `Repositories.kt`, `core/src/jvmMain/kotlin/persistence/DatabaseJvm.kt`, `core/schemas/persistence.LibraryDatabase/1.json` | Room 3 с `BundledSQLiteDriver`; пять таблиц, внешние ключи и индексы; тест закрывает базу, открывает файл снова и сравнивает доменные данные. Выполнено. |
| R13 | Не переносить искусственные форматы и самописные структуры прежней работы | финальные каталоги `core/src`, `app/src`, `README.md`, `docs/index.typ`, `presentation/main.typ` | Финальная архитектура использует UUID, Room/SQLite, DAO и репозитории; устаревшие алгоритмические описания, старый конвейер презентации и старые артефакты удалены. Выполнено. |

## Фактические результаты и ограничения

- `artifacts/test-results.txt` содержит итоговый Gradle-прогон и классификацию 35 тестов: 17 core и 18 app.
- `artifacts/build-results.txt` содержит успешный `clean test build` с итогом `BUILD SUCCESSFUL`.
- `artifacts/report-build.txt` содержит обе успешные команды компиляции отчёта, размер A4, фактическую 51 страницу и полный перечень визуальной QA.
- `artifacts/presentation-build.txt` содержит успешную сборку Typst, размер 16:9, 15 слайдов и полный перечень визуальной QA.
- `artifacts/diagram-qa/contact-sheet.png` содержит контактный лист всех десяти технических диаграмм.
- Поля даты выдачи задания и формального согласования темы в отчёте не заполняются, поскольку такие сведения не подтверждены доступными источниками.
- Финальные снимки текущего Room-окна сохранены в `docs/images/screen_*_room.png`, визуально проверены и включены в отчёт и презентацию.
