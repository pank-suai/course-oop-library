package domain

/** Domain-facing facade; the persistence-backed implementation lives in the application layer. */
interface LibraryService {
    suspend fun issueLoan(readerId: String, publicationId: String, librarianId: String, now: Long): Loan
    suspend fun returnLoan(loanId: String, now: Long): Loan
    suspend fun deleteReader(readerId: String)
    suspend fun deletePublication(publicationId: String)
    suspend fun seedDemo(): Boolean
    suspend fun clearAll(): ClearAllResult
}

data class DemoData(
    val branch: LibraryBranch,
    val librarian: Librarian,
    val readers: List<Reader>,
    val publications: List<Publication>,
    val loan: Loan,
)

data class ClearAllResult(
    val loans: Int,
    val publications: Int,
    val readers: Int,
    val librarians: Int,
    val branches: Int,
) {
    val totalDeleted: Int
        get() = loans + publications + readers + librarians + branches
}
