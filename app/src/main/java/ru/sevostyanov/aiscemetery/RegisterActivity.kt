package ru.sevostyanov.aiscemetery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.sevostyanov.aiscemetery.RetrofitClient.RegisterRequest
import ru.sevostyanov.aiscemetery.RetrofitClient.RegisterResponse

class RegisterActivity : AppCompatActivity() {

    private lateinit var registerButton: MaterialButton
    private lateinit var privacyConsentCheckbox: MaterialCheckBox
    private lateinit var loginField: TextInputEditText
    private lateinit var passwordField: TextInputEditText
    private lateinit var fioField: TextInputEditText
    private lateinit var contactsField: TextInputEditText

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
        loginField = findViewById(R.id.login_input)
        passwordField = findViewById(R.id.password_input)
        fioField = findViewById(R.id.fio_input)
        contactsField = findViewById(R.id.contacts_input)
        
        // Изначально кнопка регистрации отключена
        registerButton.isEnabled = false
        
        backToAuthorize.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkFormValidity()
            }
        }

        loginField.addTextChangedListener(textWatcher)
        passwordField.addTextChangedListener(textWatcher)
        fioField.addTextChangedListener(textWatcher)
        contactsField.addTextChangedListener(textWatcher)

        privacyConsentCheckbox.setOnCheckedChangeListener { _, _ ->
            checkFormValidity()
        }

        // Обработка клика по тексту согласия
        findViewById<TextView>(R.id.privacy_consent_text).setOnClickListener {
            privacyConsentCheckbox.isChecked = !privacyConsentCheckbox.isChecked
        }

        registerButton.setOnClickListener {
            if (areAllFieldsValid() && privacyConsentCheckbox.isChecked) {
                val login = loginField.text.toString().trim()
                val password = passwordField.text.toString().trim()
                val fio = fioField.text.toString().trim()
                val contacts = contactsField.text.toString().trim()
                registerUser(login, password, fio, contacts)
            } else {
                showValidationError()
            }
        }
    }

    private fun checkFormValidity() {
        val isFormValid = areAllFieldsValid() && privacyConsentCheckbox.isChecked
        registerButton.isEnabled = isFormValid
        
        // Меняем прозрачность кнопки в зависимости от состояния
        registerButton.alpha = if (isFormValid) 1.0f else 0.5f
    }

    private fun areAllFieldsValid(): Boolean {
        val login = loginField.text.toString().trim()
        val password = passwordField.text.toString().trim()
        val fio = fioField.text.toString().trim()
        val contacts = contactsField.text.toString().trim()

        return login.isNotEmpty() && 
               password.isNotEmpty() && 
               fio.isNotEmpty() && 
               contacts.isNotEmpty() &&
               password.length >= 6 && // Минимальная длина пароля
               fio.split(" ").size >= 2 // Проверка, что введены минимум имя и фамилия
    }

    private fun showValidationError() {
        when {
            fioField.text.toString().trim().split(" ").size < 2 -> {
                Toast.makeText(this, "Пожалуйста, введите полное ФИО (минимум имя и фамилия)", Toast.LENGTH_SHORT).show()
            }
            passwordField.text.toString().trim().length < 6 -> {
                Toast.makeText(this, "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show()
            }
            !privacyConsentCheckbox.isChecked -> {
                Toast.makeText(this, "Необходимо согласиться с обработкой персональных данных", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(login: String, password: String, fio: String, contacts: String) {
        // Отключаем кнопку на время регистрации
        registerButton.isEnabled = false
        registerButton.text = "Регистрация..."
        
        val registerRequest = RegisterRequest(login, password, fio, contacts)
        val apiService = RetrofitClient.getApiService()

        apiService.registerUser(registerRequest).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                // Восстанавливаем кнопку
                registerButton.text = "Зарегистрироваться"
                checkFormValidity()
                
                if (response.isSuccessful && response.body()?.status == "SUCCESS") {
                    Toast.makeText(this@RegisterActivity, "Регистрация успешна! Добро пожаловать!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                } else {
                    val errorMessage = response.errorBody()?.string() ?: response.message()
                    Toast.makeText(this@RegisterActivity, "Ошибка регистрации: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                // Восстанавливаем кнопку
                registerButton.text = "Зарегистрироваться"
                checkFormValidity()
                
                Toast.makeText(this@RegisterActivity, "Ошибка сети: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}