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
import kotlinx.coroutines.launch
import persistence.BranchRepository
import persistence.LibrarianRepository
import persistence.LoanRepository
import persistence.PublicationRepository
import persistence.ReaderRepository


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
) {
    private val errors = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    val query: StateFlow<String> = searchQuery.asStateFlow()
    val state: StateFlow<CollectionUiState<Reader>> = combine(repository.readers, searchQuery) { readers, query ->
        CollectionUiState(
            items = readers.filter { it.fullName.contains(query.trim(), ignoreCase = true) },
            isLoading = false,
        )
    }
        .catch { emit(CollectionUiState(isLoading = false, error = it.message ?: "Unable to load readers")) }
        .combine(errors) { value, error -> value.copy(error = error ?: value.error) }
        .stateIn(scope, SharingStarted.Eagerly, CollectionUiState())

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun add(fullName: String, birthYear: String, address: String, organization: String): Boolean {
        val year = birthYear.toIntOrNull()
        if (fullName.isBlank() || year == null || address.isBlank() || organization.isBlank()) {
            errors.value = "Enter a name, valid birth year, address, and organization"
            return false
        }
        errors.value = null
        scope.launch {
            runCatching { repository.save(Reader(ids.nextId(), fullName, year, address, organization)) }
                .onFailure { errors.value = it.message ?: "Unable to save reader" }
        }
        return true
    }

    fun delete(reader: Reader) {
        scope.launch {
            runCatching { service?.deleteReader(reader.id) ?: repository.delete(reader.id) }
                .onSuccess { errors.value = null }
                .onFailure { errors.value = it.message ?: "Unable to delete reader" }
        }
    }
}

class PublicationsViewModel(
    private val repository: PublicationRepository,
    private val branchesRepository: BranchRepository,
    private val ids: IdGenerator = UuidIdGenerator(),
    private val service: LibraryService? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
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
        .catch { emit(CollectionUiState(isLoading = false, error = it.message ?: "Unable to load publications")) }
        .combine(errors) { value, error -> value.copy(error = error ?: value.error) }
        .stateIn(scope, SharingStarted.Eagerly, CollectionUiState())

    val branches: StateFlow<CollectionUiState<LibraryBranch>> = branchesRepository.branches
        .map { CollectionUiState(items = it, isLoading = false) }
        .catch { emit(CollectionUiState(isLoading = false, error = it.message ?: "Unable to load branches")) }
        .stateIn(scope, SharingStarted.Eagerly, CollectionUiState())

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun addBook(title: String, authors: String, publisher: String, year: String, copies: String, branchId: String, isbn: String) {
        val parsedYear = year.toIntOrNull()
        val parsedCopies = copies.toIntOrNull()
        if (title.isBlank() || authors.isBlank() || publisher.isBlank() || parsedYear == null || parsedCopies == null || branchId.isBlank()) {
            errors.value = "Enter title, authors, publisher, year, copies, and a branch"
            return
        }
        errors.value = null
        scope.launch {
            runCatching { repository.save(Book(ids.nextId(), title, authors, publisher, parsedYear, parsedCopies, parsedCopies, branchId, isbn.ifBlank { null })) }
                .onFailure { errors.value = it.message ?: "Unable to save publication" }
        }
    }

    fun addPeriodical(title: String, editors: String, publisher: String, year: String, issue: String, copies: String, branchId: String) {
        val parsedYear = year.toIntOrNull()
        val parsedIssue = issue.toIntOrNull()
        val parsedCopies = copies.toIntOrNull()
        if (title.isBlank() || editors.isBlank() || publisher.isBlank() || parsedYear == null || parsedIssue == null || parsedCopies == null || branchId.isBlank()) {
            errors.value = "Enter title, editors, publisher, year, issue, copies, and a branch"
            return
        }
        errors.value = null
        scope.launch {
            runCatching { repository.save(Periodical(ids.nextId(), title, editors, publisher, parsedYear, parsedCopies, parsedCopies, branchId, parsedIssue)) }
                .onFailure { errors.value = it.message ?: "Unable to save periodical" }
        }
    }

    fun delete(publication: Publication) {
        scope.launch {
            runCatching { service?.deletePublication(publication.id) ?: repository.delete(publication.id) }
                .onSuccess { errors.value = null }
                .onFailure { errors.value = it.message ?: "Unable to delete publication" }
        }
    }
}

data class LoanRow(val loan: Loan, val readerName: String, val publicationTitle: String)

class LoansViewModel(
    private val service: LibraryService,
    private val loansRepository: LoanRepository,
    private val readersRepository: ReaderRepository,
    private val publicationsRepository: PublicationRepository,
    private val librariansRepository: LibrarianRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val errors = MutableStateFlow<String?>(null)
    val state: StateFlow<CollectionUiState<LoanRow>> = combine(
        loansRepository.loans,
        readersRepository.readers,
        publicationsRepository.publications,
    ) { loans, readers, publications ->
        val readerNames = readers.associateBy(Reader::id)
        val titles = publications.associateBy(Publication::id)
        CollectionUiState(
            items = loans.map { LoanRow(it, readerNames[it.readerId]?.fullName ?: "Unknown reader", titles[it.publicationId]?.title ?: "Unknown title") },
            isLoading = false,
        )
    }.catch { emit(CollectionUiState(isLoading = false, error = it.message ?: "Unable to load loans")) }
        .combine(errors) { value, error -> value.copy(error = error ?: value.error) }
        .stateIn(scope, SharingStarted.Eagerly, CollectionUiState())

    fun issue(reader: Reader?, publication: Publication?) {
        if (reader == null || publication == null) {
            errors.value = "Select a reader by name and a publication by title"
            return
        }
        scope.launch {
            val librarian = librariansRepository.librarians.first().firstOrNull()
            if (librarian == null) {
                errors.value = "Register a librarian before issuing a loan"
                return@launch
            }
            runCatching {
                @OptIn(kotlin.time.ExperimentalTime::class)
                val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
                service.issueLoan(reader.id, publication.id, librarian.id, now)
            }.onFailure { errors.value = it.message ?: "Unable to issue loan" }
        }
    }

    fun returnLoan(loan: Loan) {
        scope.launch {
            runCatching {
                @OptIn(kotlin.time.ExperimentalTime::class)
                service.returnLoan(loan.id, kotlin.time.Clock.System.now().toEpochMilliseconds())
            }.onFailure { errors.value = it.message ?: "Unable to return loan" }
        }
    }
}

data class AdministrationUiState(
    val isBusy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class AdministrationViewModel(
    private val service: LibraryService,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _state = MutableStateFlow(AdministrationUiState())
    val state: StateFlow<AdministrationUiState> = _state.asStateFlow()

    fun seedDemo() {
        scope.launch {
            _state.value = AdministrationUiState(isBusy = true)
            runCatching { service.seedDemo() }
                .onSuccess { inserted ->
                    _state.value = AdministrationUiState(message = if (inserted) "Demo data seeded" else "Demo data already present")
                }
                .onFailure { _state.value = AdministrationUiState(error = it.message ?: "Unable to seed demo data") }
        }
    }

    fun clearAll() {
        scope.launch {
            _state.value = AdministrationUiState(isBusy = true)
            runCatching { service.clearAll() }
                .onSuccess { result -> _state.value = AdministrationUiState(message = "Cleared ${result.totalDeleted} records") }
                .onFailure { _state.value = AdministrationUiState(error = it.message ?: "Unable to clear data") }
        }
    }
}
