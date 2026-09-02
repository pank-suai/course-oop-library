package persistence

import domain.Book
import domain.ClearAllResult
import domain.DemoData
import domain.LibraryBranch
import domain.Librarian
import domain.Loan
import domain.LoanState
import domain.Periodical
import domain.Publication
import domain.Reader
import androidx.sqlite.SQLiteException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map

interface ReaderRepository {
    val readers: Flow<List<Reader>>
    suspend fun readersOnce(): List<Reader>
    suspend fun findById(id: String): Reader?
    suspend fun save(reader: Reader)
    suspend fun delete(id: String)
}

interface LibrarianRepository {
    val librarians: Flow<List<Librarian>>
    suspend fun findById(id: String): Librarian?
    suspend fun save(librarian: Librarian)
}

interface BranchRepository {
    val branches: Flow<List<LibraryBranch>>
    suspend fun findById(id: String): LibraryBranch?
    suspend fun save(branch: LibraryBranch)
}

interface PublicationRepository {
    val publications: Flow<List<Publication>>
    suspend fun publicationsOnce(): List<Publication>
    suspend fun findById(id: String): Publication?
    suspend fun save(publication: Publication)
    suspend fun delete(id: String)
}

interface LoanRepository {
    val loans: Flow<List<Loan>>
    suspend fun findById(id: String): Loan?
    suspend fun issue(loan: Loan): Boolean
    suspend fun issue(loan: Loan, maxActiveLoansPerReader: Int): Boolean = issue(loan)
    suspend fun returnLoan(id: String, returnedAt: Long): Loan?
    suspend fun activeCountForReader(readerId: String): Int
    suspend fun refreshOverdue(now: Long): Int = 0
}

interface LibraryAdministrationRepository {
    suspend fun insertDemo(data: DemoData): Boolean
    suspend fun clearAll(): ClearAllResult
}

interface LibraryRepositoryBundle : LibraryAdministrationRepository {
    val readers: ReaderRepository
    val librarians: LibrarianRepository
    val branches: BranchRepository
    val publications: PublicationRepository
    val loans: LoanRepository
}

class ActiveLoanDeletionException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)
class InventoryException(message: String) : IllegalStateException(message)

class RoomReaderRepository(private val dao: LibraryDao) : ReaderRepository {
    override val readers: Flow<List<Reader>> = dao.observeReaders().map { records -> records.map { readerRecordToDomain(it) } }
    override suspend fun readersOnce(): List<Reader> = dao.getReaders().map { readerRecordToDomain(it) }
    override suspend fun findById(id: String): Reader? = dao.getReader(id)?.let { readerRecordToDomain(it) }
    override suspend fun save(reader: Reader) = dao.saveReader(reader.toRecord())
    override suspend fun delete(id: String) {
        try {
            when (dao.deleteReaderIfUnused(id)) {
                -1 -> Unit
                0 -> throw ActiveLoanDeletionException("Нельзя удалить читателя: есть история или активные выдачи")
            }
        } catch (error: ActiveLoanDeletionException) {
            throw error
        } catch (error: SQLiteException) {
            throw ActiveLoanDeletionException("Нельзя удалить читателя: связи с выдачами изменились", error)
        }
    }
}

class RoomLibrarianRepository(private val dao: LibraryDao) : LibrarianRepository {
    override val librarians: Flow<List<Librarian>> = dao.observeLibrarians().map { records -> records.map { librarianRecordToDomain(it) } }
    override suspend fun findById(id: String): Librarian? = dao.getLibrarian(id)?.let { librarianRecordToDomain(it) }
    override suspend fun save(librarian: Librarian) = dao.saveLibrarian(librarian.toRecord())
}

class RoomBranchRepository(private val dao: LibraryDao) : BranchRepository {
    override val branches: Flow<List<LibraryBranch>> = dao.observeBranches().map { records -> records.map { branchRecordToDomain(it) } }
    override suspend fun findById(id: String): LibraryBranch? = dao.getBranches().firstOrNull { it.id == id }?.let { branchRecordToDomain(it) }
    override suspend fun save(branch: LibraryBranch) = dao.saveBranch(branch.toRecord())
}

class RoomPublicationRepository(private val dao: LibraryDao) : PublicationRepository {
    override val publications: Flow<List<Publication>> = dao.observePublications().map { records -> records.map { publicationRecordToDomain(it) } }
    override suspend fun publicationsOnce(): List<Publication> = dao.getPublications().map { publicationRecordToDomain(it) }
    override suspend fun findById(id: String): Publication? = dao.getPublication(id)?.let { publicationRecordToDomain(it) }
    override suspend fun save(publication: Publication) = dao.savePublication(publication.toRecord())
    override suspend fun delete(id: String) {
        try {
            when (dao.deletePublicationIfUnused(id)) {
                -1 -> Unit
                0 -> throw ActiveLoanDeletionException("Нельзя удалить издание: есть история или активные выдачи")
            }
        } catch (error: ActiveLoanDeletionException) {
            throw error
        } catch (error: SQLiteException) {
            throw ActiveLoanDeletionException("Нельзя удалить издание: связи с выдачами изменились", error)
        }
    }
}

