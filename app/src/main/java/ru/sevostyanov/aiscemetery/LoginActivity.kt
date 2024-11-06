package ru.sevostyanov.aiscemetery

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

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.0.101:8080") // URL бэкэнда
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        loginService = retrofit.create(LoginService::class.java)

        val loginButton = findViewById<Button>(R.id.login_button)
        val registerLink = findViewById<TextView>(R.id.register_link)
        val loginField = findViewById<EditText>(R.id.login_email)
        val passwordField = findViewById<EditText>(R.id.login_password)

        loginButton.setOnClickListener {
            val login = loginField.text.toString()
            val password = passwordField.text.toString()
            authenticateUser(login, password)
        }

        // Переход на экран регистрации
        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
    private fun authenticateUser(login: String, password: String) {
        val call = loginService.login(UserCredentials(login, password))

        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body()?.status == "SUCCESS") {
                    val userProfile = response.body()

                    // Переход в MainActivity с данными профиля
                    val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                        putExtra("user_id", userProfile?.id)
                        putExtra("user_fio", userProfile?.fio)
                        putExtra("user_contacts", userProfile?.contacts)
                        putExtra("user_dateOfRegistration", userProfile?.dateOfRegistration)
                        putExtra("user_login", userProfile?.login)
                    }
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

    // Интерфейс для Retrofit
    interface LoginService {
        @POST("/api/login")
        fun login(@Body credentials: UserCredentials): Call<LoginResponse>
    }
}