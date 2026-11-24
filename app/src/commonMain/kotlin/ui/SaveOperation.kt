package ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SaveUiState(
    val isSaving: Boolean = false,
    val error: String? = null,
    val revision: Int = 0,
    val backgroundError: String? = null,
)

/** UI operation state: keep the form open until the actual write succeeds. */
internal class SaveOperation(
    private val scope: CoroutineScope,
) {
    private val mutable = MutableStateFlow(SaveUiState())
    val state: StateFlow<SaveUiState> = mutable.asStateFlow()

    fun discardEditorError() {
        mutable.update { if (it.isSaving) it else it.copy(error = null) }
    }

    fun reject(message: String) {
        mutable.update { if (it.isSaving) it else it.copy(error = message) }
    }

    fun submit(reportInEditor: Boolean = true, operation: suspend () -> Unit): Boolean {
        if (!scope.isActive) return false
        val previous = mutable.value
        if (previous.isSaving || !mutable.compareAndSet(previous, previous.copy(isSaving = true, error = null, backgroundError = null))) return false
        scope.launch {
            try {
                operation()
                mutable.update { it.copy(error = null, revision = it.revision + 1) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                val message = failure.message ?: "Не удалось сохранить запись"
                mutable.update {
                    if (reportInEditor) it.copy(error = message) else it.copy(backgroundError = message)
                }
            } finally {
                mutable.update { it.copy(isSaving = false) }
            }
        }
        return true
    }
}
