package ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import di.ApplicationContainer
import domain.LoanState
import domain.Publication
import domain.Reader

@Composable
fun App(container: ApplicationContainer) {
    val readers = remember(container) { ReadersViewModel(container.repositories.readers, service = container.service) }
    val publications = remember(container) {
        PublicationsViewModel(container.repositories.publications, container.repositories.branches, service = container.service)
    }
    val loans = remember(container) {
        LoansViewModel(container.service, container.repositories.loans, container.repositories.readers, container.repositories.publications, container.repositories.librarians)
    }
    val administration = remember(container) { AdministrationViewModel(container.service) }
    DisposableEffect(container) {
        container.register(readers)
        container.register(publications)
        container.register(loans)
        container.register(administration)
        onDispose {
            readers.close()
            publications.close()
            loans.close()
            administration.close()
        }
    }
    LibraryScreen(readers, publications, loans, administration)
}

@Composable
private fun LibraryScreen(
    readersVm: ReadersViewModel,
    publicationsVm: PublicationsViewModel,
    loansVm: LoansViewModel,
    administrationVm: AdministrationViewModel,
) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Библиотека курса ООП", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            TabRow(tab) {
                listOf("Читатели", "Издания", "Выдачи", "Архитектура").forEachIndexed { index, title ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
                }
            }
            Spacer(Modifier.height(12.dp))
            when (tab) {
                0 -> ReadersPane(readersVm)
                1 -> PublicationsPane(publicationsVm)
                2 -> LoansPane(loansVm, readersVm.state.collectAsState().value.items, publicationsVm.state.collectAsState().value.items)
                else -> ArchitecturePane(administrationVm)
            }
        }
    }
}

@Composable
private fun ReadersPane(vm: ReadersViewModel) {
    val state by vm.state.collectAsState()
    val query by vm.query.collectAsState()
    var name by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск читателей по имени") },
            )
        }
        item {
            FormRow {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, Modifier.weight(1f), label = { Text("Полное имя") })
                    OutlinedTextField(year, { year = it }, Modifier.weight(1f), label = { Text("Год рождения") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(address, { address = it }, Modifier.weight(1f), label = { Text("Адрес") })
                    OutlinedTextField(organization, { organization = it }, Modifier.weight(1f), label = { Text("Организация") })
                }
                Button(onClick = { vm.add(name, year, address, organization) }) { Text("Добавить читателя") }
            }
        }
        item { Status(state.isLoading, state.error, state.items.isEmpty(), "Читателей пока нет") }
        items(state.items) { reader ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${reader.fullName} · ${reader.organization}", Modifier.weight(1f).padding(4.dp))
                    TextButton(onClick = { vm.delete(reader) }) { Text("Удалить") }
                }
            }
        }
    }
}

@Composable
private fun PublicationsPane(vm: PublicationsViewModel) {
    val state by vm.state.collectAsState()
    val query by vm.query.collectAsState()
    val branches by vm.branches.collectAsState()
    var title by remember { mutableStateOf("") }
    var authors by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var copies by remember { mutableStateOf("") }
    var selectedBranch by remember { mutableStateOf<domain.LibraryBranch?>(null) }
    var isbn by remember { mutableStateOf("") }
    var issue by remember { mutableStateOf("1") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск изданий по названию или автору") },
            )
        }
        item {
            FormRow {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, Modifier.weight(1f), label = { Text("Название") })
                    OutlinedTextField(authors, { authors = it }, Modifier.weight(1f), label = { Text("Авторы / редакторы") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(publisher, { publisher = it }, Modifier.weight(1f), label = { Text("Издатель") })
                    OutlinedTextField(year, { year = it }, Modifier.weight(1f), label = { Text("Год издания") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(copies, { copies = it }, Modifier.weight(1f), label = { Text("Экземпляры") })
                    OutlinedTextField(isbn, { isbn = it }, Modifier.weight(1f), label = { Text("ISBN (необязательно)") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Филиал", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            branches.items.forEach { branch ->
                                TextButton(onClick = { selectedBranch = branch }) { Text(branch.name) }
                            }
                        }
                        Text(selectedBranch?.name ?: "Филиал не выбран")
                    }
                    OutlinedTextField(issue, { issue = it }, Modifier.weight(1f), label = { Text("Номер выпуска") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.addBook(title, authors, publisher, year, copies, selectedBranch?.id ?: "", isbn) }) { Text("Добавить книгу") }
                    Button(onClick = { vm.addPeriodical(title, authors, publisher, year, issue, copies, selectedBranch?.id ?: "") }) { Text("Добавить периодическое издание") }
                }
            }
        }
        item { Status(state.isLoading, state.error ?: branches.error, state.items.isEmpty(), "Изданий пока нет") }
        items(state.items) { publication ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${publication.title} · ${publication.authors} · ${publication.availableCopies}/${publication.totalCopies}", Modifier.weight(1f).padding(4.dp))
                    TextButton(onClick = { vm.delete(publication) }) { Text("Удалить") }
                }
            }
        }
    }
}

