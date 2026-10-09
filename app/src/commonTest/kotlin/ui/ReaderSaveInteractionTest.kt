package ui

import domain.Reader
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import persistence.ReaderRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderSaveInteractionTest {
    @Test
    fun `repeated save while first write is pending does not create duplicate reader`() = runBlocking {
        val repository = DelayedReaderRepository()
        val vm = ReadersViewModel(repository, scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
        try {
            vm.add("Ирина Иванова", "1997", "Санкт-Петербург", "ГУАП")
            vm.add("Ирина Иванова", "1997", "Санкт-Петербург", "ГУАП")
            assertEquals(1, repository.writes)
        } finally {
            repository.release.complete(Unit)
            vm.close()
        }
    }
}

private class DelayedReaderRepository : ReaderRepository {
    val release = CompletableDeferred<Unit>()
    private val values = MutableStateFlow<List<Reader>>(emptyList())
    var writes = 0
    override val readers: Flow<List<Reader>> = values
    override suspend fun readersOnce(): List<Reader> = values.value
    override suspend fun findById(id: String): Reader? = values.value.firstOrNull { it.id == id }
    override suspend fun save(reader: Reader) {
        writes += 1
        release.await()
        values.value += reader
    }
    override suspend fun delete(id: String) { values.value = values.value.filterNot { it.id == id } }
}
