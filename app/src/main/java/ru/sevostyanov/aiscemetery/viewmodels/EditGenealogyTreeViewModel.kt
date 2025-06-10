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
import javax.inject.Inject

@HiltViewModel
class EditGenealogyTreeViewModel @Inject constructor(
    private val familyTreeRepository: FamilyTreeRepository,
    private val memorialRepository: MemorialRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _familyTree = MutableLiveData<FamilyTree>()
    val familyTree: LiveData<FamilyTree> = _familyTree

    private val _treeMemorials = MutableLiveData<List<Memorial>>()
    val treeMemorials: LiveData<List<Memorial>> = _treeMemorials

    private val _availableMemorials = MutableLiveData<List<Memorial>>()
    val availableMemorials: LiveData<List<Memorial>> = _availableMemorials

    private val _memorialRelations = MutableLiveData<List<MemorialRelation>>()
    val memorialRelations: LiveData<List<MemorialRelation>> = _memorialRelations

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isSuccess = MutableLiveData<Boolean>()
    val isSuccess: LiveData<Boolean> = _isSuccess

    fun loadFamilyTree(treeId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Загружаем основную информацию о дереве
                _familyTree.value = familyTreeRepository.getFamilyTreeById(treeId)
                
                // Загружаем мемориалы дерева
                loadTreeMemorials(treeId)
                
                // Загружаем связи между мемориалами
                loadMemorialRelations(treeId)
                
                // Загружаем доступные мемориалы пользователя
                loadAvailableMemorials()

            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadTreeMemorials(treeId: Long) {
        try {
            Log.d("EditGenealogyTreeVM", "=== loadTreeMemorials trying API call ===")
            val apiMemorials = RetrofitClient.getApiService().getFamilyTreeMemorials(treeId)
            Log.d("EditGenealogyTreeVM", "API call successful, got ${apiMemorials.size} memorials:")
            apiMemorials.forEachIndexed { index, memorial ->
                Log.d("EditGenealogyTreeVM", "API memorial $index: ${memorial.fio} (ID: ${memorial.id})")
            }
            _treeMemorials.value = apiMemorials
        } catch (e: Exception) {
            Log.d("EditGenealogyTreeVM", "=== loadTreeMemorials using fallback (server returns duplicates) ===")
            Log.d("EditGenealogyTreeVM", "Reason: ${e.message}")
            // Fallback - получаем мемориалы из связей с правильной обработкой PLACEHOLDER связей
            val relations = RetrofitClient.getApiService().getMemorialRelations(treeId)
            Log.d("EditGenealogyTreeVM", "Got ${relations.size} relations for fallback")
            
            val memorials = mutableSetOf<Memorial>()
            relations.forEach { relation ->
                if (relation.relationType == RelationType.PLACEHOLDER) {
                    // Для PLACEHOLDER связей добавляем только источник (он же и цель)
                    // чтобы избежать дублирования одного и того же мемориала
                    memorials.add(relation.sourceMemorial)
                } else {
                    // Для обычных связей добавляем оба мемориала
                    memorials.add(relation.sourceMemorial)
                    memorials.add(relation.targetMemorial)
                }
            }
                
            Log.d("EditGenealogyTreeVM", "Fallback result: ${memorials.size} unique memorials:")
            memorials.forEachIndexed { index, memorial ->
                Log.d("EditGenealogyTreeVM", "Fallback memorial $index: ${memorial.fio} (ID: ${memorial.id})")
            }
            _treeMemorials.value = memorials.toList()
        }
    }

    private suspend fun loadMemorialRelations(treeId: Long) {
        _memorialRelations.value = RetrofitClient.getApiService().getMemorialRelations(treeId)
    }

    private suspend fun loadAvailableMemorials() {
        _availableMemorials.value = memorialRepository.getMyMemorials()
    }

    fun addMemorialToTree(memorialId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val treeId = _familyTree.value?.id ?: return@launch
                
                // Добавляем мемориал в дерево
                RetrofitClient.getApiService().addMemorialToTree(treeId, memorialId)
                
                // Обновляем список мемориалов дерева
                loadTreeMemorials(treeId)
                
                _isSuccess.value = true
                
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeMemorialFromTree(memorialId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val treeId = _familyTree.value?.id ?: return@launch
                
                // Удаляем мемориал из дерева
                RetrofitClient.getApiService().removeMemorialFromTree(treeId, memorialId)
                
                // Обновляем список мемориалов дерева
                loadTreeMemorials(treeId)
                
                // Обновляем связи (могли удалиться)
                loadMemorialRelations(treeId)
                
                _isSuccess.value = true
                
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createMemorialRelation(sourceMemorial: Memorial, targetMemorial: Memorial, relationType: RelationType) {
        Log.d("EditGenealogyTreeVM", "=== createMemorialRelation START ===")
        Log.d("EditGenealogyTreeVM", "Source memorial: ${sourceMemorial.fio} (ID: ${sourceMemorial.id})")
        Log.d("EditGenealogyTreeVM", "Target memorial: ${targetMemorial.fio} (ID: ${targetMemorial.id})")
        Log.d("EditGenealogyTreeVM", "Relation type: $relationType")
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val treeId = _familyTree.value?.id ?: run {
                    Log.e("EditGenealogyTreeVM", "Tree ID is null!")
                    return@launch
                }
                
                Log.d("EditGenealogyTreeVM", "Tree ID: $treeId")
                
                val relation = MemorialRelation(
                    id = 0L, // Будет назначен сервером
                    familyTreeId = treeId,
                    sourceMemorial = sourceMemorial,
                    targetMemorial = targetMemorial,
                    relationType = relationType
                )
                
                Log.d("EditGenealogyTreeVM", "Created relation object: $relation")
                Log.d("EditGenealogyTreeVM", "Calling API...")
                
                val response = RetrofitClient.getApiService().createMemorialRelation(treeId, relation)
                Log.d("EditGenealogyTreeVM", "API call successful, response: $response")
                
                // Обновляем связи
                Log.d("EditGenealogyTreeVM", "Reloading memorial relations...")
                loadMemorialRelations(treeId)
                
                _isSuccess.value = true
                Log.d("EditGenealogyTreeVM", "=== createMemorialRelation SUCCESS ===")
                
            } catch (e: Exception) {
                Log.e("EditGenealogyTreeVM", "=== createMemorialRelation ERROR ===")
                Log.e("EditGenealogyTreeVM", "Exception type: ${e.javaClass.simpleName}")
                Log.e("EditGenealogyTreeVM", "Exception message: ${e.message}")
                Log.e("EditGenealogyTreeVM", "Stack trace:", e)
                
                if (e is retrofit2.HttpException) {
                    Log.e("EditGenealogyTreeVM", "HTTP error code: ${e.code()}")
                    try {
                        val errorBody = e.response()?.errorBody()?.string()
                        Log.e("EditGenealogyTreeVM", "HTTP error body: $errorBody")
                    } catch (bodyException: Exception) {
                        Log.e("EditGenealogyTreeVM", "Could not read error body: ${bodyException.message}")
                    }
                }
                
                handleError(e)
                Log.e("EditGenealogyTreeVM", "=== createMemorialRelation END ERROR ===")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateMemorialRelation(relationId: Long, sourceMemorial: Memorial, targetMemorial: Memorial, relationType: RelationType) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val treeId = _familyTree.value?.id ?: return@launch
                
                val relation = MemorialRelation(
                    id = relationId,
                    familyTreeId = treeId,
                    sourceMemorial = sourceMemorial,
                    targetMemorial = targetMemorial,
                    relationType = relationType
                )
                
                RetrofitClient.getApiService().updateMemorialRelation(treeId, relationId, relation)
                
                // Обновляем связи
                loadMemorialRelations(treeId)
                
                _isSuccess.value = true
                
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
                
                val treeId = _familyTree.value?.id ?: return@launch
                
                RetrofitClient.getApiService().deleteMemorialRelation(treeId, relationId)
                
                // Обновляем связи
                loadMemorialRelations(treeId)
                
                _isSuccess.value = true
                
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun canEdit(): Boolean {
        val currentUserId = RetrofitClient.getCurrentUserId()
        val treeUserId = _familyTree.value?.userId
        return currentUserId != -1L && treeUserId != null && currentUserId == treeUserId
    }

    fun clearSuccess() {
        _isSuccess.value = false
    }

    fun clearError() {
        _error.value = null
    }

    private fun handleError(e: Exception) {
        Log.e("EditGenealogyTreeVM", "=== handleError START ===")
        Log.e("EditGenealogyTreeVM", "Exception: ${e.javaClass.simpleName}")
        Log.e("EditGenealogyTreeVM", "Message: ${e.message}")
        
        val errorMessage = when (e) {
            is retrofit2.HttpException -> {
                Log.e("EditGenealogyTreeVM", "HTTP Exception - Code: ${e.code()}")
                try {
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e("EditGenealogyTreeVM", "Error body: $errorBody")
                } catch (bodyException: Exception) {
                    Log.e("EditGenealogyTreeVM", "Could not read error body: ${bodyException.message}")
                }
                
                when (e.code()) {
                    401 -> "Ошибка авторизации"
                    403 -> "Нет прав для редактирования"
                    404 -> "Дерево не найдено"
                    409 -> "Связь уже существует"
                    500 -> "Ошибка сервера (500). Проверьте логи сервера"
                    else -> "Ошибка сервера (${e.code()}): ${e.message()}"
                }
            }
            is java.io.IOException -> {
                Log.e("EditGenealogyTreeVM", "IO Exception - likely network issue")
                "Ошибка сети"
            }
            is IllegalArgumentException -> {
                Log.e("EditGenealogyTreeVM", "IllegalArgumentException: ${e.message}")
                "Неверные данные: ${e.message}"
            }
            else -> {
                Log.e("EditGenealogyTreeVM", "Unknown exception type")
                "Неизвестная ошибка: ${e.message}"
            }
        }
        
        Log.e("EditGenealogyTreeVM", "Final error message: $errorMessage")
        Log.e("EditGenealogyTreeVM", "=== handleError END ===")
        _error.value = errorMessage
    }
} 
