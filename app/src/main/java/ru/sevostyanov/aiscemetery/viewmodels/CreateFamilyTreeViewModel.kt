package ru.sevostyanov.aiscemetery.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.models.FamilyTree
import ru.sevostyanov.aiscemetery.repository.FamilyTreeRepository
import ru.sevostyanov.aiscemetery.user.UserManager
import javax.inject.Inject

@HiltViewModel
class CreateFamilyTreeViewModel @Inject constructor(
    private val familyTreeRepository: FamilyTreeRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isSuccess = MutableLiveData<Boolean>()
    val isSuccess: LiveData<Boolean> = _isSuccess

    private val _createdTree = MutableLiveData<FamilyTree?>()
    val createdTree: LiveData<FamilyTree?> = _createdTree

    fun createFamilyTree(name: String, description: String, isPublic: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentUser = UserManager.getCurrentUser() ?: UserManager.loadUserFromPreferences(context)
                if (currentUser == null) {
                    _error.value = "Пользователь не авторизован"
                    return@launch
                }

                val familyTree = FamilyTree(
                    name = name,
                    description = description,
                    isPublic = isPublic,
                    userId = currentUser.id
                )
                _createdTree.value = familyTreeRepository.createFamilyTree(familyTree)
                _isSuccess.value = true
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleError(e: Exception) {
        _error.value = e.message ?: "Ошибка при создании дерева"
        _isSuccess.value = false
    }
} 