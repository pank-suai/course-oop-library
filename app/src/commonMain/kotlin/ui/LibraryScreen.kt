package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import di.ApplicationContainer
import domain.*
import kotlinx.coroutines.flow.catch

internal fun resolveAvailableSelection(selectedId: String?, availableIds: Set<String>): String? =
    selectedId?.takeIf(availableIds::contains)

@Composable
fun App(container: ApplicationContainer) {
    val readers = remember(container) { ReadersViewModel(container.repositories.readers, service = container.service) }
    val publications = remember(container) {
        PublicationsViewModel(container.repositories.publications, container.repositories.branches, service = container.service)
    }
    val loans = remember(container) {
        LoansViewModel(container.service, container.repositories.loans, container.repositories.readers,
            container.repositories.publications, container.repositories.librarians)
    }
    val administration = remember(container) { AdministrationViewModel(container.service) }
    val testData = remember(container) { TestDataViewModel(TestDataSeeder(container.repositories)) }
    val readerOptions = remember(container) { container.repositories.readers.readers.catch { emit(emptyList()) } }
    val publicationOptions = remember(container) { container.repositories.publications.publications.catch { emit(emptyList()) } }
    val allReaders by readerOptions.collectAsState(emptyList())
    val allPublications by publicationOptions.collectAsState(emptyList())
    DisposableEffect(container) {
        container.register(readers)
        container.register(publications)
        container.register(loans)
        container.register(administration)
        container.register(testData)
        onDispose {
            readers.close()
            publications.close()
            loans.close()
            administration.close()
            testData.close()
        }
    }
    LibraryTheme {
        var tab by remember { mutableIntStateOf(0) }
        BoxWithConstraints(Modifier.fillMaxSize().background(LibraryDesign.background)) {
            val narrow = maxWidth < 960.dp
            val navigationWidth = 208.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
            val pagePadding = if (maxWidth < 1080.dp) 24.dp else 32.dp
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LibraryNavigation(tab, navigationWidth) { tab = it }
                Surface(Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(24.dp), color = LibraryDesign.surface) {
                    when (tab) {
                        0 -> ReadersPane(readers, testData, pagePadding, narrow)
                        1 -> PublicationsPane(publications, testData, pagePadding, narrow)
                        2 -> LoansPane(loans, allReaders, allPublications, pagePadding, narrow)
                        else -> ArchitecturePane(administration, pagePadding)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryNavigation(selected: Int, width: Dp, onSelected: (Int) -> Unit) {
    val icons = listOf(LibraryIcon.Group, LibraryIcon.MenuBook, LibraryIcon.SwapHoriz, LibraryIcon.AccountTree)
    Surface(Modifier.width(width).fillMaxHeight(), shape = RoundedCornerShape(24.dp), color = LibraryDesign.surface) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Glyph(LibraryIcon.LibraryBooks)
                Text("Библиотека", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(24.dp))
            LibraryDesign.navigationLabels.forEachIndexed { index, title ->
                val active = selected == index
                val interaction = remember { MutableInteractionSource() }
                val hovered by interaction.collectIsHoveredAsState()
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).focusRing()
                        .hoverable(interaction)
                        .selectable(active, role = Role.Tab, onClick = { onSelected(index) }),
                    shape = RoundedCornerShape(16.dp),
                    color = if (active) LibraryDesign.primaryContainer else if (hovered) LibraryDesign.control else Color.Transparent,
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Glyph(icons[index], tint = if (active) LibraryDesign.primary else LibraryDesign.secondaryText)
                        Text(title, style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                        if (active) Box(Modifier.width(3.dp).height(20.dp).background(LibraryDesign.primary, RoundedCornerShape(2.dp)))
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.weight(1f))
            HorizontalDivider(color = LibraryDesign.divider)
            Spacer(Modifier.height(16.dp))
            Text("Данные на компьютере", style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
        }
    }
}

@Composable
private fun PaneHeader(title: String, subtitle: String, action: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 650.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HeaderText(title, subtitle)
                action()
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f)) { HeaderText(title, subtitle) }
                action()
            }
        }
    }
}

@Composable
private fun HeaderText(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
}

