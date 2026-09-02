# TDD-доказательства — финальная Room-архитектура

Все команды выполнялись из корня репозитория. Числа тестов в разделах 1–16 — исторические контрольные точки соответствующих RED → GREEN циклов, а не текущее состояние; актуальный итог приведён только в последнем разделе 17 и `artifacts/test-results.txt`.

## Вертикальные циклы RED → GREEN

### 1. Доменная модель

- RED: `./gradlew :core:jvmTest --tests DomainRewriteTest` — ошибка компиляции: новые доменные типы отсутствовали, а старый частичный domain ссылался на удалённые типы.
- GREEN: `BUILD SUCCESSFUL`; проверены 4 доменных поведения.

### 2. Room persistence и сервис

- RED: `./gradlew :core:jvmTest --tests persistence.RoomPersistenceTest` — ошибка компиляции тестов из-за отсутствующих builder базы, репозиториев и application service API.
- GREEN: `BUILD SUCCESSFUL`; 2 теста проверили закрытие, повторное открытие базы и инварианты инвентаря.

### 3. Транзакционные инварианты

- Первый запуск завершился ошибкой инициализации JUnit: coroutine-test выражение возвращало Boolean вместо Unit.
- После явного возврата Unit GREEN дал `BUILD SUCCESSFUL`; пройдены защита удаления активных выдач и полный откат демонстрационной загрузки.

### 4. Валидация ViewModel

- RED: `./gradlew :app:jvmTest --tests ui.ViewModelValidationTest` — действие ViewModel не сообщало результат валидации, а начальное значение StateFlow не было синхронным результатом.
- GREEN: `BUILD SUCCESSFUL`; некорректный читатель отклоняется без записи в репозиторий.

### 5. Детерминированный контрольный сценарий

- `./gradlew :core:jvmTest --tests application.ControlScenarioTest` — `BUILD SUCCESSFUL`; заполнение демонстрационными данными транзакционно, repeat-safe, создаёт ожидаемый граф и уменьшает остаток один раз.

### 6. Поиск, удаление и администрирование

- RED выявил отсутствующие операции удаления, поиска и администрирования в сервисе и ViewModel.
- GREEN: `./gradlew :core:jvmTest :app:jvmTest --no-daemon` — `BUILD SUCCESSFUL`; 17 тестов проверили фильтры, удаление, передачу ошибок, идемпотентную загрузку и очистку.

## Регрессионные циклы после проверки

### 7. Конкурентность, инвентарь, выдача и overdue

- RED выявил отсутствующий API LoanRepository.refreshOverdue.
- GREEN: `./gradlew :core:jvmTest --tests persistence.ReviewRegressionTest --no-daemon --offline` — `BUILD SUCCESSFUL`; 5 тестов проверили лимит читателя, сохранение остатка, начальные состояния, время возврата и переход в overdue.

### 8. Жизненный цикл ViewModel и ошибки выдач

- RED-команда сначала превысила лимит 180 секунд: пустой Flow выдач не позволял сформировать объединённое состояние; фикстура была исправлена до GREEN.
- GREEN: `./gradlew :app:jvmTest --tests ui.ViewModelLifecycleAndErrorTest --no-daemon --offline` — `BUILD SUCCESSFUL`; 2 теста проверили отмену eager-областей и передачу ошибки Flow библиотекаря в UiState выдач.

### 9. Полный сфокусированный набор

- `./gradlew :core:jvmTest :app:jvmTest --no-daemon --offline` — `BUILD SUCCESSFUL`; 24 теста, 0 failures и 0 errors.

### 10. Историческая обязательная проверка этапа

- `./gradlew clean test build --no-daemon` — `BUILD SUCCESSFUL`.
- Разбор JUnit XML: 24 теста, 0 failures, 0 errors, 0 skipped; core — 17, app — 7.

### 11. Русификация демонстрационного набора

- RED: `./gradlew :core:jvmTest --tests application.ControlScenarioTest --no-daemon` — контрольный тест ожидал русские названия и завершился с 1 failure на прежних англоязычных данных.
- GREEN: та же команда после замены пользовательских значений seed на русские завершилась `BUILD SUCCESSFUL`; затем `./gradlew clean test build --no-daemon` также завершилась успешно, 24 теста без ошибок.

### 12. Защита общего и доступного числа экземпляров

