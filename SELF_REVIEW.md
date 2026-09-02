# Self-Review

## Scope

This is a fresh independent review of the current working tree at `/Users/pank-su/course-oop-library`. The review scope was taken from `context/TASK.md:19-29`, `context/context.yaml:17-51`, and `context/requirements-checklist.md:3-28`, then checked against the current Kotlin source, tests, Room schema, Gradle/CI configuration, README, requirements matrix, report, presentation, diagrams, and saved evidence. No tracked solution/source/report/presentation file was edited during this review; only this new review file and generated review helper evidence under `artifacts/self-review-final/` were written.

## Requirement Coverage

| ID | Requirement | Evidence | Status | Finding |
|---|---|---|---|---|
| R01 | Domain class hierarchy | `core/src/commonMain/kotlin/domain/LibraryEntity.kt:14-35`, `DomainModels.kt:4-129`, `docs/images/oop_entities.puml`, report pp. 7-8 | passed | `LibraryEntity`, `Person`, `Publication`, `Reader`, `Librarian`, `Book`, and `Periodical` are present with the additional `Loan` and `LibraryBranch` entities. |
| R02 | At least five entity classes | `core/src/commonMain/kotlin/domain/DomainModels.kt:4-129`, `docs/requirements-matrix.md:8` | passed | Six concrete entity data classes are implemented: `Reader`, `Librarian`, `Book`, `Periodical`, `LibraryBranch`, and `Loan`. |
| R03 | Mandatory many-to-many dependency | `core/src/commonMain/kotlin/persistence/Records.kt`, `LibraryDao.kt:140-183`, `core/src/jvmTest/kotlin/persistence/ManyToManyTest.kt`, schema `1.json:240-360` | passed | `Loan` links `readerId` and `publicationId`; the integration test verifies two readers can receive the same publication. |
| R04 | Design patterns used and justified | `core/src/commonMain/kotlin/domain/LoanPolicy.kt:3-44`, `LibraryEntityFactory.kt`, `persistence/Repositories.kt:18-176`, `application/RoomLibraryService.kt:14-72`, `app/src/commonMain/kotlin/ui/LibraryViewModels.kt:51-293`; report pp. 10-14 | passed | Repository, MVVM, Strategy, Factory, Observer, State, and Facade have concrete production roles, tests, and diagrams. |
| R05 | Graphical test application | `app/src/jvmMain/kotlin/main.kt:6-17`, `app/src/commonMain/kotlin/ui/LibraryScreen.kt:38-299`, `docs/images/screen_*_room.png` | passed | Compose Desktop opens a JVM window with four Russian tabs and the documented reader, publication, loan, and administration scenarios. |
| R06 | Desktop JVM-only; no web application | `core/build.gradle.kts:7-9`, `app/build.gradle.kts:9-10,35-44`, `.github/workflows/ci.yml:30-37` | passed | Both modules declare only `jvm()`; the entry point is a Compose Desktop JVM `main`; no JS/WASM/Pages path was found. |
| R07 | Entity, control, and interface class separation | `core/src/commonMain/kotlin/{domain,application,persistence}`, `app/src/commonMain/kotlin/{ui,di}`, report pp. 8-10 | passed | Domain entities, Room/persistence adapters, application service, ViewModels, DI, and Compose components are separated by responsibility. |
| R08 | Entity, every-pattern, overall, and Room diagrams | `docs/images/*.puml` and `*.svg` (10 pairs), `presentation/diagrams/*.puml` and `*.svg` (2 pairs), `artifacts/self-review-final/checks.json:866-899` | passed | PlantUML check exited 0; all 10 report PUML files and both presentation PUML files have valid generated SVG counterparts. |
| R09 | Class and functional-scenario testing | `core/src/commonTest`, `core/src/jvmTest`, `app/src/commonTest`, current Gradle HTML reports, `artifacts/test-results.txt:5-24` | passed | Fresh clean test/build completed successfully; current HTML summaries are 17 core + 18 app = 35 tests, with zero failures and zero skips. |
| R10 | Structured explanatory report | `docs/index.typ`, `docs/lib/gost.typ`, `/tmp/course-oop-library-report-review.pdf`, `artifacts/report-build.txt:19-32` | passed | Direct Typst compilation succeeded and produced 51 A4 pages containing the assignment, contents, introduction, sections 1-4, conclusion, sources, and Appendix A. |
| R11 | 10-15 slide Typst/Polylux presentation, minimum 28 pt | `presentation/main.typ:1-193`, `presentation/lib/guap-template.typ:32-145`, `/tmp/course-oop-library-presentation-review.pdf`, `artifacts/presentation-build.txt:11-24` | passed | Direct compilation succeeded; the PDF is 15 pages at 16:9. Literal source sizes have a verified minimum of 28 pt. All 15 rendered slides were inspected. |
| R12 | Persistent Room 3/SQLite storage | `core/src/commonMain/kotlin/persistence/LibraryDatabase.kt:8-16`, `LibraryDao.kt:11-241`, `Records.kt`, `Repositories.kt`, `core/schemas/persistence.LibraryDatabase/1.json:1-367`, `RoomPersistenceTest` | passed | Room 3 uses the bundled SQLite driver, five tables, indexes, RESTRICT foreign keys, transactions, exported schema, and close/reopen persistence tests. |
| R13 | Remove inherited artificial algorithms/formats | `README.md:42-62`, current `core/src` and `app/src`, `artifacts/self-review-final/checks.json:900-905` | passed | Legacy scan returned no hits; current implementation uses UUID, Room/SQLite, repositories, and DAO rather than the old cipher/ticket/custom-structure solution. |
| EXT-01 | Formal topic approval | `context/context.yaml:49,90`, `docs/index.typ:28` | passed with minor external finding | The repository explicitly states that formal teacher approval is not evidenced. This is external to the software deliverable and is not a blocker or major defect; obtain and attach written approval before submission if required. |

