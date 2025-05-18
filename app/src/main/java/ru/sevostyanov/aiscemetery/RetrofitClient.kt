package ru.sevostyanov.aiscemetery

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
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
import ru.sevostyanov.aiscemetery.models.FamilyTree
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.MemorialRelation
import ru.sevostyanov.aiscemetery.order.Order
import ru.sevostyanov.aiscemetery.order.OrderReport
import ru.sevostyanov.aiscemetery.task.Task
import ru.sevostyanov.aiscemetery.user.Guest
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val TAG = "RetrofitClient"
    private const val BASE_URL = "http://192.168.0.102:8080/"
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
            .addInterceptor { chain ->
                val original: Request = chain.request()
                val requestBuilder: Request.Builder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .method(original.method, original.body)

                val token = getToken()
                if (token != null) {
                    requestBuilder.header("Authorization", "Bearer $token")
                    Log.d(TAG, "Adding token to request: Bearer $token")
                } else {
                    Log.d(TAG, "No token available for request")
                }

                val request: Request = requestBuilder.build()
                val response = chain.proceed(request)

                // Проверяем ответ на ошибки авторизации
                if (response.code == 401 || response.code == 403) {
                    Log.d(TAG, "Token expired or invalid, clearing token")
                    clearToken()
                    // Очищаем данные пользователя
                    applicationContext?.let { context ->
                        try {
                            ru.sevostyanov.aiscemetery.user.UserManager.clearUserData(context)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error clearing user data", e)
                        }
                    }
                }

                response
            }
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
            throw IllegalStateException("RetrofitClient not initialized")
        }
        return apiService!!
    }

    fun getLoginService(): LoginService {
        if (loginService == null) {
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
        val balance: Long?,
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

    interface ApiService {
        @POST("/api/register")
        fun registerUser(@Body registerRequest: RegisterRequest): Call<RegisterResponse>

        @GET("api/orders/guest/{guestId}")
        suspend fun getOrdersByGuest(@Path("guestId") guestId: Long): List<Order>

        @GET("/api/guest/get/{guestId}")
        suspend fun getGuest(@Path("guestId") guestId: Long): Guest

        @GET("/api/orders/all")
        fun getOrdersBetweenDates(
            @Query("startDate") startDate: Long,
            @Query("endDate") endDate: Long
        ): Call<List<Order>>

        @GET("/api/orders/orders/all")
        fun getAllOrders(): Call<List<OrderReport>>

        @GET("/api/guest/all")
        fun getAllGuests(): Call<List<Guest>>

        @DELETE("/api/guest/{id}")
        fun deleteGuest(@Path("id") id: Long): Call<Void>

        @POST("/api/admin/request/email")
        fun sendRequest(@Body email: String): Call<Void>

        @GET("/api/tasks/all")
        suspend fun getTasks(): List<Task>
        
        @POST("/api/tasks/perform")
        suspend fun performTask(@Body requestBody: Map<String, String>): Response<Unit>

        @GET("/api/memorials")
        suspend fun getAllMemorials(): List<Memorial>

        @GET("/api/memorials/my")
        suspend fun getMyMemorials(): List<Memorial>

        @GET("/api/memorials/public")
        suspend fun getPublicMemorials(): List<Memorial>

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

        @Multipart
        @POST("/api/memorials/{id}/photo")
        suspend fun uploadMemorialPhoto(@Path("id") id: Long, @Part photo: MultipartBody.Part): String

        @DELETE("/api/memorials/{id}/photo")
        suspend fun deleteMemorialPhoto(@Path("id") id: Long)

        @GET("/api/memorials/search")
        suspend fun searchMemorials(
            @Query("query") query: String,
            @Query("location") location: String?,
            @Query("startDate") startDate: String?,
            @Query("endDate") endDate: String?,
            @Query("isPublic") isPublic: Boolean?
        ): List<Memorial>

        @GET("/api/family-trees/my")
        suspend fun getMyFamilyTrees(): List<FamilyTree>

        @GET("/api/family-trees/public")
        suspend fun getPublicFamilyTrees(): List<FamilyTree>

        @GET("/api/family-trees/accessible")
        suspend fun getAccessibleFamilyTrees(): List<FamilyTree>

        @GET("/api/family-trees/{id}")
        suspend fun getFamilyTreeById(@Path("id") id: Long): FamilyTree

        @POST("/api/family-trees")
        suspend fun createFamilyTree(@Body familyTree: FamilyTree): FamilyTree

        @PUT("/api/family-trees/{id}")
        suspend fun updateFamilyTree(@Path("id") id: Long, @Body familyTree: FamilyTree): FamilyTree

        @DELETE("/api/family-trees/{id}")
        suspend fun deleteFamilyTree(@Path("id") id: Long)

        @GET("/api/family-trees/{familyTreeId}/memorial-relations")
        suspend fun getMemorialRelations(@Path("familyTreeId") familyTreeId: Long): List<MemorialRelation>

        @POST("/api/family-trees/{familyTreeId}/memorial-relations")
        suspend fun createMemorialRelation(
            @Path("familyTreeId") familyTreeId: Long,
            @Body relation: MemorialRelation
        ): MemorialRelation

        @PUT("/api/family-trees/{familyTreeId}/memorial-relations/{relationId}")
        suspend fun updateMemorialRelation(
            @Path("familyTreeId") familyTreeId: Long,
            @Path("relationId") relationId: Long,
            @Body relation: MemorialRelation
        ): MemorialRelation

        @DELETE("/api/family-trees/{familyTreeId}/memorial-relations/{relationId}")
        suspend fun deleteMemorialRelation(
            @Path("familyTreeId") familyTreeId: Long,
            @Path("relationId") relationId: Long
        )

        @GET("/api/family-trees/search")
        suspend fun searchFamilyTrees(
            @Query("query") query: String = "",
            @Query("isPublic") isPublic: Boolean? = null
        ): List<FamilyTree>
    }
}
