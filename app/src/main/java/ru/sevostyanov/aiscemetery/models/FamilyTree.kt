package ru.sevostyanov.aiscemetery.models

import com.google.gson.annotations.SerializedName

data class FamilyTree(
    @SerializedName("id")
    val id: Long? = null,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("user")
    val userId: Long? = null,
    
    @SerializedName("is_public")
    val isPublic: Boolean = false,
    
    @SerializedName("created_at")
    val createdAt: String? = null,
    
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    
    @SerializedName("memorialRelations")
    val memorialRelations: List<MemorialRelation>? = null,
    
    @SerializedName("accessList")
    val accessList: List<FamilyTreeAccess>? = null
) {
    fun toUpdateDTO() = FamilyTreeUpdateDTO(
        id = id,
        name = name,
        description = description,
        isPublic = isPublic
    )
}

data class FamilyTreeUpdateDTO(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    @SerializedName("is_public")
    val isPublic: Boolean = false
)

data class MemorialRelation(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("familyTree")
    val familyTreeId: Long,
    
    @SerializedName("sourceMemorial")
    val sourceMemorial: Memorial,
    
    @SerializedName("targetMemorial")
    val targetMemorial: Memorial,
    
    @SerializedName("relationType")
    val relationType: RelationType
)

data class FamilyTreeAccess(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("familyTree")
    val familyTreeId: Long,
    
    @SerializedName("user")
    val userId: Long,
    
    @SerializedName("accessLevel")
    val accessLevel: AccessLevel,
    
    @SerializedName("grantedAt")
    val grantedAt: String
)

enum class AccessLevel {
    @SerializedName("VIEWER")
    VIEWER,
    
    @SerializedName("EDITOR")
    EDITOR,
    
    @SerializedName("ADMIN")
    ADMIN
}