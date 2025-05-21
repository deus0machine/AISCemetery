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
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.PrivacyUpdateRequest
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

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
        apiService.getMemorialById(id)
    }

    suspend fun createMemorial(memorial: Memorial): Memorial = withContext(Dispatchers.IO) {
        apiService.createMemorial(memorial)
    }

    suspend fun updateMemorial(id: Long, memorial: Memorial): Memorial = withContext(Dispatchers.IO) {
        try {
            println("Updating memorial with id: $id")
            println("Memorial data: $memorial")
            apiService.updateMemorial(id, memorial)
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка при обновлении мемориала: ${e.message}\nПолный стек: ${e.stackTraceToString()}")
        }
    }

    suspend fun deleteMemorial(id: Long) = withContext(Dispatchers.IO) {
        apiService.deleteMemorial(id)
    }

    suspend fun updateMemorialPrivacy(id: Long, isPublic: Boolean) = withContext(Dispatchers.IO) {
        try {
            // Проверяем подписку пользователя, если пытаемся сделать мемориал публичным
            if (isPublic) {
                val user = RetrofitClient.getApiService().getGuest(RetrofitClient.getCurrentUserId())
                if (user.hasSubscription != true) {
                    throw Exception("Для публикации мемориала требуется подписка")
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

    companion object {
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10MB
    }
} 