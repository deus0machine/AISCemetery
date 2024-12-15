package ru.sevostyanov.aiscemetery

import BurialAdapter
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
import retrofit2.http.Path
import ru.sevostyanov.aiscemetery.LoginActivity.LoginResponse
import ru.sevostyanov.aiscemetery.LoginActivity.UserCredentials
import ru.sevostyanov.aiscemetery.RegisterActivity.RegisterRequest
import ru.sevostyanov.aiscemetery.RegisterActivity.RegisterResponse

object RetrofitClient {
    private const val BASE_URL = "http://192.168.0.100:8080" // Твой бэкэнд URL
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

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
    }
    // Интерфейс для LoginService
    interface LoginService {
        @POST("/api/login")
        fun login(@Body credentials: UserCredentials): Call<LoginResponse>
    }
}
