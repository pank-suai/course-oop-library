package ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import domain.BookCover
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.awt.Color
import java.awt.KeyboardFocusManager
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MAX_FILE = 15 * 1024 * 1024
private const val MAX_PIXELS = 30_000_000L
private const val THUMBNAIL_EDGE = 512

internal actual suspend fun pickBookCover(): BookCover? {
    val file = withContext(Dispatchers.Swing) {
        val chooser = JFileChooser().apply {
            dialogTitle = "Выбрать обложку книги"
            approveButtonText = "Выбрать"
            isAcceptAllFileFilterUsed = false
            fileFilter = FileNameExtensionFilter("Изображения JPG и PNG", "jpg", "jpeg", "png")
        }
        val parent = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    } ?: return null
    return withContext(Dispatchers.IO) { importBookCover(file) }
}

internal fun importBookCover(file: File): BookCover {
    if (!file.isFile) throw CoverImportException("Не удалось открыть файл обложки")
    try {
        val input = file.inputStream().use { it.readNBytes(MAX_FILE + 1) }
        if (input.size > MAX_FILE) throw CoverImportException("Обложка должна быть не больше 15 МБ")
        val original = MemoryCacheImageInputStream(ByteArrayInputStream(input)).use { stream ->
            val readers = ImageIO.getImageReaders(stream)
            if (!readers.hasNext()) throw CoverImportException("Выберите изображение JPG или PNG")
            val reader = readers.next()
            try {
                if (reader.formatName.lowercase() !in setOf("jpeg", "png")) {
                    throw CoverImportException("Выберите изображение JPG или PNG")
                }
                reader.setInput(stream, true, true)
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                if (width <= 0 || height <= 0 || width.toLong() * height > MAX_PIXELS) {
                    throw CoverImportException("Размер изображения превышает 30 мегапикселей")
                }
                val step = max(1, max(width, height) / THUMBNAIL_EDGE)
                reader.read(0, reader.defaultReadParam.apply { setSourceSubsampling(step, step, 0, 0) })
            } finally {
                reader.dispose()
            }
        }
        val scale = min(1.0, THUMBNAIL_EDGE.toDouble() / max(original.width, original.height))
        val width = (original.width * scale).roundToInt().coerceAtLeast(1)
        val height = (original.height * scale).roundToInt().coerceAtLeast(1)
        val thumbnail = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        thumbnail.createGraphics().let { graphics ->
            try {
                graphics.color = Color.WHITE
                graphics.fillRect(0, 0, width, height)
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                graphics.drawImage(original, 0, 0, width, height, null)
            } finally {
                graphics.dispose()
            }
        }
        val encoded = ByteArrayOutputStream()
        if (!ImageIO.write(thumbnail, "jpeg", encoded)) throw CoverImportException("Не удалось подготовить обложку")
        return BookCover(encoded.toByteArray())
    } catch (failure: IOException) {
        throw CoverImportException("Не удалось прочитать изображение JPG или PNG", failure)
    } catch (failure: RuntimeException) {
        if (failure is CoverImportException) throw failure
        throw CoverImportException("Не удалось обработать изображение JPG или PNG", failure)
    }
}

internal actual fun coverBitmap(cover: BookCover): ImageBitmap? =
    runCatching { Image.makeFromEncoded(cover.toByteArray()).use { it.toComposeImageBitmap() } }.getOrNull()
