package ru.sevostyanov.aiscemetery.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.RetrofitClient
import ru.sevostyanov.aiscemetery.models.FamilyTreeDraft
import ru.sevostyanov.aiscemetery.models.UpdateDraftRequest
import ru.sevostyanov.aiscemetery.models.ReviewRequest

class FamilyTreeDraftViewModel : ViewModel() {
    
    private val apiService = RetrofitClient.getApiService()
    
    // LiveData для текущего черновика
    private val _draft = MutableLiveData<FamilyTreeDraft?>()
    val draft: LiveData<FamilyTreeDraft?> = _draft
    
    // LiveData для списка черновиков на рассмотрении
    private val _pendingDrafts = MutableLiveData<List<FamilyTreeDraft>>()
    val pendingDrafts: LiveData<List<FamilyTreeDraft>> = _pendingDrafts
    
    // LiveData для состояния загрузки
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // LiveData для ошибок
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // LiveData для сообщений об успехе
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    /**
     * Получить или создать активный черновик для редактора
     */
    suspend fun getOrCreateActiveDraft(familyTreeId: Long, editorId: Long) {
        _isLoading.value = true
        _error.value = null
        
        try {
            val draft = apiService.getOrCreateActiveDraft(familyTreeId, editorId)
            _draft.value = draft
        } catch (e: Exception) {
            _error.value = "Ошибка при получении черновика: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Обновить данные в черновике
     */
    suspend fun updateDraft(draftId: Long, name: String, description: String?, isPublic: Boolean) {
        _isLoading.value = true
        _error.value = null
        
        try {
            val request = UpdateDraftRequest(name, description, isPublic)
            val updatedDraft = apiService.updateDraft(draftId, request)
            _draft.value = updatedDraft
            _successMessage.value = "Черновик сохранен"
        } catch (e: Exception) {
            _error.value = "Ошибка при сохранении черновика: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Отправить черновик на рассмотрение
     */
    suspend fun submitDraft(draftId: Long, message: String) {
        _isLoading.value = true
        _error.value = null
        
        try {
            val request = mapOf("message" to message)
            val submittedDraft = apiService.submitDraft(draftId, request)
            _draft.value = submittedDraft
            _successMessage.value = "Черновик отправлен на рассмотрение"
        } catch (e: Exception) {
            _error.value = "Ошибка при отправке черновика: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Получить черновики для рассмотрения владельцем
     */
    fun getSubmittedDraftsForOwner(ownerId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val drafts = apiService.getSubmittedDraftsForOwner(ownerId)
                _pendingDrafts.value = drafts
            } catch (e: Exception) {
                _error.value = "Ошибка при получении черновиков: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Одобрить черновик
     */
    suspend fun approveDraft(draftId: Long, reviewMessage: String, reviewerId: Long) {
        _isLoading.value = true
        _error.value = null
        
        try {
            val request = ReviewRequest(reviewMessage, reviewerId)
            val approvedDraft = apiService.approveDraft(draftId, request)
            _draft.value = approvedDraft
            _successMessage.value = "Черновик одобрен и изменения применены"
            
            // Обновляем список черновиков
            refreshPendingDrafts(reviewerId)
        } catch (e: Exception) {
            _error.value = "Ошибка при одобрении черновика: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Отклонить черновик
     */
    suspend fun rejectDraft(draftId: Long, reviewMessage: String, reviewerId: Long) {
        _isLoading.value = true
        _error.value = null
        
        try {
            val request = ReviewRequest(reviewMessage, reviewerId)
            val rejectedDraft = apiService.rejectDraft(draftId, request)
            _draft.value = rejectedDraft
            _successMessage.value = "Черновик отклонен"
            
            // Обновляем список черновиков
            refreshPendingDrafts(reviewerId)
        } catch (e: Exception) {
            _error.value = "Ошибка при отклонении черновика: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Обновить список черновиков на рассмотрении
     */
    private fun refreshPendingDrafts(ownerId: Long) {
        viewModelScope.launch {
            try {
                val drafts = apiService.getSubmittedDraftsForOwner(ownerId)
                _pendingDrafts.value = drafts
            } catch (e: Exception) {
                // Игнорируем ошибки при обновлении списка
            }
        }
    }
    
    /**
     * Очистить сообщения об ошибках и успехе
     */
    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }
} 