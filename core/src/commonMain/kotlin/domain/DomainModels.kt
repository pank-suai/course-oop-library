package domain

/** A person registered as a library reader. */
data class Reader(
    override val id: String,
    override val fullName: String,
    val birthYear: Int,
    val address: String,
    val organization: String,
) : Person {
    init {
        validateText(fullName, "fullName")
        require(birthYear in 1800..2100) { "birthYear is outside the supported range" }
        validateText(address, "address")
        validateText(organization, "organization")
    }
}

/** A library employee who can issue publications. */
data class Librarian(
    override val id: String,
    override val fullName: String,
    val position: String,
    val branchId: String,
) : Person {
    init {
        validateText(fullName, "fullName")
        validateText(position, "position")
        validateText(branchId, "branchId")
    }
}

/** A physical or logical library location. */
data class LibraryBranch(
    override val id: String,
    val name: String,
    val address: String,
) : LibraryEntity {
    init {
        validateText(name, "name")
        validateText(address, "address")
    }
}

/** A book in the collection. */
data class Book(
    override val id: String,
    override val title: String,
    override val authors: String,
    override val publisher: String,
    override val publicationYear: Int,
    override val totalCopies: Int,
    override val availableCopies: Int,
    override val branchId: String,
 val isbn: String?,
 ) : Publication {
    init {
        validateText(title, "title")
        validateText(authors, "authors")
        validateText(publisher, "publisher")
        validateText(branchId, "branchId")
        require(publicationYear in 1450..2100) { "publicationYear is outside the supported range" }
        validatePublicationCopies(totalCopies, availableCopies)
    }
}

/** A periodical issue in the collection. */
data class Periodical(
    override val id: String,
    override val title: String,
    override val authors: String,
    override val publisher: String,
    override val publicationYear: Int,
    override val totalCopies: Int,
    override val availableCopies: Int,
    override val branchId: String,
    val issueNumber: Int,
) : Publication {
    init {
        validateText(title, "title")
        validateText(authors, "authors")
        validateText(publisher, "publisher")
        validateText(branchId, "branchId")
        require(publicationYear in 1450..2100) { "publicationYear is outside the supported range" }
        require(issueNumber > 0) { "issueNumber must be positive" }
        validatePublicationCopies(totalCopies, availableCopies)
    }
}

/** State pattern for a loan lifecycle. */
sealed interface LoanState {
    val status: String

    data object Active : LoanState {
        override val status: String = "ACTIVE"
        override fun toString(): String = status
    }

    data object Overdue : LoanState {
        override val status: String = "OVERDUE"
        override fun toString(): String = status
    }

    data class Returned(val returnedAt: Long) : LoanState {
        override val status: String = "RETURNED"
    }
}

fun LoanState.returnAt(timestamp: Long): LoanState = when (this) {
    LoanState.Active, LoanState.Overdue -> LoanState.Returned(timestamp)
    is LoanState.Returned -> error("Loan has already been returned")
}

data class Loan(
    override val id: String,
    val readerId: String,
    val publicationId: String,
    val librarianId: String,
    val issuedAt: Long,
    val dueAt: Long,
    val state: LoanState = LoanState.Active,
) : LibraryEntity {
    init {
        validateText(readerId, "readerId")
        validateText(publicationId, "publicationId")
        validateText(librarianId, "librarianId")
        require(dueAt >= issuedAt) { "dueAt must not precede issuedAt" }
    }
}
