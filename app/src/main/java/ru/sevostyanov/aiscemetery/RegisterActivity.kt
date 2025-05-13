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
import ru.sevostyanov.aiscemetery.RetrofitClient.RegisterRequest
import ru.sevostyanov.aiscemetery.RetrofitClient.RegisterResponse

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val registerButton = findViewById<Button>(R.id.register_button)
        val backToAuthorize = findViewById<TextView>(R.id.back_to_autorize_link)
        val loginField = findViewById<EditText>(R.id.login_input)
        val passwordField = findViewById<EditText>(R.id.password_input)
        val fioField = findViewById<EditText>(R.id.fio_input)
        val contactsField = findViewById<EditText>(R.id.contacts_input)

        registerButton.setOnClickListener {
            val login = loginField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val fio = fioField.text.toString().trim()
            val contacts = contactsField.text.toString().trim()

            if (login.isEmpty() || password.isEmpty() || fio.isEmpty() || contacts.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            } else {
                registerUser(login, password, fio, contacts)
            }
        }

        backToAuthorize.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser(login: String, password: String, fio: String, contacts: String) {
        val registerRequest = RegisterRequest(login, password, fio, contacts)
        val apiService = RetrofitClient.getApiService()

        apiService.registerUser(registerRequest).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful && response.body()?.status == "SUCCESS") {
                    Toast.makeText(this@RegisterActivity, "Регистрация успешна", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                } else {
                    val errorMessage = response.errorBody()?.string() ?: response.message()
                    Toast.makeText(this@RegisterActivity, "Ошибка регистрации: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Toast.makeText(this@RegisterActivity, "Ошибка сети: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}