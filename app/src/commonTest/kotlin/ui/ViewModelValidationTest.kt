package ui

import domain.Reader
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

class ViewModelValidationTest {
    @Test
    fun readersViewModelRejectsInvalidFormWithoutWriting() {
        val repository = FakeReadersRepository()
        val vm = ReadersViewModel(repository)

        assertFalse(vm.add("", "not-a-year", "", ""))

        assertEquals(0, repository.saved.size)
    }
}

private class FakeReadersRepository : persistence.ReaderRepository {
    private val values = MutableStateFlow<List<Reader>>(emptyList())
    val saved = CopyOnWriteArrayList<Reader>()
    override val readers: Flow<List<Reader>> = values
    override suspend fun readersOnce(): List<Reader> = values.value
    override suspend fun findById(id: String): Reader? = values.value.firstOrNull { it.id == id }
    override suspend fun save(reader: Reader) {
        saved += reader
        values.value = values.value + reader
    }
    override suspend fun delete(id: String) {
        values.value = values.value.filterNot { it.id == id }
    }
}
