package ru.sevostyanov.aiscemetery.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.RetrofitClient
import ru.sevostyanov.aiscemetery.models.MemorialOwnershipRequest
import ru.sevostyanov.aiscemetery.models.Notification
import ru.sevostyanov.aiscemetery.models.NotificationStatus
import ru.sevostyanov.aiscemetery.models.NotificationType
import ru.sevostyanov.aiscemetery.models.DraftSubmission
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor() : ViewModel() {
    private val TAG = "NotificationsViewModel"

    private val _incomingNotifications = MutableLiveData<List<Notification>>()
    val incomingNotifications: LiveData<List<Notification>> = _incomingNotifications

    private val _sentNotifications = MutableLiveData<List<Notification>>()
    val sentNotifications: LiveData<List<Notification>> = _sentNotifications

    // Добавляем LiveData для уведомлений о черновиках
    private val _incomingDraftSubmissions = MutableLiveData<List<DraftSubmission>>()
    val incomingDraftSubmissions: LiveData<List<DraftSubmission>> = _incomingDraftSubmissions

    private val _outgoingDraftSubmissions = MutableLiveData<List<DraftSubmission>>()
    val outgoingDraftSubmissions: LiveData<List<DraftSubmission>> = _outgoingDraftSubmissions

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val apiService = RetrofitClient.getApiService()

    // Очистка ошибки
    fun clearError() {
        _error.value = null
    }

    // Загрузка входящих уведомлений
    fun loadIncomingNotifications() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Загружаем входящие уведомления")
                _isLoading.value = true
                _error.value = null  // Сбрасываем ошибку перед новым запросом
                val notifications = apiService.getMyNotifications()
                Log.d(TAG, "Получены входящие уведомления: ${notifications.size}")
                
                // Добавляем расширенное логирование для отладки
                if (notifications.isNotEmpty()) {
                    notifications.forEachIndexed { index, notification ->
                        Log.d(TAG, "Входящее уведомление #$index: " +
                                "ID=${notification.id}, " + 
                                "Тип=${notification.type}, " +
                                "Статус=${notification.status}, " +
                                "Заголовок=${notification.title}, " +
                                "Сообщение=${notification.message.take(50)}...")
                    }
                } else {
                    Log.d(TAG, "Входящие уведомления: список пуст")
                }
                
                _incomingNotifications.value = notifications
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке входящих уведомлений: ${e.message}", e)
                // Проверяем, является ли ошибка HttpException
                if (e is retrofit2.HttpException) {
                    try {
                        // Пытаемся получить тело ответа сервера для более детальной информации
                        val errorBody = e.response()?.errorBody()?.string()
                        Log.e(TAG, "HTTP код ошибки: ${e.code()}, тело ответа: $errorBody")
                        _error.value = "Ошибка сервера: ${e.code()}" + 
                                if (errorBody != null) ", $errorBody" else ""
                    } catch (ex: Exception) {
                        Log.e(TAG, "Не удалось прочитать тело ошибки: ${ex.message}")
                        _error.value = "Ошибка сервера: ${e.code()}"
                    }
                } else {
                    _error.value = "Ошибка загрузки уведомлений: ${e.message}"
                }
                
                // В случае ошибки загружаем пустой список, чтобы UI не ломался
                _incomingNotifications.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Загрузка исходящих уведомлений
    fun loadSentNotifications() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Загружаем исходящие уведомления")
                _isLoading.value = true
                _error.value = null  // Сбрасываем ошибку перед новым запросом
                val notifications = apiService.getSentNotifications()
                Log.d(TAG, "Получены исходящие уведомления: ${notifications.size}")
                
                // Добавляем расширенное логирование для отладки
                if (notifications.isNotEmpty()) {
                    notifications.forEachIndexed { index, notification ->
                        Log.d(TAG, "Исходящее уведомление #$index: " +
                                "ID=${notification.id}, " + 
                                "Тип=${notification.type}, " +
                                "Статус=${notification.status}, " +
                                "Заголовок=${notification.title}, " +
                                "Сообщение=${notification.message.take(50)}...")
                    }
                } else {
                    Log.d(TAG, "Исходящие уведомления: список пуст")
                }
                
                // Дополнительная фильтрация на клиенте на случай, если серверная фильтрация не сработала
                val filteredNotifications = notifications.filter { notification ->
                    // Исключаем уведомления, где отправитель и получатель совпадают
                    notification.senderId != notification.receiverId
                }
                
                _sentNotifications.value = filteredNotifications
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке исходящих уведомлений: ${e.message}", e)
                // Проверяем, является ли ошибка HttpException
                if (e is retrofit2.HttpException) {
                    try {
                        // Пытаемся получить тело ответа сервера для более детальной информации
                        val errorBody = e.response()?.errorBody()?.string()
                        Log.e(TAG, "HTTP код ошибки: ${e.code()}, тело ответа: $errorBody")
                        _error.value = "Ошибка сервера: ${e.code()}" + 
                                if (errorBody != null) ", $errorBody" else ""
                    } catch (ex: Exception) {
                        Log.e(TAG, "Не удалось прочитать тело ошибки: ${ex.message}")
                        _error.value = "Ошибка сервера: ${e.code()}"
                    }
                } else {
                    _error.value = "Ошибка загрузки уведомлений: ${e.message}"
                }
                
                // В случае ошибки загружаем пустой список, чтобы UI не ломался
                _sentNotifications.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Создать запрос на совместное владение мемориалом
    fun createMemorialOwnershipRequest(receiverId: Long, memorialId: Long, message: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Отправляем запрос на совместное владение мемориалом")
                Log.d(TAG, "receiverId: $receiverId, memorialId: $memorialId, message: $message")
                
                _isLoading.value = true
                
                val request = MemorialOwnershipRequest(
                    receiverId = receiverId.toString(),
                    memorialId = memorialId.toString(),
                    message = message
                )
                
                Log.d(TAG, "Данные запроса: $request")
                
                val notification = apiService.createMemorialOwnershipRequest(request)
                Log.d(TAG, "Успешно создано уведомление с ID: ${notification.id}")
                
                // Обновляем список исходящих
                val currentList = _sentNotifications.value?.toMutableList() ?: mutableListOf()
                currentList.add(0, notification)
                _sentNotifications.value = currentList
                
                Log.d(TAG, "Обновлен список исходящих уведомлений, теперь в нем ${currentList.size} элементов")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при создании запроса на совместное владение: ${e.message}", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Ответить на уведомление (принять или отклонить)
    fun respondToNotification(notificationId: Long, accept: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val requestData = mapOf("accept" to accept)
                val response = apiService.respondToNotification(notificationId, requestData)
                
                if (response.status == "SUCCESS") {
                    Log.d(TAG, "Ответ на уведомление отправлен успешно")
                    // Перезагружаем уведомления
                    loadIncomingNotifications()
                } else {
                    _error.value = "Ошибка при ответе на уведомление: ${response.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при ответе на уведомление", e)
                _error.value = "Ошибка при ответе на уведомление: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Ответ на запрос доступа к семейному дереву
    fun respondToFamilyTreeAccessRequest(notificationId: Long, approve: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.respondToFamilyTreeAccessRequest(notificationId, approve, null)
                
                Log.d(TAG, "Ответ на запрос доступа к дереву отправлен успешно: $response")
                // Перезагружаем уведомления
                loadIncomingNotifications()
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 200) {
                    // Успешный ответ, но проблема с десериализацией
                    Log.d(TAG, "Ответ на запрос доступа к дереву отправлен успешно (HTTP 200)")
                    loadIncomingNotifications()
                } else {
                    Log.e(TAG, "HTTP ошибка при ответе на запрос доступа к дереву: ${e.code()}", e)
                    _error.value = "Ошибка при ответе на запрос доступа к дереву: ${e.message()}"
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("JSON") == true -> {
                        Log.d(TAG, "JSON ошибка, но запрос успешен")
                        loadIncomingNotifications()
                        return@launch
                    }
                    else -> "Ошибка при ответе на запрос доступа к дереву: ${e.message}"
                }
                
                Log.e(TAG, "Ошибка при ответе на запрос доступа к дереву", e)
                _error.value = errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Отмечаем уведомление как прочитанное, id=$notificationId")
                _isLoading.value = true
                val updated = apiService.markNotificationAsRead(notificationId)
                Log.d(TAG, "Успешно отмечено уведомление с ID: ${updated.id}")
                
                // Обновляем список входящих
                val currentList = _incomingNotifications.value?.toMutableList() ?: mutableListOf()
                val index = currentList.indexOfFirst { it.id == notificationId }
                if (index != -1) {
                    currentList[index] = updated
                    _incomingNotifications.value = currentList
                    Log.d(TAG, "Обновлено уведомление в списке входящих")
                } else {
                    Log.w(TAG, "Не найдено уведомление с ID $notificationId в списке входящих")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при отметке уведомления как прочитанного: ${e.message}", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteNotification(notificationId: Long, isIncoming: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Удаляем уведомление id=$notificationId, isIncoming=$isIncoming")
                _isLoading.value = true
                apiService.deleteNotification(notificationId)
                Log.d(TAG, "Успешно удалено уведомление с ID: $notificationId")
                
                // Обновляем соответствующий список
                if (isIncoming) {
                    val currentList = _incomingNotifications.value?.toMutableList() ?: mutableListOf()
                    currentList.removeAll { it.id == notificationId }
                    _incomingNotifications.value = currentList
                    Log.d(TAG, "Удалено уведомление из списка входящих")
                } else {
                    val currentList = _sentNotifications.value?.toMutableList() ?: mutableListOf()
                    currentList.removeAll { it.id == notificationId }
                    _sentNotifications.value = currentList
                    Log.d(TAG, "Удалено уведомление из списка исходящих")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при удалении уведомления: ${e.message}", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Создать техническое уведомление для администраторов
    fun createTechnicalSupport(message: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Отправляем техническое уведомление администраторам")
                _isLoading.value = true
                
                val requestData = mapOf("message" to message)
                val response = apiService.createTechnicalSupport(requestData)
                
                Log.d(TAG, "Успешно отправлено техническое уведомление")
                onSuccess()
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при отправке технического уведомления: ${e.message}", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Методы для работы с уведомлениями о черновиках
    
    // Загрузка входящих уведомлений о черновиках (для владельцев деревьев)
    fun loadIncomingDraftSubmissions() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Загружаем входящие уведомления о черновиках")
                _isLoading.value = true
                _error.value = null
                
                val userId = RetrofitClient.getCurrentUserId()
                val submissions = apiService.getIncomingDraftSubmissions(userId)
                Log.d(TAG, "Получены входящие уведомления о черновиках: ${submissions.size}")
                
                _incomingDraftSubmissions.value = submissions
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке входящих уведомлений о черновиках: ${e.message}", e)
                _error.value = "Ошибка загрузки уведомлений о черновиках: ${e.message}"
                _incomingDraftSubmissions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Загрузка исходящих уведомлений о черновиках (для редакторов)
    fun loadOutgoingDraftSubmissions() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Загружаем исходящие уведомления о черновиках")
                _isLoading.value = true
                _error.value = null
                
                val userId = RetrofitClient.getCurrentUserId()
                val submissions = apiService.getOutgoingDraftSubmissions(userId)
                Log.d(TAG, "Получены исходящие уведомления о черновиках: ${submissions.size}")
                
                _outgoingDraftSubmissions.value = submissions
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке исходящих уведомлений о черновиках: ${e.message}", e)
                _error.value = "Ошибка загрузки уведомлений о черновиках: ${e.message}"
                _outgoingDraftSubmissions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Ответить на уведомление о черновике (одобрить/отклонить)
    fun respondToDraftSubmission(submissionId: Long, approved: Boolean, reviewMessage: String?) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Отвечаем на уведомление о черновике: submissionId=$submissionId, approved=$approved")
                _isLoading.value = true
                
                val request = ru.sevostyanov.aiscemetery.models.RespondToSubmissionRequest(
                    approved = approved,
                    reviewMessage = reviewMessage
                )
                
                apiService.respondToDraftSubmission(submissionId, request)
                Log.d(TAG, "Ответ на уведомление о черновике отправлен успешно")
                
                // Перезагружаем входящие уведомления о черновиках
                loadIncomingDraftSubmissions()
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при ответе на уведомление о черновике: ${e.message}", e)
                _error.value = "Ошибка при ответе на уведомление о черновике: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Получить конкретное уведомление о черновике по ID
    suspend fun getDraftSubmissionById(submissionId: Long): DraftSubmission? {
        return try {
            Log.d(TAG, "Получаем уведомление о черновике по ID: $submissionId")
            val submission = apiService.getDraftSubmissionById(submissionId)
            Log.d(TAG, "Получено уведомление о черновике: ${submission.id}")
            submission
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении уведомления о черновике: ${e.message}", e)
            null
        }
    }
} 