package ui

import domain.Book
import domain.LibraryBranch
import domain.LibraryService
import domain.Loan
import domain.LoanState
import domain.Publication
import domain.Reader
import domain.ClearAllResult
import domain.Librarian
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import persistence.BranchRepository
import persistence.LibrarianRepository
import persistence.LoanRepository
import persistence.PublicationRepository
import persistence.ReaderRepository

class ViewModelLifecycleAndErrorTest {
    @Test fun pendingReturnIgnoresRepeatedSubmit() = runBlocking {
        val service = PendingService()
        val vm = LoansViewModel(service, EmptyLoans(), StaticReaders(), StaticPublications(), StaticLibrarians(), scope = testScope())
        try {
            val loan = Loan("loan", "reader", "book", "librarian", 0L, 1L)
            vm.returnLoan(loan)
            vm.returnLoan(loan)
            assertEquals(1, service.returnCalls)
        } finally { service.release.complete(Unit); vm.close() }
    }

    @Test fun pendingIssueIgnoresRepeatedSubmit() = runBlocking {
        val service = PendingService()
        val vm = LoansViewModel(service, EmptyLoans(), StaticReaders(), StaticPublications(), StaticLibrarians(), scope = testScope())
        try {
            val reader = Reader("reader", "Ada", 1815, "Address", "Org")
            vm.issue(reader, book())
            vm.issue(reader, book())
            assertEquals(1, service.issueCalls)
        } finally { service.release.complete(Unit); vm.close() }
    }

    @Test
    fun everyViewModelCloseCancelsItsEagerScope() {
        val readersScope = testScope()
        val publicationsScope = testScope()
        val loansScope = testScope()
        val administrationScope = testScope()
        ReadersViewModel(StaticReaders(), scope = readersScope).close()
        PublicationsViewModel(StaticPublications(), StaticBranches(), scope = publicationsScope).close()
        LoansViewModel(FailingService(), EmptyLoans(), StaticReaders(), StaticPublications(), FailingLibrarians(), scope = loansScope).close()
        AdministrationViewModel(FailingService(), scope = administrationScope).close()

        assertFalse(readersScope.coroutineContext[Job]!!.isActive)
        assertFalse(publicationsScope.coroutineContext[Job]!!.isActive)
        assertFalse(loansScope.coroutineContext[Job]!!.isActive)
        assertFalse(administrationScope.coroutineContext[Job]!!.isActive)
    }

    @Test
    fun readerOperationCancellationIsNotReportedAsError() {
        val repository = CancellingReaders()
        val vm = ReadersViewModel(repository, scope = testScope())

        vm.add("Ada", "1815", "Address", "Org")
        assertEquals(null, vm.state.value.error)

        vm.delete(repository.value)
        assertEquals(null, vm.state.value.error)
        vm.close()
    }

    @Test
    fun publicationOperationCancellationIsNotReportedAsError() {
        val repository = CancellingPublications()
        val vm = PublicationsViewModel(repository, StaticBranches(), scope = testScope())

        vm.addBook("Book", "Author", "Press", "2020", "1", "branch", "")
        assertEquals(null, vm.state.value.error)

        vm.addPeriodical("Journal", "Editor", "Press", "2020", "1", "1", "branch")
        assertEquals(null, vm.state.value.error)

        vm.delete(repository.value)
        assertEquals(null, vm.state.value.error)
        vm.close()
    }

    @Test
    fun librarianFlowFailureIsReportedByIssueState() = runBlocking {
        val vm = LoansViewModel(
            FailingService(),
            EmptyLoans(),
            StaticReaders(),
            StaticPublications(),
            FailingLibrarians(),
            scope = testScope(),
        )

        vm.issue(Reader("reader", "Ada", 1815, "Address", "Org"), book())

        assertEquals("Библиотекари недоступны", vm.state.first { it.error != null }.error)
    }

