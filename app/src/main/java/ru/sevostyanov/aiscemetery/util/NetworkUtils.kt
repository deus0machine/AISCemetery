package ru.sevostyanov.aiscemetery.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object NetworkUtils {
    
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
     * Показывает сообщение об отсутствии интернета
     */
    fun showNoInternetToast(context: Context) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "Нет подключения к интернету. Проверьте настройки сети.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Возвращает LiveData для отслеживания состояния сети
     */
    fun getNetworkLiveData(context: Context): LiveData<Boolean> {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkLiveData = MutableLiveData<Boolean>()
        
        // Проверяем текущее состояние сети
        val isConnected = isInternetAvailable(context)
        networkLiveData.postValue(isConnected)
        
        // Создаем запрос на отслеживание сети
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networkLiveData.postValue(true)
            }
            
            override fun onLost(network: Network) {
                networkLiveData.postValue(false)
            }
            
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                networkLiveData.postValue(hasInternet)
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        return networkLiveData
    }
    
    /**
     * Проверяет наличие сети и показывает сообщение, если сети нет
     * @return true если сеть доступна, false в противном случае
     */
    fun checkNetworkAndShowError(context: Context): Boolean {
        val isConnected = isInternetAvailable(context)
        if (!isConnected) {
            showNoInternetToast(context)
        }
        return isConnected
    }
    
    /**
     * Обработка ошибок сети
     */
    fun handleNetworkError(context: Context, exception: Exception): String {
        return when (exception) {
            is java.net.UnknownHostException -> {
                showNoInternetToast(context)
                "Не удалось подключиться к серверу. Проверьте подключение к интернету."
            }
            is java.net.SocketTimeoutException -> {
                "Превышено время ожидания ответа от сервера. Попробуйте позже."
            }
            is javax.net.ssl.SSLHandshakeException -> {
                "Ошибка безопасного соединения с сервером."
            }
            else -> {
                "Ошибка сети: ${exception.message}"
            }
        }
    }
} 