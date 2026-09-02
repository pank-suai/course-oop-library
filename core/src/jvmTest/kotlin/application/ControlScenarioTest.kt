package application

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import persistence.RoomRepositoryBundle
import persistence.buildDatabase
import persistence.databaseBuilder

class ControlScenarioTest {
    @Test
    fun demoSeedIsDeterministicAndIdempotent() = runBlocking {
        val directory = Files.createTempDirectory("course-oop-control-").toFile()
        val database = buildDatabase(databaseBuilder(directory.resolve("library.db")))
        try {
            val repositories = RoomRepositoryBundle(database)
            val service = RoomLibraryService(repositories)
            assertTrue(service.seedDemo())
            assertFalse(service.seedDemo())
            assertEquals(setOf("Анна Смирнова", "Алексей Петров"), repositories.readers.readersOnce().map { it.fullName }.toSet())
            assertEquals(setOf("Объектно-ориентированное программирование", "Библиотечное дело"), repositories.publications.publicationsOnce().map { it.title }.toSet())
            assertEquals("Центральная библиотека", repositories.branches.branches.first().single().name)
            assertEquals("Елена Волкова", repositories.librarians.librarians.first().single().fullName)
            assertEquals(1, repositories.loans.loans.first().size)
            assertEquals(1, repositories.publications.publicationsOnce().first { it.title == "Объектно-ориентированное программирование" }.availableCopies)
        } finally {
            database.close()
            directory.deleteRecursively()
        }
        Unit
    }
}
