package ru.sevostyanov.aiscemetery.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class PagedResponse<T : Parcelable>(
    @SerializedName("content")
    val content: List<T>,
    
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("size")
    val size: Int,
    
    @SerializedName("totalElements")
    val totalElements: Long,
    
    @SerializedName("totalPages")
    val totalPages: Int,
    
    @SerializedName("first")
    val isFirst: Boolean,
    
    @SerializedName("last")
    val isLast: Boolean,
    
    @SerializedName("hasNext")
    val hasNext: Boolean,
    
    @SerializedName("hasPrevious")
    val hasPrevious: Boolean
) : Parcelable 