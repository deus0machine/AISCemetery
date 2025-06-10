package ru.sevostyanov.aiscemetery.models

import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.JsonAdapter
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type
import ru.sevostyanov.aiscemetery.user.UserManager

// Класс для информации о пользователе
data class UserInfo(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("fio")
    val fio: String?,
    
    @SerializedName("contacts")
    val contacts: String?,
    
    @SerializedName("login")
    val login: String
)

data class FamilyTree(
    @SerializedName("id")
    val id: Long? = null,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("user")
    val userId: Long? = null,
    
    @SerializedName("owner")
    val owner: UserInfo? = null, // Добавляем полную информацию о владельце
    
    @SerializedName("is_public")
    val isPublic: Boolean = false,
    
    @SerializedName("publicationStatus")
    val publicationStatus: PublicationStatus? = null,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("updatedAt")
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
    
    // Вычисляемое свойство для проверки, является ли текущий пользователь владельцем
    val isUserOwner: Boolean
        get() {
            val currentUser = UserManager.getCurrentUser()
            return currentUser?.id == userId
        }
    
    // Метод для проверки, может ли дерево быть отредактировано
    fun canEdit(): Boolean {
        // Если дерево на модерации, запрещаем его редактирование
        if (publicationStatus == PublicationStatus.PENDING_MODERATION) {
            return false
        }
        
        return isUserOwner
    }
    
    // Метод для получения статуса публикации в виде строки
    fun getPublicationStatusText(): String {
        return when (publicationStatus) {
            PublicationStatus.PUBLISHED -> "Опубликовано"
            PublicationStatus.PENDING_MODERATION -> "На модерации"
            PublicationStatus.REJECTED -> "Отклонено"
            PublicationStatus.DRAFT -> "Приватный"
            null -> if (isPublic) "Опубликовано" else "Приватный"
        }
    }
    
    // Метод для получения цвета статуса публикации
    fun getPublicationStatusColor(): Int {
        return when (publicationStatus) {
            PublicationStatus.PUBLISHED -> android.R.color.holo_green_dark
            PublicationStatus.PENDING_MODERATION -> android.R.color.holo_orange_dark
            PublicationStatus.REJECTED -> android.R.color.holo_red_dark
            PublicationStatus.DRAFT -> android.R.color.darker_gray
            null -> if (isPublic) android.R.color.holo_green_dark else android.R.color.darker_gray
        }
    }
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
                try {
                    context?.deserialize(json, Memorial::class.java)
                } catch (e: Exception) {
                    // Если стандартная десериализация не удалась, создаем минимальный Memorial
                    val jsonObj = json.asJsonObject
                    Memorial(
                        id = jsonObj.get("id")?.asLong,
                        fio = jsonObj.get("fio")?.asString ?: "Неизвестно",
                        birthDate = jsonObj.get("birthDate")?.asString,
                        deathDate = jsonObj.get("deathDate")?.asString,
                        biography = jsonObj.get("biography")?.asString,
                        mainLocation = null,
                        burialLocation = null
                    )
                }
            }
            else -> null
        }
    }
}

// Модель для полного дерева с мемориалами и связями
data class FamilyTreeFullData(
    @SerializedName("familyTree")
    val familyTree: FamilyTree,
    
    @SerializedName("memorials")
    val memorials: List<Memorial>,
    
    @SerializedName("relations")
    val relations: List<MemorialRelation>
)

// Упрощенная модель для отображения мемориала в дереве
data class TreeMemorial(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("fio")
    val fio: String,
    
    @SerializedName("birthDate")
    val birthDate: String?,
    
    @SerializedName("deathDate")
    val deathDate: String?,
    
    @SerializedName("photoUrl")
    val photoUrl: String? = null,
    
    @SerializedName("isPublic")
    val isPublic: Boolean = false
)

// Упрощенная модель связи для дерева
data class TreeRelation(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("sourceMemorialId")
    val sourceMemorialId: Long,
    
    @SerializedName("targetMemorialId")
    val targetMemorialId: Long,
    
    @SerializedName("relationType")
    val relationType: RelationType
)

// Оптимизированная модель дерева
data class OptimizedFamilyTree(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("isPublic")
    val isPublic: Boolean = false,
    
    @SerializedName("memorials")
    val memorials: List<TreeMemorial>,
    
    @SerializedName("relations")
    val relations: List<TreeRelation>
)