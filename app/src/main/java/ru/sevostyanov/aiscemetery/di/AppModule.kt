package ru.sevostyanov.aiscemetery.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.sevostyanov.aiscemetery.RetrofitClient
import ru.sevostyanov.aiscemetery.RetrofitClient.ApiService
import ru.sevostyanov.aiscemetery.RetrofitClient.LoginService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideApiService(context: Context): ApiService {
        RetrofitClient.initialize(context)
        return RetrofitClient.getApiService()
    }

    @Provides
    @Singleton
    fun provideLoginService(context: Context): LoginService {
        RetrofitClient.initialize(context)
        return RetrofitClient.getLoginService()
    }
} 