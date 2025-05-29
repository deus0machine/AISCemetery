package ru.sevostyanov.aiscemetery.repository

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.sevostyanov.aiscemetery.RetrofitClient
import ru.sevostyanov.aiscemetery.models.ApproveChangesRequest
import ru.sevostyanov.aiscemetery.models.EditorRequest
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.PrivacyUpdateRequest
import ru.sevostyanov.aiscemetery.models.PublicationStatus
import ru.sevostyanov.aiscemetery.user.Guest
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import retrofit2.HttpException
import android.util.Log

class MemorialRepository {
    private val apiService = RetrofitClient.getApiService()

    suspend fun getAllMemorials(): List<Memorial> = withContext(Dispatchers.IO) {
        apiService.getAllMemorials()
    }

    suspend fun getMyMemorials(): List<Memorial> = withContext(Dispatchers.IO) {
        apiService.getMyMemorials()
    }

    suspend fun getPublicMemorials(): List<Memorial> = withContext(Dispatchers.IO) {
        apiService.getPublicMemorials()
    }

    suspend fun getMemorialById(id: Long): Memorial = withContext(Dispatchers.IO) {
        val result = apiService.getMemorialById(id)
        println("LOCATION DEBUG - Received memorial from server:")
        println("Memorial: $result")
        println("PublicationStatus: ${result.publicationStatus}")
        if (result.mainLocation != null) {
            println("mainLocation: ${result.mainLocation}")
        }
        if (result.burialLocation != null) {
            println("burialLocation: ${result.burialLocation}")
        }
        result
    }

    suspend fun createMemorial(memorial: Memorial): Memorial = withContext(Dispatchers.IO) {
        try {
            Log.d("MemorialRepository", "Создание нового мемориала")
            apiService.createMemorial(memorial)
        } catch (e: Exception) {
            Log.e("MemorialRepository", "Ошибка при создании мемориала: ${e.message}", e)
            throw Exception("Не удалось создать мемориал: ${e.message}")
        }
    }

    suspend fun updateMemorial(id: Long, memorial: Memorial): Memorial = withContext(Dispatchers.IO) {
        try {
            Log.d("MemorialRepository", "Обновление мемориала с id: $id")
            Log.d("MemorialRepository", "Данные мемориала: $memorial")
            
            // Проверяем текущий мемориал на сервере
            val currentMemorial = apiService.getMemorialById(id)
            
            // Проверяем, находится ли мемориал на модерации
            if (currentMemorial.publicationStatus == PublicationStatus.PENDING_MODERATION) {
                Log.e("MemorialRepository", "Попытка обновить мемориал на модерации! id=$id, статус=${currentMemorial.publicationStatus}")
                throw Exception("Невозможно обновить мемориал, находящийся на модерации")
            }
            
            // Дополнительная проверка на всякий случай
            memorial.id?.let { memorialId ->
                if (memorial.publicationStatus == PublicationStatus.PENDING_MODERATION) {
                    Log.e("MemorialRepository", "Попытка отправить мемориал на модерации! id=$memorialId, статус=${memorial.publicationStatus}")
                    throw Exception("Невозможно обновить мемориал, находящийся на модерации")
                }
            }
            
            // Выполняем обновление
            val result = apiService.updateMemorial(id, memorial)
            Log.d("MemorialRepository", "Мемориал успешно обновлен, id=$id")
            result
        } catch (e: Exception) {
            Log.e("MemorialRepository", "Ошибка при обновлении мемориала: ${e.message}", e)
            throw Exception("Ошибка при обновлении мемориала: ${e.message}")
        }
    }

    suspend fun deleteMemorial(id: Long) = withContext(Dispatchers.IO) {
        apiService.deleteMemorial(id)
    }

    suspend fun updateMemorialPrivacy(id: Long, isPublic: Boolean) = withContext(Dispatchers.IO) {
        try {
            // Проверяем подписку пользователя, если пытаемся сделать мемориал публичным
            if (isPublic) {
                try {
                    val userId = RetrofitClient.getCurrentUserId()
                    val user = RetrofitClient.getApiService().getGuest(userId)
                    if (user.hasSubscription != true) {
                        throw Exception("Для публикации мемориала требуется подписка")
                    }
                } catch (e: HttpException) {
                    if (e.code() == 404) {
                        throw Exception("Пользователь не найден")
                    } else {
                        throw Exception("Ошибка получения информации о пользователе: ${e.message()}")
                    }
                }
            }
            
            println("Отправляем запрос на обновление статуса: id=$id, isPublic=$isPublic")
            apiService.updateMemorialPrivacy(id, isPublic)
            println("Статус успешно обновлен")
            
            // Проверяем обновленный статус
            val updatedMemorial = apiService.getMemorialById(id)
            println("Проверка после обновления: id=${updatedMemorial.id}, isPublic=${updatedMemorial.isPublic}")
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Не удалось обновить статус мемориала: ${e.message}")
        }
    }
    
