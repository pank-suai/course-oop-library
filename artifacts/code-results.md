# Code results — final Room architecture

The implementation is split into a pure domain/application core and a Compose Desktop UI. No UI or repository API accepts technical identifiers from users.

| Area | Final implementation | Evidence |
|---|---|---|
| Domain hierarchy | `core/src/commonMain/kotlin/domain/LibraryEntity.kt`, `DomainModels.kt` | `DomainRewriteTest` covers UUID generation, validation, hierarchy, and state transitions |
| Concrete entities | Reader, Librarian, Book, Periodical, LibraryBranch, Loan | Domain compilation plus Room persistence and control tests |
| Strategy and State | `LoanPolicy.kt`, `LoanState` in `DomainModels.kt` | `DomainRewriteTest.loanPolicyDifferentiatesPublicationTypes`, `loanStateAllowsReturnOnlyOnce` |
| Factory | `LibraryEntityFactory.kt` | Domain tests and transactional demo seed |
| Room schema | `persistence/Records.kt`, `LibraryDao.kt`, `LibraryDatabase.kt` | `core/schemas/persistence.LibraryDatabase/1.json` |
| Repositories | `persistence/Repositories.kt` | Close/reopen persistence test and domain-only interfaces |
| Application service | `application/RoomLibraryService.kt` | Atomic issue/return, protected deletion, repeat-safe seed, transactional clear-all, and deterministic control scenario |
| MVVM/Observer | `app/src/commonMain/kotlin/ui/LibraryViewModels.kt` | Validation and operations tests; filtering, deletion errors, administration actions; DAO Flow mapped to StateFlow |
| Dependency injection | `app/src/commonMain/kotlin/di/ApplicationContainer.kt` | Explicit composition root with no global repository instances |
| Desktop UI | `app/src/commonMain/kotlin/ui/LibraryScreen.kt` | Four Compose panes: readers, publications, loans, and architecture; search/add/delete, issue/return, seed/clear; verified 800×600 GUI smoke test |
| Database location | `core/src/jvmMain/kotlin/persistence/DatabaseJvm.kt` | OS-specific application-data paths documented above |

## Verified test slices

- Domain RED→GREEN: `DomainRewriteTest`.
- Room RED→GREEN: `persistence.RoomPersistenceTest`.
- Transaction and invariants: `persistence.RoomInvariantTest`.
- Deterministic non-GUI scenario: `application.ControlScenarioTest`.
- Transactional administration: `application.RoomAdministrationTest`.
- ViewModel validation: `ui.ViewModelValidationTest`.
- ViewModel filtering/deletion/service errors: `ui.ViewModelOperationsTest`.

Exact command output is recorded in `artifacts/tdd-room-rewrite.md`, `artifacts/test-results.txt`, and `artifacts/build-results.txt`.
