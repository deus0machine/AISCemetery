package ru.sevostyanov.aiscemetery

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import ru.sevostyanov.aiscemetery.models.ApproveChangesRequest
import ru.sevostyanov.aiscemetery.models.EditorRequest
import ru.sevostyanov.aiscemetery.models.FamilyTree
import ru.sevostyanov.aiscemetery.models.FamilyTreeAccess
import ru.sevostyanov.aiscemetery.models.FamilyTreeFullData
import ru.sevostyanov.aiscemetery.models.FamilyTreeUpdateDTO
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.MemorialOwnershipRequest
import ru.sevostyanov.aiscemetery.models.MemorialRelation
import ru.sevostyanov.aiscemetery.models.Notification
import ru.sevostyanov.aiscemetery.models.PagedResponse
import ru.sevostyanov.aiscemetery.user.Guest
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val TAG = "RetrofitClient"
    private const val BASE_URL = "http://192.168.0.101:8080/"
    private const val TOKEN_KEY = "auth_token"
    private const val USER_ID_KEY = "user_id"
    private const val PREF_NAME = "app_prefs"

    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null
    private var loginService: LoginService? = null
    private var applicationContext: Context? = null
    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
            setupRetrofit()
        }
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun setupRetrofit() {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(createAuthInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson: Gson = GsonBuilder()
            .setLenient()
            .create()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val currentRetrofit = retrofit
        if (currentRetrofit != null) {
            apiService = currentRetrofit.create(ApiService::class.java)
            loginService = currentRetrofit.create(LoginService::class.java)
        }
    }

    private fun createAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            // Получаем токен
            val token = getToken()
            val originalRequest = chain.request()
            
            // Проверяем, нужен ли токен для этого запроса
            // Для запросов аутентификации и регистрации токен не нужен
            val path = originalRequest.url.encodedPath
            if (path.contains("/auth/login") || path.contains("/auth/register")) {
                Log.d(TAG, "Запрос не требует токена: $path")
                return@Interceptor chain.proceed(originalRequest)
            }
            
            // Добавляем заголовок с токеном
            val requestWithToken = if (!token.isNullOrEmpty()) {
                Log.d(TAG, "Добавляем токен к запросу: $path, первые 20 символов: ${token.substring(0, Math.min(20, token.length))}...")
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                Log.w(TAG, "Токен пустой или отсутствует для запроса: $path")
                originalRequest
            }
            
            chain.proceed(requestWithToken)
        }
    }

    fun setToken(newToken: String) {
        Log.d(TAG, "Setting new token: $newToken")
        saveToken(newToken)
        setupRetrofit()
    }

    fun clearToken() {
        Log.d(TAG, "Clearing token")
        saveToken(null)
        setupRetrofit()
    }

    fun getApiService(): ApiService {
        if (apiService == null) {
            Log.e(TAG, "RetrofitClient not initialized")
            throw IllegalStateException("RetrofitClient not initialized")
        }
        return apiService!!
    }

    fun getLoginService(): LoginService {
        if (loginService == null) {
            Log.e(TAG, "RetrofitClient not initialized")
            throw IllegalStateException("RetrofitClient not initialized")
        }
        return loginService!!
    }

    fun saveToken(token: String?) {
        sharedPreferences?.edit()?.putString(TOKEN_KEY, token)?.apply()
    }

    fun getToken(): String? {
        return sharedPreferences?.getString(TOKEN_KEY, null)
    }

    fun saveUserId(userId: Long) {
        sharedPreferences?.edit()?.putLong(USER_ID_KEY, userId)?.apply()
    }

    fun getCurrentUserId(): Long {
        return sharedPreferences?.getLong(USER_ID_KEY, -1) ?: -1
    }

    interface LoginService {
        @POST("/api/login")
        fun login(@Body credentials: UserCredentials): Call<LoginResponse>
    }

    data class UserCredentials(val login: String, val password: String)

    data class LoginResponse(
        val status: String,
        val id: Long?,
        val fio: String?,
        val contacts: String?,
        val dateOfRegistration: String?,
        val login: String?,
        val hasSubscription: Boolean?,
        val role: String?,
        val token: String?
    )

    data class RegisterRequest(
        val login: String,
        val password: String,
        val fio: String,
        val contacts: String
    )

    data class RegisterResponse(
        val status: String,
        val message: String
    )

    // Wrapper для ответов API, которые возвращают данные в обёртке
    data class ResponseWrapper<T>(
        val status: String,
        val data: T,
        val message: String? = null
    )

    interface ApiService {
        @POST("/api/register")
        fun registerUser(@Body registerRequest: RegisterRequest): Call<RegisterResponse>

        @GET("/api/guest/get/{guestId}")
        suspend fun getGuest(@Path("guestId") guestId: Long): ru.sevostyanov.aiscemetery.user.Guest

        @GET("/api/guest/all")
        fun getAllGuests(): Call<List<ru.sevostyanov.aiscemetery.user.Guest>>

        @DELETE("/api/guest/{id}")
        fun deleteGuest(@Path("id") id: Long): Call<Void>

        @POST("/api/admin/request/email")
        fun sendRequest(@Body email: String): Call<Void>

        @GET("/api/memorials")
        suspend fun getAllMemorials(): List<Memorial>

        @GET("/api/memorials")
        suspend fun getAllMemorials(
            @Query("page") page: Int,
            @Query("size") size: Int
        ): PagedResponse<Memorial>

        @GET("/api/memorials/my")
        suspend fun getMyMemorials(
            @Query("page") page: Int,
            @Query("size") size: Int
        ): PagedResponse<Memorial>

        @GET("/api/memorials/public")
        suspend fun getPublicMemorials(): PagedResponse<Memorial>

        @GET("/api/memorials/public")
        suspend fun getPublicMemorials(
            @Query("page") page: Int,
            @Query("size") size: Int
        ): PagedResponse<Memorial>

        @GET("/api/memorials/{id}")
        suspend fun getMemorialById(@Path("id") id: Long): Memorial

        @POST("/api/memorials")
        suspend fun createMemorial(@Body memorial: Memorial): Memorial

        @PUT("/api/memorials/{id}")
        suspend fun updateMemorial(@Path("id") id: Long, @Body memorial: Memorial): Memorial

        @DELETE("/api/memorials/{id}")
        suspend fun deleteMemorial(@Path("id") id: Long)

        @PUT("/api/memorials/{id}/privacy")
        suspend fun updateMemorialPrivacy(@Path("id") id: Long, @Body isPublic: Boolean)

        @GET("/api/memorials/{id}/editors")
        suspend fun getMemorialEditors(@Path("id") id: Long): List<ru.sevostyanov.aiscemetery.user.Guest>

        @POST("/api/memorials/{id}/editors")
        suspend fun manageEditor(@Path("id") id: Long, @Body request: EditorRequest): Memorial

        @POST("/api/memorials/{id}/approve-changes")
        suspend fun approveChanges(@Path("id") id: Long, @Body request: ApproveChangesRequest): Memorial

        @GET("/api/memorials/edited")
        suspend fun getEditedMemorials(): List<Memorial>

        @GET("/api/memorials/{id}/pending-changes")
        suspend fun getMemorialPendingChanges(@Path("id") id: Long): Memorial

        @Multipart
        @POST("/api/memorials/{id}/photo")
        suspend fun uploadMemorialPhoto(@Path("id") id: Long, @Part photo: MultipartBody.Part): ResponseBody

        @DELETE("/api/memorials/{id}/photo")
        suspend fun deleteMemorialPhoto(@Path("id") id: Long)

        @Multipart
        @POST("/api/memorials/{id}/document")
        suspend fun uploadMemorialDocument(@Path("id") id: Long, @Part document: MultipartBody.Part): ResponseBody

        @GET("/api/memorials/search")
        suspend fun searchMemorials(
            @Query("query") query: String,
            @Query("location") location: String?,
            @Query("startDate") startDate: String?,
            @Query("endDate") endDate: String?,
            @Query("isPublic") isPublic: Boolean?
        ): List<Memorial>

        @GET("/api/memorials/search")
        suspend fun searchMemorials(
            @Query("query") query: String,
            @Query("location") location: String?,
            @Query("startDate") startDate: String?,
            @Query("endDate") endDate: String?,
            @Query("isPublic") isPublic: Boolean?,
            @Query("page") page: Int,
            @Query("size") size: Int
        ): PagedResponse<Memorial>

        @GET("api/family-trees/my")
        suspend fun getMyFamilyTrees(): List<FamilyTree>

        @GET("api/family-trees/public")
        suspend fun getPublicFamilyTrees(): List<FamilyTree>

        @GET("api/family-trees/accessible")
        suspend fun getAccessibleFamilyTrees(): List<FamilyTree>

        @GET("api/family-trees/{id}")
        suspend fun getFamilyTreeById(@Path("id") id: Long): FamilyTree

        @GET("api/family-trees/{id}/full-data")
        suspend fun getFamilyTreeFullData(@Path("id") id: Long): FamilyTreeFullData

        @POST("api/family-trees")
        suspend fun createFamilyTree(@Body familyTree: FamilyTree): FamilyTree

        @PUT("api/family-trees/{id}")
        suspend fun updateFamilyTree(
            @Path("id") id: Long,
            @Body updateDTO: FamilyTreeUpdateDTO
        ): FamilyTree

        @DELETE("api/family-trees/{id}")
        suspend fun deleteFamilyTree(@Path("id") id: Long): Response<Unit>

        @GET("api/family-trees/search")
        suspend fun searchFamilyTrees(
            @Query("query") query: String?,
            @Query("ownerName") ownerName: String?,
            @Query("startDate") startDate: String?,
            @Query("endDate") endDate: String?,
            @Query("myOnly") myOnly: Boolean = false
        ): List<FamilyTree>

        @GET("api/family-trees/{treeId}/access")
        suspend fun getFamilyTreeAccess(@Path("treeId") treeId: Long): List<FamilyTreeAccess>

        @POST("api/family-trees/{treeId}/access")
        suspend fun grantFamilyTreeAccess(
            @Path("treeId") treeId: Long,
            @Query("userId") userId: Long,
            @Query("accessLevel") accessLevel: String
        ): FamilyTreeAccess

        @PUT("api/family-trees/{treeId}/access/{userId}")
        suspend fun updateFamilyTreeAccess(
            @Path("treeId") treeId: Long,
            @Path("userId") userId: Long,
            @Query("accessLevel") accessLevel: String
        ): FamilyTreeAccess

        @DELETE("api/family-trees/{treeId}/access/{userId}")
        suspend fun revokeFamilyTreeAccess(
            @Path("treeId") treeId: Long,
            @Path("userId") userId: Long
        ): Response<Unit>

        @GET("api/family-trees/{treeId}/relations")
        suspend fun getMemorialRelations(@Path("treeId") treeId: Long): List<MemorialRelation>

        @POST("api/family-trees/{treeId}/relations")
        suspend fun createMemorialRelation(
            @Path("treeId") treeId: Long,
            @Body relation: MemorialRelation
        ): MemorialRelation

        @PUT("api/family-trees/{treeId}/relations/{relationId}")
        suspend fun updateMemorialRelation(
            @Path("treeId") treeId: Long,
            @Path("relationId") relationId: Long,
            @Body relation: MemorialRelation
        ): MemorialRelation

        @DELETE("api/family-trees/{treeId}/relations/{relationId}")
        suspend fun deleteMemorialRelation(
            @Path("treeId") treeId: Long,
            @Path("relationId") relationId: Long
        ): Response<Unit>

        // Методы для работы с мемориалами в дереве
        @GET("api/family-trees/{treeId}/memorials")
        suspend fun getFamilyTreeMemorials(@Path("treeId") treeId: Long): List<Memorial>

        @POST("api/family-trees/{treeId}/memorials/{memorialId}")
        suspend fun addMemorialToTree(
            @Path("treeId") treeId: Long,
            @Path("memorialId") memorialId: Long
        ): Response<Unit>

        @DELETE("api/family-trees/{treeId}/memorials/{memorialId}")
        suspend fun removeMemorialFromTree(
            @Path("treeId") treeId: Long,
            @Path("memorialId") memorialId: Long
        ): Response<Unit>

        // Notifications API
        @GET("/api/notifications")
        suspend fun getMyNotifications(): List<Notification>

        @GET("/api/notifications/sent")
        suspend fun getSentNotifications(): List<Notification>

        @POST("/api/notifications/memorial-ownership")
        suspend fun createMemorialOwnershipRequest(@Body request: MemorialOwnershipRequest): Notification

        @POST("/api/notifications/{id}/respond")
        suspend fun respondToNotification(
            @Path("id") id: Long,
            @Body requestData: Map<String, Boolean>
        ): ResponseWrapper<Notification>

        @POST("/api/notifications/{id}/read")
        suspend fun markNotificationAsRead(@Path("id") id: Long): Notification

        @DELETE("/api/notifications/{id}")
        suspend fun deleteNotification(@Path("id") id: Long): Response<Unit>
        
        // Метод для отправки технических уведомлений администраторам
        @POST("/api/notifications/technical-support")
        suspend fun createTechnicalSupport(@Body requestData: Map<String, String>): ResponseWrapper<Notification>
        
        // Методы для модерации мемориалов
        @POST("/api/memorials/{id}/send-for-moderation")
        suspend fun sendMemorialForModeration(@Path("id") id: Long): Memorial
        
        @POST("/api/memorials/{id}/send-changes-for-moderation")
        suspend fun sendChangesForModeration(@Path("id") id: Long): Memorial
        
        @POST("/api/memorials/{id}/approve")
        suspend fun approveMemorial(@Path("id") id: Long): Memorial
        
        @POST("/api/memorials/{id}/reject")
        suspend fun rejectMemorial(@Path("id") id: Long, @Body reason: String): Memorial
        
        @POST("/api/memorials/{id}/admin/approve-changes")
        suspend fun approveChangesByAdmin(@Path("id") id: Long): Memorial
        
        @POST("/api/memorials/{id}/admin/reject-changes")
        suspend fun rejectChangesByAdmin(@Path("id") id: Long, @Body reason: String): Memorial
        
        // Метод для отправки жалобы на мемориал
        @POST("/api/memorials/{id}/report")
        suspend fun reportMemorial(@Path("id") id: Long, @Body request: ru.sevostyanov.aiscemetery.models.MemorialReportRequest): ru.sevostyanov.aiscemetery.models.MemorialReportResponse

        // Методы для модерации семейных деревьев
        @POST("api/family-trees/{id}/send-for-moderation")
        suspend fun sendFamilyTreeForModeration(@Path("id") id: Long): FamilyTree

        @POST("api/family-trees/{id}/unpublish")
        suspend fun unpublishFamilyTree(@Path("id") id: Long): FamilyTree
        
        @POST("api/family-trees/{id}/approve")
        suspend fun approveFamilyTree(@Path("id") id: Long): FamilyTree
        
        @POST("api/family-trees/{id}/reject")
        suspend fun rejectFamilyTree(@Path("id") id: Long, @Body reason: String): FamilyTree
    }
}