    // Получение списка редакторов мемориала
    suspend fun getMemorialEditors(id: Long): List<Guest> = withContext(Dispatchers.IO) {
        try {
            apiService.getMemorialEditors(id)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Не удалось получить список редакторов: ${e.message}")
        }
    }
    
    // Добавление или удаление редактора
    suspend fun manageEditor(memorialId: Long, userId: Long, addEditor: Boolean): Memorial = withContext(Dispatchers.IO) {
        try {
            val request = EditorRequest(
                userId = userId,
                action = if (addEditor) "add" else "remove"
            )
            apiService.manageEditor(memorialId, request)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Не удалось ${if (addEditor) "добавить" else "удалить"} редактора: ${e.message}")
        }
    }
    
    // Получение мемориалов, требующих подтверждения изменений
    suspend fun getEditedMemorials(): List<Memorial> = withContext(Dispatchers.IO) {
        try {
            apiService.getEditedMemorials()
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Не удалось получить список мемориалов с изменениями: ${e.message}")
        }
    }
    
    // Подтверждение или отклонение изменений мемориала
    suspend fun approveChanges(memorialId: Long, approve: Boolean): Memorial = withContext(Dispatchers.IO) {
        try {
            val request = ApproveChangesRequest(approve = approve)
            apiService.approveChanges(memorialId, request)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Не удалось ${if (approve) "подтвердить" else "отклонить"} изменения: ${e.message}")
        }
    }

    suspend fun uploadPhoto(id: Long, photoUri: Uri, context: Context): String = withContext(Dispatchers.IO) {
        try {
            // Создаем уникальное имя файла
            val extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(context.contentResolver.getType(photoUri))
                ?: "jpg"
            val fileName = "photo_${UUID.randomUUID()}.$extension"

            // Создаем временный файл
            val tempFile = File(context.cacheDir, fileName)

            try {
                // Копируем содержимое URI во временный файл
                context.contentResolver.openInputStream(photoUri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw Exception("Не удалось открыть файл изображения")

                // Проверяем размер файла
                if (tempFile.length() > MAX_FILE_SIZE) {
                    throw Exception("Размер файла не должен превышать 10MB")
                }

                // Создаем MultipartBody.Part
                val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("photo", fileName, requestFile)

                // Отправляем файл и возвращаем результат
                try {
                    val response = apiService.uploadMemorialPhoto(id, body)
                    val result = response.string()
                    
                    // Удаляем временный файл
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                    
                    result
                } catch (e: java.net.UnknownHostException) {
                    throw Exception("Ошибка подключения к серверу. Проверьте подключение к интернету.")
                } catch (e: java.net.SocketTimeoutException) {
                    throw Exception("Превышено время ожидания ответа от сервера. Попробуйте позже.")
                } catch (e: javax.net.ssl.SSLHandshakeException) {
                    throw Exception("Ошибка безопасного соединения с сервером.")
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw Exception("Ошибка при загрузке фото: ${e.message}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw Exception("Ошибка при работе с файлом: ${e.message}")
            } finally {
                // Удаляем временный файл в любом случае
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при загрузке фото: ${e.message}")
        }
    }

    suspend fun deletePhoto(id: Long) = withContext(Dispatchers.IO) {
        try {
            apiService.deleteMemorialPhoto(id)
        } catch (e: Exception) {
            throw Exception("Не удалось удалить фото мемориала: ${e.message}")
        }
    }

    suspend fun searchMemorials(
        query: String = "",
        location: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        isPublic: Boolean? = null
    ): List<Memorial> = withContext(Dispatchers.IO) {
        apiService.searchMemorials(query, location, startDate, endDate, isPublic)
    }

    // Получить подробности ожидающих изменений мемориала для предпросмотра
    suspend fun getMemorialPendingChanges(id: Long): Memorial = withContext(Dispatchers.IO) {
        val result = apiService.getMemorialPendingChanges(id)
        println("DEBUG - Received pending changes for memorial ID $id")
        result
    }

    // Отправить мемориал на модерацию
    suspend fun sendMemorialForModeration(id: Long): Memorial = withContext(Dispatchers.IO) {
        try {
            apiService.sendMemorialForModeration(id)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Не удалось отправить мемориал на модерацию: ${e.message}")
        }
    }
    
    // Одобрить публикацию мемориала (для администраторов)
    suspend fun approveMemorial(id: Long): Memorial = withContext(Dispatchers.IO) {
        try {
            apiService.approveMemorial(id)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Не удалось одобрить публикацию мемориала: ${e.message}")
        }
    }
    
    // Отклонить публикацию мемориала с указанием причины (для администраторов)
    suspend fun rejectMemorial(id: Long, reason: String): Memorial = withContext(Dispatchers.IO) {
        try {
            apiService.rejectMemorial(id, reason)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Не удалось отклонить публикацию мемориала: ${e.message}")
        }
    }

    companion object {
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10MB
    }
} 