package ru.sevostyanov.aiscemetery.models

import com.google.gson.annotations.SerializedName

data class FamilyTree(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("ownerId")
    val ownerId: Long,
    
    @SerializedName("isPublic")
    val isPublic: Boolean,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("updatedAt")
    val updatedAt: String
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