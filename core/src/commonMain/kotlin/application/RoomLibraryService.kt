package application

import domain.DefaultLoanPolicy
import domain.DemoData
import domain.IdGenerator
import domain.LibraryEntityFactory
import domain.LibraryService
import domain.Loan
import domain.LoanPolicy
import domain.LoanState
import domain.UuidIdGenerator
import persistence.LibraryRepositoryBundle

class RoomLibraryService(
    private val repositories: LibraryRepositoryBundle,
    private val policy: LoanPolicy = DefaultLoanPolicy(),
    private val ids: IdGenerator = UuidIdGenerator(),
) : LibraryService {
    override suspend fun issueLoan(readerId: String, publicationId: String, librarianId: String, now: Long): Loan {
        val reader = repositories.readers.findById(readerId) ?: error("Reader was not found")
        val publication = repositories.publications.findById(publicationId) ?: error("Publication was not found")
        repositories.librarians.findById(librarianId) ?: error("Librarian was not found")
        val decision = policy.canIssue(reader, publication, repositories.loans.activeCountForReader(readerId))
        if (!decision.isAllowed) error(decision.reason ?: "Loan is not allowed")
        val loan = Loan(
            id = ids.nextId(),
            readerId = readerId,
            publicationId = publicationId,
            librarianId = librarianId,
            issuedAt = now,
            dueAt = policy.dueAt(now, publication),
            state = LoanState.Active,
        )
        if (!repositories.loans.issue(loan)) error("Publication has no available copies")
        return loan
    }

    override suspend fun returnLoan(loanId: String, now: Long): Loan =
        repositories.loans.returnLoan(loanId, now) ?: error("Loan was not found")

    override suspend fun deleteReader(readerId: String) {
        repositories.readers.delete(readerId)
    }

    override suspend fun deletePublication(publicationId: String) {
        repositories.publications.delete(publicationId)
    }

    override suspend fun seedDemo(): Boolean {
        val factory = LibraryEntityFactory(ids)
        val branch = factory.branch("Central Library", "1 Library Way")
        val librarian = factory.librarian("Grace Hopper", "Senior librarian", branch.id)
        val reader = factory.reader("Ada Lovelace", 1815, "London", "Analytical Society")
        val secondReader = factory.reader("Alan Turing", 1912, "Manchester", "Computing Society")
        val book = factory.book("Computing", "A. Lovelace", "Press", 2020, 2, branch.id, "978-demo")
        val periodical = factory.periodical("Library Review", "Editorial board", "Press", 2025, 1, 2, branch.id)
        val loan = factory.loan(reader.id, book.id, librarian.id, 1_700_000_000_000L, 1_701_814_400_000L)
        return repositories.insertDemo(
            DemoData(
                branch = branch,
                librarian = librarian,
                readers = listOf(reader, secondReader),
                publications = listOf(book, periodical),
                loan = loan,
            ),
        )
    }

    override suspend fun clearAll() = repositories.clearAll()
}
