package domain

class LibraryEntityFactory(private val ids: IdGenerator = UuidIdGenerator()) {
    fun reader(fullName: String, birthYear: Int, address: String, organization: String): Reader =
        Reader(ids.nextId(), fullName, birthYear, address, organization)

    fun librarian(fullName: String, position: String, branchId: String): Librarian =
        Librarian(ids.nextId(), fullName, position, branchId)

    fun branch(name: String, address: String): LibraryBranch =
        LibraryBranch(ids.nextId(), name, address)

    fun book(
        title: String,
        authors: String,
        publisher: String,
        publicationYear: Int,
        totalCopies: Int,
        branchId: String,
        isbn: String? = null,
    ): Book = Book(
        ids.nextId(), title, authors, publisher, publicationYear,
        totalCopies, totalCopies, branchId, isbn,
    )

    fun periodical(
        title: String,
        authors: String,
        publisher: String,
        publicationYear: Int,
        issueNumber: Int,
        totalCopies: Int,
        branchId: String,
    ): Periodical = Periodical(
        ids.nextId(), title, authors, publisher, publicationYear,
        totalCopies, totalCopies, branchId, issueNumber,
    )

    fun loan(
        readerId: String,
        publicationId: String,
        librarianId: String,
        issuedAt: Long,
        dueAt: Long,
    ): Loan = Loan(ids.nextId(), readerId, publicationId, librarianId, issuedAt, dueAt)
}
