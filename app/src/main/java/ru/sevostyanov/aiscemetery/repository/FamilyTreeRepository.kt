package ru.sevostyanov.aiscemetery.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.sevostyanov.aiscemetery.RetrofitClient
import ru.sevostyanov.aiscemetery.models.FamilyTree
import ru.sevostyanov.aiscemetery.models.FamilyTreeAccess
import ru.sevostyanov.aiscemetery.models.MemorialRelation
import ru.sevostyanov.aiscemetery.models.PublicationStatus
import ru.sevostyanov.aiscemetery.models.RelationType
import ru.sevostyanov.aiscemetery.user.UserManager
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class FamilyTreeRepository @Inject constructor() {
    private val apiService = RetrofitClient.getApiService()

    suspend fun getMyFamilyTrees(): List<FamilyTree> = withContext(Dispatchers.IO) {
        try {
            apiService.getMyFamilyTrees()
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при получении списка деревьев: ${e.message}")
        }
    }

    suspend fun getPublicFamilyTrees(): List<FamilyTree> = withContext(Dispatchers.IO) {
        try {
            apiService.getPublicFamilyTrees()
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при получении публичных деревьев: ${e.message}")
        }
    }

    suspend fun getAccessibleFamilyTrees(): List<FamilyTree> = withContext(Dispatchers.IO) {
        try {
            apiService.getAccessibleFamilyTrees()
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при получении доступных деревьев: ${e.message}")
        }
    }

    suspend fun getFamilyTreeById(id: Long): FamilyTree = withContext(Dispatchers.IO) {
        try {
            apiService.getFamilyTreeById(id)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при получении дерева: ${e.message}")
        }
    }

    suspend fun createFamilyTree(familyTree: FamilyTree): FamilyTree = withContext(Dispatchers.IO) {
        try {
            apiService.createFamilyTree(familyTree)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при создании дерева: ${e.message}")
        }
    }

    suspend fun updateFamilyTree(id: Long, familyTree: FamilyTree): FamilyTree = withContext(Dispatchers.IO) {
        try {
            apiService.updateFamilyTree(id, familyTree.toUpdateDTO())
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при обновлении дерева: ${e.message}")
        }
    }

    suspend fun deleteFamilyTree(id: Long) = withContext(Dispatchers.IO) {
        try {
            val token = RetrofitClient.getToken()
            if (token == null) {
                throw Exception("Требуется авторизация")
            }
            apiService.deleteFamilyTree(id)
        } catch (e: Exception) {
            Log.e("FamilyTreeRepository", "Error deleting tree: ${e.message}", e)
            e.printStackTrace()
            throw Exception("Ошибка при удалении дерева: ${e.message}")
        }
    }

    // Методы для работы с правами доступа
    suspend fun getFamilyTreeAccess(treeId: Long): List<FamilyTreeAccess> = withContext(Dispatchers.IO) {
        try {
            apiService.getFamilyTreeAccess(treeId)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при получении прав доступа: ${e.message}")
        }
    }

    suspend fun grantAccess(treeId: Long, userId: Long, accessLevel: String): FamilyTreeAccess = withContext(Dispatchers.IO) {
        try {
            apiService.grantFamilyTreeAccess(treeId, userId, accessLevel)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при предоставлении доступа: ${e.message}")
        }
    }

    suspend fun updateAccess(treeId: Long, userId: Long, accessLevel: String): FamilyTreeAccess = withContext(Dispatchers.IO) {
        try {
            apiService.updateFamilyTreeAccess(treeId, userId, accessLevel)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при обновлении прав доступа: ${e.message}")
        }
    }

    suspend fun revokeAccess(treeId: Long, userId: Long) = withContext(Dispatchers.IO) {
        try {
            apiService.revokeFamilyTreeAccess(treeId, userId)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при отзыве прав доступа: ${e.message}")
        }
    }

    // Методы для работы с связями мемориалов
    suspend fun getMemorialRelations(familyTreeId: Long): List<MemorialRelation> = withContext(Dispatchers.IO) {
        try {
            apiService.getMemorialRelations(familyTreeId)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при получении связей: ${e.message}")
        }
    }

    suspend fun createMemorialRelation(familyTreeId: Long, relation: MemorialRelation): MemorialRelation = withContext(Dispatchers.IO) {
        try {
            apiService.createMemorialRelation(familyTreeId, relation)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при создании связи: ${e.message}")
        }
    }

    suspend fun updateMemorialRelation(familyTreeId: Long, relationId: Long, relation: MemorialRelation): MemorialRelation = withContext(Dispatchers.IO) {
        try {
            apiService.updateMemorialRelation(familyTreeId, relationId, relation)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при обновлении связи: ${e.message}")
        }
    }

    suspend fun deleteMemorialRelation(familyTreeId: Long, relationId: Long) = withContext(Dispatchers.IO) {
        try {
            apiService.deleteMemorialRelation(familyTreeId, relationId)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при удалении связи: ${e.message}")
        }
    }

    suspend fun searchTrees(
        query: String? = null,
        ownerName: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        myOnly: Boolean = false
    ): List<FamilyTree> = withContext(Dispatchers.IO) {
        try {
            apiService.searchFamilyTrees(query, ownerName, startDate, endDate, myOnly)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при поиске деревьев: ${e.message}")
        }
    }

    // Методы для модерации семейных деревьев
    suspend fun sendFamilyTreeForModeration(id: Long): FamilyTree = withContext(Dispatchers.IO) {
        try {
            apiService.sendFamilyTreeForModeration(id)
        } catch (e: retrofit2.HttpException) {
            e.printStackTrace()
            when (e.code()) {
                400 -> {
                    val errorBody = e.response()?.errorBody()?.string()
                    when {
                        errorBody?.contains("must be public") == true -> 
                            throw Exception("Не все мемориалы в дереве опубликованы. Для отправки дерева на публикацию все включенные мемориалы должны быть опубликованы.")
                        else -> 
                            throw Exception("Ошибка валидации: ${errorBody ?: e.message()}")
                    }
                }
                401 -> throw Exception("Необходима авторизация")
                403 -> throw Exception("Недостаточно прав доступа")
                404 -> throw Exception("Дерево не найдено")
                else -> throw Exception("Ошибка сервера: ${e.message()}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Не удалось отправить дерево на модерацию: ${e.message}")
        }
    }

    suspend fun approveFamilyTree(id: Long): FamilyTree = withContext(Dispatchers.IO) {
        try {
            apiService.approveFamilyTree(id)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Не удалось одобрить дерево: ${e.message}")
        }
    }

    suspend fun rejectFamilyTree(id: Long, reason: String): FamilyTree = withContext(Dispatchers.IO) {
        try {
            apiService.rejectFamilyTree(id, reason)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Не удалось отклонить дерево: ${e.message}")
        }
    }

    suspend fun unpublishFamilyTree(id: Long): FamilyTree = withContext(Dispatchers.IO) {
        try {
            apiService.unpublishFamilyTree(id)
        } catch (e: retrofit2.HttpException) {
            e.printStackTrace()
            when (e.code()) {
                400 -> {
                    val errorBody = e.response()?.errorBody()?.string()
                    when {
                        errorBody?.contains("not published") == true -> 
                            throw Exception("Дерево не опубликовано и не может быть снято с публикации")
                        errorBody?.contains("Only tree owner") == true ->
                            throw Exception("Только владелец дерева может снять его с публикации")
                        else -> 
                            throw Exception("Ошибка валидации: ${errorBody ?: e.message()}")
                    }
                }
                401 -> throw Exception("Необходима авторизация")
                403 -> throw Exception("Недостаточно прав доступа")
                404 -> throw Exception("Дерево не найдено")
                else -> throw Exception("Ошибка сервера: ${e.message()}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Не удалось снять дерево с публикации: ${e.message}")
        }
    }

    // Метод для проверки публичности мемориалов в дереве
    suspend fun checkTreeMemorialsPublicity(treeId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val relations = apiService.getMemorialRelations(treeId)
            val memorialIds = mutableSetOf<Long>()
            
            // Извлекаем уникальные ID мемориалов из связей
            relations.forEach { relation ->
                if (relation.relationType == RelationType.PLACEHOLDER) {
                    relation.sourceMemorial.id?.let { memorialIds.add(it) }
                } else {
                    relation.sourceMemorial.id?.let { memorialIds.add(it) }
                    relation.targetMemorial.id?.let { memorialIds.add(it) }
                }
            }
            
            // Проверяем публичность каждого мемориала
            for (memorialId in memorialIds) {
                val memorial = RetrofitClient.getApiService().getMemorialById(memorialId)
                if (memorial.publicationStatus != PublicationStatus.PUBLISHED) {
                    return@withContext false
                }
            }
            
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при проверке публичности мемориалов: ${e.message}")
        }
    }
} 