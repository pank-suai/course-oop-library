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
            Text("Course OOP Library", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            TabRow(tab) {
                listOf("Readers", "Publications", "Loans", "Architecture").forEachIndexed { index, title ->
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
                label = { Text("Search readers by name") },
            )
        }
        item {
            FormRow {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, Modifier.weight(1f), label = { Text("Full name") })
                    OutlinedTextField(year, { year = it }, Modifier.weight(1f), label = { Text("Birth year") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(address, { address = it }, Modifier.weight(1f), label = { Text("Address") })
                    OutlinedTextField(organization, { organization = it }, Modifier.weight(1f), label = { Text("Organization") })
                }
                Button(onClick = { vm.add(name, year, address, organization) }) { Text("Add reader") }
            }
        }
        item { Status(state.isLoading, state.error, state.items.isEmpty(), "No readers yet") }
        items(state.items) { reader ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${reader.fullName} · ${reader.organization}", Modifier.weight(1f).padding(4.dp))
                    TextButton(onClick = { vm.delete(reader) }) { Text("Delete") }
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
                label = { Text("Search publications by title or author") },
            )
        }
        item {
            FormRow {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, Modifier.weight(1f), label = { Text("Title") })
                    OutlinedTextField(authors, { authors = it }, Modifier.weight(1f), label = { Text("Authors / editors") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(publisher, { publisher = it }, Modifier.weight(1f), label = { Text("Publisher") })
                    OutlinedTextField(year, { year = it }, Modifier.weight(1f), label = { Text("Publication year") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(copies, { copies = it }, Modifier.weight(1f), label = { Text("Copies") })
                    OutlinedTextField(isbn, { isbn = it }, Modifier.weight(1f), label = { Text("ISBN (optional)") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Branch", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            branches.items.forEach { branch ->
                                TextButton(onClick = { selectedBranch = branch }) { Text(branch.name) }
                            }
                        }
                        Text(selectedBranch?.name ?: "No branch selected")
                    }
                    OutlinedTextField(issue, { issue = it }, Modifier.weight(1f), label = { Text("Periodical issue number") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.addBook(title, authors, publisher, year, copies, selectedBranch?.id ?: "", isbn) }) { Text("Add book") }
                    Button(onClick = { vm.addPeriodical(title, authors, publisher, year, issue, copies, selectedBranch?.id ?: "") }) { Text("Add periodical") }
                }
            }
        }
        item { Status(state.isLoading, state.error ?: branches.error, state.items.isEmpty(), "No publications yet") }
        items(state.items) { publication ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${publication.title} · ${publication.authors} · ${publication.availableCopies}/${publication.totalCopies}", Modifier.weight(1f).padding(4.dp))
                    TextButton(onClick = { vm.delete(publication) }) { Text("Delete") }
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
                Text("Choose a reader by name and a publication by title; technical IDs stay inside the application.")
                Text("Readers", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    readers.forEach { reader ->
                        TextButton(onClick = { selectedReader = reader }) { Text(reader.fullName) }
                    }
                }
                Text("Publications", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    publications.filter { it.availableCopies > 0 }.forEach { publication ->
                        TextButton(onClick = { selectedPublication = publication }) { Text(publication.title) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(selectedReader?.fullName ?: "No reader selected")
                    Text(selectedPublication?.title ?: "No publication selected")
                    Button(onClick = { vm.issue(selectedReader, selectedPublication) }) { Text("Issue") }
                }
            }
        }
        item { Status(state.isLoading, state.error, state.items.isEmpty(), "No loans yet") }
        items(state.items) { row ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${row.readerName} · ${row.publicationTitle}")
                    if (row.loan.state !is LoanState.Returned) Button(onClick = { vm.returnLoan(row.loan) }) { Text("Return") }
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
                    Text("Inheritance: LibraryEntity → Person → Reader / Librarian")
                    Text("Inheritance: LibraryEntity → Publication → Book / Periodical")
                    Text("Reader ↔ Publication: many-to-many through Loan")
                    Text("Room pipeline: Compose → ViewModel → LibraryService → Repository → DAO → SQLite")
                    Text("Patterns: Repository · MVVM · Strategy · Factory · Observer · State · Facade")
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Application data", style = MaterialTheme.typography.titleMedium)
                    Text("Seed is idempotent. Clear all is transactional and removes loans before dependent records.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = !state.isBusy, onClick = vm::seedDemo) { Text("Seed demo data") }
                        Button(enabled = !state.isBusy, onClick = vm::clearAll) { Text("Clear all data") }
                    }
                    when {
                        state.isBusy -> Text("Working…")
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
        loading -> Text("Loading…")
        error != null -> Text(error, color = MaterialTheme.colorScheme.error)
        empty -> Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
