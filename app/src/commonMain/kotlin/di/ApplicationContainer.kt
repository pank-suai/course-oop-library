package di

import application.RoomLibraryService
import persistence.LibraryDatabase
import persistence.RoomRepositoryBundle
import persistence.openDefaultDatabase

/** Composition root. All collaborators are explicit dependencies; there are no global repositories. */
class ApplicationContainer(
    database: LibraryDatabase = openDefaultDatabase(),
) : AutoCloseable {
    private val closeables = LinkedHashSet<AutoCloseable>()
    private var closed = false
    val repositories: RoomRepositoryBundle = RoomRepositoryBundle(database)
    val service: RoomLibraryService = RoomLibraryService(repositories)

    fun register(closeable: AutoCloseable) {
        if (closed) closeable.close() else closeables += closeable
    }

    override fun close() {
        if (closed) return
        closed = true
        closeables.toList().asReversed().forEach(AutoCloseable::close)
        closeables.clear()
        repositories.database.close()
    }
}
