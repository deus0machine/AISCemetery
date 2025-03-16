package ru.sevostyanov.aiscemetery

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Converter
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import ru.sevostyanov.aiscemetery.LoginActivity.LoginResponse
import ru.sevostyanov.aiscemetery.LoginActivity.UserCredentials
import ru.sevostyanov.aiscemetery.RegisterActivity.RegisterRequest
import ru.sevostyanov.aiscemetery.RegisterActivity.RegisterResponse
import ru.sevostyanov.aiscemetery.memorial.Burial
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.PrivacyUpdateRequest
import ru.sevostyanov.aiscemetery.models.User
import ru.sevostyanov.aiscemetery.order.Order
import ru.sevostyanov.aiscemetery.order.OrderReport
import ru.sevostyanov.aiscemetery.task.Task
import ru.sevostyanov.aiscemetery.user.GuestItem
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.util.Date
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://192.168.0.14:8080"
    private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()
    
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }
    private var appContext: Context? = null // Контекст для доступа к SharedPreferences

    class GuestItemTypeAdapter : TypeAdapter<GuestItem?>() {
        private val gson = Gson()

        override fun write(out: JsonWriter, value: GuestItem?) {
            if (value == null) {
                out.nullValue()
            } else {
                gson.toJson(value, GuestItem::class.java, out)
            }
        }

        override fun read(reader: JsonReader): GuestItem? {
            return try {
                when (reader.peek()) {
                    com.google.gson.stream.JsonToken.NUMBER -> {
                        val id = reader.nextLong()
                        GuestItem(
                            id = id,
                            fio = "",
                            contacts = null,
                            dateOfRegistration = Date(),
                            login = "",
                            balance = 0
                        )
                    }
                    com.google.gson.stream.JsonToken.BEGIN_OBJECT -> {
                        gson.fromJson(reader, GuestItem::class.java)
                    }
                    com.google.gson.stream.JsonToken.NULL -> {
                        reader.nextNull()
                        null
                    }
                    else -> {
                        reader.skipValue()
                        null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext // Сохраняем контекст приложения
    }
    private fun getToken(): String? {
        val sharedPreferences = appContext?.getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return sharedPreferences?.getString("user_token", null) // Считываем токен из SharedPreferences
    }
    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(GuestItem::class.java, GuestItemTypeAdapter())
            .setLenient()
            .create()
    }

    private val stringConverter: Converter.Factory = object : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type,
            annotations: Array<out Annotation>,
            retrofit: Retrofit
        ): Converter<ResponseBody, *>? {
            if (type == String::class.java) {
                return Converter<ResponseBody, String> { body -> body.string() }
            }
            return null
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val token = getToken()
                val original = chain.request()
                
                // Добавляем кодировку UTF-8 для всех запросов
                val requestBuilder = original.newBuilder()
                    .header("Accept-Charset", "utf-8")
                
                // Устанавливаем Content-Type только для не-multipart запросов
                if (!original.url.toString().contains("/photo")) {
                    requestBuilder.header("Content-Type", "application/json; charset=utf-8")
                }
                
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                
                // Для GET-запросов с параметрами перекодируем их в UTF-8
                if (original.method == "GET" && original.url.query != null) {
                    val originalUrl = original.url.toString()
                    val encodedUrl = originalUrl.replace("+", "%20")
                    requestBuilder.url(encodedUrl)
                }
                
                chain.proceed(requestBuilder.build())
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(stringConverter)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
    }

    fun getApiService(): ApiService {
        return retrofit.create(ApiService::class.java)
    }
    fun getLoginService(): LoginService {
        return retrofit.create(LoginService::class.java)
    }
    interface ApiService : ru.sevostyanov.aiscemetery.order.ApiService {
        @POST("/api/register")
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

        @GET("/api/orders/orders/all")
        fun getAllOrders(): Call<List<OrderReport>>
        @PUT("/api/orders/update/{id}")
        fun updateOrderStatus(@Path("id") id: Long, @Body isCompleted: Boolean): Call<Void>
        @DELETE("/api/orders/{id}")
        fun deleteOrder(@Path("id") id: Long): Call<Void>
        @GET("/api/guest/all")
        fun getAllGuests(): Call<List<GuestItem>>
        @DELETE("/api/guest/{id}")
        fun deleteGuest(@Path("id") id: Long): Call<Void>
        @POST("/api/admin/request/email")
        fun sendRequest(@Body email: String): Call<Void>

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
    }
    // Интерфейс для LoginService
    interface LoginService {
        @POST("/api/login")
        fun login(@Body credentials: UserCredentials): Call<LoginResponse>
    }
}