- RED: `./gradlew :core:jvmTest --tests persistence.ReviewRegressionTest.savingAnExistingPublicationPreservesDatabaseInventoryAndReturnability --no-daemon` — 1 failure: generic save принял устаревшее `totalCopies` во время активной выдачи.
- GREEN: после перевода `totalCopies` и `availableCopies` существующего издания под управление базы та же команда завершилась `BUILD SUCCESSFUL`; возврат после metadata-save восстанавливает остаток до 2. Полный `./gradlew clean test build --no-daemon` также завершился успешно, 24 теста без ошибок.

### 13. Периодическое обновление просроченных выдач

- RED: `./gradlew :app:jvmTest --tests ui.ViewModelLifecycleAndErrorTest.overdueLoansRefreshWhileViewModelRemainsOpen --no-daemon` — компиляция теста завершилась ошибкой: LoansViewModel не имел управляемого интервала и источника времени, а в production отсутствовал периодический вызов `refreshOverdue`.
- GREEN: LoansViewModel получил отменяемую вместе со scope минутную корутину обновления, а тест — детерминированные интервал и время. Сфокусированная команда и последующий `./gradlew clean test build --no-daemon` завершились `BUILD SUCCESSFUL`.
- Разбор JUnit XML на этом этапе: 25 тестов, 0 failures, 0 errors, 0 skipped; core — 17, app — 8.

### 14. Сброс устаревшей ошибки после успешной операции

- RED: два теста `successfulIssueClearsPreviousOperationError` и `successfulReturnClearsPreviousOperationError` завершились тайм-аутом после схемы «ошибка → успешный повтор»: LoansViewModel сохранял прежний текст ошибки в UiState.
- GREEN: успешные `issueLoan` и `returnLoan` теперь очищают error-flow; сфокусированная команда завершилась `BUILD SUCCESSFUL`, а последующий `./gradlew clean test build --no-daemon` прошёл полностью.
- Разбор JUnit XML на этом этапе: 27 тестов, 0 failures, 0 errors, 0 skipped; core — 17, app — 10.

### 15. Распространение отмены корутин

- RED: пять проверок reader, publication, loan action, overdue refresh и administration воспроизвели преобразование `CancellationException` в пользовательскую ошибку внутри обычного `runCatching`.
- GREEN: общий `runCatchingCancellable` повторно выбрасывает `CancellationException`, а остальные исключения сохраняет как `Result.failure`; все suspend-операции ViewModel переведены на этот путь.
- Сфокусированные проверки каждого сценария, весь `ViewModelLifecycleAndErrorTest` и `./gradlew clean test build --no-daemon` завершились `BUILD SUCCESSFUL`.
- Разбор JUnit XML на этом этапе: 32 теста, 0 failures, 0 errors, 0 skipped; core — 17, app — 15.

### 16. Восстановление error и busy после временного сбоя

- RED: `successfulOverdueRefreshClearsPreviousRefreshError` завершился тайм-аутом после схемы «ошибка refresh → успешный refresh», а расширенный administration cancellation-тест обнаружил оставшийся `isBusy=true`.
- GREEN: успешный периодический refresh очищает прежнюю ошибку; административные операции в `finally` снимают busy-state при отдельной отмене, но не изменяют состояние после `close()`, когда scope уже неактивен.
- Сфокусированные проверки, весь `ViewModelLifecycleAndErrorTest` и полный clean build завершились `BUILD SUCCESSFUL`.
- Разбор JUnit XML на этом этапе: 33 теста, 0 failures, 0 errors, 0 skipped; core — 17, app — 16.

### 17. Независимые ошибки пользовательских операций и обновления просрочки

- RED: `successfulRefreshDoesNotClearOperationError` воспроизвёл перекрёстное скрытие: успешный периодический refresh обнулял ещё актуальную ошибку выдачи.
- GREEN: LoansViewModel получил независимые `operationErrors` и `refreshErrors`; успешный сценарий очищает только ошибку собственного типа, а UI отдаёт приоритет ошибке пользовательской операции.
- Симметричная регрессионная проверка `successfulOperationDoesNotClearRefreshError` подтверждает, что успешная выдача не скрывает ошибку refresh.
- Сфокусированные проверки обоих направлений, весь `ViewModelLifecycleAndErrorTest` и полный clean build завершились `BUILD SUCCESSFUL`.
- Актуальный разбор JUnit XML: 35 тестов, 0 failures, 0 errors, 0 skipped; core — 17, app — 18.

## Примечания

Экспорт схемы Room включён для версии 1. Room-тесты используют реальный дисковый SQLite-файл и закрывают его перед повторным открытием. Bundled SQLite выводит предупреждение текущего JDK о native access; оно не приводит к ошибке. CI настроен на Java 17 bytecode/runtime.
