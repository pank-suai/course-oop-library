package persistence

import application.RoomLibraryService
import domain.LibraryEntityFactory
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ManyToManyTest {
    @Test
    fun loansMaterializeManyReadersToOnePublicationRelationship() = runBlocking {
        val directory = Files.createTempDirectory("course-oop-many-to-many-").toFile()
        val database = buildDatabase(databaseBuilder(directory.resolve("library.db")))
        try {
            val repositories = RoomRepositoryBundle(database)
            val factory = LibraryEntityFactory()
            val branch = factory.branch("Central", "Address")
            val librarian = factory.librarian("Grace Hopper", "Librarian", branch.id)
            val firstReader = factory.reader("Ada Lovelace", 1815, "Address", "Society")
            val secondReader = factory.reader("Alan Turing", 1912, "Address", "Society")
            val book = factory.book("Computing", "A. Lovelace", "Press", 2020, 2, branch.id)
            repositories.branches.save(branch)
            repositories.librarians.save(librarian)
            repositories.readers.save(firstReader)
            repositories.readers.save(secondReader)
            repositories.publications.save(book)
            val service = RoomLibraryService(repositories)
            service.issueLoan(firstReader.id, book.id, librarian.id, 1L)
            service.issueLoan(secondReader.id, book.id, librarian.id, 2L)

            assertEquals(2, repositories.loans.loans.first().size)
            assertEquals(0, repositories.publications.findById(book.id)!!.availableCopies)
        } finally {
            database.close()
            directory.deleteRecursively()
        }
        Unit
    }
}
