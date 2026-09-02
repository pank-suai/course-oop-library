package persistence

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM readers ORDER BY fullName")
    fun observeReaders(): Flow<List<ReaderRecord>>

    @Query("SELECT * FROM readers ORDER BY fullName")
    suspend fun getReaders(): List<ReaderRecord>

    @Query("SELECT * FROM readers WHERE id = :id")
    suspend fun getReader(id: String): ReaderRecord?

    @Upsert
    suspend fun saveReader(record: ReaderRecord)

    @Query("DELETE FROM readers WHERE id = :id")
    suspend fun deleteReader(id: String): Int

    @Query("SELECT COUNT(*) FROM loans WHERE readerId = :id AND returnedAt IS NULL")
    suspend fun countActiveLoansForReader(id: String): Int

    @Query("SELECT COUNT(*) FROM loans WHERE readerId = :id")
    suspend fun countLoansForReader(id: String): Int

    @Transaction
    suspend fun deleteReaderIfUnused(id: String): Int {
        if (getReader(id) == null) return -1
        if (countLoansForReader(id) > 0) return 0
        return deleteReader(id)
    }

    @Query("SELECT * FROM librarians ORDER BY fullName")
    fun observeLibrarians(): Flow<List<LibrarianRecord>>

    @Query("SELECT * FROM librarians ORDER BY fullName")
    suspend fun getLibrarians(): List<LibrarianRecord>

    @Query("SELECT * FROM librarians WHERE id = :id")
    suspend fun getLibrarian(id: String): LibrarianRecord?

    @Upsert
    suspend fun saveLibrarian(record: LibrarianRecord)

    @Query("SELECT * FROM branches ORDER BY name")
    fun observeBranches(): Flow<List<BranchRecord>>

    @Query("SELECT * FROM branches ORDER BY name")
    suspend fun getBranches(): List<BranchRecord>

    @Query("SELECT COUNT(*) FROM branches")
    suspend fun countBranches(): Int

    @Upsert
    suspend fun saveBranch(record: BranchRecord)

    @Query("SELECT * FROM publications ORDER BY title")
    fun observePublications(): Flow<List<PublicationRecord>>

    @Query("SELECT * FROM publications ORDER BY title")
    suspend fun getPublications(): List<PublicationRecord>

    @Query("SELECT * FROM publications WHERE id = :id")
    suspend fun getPublication(id: String): PublicationRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPublicationIfAbsent(record: PublicationRecord): Long

    @Query("""
        UPDATE publications SET
            kind = :kind,
            title = :title,
            authors = :authors,
            publisher = :publisher,
            publicationYear = :publicationYear,
            isbn = :isbn,
            issueNumber = :issueNumber,
            branchId = :branchId
        WHERE id = :id
    """)
    suspend fun updatePublicationMetadata(
        id: String,
        kind: String,
        title: String,
        authors: String,
        publisher: String,
        publicationYear: Int,
        isbn: String?,
        issueNumber: Int?,
        branchId: String,
    ): Int

    @Transaction
    suspend fun savePublication(record: PublicationRecord) {
        if (insertPublicationIfAbsent(record) == -1L) {
            updatePublicationMetadata(
                record.id,
                record.kind,
                record.title,
                record.authors,
                record.publisher,
                record.publicationYear,
                record.isbn,
                record.issueNumber,
                record.branchId,
            )
        }
    }

    @Query("UPDATE publications SET availableCopies = availableCopies - 1 WHERE id = :publicationId AND availableCopies > 0")
    suspend fun decrementAvailable(publicationId: String): Int

    @Query("UPDATE publications SET availableCopies = availableCopies + 1 WHERE id = :publicationId AND availableCopies < totalCopies")
    suspend fun incrementAvailable(publicationId: String): Int

    @Query("DELETE FROM publications WHERE id = :id")
    suspend fun deletePublication(id: String): Int

    @Query("SELECT COUNT(*) FROM loans WHERE publicationId = :id AND returnedAt IS NULL")
    suspend fun countActiveLoansForPublication(id: String): Int

    @Query("SELECT COUNT(*) FROM loans WHERE publicationId = :id")
    suspend fun countLoansForPublication(id: String): Int

    @Transaction
    suspend fun deletePublicationIfUnused(id: String): Int {
        if (getPublication(id) == null) return -1
        if (countLoansForPublication(id) > 0) return 0
        return deletePublication(id)
    }

    @Query("SELECT * FROM loans ORDER BY issuedAt DESC")
    fun observeLoans(): Flow<List<LoanRecord>>

    @Query("SELECT * FROM loans ORDER BY issuedAt DESC")
    suspend fun getLoans(): List<LoanRecord>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoan(id: String): LoanRecord?

    @Query("SELECT COUNT(*) FROM loans WHERE readerId = :readerId AND returnedAt IS NULL")
    suspend fun countActiveLoans(readerId: String): Int

    @Query("UPDATE loans SET status = 'OVERDUE' WHERE status = 'ACTIVE' AND returnedAt IS NULL AND dueAt < :now")
    suspend fun markOverdueLoans(now: Long): Int

    @Insert
    suspend fun insertLoan(record: LoanRecord)

    @Query("UPDATE loans SET returnedAt = :returnedAt, status = 'RETURNED' WHERE id = :loanId AND returnedAt IS NULL")
    suspend fun markReturned(loanId: String, returnedAt: Long): Int

    @Transaction
    suspend fun issueLoanAtomically(record: LoanRecord, maxActiveLoansPerReader: Int): Boolean {
        if (record.status != "ACTIVE" || record.returnedAt != null) return false
        if (countActiveLoans(record.readerId) >= maxActiveLoansPerReader) return false
        if (decrementAvailable(record.publicationId) != 1) return false
        insertLoan(record)
        return true
    }

    @Transaction
    suspend fun issueLoanAtomically(record: LoanRecord): Boolean =
        issueLoanAtomically(record, Int.MAX_VALUE)

    @Transaction
    suspend fun returnLoanAtomically(loanId: String, returnedAt: Long): LoanRecord? {
        val current = getLoan(loanId) ?: return null
        require(returnedAt >= current.issuedAt) { "Дата возврата не может быть раньше даты выдачи" }
        if (current.returnedAt != null) return current
        if (markReturned(loanId, returnedAt) != 1) return getLoan(loanId)
        if (incrementAvailable(current.publicationId) != 1) {
            error("Нарушен инвариант складского остатка издания")
        }
        return getLoan(loanId)
    }

    @Query("SELECT COUNT(*) FROM loans")
    suspend fun countLoans(): Int

    @Query("SELECT COUNT(*) FROM publications")
    suspend fun countPublications(): Int

    @Query("SELECT COUNT(*) FROM readers")
    suspend fun countReaders(): Int

    @Query("SELECT COUNT(*) FROM librarians")
    suspend fun countLibrarians(): Int

    @Query("DELETE FROM loans")
    suspend fun deleteAllLoans(): Int

    @Query("DELETE FROM publications")
    suspend fun deleteAllPublications(): Int

    @Query("DELETE FROM readers")
    suspend fun deleteAllReaders(): Int

    @Query("DELETE FROM librarians")
    suspend fun deleteAllLibrarians(): Int

    @Query("DELETE FROM branches")
    suspend fun deleteAllBranches(): Int

    @Transaction
    suspend fun clearAllData(): ClearAllCounts {
        val counts = ClearAllCounts(countLoans(), countPublications(), countReaders(), countLibrarians(), countBranches())
        // Loans must be removed first because all three entity relationships are RESTRICT.
        deleteAllLoans()
        deleteAllPublications()
        deleteAllReaders()
        deleteAllLibrarians()
        deleteAllBranches()
        return counts
    }

    @Transaction
    suspend fun insertDemoData(
        branch: BranchRecord,
        librarian: LibrarianRecord,
        readers: List<ReaderRecord>,
        publications: List<PublicationRecord>,
        loan: LoanRecord,
    ): Boolean {
        if (countBranches() > 0) return false
        saveBranch(branch)
        saveLibrarian(librarian)
        readers.forEach { saveReader(it) }
        publications.forEach { savePublication(it) }
        if (!issueLoanAtomically(loan)) error("Не удалось оформить демонстрационную выдачу; транзакция отменена")
        return true
    }
}
