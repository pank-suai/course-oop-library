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
        val reader = repositories.readers.findById(readerId) ?: error("Читатель не найден")
        val publication = repositories.publications.findById(publicationId) ?: error("Издание не найдено")
        repositories.librarians.findById(librarianId) ?: error("Библиотекарь не найден")
        val decision = policy.canIssue(reader, publication, repositories.loans.activeCountForReader(readerId))
        if (!decision.isAllowed) error(decision.reason ?: "Выдача запрещена")
        val loan = Loan(
            id = ids.nextId(),
            readerId = readerId,
            publicationId = publicationId,
            librarianId = librarianId,
            issuedAt = now,
            dueAt = policy.dueAt(now, publication),
            state = LoanState.Active,
        )
        if (!repositories.loans.issue(loan, policy.maxActiveLoansPerReader)) {
            error("Выдача запрещена или у издания нет доступных экземпляров")
        }
        return loan
    }

    override suspend fun returnLoan(loanId: String, now: Long): Loan =
        repositories.loans.returnLoan(loanId, now) ?: error("Выдача не найдена")

    override suspend fun deleteReader(readerId: String) {
        repositories.readers.delete(readerId)
    }

    override suspend fun deletePublication(publicationId: String) {
        repositories.publications.delete(publicationId)
    }

    override suspend fun seedDemo(): Boolean {
        val factory = LibraryEntityFactory(ids)
        val branch = factory.branch("Центральная библиотека", "Невский проспект, 28")
        val librarian = factory.librarian("Елена Волкова", "Старший библиотекарь", branch.id)
        val reader = factory.reader("Анна Смирнова", 1995, "Санкт-Петербург", "ГУАП")
        val secondReader = factory.reader("Алексей Петров", 1994, "Санкт-Петербург", "ГУАП")
        val book = factory.book("Объектно-ориентированное программирование", "Е. О. Шумова", "ГУАП", 2026, 2, branch.id, "978-5-00000-001-0")
        val periodical = factory.periodical("Библиотечное дело", "Редакционная коллегия", "Профессия", 2025, 1, 2, branch.id)
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