@Composable
private fun Catalogue(padding: Dp, content: LazyListScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(padding),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

@Composable
private fun ReadersPane(vm: ReadersViewModel, testData: TestDataViewModel, padding: Dp, narrow: Boolean) {
    val testState = testData.state.collectAsState().value
    val testMessage = testData.message.collectAsState().value
    val state by vm.state.collectAsState()
    val save by vm.saveState.collectAsState()
    val query by vm.query.collectAsState()
    var editor by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf<Reader?>(null) }
    Catalogue(padding) {
        item {
            PaneHeader("Читатели", "Регистрация и поиск читателей") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CatalogButton("Добавить читателя", LibraryIcon.Add) { editor = true }
                    SecondaryButton("Тестовые читатели", !testState.isSaving, testData::addReaders)
                }
            }
        }
        item { SearchField(query, vm::setSearchQuery, "Поиск по имени") }
        if (testState.error != null) item { Notice("Не удалось добавить тестовые данные", error = true) }
        testMessage?.takeIf { it.kind == "readers" }?.let { message -> item { Notice(message.text) } }
        if (state.error != null && !editor) item { Notice(state.error!!, error = true) }
        if (state.isLoading) item { Notice("Загружаем читателей", loading = true) }
        else if (state.items.isEmpty() && state.error == null) item {
            EmptyState(if (query.isBlank()) "Читателей пока нет" else "Ничего не найдено",
                if (query.isBlank()) "Добавьте первого читателя с помощью кнопки выше." else "Попробуйте другое имя или очистите поиск.",
                if (query.isNotBlank()) ({ vm.setSearchQuery("") }) else null)
        }
        if (!state.isLoading && state.items.isNotEmpty()) {
            item { ListHeading("Читатели", state.items.size, query.isNotBlank()) }
            items(state.items, key = { it.id }) { reader ->
                RecordRow {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(reader.fullName, style = MaterialTheme.typography.titleMedium)
                        Text("${reader.birthYear} · ${reader.organization}", style = MaterialTheme.typography.bodySmall,
                            color = LibraryDesign.secondaryText)
                        Text(reader.address, style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
                    }
                    QuietAction("Удалить", "Удалить читателя ${reader.fullName}", LibraryIcon.Delete) { deleted = reader }
                }
            }
        }
    }
    if (editor) ReaderEditor(vm, save, narrow) { editor = false }
    deleted?.let { reader ->
        ConfirmDialog("Удалить читателя?", "${reader.fullName}. Если у читателя есть история выдач, удаление будет запрещено.", "Удалить",
            onDismiss = { deleted = null }, onConfirm = { deleted = null; vm.delete(reader) })
    }
}

@Composable
private fun ReaderEditor(vm: ReadersViewModel, save: SaveUiState, narrow: Boolean, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }
    val revision = remember { save.revision }
    LaunchedEffect(save.revision) { if (save.revision > revision) onDismiss() }
    EditorDialog("Новый читатель", "Заполните сведения о читателе", save.isSaving, save.error, onDismiss,
        onSave = { vm.add(name, year, address, organization) }) {
        FieldsRow(narrow) {
            Field("Полное имя", name, { name = it }, Modifier.weight(1f), !save.isSaving)
            Field("Год рождения", year, { year = it }, Modifier.weight(1f), !save.isSaving)
        }
        Field("Адрес", address, { address = it }, enabled = !save.isSaving)
        Field("Организация", organization, { organization = it }, enabled = !save.isSaving)
    }
}

