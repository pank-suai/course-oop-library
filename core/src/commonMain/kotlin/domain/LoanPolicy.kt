package domain

interface LoanPolicy {
    fun canIssue(reader: Reader, publication: Publication, activeLoansForReader: Int): PolicyDecision
    fun dueAt(issuedAt: Long, publication: Publication): Long
}

data class PolicyDecision(val isAllowed: Boolean, val reason: String? = null)

class DefaultLoanPolicy(
    private val maxActiveLoansPerReader: Int = 5,
    private val bookLoanDays: Long = 21,
    private val periodicalLoanDays: Long = 7,
) : LoanPolicy {
    init {
        require(maxActiveLoansPerReader > 0)
        require(bookLoanDays > 0)
        require(periodicalLoanDays > 0)
    }

    override fun canIssue(
        reader: Reader,
        publication: Publication,
        activeLoansForReader: Int,
    ): PolicyDecision = when {
        activeLoansForReader >= maxActiveLoansPerReader ->
            PolicyDecision(false, "Reader has reached the active loan limit")
        publication.availableCopies <= 0 ->
            PolicyDecision(false, "Publication has no available copies")
        else -> PolicyDecision(true)
    }

    override fun dueAt(issuedAt: Long, publication: Publication): Long {
        val days = if (publication is Periodical) periodicalLoanDays else bookLoanDays
        return issuedAt + days * MILLIS_PER_DAY
    }

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
