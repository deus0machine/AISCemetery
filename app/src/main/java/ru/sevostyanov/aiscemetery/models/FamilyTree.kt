package ru.sevostyanov.aiscemetery.models

import com.google.gson.annotations.SerializedName

data class FamilyTree(
    @SerializedName("id")
    val id: Long? = null,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("owner")
    val ownerId: Long? = null,
    
    @SerializedName("isPublic")
    val isPublic: Boolean = false,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class MemorialRelation(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("familyTreeId")
    val familyTreeId: Long,
    
    @SerializedName("sourceMemorial")
    val sourceMemorial: Memorial,
    
    @SerializedName("targetMemorial")
    val targetMemorial: Memorial,
    
    @SerializedName("relationType")
    val relationType: RelationType
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