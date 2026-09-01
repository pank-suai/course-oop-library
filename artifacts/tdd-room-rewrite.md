# TDD evidence — Room-backed rewrite

All commands below were run from the repository root.

## Vertical RED → GREEN cycles

### 1. Domain

- RED command: `./gradlew :core:jvmTest --tests DomainRewriteTest`
- RED result: failed during compilation because the new domain types were not present and the old partial domain still referenced removed types.
- GREEN command: `./gradlew :core:jvmTest --tests DomainRewriteTest`
- GREEN result: `BUILD SUCCESSFUL`; 4 domain behaviors passed.

### 2. Room persistence and service

- RED command: `./gradlew :core:jvmTest --tests persistence.RoomPersistenceTest`
- RED result: failed during test compilation with unresolved database builder, repositories, and application service APIs.
- GREEN command: `./gradlew :core:jvmTest --tests persistence.RoomPersistenceTest`
- GREEN result: `BUILD SUCCESSFUL`; 2 persistence/service tests passed, including close/reopen and inventory invariants.

### 3. Transaction invariants

- Command: `./gradlew :core:jvmTest --tests persistence.RoomInvariantTest`
- First result: failed with a JUnit initialization error because the coroutine test expression returned a Boolean instead of Unit.
- Fix: make both test functions return Unit explicitly.
- GREEN result: `BUILD SUCCESSFUL`; active-loan deletion protection and full demo rollback passed.

### 4. ViewModel validation

- RED command: `./gradlew :app:jvmTest --tests ui.ViewModelValidationTest`
- RED result: failed because the ViewModel action did not expose validation success/failure and the initial StateFlow value was not a synchronous validation result.
- GREEN command: `./gradlew :app:jvmTest --tests ui.ViewModelValidationTest`
- GREEN result: `BUILD SUCCESSFUL`; invalid reader input is rejected without a repository write.

### 5. Deterministic control scenario

- Command: `./gradlew :core:jvmTest --tests application.ControlScenarioTest`
- Result: `BUILD SUCCESSFUL`; demo seed is transactional, repeat-safe, creates the expected graph, and decrements inventory once.

### 6. User-facing search, deletion, and administration

- RED command: `./gradlew :core:jvmTest --tests application.RoomAdministrationTest --no-daemon`
- RED result: failed in test compilation because `RoomLibraryService.deleteReader`, `deletePublication`, and `clearAll` were not yet exposed; the test also exposed the missing Flow assertion import.
- RED command: `./gradlew :app:jvmTest --tests ui.ViewModelOperationsTest --no-daemon`
- RED result: failed in test compilation because ViewModels had no search action/deletion API and `LibraryService` had no administration operations.
- GREEN command: `./gradlew :core:jvmTest :app:jvmTest --no-daemon`
- GREEN result: `BUILD SUCCESSFUL`; 17 tests passed, including name/title/author filters, successful ViewModel deletion, active-loan error propagation, idempotent seed, and transactional clear-all counts.

## Notes

- Room schema export is enabled for version 1.
- The Room tests use a real on-disk SQLite file and close/reopen it.
- Bundled SQLite prints a Java native-access warning on the current JDK; it does not fail tests. CI uses Java 17 bytecode/runtime as configured.