## Checks Executed

| Check | Command or tool | Exit status | Evidence | Status |
|---|---|---:|---|---|
| Clean project test/build with forced task execution | `./gradlew clean test build --no-daemon --rerun-tasks` | 0 | Terminal output ended `BUILD SUCCESSFUL`; current HTML reports show core 17 and app 18. | passed |
| Test-count and failure summary | Python XML/HTML parser over `core/build/reports/tests/allTests/index.html` and `app/build/reports/tests/allTests/index.html` | 0 | `HTML_TEST_TOTAL 35`; module counters 17 and 18; failures/skips both zero. | passed |
| Report compilation | `typst compile --root . docs/index.typ /tmp/course-oop-library-report-review.pdf` | 0 | `pdfinfo`: 51 pages, A4, 595.276 x 841.89 pt. | passed |
| Presentation compilation | `typst compile --root . presentation/main.typ /tmp/course-oop-library-presentation-review.pdf` | 0 | `pdfinfo`: 15 pages, 841.89 x 473.563 pt (16:9). | passed |
| PlantUML source validation | `plantuml -checkonly docs/images/*.puml` | 0 | Ten report diagram sources accepted; generated SVG/XML counts in `artifacts/self-review-final/checks.json:866-899` are 12/12 valid. | passed |
| Presentation font floor | Python literal-size scan of `presentation/main.typ` and `presentation/lib/guap-template.typ` | 0 | Sizes found: 28, 30, 32, 34, 40, 46, 50 pt; minimum 28 pt and default text 28 pt. | passed |
| Production listing coverage | Python comparison of current production Kotlin text with extracted report text | 0 | `LISTING_CONTENT_MATCHES 17 / 17`; no missing files; report listing headings 17 and production Kotlin count 17. | passed |
| Report page structure | Python `pdftotext` assertions plus native visual inspection | 0 | 51 text pages; page 20 contains 35 tests and lifecycle 13; page 21 has 1300 non-whitespace characters; page 22 begins `ЗАКЛЮЧЕНИЕ`; page 51 ends in the final `}` before the printed page number. | passed |
| Legacy/secret and whitespace scans | Python scan of current source/docs plus `git diff --check` | 0 | Legacy hits `[]`, secret hits `[]`, and `git diff --check` produced no output. No secret values were reproduced. | passed |

The first attempted forced clean build encountered a transient Kotlin incremental-cache registration lock. After `./gradlew --stop`, the same command passed without any source change. This was an environment/daemon condition, not a reproducible project failure. The build also emitted non-fatal JDK native-access and Kotlin deprecation/Beta warnings; neither changed the successful result.

## Code Review

The domain model has explicit validation and sealed `LoanState` transitions (`core/src/commonMain/kotlin/domain/DomainModels.kt:90-129`). `DefaultLoanPolicy` separates issuance limits and Book/Periodical due dates (`LoanPolicy.kt:13-43`). `RoomLibraryService` coordinates lookup, policy, UUID creation, and atomic issue/return/administration scenarios (`RoomLibraryService.kt:19-72`). `LibraryDao` contains explicit transactional issue, return, cleanup, and repeat-safe demo-seeding operations (`LibraryDao.kt:101-239`). Repository adapters keep Room records behind domain interfaces (`Repositories.kt:18-176`).

The UI/ViewModel layer validates user input before asynchronous writes, maps Room `Flow` to `StateFlow`, keeps operation and refresh errors independent, propagates cancellation, periodically refreshes overdue loans, and closes its scopes (`app/src/commonMain/kotlin/ui/LibraryViewModels.kt:37-293`). The Compose screen exposes only human-readable selection and does not ask users to enter technical IDs (`LibraryScreen.kt:73-299`). CI uses JDK 17 and runs JVM tests/build plus report compilation (`.github/workflows/ci.yml:20-47`). No placeholders, inherited algorithm names, embedded credentials, or unhandled build failures were found in the reviewed current paths.

