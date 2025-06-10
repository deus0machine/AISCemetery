package ru.sevostyanov.aiscemetery.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.RetrofitClient
import ru.sevostyanov.aiscemetery.models.FamilyTree
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.MemorialRelation
import ru.sevostyanov.aiscemetery.models.RelationType
import ru.sevostyanov.aiscemetery.repository.FamilyTreeRepository
import ru.sevostyanov.aiscemetery.repository.MemorialRepository
import ru.sevostyanov.aiscemetery.user.GuestItem
import ru.sevostyanov.aiscemetery.user.UserManager
import javax.inject.Inject
import java.io.IOException
import retrofit2.HttpException

@HiltViewModel
class FamilyTreeDetailViewModel @Inject constructor(
    private val repository: FamilyTreeRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _familyTree = MutableLiveData<FamilyTree>()
    val familyTree: LiveData<FamilyTree> = _familyTree

    private val _memorialRelations = MutableLiveData<List<MemorialRelation>>()
    val memorialRelations: LiveData<List<MemorialRelation>> = _memorialRelations

    private val _availableMemorials = MutableLiveData<List<Memorial>>()
    val availableMemorials: LiveData<List<Memorial>> = _availableMemorials

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isAuthorized = MutableLiveData<Boolean>(true)
    val isAuthorized: LiveData<Boolean> = _isAuthorized

    private val familyTreeRepository = FamilyTreeRepository()
    private val memorialRepository = MemorialRepository()

    private fun handleError(e: Exception) {
        when (e) {
            is HttpException -> {
                when (e.code()) {
                    401 -> {
                        _isAuthorized.value = false
                        _error.value = "Ошибка авторизации. Пожалуйста, войдите в систему."
                        RetrofitClient.clearToken()
                    }
                    else -> _error.value = "Ошибка сервера: ${e.message()}"
                }
            }
            is IOException -> _error.value = "Ошибка сети: ${e.message}"
            else -> _error.value = "Ошибка: ${e.message}"
        }
    }

    fun loadFamilyTree(id: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _familyTree.value = repository.getFamilyTreeById(id)
                _isAuthorized.value = true
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadGenealogyData(treeId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Пробуем загрузить полные данные одним запросом
                try {
                    val fullData = RetrofitClient.getApiService().getFamilyTreeFullData(treeId)
                    _familyTree.value = fullData.familyTree
                    _memorialRelations.value = fullData.relations
                    
                    // Извлекаем уникальные мемориалы из связей для доступных мемориалов
                    val uniqueMemorials = mutableSetOf<Memorial>()
                    fullData.relations.forEach { relation ->
                        if (relation.relationType == RelationType.PLACEHOLDER) {
                            uniqueMemorials.add(relation.sourceMemorial)
                        } else {
                            uniqueMemorials.add(relation.sourceMemorial)
                            uniqueMemorials.add(relation.targetMemorial)
                        }
                    }
                    _availableMemorials.value = uniqueMemorials.toList()
                    
                } catch (e: Exception) {
                    Log.w("FamilyTreeDetailVM", "Full data endpoint not available, using fallback: ${e.message}")
                    
                    // Fallback - загружаем данные отдельными запросами
                    _familyTree.value = repository.getFamilyTreeById(treeId)
                    _memorialRelations.value = repository.getMemorialRelations(treeId)
                    _availableMemorials.value = memorialRepository.getMyMemorials()
                }
                
                _isAuthorized.value = true
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadMemorialRelations(familyTreeId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _memorialRelations.value = repository.getMemorialRelations(familyTreeId)
                _isAuthorized.value = true
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadAvailableMemorials() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _availableMemorials.value = memorialRepository.getMyMemorials()
                _isAuthorized.value = true
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addMemorialToTree(memorialId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentTree = _familyTree.value ?: return@launch
                val treeId = currentTree.id ?: return@launch
                val memorial = memorialRepository.getMemorialById(memorialId)
                val relation = MemorialRelation(
                    id = 0,
                    familyTreeId = treeId,
                    sourceMemorial = memorial,
                    targetMemorial = memorial,
                    relationType = RelationType.PLACEHOLDER
                )
                repository.createMemorialRelation(treeId, relation)
                loadMemorialRelations(treeId)
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }



    fun createMemorialRelation(relation: MemorialRelation) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val currentTree = _familyTree.value ?: return@launch
                val treeId = currentTree.id ?: return@launch
                val response = repository.createMemorialRelation(treeId, relation)
                val currentList = _memorialRelations.value?.toMutableList() ?: mutableListOf()
                currentList.add(response)
                _memorialRelations.value = currentList
                _isAuthorized.value = true
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateMemorialRelation(relationId: Long, relation: MemorialRelation) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val currentTree = _familyTree.value ?: return@launch
                val treeId = currentTree.id ?: return@launch
                val response = repository.updateMemorialRelation(treeId, relationId, relation)
                val currentList = _memorialRelations.value?.toMutableList() ?: mutableListOf()
                val index = currentList.indexOfFirst { it.id == relationId }
                if (index != -1) {
                    currentList[index] = response
                    _memorialRelations.value = currentList
                }
                _isAuthorized.value = true
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteMemorialRelation(relationId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val currentTree = _familyTree.value ?: return@launch
                val treeId = currentTree.id ?: return@launch
                repository.deleteMemorialRelation(treeId, relationId)
                val currentList = _memorialRelations.value?.toMutableList() ?: mutableListOf()
                currentList.removeAll { it.id == relationId }
                _memorialRelations.value = currentList
                _isAuthorized.value = true
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun canEdit(): Boolean {
        val currentUserId = RetrofitClient.getCurrentUserId()
        val treeOwnerId = _familyTree.value?.id
        return currentUserId != -1L && treeOwnerId != null && currentUserId == treeOwnerId
    }

    fun canDelete(): Boolean {
        val currentUserId = RetrofitClient.getCurrentUserId()
        val treeOwnerId = _familyTree.value?.id
        return currentUserId != -1L && treeOwnerId != null && currentUserId == treeOwnerId
    }

    fun updateFamilyTree(id: Long, name: String, description: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentTree = _familyTree.value ?: return@launch
                val currentUser = UserManager.getCurrentUser() ?: UserManager.loadUserFromPreferences(context)
                if (currentUser == null) {
                    _error.value = "Пользователь не авторизован"
                    return@launch
                }

                val updatedTree = currentTree.copy(
                    name = name,
                    description = description,
                    userId = currentUser.id
                )
                _familyTree.value = repository.updateFamilyTree(id, updatedTree)
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Методы для модерации семейных деревьев
    fun sendFamilyTreeForModeration(id: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val updatedTree = repository.sendFamilyTreeForModeration(id)
                _familyTree.value = updatedTree
                _isAuthorized.value = true
            } catch (e: Exception) {
                Log.e("FamilyTreeDetailViewModel", "Error sending tree for moderation: ${e.message}", e)
                _error.value = e.message
                
                // Дополнительная проверка для специфичных ошибок
                when {
                    e.message?.contains("Не все мемориалы в дереве опубликованы") == true -> {
                        // Это уже обработано в репозитории
                    }
                    e.message?.contains("must be public") == true -> {
                        _error.value = "Не все мемориалы в дереве опубликованы. Для отправки дерева на публикацию все включенные мемориалы должны быть опубликованы."
                    }
                    else -> {
                        handleError(e)
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveFamilyTree(treeId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val updatedTree = repository.approveFamilyTree(treeId)
                _familyTree.value = updatedTree
                _isAuthorized.value = true
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rejectFamilyTree(treeId: Long, reason: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val updatedTree = repository.rejectFamilyTree(treeId, reason)
                _familyTree.value = updatedTree
                _isAuthorized.value = true
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun unpublishFamilyTree(treeId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val updatedTree = repository.unpublishFamilyTree(treeId)
                _familyTree.value = updatedTree
                _isAuthorized.value = true
            } catch (e: Exception) {
                Log.e("FamilyTreeDetailViewModel", "Error unpublishing tree: ${e.message}", e)
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Проверяем, может ли пользователь редактировать дерево
    fun canEditTree(): Boolean {
        val tree = _familyTree.value ?: return false
        
        // Проверяем, не находится ли дерево на модерации
        if (tree.publicationStatus == ru.sevostyanov.aiscemetery.models.PublicationStatus.PENDING_MODERATION) {
            return false
        }
        
        // Проверяем права пользователя
        return tree.canEdit()
    }

    // Проверяем, может ли пользователь отправить дерево на модерацию
    fun canSendForModeration(): Boolean {
        val tree = _familyTree.value ?: return false
        
        // Может отправить только владелец и только если дерево не опубликовано и не на модерации
        return tree.isUserOwner && 
               tree.publicationStatus != ru.sevostyanov.aiscemetery.models.PublicationStatus.PUBLISHED &&
               tree.publicationStatus != ru.sevostyanov.aiscemetery.models.PublicationStatus.PENDING_MODERATION
    }

    // Проверяем, может ли пользователь снять дерево с публикации
    fun canUnpublishTree(): Boolean {
        val tree = _familyTree.value ?: return false
        
        // Может снять с публикации только владелец и только если дерево опубликовано
        return tree.isUserOwner && 
               tree.publicationStatus == ru.sevostyanov.aiscemetery.models.PublicationStatus.PUBLISHED
    }

    // Получаем текстовое описание статуса дерева
    fun getTreeStatusText(): String {
        val tree = _familyTree.value ?: return "Неизвестно"
        return tree.getPublicationStatusText()
    }

    // Получаем цвет для статуса дерева
    fun getTreeStatusColor(): Int {
        val tree = _familyTree.value ?: return android.graphics.Color.GRAY
        return tree.getPublicationStatusColor()
    }
} 