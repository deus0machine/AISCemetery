package ru.sevostyanov.aiscemetery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.sevostyanov.aiscemetery.RetrofitClient.RegisterRequest
import ru.sevostyanov.aiscemetery.RetrofitClient.RegisterResponse
import ru.sevostyanov.aiscemetery.util.ValidationUtils

class RegisterActivity : AppCompatActivity() {

    private lateinit var registerButton: MaterialButton
    private lateinit var privacyConsentCheckbox: MaterialCheckBox
    
    // Поля ввода
    private lateinit var loginField: TextInputEditText
    private lateinit var passwordField: TextInputEditText
    private lateinit var fioField: TextInputEditText
    private lateinit var contactsField: TextInputEditText
    
    // Layout контейнеры для отображения ошибок
    private lateinit var loginLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var fioLayout: TextInputLayout
    private lateinit var contactsLayout: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupListeners()
        checkFormValidity()
    }

    private fun initViews() {
        registerButton = findViewById(R.id.register_button)
        privacyConsentCheckbox = findViewById(R.id.privacy_consent_checkbox)
        val backToAuthorize = findViewById<TextView>(R.id.back_to_autorize_link)
        val userAgreementLink = findViewById<TextView>(R.id.user_agreement_link)
        
        // Поля ввода
        loginField = findViewById(R.id.login_input)
        passwordField = findViewById(R.id.password_input)
        fioField = findViewById(R.id.fio_input)
        contactsField = findViewById(R.id.contacts_input)
        
        // Layout контейнеры
        loginLayout = findViewById(R.id.login_input_layout)
        passwordLayout = findViewById(R.id.password_input_layout)
        fioLayout = findViewById(R.id.fio_input_layout)
        contactsLayout = findViewById(R.id.contacts_input_layout)

        // Изначально кнопка регистрации отключена
        registerButton.isEnabled = false

        backToAuthorize.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Обработчик клика по ссылке на пользовательское соглашение
        userAgreementLink.setOnClickListener {
            val intent = Intent(this, UserAgreementActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupListeners() {
        // Создаем отдельные TextWatcher для каждого поля
        loginField.addTextChangedListener(createFieldWatcher { validateLogin() })
        passwordField.addTextChangedListener(createFieldWatcher { validatePassword() })
        fioField.addTextChangedListener(createFieldWatcher { validateFio() })
        contactsField.addTextChangedListener(createFieldWatcher { validateContacts() })

        privacyConsentCheckbox.setOnCheckedChangeListener { _, _ ->
            checkFormValidity()
        }

        // Обработка клика по тексту согласия
        findViewById<TextView>(R.id.privacy_consent_text).setOnClickListener {
            privacyConsentCheckbox.isChecked = !privacyConsentCheckbox.isChecked
        }

        registerButton.setOnClickListener {
            if (validateAllFields() && privacyConsentCheckbox.isChecked) {
                val login = loginField.text.toString().trim()
                val password = passwordField.text.toString().trim()
                val fio = fioField.text.toString().trim()
                val contacts = contactsField.text.toString().trim()
                registerUser(login, password, fio, contacts)
            } else {
                showValidationErrors()
            }
        }
    }

    private fun createFieldWatcher(validationAction: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validationAction()
                checkFormValidity()
            }
        }
    }

    private fun validateLogin(): Boolean {
        val login = loginField.text.toString()
        val error = ValidationUtils.validateLogin(login)
        
        loginLayout.error = error
        return error == null
    }

    private fun validatePassword(): Boolean {
        val password = passwordField.text.toString()
        val error = ValidationUtils.validatePassword(password)
        
        passwordLayout.error = error
        return error == null
    }

    private fun validateFio(): Boolean {
        val fio = fioField.text.toString()
        val error = ValidationUtils.validateFio(fio)
        
        fioLayout.error = error
        return error == null
    }

    private fun validateContacts(): Boolean {
        val contacts = contactsField.text.toString()
        val error = ValidationUtils.validateContacts(contacts)
        
        contactsLayout.error = error
        return error == null
    }

    private fun validateAllFields(): Boolean {
        val isLoginValid = validateLogin()
        val isPasswordValid = validatePassword()
        val isFioValid = validateFio()
        val isContactsValid = validateContacts()
        
        return isLoginValid && isPasswordValid && isFioValid && isContactsValid
    }

    private fun checkFormValidity() {
        val isFormValid = validateAllFields() && privacyConsentCheckbox.isChecked
        registerButton.isEnabled = isFormValid
        
        // Меняем прозрачность кнопки в зависимости от состояния
        registerButton.alpha = if (isFormValid) 1.0f else 0.5f
    }

    private fun areAllFieldsValid(): Boolean {
        return validateAllFields()
    }

    private fun showValidationErrors() {
        // Принудительно валидируем все поля для отображения ошибок
        validateLogin()
        validatePassword()
        validateFio()
        validateContacts()
        
        when {
            !privacyConsentCheckbox.isChecked -> {
                Toast.makeText(this, "Необходимо согласиться с обработкой персональных данных", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(this, "Исправьте ошибки в полях выше", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showValidationError() {
        showValidationErrors()
    }

    private fun registerUser(login: String, password: String, fio: String, contacts: String) {
        Log.d("RegisterActivity", "Attempting to register user: $login")
        
        // Отключаем кнопку на время регистрации
        registerButton.isEnabled = false
        registerButton.text = "Регистрация..."
        
        val registerRequest = RegisterRequest(login, password, fio, contacts)
        val apiService = RetrofitClient.getApiService()

        Log.d("RegisterActivity", "Sending registration request: $registerRequest")

        apiService.registerUser(registerRequest).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                Log.d("RegisterActivity", "Registration response: ${response.code()}, ${response.message()}")
                
                // Восстанавливаем кнопку
                registerButton.text = "Зарегистрироваться"
                checkFormValidity()
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d("RegisterActivity", "Registration response body: $responseBody")
                    
                    if (responseBody?.status == "SUCCESS") {
                        Toast.makeText(this@RegisterActivity, "Регистрация успешна! Добро пожаловать!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        val errorMessage = responseBody?.message ?: "Неизвестная ошибка"
                        Toast.makeText(this@RegisterActivity, "Ошибка регистрации: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorMessage = try {
                        response.errorBody()?.string() ?: response.message()
                    } catch (e: Exception) {
                        "Ошибка сети: ${response.code()}"
                    }
                    Log.e("RegisterActivity", "Registration failed: $errorMessage")
                    Toast.makeText(this@RegisterActivity, "Ошибка регистрации: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Log.e("RegisterActivity", "Registration network error", t)
                
                // Восстанавливаем кнопку
                registerButton.text = "Зарегистрироваться"
                checkFormValidity()
                
                Toast.makeText(this@RegisterActivity, "Ошибка сети: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}