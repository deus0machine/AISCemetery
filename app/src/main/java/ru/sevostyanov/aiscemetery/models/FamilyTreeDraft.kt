package ru.sevostyanov.aiscemetery.models

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonElement
import com.google.gson.Gson

// Модель черновика семейного дерева
data class FamilyTreeDraft(
    @SerializedName("id")
    val id: Long,
    
    // Используем JsonElement для гибкой обработки
    @SerializedName("familyTree")
    private val _familyTree: JsonElement? = null,
    
    // Используем JsonElement для гибкой обработки editor
    @SerializedName("editor")
    private val _editor: JsonElement? = null,
    
    @SerializedName("status")
    val status: DraftStatus,
    
    @SerializedName("draftName")
    val draftName: String,
    
    @SerializedName("draftDescription")
    val draftDescription: String?,
    
    @SerializedName("draftIsPublic")
    val draftIsPublic: Boolean,
    
    @SerializedName("originalName")
    val originalName: String,
    
    @SerializedName("originalDescription")
    val originalDescription: String?,
    
    @SerializedName("originalIsPublic")
    val originalIsPublic: Boolean,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("submittedAt")
    val submittedAt: String?,
    
    @SerializedName("reviewedAt")
    val reviewedAt: String?,
    
    @SerializedName("reviewMessage")
    val reviewMessage: String?,
    
    // JSON данные черновика
    @SerializedName("draftMemorialsJson")
    val draftMemorialsJson: String?,
    
    @SerializedName("draftRelationsJson")
    val draftRelationsJson: String?,
    
    @SerializedName("originalMemorialsJson")
    val originalMemorialsJson: String?,
    
    @SerializedName("originalRelationsJson")
    val originalRelationsJson: String?
) {
    // Вычисляемое свойство для familyTree
    val familyTree: FamilyTree?
        get() = when {
            _familyTree == null -> null
            _familyTree.isJsonObject -> Gson().fromJson(_familyTree, FamilyTree::class.java)
            _familyTree.isJsonPrimitive -> {
                // Если это ID, создаем объект только с ID
                val id = _familyTree.asLong
                FamilyTree(id = id, name = "Tree $id", description = "")
            }
            else -> null
        }
    
    // Вычисляемое свойство для editor
    val editor: UserInfo?
        get() = when {
            _editor == null -> null
            _editor.isJsonObject -> Gson().fromJson(_editor, UserInfo::class.java)
            _editor.isJsonPrimitive -> {
                // Если это ID, создаем объект только с ID
                val id = _editor.asLong
                UserInfo(id = id, fio = "User $id", contacts = "", login = "user$id")
            }
            else -> null
        }
    
    // Преобразование черновика в FamilyTree для отображения
    fun toFamilyTree(): FamilyTree {
        val baseTree = familyTree
        return if (baseTree != null) {
            baseTree.copy(
                name = draftName,
                description = draftDescription,
                isPublic = draftIsPublic,
                // Сохраняем остальные поля из оригинального дерева
                userId = baseTree.userId,
                owner = baseTree.owner, // Копируем информацию о владельце
                publicationStatus = baseTree.publicationStatus,
                createdAt = baseTree.createdAt,
                updatedAt = baseTree.updatedAt,
                memorialRelations = baseTree.memorialRelations,
                // Копируем количество мемориалов из оригинального дерева
                memorialCount = baseTree.memorialCount,
                accessList = baseTree.accessList
            )
        } else {
            // Если familyTree null, создаем минимальный объект
            FamilyTree(
                name = draftName,
                description = draftDescription,
                isPublic = draftIsPublic
            )
        }
    }
    
    // Получить текстовое описание статуса
    fun getStatusText(): String {
        return when (status) {
            DraftStatus.DRAFT -> "Черновик"
            DraftStatus.SUBMITTED -> "На рассмотрении"
            DraftStatus.APPLIED -> "Применен"
            DraftStatus.REJECTED -> "Отклонен"
        }
    }
    
    // Получить описание изменений
    fun getChangesDescription(): String {
        val changes = mutableListOf<String>()
        
        if (draftName != originalName) {
            changes.add("название изменено")
        }
        
        if (draftDescription != originalDescription) {
            changes.add("описание изменено")
        }
        
        if (draftIsPublic != originalIsPublic) {
            changes.add("видимость изменена")
        }
        
        return if (changes.isEmpty()) {
            "Нет изменений"
        } else {
            changes.joinToString(", ")
        }
    }
    
    // Можно ли редактировать черновик
    fun canEdit(): Boolean {
        return status == DraftStatus.DRAFT || status == DraftStatus.REJECTED
    }
    
    // Можно ли отправить черновик на рассмотрение
    fun canSubmit(): Boolean {
        return status == DraftStatus.DRAFT && hasChanges()
    }
    
    // Есть ли изменения в черновике
    fun hasChanges(): Boolean {
        return draftName != originalName || 
               draftDescription != originalDescription || 
               draftIsPublic != originalIsPublic
    }
}

// Статусы черновика
enum class DraftStatus {
    @SerializedName("DRAFT")
    DRAFT,
    
    @SerializedName("SUBMITTED")
    SUBMITTED,
    
    @SerializedName("APPLIED")
    APPLIED,
    
    @SerializedName("REJECTED")
    REJECTED
}

// Модель для обновления черновика
data class UpdateDraftRequest(
    val name: String,
    val description: String?,
    val isPublic: Boolean
)

// Модель для рецензирования черновика
data class ReviewRequest(
    val message: String,
    val reviewerId: Long
) 