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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.sevostyanov.aiscemetery.user.Guest
import ru.sevostyanov.aiscemetery.user.UserManager
import ru.sevostyanov.aiscemetery.util.ValidationUtils

class LoginActivity : AppCompatActivity() {
    private var isInitialized = false

    // UI элементы
    private lateinit var loginInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var loginField: TextInputEditText
    private lateinit var passwordField: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var loginProgress: android.widget.ProgressBar
    private lateinit var errorMessageText: TextView

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

            initializeViews()
            setupListeners()
            
        } catch (e: Exception) {
            Log.e("LoginActivity", "Initialization error", e)
            Toast.makeText(this, "Ошибка инициализации: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews() {
        loginButton = findViewById(R.id.login_button)
        loginInputLayout = findViewById(R.id.login_input_layout)
        passwordInputLayout = findViewById(R.id.password_input_layout)
        loginField = findViewById(R.id.login_email)
        passwordField = findViewById(R.id.login_password)
        loginProgress = findViewById(R.id.login_progress)
        errorMessageText = findViewById(R.id.error_message)
    }

    private fun setupListeners() {
        val registerLink = findViewById<TextView>(R.id.register_link)

        // Авторизация
        loginButton.setOnClickListener {
            attemptLogin()
        }

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Очистка ошибок при вводе текста
        loginField.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                clearFieldError(loginInputLayout)
            }
        })
        
        passwordField.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                clearFieldError(passwordInputLayout)
            }
        })
    }

    private fun attemptLogin() {
        // Очищаем предыдущие ошибки
        clearAllErrors()
        
        val login = loginField.text.toString().trim()
        val password = passwordField.text.toString().trim()

        // Валидация полей
        var hasErrors = false

        // Валидация логина
        val loginError = ValidationUtils.validateLogin(login)
        if (loginError != null) {
            showFieldError(loginInputLayout, loginError)
            hasErrors = true
        }

        // Валидация пароля
        val passwordError = ValidationUtils.validatePassword(password)
        if (passwordError != null) {
            showFieldError(passwordInputLayout, passwordError)
            hasErrors = true
        }

        if (hasErrors) {
            return
        }

        // Показываем состояние загрузки
        setLoadingState(true)
        authenticateUser(login, password)
    }

    private fun showFieldError(inputLayout: TextInputLayout, message: String) {
        inputLayout.error = message
        inputLayout.isErrorEnabled = true
    }

    private fun clearFieldError(inputLayout: TextInputLayout) {
        inputLayout.error = null
        inputLayout.isErrorEnabled = false
    }

    private fun clearAllErrors() {
        clearFieldError(loginInputLayout)
        clearFieldError(passwordInputLayout)
        hideErrorMessage()
    }

    private fun showErrorMessage(message: String) {
        errorMessageText.text = message
        errorMessageText.visibility = android.view.View.VISIBLE
    }

    private fun hideErrorMessage() {
        errorMessageText.visibility = android.view.View.GONE
    }

    private fun setLoadingState(loading: Boolean) {
        if (loading) {
            loginButton.text = "Вход..."
            loginButton.isEnabled = false
            loginProgress.visibility = android.view.View.VISIBLE
        } else {
            loginButton.text = "Войти"
            loginButton.isEnabled = true
            loginProgress.visibility = android.view.View.GONE
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
                    setLoadingState(false)
                    
                    if (response.isSuccessful && response.body()?.status == "SUCCESS") {
                        handleSuccessfulLogin(response.body()!!, login)
                    } else {
                        handleLoginError(response)
                    }
                }

                override fun onFailure(call: Call<RetrofitClient.LoginResponse>, t: Throwable) {
                    setLoadingState(false)
                    handleNetworkError(t)
                }
            })
        } catch (e: Exception) {
            setLoadingState(false)
            Log.e("LoginActivity", "Authentication error", e)
            Toast.makeText(this, "Ошибка авторизации: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleSuccessfulLogin(userProfile: RetrofitClient.LoginResponse, login: String) {
        val token = userProfile.token ?: ""
        Log.d("LoginActivity", "Received token: $token")
        
        // Сохраняем ID пользователя
        val userId = userProfile.id ?: -1L
        if (userId != -1L) {
            RetrofitClient.saveUserId(userId)
        }
        
        RetrofitClient.setToken(token)
        
        val guest = Guest(
            id = userId,
            fio = userProfile.fio ?: "",
            contacts = userProfile.contacts ?: "",
            dateOfRegistration = userProfile.dateOfRegistration ?: "",
            login = login,
            hasSubscription = userProfile.hasSubscription ?: false,
            role = userProfile.role ?: "USER",
            token = token
        )
        Log.d("LoginActivity", "Created guest object: $guest")
        UserManager.saveUserToPreferences(this, guest)
        Log.d("LoginActivity", "Saved user data to preferences")
        
        Toast.makeText(this, "Добро пожаловать, ${userProfile.fio}!", Toast.LENGTH_SHORT).show()
        startMainActivity()
    }

    private fun handleLoginError(response: Response<RetrofitClient.LoginResponse>) {
        Log.e("LoginActivity", "Login failed: ${response.code()} - ${response.message()}")
        
        when (response.code()) {
            400 -> {
                // Неверные учетные данные
                showFieldError(passwordInputLayout, "Неверный логин или пароль")
                showErrorMessage("Проверьте правильность введенных данных")
            }
            401 -> {
                // Неавторизован - неверные учетные данные
                showFieldError(passwordInputLayout, "Неверный логин или пароль")
                showErrorMessage("Неверный логин или пароль")
            }
            403 -> {
                // Доступ запрещен - аккаунт заблокирован
                showErrorMessage("Ваш аккаунт заблокирован. Обратитесь к администратору")
            }
            404 -> {
                // Пользователь не найден
                showFieldError(loginInputLayout, "Пользователь не найден")
                showErrorMessage("Пользователь с таким логином не найден")
            }
            429 -> {
                // Слишком много попыток входа
                showErrorMessage("Слишком много попыток входа. Попробуйте позже")
            }
            500 -> {
                // Ошибка сервера
                showErrorMessage("Ошибка сервера. Попробуйте позже")
            }
            else -> {
                // Другие ошибки
                val errorMessage = try {
                    response.errorBody()?.string() ?: "Неизвестная ошибка"
                } catch (e: Exception) {
                    "Ошибка авторизации"
                }
                showErrorMessage("Ошибка: $errorMessage")
            }
        }
    }

    private fun handleNetworkError(throwable: Throwable) {
        Log.e("LoginActivity", "Network error", throwable)
        
        val errorMessage = when (throwable) {
            is java.net.UnknownHostException -> {
                "Нет подключения к интернету. Проверьте сетевое соединение"
            }
            is java.net.SocketTimeoutException -> {
                "Превышено время ожидания. Проверьте подключение и попробуйте снова"
            }
            is javax.net.ssl.SSLHandshakeException -> {
                "Ошибка безопасного соединения. Проверьте настройки сети"
            }
            is java.net.ConnectException -> {
                "Не удается подключиться к серверу. Попробуйте позже"
            }
            else -> {
                "Ошибка сети: ${throwable.localizedMessage ?: "Неизвестная ошибка"}"
            }
        }
        
        showErrorMessage(errorMessage)
    }
}