class RoomLoanRepository(private val dao: LibraryDao) : LoanRepository {
    @OptIn(kotlin.time.ExperimentalTime::class)
    override val loans: Flow<List<Loan>> = kotlinx.coroutines.flow.flow {
        dao.markOverdueLoans(kotlin.time.Clock.System.now().toEpochMilliseconds())
        emitAll(dao.observeLoans())
    }.map { records -> records.map { loanRecordToDomain(it) } }

    @OptIn(kotlin.time.ExperimentalTime::class)
    override suspend fun findById(id: String): Loan? {
        dao.markOverdueLoans(kotlin.time.Clock.System.now().toEpochMilliseconds())
        return dao.getLoan(id)?.let { loanRecordToDomain(it) }
    }

    override suspend fun issue(loan: Loan): Boolean = issue(loan, Int.MAX_VALUE)
    override suspend fun issue(loan: Loan, maxActiveLoansPerReader: Int): Boolean {
        if (loan.state != LoanState.Active) return false
        require(maxActiveLoansPerReader > 0)
        return dao.issueLoanAtomically(loan.toRecord(), maxActiveLoansPerReader)
    }

    override suspend fun returnLoan(id: String, returnedAt: Long): Loan? =
        dao.returnLoanAtomically(id, returnedAt)?.let { loanRecordToDomain(it) }

    override suspend fun activeCountForReader(readerId: String): Int = dao.countActiveLoans(readerId)
    override suspend fun refreshOverdue(now: Long): Int = dao.markOverdueLoans(now)
}

class RoomRepositoryBundle(val database: LibraryDatabase) : LibraryRepositoryBundle {
    private val dao = database.libraryDao()
    override val readers = RoomReaderRepository(dao)
    override val librarians = RoomLibrarianRepository(dao)
    override val branches = RoomBranchRepository(dao)
    override val publications = RoomPublicationRepository(dao)
    override val loans = RoomLoanRepository(dao)

    override suspend fun insertDemo(data: DemoData): Boolean = dao.insertDemoData(
        data.branch.toRecord(),
        data.librarian.toRecord(),
        data.readers.map { it.toRecord() },
        data.publications.map { it.toRecord() },
        data.loan.toRecord(),
    )

    override suspend fun clearAll(): ClearAllResult = dao.clearAllData().let {
        ClearAllResult(it.loans, it.publications, it.readers, it.librarians, it.branches)
    }

    /** Compatibility entry point for persistence-focused rollback tests. */
    suspend fun insertDemoData(
        branch: BranchRecord,
        librarian: LibrarianRecord,
        readers: List<ReaderRecord>,
        publications: List<PublicationRecord>,
        loan: LoanRecord,
    ): Boolean = dao.insertDemoData(branch, librarian, readers, publications, loan)
}

private fun Reader.toRecord() = ReaderRecord(id, fullName, birthYear, address, organization)
private fun readerRecordToDomain(record: Reader) = record
private fun readerRecordToDomain(record: ReaderRecord) = Reader(record.id, record.fullName, record.birthYear, record.address, record.organization)
private fun Librarian.toRecord() = LibrarianRecord(id, fullName, position, branchId)
private fun librarianRecordToDomain(record: LibrarianRecord) = Librarian(record.id, record.fullName, record.position, record.branchId)
private fun LibraryBranch.toRecord() = BranchRecord(id, name, address)
private fun branchRecordToDomain(record: BranchRecord) = LibraryBranch(record.id, record.name, record.address)
private fun Publication.toRecord() = when (this) {
    is Book -> PublicationRecord(id, "BOOK", title, authors, publisher, publicationYear, isbn, null, totalCopies, availableCopies, branchId)
    is Periodical -> PublicationRecord(id, "PERIODICAL", title, authors, publisher, publicationYear, null, issueNumber, totalCopies, availableCopies, branchId)
    else -> error("Неподдерживаемый тип издания")
}
private fun publicationRecordToDomain(record: PublicationRecord): Publication = when (record.kind) {
    "BOOK" -> Book(record.id, record.title, record.authors, record.publisher, record.publicationYear, record.totalCopies, record.availableCopies, record.branchId, record.isbn)
    "PERIODICAL" -> Periodical(record.id, record.title, record.authors, record.publisher, record.publicationYear, record.totalCopies, record.availableCopies, record.branchId, record.issueNumber ?: 1)
    else -> error("Неизвестный тип издания: ${record.kind}")
}
private fun Loan.toRecord() = LoanRecord(id, readerId, publicationId, librarianId, issuedAt, dueAt, (state as? LoanState.Returned)?.returnedAt, state.status)
private fun loanRecordToDomain(record: LoanRecord): Loan {
    val state = when (record.status) {
        "ACTIVE" -> LoanState.Active
        "OVERDUE" -> LoanState.Overdue
        "RETURNED" -> LoanState.Returned(record.returnedAt ?: error("У возвращённой выдачи нет даты возврата"))
        else -> error("Неизвестный статус выдачи: ${record.status}")
    }
    return Loan(record.id, record.readerId, record.publicationId, record.librarianId, record.issuedAt, record.dueAt, state)
}
