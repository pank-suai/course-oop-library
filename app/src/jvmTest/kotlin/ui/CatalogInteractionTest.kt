package ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import di.ApplicationContainer
import domain.LibraryBranch
import domain.Librarian
import domain.Reader
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import persistence.buildDatabase
import persistence.databaseBuilder
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val parent = File(System.getenv("TMPDIR") ?: "build/tmp").apply { mkdirs() }
    private val directory = Files.createTempDirectory(parent.toPath(), "library-ui-").toFile()
    private val container = ApplicationContainer(buildDatabase(databaseBuilder(File(directory, "library.db"))))

    @After fun close() { container.close(); directory.deleteRecursively() }

    @Test fun `primary action gets the specified darker hover state`() {
        compose.setContent { App(container) }
        val button = compose.onNodeWithText("Добавить читателя")
        button.performMouseInput { enter(center) }
        compose.waitForIdle()
        val pixels = button.captureToImage().toPixelMap()
        assertEquals(0xFF101018.toInt(), pixels[10, pixels.height / 2].toArgb())
    }

    @Test fun `search has an accessible name and clears its query`() {
        compose.setContent { App(container) }
        compose.onNodeWithContentDescription("Поиск по имени").performTextInput("несуществующий читатель")
        compose.onNodeWithText("Ничего не найдено").assertIsDisplayed()
        snapshot("search-no-results")
        compose.onNodeWithContentDescription("Очистить поиск").performClick()
        compose.onNodeWithText("Читателей пока нет").assertIsDisplayed()
    }

    @Test fun `reader catalogue opens editor only after add action`() {
        compose.setContent { App(container) }
        compose.onNodeWithText("Новый читатель").assertDoesNotExist()
        compose.onNodeWithText("Добавить читателя").performClick()
        compose.onNodeWithText("Новый читатель").assertIsDisplayed()
        compose.onNodeWithText("Отмена").performClick()
        compose.onNodeWithText("Новый читатель").assertDoesNotExist()
    }

    @Test fun `wide reader editor places name and year on one row`() {
        compose.setContent { App(container) }
        compose.onNodeWithText("Добавить читателя").performClick()
        val name = compose.onNodeWithContentDescription("Полное имя").getUnclippedBoundsInRoot()
        val year = compose.onNodeWithContentDescription("Год рождения").getUnclippedBoundsInRoot()
        assertEquals(name.top, year.top)
    }

    @Test fun `reader added after the empty catalogue settles appears without another UI action`() = runBlocking {
        compose.setContent { App(container) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Читателей пока нет").fetchSemanticsNodes().isNotEmpty() }
        container.repositories.readers.save(Reader("external-reader", "Читатель после загрузки", 1997, "Учебный адрес", "Учебная организация"))
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Читатель после загрузки").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Читателей пока нет").assertDoesNotExist()
    }

    @Test fun `invalid reader keeps the form and entered name then saves once after correction`() {
        compose.setContent { App(container) }
        compose.onNodeWithText("Добавить читателя").performClick()
        field("Полное имя", "Ирина Иванова")
        field("Год рождения", "не-год")
        field("Адрес", "Санкт-Петербург")
        field("Организация", "ГУАП")
        compose.onNodeWithText("Сохранить").performClick()
        compose.onNodeWithText("Введите имя, корректный год рождения, адрес и организацию").assertIsDisplayed()
        compose.onNodeWithContentDescription("Полное имя").assertTextContains("Ирина Иванова")
        assertTrue(runBlocking { container.repositories.readers.readersOnce().isEmpty() })
        snapshot("reader-error", compose.onNode(isDialog()))
        compose.onNodeWithContentDescription("Год рождения").performTextReplacement("1997")
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Новый читатель").fetchSemanticsNodes().isEmpty() }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Ирина Иванова").fetchSemanticsNodes().isNotEmpty() }
        val saved = runBlocking { container.repositories.readers.readersOnce() }.single()
        assertEquals("Ирина Иванова", saved.fullName)
        assertEquals(1997, saved.birthYear)
        snapshot("readers")
        compose.onNodeWithContentDescription("Удалить читателя Ирина Иванова").performClick()
        compose.onNodeWithText("Удалить читателя?").assertIsDisplayed()
        compose.onNodeWithText("Отмена").performClick()
        assertEquals(1, runBlocking { container.repositories.readers.readersOnce() }.size)
        compose.onNodeWithContentDescription("Удалить читателя Ирина Иванова").performClick()
        compose.onNode(hasText("Удалить") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Ирина Иванова").fetchSemanticsNodes().isEmpty() }
        assertTrue(runBlocking { container.repositories.readers.readersOnce().isEmpty() })
    }

    @Test fun `clear confirmation cancel preserves rows and confirmed clear updates the database`() = runBlocking {
        container.repositories.readers.save(Reader("qa-clear", "Читатель для проверки очистки", 1997, "Санкт-Петербург", "ГУАП"))
        compose.setContent { App(container) }
        tab("Архитектура")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Очистить данные"))
        compose.onNodeWithText("Очистить данные").performClick()
        compose.onNodeWithText("Очистить базу?").assertIsDisplayed()
        compose.onNodeWithText("Отмена").performClick()
        assertEquals(1, container.repositories.readers.readersOnce().size)
        compose.onNodeWithText("Очистить данные").performClick()
        compose.onNodeWithText("Очистить").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Удалено записей: 1").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(container.repositories.readers.readersOnce().isEmpty())
        snapshot("cleared-database")
    }

    @Test fun `book and periodical forms persist actual records and a loan returns only once`() = runBlocking {
        container.repositories.branches.save(LibraryBranch("qa-branch", "Учебный филиал", "Санкт-Петербург"))
        container.repositories.librarians.save(Librarian("qa-librarian", "Библиотекарь для проверки", "Библиотекарь", "qa-branch"))
        container.repositories.readers.save(Reader("qa-reader", "Ирина Иванова", 1997, "Санкт-Петербург", "ГУАП"))
        compose.setContent { App(container) }
        tab("Издания")
        compose.onNodeWithText("Добавить издание").performClick()
        field("Название", "Основы объектно-ориентированного программирования и проектирования библиотечных систем")
        field("Авторы", "Иван Иванов")
        field("Издательство", "ГУАП")
        field("Год издания", "2026")
        field("Экземпляры", "2")
        field("ISBN (необязательно)", "978-5-00000-000-0")
        compose.onNodeWithText("Выберите: филиал").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("Учебный филиал").performClick()
        snapshot("book-form", compose.onNode(isDialog()))
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Новое издание").fetchSemanticsNodes().isEmpty() }
        assertEquals(1, container.repositories.publications.publicationsOnce().size)
        compose.onNodeWithText("Добавить издание").performClick()
        compose.onNodeWithText("Периодика").performClick()
        field("Название", "Учебная периодика")
        field("Редакторы", "Иван Иванов")
        field("Издательство", "ГУАП")
        field("Год издания", "2026")
        field("Экземпляры", "1")
        compose.onNodeWithContentDescription("Номер выпуска").performTextReplacement("3")
        compose.onNodeWithText("Выберите: филиал").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("Учебный филиал").performClick()
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Новое издание").fetchSemanticsNodes().isEmpty() }
        assertEquals(2, container.repositories.publications.publicationsOnce().size)
        compose.onNodeWithText("Учебный филиал · Свободно 2 из 2").assertIsDisplayed()
        compose.onNodeWithText("ISBN 978-5-00000-000-0").assertIsDisplayed()
        compose.onNodeWithText("Периодика № 3 · ГУАП · 2026").assertIsDisplayed()
        snapshot("publications")
        // Search in a catalogue must not restrict the independent issuing selector.
        compose.onNodeWithContentDescription("Поиск по названию или автору").performTextInput("нет такого издания")
        tab("Выдачи")
        compose.onNodeWithText("Оформить выдачу").performClick()
        compose.onNodeWithText("Выдать").assertIsNotEnabled()
        compose.onNodeWithText("Выберите: читатель").performClick()
        compose.onNodeWithText("Ирина Иванова").performClick()
        compose.onNodeWithText("Выберите: издание").performClick()
        compose.onNodeWithText("Учебная периодика").performClick()
        snapshot("loan-form", compose.onNode(isDialog()))
        compose.onNodeWithText("Выдать").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Новая выдача").fetchSemanticsNodes().isEmpty() }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("На руках").fetchSemanticsNodes().isNotEmpty() }
        snapshot("loans")
        compose.onNodeWithText("Вернуть").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Возвращено").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Вернуть").assertIsNotEnabled()
        snapshot("loans-returned")
        assertEquals(1, container.repositories.publications.publicationsOnce().first { it.title == "Учебная периодика" }.availableCopies)
    }

    @Test fun `narrow increased-font editor keeps all fields and stacks them vertically`() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.25f)) {
                Box(Modifier.size(800.dp, 680.dp)) { App(container) }
            }
        }
        for (label in listOf("Библиотека", "Архитектура")) {
            compose.onNodeWithText(label).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { action ->
                val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                assertTrue(action(results))
                assertEquals(1, results.single().lineCount, "Navigation label must not break a word: $label")
            }
        }
        compose.onNodeWithText("Добавить читателя").assertIsDisplayed().performClick()
        val name = compose.onNodeWithContentDescription("Полное имя").getUnclippedBoundsInRoot()
        val year = compose.onNodeWithContentDescription("Год рождения").getUnclippedBoundsInRoot()
        assertTrue(year.top >= name.bottom)
        compose.onNodeWithContentDescription("Адрес").assertExists()
        compose.onNodeWithContentDescription("Организация").assertExists()
        compose.onNodeWithText("Отмена").assertIsDisplayed()
        snapshot("reader-form-narrow", compose.onNode(isDialog()))
        compose.onNodeWithText("Отмена").performClick()
        snapshot("readers-narrow")
    }

    @Test fun `selected navigation labels remain on one line in the default viewport`() {
        compose.setContent { App(container) }
        for (label in LibraryDesign.navigationLabels) {
            tab(label)
            compose.onNode(hasText(label) and hasClickAction()).performSemanticsAction(
                androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult,
            ) { action ->
                val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                assertTrue(action(results))
                assertEquals(1, results.single().lineCount, "Selected navigation label must not wrap: $label")
            }
        }
    }

    @Test fun `database read failure stays inside the catalogue as a readable error`() {
        runBlocking { container.repositories.readers.readersOnce() }
        container.repositories.database.close()
        compose.setContent { App(container) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Не удалось загрузить читателей").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Не удалось загрузить читателей").assertIsDisplayed()
        compose.onNodeWithText("Добавить читателя").assertIsDisplayed()
    }

    @Test fun `all sections render without implicit editors or database writes`() {
        compose.setContent { App(container) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Читателей пока нет").fetchSemanticsNodes().isNotEmpty() }
        snapshot("readers-empty")
        tab("Издания")
        compose.onNodeWithText("Добавить издание").assertIsDisplayed()
        compose.onNodeWithText("Новое издание").assertDoesNotExist()
        snapshot("publications-empty")
        tab("Выдачи")
        compose.onNodeWithText("Оформить выдачу").assertIsDisplayed()
        snapshot("loans-empty")
        tab("Архитектура")
        compose.onNodeWithText("Основные объекты").assertIsDisplayed()
        snapshot("architecture")
        assertTrue(runBlocking { container.repositories.readers.readersOnce().isEmpty() })
    }

    private fun field(label: String, value: String) = compose.onNodeWithContentDescription(label).performTextInput(value)
    private fun tab(label: String) = compose.onNode(hasText(label) and hasClickAction()).performClick()
    private fun snapshot(name: String, node: SemanticsNodeInteraction = compose.onRoot()) {
        val pixels = node.captureToImage().toPixelMap()
        val image = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) image.setRGB(x, y, pixels[x, y].toArgb())
        val output = File("build/reports/ui-design/$name.png")
        output.parentFile.mkdirs()
        check(ImageIO.write(image, "png", output))
    }
}
