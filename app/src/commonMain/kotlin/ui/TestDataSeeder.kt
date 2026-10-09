package ui

import domain.Book
import domain.LibraryEntityFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import persistence.LibraryRepositoryBundle
import kotlin.random.Random

internal interface TestDataSource {
    suspend fun addBooks(): Int
    suspend fun addReaders(): Int
}

/** Explicit, additive test-data actions. Never called during application startup. */
internal class TestDataSeeder(
    private val repositories: LibraryRepositoryBundle,
    private val random: Random = Random.Default,
) : TestDataSource {
    private val factory = LibraryEntityFactory()
    private val mutex = Mutex()

    override suspend fun addBooks(): Int = mutex.withLock {
        val existing = repositories.publications.publicationsOnce().filterIsInstance<Book>()
            .mapNotNull { TestBookCatalogue.find(it.isbn)?.isbn }.toSet()
        val samples = TestBookCatalogue.books.filterNot { it.isbn in existing }.shuffled(random).take(3)
        if (samples.isEmpty()) return@withLock 0
        val branch = repositories.branches.branches.first().firstOrNull()
            ?: factory.branch("Тестовый филиал", "Учебный адрес").also { repositories.branches.save(it) }
        if (repositories.librarians.librarians.first().isEmpty()) {
            repositories.librarians.save(factory.librarian("Тестовый библиотекарь", "Учебная демонстрация", branch.id))
        }
        for (sample in samples) {
            repositories.publications.save(factory.book(sample.title, sample.authors, "Питер", sample.year,
                random.nextInt(2, 6), branch.id, sample.isbn))
        }
        samples.size
    }

    override suspend fun addReaders(): Int = mutex.withLock {
        val existing = repositories.readers.readersOnce().map { it.fullName }.toSet()
        val names = readerNames.filterNot { it in existing }.shuffled(random).take(5)
        for (name in names) {
            repositories.readers.save(factory.reader(name, random.nextInt(1980, 2008),
                "Учебная улица, дом ${random.nextInt(1, 90)}", "Тестовые данные"))
        }
        names.size
    }

    private val readerNames = listOf(
        "Анна Соколова", "Илья Петров", "Мария Волкова", "Дмитрий Кузнецов", "Екатерина Орлова",
        "Алексей Морозов", "Ольга Смирнова", "Павел Крылов", "Дарья Новикова", "Никита Белов",
        "София Лебедева", "Артём Васильев", "Полина Романова", "Михаил Попов", "Елена Зайцева",
        "Роман Громов", "Алиса Фёдорова", "Максим Сидоров", "Вера Макарова", "Сергей Егоров",
    )
}
