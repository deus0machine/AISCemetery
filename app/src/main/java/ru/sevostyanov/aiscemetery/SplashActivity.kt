package ru.sevostyanov.aiscemetery

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    
    private val SPLASH_DELAY = 3000L // 3 секунды для показа анимации
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Запускаем анимации
        startAnimations()
        
        // Задержка перед переходом к основному экрану
        Handler(Looper.getMainLooper()).postDelayed({
            // Переходим к LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            
            // Добавляем плавный переход
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, SPLASH_DELAY)
    }
    
    private fun startAnimations() {
        val logo = findViewById<ImageView>(R.id.splash_logo)
        val mainText = findViewById<TextView>(R.id.main_text)
        val thankYouText = findViewById<TextView>(R.id.thank_you_text)
        val loadingProgress = findViewById<ProgressBar>(R.id.loading_progress)
        val versionText = findViewById<TextView>(R.id.version_text)
        
        // Анимация логотипа - появление с масштабированием
        val logoFadeIn = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f)
        val logoScaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.5f, 1f)
        val logoScaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.5f, 1f)
        
        val logoAnimatorSet = AnimatorSet()
        logoAnimatorSet.playTogether(logoFadeIn, logoScaleX, logoScaleY)
        logoAnimatorSet.duration = 800
        logoAnimatorSet.interpolator = AccelerateDecelerateInterpolator()
        
        // Анимация основного текста
        val mainTextFadeIn = ObjectAnimator.ofFloat(mainText, "alpha", 0f, 1f)
        val mainTextSlideUp = ObjectAnimator.ofFloat(mainText, "translationY", 30f, 0f)
        
        val mainTextAnimatorSet = AnimatorSet()
        mainTextAnimatorSet.playTogether(mainTextFadeIn, mainTextSlideUp)
        mainTextAnimatorSet.duration = 600
        mainTextAnimatorSet.interpolator = AccelerateDecelerateInterpolator()
        
        // Анимация текста благодарности
        val thankYouFadeIn = ObjectAnimator.ofFloat(thankYouText, "alpha", 0f, 1f)
        val thankYouSlideUp = ObjectAnimator.ofFloat(thankYouText, "translationY", 20f, 0f)
        
        val thankYouAnimatorSet = AnimatorSet()
        thankYouAnimatorSet.playTogether(thankYouFadeIn, thankYouSlideUp)
        thankYouAnimatorSet.duration = 500
        thankYouAnimatorSet.interpolator = AccelerateDecelerateInterpolator()
        
        // Анимация индикатора загрузки
        val loadingFadeIn = ObjectAnimator.ofFloat(loadingProgress, "alpha", 0f, 1f)
        loadingFadeIn.duration = 400
        
        // Анимация версии
        val versionFadeIn = ObjectAnimator.ofFloat(versionText, "alpha", 0f, 0.7f)
        versionFadeIn.duration = 400
        
        // Последовательный запуск анимаций
        val mainAnimatorSet = AnimatorSet()
        mainAnimatorSet.play(logoAnimatorSet)
            .before(mainTextAnimatorSet)
        mainAnimatorSet.play(mainTextAnimatorSet)
            .before(thankYouAnimatorSet)
        mainAnimatorSet.play(thankYouAnimatorSet)
            .before(loadingFadeIn)
        mainAnimatorSet.play(loadingFadeIn)
            .with(versionFadeIn)
        
        // Запускаем анимации с небольшой задержкой
        Handler(Looper.getMainLooper()).postDelayed({
            mainAnimatorSet.start()
        }, 200)
    }
    
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Отключаем кнопку "Назад" на splash screen
        // super.onBackPressed()
    }
} 