@Composable
private fun PublicationsPane(vm: PublicationsViewModel, testData: TestDataViewModel, padding: Dp, narrow: Boolean) {
    val testState = testData.state.collectAsState().value
    val testMessage = testData.message.collectAsState().value
    val state by vm.state.collectAsState()
    val save by vm.saveState.collectAsState()
    val branches by vm.branches.collectAsState()
    val query by vm.query.collectAsState()
    var editor by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf<Publication?>(null) }
    Catalogue(padding) {
        item {
            PaneHeader("Издания", "Книги и периодика в библиотечном фонде") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CatalogButton("Добавить издание", LibraryIcon.Add) { editor = true }
                    SecondaryButton("Тестовые книги", !testState.isSaving, testData::addBooks)
                }
            }
        }
        item { SearchField(query, vm::setSearchQuery, "Поиск по названию или автору") }
        if (testState.error != null) item { Notice("Не удалось добавить тестовые данные", error = true) }
        testMessage?.takeIf { it.kind == "books" }?.let { message -> item { Notice(message.text) } }
        val error = state.error ?: branches.error
        if (error != null && !editor) item { Notice(error, error = true) }
        if (state.isLoading) item { Notice("Загружаем издания", loading = true) }
        else if (state.items.isEmpty() && error == null) item {
            EmptyState(if (query.isBlank()) "Изданий пока нет" else "Ничего не найдено",
                if (query.isBlank()) "Добавьте книгу или периодическое издание." else "Попробуйте другое название или имя автора.",
                if (query.isNotBlank()) ({ vm.setSearchQuery("") }) else null)
        }
        if (!state.isLoading && state.items.isNotEmpty()) {
            item { ListHeading("Каталог", state.items.size, query.isNotBlank()) }
            items(state.items, key = { it.id }) { publication ->
                RecordRow {
                    PublicationCover(publication)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(publication.title, style = MaterialTheme.typography.titleMedium)
                        Text(publication.authors, style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
                        val kind = when (publication) {
                            is Book -> "Книга"
                            is Periodical -> "Периодика № ${publication.issueNumber}"
                            else -> "Издание"
                        }
                        Text("$kind · ${publication.publisher} · ${publication.publicationYear}",
                            style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
                        if (publication is Book && !publication.isbn.isNullOrBlank()) {
                            Text("ISBN ${publication.isbn}", style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
                        }
                        val branchName = branches.items.firstOrNull { it.id == publication.branchId }?.name ?: "Филиал недоступен"
                        Text("$branchName · Свободно ${publication.availableCopies} из ${publication.totalCopies}", style = MaterialTheme.typography.bodySmall)
                    }
                    QuietAction("Удалить", "Удалить издание ${publication.title}", LibraryIcon.Delete) { deleted = publication }
                }
            }
        }
    }
    if (editor) PublicationEditor(vm, save, branches.items, narrow) { editor = false }
    deleted?.let { publication ->
        ConfirmDialog("Удалить издание?", "«${publication.title}». Издание с историей выдач удалить нельзя.", "Удалить",
            onDismiss = { deleted = null }, onConfirm = { deleted = null; vm.delete(publication) })
    }
}

@Composable
private fun PublicationEditor(vm: PublicationsViewModel, save: SaveUiState, branches: List<LibraryBranch>, narrow: Boolean, onDismiss: () -> Unit) {
    var periodical by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var authors by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var copies by remember { mutableStateOf("") }
    var isbn by remember { mutableStateOf("") }
    var issue by remember { mutableStateOf("1") }
    var branchId by remember { mutableStateOf<String?>(null) }
    val validBranch = branches.firstOrNull { it.id == branchId }
    val revision = remember { save.revision }
    LaunchedEffect(save.revision) { if (save.revision > revision) onDismiss() }
    EditorDialog("Новое издание", "Библиографические сведения и место хранения", save.isSaving, save.error, onDismiss,
        saveEnabled = validBranch != null && !save.isSaving,
        onSave = {
            if (periodical) vm.addPeriodical(title, authors, publisher, year, issue, copies, validBranch?.id ?: "")
            else vm.addBook(title, authors, publisher, year, copies, validBranch?.id ?: "", isbn)
        }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !periodical, onClick = { periodical = false }, enabled = !save.isSaving,
                label = { Text("Книга") }, modifier = Modifier.heightIn(min = 48.dp))
            FilterChip(selected = periodical, onClick = { periodical = true }, enabled = !save.isSaving,
                label = { Text("Периодика") }, modifier = Modifier.heightIn(min = 48.dp))
        }
        Field("Название", title, { title = it }, enabled = !save.isSaving)
        Field(if (periodical) "Редакторы" else "Авторы", authors, { authors = it }, enabled = !save.isSaving)
        FieldsRow(narrow) {
            Field("Издательство", publisher, { publisher = it }, Modifier.weight(1f), !save.isSaving)
            Field("Год издания", year, { year = it }, Modifier.weight(1f), !save.isSaving)
        }
        FieldsRow(narrow) {
            Field("Экземпляры", copies, { copies = it }, Modifier.weight(1f), !save.isSaving)
            if (periodical) Field("Номер выпуска", issue, { issue = it }, Modifier.weight(1f), !save.isSaving)
            else Field("ISBN (необязательно)", isbn, { isbn = it }, Modifier.weight(1f), !save.isSaving)
        }
        ChoiceField("Филиал", validBranch?.name, branches.map { it.id to it.name }, !save.isSaving) { branchId = it }
        if (branches.isEmpty()) Notice("Филиалов пока нет. Загрузите демонстрационные данные в разделе «Архитектура».")
    }
}

