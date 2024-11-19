package ru.sevostyanov.aiscemetery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class LoginActivity : AppCompatActivity() {

    private lateinit var loginService: LoginService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Инициализация API-сервиса через RetrofitClient
        loginService = RetrofitClient.getApiService()

        val loginButton = findViewById<Button>(R.id.login_button)
        val registerLink = findViewById<TextView>(R.id.register_link)
        val loginField = findViewById<EditText>(R.id.login_email)
        val passwordField = findViewById<EditText>(R.id.login_password)

        // Авторизация
        loginButton.setOnClickListener {
            val login = loginField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (login.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
            } else {
                authenticateUser(login, password)
            }
        }

        // Переход на экран регистрации
        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_fio"
        const val KEY_USER_CONTACTS = "user_contacts"
        const val KEY_USER_REG_DATE = "user_dateOfRegistration"
    }

    private fun saveUserData(userId: Long, fio: String, contacts: String, regDate: String) {
        val sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, fio)
            putString(KEY_USER_CONTACTS, contacts)
            putString(KEY_USER_REG_DATE, regDate)
            apply()
        }
    }

    private fun authenticateUser(login: String, password: String) {
        val call = loginService.login(UserCredentials(login, password))

        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body()?.status == "SUCCESS") {
                    val userProfile = response.body()
                    saveUserData(
                        userProfile?.id ?: -1L,
                        userProfile?.fio ?: "Неизвестно",
                        userProfile?.contacts ?: "Неизвестно",
                        userProfile?.dateOfRegistration ?: "Неизвестно"
                    )

                    // Запуск MainActivity
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Ошибка подключения: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Данные для запроса
    data class UserCredentials(val login: String, val password: String)

    // Ожидаемый ответ от сервера
    data class LoginResponse(
        val status: String,
        val id: Long?,
        val fio: String?,
        val contacts: String?,
        val dateOfRegistration: String?,
        val login: String?
    )

    // Интерфейс для LoginService
    interface LoginService {
        @POST("/api/login")
        fun login(@Body credentials: UserCredentials): Call<LoginResponse>
    }
}

