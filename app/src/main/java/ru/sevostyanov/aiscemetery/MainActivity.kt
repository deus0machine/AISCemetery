package ru.sevostyanov.aiscemetery

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.sevostyanov.aiscemetery.LoginActivity.Companion.KEY_USER_ROLE
import ru.sevostyanov.aiscemetery.fragments.AdminFragment
import ru.sevostyanov.aiscemetery.fragments.ProfileFragment
import ru.sevostyanov.aiscemetery.fragments.BurialsFragment
import ru.sevostyanov.aiscemetery.task.TaskChoiceFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        RetrofitClient.initialize(this)
        val sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val role = sharedPreferences.getString(KEY_USER_ROLE, null)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        // Скрываем Admin-фрагмент, если пользователь не Admin
        if (role != "ADMIN") {
            val menu = navView.menu
            menu.removeItem(R.id.navigation_admin)
        }

        // Устанавливаем начальный фрагмент
        supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, BurialsFragment()).commit()

        // Обработка навигации
        navView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_order -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, TaskChoiceFragment())
                        .commit()
                    true
                }
                R.id.navigation_profile -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, ProfileFragment())
                        .commit()
                    true
                }
                R.id.navigation_graves -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, BurialsFragment())
                        .commit()
                    true
                }
                R.id.navigation_admin -> { // Добавьте обработку для Admin
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, AdminFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}