@Composable
private fun LoansPane(vm: LoansViewModel, readers: List<Reader>, publications: List<Publication>, padding: Dp, narrow: Boolean) {
    val state by vm.state.collectAsState()
    val action by vm.actionState.collectAsState()
    var editor by remember { mutableStateOf(false) }
    Catalogue(padding) {
        item {
            PaneHeader("Выдачи", "Выдача, возврат и история пользования") {
                CatalogButton("Оформить выдачу", LibraryIcon.Add, enabled = !state.isLoading && !action.isSaving) { editor = true }
            }
        }
        if (state.error != null && !editor) item { Notice(state.error!!, error = true) }
        if (state.isLoading) item { Notice("Загружаем выдачи", loading = true) }
        else if (state.items.isEmpty() && state.error == null) item { EmptyState("Выдач пока нет", "Выберите читателя и доступное издание, чтобы оформить первую выдачу.") }
        if (!state.isLoading && state.items.isNotEmpty()) {
            item { ListHeading("История выдач", state.items.size) }
            items(state.items, key = { it.loan.id }) { row ->
                RecordRow {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(row.publicationTitle, style = MaterialTheme.typography.titleMedium)
                        Text(row.readerName, style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
                        Text("Вернуть до ${formatLoanDate(row.loan.dueAt)}", style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
                        LoanBadge(row.loan.state)
                    }
                    SecondaryButton("Вернуть", enabled = row.loan.state !is LoanState.Returned && !action.isSaving) { vm.returnLoan(row.loan) }
                }
            }
        }
    }
    if (editor) LoanEditor(vm, action, readers, publications, narrow) { editor = false }
}

@Composable
private fun LoanEditor(vm: LoansViewModel, action: SaveUiState, readers: List<Reader>, publications: List<Publication>, narrow: Boolean, onDismiss: () -> Unit) {
    var readerId by remember { mutableStateOf<String?>(null) }
    var publicationId by remember { mutableStateOf<String?>(null) }
    val available = publications.filter { it.availableCopies > 0 }
    val validReaderId = resolveAvailableSelection(readerId, readers.mapTo(mutableSetOf()) { it.id })
    val validPublicationId = resolveAvailableSelection(publicationId, available.mapTo(mutableSetOf()) { it.id })
    val reader = readers.firstOrNull { it.id == validReaderId }
    val publication = available.firstOrNull { it.id == validPublicationId }
    LaunchedEffect(validReaderId) { readerId = validReaderId }
    LaunchedEffect(validPublicationId) { publicationId = validPublicationId }
    val revision = remember { action.revision }
    LaunchedEffect(action.revision) { if (action.revision > revision) onDismiss() }
    EditorDialog("Новая выдача", "Выберите читателя и доступное издание", action.isSaving, action.error, onDismiss,
        saveLabel = "Выдать", saveEnabled = reader != null && publication != null && !action.isSaving,
        onSave = { vm.issue(reader, publication) }) {
        ChoiceField("Читатель", reader?.fullName, readers.map { it.id to it.fullName }, !action.isSaving) { readerId = it }
        ChoiceField("Издание", publication?.title, available.map { it.id to it.title }, !action.isSaving) { publicationId = it }
        if (readers.isEmpty()) Notice("Сначала зарегистрируйте читателя.")
        if (available.isEmpty()) Notice("Нет свободных экземпляров. Добавьте издание или оформите возврат.")
        if (publication != null) Text("Свободных экземпляров: ${publication.availableCopies}", style = MaterialTheme.typography.bodySmall,
            color = LibraryDesign.secondaryText)
        Text("Срок возврата определяется правилами библиотеки. Идентификаторы вводить не нужно.",
            style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
    }
}

@Composable
private fun LoanBadge(state: LoanState) {
    val label: String
    val background: Color
    val foreground: Color
    when (state) {
        is LoanState.Active -> { label = "На руках"; background = LibraryDesign.control; foreground = LibraryDesign.primary }
        is LoanState.Overdue -> { label = "Просрочено"; background = LibraryDesign.warningContainer; foreground = LibraryDesign.warning }
        is LoanState.Returned -> { label = "Возвращено"; background = LibraryDesign.successContainer; foreground = LibraryDesign.success }
    }
    Surface(color = background, shape = RoundedCornerShape(100.dp)) {
        Text(label, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall, color = foreground)
    }
}

@Composable
private fun ArchitecturePane(vm: AdministrationViewModel, padding: Dp) {
    val state by vm.state.collectAsState()
    var clearConfirmation by remember { mutableStateOf(false) }
    Catalogue(padding) {
        item { PaneHeader("Архитектура", "Как устроена библиотечная программа") {} }
        item {
            InformationBlock("Основные объекты", LibraryIcon.AccountTree) {
                Explanation("Люди", "У читателя и библиотекаря есть общие сведения о человеке. В программе: Person → Reader / Librarian.")
                Explanation("Издания", "Книга и периодика имеют общие поля, но отличаются дополнительными сведениями. В программе: Publication → Book / Periodical.")
                Explanation("Выдача", "Запись соединяет читателя и издание, хранит срок и состояние. Через несколько выдач читатели связаны с разными изданиями: Reader ↔ Loan ↔ Publication.")
            }
        }
        item {
            InformationBlock("Разделение обязанностей", LibraryIcon.Info) {
                Text("Экран показывает сведения, модель представления обрабатывает действия, служба выполняет библиотечные операции, а хранилище работает с базой.")
                Text("Compose → ViewModel → LibraryService → Repository → DAO → SQLite", style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
                Explanation("Шаблоны проектирования", "Repository, MVVM, Strategy, Factory, Observer, State и Facade помогают разделить эти задачи и не смешивать правила выдачи с оформлением окна.")
            }
        }
        item {
            InformationBlock("Демонстрационные данные", LibraryIcon.LibraryBooks) {
                Text("Учебный набор содержит филиал, библиотекаря, читателей, книгу, периодику и выдачу. Повторная загрузка не создаёт дубликаты.")
                CatalogButton("Добавить демоданные", LibraryIcon.Add, !state.isBusy, vm::seedDemo)
                when {
                    state.isBusy -> Notice("Выполняем операцию", loading = true)
                    state.error != null -> Notice(state.error!!, error = true)
                    state.message != null -> Notice(state.message!!)
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, LibraryDesign.divider)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Очистка учебной базы", style = MaterialTheme.typography.titleMedium)
                    Text("Удалит читателей, издания, сотрудников, филиалы и историю выдач. Это действие нельзя отменить.",
                        style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
                    QuietAction("Очистить данные", "Очистить все данные библиотеки", LibraryIcon.Delete, !state.isBusy) { clearConfirmation = true }
                }
            }
        }
    }
    if (clearConfirmation) ConfirmDialog("Очистить базу?", "Будут удалены все записи библиотеки, включая историю выдач. Отменить это действие нельзя.", "Очистить",
        onDismiss = { clearConfirmation = false }, onConfirm = { clearConfirmation = false; vm.clearAll() })
}

@Composable
private fun InformationBlock(title: String, icon: LibraryIcon, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = LibraryDesign.control, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Glyph(icon)
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            content()
        }
    }
}

@Composable
private fun Explanation(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
    }
}

