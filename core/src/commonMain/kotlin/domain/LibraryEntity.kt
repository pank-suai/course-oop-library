@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package domain

import kotlin.uuid.Uuid

/** Root contract for all domain entities. IDs are generated internally and opaque to users. */
interface LibraryEntity {
    val id: String
}

interface IdGenerator {
    fun nextId(): String
}

class UuidIdGenerator : IdGenerator {
    override fun nextId(): String = Uuid.random().toString()
}

interface Person : LibraryEntity {
    val fullName: String
}

interface Publication : LibraryEntity {
    val title: String
    val authors: String
    val publisher: String
    val publicationYear: Int
    val totalCopies: Int
    val availableCopies: Int
    val branchId: String
}

fun validateText(value: String, field: String) {
    val label = mapOf(
        "fullName" to "Имя",
        "address" to "Адрес",
        "organization" to "Организация",
        "position" to "Должность",
        "branchId" to "Филиал",
        "name" to "Название",
        "title" to "Название",
        "authors" to "Авторы",
        "publisher" to "Издатель",
        "readerId" to "Читатель",
        "publicationId" to "Издание",
        "librarianId" to "Библиотекарь",
    )[field] ?: field
    require(value.isNotBlank()) { "$label не должно быть пустым" }
}

fun validatePublicationCopies(totalCopies: Int, availableCopies: Int) {
    require(totalCopies > 0) { "Общее число экземпляров должно быть положительным" }
    require(availableCopies in 0..totalCopies) { "Доступное число экземпляров должно быть от 0 до общего числа" }
}
