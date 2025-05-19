package ru.sevostyanov.aiscemetery.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.sevostyanov.aiscemetery.RetrofitClient
import ru.sevostyanov.aiscemetery.models.FamilyTree
import ru.sevostyanov.aiscemetery.models.FamilyTreeAccess
import ru.sevostyanov.aiscemetery.models.MemorialRelation
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
        query: String = "",
        isPublic: Boolean? = null
    ): List<FamilyTree> = withContext(Dispatchers.IO) {
        try {
            apiService.searchFamilyTrees(query, isPublic)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при поиске деревьев: ${e.message}")
        }
    }
} 