    @Test
    fun overdueLoansRefreshWhileViewModelRemainsOpen() = runBlocking {
        val loans = RefreshingLoans()
        val vm = LoansViewModel(
            FailingService(),
            loans,
            StaticReaders(),
            StaticPublications(),
            FailingLibrarians(),
            scope = testScope(),
            overdueRefreshIntervalMillis = 100L,
            currentTimeMillis = { 2L },
        )

        val row = withTimeout(1_000L) {
            vm.state.first { state -> state.items.firstOrNull()?.loan?.state == LoanState.Overdue }
        }.items.single()

        assertIs<LoanState.Overdue>(row.loan.state)
        assertEquals(1, loans.refreshCalls)
        vm.close()
    }

    @Test
    fun overdueRefreshCancellationIsNotReportedAsError() = runBlocking {
        val loans = CancellingRefreshLoans()
        val vm = LoansViewModel(
            FailingService(),
            loans,
            StaticReaders(),
            StaticPublications(),
            FailingLibrarians(),
            scope = testScope(),
            overdueRefreshIntervalMillis = 10L,
            currentTimeMillis = { 2L },
        )

        withTimeout(1_000L) {
            while (loans.refreshCalls == 0) delay(1L)
        }
        delay(10L)

        assertEquals(null, vm.state.value.error)
        assertEquals(1, loans.refreshCalls)
        vm.close()
    }

    @Test
    fun successfulOverdueRefreshClearsPreviousRefreshError() = runBlocking {
        val loans = RecoveringRefreshLoans()
        val vm = LoansViewModel(
            FailingService(),
            loans,
            StaticReaders(),
            StaticPublications(),
            FailingLibrarians(),
            scope = testScope(),
            overdueRefreshIntervalMillis = 20L,
            currentTimeMillis = { 2L },
        )

        assertEquals("Обновление не выполнено", withTimeout(1_000L) { vm.state.first { it.error != null } }.error)
        val recovered = withTimeout(1_000L) {
            vm.state.first { loans.refreshCalls >= 2 && it.error == null }
        }

        assertEquals(null, recovered.error)
        assertEquals(2, loans.refreshCalls)
        vm.close()
    }

    @Test
    fun successfulRefreshDoesNotClearOperationError() = runBlocking {
        val loans = SuccessfulRefreshLoans()
        val service = RecoveringService(issueFailuresRemaining = 1)
        val vm = LoansViewModel(
            service,
            loans,
            StaticReaders(),
            StaticPublications(),
            StaticLibrarians(),
            scope = testScope(),
            overdueRefreshIntervalMillis = 20L,
            currentTimeMillis = { 2L },
        )

        vm.issue(Reader("reader", "Ada", 1815, "Address", "Org"), book())
        assertEquals("Выдача не выполнена", withTimeout(1_000L) { vm.state.first { it.error != null } }.error)
        withTimeout(1_000L) {
            while (loans.refreshCalls == 0) delay(1L)
        }
        delay(10L)

        assertEquals("Выдача не выполнена", vm.state.value.error)
        vm.close()
    }

    @Test
    fun successfulOperationDoesNotClearRefreshError() = runBlocking {
        val loans = FailingThenSuspendingRefreshLoans()
        val service = RecoveringService()
        val vm = LoansViewModel(
            service,
            loans,
            StaticReaders(),
            StaticPublications(),
            StaticLibrarians(),
            scope = testScope(),
            overdueRefreshIntervalMillis = 20L,
            currentTimeMillis = { 2L },
        )

        assertEquals("Обновление не выполнено", withTimeout(1_000L) { vm.state.first { it.error != null } }.error)
        vm.issue(Reader("reader", "Ada", 1815, "Address", "Org"), book())

        assertEquals(1, service.issueCalls)
        assertEquals("Обновление не выполнено", vm.state.value.error)
        vm.close()
    }

