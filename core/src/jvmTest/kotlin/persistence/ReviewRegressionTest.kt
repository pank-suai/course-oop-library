package persistence

import application.RoomLibraryService
import domain.DefaultLoanPolicy
import domain.LibraryEntityFactory
import domain.Loan
import domain.LoanState
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ReviewRegressionTest {
    @Test
    fun concurrentIssuesRespectTheReaderActiveLoanLimit() = runBlocking {
        withDatabase { repositories, factory ->
            val branch = factory.branch("Central", "Address")
            val reader = factory.reader("Ada Lovelace", 1815, "Address", "Society")
            val librarian = factory.librarian("Grace Hopper", "Librarian", branch.id)
            val firstBook = factory.book("First", "Author", "Press", 2020, 1, branch.id)
            val secondBook = factory.book("Second", "Author", "Press", 2020, 1, branch.id)
            repositories.branches.save(branch)
            repositories.readers.save(reader)
            repositories.librarians.save(librarian)
            repositories.publications.save(firstBook)
            repositories.publications.save(secondBook)
            val service = RoomLibraryService(repositories, DefaultLoanPolicy(maxActiveLoansPerReader = 1))

            val results = listOf(firstBook, secondBook).map { book ->
                async(Dispatchers.Default) {
                    runCatching { service.issueLoan(reader.id, book.id, librarian.id, 1L) }
                }
            }.awaitAll()

            assertEquals(1, results.count { it.isSuccess })
            assertEquals(1, repositories.loans.activeCountForReader(reader.id))
            assertEquals(1, repositories.publications.publicationsOnce().sumOf { it.availableCopies })
        }
    }

    @Test
    fun savingAnExistingPublicationPreservesDatabaseInventoryAndReturnability() = runBlocking {
        withDatabase { repositories, factory ->
            val branch = factory.branch("Central", "Address")
            val reader = factory.reader("Ada Lovelace", 1815, "Address", "Society")
            val librarian = factory.librarian("Grace Hopper", "Librarian", branch.id)
            val book = factory.book("Computing", "Author", "Press", 2020, 2, branch.id)
            repositories.branches.save(branch)
            repositories.readers.save(reader)
            repositories.librarians.save(librarian)
            repositories.publications.save(book)
            val service = RoomLibraryService(repositories)
            val loan = service.issueLoan(reader.id, book.id, librarian.id, 1L)

            repositories.publications.save(
                book.copy(
                    title = "Updated computing",
                    totalCopies = 1,
                    availableCopies = 1,
                ),
            )

            val saved = repositories.publications.findById(book.id)!!
            assertEquals("Updated computing", saved.title)
            assertEquals(2, saved.totalCopies)
            assertEquals(1, saved.availableCopies)

            service.returnLoan(loan.id, loan.issuedAt + 1L)
            assertEquals(2, repositories.publications.findById(book.id)!!.availableCopies)
        }
    }

    @Test
    fun issueRejectsReturnedAndOverdueLoansWithoutChangingInventory() = runBlocking {
        withDatabase { repositories, factory ->
            val branch = factory.branch("Central", "Address")
            val reader = factory.reader("Ada Lovelace", 1815, "Address", "Society")
            val librarian = factory.librarian("Grace Hopper", "Librarian", branch.id)
            val book = factory.book("Computing", "Author", "Press", 2020, 1, branch.id)
            repositories.branches.save(branch)
            repositories.readers.save(reader)
            repositories.librarians.save(librarian)
            repositories.publications.save(book)
            val returned = Loan("returned", reader.id, book.id, librarian.id, 1L, 2L, LoanState.Returned(2L))
            val overdue = returned.copy(id = "overdue", state = LoanState.Overdue)

            assertFalse(repositories.loans.issue(returned))
            assertFalse(repositories.loans.issue(overdue))

            assertEquals(0, repositories.loans.loans.first().size)
            assertEquals(1, repositories.publications.findById(book.id)!!.availableCopies)
        }
    }

    @Test
    fun returnBeforeIssueTimestampIsRejectedAndLoanRemainsActive() = runBlocking {
        withDatabase { repositories, factory ->
            val branch = factory.branch("Central", "Address")
            val reader = factory.reader("Ada Lovelace", 1815, "Address", "Society")
            val librarian = factory.librarian("Grace Hopper", "Librarian", branch.id)
            val book = factory.book("Computing", "Author", "Press", 2020, 1, branch.id)
            repositories.branches.save(branch)
            repositories.readers.save(reader)
            repositories.librarians.save(librarian)
            repositories.publications.save(book)
            val service = RoomLibraryService(repositories)
            val loan = service.issueLoan(reader.id, book.id, librarian.id, 1_800_000_000_000L)

            assertFailsWith<IllegalArgumentException> { service.returnLoan(loan.id, 99L) }

            assertIs<LoanState.Active>(repositories.loans.findById(loan.id)!!.state)
            assertEquals(0, repositories.publications.findById(book.id)!!.availableCopies)
        }
    }

    @Test
    fun overdueRefreshTransitionsExpiredActiveLoans() = runBlocking {
        withDatabase { repositories, factory ->
            val branch = factory.branch("Central", "Address")
            val reader = factory.reader("Ada Lovelace", 1815, "Address", "Society")
            val librarian = factory.librarian("Grace Hopper", "Librarian", branch.id)
            val book = factory.book("Computing", "Author", "Press", 2020, 1, branch.id)
            repositories.branches.save(branch)
            repositories.readers.save(reader)
            repositories.librarians.save(librarian)
            repositories.publications.save(book)
            val service = RoomLibraryService(repositories)
            val loan = service.issueLoan(reader.id, book.id, librarian.id, 1L)

            repositories.loans.refreshOverdue(loan.dueAt + 1L)

            assertIs<LoanState.Overdue>(repositories.loans.findById(loan.id)!!.state)
            assertIs<LoanState.Overdue>(repositories.loans.loans.first().single().loanState())
        }
    }

    private suspend fun withDatabase(block: suspend (RoomRepositoryBundle, LibraryEntityFactory) -> Unit) {
        val directory = Files.createTempDirectory("course-oop-review-").toFile()
        val database = buildDatabase(databaseBuilder(directory.resolve("library.db")))
        try {
            block(RoomRepositoryBundle(database), LibraryEntityFactory())
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }

    private fun Loan.loanState(): LoanState = state
}
