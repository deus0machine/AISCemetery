package ru.sevostyanov.aiscemetery

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginButton = findViewById<Button>(R.id.login_button)
        val registerLink = findViewById<TextView>(R.id.register_link)
        val loginField = findViewById<EditText>(R.id.login_email)
        val passwordField = findViewById<EditText>(R.id.login_password)

        loginButton.setOnClickListener {
            val login = loginField.text.toString()
            val password = passwordField.text.toString()

            // Простая проверка заглушки: логин и пароль "1111"
            if (login == "1111" && password == "1111") {
                // Переход в MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // Закрываем LoginActivity
            } else {
                // Ошибка при неверном логине или пароле
                Toast.makeText(this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
            }
        }

        // Переход на экран регистрации
        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}