package ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class TestDataMessage(val kind: String, val text: String)

internal class TestDataViewModel(
    private val source: TestDataSource,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : AutoCloseable {
    private val operation = SaveOperation(scope) {}
    val state = operation.state
    private val mutableMessage = MutableStateFlow<TestDataMessage?>(null)
    val message = mutableMessage.asStateFlow()

    fun addBooks() { operation.submit {
        mutableMessage.value = null
        val count = source.addBooks()
        mutableMessage.value = TestDataMessage("books", if (count == 0) "Все тестовые книги уже добавлены" else "Добавлено тестовых книг: $count")
    } }
    fun addReaders() { operation.submit {
        mutableMessage.value = null
        val count = source.addReaders()
        mutableMessage.value = TestDataMessage("readers", if (count == 0) "Все тестовые читатели уже добавлены" else "Добавлено тестовых читателей: $count")
    } }
    override fun close() = scope.cancel()
}
