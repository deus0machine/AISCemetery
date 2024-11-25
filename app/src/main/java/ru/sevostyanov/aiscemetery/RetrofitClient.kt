package ru.sevostyanov.aiscemetery

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import ru.sevostyanov.aiscemetery.LoginActivity.LoginResponse
import ru.sevostyanov.aiscemetery.LoginActivity.UserCredentials
import ru.sevostyanov.aiscemetery.RegisterActivity.RegisterRequest
import ru.sevostyanov.aiscemetery.RegisterActivity.RegisterResponse

object RetrofitClient {
    private const val BASE_URL = "http://192.168.0.101:8080" // Твой бэкэнд URL

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
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
    }
    // Интерфейс для LoginService
    interface LoginService {
        @POST("/api/login")
        fun login(@Body credentials: UserCredentials): Call<LoginResponse>
    }
}
