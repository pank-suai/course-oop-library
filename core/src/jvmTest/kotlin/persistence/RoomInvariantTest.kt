package persistence

import application.RoomLibraryService
import domain.DefaultLoanPolicy
import domain.LibraryEntityFactory
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking

class RoomInvariantTest {
    @Test
    fun activeLoansRejectReaderAndPublicationDeletion() = runBlocking {
        val directory = Files.createTempDirectory("course-oop-invariants-").toFile()
        val database = buildDatabase(databaseBuilder(directory.resolve("library.db")))
        try {
            val repositories = RoomRepositoryBundle(database)
            val factory = LibraryEntityFactory()
            val branch = factory.branch("Central", "Address")
            val reader = factory.reader("Ada Lovelace", 1815, "Address", "Society")
            val librarian = factory.librarian("Grace Hopper", "Librarian", branch.id)
            val book = factory.book("Computing", "A. Lovelace", "Press", 2020, 1, branch.id)
            repositories.branches.save(branch)
            repositories.readers.save(reader)
            repositories.librarians.save(librarian)
            repositories.publications.save(book)
            val service = RoomLibraryService(repositories)
            service.issueLoan(reader.id, book.id, librarian.id, 1L)

            assertFailsWith<ActiveLoanDeletionException> { repositories.readers.delete(reader.id) }
            assertFailsWith<ActiveLoanDeletionException> { repositories.publications.delete(book.id) }
        } finally {
            database.close()
            directory.deleteRecursively()
        }
        Unit
    }

    @Test
    fun failedDemoIssueRollsBackTheEntireSeedTransaction() = runBlocking {
        val directory = Files.createTempDirectory("course-oop-rollback-").toFile()
        val database = buildDatabase(databaseBuilder(directory.resolve("library.db")))
        try {
            val repositories = RoomRepositoryBundle(database)
            val factory = LibraryEntityFactory()
            val branch = factory.branch("Central", "Address")
            val librarian = factory.librarian("Grace Hopper", "Librarian", branch.id)
            val reader = factory.reader("Ada Lovelace", 1815, "Address", "Society")
            val book = factory.book("Computing", "A. Lovelace", "Press", 2020, 1, branch.id)
            val impossibleLoan = factory.loan(reader.id, book.id, librarian.id, 1L, 2L)

            assertFailsWith<IllegalStateException> {
                repositories.insertDemoData(
                    BranchRecord(branch.id, branch.name, branch.address),
                    LibrarianRecord(librarian.id, librarian.fullName, librarian.position, librarian.branchId),
                    listOf(ReaderRecord(reader.id, reader.fullName, reader.birthYear, reader.address, reader.organization)),
                    listOf(PublicationRecord(book.id, "BOOK", book.title, book.authors, book.publisher, book.publicationYear, null, null, 1, 0, branch.id)),
                    LoanRecord(impossibleLoan.id, reader.id, book.id, librarian.id, 1L, 2L, null, "ACTIVE"),
                )
            }
            assertEquals(emptyList(), repositories.readers.readersOnce())
            assertEquals(emptyList(), repositories.publications.publicationsOnce())
        } finally {
            database.close()
            directory.deleteRecursively()
        }
        Unit
    }
}
