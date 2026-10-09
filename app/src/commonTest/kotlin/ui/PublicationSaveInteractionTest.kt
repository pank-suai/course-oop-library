package ui

import domain.LibraryBranch
import domain.Publication
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import persistence.BranchRepository
import persistence.PublicationRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PublicationSaveInteractionTest {
    @Test fun `periodical form also ignores repeated submit until the write succeeds`() = runBlocking {
        val repository = PendingPublications()
        val vm = PublicationsViewModel(repository, EmptyBranches(), scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
        try {
            vm.addPeriodical("Наука", "Иван Иванов", "ГУАП", "2026", "3", "2", "branch")
            vm.addPeriodical("Наука", "Иван Иванов", "ГУАП", "2026", "3", "2", "branch")
            assertEquals(1, repository.writes)
            assertTrue(vm.saveState.value.isSaving)
            repository.release.complete(Unit)
            assertEquals(1, vm.saveState.value.revision)
        } finally { repository.release.complete(Unit); vm.close() }
    }

    @Test fun `book form stays busy until persistence completes and ignores a second submit`() = runBlocking {
        val repository = PendingPublications()
        val vm = PublicationsViewModel(repository, EmptyBranches(), scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
        try {
            vm.addBook("Алгоритмы", "Иван Иванов", "ГУАП", "2026", "2", "branch", "")
            vm.addBook("Алгоритмы", "Иван Иванов", "ГУАП", "2026", "2", "branch", "")
            assertEquals(1, repository.writes)
            assertTrue(vm.saveState.value.isSaving)
            assertEquals(0, vm.saveState.value.revision)
            repository.release.complete(Unit)
            assertEquals(1, vm.saveState.value.revision)
        } finally { repository.release.complete(Unit); vm.close() }
    }
}

private class PendingPublications : PublicationRepository {
    val release = CompletableDeferred<Unit>()
    var writes = 0
    private val values = MutableStateFlow<List<Publication>>(emptyList())
    override val publications: Flow<List<Publication>> = values
    override suspend fun publicationsOnce(): List<Publication> = values.value
    override suspend fun findById(id: String): Publication? = values.value.firstOrNull { it.id == id }
    override suspend fun save(publication: Publication) { writes++; release.await(); values.value += publication }
    override suspend fun delete(id: String) { values.value = values.value.filterNot { it.id == id } }
}

private class EmptyBranches : BranchRepository {
    override val branches: Flow<List<LibraryBranch>> = MutableStateFlow(emptyList())
    override suspend fun findById(id: String): LibraryBranch? = null
    override suspend fun save(branch: LibraryBranch) = Unit
}
