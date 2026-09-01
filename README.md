# Course OOP Library

Desktop JVM application for managing readers, library branches, publications, and loans with a Room-backed SQLite store.

## Architecture

```text
Compose Desktop UI
    ↓ MVVM (StateFlow UI state)
LibraryService facade / application service
    ↓ domain-only repositories
Room repositories → Room 3 DAOs → Bundled SQLite
```

The domain hierarchy is:

```text
LibraryEntity
├── Person
│   ├── Reader
│   └── Librarian
└── Publication
    ├── Book
    └── Periodical

LibraryBranch and Loan also implement LibraryEntity.
```

`Loan` is the associative entity for the many-to-many relationship between readers and publications. A librarian belongs to a branch. All Room relationships use foreign keys and indexes.

The Desktop UI provides four tabs:

- **Readers:** register, search by name, inspect, and delete readers;
- **Publications:** add books/periodicals, search by title or author, inspect inventory, and delete publications;
- **Loans:** select people and publications by names/titles, issue copies, and return active loans;
- **Architecture:** review the class hierarchy, M:N association and implemented patterns, seed the demonstration graph, or transactionally clear application data.

Deletion errors are shown in the UI when active loans protect a reader or publication. Demo seeding is transactional and repeat-safe; cleanup removes dependent rows in referentially safe order.

## Patterns

- **Repository:** domain-only repository interfaces with Room implementations and mappers.
- **MVVM:** Compose screens observe ViewModel `StateFlow` values.
- **Strategy:** `LoanPolicy` provides publication-sensitive due dates and issue rules.
- **Factory:** `LibraryEntityFactory` creates valid entities with generated UUIDs.
- **Observer:** Room DAO `Flow` values are mapped into UI `StateFlow` values.
- **State:** `LoanState` models active, overdue, and returned transitions.
- **Facade/Application Service:** `RoomLibraryService` coordinates validation, atomic issue/return, deletion protection, repeat-safe demo seeding, and transactional cleanup through domain-facing repository abstractions.

Technical identifiers are generated inside the application and are not entered in forms. Users choose readers by names and publications by titles. ISBN is an optional bibliographic field.

## Persistence

Room 3.0.0, KSP 2.3.11, and `androidx.sqlite:sqlite-bundled:2.7.0` are used. The exported schema is committed at:

```text
core/schemas/persistence.LibraryDatabase/1.json
```

The default database path is selected by the host OS:

- macOS: `~/Library/Application Support/CourseOopLibrary/library.db`
- Linux: `~/.local/share/course-oop-library/library.db`
- Windows: `%LOCALAPPDATA%/CourseOopLibrary/library.db`

The schema is version 1 and uses no destructive migration.

## Build and tests

A JDK with Java 17-compatible bytecode support is sufficient; the build config targets JVM 17 without requiring a local JDK 17 toolchain.

```bash
./gradlew clean test build
./gradlew :core:jvmTest
./gradlew :app:jvmTest
./gradlew :core:jvmTest --tests application.ControlScenarioTest
./gradlew :docs:compileTypst
./gradlew :app:run
```

The verified final suite contains **17 passing tests** with zero failures, errors, or skips. The deterministic control test verifies transactional demo seeding, idempotent repeat seeding, issue inventory, and the non-GUI Room path. Persistence and application tests verify write, close, reopen, mapping, atomic issue/return, idempotent return, search filters, deletion protection, transactional cleanup, and ViewModel operations.

## Coursework artifacts

- [`docs/report.pdf`](docs/report.pdf) — 28-page A4 explanatory report.
- [`docs/requirements-matrix.md`](docs/requirements-matrix.md) — requirement-to-evidence matrix.
- [`presentation/course-oop-library.pptx`](presentation/course-oop-library.pptx) — 14-slide 16:9 defense presentation; visible text is at least 28 pt.
- [`docs/images`](docs/images) — UML, Room/ER, architecture, and individual pattern diagrams.

## Project layout

- `core/src/commonMain/kotlin/domain` — domain hierarchy, policy, state, and factory.
- `core/src/commonMain/kotlin/persistence` — Room records, DAO, database, repositories, and mappers.
- `core/src/commonMain/kotlin/application` — application service.
- `core/src/jvmMain/kotlin/persistence` — OS-specific database path and JVM builder.
- `app/src/commonMain/kotlin/di` — explicit application container.
- `app/src/commonMain/kotlin/ui` — Compose UI and ViewModels.
- `artifacts` — factual TDD and verification evidence.
