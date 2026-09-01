package persistence

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "readers")
data class ReaderRecord(
    @PrimaryKey val id: String,
    val fullName: String,
    val birthYear: Int,
    val address: String,
    val organization: String,
)

@Entity(
    tableName = "librarians",
    foreignKeys = [ForeignKey(
        entity = BranchRecord::class,
        parentColumns = ["id"],
        childColumns = ["branchId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("branchId")],
)
data class LibrarianRecord(
    @PrimaryKey val id: String,
    val fullName: String,
    val position: String,
    val branchId: String,
)

@Entity(tableName = "branches")
data class BranchRecord(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
)

@Entity(
    tableName = "publications",
    foreignKeys = [ForeignKey(
        entity = BranchRecord::class,
        parentColumns = ["id"],
        childColumns = ["branchId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("branchId")],
)
data class PublicationRecord(
    @PrimaryKey val id: String,
    val kind: String,
    val title: String,
    val authors: String,
    val publisher: String,
    val publicationYear: Int,
    val isbn: String?,
    val issueNumber: Int?,
    val totalCopies: Int,
    val availableCopies: Int,
    val branchId: String,
)

@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(
            entity = ReaderRecord::class,
            parentColumns = ["id"],
            childColumns = ["readerId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = PublicationRecord::class,
            parentColumns = ["id"],
            childColumns = ["publicationId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = LibrarianRecord::class,
            parentColumns = ["id"],
            childColumns = ["librarianId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("readerId"), Index("publicationId"), Index("librarianId")],
)
data class LoanRecord(
    @PrimaryKey val id: String,
    val readerId: String,
    val publicationId: String,
    val librarianId: String,
    val issuedAt: Long,
    val dueAt: Long,
    val returnedAt: Long?,
    val status: String,
)

data class ClearAllCounts(
    val loans: Int,
    val publications: Int,
    val readers: Int,
    val librarians: Int,
    val branches: Int,
)
