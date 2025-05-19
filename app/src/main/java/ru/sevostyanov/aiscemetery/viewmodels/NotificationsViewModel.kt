package ru.sevostyanov.aiscemetery.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.RetrofitClient
import ru.sevostyanov.aiscemetery.models.Notification
import ru.sevostyanov.aiscemetery.models.NotificationType
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor() : ViewModel() {

    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val apiService = RetrofitClient.getApiService()

    fun loadNotifications() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // Временно используем тестовые данные
                _notifications.value = getTestNotifications()
                // Когда API будет готов, раскомментируйте следующую строку:
                // _notifications.value = apiService.getNotifications()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // Временно обновляем локально
                val currentList = _notifications.value?.toMutableList() ?: mutableListOf()
                val index = currentList.indexOfFirst { it.id == notificationId }
                if (index != -1) {
                    currentList[index] = currentList[index].copy(isRead = true)
                    _notifications.value = currentList
                }
                // Когда API будет готов, раскомментируйте следующую строку:
                // apiService.markNotificationAsRead(notificationId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteNotification(notificationId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // Временно удаляем локально
                val currentList = _notifications.value?.toMutableList() ?: mutableListOf()
                currentList.removeAll { it.id == notificationId }
                _notifications.value = currentList
                // Когда API будет готов, раскомментируйте следующую строку:
                // apiService.deleteNotification(notificationId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getTestNotifications(): List<Notification> {
        return listOf(
            Notification(
                id = 1,
                userId = 1,
                type = NotificationType.TREE_ACCESS_REQUEST,
                title = "Новый запрос на доступ",
                message = "Пользователь Иван Иванов запрашивает доступ к вашему древу 'Род Ивановых'",
                relatedId = 1,
                isRead = false,
                createdAt = "2024-03-20T10:00:00"
            ),
            Notification(
                id = 2,
                userId = 1,
                type = NotificationType.MEMORIAL_COMMENT,
                title = "Новый комментарий",
                message = "Пользователь Петр Петров оставил комментарий к мемориалу",
                relatedId = 2,
                isRead = false,
                createdAt = "2024-03-20T09:30:00"
            ),
            Notification(
                id = 3,
                userId = 1,
                type = NotificationType.ANNIVERSARY,
                title = "Годовщина",
                message = "Завтра годовщина памяти Ивана Ивановича",
                relatedId = 1,
                isRead = true,
                createdAt = "2024-03-19T15:00:00"
            )
        )
    }
} 