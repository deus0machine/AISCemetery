package ru.sevostyanov.aiscemetery.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast

/**
 * Утилиты для работы с сетью
 */
object NetworkUtil {
    /**
     * Проверяет доступность интернет-соединения
     */
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * Проверяет интернет-соединение и показывает сообщение, если его нет
     * @return true если есть соединение, false в противном случае
     */
    fun checkInternetAndShowMessage(context: Context, showMessage: Boolean = true): Boolean {
        val isConnected = isInternetAvailable(context)
        
        if (!isConnected && showMessage) {
            Toast.makeText(
                context,
                "Нет подключения к интернету. Проверьте настройки сети.",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        return isConnected
    }
} 