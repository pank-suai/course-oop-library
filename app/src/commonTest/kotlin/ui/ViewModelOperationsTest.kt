package ui

import domain.Book
import domain.DefaultLoanPolicy
import domain.LibraryBranch
import domain.LibraryService
import domain.Loan
import domain.LoanState
import domain.Periodical
import domain.Publication
import domain.Reader
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import persistence.ActiveLoanDeletionException
import persistence.BranchRepository
import persistence.LibrarianRepository
import persistence.LoanRepository
import persistence.PublicationRepository
import persistence.ReaderRepository

class ViewModelOperationsTest {
    @Test
    fun readerSearchFiltersByHumanReadableName() = runBlocking {
        val repository = FakeReaderRepository(
            listOf(
                Reader("reader-ada", "Ada Lovelace", 1815, "London", "Society"),
                Reader("reader-alan", "Alan Turing", 1912, "Manchester", "Society"),
            ),
        )
        val vm = ReadersViewModel(repository, scope = testScope())

        vm.setSearchQuery("lovelace")

        assertEquals(listOf("Ada Lovelace"), vm.state.first { !it.isLoading && it.items.size == 1 }.items.map { it.fullName })
    }

    @Test
    fun publicationSearchFiltersByTitleOrAuthor() = runBlocking {
        val repository = FakePublicationRepository(
            listOf(
                Book("book-1", "Computing", "Ada Lovelace", "Press", 2020, 1, 1, "branch-1", null),
                Periodical("periodical-1", "Library Review", "Editorial Board", "Press", 2025, 1, 1, "branch-1", 1),
            ),
        )
        val vm = PublicationsViewModel(repository, FakeBranchRepository(), scope = testScope())

        vm.setSearchQuery("editorial")

        assertEquals(listOf("Library Review"), vm.state.first { !it.isLoading && it.items.size == 1 }.items.map { it.title })
    }

    @Test
    fun readerDeleteUsesServiceAndReportsSuccess() = runBlocking {
        val repository = FakeReaderRepository(listOf(Reader("reader-ada", "Ada Lovelace", 1815, "London", "Society")))
        val service = FakeLibraryService()
        val vm = ReadersViewModel(repository, service = service, scope = testScope())

        vm.delete(repository.values.value.single())
        waitUntil { service.deletedReaderIds == listOf("reader-ada") }

        assertTrue(service.deletedReaderIds.contains("reader-ada"))
    }

    @Test
    fun readerDeleteSurfacesActiveLoanError() = runBlocking {
        val repository = FakeReaderRepository(listOf(Reader("reader-ada", "Ada Lovelace", 1815, "London", "Society")))
        val service = FakeLibraryService().also { it.readerDeletionError = ActiveLoanDeletionException("Cannot delete reader: active loans exist") }
        val vm = ReadersViewModel(repository, service = service, scope = testScope())

        vm.delete(repository.values.value.single())
        val state = vm.state.first { it.error != null }

        assertEquals("Cannot delete reader: active loans exist", state.error)
    }

    private suspend fun waitUntil(predicate: () -> Boolean) {
        repeat(100) {
            if (predicate()) return
            kotlinx.coroutines.delay(5)
        }
        error("condition was not reached")
    }
}

private fun testScope() = CoroutineScope(Dispatchers.Unconfined + Job())

private class FakeReaderRepository(initial: List<Reader>) : ReaderRepository {
    val values = MutableStateFlow(initial)
    override val readers: Flow<List<Reader>> = values
    override suspend fun readersOnce(): List<Reader> = values.value
    override suspend fun findById(id: String): Reader? = values.value.firstOrNull { it.id == id }
    override suspend fun save(reader: Reader) { values.value = values.value.filterNot { it.id == reader.id } + reader }
    override suspend fun delete(id: String) { values.value = values.value.filterNot { it.id == id } }
}

private class FakePublicationRepository(initial: List<Publication>) : PublicationRepository {
    private val values = MutableStateFlow(initial)
    override val publications: Flow<List<Publication>> = values
    override suspend fun publicationsOnce(): List<Publication> = values.value
    override suspend fun findById(id: String): Publication? = values.value.firstOrNull { it.id == id }
    override suspend fun save(publication: Publication) { values.value = values.value.filterNot { it.id == publication.id } + publication }
    override suspend fun delete(id: String) { values.value = values.value.filterNot { it.id == id } }
}

private class FakeBranchRepository : BranchRepository {
    private val values = MutableStateFlow<List<LibraryBranch>>(emptyList())
    override val branches: Flow<List<LibraryBranch>> = values
    override suspend fun findById(id: String): LibraryBranch? = values.value.firstOrNull { it.id == id }
    override suspend fun save(branch: LibraryBranch) { values.value = values.value + branch }
}

private class FakeLibraryService : LibraryService {
    val deletedReaderIds = CopyOnWriteArrayList<String>()
    var readerDeletionError: Throwable? = null
    override suspend fun issueLoan(readerId: String, publicationId: String, librarianId: String, now: Long): Loan = error("unused")
    override suspend fun returnLoan(loanId: String, now: Long): Loan = error("unused")
    override suspend fun deleteReader(readerId: String) {
        readerDeletionError?.let { throw it }
        deletedReaderIds += readerId
    }
    override suspend fun deletePublication(publicationId: String) = Unit
    override suspend fun seedDemo(): Boolean = false
    override suspend fun clearAll() = error("unused")
}
