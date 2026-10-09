import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import di.ApplicationContainer
import ui.App
import ui.LibraryDesign

fun main() {
    val container = ApplicationContainer()
    application {
        Window(
            state = rememberWindowState(
                width = LibraryDesign.windowWidthDp.dp,
                height = LibraryDesign.windowHeightDp.dp,
            ),
            onCloseRequest = {
                container.close()
                exitApplication()
            },
            title = "Библиотека курса ООП",
        ) {
            App(container)
        }
    }
}
