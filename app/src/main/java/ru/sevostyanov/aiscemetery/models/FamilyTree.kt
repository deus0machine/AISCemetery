package ru.sevostyanov.aiscemetery.models

data class FamilyTree(
    val id: Long,
    val name: String,
    val ownerId: Long,
    val isPublic: Boolean,
    val description: String? = null,
    val createdAt: String,
    val updatedAt: String
)

// Модель для прав доступа к древу
data class TreeAccess(
    val treeId: Long,
    val userId: Long,
    val accessLevel: AccessLevel,
    val status: AccessStatus
)

enum class AccessLevel {
    OWNER,      // Полный доступ
    EDITOR,     // Может редактировать, но не может менять права доступа
    VIEWER      // Только просмотр
}

enum class AccessStatus {
    PENDING,    // Ожидает подтверждения
    APPROVED,   // Подтверждено
    REJECTED    // Отклонено
} 