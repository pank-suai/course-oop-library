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
)

/** UI operation state: keep the form open until the actual write succeeds. */
internal class SaveOperation(
    private val scope: CoroutineScope,
    private val onError: (String?) -> Unit,
) {
    private val mutable = MutableStateFlow(SaveUiState())
    val state: StateFlow<SaveUiState> = mutable.asStateFlow()

    fun reject(message: String) {
        if (mutable.value.isSaving) return
        mutable.update { it.copy(error = message) }
        onError(message)
    }

    fun submit(operation: suspend () -> Unit): Boolean {
        if (!scope.isActive) return false
        val previous = mutable.value
        if (previous.isSaving || !mutable.compareAndSet(previous, previous.copy(isSaving = true, error = null))) return false
        onError(null)
        scope.launch {
            try {
                operation()
                mutable.update { it.copy(error = null, revision = it.revision + 1) }
                onError(null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                val message = failure.message ?: "Не удалось сохранить запись"
                mutable.update { it.copy(error = message) }
                onError(message)
            } finally {
                mutable.update { it.copy(isSaving = false) }
            }
        }
        return true
    }
}
