import domain.Book
import domain.DefaultLoanPolicy
import domain.IdGenerator
import domain.LibraryBranch
import domain.LibraryEntityFactory
import domain.LoanState
import domain.Periodical
import domain.Reader
import domain.returnAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DomainRewriteTest {
    @Test
    fun factoryGeneratesOpaqueUuidIdsForConcreteEntities() {
        val factory = LibraryEntityFactory()
        val reader = factory.reader("Ada Lovelace", 1815, "London", "Analytical Society")
        val branch = factory.branch("Central", "1 Library Way")
        val book = factory.book("Computing", "A. Lovelace", "Press", 2020, 2, branch.id, "978-1")

        assertTrue(reader.id.matches(Regex("[0-9a-f-]{36}")))
        assertNotEquals(reader.id, branch.id)
        assertNotEquals(branch.id, book.id)
    }

    @Test
    fun domainRejectsInvalidValues() {
        assertFailsWith<IllegalArgumentException> {
            Reader("r", "", 1980, "Address", "Org")
        }
        assertFailsWith<IllegalArgumentException> {
            Book("b", "Title", "Author", "Press", 2020, 0, 1, "branch", "isbn")
        }
        assertFailsWith<IllegalArgumentException> {
            Periodical("p", "Title", "Publisher", "Press", 2020, 2, 1, "branch", 0)
        }
    }

    @Test
    fun loanStateAllowsReturnOnlyOnce() {
        val active = LoanState.Active
        val returned = active.returnAt(100L)
        assertTrue(returned is LoanState.Returned)
        assertFailsWith<IllegalStateException> { returned.returnAt(200L) }
    }

    @Test
    fun loanPolicyDifferentiatesPublicationTypes() {
        val policy = DefaultLoanPolicy(maxActiveLoansPerReader = 2)
        val factory = LibraryEntityFactory()
        val branch = factory.branch("Central", "Address")
        val reader = factory.reader("Ada Lovelace", 1815, "Address", "Org")
        val book = factory.book("Book", "Author", "Press", 2020, 2, branch.id, "isbn")
        val periodical = factory.periodical("Journal", "Editor", "Press", 2020, 3, 2, branch.id)

        assertTrue(policy.canIssue(reader, book, 1).isAllowed)
        assertTrue(policy.canIssue(reader, periodical, 1).isAllowed)
        assertTrue(!policy.canIssue(reader, book, 2).isAllowed)
        assertTrue(policy.dueAt(0L, book) > policy.dueAt(0L, periodical))
    }
}
