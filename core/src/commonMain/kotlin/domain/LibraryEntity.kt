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
    require(value.isNotBlank()) { "$field must not be blank" }
}

fun validatePublicationCopies(totalCopies: Int, availableCopies: Int) {
    require(totalCopies > 0) { "totalCopies must be positive" }
    require(availableCopies in 0..totalCopies) { "availableCopies must be between 0 and totalCopies" }
}
