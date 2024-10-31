package ru.sevostyanov.aiscemetery

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val loginButton: Button = findViewById(R.id.register_button)
        loginButton.setOnClickListener {
            // Логика регистрации через API (например, с использованием Retrofit)
        }
    }
}