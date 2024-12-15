package ru.sevostyanov.aiscemetery

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

class RegisterActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val backToAuthorize = findViewById<TextView>(R.id.back_to_autorize_link)
        val registerButton: Button = findViewById(R.id.register_button)
        val fioField = findViewById<EditText>(R.id.fio_input)
        val loginField = findViewById<EditText>(R.id.login_input)
        val passwordField = findViewById<EditText>(R.id.password_input)
        val contactsField = findViewById<EditText>(R.id.contacts_input)

        val apiService = RetrofitClient.getApiService()

        registerButton.setOnClickListener {
            val fio = fioField.text.toString()
            val login = loginField.text.toString()
            val password = passwordField.text.toString()
            val contacts = contactsField.text.toString()

            if (fio.isEmpty() || login.isEmpty() || password.isEmpty() || contacts.isEmpty()) {
                Toast.makeText(this, "Все поля обязательны для заполнения", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val registerRequest = RegisterRequest(fio, login, password, contacts)

            apiService.registerUser(registerRequest).enqueue(object : Callback<RegisterResponse> {
                override fun onResponse(
                    call: Call<RegisterResponse>,
                    response: Response<RegisterResponse>
                ) {
                    Log.d("RegisterActivity", "Response: ${response.body()}")  // Логирование ответа
                    if (response.isSuccessful && response.body()?.status == "SUCCESS") {
                        Toast.makeText(this@RegisterActivity, "Регистрация успешна", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@RegisterActivity,
                            response.body()?.message ?: "Ошибка регистрации",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    Toast.makeText(this@RegisterActivity, "Ошибка подключения: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        backToAuthorize.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
    data class RegisterRequest(
        val fio: String,
        val login: String,
        val password: String,
        val contacts: String
    )

    data class RegisterResponse(
        val status: String,
        val message: String
    )

}