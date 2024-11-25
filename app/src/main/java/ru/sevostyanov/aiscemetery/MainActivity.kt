package ru.sevostyanov.aiscemetery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // BottomNavigationView setup
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        // Устанавливаем начальный фрагмент
        supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, OrderFragment()).commit()

        // Обработка навигации
        navView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_order -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, OrderFragment())
                        .commit()
                    true
                }
                R.id.navigation_profile -> {
                    val profileFragment = ProfileFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, profileFragment)
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}


