package ui

internal data class TestBook(val title: String, val authors: String, val year: Int, val isbn: String, val coverKey: String)

internal object TestBookCatalogue {
    val books = listOf(
        TestBook("Чистый код: создание, анализ и рефакторинг. Библиотека программиста", "Мартин Р.", 2025, "978-5-4461-0960-9", "clean_code"),
        TestBook("Head First. Kotlin", "Гриффитс Д.", 2023, "978-5-4461-1335-4", "kotlin"),
        TestBook("Паттерны проектирования API", "Гивакс Д.", 2026, "978-5-4461-1984-4", "api_patterns"),
        TestBook("Паттерны проектирования Rust", "Мэтьюз Б.", 2026, "978-5-4461-4389-4", "rust_patterns"),
        TestBook("Паттерны проектирования на платформе .NET", "Тепляков С. В.", 2021, "978-5-4461-0843-5", "dotnet_patterns"),
        TestBook("Head First. Паттерны проектирования. 2-е издание", "Фримен Э., Робсон Э., Сьерра К., Бейтс Б.", 2025, "978-5-4461-1819-9", "headfirst_patterns"),
    )
    fun find(isbn: String?): TestBook? = isbn?.let { value -> books.firstOrNull { it.isbn == value || it.isbn.replace("-", "") == value } }
}
