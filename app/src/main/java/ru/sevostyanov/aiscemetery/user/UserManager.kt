package ru.sevostyanov.aiscemetery.user

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ru.sevostyanov.aiscemetery.LoginActivity
import ru.sevostyanov.aiscemetery.RetrofitClient

object UserManager {
    private var currentUser: Guest? = null
    
    fun setCurrentUser(user: Guest) {
        Log.d("UserManager", "Setting current user: $user")
        currentUser = user
        // Устанавливаем токен в RetrofitClient при установке пользователя
        RetrofitClient.setToken(user.token)
    }
    
    fun getCurrentUser(): Guest? {
        Log.d("UserManager", "Getting current user: $currentUser")
        return currentUser
    }
    
    fun clearUser() {
        Log.d("UserManager", "Clearing current user")
        currentUser = null
        RetrofitClient.clearToken()
    }
    
    private fun getEncryptedPreferences(context: Context) = try {
        Log.d("UserManager", "Creating EncryptedSharedPreferences")
        EncryptedSharedPreferences.create(
            context,
            "user_data",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("UserManager", "Failed to create EncryptedSharedPreferences, falling back to regular SharedPreferences", e)
        context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
    }
    
    fun saveUserToPreferences(context: Context, user: Guest) {
        Log.d("UserManager", "Saving user to preferences: $user")
        try {
            val sharedPreferences = getEncryptedPreferences(context)
            with(sharedPreferences.edit()) {
                putLong(LoginActivity.KEY_USER_ID, user.id)
                putString(LoginActivity.KEY_USER_NAME, user.fio)
                putString(LoginActivity.KEY_USER_CONTACTS, user.contacts)
                putString(LoginActivity.KEY_USER_REG_DATE, user.dateOfRegistration)
                putString(LoginActivity.KEY_USER_LOGIN, user.login)
                putLong(LoginActivity.KEY_USER_BALANCE, user.balance)
                putString(LoginActivity.KEY_USER_ROLE, user.role)
                putString(LoginActivity.KEY_USER_TOKEN, user.token)
                commit() // Используем commit() вместо apply() для немедленного сохранения
            }
            Log.d("UserManager", "Successfully saved user data to preferences")
            setCurrentUser(user)
        } catch (e: Exception) {
            Log.e("UserManager", "Failed to save user data to preferences", e)
            throw e
        }
    }
    
    fun loadUserFromPreferences(context: Context): Guest? {
        Log.d("UserManager", "Loading user from preferences")
        try {
            val sharedPreferences = getEncryptedPreferences(context)
            val userId = sharedPreferences.getLong(LoginActivity.KEY_USER_ID, -1)
            if (userId == -1L) {
                Log.d("UserManager", "No user data found in preferences")
                return null
            }
            
            val user = Guest(
                id = userId,
                fio = sharedPreferences.getString(LoginActivity.KEY_USER_NAME, "") ?: "",
                contacts = sharedPreferences.getString(LoginActivity.KEY_USER_CONTACTS, "") ?: "",
                dateOfRegistration = sharedPreferences.getString(LoginActivity.KEY_USER_REG_DATE, "") ?: "",
                login = sharedPreferences.getString(LoginActivity.KEY_USER_LOGIN, "") ?: "",
                balance = sharedPreferences.getLong(LoginActivity.KEY_USER_BALANCE, 0),
                role = sharedPreferences.getString(LoginActivity.KEY_USER_ROLE, "USER") ?: "USER",
                token = sharedPreferences.getString(LoginActivity.KEY_USER_TOKEN, "") ?: ""
            )
            Log.d("UserManager", "Successfully loaded user from preferences: $user")
            setCurrentUser(user)
            return user
        } catch (e: Exception) {
            Log.e("UserManager", "Failed to load user from preferences", e)
            return null
        }
    }
    
    fun clearUserData(context: Context) {
        Log.d("UserManager", "Clearing user data")
        try {
            // Очищаем обычные SharedPreferences
            context.getSharedPreferences("user_data", Context.MODE_PRIVATE).edit().clear().commit()
            Log.d("UserManager", "Cleared regular SharedPreferences")
            
            // Пробуем очистить EncryptedSharedPreferences
            try {
                getEncryptedPreferences(context).edit().clear().commit()
                Log.d("UserManager", "Cleared EncryptedSharedPreferences")
            } catch (e: Exception) {
                Log.e("UserManager", "Failed to clear EncryptedSharedPreferences", e)
            }
            
            // Очищаем данные в памяти
            clearUser()
            
            // Очищаем токен в RetrofitClient
            RetrofitClient.clearToken()
        } catch (e: Exception) {
            Log.e("UserManager", "Failed to clear user data", e)
            // Даже если произошла ошибка, очищаем данные в памяти
            clearUser()
            RetrofitClient.clearToken()
        }
    }
} 