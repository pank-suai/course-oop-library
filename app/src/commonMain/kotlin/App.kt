import androidx.compose.runtime.Composable
import di.ApplicationContainer
import ui.App as LibraryApp

@Composable
fun App(container: ApplicationContainer) {
    LibraryApp(container)
}