    @Test
    fun successfulIssueClearsPreviousOperationError() = runBlocking {
        val service = RecoveringService(issueFailuresRemaining = 1)
        val vm = LoansViewModel(
            service,
            EmptyLoans(),
            StaticReaders(),
            StaticPublications(),
            StaticLibrarians(),
            scope = testScope(),
        )

        vm.issue(Reader("reader", "Ada", 1815, "Address", "Org"), book())
        assertEquals("Выдача не выполнена", withTimeout(1_000L) { vm.state.first { it.error != null } }.error)

        vm.issue(Reader("reader", "Ada", 1815, "Address", "Org"), book())
        val recovered = withTimeout(1_000L) { vm.state.first { !it.isLoading && it.error == null } }

        assertEquals(null, recovered.error)
        assertEquals(2, service.issueCalls)
        vm.close()
    }

    @Test
    fun successfulReturnClearsPreviousOperationError() = runBlocking {
        val service = RecoveringService(returnFailuresRemaining = 1)
        val vm = LoansViewModel(
            service,
            EmptyLoans(),
            StaticReaders(),
            StaticPublications(),
            StaticLibrarians(),
            scope = testScope(),
        )
        val loan = Loan("loan", "reader", "book", "librarian", 0L, 1L)

        vm.returnLoan(loan)
        assertEquals("Возврат не выполнен", withTimeout(1_000L) { vm.state.first { it.error != null } }.error)

        vm.returnLoan(loan)
        val recovered = withTimeout(1_000L) { vm.state.first { !it.isLoading && it.error == null } }

        assertEquals(null, recovered.error)
        assertEquals(2, service.returnCalls)
        vm.close()
    }

    @Test
    fun loanActionCancellationIsNotReportedAsError() {
        val service = CancellingService()
        val vm = LoansViewModel(
            service,
            EmptyLoans(),
            StaticReaders(),
            StaticPublications(),
            StaticLibrarians(),
            scope = testScope(),
        )

        vm.issue(Reader("reader", "Ada", 1815, "Address", "Org"), book())
        assertEquals(null, vm.state.value.error)

        vm.returnLoan(Loan("loan", "reader", "book", "librarian", 0L, 1L))
        assertEquals(null, vm.state.value.error)
        vm.close()
    }

    @Test
    fun administrationCancellationIsNotReportedAsError() {
        val vm = AdministrationViewModel(CancellingService(), scope = testScope())

        vm.seedDemo()
        assertEquals(null, vm.state.value.error)
        assertFalse(vm.state.value.isBusy)

        vm.clearAll()
        assertEquals(null, vm.state.value.error)
        assertFalse(vm.state.value.isBusy)
        vm.close()
    }
}

private fun testScope() = CoroutineScope(Dispatchers.Unconfined + Job())
private fun book() = Book("book", "Book", "Author", "Press", 2020, 1, 1, "branch", null)

private class StaticReaders : ReaderRepository {
    private val value = Reader("reader", "Ada", 1815, "Address", "Org")
    override val readers: Flow<List<Reader>> = MutableStateFlow(listOf(value))
    override suspend fun readersOnce() = listOf(value)
    override suspend fun findById(id: String) = value.takeIf { it.id == id }
    override suspend fun save(reader: Reader) = Unit
    override suspend fun delete(id: String) = Unit
}

private class CancellingReaders : ReaderRepository {
    val value = Reader("reader", "Ada", 1815, "Address", "Org")
    override val readers: Flow<List<Reader>> = MutableStateFlow(listOf(value))
    override suspend fun readersOnce() = listOf(value)
    override suspend fun findById(id: String) = value.takeIf { it.id == id }
    override suspend fun save(reader: Reader): Unit = throw CancellationException("reader save cancelled")
    override suspend fun delete(id: String): Unit = throw CancellationException("reader delete cancelled")
}

private class StaticPublications : PublicationRepository {
    private val value = book()
    override val publications: Flow<List<Publication>> = MutableStateFlow(listOf(value))
    override suspend fun publicationsOnce() = listOf(value)
    override suspend fun findById(id: String) = value.takeIf { it.id == id }
    override suspend fun save(publication: Publication) = Unit
    override suspend fun delete(id: String) = Unit
}

