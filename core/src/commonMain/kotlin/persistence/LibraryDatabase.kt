package persistence

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [ReaderRecord::class, LibrarianRecord::class, BranchRecord::class, PublicationRecord::class, LoanRecord::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(LibraryDatabaseConstructor::class)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}

@Suppress("KotlinNoActualForExpect")
expect object LibraryDatabaseConstructor : RoomDatabaseConstructor<LibraryDatabase> {
    override fun initialize(): LibraryDatabase
}
