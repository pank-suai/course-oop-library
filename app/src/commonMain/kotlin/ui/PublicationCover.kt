package ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import domain.Book
import domain.Publication
import org.jetbrains.compose.resources.painterResource
import ui.resources.*

/** Covers retain their identity; only presentation saturation is reduced. No network requests. */
@Composable
internal fun PublicationCover(publication: Publication) {
    val book = (publication as? Book)?.isbn?.let(TestBookCatalogue::find)
    val asset = when (book?.coverKey) {
        "clean_code" -> Res.drawable.cover_clean_code
        "kotlin" -> Res.drawable.cover_kotlin
        "api_patterns" -> Res.drawable.cover_api_patterns
        "rust_patterns" -> Res.drawable.cover_rust_patterns
        "dotnet_patterns" -> Res.drawable.cover_dotnet_patterns
        "headfirst_patterns" -> Res.drawable.cover_headfirst_patterns
        else -> null
    }
    val shape = RoundedCornerShape(10.dp)
    Box(Modifier.size(68.dp, 96.dp).clip(shape).background(LibraryDesign.control)
        .border(1.dp, LibraryDesign.divider, shape).padding(5.dp)
        .semantics { contentDescription = if (asset == null) "Обложка недоступна: ${publication.title}" else "Обложка: ${publication.title}" },
        contentAlignment = Alignment.Center) {
        if (asset != null) {
            Image(painterResource(asset), contentDescription = null, modifier = Modifier.size(58.dp, 86.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.28f) }))
        } else {
            Icon(LibraryIcon.MenuBook.vector, contentDescription = null, tint = LibraryDesign.secondaryText, modifier = Modifier.size(28.dp))
        }
    }
}
