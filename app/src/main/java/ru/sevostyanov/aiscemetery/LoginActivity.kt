package ru.sevostyanov.aiscemetery

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

class LoginActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    val loginService = RetrofitClient.getLoginService()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.initialize(this)
        setContentView(R.layout.activity_login)

        // Очищаем старые данные при запуске активити
        clearUserData()

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

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_fio"
        const val KEY_USER_CONTACTS = "user_contacts"
        const val KEY_USER_REG_DATE = "user_dateOfRegistration"
        const val KEY_USER_BALANCE = "user_balance"
        const val KEY_USER_ROLE = "user_role"
        const val KEY_USER_TOKEN = "user_token"
    }

    private fun saveUserData(userId: Long, fio: String, contacts: String, regDate: String, balance: Long, role: String, token: String) {
        val sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, fio)
            putString(KEY_USER_CONTACTS, contacts)
            putString(KEY_USER_REG_DATE, regDate)
            putLong(KEY_USER_BALANCE, balance)
            putString(KEY_USER_ROLE, role) // Сохраняем роль
            putString(KEY_USER_TOKEN, token)
            apply()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
                        userProfile?.dateOfRegistration ?: "Неизвестно",
                        userProfile?.balance ?: -1L,
                        userProfile?.role ?: "USER", // Передаём роль, по умолчанию "USER"
                        userProfile?.token ?: "" // Сохраняем токен
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

    // Интерфейс для LoginService
    interface LoginService {
        @POST("/api/login")
        fun login(@Body credentials: UserCredentials): Call<LoginResponse>
    }

    private fun clearUserData() {
        val sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }
}

