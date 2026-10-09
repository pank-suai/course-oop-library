package ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import di.ApplicationContainer
import domain.Book
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
import org.jetbrains.compose.resources.painterResource
import ui.resources.Res
import ui.resources.cover_clean_code
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDataInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val parent = File(System.getenv("TMPDIR") ?: "build/tmp").apply { mkdirs() }
    private val directory = Files.createTempDirectory(parent.toPath(), "library-cover-ui-").toFile()
    private val container = ApplicationContainer(buildDatabase(databaseBuilder(File(directory, "library.db"))))
    @After fun close() { container.close(); directory.deleteRecursively() }

    @Test fun `books are generated only by the test button and preserve existing records`() = runBlocking {
        compose.setContent { App(container) }
        assertTrue(container.repositories.publications.publicationsOnce().isEmpty())
        assertTrue(container.repositories.readers.readersOnce().isEmpty())
        compose.onNode(hasText("Издания") and hasClickAction()).performClick()
        compose.onNodeWithText("Тестовые книги").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Добавлено тестовых книг: 3").fetchSemanticsNodes().isNotEmpty() }

        val first = container.repositories.publications.publicationsOnce()
        assertEquals(3, first.size)
        assertTrue(first.all { it is Book && !it.isbn.isNullOrBlank() && it.availableCopies == it.totalCopies })
        assertTrue(container.repositories.readers.readersOnce().isEmpty())
        for (book in first) compose.onNodeWithContentDescription("Обложка: ${book.title}").assertExists()
        snapshot("publications-with-covers")
        compose.onNodeWithText("Тестовые книги").performClick()
        compose.waitUntil(5_000) { runBlocking { container.repositories.publications.publicationsOnce().size == 6 } }
        val all = container.repositories.publications.publicationsOnce()
        assertTrue(all.map { it.id }.containsAll(first.map { it.id }))
        assertEquals(6, all.map { (it as Book).isbn }.toSet().size)
        compose.onNodeWithText("Тестовые книги").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Все тестовые книги уже добавлены").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(6, container.repositories.publications.publicationsOnce().size)
    }

    @Test fun `reader button adds five readers without generating books or loans`() = runBlocking {
        compose.setContent { App(container) }
        compose.onNodeWithText("Тестовые читатели").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Добавлено тестовых читателей: 5").fetchSemanticsNodes().isNotEmpty() }

        val first = container.repositories.readers.readersOnce()
        assertEquals(5, first.size)
        assertTrue(first.all { it.organization == "Тестовые данные" && it.fullName.isNotBlank() })
        snapshot("readers-with-testdata")
        compose.onNodeWithText("Тестовые читатели").performClick()
        compose.waitUntil(5_000) { runBlocking { container.repositories.readers.readersOnce().size == 10 } }
        val all = container.repositories.readers.readersOnce()
        assertTrue(all.map { it.id }.containsAll(first.map { it.id }))
        assertEquals(10, all.map { it.fullName }.toSet().size)
        assertTrue(container.repositories.publications.publicationsOnce().isEmpty())
    }

    private fun snapshot(name: String) {
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        val image = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) image.setRGB(x, y, pixels[x, y].toArgb())
        val output = File("build/reports/ui-design/$name.png"); output.parentFile.mkdirs()
        check(ImageIO.write(image, "png", output))
    }

    @Test fun `cover is actually desaturated without changing the source image`() {
        val sample = TestBookCatalogue.books.first()
        val book = Book("cover-test", sample.title, sample.authors, "Питер", sample.year, 1, 1, "branch", sample.isbn)
        compose.setContent { LibraryTheme { Row {
            Image(painterResource(Res.drawable.cover_clean_code), "Исходная обложка", Modifier.size(68.dp, 96.dp), contentScale = ContentScale.Fit)
            PublicationCover(book)
        } } }
        val original = compose.onNodeWithContentDescription("Исходная обложка").captureToImage().toPixelMap()
        val toned = compose.onNodeWithContentDescription("Обложка: ${sample.title}").captureToImage().toPixelMap()
        fun chroma(pixels: androidx.compose.ui.graphics.PixelMap): Float {
            var sum = 0f
            for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                val color = pixels[x, y]
                sum += maxOf(color.red, color.green, color.blue) - minOf(color.red, color.green, color.blue)
            }
            return sum / (pixels.width * pixels.height)
        }
        assertTrue(chroma(original) > 0.01f)
        assertTrue(chroma(toned) < chroma(original) * 0.6f)
    }

    @Test fun `unknown ISBN has a neutral fallback rather than a borrowed cover`() {
        val book = Book("unknown", "Учебная книга", "Учебный автор", "ГУАП", 2026, 1, 1, "branch", "978-5-00000-000-0")
        compose.setContent { LibraryTheme { PublicationCover(book) } }
        compose.onNodeWithContentDescription("Обложка недоступна: Учебная книга").assertExists()
        compose.onNodeWithContentDescription("Обложка: Учебная книга").assertDoesNotExist()
    }

    @Test fun `failed test data write shows an error and never claims success`() = runBlocking {
        container.repositories.readers.readersOnce()
        container.repositories.database.close()
        compose.setContent { App(container) }
        compose.onNodeWithText("Тестовые читатели").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Не удалось добавить тестовые данные").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Тестовые читатели").assertIsEnabled()
        compose.onNodeWithText("Добавлено тестовых читателей: 5").assertDoesNotExist()
    }
}
