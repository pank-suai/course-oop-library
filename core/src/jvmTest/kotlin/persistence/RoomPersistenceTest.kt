package persistence

import application.RoomLibraryService
import domain.DefaultLoanPolicy
import domain.LibraryEntityFactory
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class RoomPersistenceTest {
    @Test
    fun writeCloseReopenReadsTheSameDomainData() = runBlocking {
        val directory = Files.createTempDirectory("course-oop-room-").toFile()
        val databaseFile = directory.resolve("library.db")
        try {
            val first = buildDatabase(databaseBuilder(databaseFile))
            val repositories = RoomRepositoryBundle(first)
            val factory = LibraryEntityFactory()
            val branch = factory.branch("Central", "1 Library Way")
            val reader = factory.reader("Ada Lovelace", 1815, "London", "Society")
            val librarian = factory.librarian("Grace Hopper", "Librarian", branch.id)
            val book = factory.book("Computing", "A. Lovelace", "Press", 2020, 2, branch.id, "978-1")
            repositories.branches.save(branch)
            repositories.readers.save(reader)
            repositories.librarians.save(librarian)
            repositories.publications.save(book)
            assertEquals(reader, repositories.readers.findById(reader.id))
            first.close()

            assertTrue(databaseFile.isFile)
            assertTrue(databaseFile.length() > 0)
            val reopened = buildDatabase(databaseBuilder(databaseFile))
            try {
                val reopenedRepositories = RoomRepositoryBundle(reopened)
                assertEquals(listOf(reader), reopenedRepositories.readers.readersOnce())
                assertEquals(listOf(book), reopenedRepositories.publications.publicationsOnce())
            } finally {
                reopened.close()
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun serviceIssueAndReturnAreAtomicAndReturnIsIdempotent() = runBlocking {
        val directory = Files.createTempDirectory("course-oop-room-service-").toFile()
        val databaseFile = directory.resolve("library.db")
        try {
            val database = buildDatabase(databaseBuilder(databaseFile))
            try {
                val repositories = RoomRepositoryBundle(database)
                val factory = LibraryEntityFactory()
                val branch = factory.branch("Central", "Address")
                val reader = factory.reader("Ada Lovelace", 1815, "Address", "Society")
                val secondReader = factory.reader("Alan Turing", 1912, "Address", "Society")
                val librarian = factory.librarian("Grace Hopper", "Librarian", branch.id)
                val book = factory.book("Computing", "A. Lovelace", "Press", 2020, 1, branch.id, "978-1")
                repositories.branches.save(branch)
                repositories.readers.save(reader)
                repositories.librarians.save(librarian)
                repositories.publications.save(book)
                val service = RoomLibraryService(repositories, DefaultLoanPolicy())

                val loan = service.issueLoan(reader.id, book.id, librarian.id, 1_000L)
                assertEquals(0, repositories.publications.findById(book.id)!!.availableCopies)
                val returned = service.returnLoan(loan.id, 2_000L)
                val repeated = service.returnLoan(loan.id, 3_000L)
                assertEquals(returned, repeated)
                assertEquals(1, repositories.publications.findById(book.id)!!.availableCopies)
            } finally {
                database.close()
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
