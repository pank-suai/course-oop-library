package di

import application.RoomLibraryService
import persistence.LibraryDatabase
import persistence.RoomRepositoryBundle
import persistence.openDefaultDatabase

/** Composition root. All collaborators are explicit dependencies; there are no global repositories. */
class ApplicationContainer(
    database: LibraryDatabase = openDefaultDatabase(),
) : AutoCloseable {
    val repositories: RoomRepositoryBundle = RoomRepositoryBundle(database)
    val service: RoomLibraryService = RoomLibraryService(repositories)

    override fun close() {
        repositories.database.close()
    }
}
