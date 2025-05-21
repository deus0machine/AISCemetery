package ru.sevostyanov.aiscemetery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.sevostyanov.aiscemetery.user.Guest
import ru.sevostyanov.aiscemetery.user.UserManager

class LoginActivity : AppCompatActivity() {
    private var isInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (isInitialized) {
            Log.d("LoginActivity", "Activity already initialized, skipping")
            return
        }
        
        try {
            // Инициализируем RetrofitClient
            RetrofitClient.initialize(this)
            
            // Проверяем, есть ли активная сессия
            val user = UserManager.getCurrentUser() ?: UserManager.loadUserFromPreferences(this)
            if (user != null) {
                Log.d("LoginActivity", "User already logged in, starting MainActivity")
                startMainActivity()
                return
            }

            setContentView(R.layout.activity_login)
            isInitialized = true

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
        } catch (e: Exception) {
            Log.e("LoginActivity", "Initialization error", e)
            Toast.makeText(this, "Ошибка инициализации: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Проверяем авторизацию только если активити уже инициализирована
        if (isInitialized) {
            val user = UserManager.getCurrentUser() ?: UserManager.loadUserFromPreferences(this)
            if (user != null) {
                Log.d("LoginActivity", "User logged in during onResume, starting MainActivity")
                startMainActivity()
            }
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_CONTACTS = "user_contacts"
        const val KEY_USER_REG_DATE = "user_reg_date"
        const val KEY_USER_LOGIN = "user_login"
        const val KEY_USER_HAS_SUBSCRIPTION = "user_has_subscription"
        const val KEY_USER_ROLE = "user_role"
        const val KEY_USER_TOKEN = "user_token"
    }

    private fun authenticateUser(login: String, password: String) {
        try {
            val call = RetrofitClient.getLoginService().login(RetrofitClient.UserCredentials(login, password))

            call.enqueue(object : Callback<RetrofitClient.LoginResponse> {
                override fun onResponse(call: Call<RetrofitClient.LoginResponse>, response: Response<RetrofitClient.LoginResponse>) {
                    if (response.isSuccessful && response.body()?.status == "SUCCESS") {
                        val userProfile = response.body()
                        val token = userProfile?.token ?: ""
                        Log.d("LoginActivity", "Received token: $token")
                        
                        // Сохраняем ID пользователя
                        val userId = userProfile?.id ?: -1L
                        if (userId != -1L) {
                            RetrofitClient.saveUserId(userId)
                        }
                        
                        RetrofitClient.setToken(token)
                        
                        val guest = Guest(
                            id = userId,
                            fio = userProfile?.fio ?: "",
                            contacts = userProfile?.contacts ?: "",
                            dateOfRegistration = userProfile?.dateOfRegistration ?: "",
                            login = login,
                            hasSubscription = userProfile?.hasSubscription ?: false,
                            role = userProfile?.role ?: "USER",
                            token = token
                        )
                        Log.d("LoginActivity", "Created guest object: $guest")
                        UserManager.saveUserToPreferences(this@LoginActivity, guest)
                        Log.d("LoginActivity", "Saved user data to preferences")
                        startMainActivity()
                    } else {
                        val errorMessage = response.errorBody()?.string() ?: response.message()
                        Log.e("LoginActivity", "Login failed: $errorMessage")
                        Toast.makeText(this@LoginActivity, "Ошибка авторизации: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<RetrofitClient.LoginResponse>, t: Throwable) {
                    Log.e("LoginActivity", "Network error", t)
                    Toast.makeText(this@LoginActivity, "Ошибка сети: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            Log.e("LoginActivity", "Authentication error", e)
            Toast.makeText(this, "Ошибка авторизации: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