private class CancellingPublications : PublicationRepository {
    val value = book()
    override val publications: Flow<List<Publication>> = MutableStateFlow(listOf(value))
    override suspend fun publicationsOnce() = listOf(value)
    override suspend fun findById(id: String) = value.takeIf { it.id == id }
    override suspend fun save(publication: Publication): Unit = throw CancellationException("publication save cancelled")
    override suspend fun delete(id: String): Unit = throw CancellationException("publication delete cancelled")
}

private class StaticBranches : BranchRepository {
    override val branches: Flow<List<LibraryBranch>> = emptyFlow()
    override suspend fun findById(id: String): LibraryBranch? = null
    override suspend fun save(branch: LibraryBranch) = Unit
}

private class EmptyLoans : LoanRepository {
    override val loans: Flow<List<Loan>> = MutableStateFlow(emptyList())
    override suspend fun findById(id: String): Loan? = null
    override suspend fun issue(loan: Loan) = false
    override suspend fun returnLoan(id: String, returnedAt: Long): Loan? = null
    override suspend fun activeCountForReader(readerId: String) = 0
}

private class RefreshingLoans : LoanRepository {
    private val values = MutableStateFlow(
        listOf(Loan("loan", "reader", "book", "librarian", 0L, 1L, LoanState.Active)),
    )
    var refreshCalls = 0
        private set

    override val loans: Flow<List<Loan>> = values
    override suspend fun findById(id: String): Loan? = values.value.firstOrNull { it.id == id }
    override suspend fun issue(loan: Loan) = false
    override suspend fun returnLoan(id: String, returnedAt: Long): Loan? = null
    override suspend fun activeCountForReader(readerId: String) = 0
    override suspend fun refreshOverdue(now: Long): Int {
        refreshCalls += 1
        values.value = values.value.map { current ->
            if (current.state == LoanState.Active && current.dueAt < now) current.copy(state = LoanState.Overdue) else current
        }
        return 1
    }
}

private class CancellingRefreshLoans : LoanRepository {
    override val loans: Flow<List<Loan>> = MutableStateFlow(emptyList())
    var refreshCalls = 0
        private set

    override suspend fun findById(id: String): Loan? = null
    override suspend fun issue(loan: Loan) = false
    override suspend fun returnLoan(id: String, returnedAt: Long): Loan? = null
    override suspend fun activeCountForReader(readerId: String) = 0
    override suspend fun refreshOverdue(now: Long): Int {
        refreshCalls += 1
        throw CancellationException("refresh cancelled")
    }
}

private class RecoveringRefreshLoans : LoanRepository {
    override val loans: Flow<List<Loan>> = MutableStateFlow(emptyList())
    var refreshCalls = 0
        private set

    override suspend fun findById(id: String): Loan? = null
    override suspend fun issue(loan: Loan) = false
    override suspend fun returnLoan(id: String, returnedAt: Long): Loan? = null
    override suspend fun activeCountForReader(readerId: String) = 0
    override suspend fun refreshOverdue(now: Long): Int {
        refreshCalls += 1
        if (refreshCalls == 1) error("Обновление не выполнено")
        return 0
    }
}

private class SuccessfulRefreshLoans : LoanRepository {
    override val loans: Flow<List<Loan>> = MutableStateFlow(emptyList())
    var refreshCalls = 0
        private set

    override suspend fun findById(id: String): Loan? = null
    override suspend fun issue(loan: Loan) = false
    override suspend fun returnLoan(id: String, returnedAt: Long): Loan? = null
    override suspend fun activeCountForReader(readerId: String) = 0
    override suspend fun refreshOverdue(now: Long): Int {
        refreshCalls += 1
        return 0
    }
}

private class FailingThenSuspendingRefreshLoans : LoanRepository {
    override val loans: Flow<List<Loan>> = MutableStateFlow(emptyList())
    private var refreshCalls = 0

