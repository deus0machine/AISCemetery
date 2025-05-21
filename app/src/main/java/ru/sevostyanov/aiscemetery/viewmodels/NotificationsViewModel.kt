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
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor() : ViewModel() {
    private val TAG = "NotificationsViewModel"

    private val _incomingNotifications = MutableLiveData<List<Notification>>()
    val incomingNotifications: LiveData<List<Notification>> = _incomingNotifications

    private val _sentNotifications = MutableLiveData<List<Notification>>()
    val sentNotifications: LiveData<List<Notification>> = _sentNotifications

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val apiService = RetrofitClient.getApiService()

    // Загрузка входящих уведомлений
    fun loadIncomingNotifications() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Загружаем входящие уведомления")
                _isLoading.value = true
                val notifications = apiService.getMyNotifications()
                Log.d(TAG, "Получены входящие уведомления: ${notifications.size}")
                _incomingNotifications.value = notifications
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке входящих уведомлений: ${e.message}", e)
                _error.value = e.message
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
                val notifications = apiService.getSentNotifications()
                Log.d(TAG, "Получены исходящие уведомления: ${notifications.size}")
                _sentNotifications.value = notifications
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке исходящих уведомлений: ${e.message}", e)
                _error.value = e.message
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
                Log.d(TAG, "Отвечаем на уведомление id=$notificationId, accept=$accept")
                _isLoading.value = true
                val requestData = mapOf("accept" to accept)
                val updated = apiService.respondToNotification(notificationId, requestData)
                Log.d(TAG, "Успешно обновлен статус уведомления с ID: ${updated.id}")
                
                // Обновляем список входящих
                val currentIncomingList = _incomingNotifications.value?.toMutableList() ?: mutableListOf()
                val index = currentIncomingList.indexOfFirst { it.id == notificationId }
                if (index != -1) {
                    currentIncomingList[index] = updated
                    _incomingNotifications.value = currentIncomingList
                    Log.d(TAG, "Обновлено уведомление в списке входящих")
                } else {
                    Log.w(TAG, "Не найдено уведомление с ID $notificationId в списке входящих")
                }
                
                // После ответа на уведомление удаляем соответствующее исходящее уведомление
                // из списка, чтобы не показывать пользователю уведомления, на которые он уже ответил
                if (updated.relatedEntityId != null) {
                    val currentSentList = _sentNotifications.value?.toMutableList() ?: mutableListOf()
                    
                    // Удаляем все исходящие уведомления, которые относятся к тому же объекту
                    // и имеют тот же тип, но еще не имеют статуса ACCEPTED или REJECTED
                    val toRemove = currentSentList.filter { 
                        it.relatedEntityId == updated.relatedEntityId &&
                        it.type == updated.type &&
                        it.status == NotificationStatus.PENDING
                    }
                    
                    if (toRemove.isNotEmpty()) {
                        Log.d(TAG, "Удаляем ${toRemove.size} исходящих уведомлений после ответа")
                        
                        // Удаляем эти уведомления из списка исходящих
                        currentSentList.removeAll(toRemove)
                        _sentNotifications.value = currentSentList
                        
                        // Также удаляем их физически с сервера
                        toRemove.forEach { notification ->
                            try {
                                apiService.deleteNotification(notification.id)
                                Log.d(TAG, "Удалено исходящее уведомление ${notification.id}")
                            } catch (e: Exception) {
                                Log.e(TAG, "Ошибка удаления уведомления ${notification.id}: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при ответе на уведомление: ${e.message}", e)
                _error.value = e.message
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
} 