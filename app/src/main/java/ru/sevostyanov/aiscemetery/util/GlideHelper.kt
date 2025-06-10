package ru.sevostyanov.aiscemetery.util

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

/**
 * Простая утилита для загрузки изображений через Glide
 */
object GlideHelper {
    /**
     * Загружает изображение из URL
     */
    fun loadImage(
        context: Context, 
        url: String?, 
        imageView: ImageView,
        @DrawableRes placeholderRes: Int = android.R.drawable.ic_menu_gallery,
        @DrawableRes errorRes: Int = android.R.drawable.ic_menu_report_image
    ) {
        Glide.with(context)
            .load(url)
            .placeholder(placeholderRes)
            .error(errorRes)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(imageView)
    }
    
    /**
     * Загружает изображение из Uri
     */
    fun loadImageFromUri(
        context: Context, 
        uri: Uri?, 
        imageView: ImageView,
        @DrawableRes placeholderRes: Int = android.R.drawable.ic_menu_gallery,
        @DrawableRes errorRes: Int = android.R.drawable.ic_menu_report_image
    ) {
        Glide.with(context)
            .load(uri)
            .placeholder(placeholderRes)
            .error(errorRes)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .centerCrop()
            .into(imageView)
    }
    
    /**
     * Очищает кэш Glide
     */
    fun clearCache(context: Context) {
        Glide.get(context).clearMemory()
        Thread {
            Glide.get(context).clearDiskCache()
        }.start()
    }
} 