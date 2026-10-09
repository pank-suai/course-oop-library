package ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestDataViewModelTest {
    @Test fun repeatedClicksWhilePendingDoNotStartAnotherBatch() = runBlocking {
        val source = PendingSource()
        val vm = TestDataViewModel(source, CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
        vm.addBooks(); vm.addBooks(); vm.addReaders()
        assertTrue(vm.state.value.isSaving)
        assertEquals(1, source.booksCalls)
        assertEquals(0, source.readerCalls)
        source.release.complete(Unit)
        assertFalse(vm.state.value.isSaving)
        assertEquals(1, vm.state.value.revision)
        assertEquals("Добавлено тестовых книг: 3", vm.message.value?.text)
        vm.close()
    }

    @Test fun closingOwnerCancelsPendingBatchAndDoesNotPublishSuccess() = runBlocking {
        val source = PendingSource()
        val vm = TestDataViewModel(source, CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
        vm.addBooks(); vm.close(); source.release.complete(Unit)
        assertEquals(0, source.completed)
        assertNull(vm.message.value)
        assertNull(vm.state.value.error)
        vm.addReaders()
        assertEquals(0, source.readerCalls)
    }

    @Test fun coverLookupDoesNotRepairMalformedSourceIdentifiers() {
        val book = TestBookCatalogue.books.first()
        assertEquals(book, TestBookCatalogue.find(book.isbn))
        assertEquals(book, TestBookCatalogue.find(book.isbn.replace("-", "")))
        assertNull(TestBookCatalogue.find(" ${book.isbn}"))
        assertNull(TestBookCatalogue.find(book.isbn.replaceFirst("-", "--")))
        assertNull(TestBookCatalogue.find("978-5-00000-000-0"))
        assertNull(TestBookCatalogue.find(null))
        assertEquals(6, TestBookCatalogue.books.map { it.isbn }.toSet().size)
    }

    private class PendingSource : TestDataSource {
        val release = CompletableDeferred<Unit>()
        var booksCalls = 0
        var readerCalls = 0
        var completed = 0
        override suspend fun addBooks(): Int { booksCalls++; release.await(); completed++; return 3 }
        override suspend fun addReaders(): Int { readerCalls++; release.await(); completed++; return 5 }
    }
}
