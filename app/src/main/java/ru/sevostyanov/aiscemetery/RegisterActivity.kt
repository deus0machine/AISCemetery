package ru.sevostyanov.aiscemetery

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        val backToAutorize = findViewById<TextView>(R.id.back_to_autorize_link)
        val loginButton: Button = findViewById(R.id.register_button)
        loginButton.setOnClickListener {
            // Логика регистрации через API (например, с использованием Retrofit)
        }
        backToAutorize.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}