package persistence

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

fun databaseBuilder(databaseFile: File): RoomDatabase.Builder<LibraryDatabase> =
    Room.databaseBuilder<LibraryDatabase>(name = databaseFile.absolutePath)

fun defaultDatabaseFile(): File {
    val home = System.getProperty("user.home")
    val os = System.getProperty("os.name").lowercase()
    val path = when {
        os.contains("mac") -> File(home, "Library/Application Support/CourseOopLibrary/library.db")
        os.contains("win") -> File(
            System.getenv("LOCALAPPDATA") ?: File(home, "AppData/Local").absolutePath,
            "CourseOopLibrary/library.db",
        )
        else -> File(home, ".local/share/course-oop-library/library.db")
    }
    path.parentFile.mkdirs()
    return path
}

fun openDefaultDatabase(): LibraryDatabase = buildDatabase(databaseBuilder(defaultDatabaseFile()))