@Composable
private fun RecordRow(content: @Composable RowScope.() -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).hoverable(interaction)
            .background(if (hovered) LibraryDesign.control else Color.Transparent, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp), content = content)
        HorizontalDivider(color = LibraryDesign.divider)
    }
}

@Composable
private fun ListHeading(label: String, count: Int, filtered: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = LibraryDesign.secondaryText)
        Text(if (filtered) "Найдено: $count" else "Записей: $count", style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, label: String) {
    OutlinedTextField(query, onQuery, modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }, label = { Text(label) }, singleLine = true,
        shape = RoundedCornerShape(12.dp), leadingIcon = { Glyph(LibraryIcon.Search, tint = LibraryDesign.secondaryText) },
        trailingIcon = if (query.isNotEmpty()) ({ IconButton(onClick = { onQuery("") }, modifier = Modifier.focusRing()) {
            Glyph(LibraryIcon.Close, "Очистить поиск")
        } }) else null)
}

@Composable
private fun Field(label: String, value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedTextField(value, onValue, modifier.fillMaxWidth().semantics { contentDescription = label }, enabled = enabled, label = { Text(label) }, singleLine = true,
        shape = RoundedCornerShape(12.dp))
}

@Composable
private fun FieldsRow(narrow: Boolean, content: @Composable RowScope.() -> Unit) {
    if (narrow) {
        FlowRow(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 1, content = content)
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun ChoiceField(label: String, selected: String?, options: List<Pair<String, String>>, enabled: Boolean = true, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled && options.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).focusRing(12.dp), shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(12.dp)) {
                Text(selected ?: "Выберите: ${label.lowercase()}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Glyph(LibraryIcon.ChevronRight, tint = LibraryDesign.secondaryText)
            }
            DropdownMenu(expanded, { expanded = false }, modifier = Modifier.widthIn(max = 500.dp).heightIn(max = 280.dp),
                containerColor = LibraryDesign.surface) {
                options.forEach { (id, title) ->
                    DropdownMenuItem(text = { Text(title, style = MaterialTheme.typography.bodySmall) },
                        onClick = { expanded = false; onSelected(id) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
        }
    }
}

@Composable
private fun EditorDialog(title: String, subtitle: String, busy: Boolean, error: String?, onDismiss: () -> Unit,
                         saveLabel: String = "Сохранить", saveEnabled: Boolean = !busy, onSave: () -> Unit,
                         content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(Modifier.width(600.dp).heightIn(max = 700.dp), shape = RoundedCornerShape(24.dp), color = LibraryDesign.surface) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
                    }
                    IconButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.focusRing()) { Glyph(LibraryIcon.Close, "Закрыть форму") }
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    content()
                    if (error != null) Notice(error, error = true)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    SecondaryButton("Отмена", enabled = !busy, onClick = onDismiss)
                    CatalogButton(if (busy) "Сохраняем…" else saveLabel, enabled = saveEnabled && !busy, onClick = onSave)
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, description: String, action: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(description) },
        confirmButton = { CatalogButton(action, onClick = onConfirm) },
        dismissButton = { SecondaryButton("Отмена", onClick = onDismiss) },
        containerColor = LibraryDesign.surface, shape = RoundedCornerShape(24.dp))
}

