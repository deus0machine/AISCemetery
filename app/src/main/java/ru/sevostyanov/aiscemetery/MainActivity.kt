package ru.sevostyanov.aiscemetery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import ru.sevostyanov.aiscemetery.user.UserManager

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var isInitialized = false
    private lateinit var navController: NavController
    private lateinit var bottomNavigationView: BottomNavigationView

    private fun checkAuthAndRedirect() {
        val user = UserManager.getCurrentUser() ?: UserManager.loadUserFromPreferences(this)
        if (user == null) {
            Log.d("MainActivity", "User not authenticated, returning to LoginActivity")
            UserManager.clearUserData(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (isInitialized) {
            Log.d("MainActivity", "Activity already initialized, skipping")
            return
        }

        try {
            // Инициализируем RetrofitClient
            RetrofitClient.initialize(applicationContext)
            
            // Проверяем авторизацию
            checkAuthAndRedirect()

            setContentView(R.layout.activity_main)
            isInitialized = true

            // Устанавливаем токен
            RetrofitClient.setToken(UserManager.getCurrentUser()?.token ?: "")

            // Настраиваем навигацию
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController

            // Настраиваем нижнюю навигацию
            bottomNavigationView = findViewById(R.id.bottom_navigation)
            bottomNavigationView.setupWithNavController(navController)

        } catch (e: Exception) {
            Log.e("MainActivity", "Initialization error", e)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Проверяем авторизацию только если активити уже инициализирована
        if (isInitialized) {
            checkAuthAndRedirect()
        }
    }
}