## Mathematics and Artifacts

The principal numeric rules are internally consistent: the Strategy implementation uses 21 days for books, 7 days for periodicals, and a default active-loan limit of 5 (`core/src/commonMain/kotlin/domain/LoanPolicy.kt:13-42`); the report states and the tests exercise those same values. DAO inventory updates are conditional and transactional (`LibraryDao.kt:118-183`), and return increments availability once only. The exported schema contains five tables, the publication inventory fields, loan status/timestamps, indexes, and RESTRICT foreign keys (`core/schemas/persistence.LibraryDatabase/1.json:8-360`).

The current evidence records 35 passing tests (17 core, 18 app), a successful deterministic control scenario, successful report/presentation compilation, 10 technical diagram pairs, 2 presentation diagram pairs, and 17/17 production listings. Historical checkpoints in `artifacts/tdd-room-rewrite.md:71-99` are explicitly labelled as intermediate or current; the final current checkpoint is 35 tests and is not confused with the historical 25/27/32/33-test checkpoints.

## Visual Report Review

All 51 rendered report pages in `artifacts/self-review-final/report-pages/` were inspected through the four page-numbered contact sheets and the native-size pages 20, 21, 22, and 51. Pages 1-4 contain the assignment, contents, and introduction; pages 5-23 contain the report body, tables, diagrams, and UI evidence; pages 24-51 contain Appendix A listings.

- Page 20 visibly contains the complete testing table, the 35-test total, and `ViewModelLifecycleAndErrorTest` with 13 tests; the table is contained and legible.
- Page 21 is a substantive, normal end-of-testing page rather than a sparse accidental remainder; it contains the focused-run and evidence paragraphs and ends cleanly.
- Page 22 begins with the centered `ЗАКЛЮЧЕНИЕ` heading and has a complete conclusion page.
- Pages 24-51 are all non-empty listing pages. The current source-to-extracted-text comparison matches all 17 production Kotlin files, with no missing or unexpected listing headings. Page 51 contains the end of `LibraryScreen.kt`, the `app/src/jvmMain/kotlin/main.kt` heading, and its complete closing braces before the page number.
- Contact-sheet and native inspection found no clipped text, overlapping blocks, broken tables, broken diagrams, blank page, or near-empty accidental page. The `checks.json` render statistics show all report pages at 1191 x 1684 with zero edge ink.

## Visual Evidence

| Render set | Coverage and inspection evidence | Result |
|---|---|---|
| Report contact 01 | pages 1-16: `artifacts/self-review-final/report-contact-01.png` | All pages present; title/contents/introduction/body layouts contained. |
| Report contact 02 | pages 17-32: `report-contact-02.png` | UI, testing, conclusion, sources, and listing transition contained; pages 20-22 additionally inspected natively. |
| Report contact 03 | pages 33-48: `report-contact-03.png` | Continuous current listings with readable syntax coloring and no clipping/overlap. |
| Report contact 04 | pages 49-51: `report-contact-04.png` | Final listing pages contained; page 51 additionally inspected natively and ends with final braces. |
| Presentation slides 1-5 | `artifacts/self-review-final/presentation-slides/slide-01.png` through `slide-05.png` | Title, goal, requirements, domain, and Room slides intact. |
| Presentation slides 6-10 | `slide-06.png` through `slide-10.png` | Architecture and pattern slides intact; intentional whitespace is balanced and not an accidental blank slide. |
| Presentation slides 11-13 | `slide-11.png` through `slide-13.png` | State, transaction, and UI slides intact with contained arrows, cards, and screenshots. |
| Presentation slide 14 | `slide-14.png` | Visible headline is `35 тестов`; table shows `app`/`ViewModel` = `18`, with core rows 4 + 10 + 3 and no clipping or stale total. |
| Presentation slide 15 | `slide-15.png` | Final `ИТОГ` slide is intact, all conclusion lines and `Спасибо за внимание` are visible, and the decorative background does not obscure text. |

All 15 slides were inspected individually. No slide is blank or accidentally near-empty; the sparse card slides are intentional presentations of two or three pattern roles and retain their headings, body text, page number, and visual hierarchy. No clipping, overlap, or content loss was observed.

## Changes Requested

None for the reviewed repository state. The only open item is the minor external approval evidence recorded above; it does not require a code/report repair and does not prevent the technical deliverables from passing this review.

## Blockers

None. The transient first-run cache lock was cleared by stopping Gradle daemons and did not recur on the verified rerun. No blocker or major finding remains.

## Final Status

Final Status: passed
