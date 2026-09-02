package ui

import domain.Book
import domain.IdGenerator
import domain.LibraryBranch
import domain.LibraryService
import domain.Loan
import domain.Librarian
import domain.Periodical
import domain.Publication
import domain.Reader
import domain.UuidIdGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import persistence.BranchRepository
import persistence.LibrarianRepository
import persistence.LoanRepository
import persistence.PublicationRepository
import persistence.ReaderRepository

private inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (error: CancellationException) {
    throw error
} catch (error: Throwable) {
    Result.failure(error)
}

data class CollectionUiState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class ReadersViewModel(
    private val repository: ReaderRepository,
    private val ids: IdGenerator = UuidIdGenerator(),
    private val service: LibraryService? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : AutoCloseable {
    private val errors = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    val query: StateFlow<String> = searchQuery.asStateFlow()
    val state: StateFlow<CollectionUiState<Reader>> = combine(repository.readers, searchQuery) { readers, query ->
        CollectionUiState(
            items = readers.filter { it.fullName.contains(query.trim(), ignoreCase = true) },
            isLoading = false,
        )
    }
        .catch { emit(CollectionUiState(isLoading = false, error = it.message ?: "Не удалось загрузить читателей")) }
        .combine(errors) { value, error -> value.copy(error = error ?: value.error) }
        .stateIn(scope, SharingStarted.Eagerly, CollectionUiState())

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun add(fullName: String, birthYear: String, address: String, organization: String): Boolean {
        val year = birthYear.toIntOrNull()
        if (fullName.isBlank() || year == null || address.isBlank() || organization.isBlank()) {
            errors.value = "Введите имя, корректный год рождения, адрес и организацию"
            return false
        }
        errors.value = null
        scope.launch {
            runCatchingCancellable { repository.save(Reader(ids.nextId(), fullName, year, address, organization)) }
                .onFailure { errors.value = it.message ?: "Не удалось сохранить читателя" }
        }
        return true
    }

    fun delete(reader: Reader) {
        scope.launch {
            runCatchingCancellable { service?.deleteReader(reader.id) ?: repository.delete(reader.id) }
                .onSuccess { errors.value = null }
                .onFailure { errors.value = it.message ?: "Не удалось удалить читателя" }
        }
    }
    override fun close() = scope.cancel()
}

class PublicationsViewModel(
    private val repository: PublicationRepository,
    private val branchesRepository: BranchRepository,
    private val ids: IdGenerator = UuidIdGenerator(),
    private val service: LibraryService? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : AutoCloseable {
    private val errors = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    val query: StateFlow<String> = searchQuery.asStateFlow()
    val state: StateFlow<CollectionUiState<Publication>> = combine(repository.publications, searchQuery) { publications, query ->
        val normalized = query.trim()
        CollectionUiState(
            items = publications.filter {
                normalized.isBlank() || it.title.contains(normalized, ignoreCase = true) || it.authors.contains(normalized, ignoreCase = true)
            },
            isLoading = false,
        )
    }
        .catch { emit(CollectionUiState(isLoading = false, error = it.message ?: "Не удалось загрузить издания")) }
        .combine(errors) { value, error -> value.copy(error = error ?: value.error) }
        .stateIn(scope, SharingStarted.Eagerly, CollectionUiState())

    val branches: StateFlow<CollectionUiState<LibraryBranch>> = branchesRepository.branches
        .map { CollectionUiState(items = it, isLoading = false) }
        .catch { emit(CollectionUiState(isLoading = false, error = it.message ?: "Не удалось загрузить филиалы")) }
        .stateIn(scope, SharingStarted.Eagerly, CollectionUiState())

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun addBook(title: String, authors: String, publisher: String, year: String, copies: String, branchId: String, isbn: String) {
        val parsedYear = year.toIntOrNull()
        val parsedCopies = copies.toIntOrNull()
        if (title.isBlank() || authors.isBlank() || publisher.isBlank() || parsedYear == null || parsedCopies == null || branchId.isBlank()) {
            errors.value = "Введите название, авторов, издателя, год, число экземпляров и филиал"
            return
        }
        errors.value = null
        scope.launch {
            runCatchingCancellable { repository.save(Book(ids.nextId(), title, authors, publisher, parsedYear, parsedCopies, parsedCopies, branchId, isbn.ifBlank { null })) }
                .onFailure { errors.value = it.message ?: "Не удалось сохранить издание" }
        }
    }

    fun addPeriodical(title: String, editors: String, publisher: String, year: String, issue: String, copies: String, branchId: String) {
        val parsedYear = year.toIntOrNull()
        val parsedIssue = issue.toIntOrNull()
        val parsedCopies = copies.toIntOrNull()
        if (title.isBlank() || editors.isBlank() || publisher.isBlank() || parsedYear == null || parsedIssue == null || parsedCopies == null || branchId.isBlank()) {
            errors.value = "Введите название, редакторов, издателя, год, номер выпуска, число экземпляров и филиал"
            return
        }
        errors.value = null
        scope.launch {
            runCatchingCancellable { repository.save(Periodical(ids.nextId(), title, editors, publisher, parsedYear, parsedCopies, parsedCopies, branchId, parsedIssue)) }
                .onFailure { errors.value = it.message ?: "Не удалось сохранить периодическое издание" }
        }
    }

    fun delete(publication: Publication) {
        scope.launch {
            runCatchingCancellable { service?.deletePublication(publication.id) ?: repository.delete(publication.id) }
                .onSuccess { errors.value = null }
                .onFailure { errors.value = it.message ?: "Не удалось удалить издание" }
        }
    }
    override fun close() = scope.cancel()
}

data class LoanRow(val loan: Loan, val readerName: String, val publicationTitle: String)

class LoansViewModel(
    private val service: LibraryService,
    private val loansRepository: LoanRepository,
    private val readersRepository: ReaderRepository,
    private val publicationsRepository: PublicationRepository,
    private val librariansRepository: LibrarianRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val overdueRefreshIntervalMillis: Long = 60_000L,
    private val currentTimeMillis: () -> Long = ::systemTimeMillis,
) : AutoCloseable {
    private val operationErrors = MutableStateFlow<String?>(null)
    private val refreshErrors = MutableStateFlow<String?>(null)
    val state: StateFlow<CollectionUiState<LoanRow>> = combine(
        loansRepository.loans,
        readersRepository.readers,
        publicationsRepository.publications,
    ) { loans, readers, publications ->
        val readerNames = readers.associateBy(Reader::id)
        val titles = publications.associateBy(Publication::id)
        CollectionUiState(
            items = loans.map { LoanRow(it, readerNames[it.readerId]?.fullName ?: "Неизвестный читатель", titles[it.publicationId]?.title ?: "Неизвестное издание") },
            isLoading = false,
        )
    }.catch { emit(CollectionUiState(isLoading = false, error = it.message ?: "Не удалось загрузить выдачи")) }
        .combine(refreshErrors) { value, error -> value.copy(error = error ?: value.error) }
        .combine(operationErrors) { value, error -> value.copy(error = error ?: value.error) }
        .stateIn(scope, SharingStarted.Eagerly, CollectionUiState())

    init {
        require(overdueRefreshIntervalMillis > 0L) { "Интервал обновления просроченных выдач должен быть положительным" }
        scope.launch {
            while (isActive) {
                delay(overdueRefreshIntervalMillis)
                runCatchingCancellable { loansRepository.refreshOverdue(currentTimeMillis()) }
                    .onSuccess { refreshErrors.value = null }
                    .onFailure { refreshErrors.value = it.message ?: "Не удалось обновить просроченные выдачи" }
            }
        }
    }

    fun issue(reader: Reader?, publication: Publication?) {
        if (reader == null || publication == null) {
            operationErrors.value = "Выберите читателя по имени и издание по названию"
            return
        }
        scope.launch {
            runCatchingCancellable {
                val librarian = librariansRepository.librarians.first().firstOrNull()
                    ?: error("Зарегистрируйте библиотекаря перед выдачей")
                @OptIn(kotlin.time.ExperimentalTime::class)
                val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
                service.issueLoan(reader.id, publication.id, librarian.id, now)
            }
                .onSuccess { operationErrors.value = null }
                .onFailure { operationErrors.value = it.message ?: "Не удалось оформить выдачу" }
        }
    }

    fun returnLoan(loan: Loan) {
        scope.launch {
            runCatchingCancellable {
                @OptIn(kotlin.time.ExperimentalTime::class)
                service.returnLoan(loan.id, kotlin.time.Clock.System.now().toEpochMilliseconds())
            }
                .onSuccess { operationErrors.value = null }
                .onFailure { operationErrors.value = it.message ?: "Не удалось оформить возврат" }
        }
    }
    override fun close() = scope.cancel()
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun systemTimeMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

data class AdministrationUiState(
    val isBusy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class AdministrationViewModel(
    private val service: LibraryService,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : AutoCloseable {
    private val _state = MutableStateFlow(AdministrationUiState())
    val state: StateFlow<AdministrationUiState> = _state.asStateFlow()

    fun seedDemo() {
        scope.launch {
            _state.value = AdministrationUiState(isBusy = true)
            try {
                runCatchingCancellable { service.seedDemo() }
                    .onSuccess { inserted ->
                        _state.value = AdministrationUiState(message = if (inserted) "Демонстрационные данные добавлены" else "Демонстрационные данные уже существуют")
                    }
                    .onFailure { _state.value = AdministrationUiState(error = it.message ?: "Не удалось добавить демонстрационные данные") }
            } finally {
                clearBusyAfterStandaloneCancellation()
            }
        }
    }

    fun clearAll() {
        scope.launch {
            _state.value = AdministrationUiState(isBusy = true)
            try {
                runCatchingCancellable { service.clearAll() }
                    .onSuccess { result -> _state.value = AdministrationUiState(message = "Удалено записей: ${result.totalDeleted}") }
                    .onFailure { _state.value = AdministrationUiState(error = it.message ?: "Не удалось очистить данные") }
            } finally {
                clearBusyAfterStandaloneCancellation()
            }
        }
    }

    private fun clearBusyAfterStandaloneCancellation() {
        if (scope.isActive && _state.value.isBusy) {
            _state.value = _state.value.copy(isBusy = false)
        }
    }

    override fun close() = scope.cancel()
}
