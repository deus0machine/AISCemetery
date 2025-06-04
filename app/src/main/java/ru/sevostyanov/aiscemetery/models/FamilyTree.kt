package ru.sevostyanov.aiscemetery.models

import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.JsonAdapter
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

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
    
    @SerializedName("memorialCount")
    val memorialCount: Int? = null,
    
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
    @JsonAdapter(MemorialInRelationDeserializer::class)
    val sourceMemorial: Memorial,
    
    @SerializedName("targetMemorial")
    @JsonAdapter(MemorialInRelationDeserializer::class)
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

// Custom deserializer для Memorial в связях
class MemorialInRelationDeserializer : JsonDeserializer<Memorial?> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Memorial? {
        return when {
            json == null || json.isJsonNull -> null
            json.isJsonPrimitive && json.asJsonPrimitive.isNumber -> {
                // Если это число (ID), создаем заглушку Memorial с только ID
                Memorial(
                    id = json.asLong,
                    fio = "Загрузка...", // Заглушка для имени
                    birthDate = null,
                    deathDate = null,
                    biography = null,
                    mainLocation = null,
                    burialLocation = null
                )
            }
            json.isJsonObject -> {
                // Если это полный объект, используем стандартную десериализацию
                context?.deserialize(json, Memorial::class.java)
            }
            else -> null
        }
    }
}