package ru.sevostyanov.aiscemetery

import android.app.Application
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CemeteryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapKitFactory.setApiKey("0ccbd4ff-b32f-410d-87d9-a7b1528f773e") // Замените на ваш API ключ
        MapKitFactory.initialize(this)
    }
} 