    override suspend fun findById(id: String): Loan? = null
    override suspend fun issue(loan: Loan) = false
    override suspend fun returnLoan(id: String, returnedAt: Long): Loan? = null
    override suspend fun activeCountForReader(readerId: String) = 0
    override suspend fun refreshOverdue(now: Long): Int {
        refreshCalls += 1
        if (refreshCalls == 1) error("Обновление не выполнено")
        return awaitCancellation()
    }
}

private class StaticLibrarians : LibrarianRepository {
    private val value = Librarian("librarian", "Мария Иванова", "Библиотекарь", "branch")
    override val librarians: Flow<List<Librarian>> = MutableStateFlow(listOf(value))
    override suspend fun findById(id: String): Librarian? = value.takeIf { it.id == id }
    override suspend fun save(librarian: Librarian) = Unit
}

private class FailingLibrarians : LibrarianRepository {
    override val librarians: Flow<List<Librarian>> = flow { error("Библиотекари недоступны") }
    override suspend fun findById(id: String): Librarian? = null
    override suspend fun save(librarian: Librarian) = Unit
}

private class CancellingService : LibraryService {
    override suspend fun issueLoan(readerId: String, publicationId: String, librarianId: String, now: Long): Loan =
        throw CancellationException("issue cancelled")

    override suspend fun returnLoan(loanId: String, now: Long): Loan =
        throw CancellationException("return cancelled")

    override suspend fun deleteReader(readerId: String): Unit = throw CancellationException("reader delete cancelled")
    override suspend fun deletePublication(publicationId: String): Unit = throw CancellationException("publication delete cancelled")
    override suspend fun seedDemo(): Boolean = throw CancellationException("seed cancelled")
    override suspend fun clearAll(): ClearAllResult = throw CancellationException("clear cancelled")
}

private class RecoveringService(
    private var issueFailuresRemaining: Int = 0,
    private var returnFailuresRemaining: Int = 0,
) : LibraryService {
    var issueCalls = 0
        private set
    var returnCalls = 0
        private set

    override suspend fun issueLoan(readerId: String, publicationId: String, librarianId: String, now: Long): Loan {
        issueCalls += 1
        if (issueFailuresRemaining-- > 0) error("Выдача не выполнена")
        return Loan("loan-$issueCalls", readerId, publicationId, librarianId, now, now + 1L)
    }

    override suspend fun returnLoan(loanId: String, now: Long): Loan {
        returnCalls += 1
        if (returnFailuresRemaining-- > 0) error("Возврат не выполнен")
        return Loan(loanId, "reader", "book", "librarian", 0L, 1L, LoanState.Returned(now))
    }

    override suspend fun deleteReader(readerId: String) = Unit
    override suspend fun deletePublication(publicationId: String) = Unit
    override suspend fun seedDemo() = false
    override suspend fun clearAll() = ClearAllResult(0, 0, 0, 0, 0)
}

private class PendingService : LibraryService by RecoveringService() {
    val release = kotlinx.coroutines.CompletableDeferred<Unit>()
    var issueCalls = 0
    var returnCalls = 0
    override suspend fun issueLoan(readerId: String, publicationId: String, librarianId: String, now: Long): Loan {
        issueCalls++
        release.await()
        return Loan("loan", readerId, publicationId, librarianId, now, now + 1L)
    }
    override suspend fun returnLoan(loanId: String, now: Long): Loan {
        returnCalls++
        release.await()
        return Loan(loanId, "reader", "book", "librarian", 0L, 1L, LoanState.Returned(now))
    }
}

private class FailingService : LibraryService {
    override suspend fun issueLoan(readerId: String, publicationId: String, librarianId: String, now: Long): Loan = error("unused")
    override suspend fun returnLoan(loanId: String, now: Long): Loan = error("unused")
    override suspend fun deleteReader(readerId: String) = Unit
    override suspend fun deletePublication(publicationId: String) = Unit
    override suspend fun seedDemo() = false
    override suspend fun clearAll() = ClearAllResult(0, 0, 0, 0, 0)
}
