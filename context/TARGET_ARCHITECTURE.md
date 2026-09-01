# Целевая архитектура курсового проекта

## Назначение

Настольное JVM-приложение для ведения читателей, библиотечного фонда и выдач. Интерфейс не просит пользователя вводить технические идентификаторы: они создаются `IdGenerator`. Данные сохраняются в SQLite через Room 3 и восстанавливаются после перезапуска.

## Предметная модель

Иерархия предметной области отделена от способа хранения:

- `LibraryEntity` — корневой интерфейс со стабильным непрозрачным `id`;
- `Person` → `Reader`, `Librarian`;
- `Publication` → `Book`, `Periodical`;
- `LibraryBranch` — филиал;
- `Loan` — ассоциативная сущность, реализующая связь M:N между читателями и изданиями;
- `LoanState` → `Active`, `Overdue`, `Returned`.

Конкретных сущностей не менее семи: `Reader`, `Librarian`, `Book`, `Periodical`, `LibraryBranch`, `Loan` и состояние/правило выдачи как самостоятельные объекты предметной области.

## Идентификаторы

- В модели идентификатор представлен `String` и не несёт предметной семантики.
- `UuidIdGenerator` создаёт UUID на JVM.
- Формы создания читателя, издания и выдачи не содержат поля ID, шифра книги или номера билета.
- В интерфейсе сущности выбираются по понятным названиям и ФИО; ID используется только внутри приложения и базы.

## Room 3

Используются официальный KMP API Room 3, KSP и `BundledSQLiteDriver`.

### Таблицы

- `readers`: `id`, `fullName`, `birthYear`, `address`, `organization`, `registeredAt`;
- `librarians`: `id`, `fullName`, `position`;
- `branches`: `id`, `name`, `address`;
- `publications`: `id`, discriminator `kind`, `title`, `authors`, `publisher`, `publicationYear`, поля подтипа `isbn`/`issueNumber`, `totalCopies`, `availableCopies`, `branchId`;
- `loans`: `id`, `readerId`, `publicationId`, `librarianId`, `issuedAt`, `dueAt`, `returnedAt`, `status`.

`loans.readerId` и `loans.publicationId` — внешние ключи. История выдач материализует M:N связь `Reader ↔ Publication`.

### Слои

1. `domain` — чистые сущности, состояния, правила выдачи, фабрики, ошибки;
2. `persistence` — Room entities, DAO, database, converters, mappers;
3. `repository` — интерфейсы и Room-реализации;
4. `application` — `LibraryService`, атомарные сценарии выдачи/возврата и проверки инвариантов;
5. `ui` — Compose Desktop, MVVM, состояния экранов.

### Транзакции

Выдача атомарно:

1. проверяет существование читателя и издания;
2. применяет `LoanPolicy`;
3. проверяет доступность экземпляра;
4. добавляет `Loan`;
5. уменьшает `availableCopies`.

Возврат атомарно переводит выдачу в `Returned` и увеличивает `availableCopies` ровно один раз. Удаление читателя/издания с активными выдачами запрещено понятной доменной ошибкой.

## Паттерны проектирования

- **Repository** — домен зависит от интерфейсов репозиториев, а не от Room;
- **MVVM** — Compose-экраны наблюдают `UiState`, ViewModel вызывает application service;
- **Strategy** — `LoanPolicy` определяет допустимость и срок выдачи для книги/периодики;
- **Factory** — `LibraryEntityFactory` и `DemoLibraryFactory` создают валидные объекты и связанный демонстрационный граф;
- **Observer** — `Flow` DAO → repository → `StateFlow` ViewModel автоматически обновляет UI;
- **State** — `LoanState` задаёт `Active`, `Overdue`, `Returned` и допустимые переходы;
- **Facade / Application Service** — `LibraryService` предоставляет цельные пользовательские сценарии и скрывает координацию нескольких DAO.

Паттерны должны быть реальными точками расширения, покрыты тестами и иметь отдельные диаграммы. Не допускается декоративная реализация паттерна только ради названия.

## Что удалить из старого проекта

- `BookCipher`, `ReaderTicket` и ручную валидацию их форматов;
- `BookAVLTree`, `ReaderHashTable`, `LinearLinkedList`, `LoanSkipList`;
- Boyer–Moore, сортировку включением, симметричный обход и связанные настройки;
- репозитории `AVLBooksRepository`, `MapReadersRepository`, `SkipListLoansRepository` и singleton service locator;
- упоминания варианта 624 и старой методички по САОД;
- SVG старых алгоритмических структур и тесты алгоритмов;
- web/JS/WASM targets, ресурсы и Pages workflow.

## Проверка

Минимальные доказательства:

1. unit-тесты иерархии, фабрики, Strategy и State;
2. Room integration test: записать данные → закрыть базу → открыть снова → получить те же данные;
3. integration tests атомарной выдачи и идемпотентного возврата;
4. поиск по репозиторию не находит `BookCipher`, `ReaderTicket`, `AVL`, `SkipList`, `BoyerMoore`, `624` в актуальных исходниках и документации;
5. `./gradlew clean test build` проходит;
6. Desktop-приложение запускается и проходит контрольный сценарий;
7. отчёт и презентация описывают только фактическую архитектуру Room и перечисленные паттерны.
