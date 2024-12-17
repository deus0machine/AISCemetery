package ru.sevostyanov.aiscemetery

import BurialAdapter
import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import ru.sevostyanov.aiscemetery.LoginActivity.LoginResponse
import ru.sevostyanov.aiscemetery.LoginActivity.UserCredentials
import ru.sevostyanov.aiscemetery.RegisterActivity.RegisterRequest
import ru.sevostyanov.aiscemetery.RegisterActivity.RegisterResponse

object RetrofitClient {
    private const val BASE_URL = "http://192.168.0.101:8080" // Твой бэкэнд URL
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private var appContext: Context? = null // Контекст для доступа к SharedPreferences

    fun initialize(context: Context) {
        appContext = context.applicationContext // Сохраняем контекст приложения
    }


    private fun getToken(): String? {
        val sharedPreferences = appContext?.getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return sharedPreferences?.getString("user_token", null) // Считываем токен из SharedPreferences
    }
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val token = getToken()
                val requestBuilder = chain.request().newBuilder()
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token") // Добавляем заголовок Authorization
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    fun getApiService(): ApiService {
        return retrofit.create(ApiService::class.java)
    }
    fun getLoginService(): LoginService {
        return retrofit.create(LoginService::class.java)
    }
    interface ApiService : ru.sevostyanov.aiscemetery.ApiService {
        @POST("/api/register") // Убедись, что эндпоинт правильный
        fun registerUser(@Body registerRequest: RegisterRequest): Call<RegisterResponse>
        @POST("/api/burials")
        fun registerBurial(@Body burial: Burial): Call<Void>
        @GET("/api/burials/guest/{guestId}")
        suspend fun getBurialsByGuest(@Path("guestId") guestId: Long): List<Burial>
        @GET("/api/burials/fio/{fio}")
        suspend fun getBurialsByFio(@Path("fio") fio: String): List<Burial>
        @GET("/api/burials/all")
        suspend fun getAllBurials(): List<Burial>
        @GET("/api/burials/burial/{burialId}")
        suspend fun getBurialById(@Path("burialId") burialId: Long): Burial
        @DELETE("/api/burials/{id}")
        suspend fun deleteBurialById(@Path("id") burialId: Long) : Response<Void>
        @PUT("api/burials/{id}")
        suspend fun updateBurial(@Path("id") id: Long, @Body burial: Burial): Response<Void>
        @PUT("api/burials/part/{id}")
        suspend fun updatePartBurial(@Path("id") id: Long, @Body burial: Burial): Response<Void>
        @GET("/api/tasks/all")
        suspend fun getTasks(): List<Task>
        @POST("/api/tasks/perform")
        suspend fun performTask(@Body requestBody: Map<String, String>): Response<Unit>

        @GET("/api/orders/all")
        fun getOrdersBetweenDates(
            @Query("startDate") startDate: Long,
            @Query("endDate") endDate: Long
        ): Call<List<Order>>

    }
    // Интерфейс для LoginService
    interface LoginService {
        @POST("/api/login")
        fun login(@Body credentials: UserCredentials): Call<LoginResponse>
    }
}
