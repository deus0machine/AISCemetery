package ru.sevostyanov.aiscemetery

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.sevostyanov.aiscemetery.fragments.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        RetrofitClient.initialize(this)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        // Устанавливаем начальный фрагмент
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, MemorialsFragment())
            .commit()

        // Обработка навигации
        navView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_memorials -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, MemorialsFragment())
                        .commit()
                    true
                }
                R.id.navigation_trees -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, FamilyTreeFragment())
                        .commit()
                    true
                }
                R.id.navigation_map -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, MapFragment())
                        .commit()
                    true
                }
                R.id.navigation_notifications -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, NotificationsFragment())
                        .commit()
                    true
                }
                R.id.navigation_profile -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, ProfileFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}


