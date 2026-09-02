import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import di.ApplicationContainer
import ui.App

fun main() {
    val container = ApplicationContainer()
    application {
        Window(
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
