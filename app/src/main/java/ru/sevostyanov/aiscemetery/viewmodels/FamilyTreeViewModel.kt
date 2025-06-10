package ru.sevostyanov.aiscemetery.viewmodels

import android.content.Context
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
import java.io.IOException
import retrofit2.HttpException
import ru.sevostyanov.aiscemetery.repository.FamilyTreeRepository
import ru.sevostyanov.aiscemetery.repository.MemorialRepository
import ru.sevostyanov.aiscemetery.user.UserManager
import javax.inject.Inject

@HiltViewModel
class FamilyTreeViewModel @Inject constructor(
    private val repository: FamilyTreeRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _familyTrees = MutableLiveData<List<FamilyTree>>()
    val familyTrees: LiveData<List<FamilyTree>> = _familyTrees

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isAuthorized = MutableLiveData<Boolean>(true)
    val isAuthorized: LiveData<Boolean> = _isAuthorized

    private var currentTabPosition = 0

    fun setCurrentTabPosition(position: Int) {
        currentTabPosition = position
    }

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

    fun loadMyFamilyTrees() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _familyTrees.value = repository.getMyFamilyTrees()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPublicFamilyTrees() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _familyTrees.value = repository.getPublicFamilyTrees()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAccessibleFamilyTrees() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = repository.getAccessibleFamilyTrees()
                _familyTrees.value = response
                _isAuthorized.value = true
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createFamilyTree(familyTree: FamilyTree) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = repository.createFamilyTree(familyTree)
                val currentList = _familyTrees.value?.toMutableList() ?: mutableListOf()
                currentList.add(response)
                _familyTrees.value = currentList
                _isAuthorized.value = true
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteFamilyTree(id: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Получаем текущего пользователя
                val currentUser = UserManager.getCurrentUser() ?: UserManager.loadUserFromPreferences(context)
                if (currentUser == null) {
                    _error.value = "Пользователь не авторизован"
                    return@launch
                }

                // Получаем дерево для проверки прав
                val tree = repository.getFamilyTreeById(id)
                if (tree.userId != currentUser.id) {
                    _error.value = "У вас нет прав на удаление этого дерева"
                    return@launch
                }

                // Если всё в порядке, удаляем дерево
                repository.deleteFamilyTree(id)
                if (currentTabPosition == 0) {
                    loadMyFamilyTrees()
                } else {
                    loadPublicFamilyTrees()
                }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchTrees(
        query: String? = null,
        ownerName: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        myOnly: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _familyTrees.value = repository.searchTrees(query, ownerName, startDate, endDate, myOnly)
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFamilyTrees() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _familyTrees.value = repository.getMyFamilyTrees()
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка при загрузке деревьев"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFamilyTree(id: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val tree = repository.getFamilyTreeById(id)
                _familyTrees.value = listOf(tree)
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка при загрузке дерева"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCurrentUserId(): Long? {
        val user = UserManager.getCurrentUser() ?: UserManager.loadUserFromPreferences(context)
        return user?.id
    }
} 