package ru.sevostyanov.aiscemetery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import ru.sevostyanov.aiscemetery.fragments.FamilyTreesListFragment
import ru.sevostyanov.aiscemetery.fragments.MapFragment
import ru.sevostyanov.aiscemetery.fragments.MemorialsFragment
import ru.sevostyanov.aiscemetery.fragments.NotificationsFragment
import ru.sevostyanov.aiscemetery.fragments.ProfileFragment
import ru.sevostyanov.aiscemetery.user.UserManager

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var isInitialized = false
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (isInitialized) {
            Log.d("MainActivity", "Activity already initialized, skipping")
            return
        }

        try {
            // Проверяем авторизацию
            val user = UserManager.getCurrentUser()
            if (user == null) {
                Log.d("MainActivity", "No user found, returning to LoginActivity")
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }

            setContentView(R.layout.activity_main)
            isInitialized = true

            // Инициализируем RetrofitClient и устанавливаем токен
            RetrofitClient.initialize(applicationContext)
            RetrofitClient.setToken(user.token)

            bottomNavigationView = findViewById(R.id.bottom_navigation)
            bottomNavigationView.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_memorials -> {
                        loadFragment(MemorialsFragment())
                        true
                    }
                    R.id.navigation_family_trees -> {
                        val fragment = FamilyTreesListFragment().apply {
                            arguments = Bundle().apply {
                                putString("type", "my")
                            }
                        }
                        loadFragment(fragment)
                        true
                    }
                    R.id.navigation_map -> {
                        loadFragment(MapFragment())
                        true
                    }
                    R.id.navigation_notifications -> {
                        loadFragment(NotificationsFragment())
                        true
                    }
                    R.id.navigation_profile -> {
                        loadFragment(ProfileFragment())
                        true
                    }
                    else -> false
                }
            }

            // Устанавливаем начальный фрагмент
            if (savedInstanceState == null) {
                bottomNavigationView.selectedItemId = R.id.navigation_memorials
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Initialization error", e)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        // Проверяем авторизацию только если активити уже инициализирована
        if (isInitialized) {
            val user = UserManager.getCurrentUser()
            if (user == null) {
                Log.d("MainActivity", "No user found during onResume, returning to LoginActivity")
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }
}