@Composable
private fun CatalogButton(label: String, icon: LibraryIcon? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Button(onClick, enabled = enabled, interactionSource = interaction,
        modifier = Modifier.heightIn(min = 48.dp).hoverable(interaction, enabled).focusRing(), shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (hovered) LibraryDesign.primaryHover else LibraryDesign.primary, contentColor = Color.White,
            disabledContainerColor = LibraryDesign.disabled, disabledContentColor = LibraryDesign.secondaryText)) {
        if (icon != null) { Glyph(icon, tint = if (enabled) Color.White else LibraryDesign.secondaryText); Spacer(Modifier.width(8.dp)) }
        Text(label)
    }
}

@Composable
private fun SecondaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Button(onClick, enabled = enabled, interactionSource = interaction,
        modifier = Modifier.heightIn(min = 48.dp).hoverable(interaction, enabled).focusRing(), shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (hovered) LibraryDesign.controlHover else LibraryDesign.control, contentColor = LibraryDesign.primary,
            disabledContainerColor = LibraryDesign.disabled, disabledContentColor = LibraryDesign.secondaryText)) { Text(label) }
}

@Composable
private fun QuietAction(label: String, description: String, icon: LibraryIcon, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(onClick, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp).focusRing().semantics { contentDescription = description },
        colors = ButtonDefaults.textButtonColors(contentColor = LibraryDesign.secondaryText)) {
        Glyph(icon, tint = LibraryDesign.secondaryText)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Glyph(icon: LibraryIcon, description: String? = null, tint: Color = LibraryDesign.primary) {
    Icon(icon.vector, contentDescription = description, modifier = Modifier.size(24.dp), tint = tint)
}

@Composable
private fun Modifier.focusRing(radius: Dp = 16.dp): Modifier {
    var focused by remember { mutableStateOf(false) }
    return onFocusChanged { focused = it.isFocused }
        .border(if (focused) 2.dp else 0.dp, if (focused) LibraryDesign.primary else Color.Transparent, RoundedCornerShape(radius))
}

@Composable
private fun Notice(text: String, loading: Boolean = false, error: Boolean = false) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = if (error) LibraryDesign.errorContainer else LibraryDesign.control) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Glyph(if (error) LibraryIcon.Warning else LibraryIcon.Info, tint = if (error) LibraryDesign.error else LibraryDesign.secondaryText)
            Text(text, style = MaterialTheme.typography.bodySmall, color = if (error) LibraryDesign.error else LibraryDesign.secondaryText)
        }
    }
}

@Composable
private fun EmptyState(title: String, description: String, clearSearch: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Glyph(LibraryIcon.Search, tint = LibraryDesign.secondaryText)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(description, style = MaterialTheme.typography.bodySmall, color = LibraryDesign.secondaryText)
        if (clearSearch != null) SecondaryButton("Очистить поиск", onClick = clearSearch)
    }
}
