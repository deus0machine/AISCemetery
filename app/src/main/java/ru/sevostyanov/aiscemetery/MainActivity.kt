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
import androidx.core.view.WindowCompat
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDestination
import com.google.android.material.badge.BadgeDrawable
import ru.sevostyanov.aiscemetery.viewmodels.NotificationsViewModel
import android.view.MenuItem
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var isInitialized = false
    private lateinit var navController: NavController
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var notificationsViewModel: NotificationsViewModel
    private var notificationBadge: BadgeDrawable? = null
    
    companion object {
        const val TAG = "MainActivity"
    }

    private fun checkAuthAndRedirect() {
        val user = UserManager.getCurrentUser() ?: UserManager.loadUserFromPreferences(this)
        if (user == null) {
            Log.d(TAG, "User not authenticated, returning to LoginActivity")
            UserManager.clearUserData(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Явно показываем системный статус-бар и устанавливаем нужные флаги
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_700)
        
        if (isInitialized) {
            Log.d(TAG, "Activity already initialized, skipping")
            return
        }

        try {
            // Инициализируем RetrofitClient
            RetrofitClient.initialize(applicationContext)
            
            // Проверяем авторизацию
            checkAuthAndRedirect()

            setContentView(R.layout.activity_main)
            isInitialized = true

            // Инициализируем ViewModel
            notificationsViewModel = ViewModelProvider(this).get(NotificationsViewModel::class.java)
            
            // Устанавливаем токен
            RetrofitClient.setToken(UserManager.getCurrentUser()?.token ?: "")

            // Настраиваем навигацию
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController

            // Настраиваем нижнюю навигацию
            bottomNavigationView = findViewById(R.id.bottom_navigation)
            bottomNavigationView.setupWithNavController(navController)
            
            // Настраиваем боковое меню
            val navigationView = findViewById<NavigationView>(R.id.nav_view)
            navigationView.setNavigationItemSelectedListener(this)
            
            // Инициализируем бейдж для уведомлений
            setupNotificationBadge()
            
            // Следим за изменениями списка уведомлений
            observeNotifications()
            
            // Добавляем слушатель для очистки бейджа при переходе в раздел уведомлений
            navController.addOnDestinationChangedListener { _, destination, _ ->
                if (destination.id == R.id.notificationsFragment) {
                    notificationBadge?.isVisible = false
                }
            }
            
            // Проверяем, нужно ли перейти к уведомлениям
            handleNavigationArgs(intent)
            
            // Загружаем уведомления для отображения индикатора
            loadNotifications()

        } catch (e: Exception) {
            Log.e(TAG, "Initialization error", e)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    
    private fun setupNotificationBadge() {
        // Создаем бейдж для элемента нижней навигации уведомлений
        notificationBadge = bottomNavigationView.getOrCreateBadge(R.id.notificationsFragment)
        notificationBadge?.apply {
            backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.error)
            badgeTextColor = ContextCompat.getColor(this@MainActivity, android.R.color.white)
            // По умолчанию скрываем бейдж
            isVisible = false
        }
    }
    
    private fun observeNotifications() {
        // Наблюдаем за списком входящих уведомлений
        notificationsViewModel.incomingNotifications.observe(this) { notifications ->
            // Считаем количество непрочитанных уведомлений
            val unreadCount = notifications.count { !it.isRead }
            if (unreadCount > 0) {
                // Если есть непрочитанные, показываем бейдж с их количеством
                notificationBadge?.apply {
                    number = unreadCount
                    isVisible = true
                }
            } else {
                // Если нет непрочитанных, скрываем бейдж
                notificationBadge?.isVisible = false
            }
            
            Log.d(TAG, "Непрочитанных уведомлений: $unreadCount")
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNavigationArgs(intent)
    }
    
    private fun handleNavigationArgs(intent: Intent) {
        val navigateTo = intent.getStringExtra("navigate_to")
        if (navigateTo == "notifications") {
            // Переходим к фрагменту уведомлений
            navController.navigate(R.id.notificationsFragment)
            
            // Проверяем, нужно ли открыть конкретную вкладку
            val tabPosition = intent.getIntExtra("tab_position", -1)
            if (tabPosition != -1) {
                // Сохраняем позицию вкладки для использования в NotificationsFragment
                intent.putExtra("tab_position", tabPosition)
            }
        }
    }
    
    private fun loadNotifications() {
        // Загружаем уведомления для обновления индикатора
        notificationsViewModel.loadIncomingNotifications()
    }

    override fun onResume() {
        super.onResume()
        // Проверяем авторизацию только если активити уже инициализирована
        if (isInitialized) {
            checkAuthAndRedirect()
            // Обновляем уведомления при возвращении к активности
            loadNotifications()
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    
    // Метод для открытия бокового меню
    fun openDrawer() {
        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }
    
    // Переопределяем метод для добавления кнопки меню в ActionBar
    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    // Обработка нажатий на элементы ActionBar меню
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_menu -> {
                openDrawer()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_notifications -> {
                navController.navigate(R.id.notificationsFragment)
            }
            R.id.nav_pending_changes -> {
                // Открываем активность ожидающих изменений
                ru.sevostyanov.aiscemetery.activities.PendingChangesActivity.start(this)
            }
            R.id.nav_account -> {
                navController.navigate(R.id.profileFragment)
            }
        }
        
        // Закрываем боковое меню после выбора пункта
        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}


