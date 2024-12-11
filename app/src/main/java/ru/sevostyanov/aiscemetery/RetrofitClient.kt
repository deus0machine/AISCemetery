package ru.sevostyanov.aiscemetery

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Request
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import ru.sevostyanov.aiscemetery.LoginActivity.LoginResponse
import ru.sevostyanov.aiscemetery.LoginActivity.UserCredentials
import ru.sevostyanov.aiscemetery.RegisterActivity.RegisterRequest
import ru.sevostyanov.aiscemetery.RegisterActivity.RegisterResponse
import java.time.LocalDate

object RetrofitClient {
    private const val BASE_URL = "http://192.168.0.101:8080" // Твой бэкэнд URL
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

    }
    // Интерфейс для LoginService
    interface LoginService {
        @POST("/api/login")
        fun login(@Body credentials: UserCredentials): Call<LoginResponse>
    }
}
