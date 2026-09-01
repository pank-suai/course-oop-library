package application

import domain.LibraryEntityFactory
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import persistence.RoomRepositoryBundle
import persistence.buildDatabase
import persistence.databaseBuilder

class RoomAdministrationTest {
    @Test
    fun serviceDeletesUnloanedReaderAndPublication() = runBlocking {
        val directory = Files.createTempDirectory("course-oop-delete-").toFile()
        val database = buildDatabase(databaseBuilder(directory.resolve("library.db")))
        try {
            val repositories = RoomRepositoryBundle(database)
            val factory = LibraryEntityFactory()
            val branch = factory.branch("Central", "Address")
            val reader = factory.reader("Ada Lovelace", 1815, "Address", "Society")
            val librarian = factory.librarian("Grace Hopper", "Librarian", branch.id)
            val book = factory.book("Computing", "A. Lovelace", "Press", 2020, 1, branch.id)
            repositories.branches.save(branch)
            repositories.librarians.save(librarian)
            repositories.readers.save(reader)
            repositories.publications.save(book)
            val service = RoomLibraryService(repositories)

            service.deleteReader(reader.id)
            service.deletePublication(book.id)

            assertNull(repositories.readers.findById(reader.id))
            assertNull(repositories.publications.findById(book.id))
        } finally {
            database.close()
            directory.deleteRecursively()
        }
        Unit
    }

    @Test
    fun clearAllReturnsDeletedCountsAndLeavesEveryTableEmpty() = runBlocking {
        val directory = Files.createTempDirectory("course-oop-clear-").toFile()
        val database = buildDatabase(databaseBuilder(directory.resolve("library.db")))
        try {
            val repositories = RoomRepositoryBundle(database)
            val service = RoomLibraryService(repositories)
            assertTrue(service.seedDemo())

            val result = service.clearAll()

            assertEquals(7, result.totalDeleted)
            assertTrue(repositories.readers.readersOnce().isEmpty())
            assertTrue(repositories.publications.publicationsOnce().isEmpty())
            assertTrue(repositories.librarians.librarians.first().isEmpty())
            assertTrue(repositories.branches.branches.first().isEmpty())
            assertTrue(repositories.loans.loans.first().isEmpty())
        } finally {
            database.close()
            directory.deleteRecursively()
        }
        Unit
    }
}
