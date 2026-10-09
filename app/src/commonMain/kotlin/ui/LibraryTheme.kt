package ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import ui.resources.Res
import ui.resources.inter_medium
import ui.resources.inter_regular
import ui.resources.inter_semibold

/** Application-owned tokens from Design.md, not official Yandex tokens. */
internal object LibraryDesign {
    val primary = Color(0xFF242432)
    val primaryHover = Color(0xFF101018)
    val primaryContainer = Color(0xFFEBEBF2)
    val background = Color(0xFFF7F7FA)
    val surface = Color.White
    val secondaryText = Color(0xFF626272)
    val control = Color(0xFFF4F4F8)
    val controlHover = Color(0xFFEAEAF1)
    val outline = Color(0xFF858594)
    val divider = Color(0xFFE5E5EC)
    val success = Color(0xFF1E6A43)
    val successContainer = Color(0xFFE9F7EF)
    val warning = Color(0xFF7A4B00)
    val warningContainer = Color(0xFFFFF3DC)
    val error = Color(0xFFA12323)
    val errorContainer = Color(0xFFFFF0F0)
    val disabled = Color(0xFFEDEDF1)
    val navigationLabels = listOf("Читатели", "Издания", "Выдачи", "Архитектура")
    const val windowWidthDp = 1180
    const val windowHeightDp = 780
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun LibraryTheme(content: @Composable () -> Unit) {
    val inter = FontFamily(
        Font(Res.font.inter_regular, weight = FontWeight.Normal),
        Font(Res.font.inter_medium, weight = FontWeight.Medium),
        Font(Res.font.inter_semibold, weight = FontWeight.SemiBold),
    )
    val colors = lightColorScheme(
        primary = LibraryDesign.primary,
        onPrimary = Color.White,
        primaryContainer = LibraryDesign.primaryContainer,
        onPrimaryContainer = LibraryDesign.primary,
        secondary = LibraryDesign.secondaryText,
        onSecondary = Color.White,
        secondaryContainer = LibraryDesign.control,
        onSecondaryContainer = LibraryDesign.primary,
        tertiary = LibraryDesign.success,
        onTertiary = Color.White,
        tertiaryContainer = LibraryDesign.successContainer,
        onTertiaryContainer = LibraryDesign.success,
        error = LibraryDesign.error,
        onError = Color.White,
        errorContainer = LibraryDesign.errorContainer,
        onErrorContainer = LibraryDesign.error,
        background = LibraryDesign.background,
        onBackground = LibraryDesign.primary,
        surface = LibraryDesign.surface,
        onSurface = LibraryDesign.primary,
        surfaceVariant = LibraryDesign.control,
        onSurfaceVariant = LibraryDesign.secondaryText,
        outline = LibraryDesign.outline,
        outlineVariant = LibraryDesign.divider,
        surfaceTint = Color.White,
    )
    // The design owns hover colors; a second Material hover overlay would alter the tokens.
    CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(
        rippleAlpha = RippleAlpha(draggedAlpha = 0.16f, focusedAlpha = 0.12f, hoveredAlpha = 0f, pressedAlpha = 0.12f),
    )) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography(
                headlineLarge = TextStyle(fontFamily = inter, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
                headlineMedium = TextStyle(fontFamily = inter, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
                titleLarge = TextStyle(fontFamily = inter, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
                titleMedium = TextStyle(fontFamily = inter, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
                titleSmall = TextStyle(fontFamily = inter, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
                bodyLarge = TextStyle(fontFamily = inter, fontSize = 16.sp, lineHeight = 24.sp),
                bodyMedium = TextStyle(fontFamily = inter, fontSize = 16.sp, lineHeight = 24.sp),
                bodySmall = TextStyle(fontFamily = inter, fontSize = 14.sp, lineHeight = 20.sp),
                labelLarge = TextStyle(fontFamily = inter, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
                labelMedium = TextStyle(fontFamily = inter, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
                labelSmall = TextStyle(fontFamily = inter, fontSize = 12.sp, lineHeight = 16.sp),
            ),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(24.dp),
                extraLarge = RoundedCornerShape(24.dp),
            ),
            content = content,
        )
    }
}