@Composable
private fun LoansPane(vm: LoansViewModel, readers: List<Reader>, publications: List<Publication>) {
    val state by vm.state.collectAsState()
    var selectedReader by remember { mutableStateOf<Reader?>(null) }
    var selectedPublication by remember { mutableStateOf<Publication?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Выберите читателя по имени и издание по названию; технические идентификаторы скрыты внутри приложения.")
                Text("Читатели", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    readers.forEach { reader ->
                        TextButton(onClick = { selectedReader = reader }) { Text(reader.fullName) }
                    }
                }
                Text("Издания", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    publications.filter { it.availableCopies > 0 }.forEach { publication ->
                        TextButton(onClick = { selectedPublication = publication }) { Text(publication.title) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(selectedReader?.fullName ?: "Читатель не выбран")
                    Text(selectedPublication?.title ?: "Издание не выбрано")
                    Button(onClick = { vm.issue(selectedReader, selectedPublication) }) { Text("Выдать") }
                }
            }
        }
        item { Status(state.isLoading, state.error, state.items.isEmpty(), "Выдач пока нет") }
        items(state.items) { row ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${row.readerName} · ${row.publicationTitle}")
                    if (row.loan.state !is LoanState.Returned) Button(onClick = { vm.returnLoan(row.loan) }) { Text("Вернуть") }
                }
            }
        }
    }
}

@Composable
private fun ArchitecturePane(vm: AdministrationViewModel) {
    val state by vm.state.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("ООП-архитектура", style = MaterialTheme.typography.titleLarge)
                    Text("Наследование: LibraryEntity → Person → Reader / Librarian")
                    Text("Наследование: LibraryEntity → Publication → Book / Periodical")
                    Text("Читатель ↔ издание: связь многие-ко-многим через Loan")
                    Text("Конвейер Room: Compose → ViewModel → LibraryService → Repository → DAO → SQLite")
                    Text("Паттерны: Repository · MVVM · Strategy · Factory · Observer · State · Facade")
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Данные приложения", style = MaterialTheme.typography.titleMedium)
                    Text("Заполнение демонстрационными данными идемпотентно. Очистка транзакционна и удаляет выдачи перед зависимыми записями.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !state.isBusy, onClick = vm::seedDemo) { Text("Добавить демоданные") }
                        Button(enabled = !state.isBusy, onClick = vm::clearAll) { Text("Очистить все данные") }
                    }
                    when {
                        state.isBusy -> Text("Выполняется…")
                        state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
                        state.message != null -> Text(state.message!!)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormRow(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) { content() }
}

@Composable
private fun Status(loading: Boolean, error: String?, empty: Boolean, emptyText: String) {
    when {
        loading -> Text("Загрузка…")
        error != null -> Text(error, color = MaterialTheme.colorScheme.error)
        empty -> Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
