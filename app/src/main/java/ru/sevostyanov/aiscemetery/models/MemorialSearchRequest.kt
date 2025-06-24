package ru.sevostyanov.aiscemetery.models

import com.google.gson.annotations.SerializedName

data class MemorialSearchRequest(
    @SerializedName("firstName")
    val firstName: String? = null,
    
    @SerializedName("lastName")
    val lastName: String? = null,
    
    @SerializedName("middleName")
    val middleName: String? = null,
    
    @SerializedName("birthDateFrom")
    val birthDateFrom: String? = null,
    
    @SerializedName("birthDateTo")
    val birthDateTo: String? = null,
    
    @SerializedName("deathDateFrom")
    val deathDateFrom: String? = null,
    
    @SerializedName("deathDateTo")
    val deathDateTo: String? = null,
    
    @SerializedName("location")
    val location: String? = null,
    
    @SerializedName("query")
    val query: String? = null,
    
    @SerializedName("isPublic")
    val isPublic: Boolean? = null,
    
    @SerializedName("sortBy")
    val sortBy: String? = null,
    
    @SerializedName("sortDirection")
    val sortDirection: String? = null,
    
    @SerializedName("page")
    val page: Int = 0,
    
    @SerializedName("size")
    val size: Int = 10
) {
    fun hasFilters(): Boolean {
        return !firstName.isNullOrBlank() ||
               !lastName.isNullOrBlank() ||
               !middleName.isNullOrBlank() ||
               !birthDateFrom.isNullOrBlank() ||
               !birthDateTo.isNullOrBlank() ||
               !deathDateFrom.isNullOrBlank() ||
               !deathDateTo.isNullOrBlank() ||
               !location.isNullOrBlank() ||
               !query.isNullOrBlank() ||
               isPublic != null
    }
    
    fun clearFilters(): MemorialSearchRequest {
        return copy(
            firstName = null,
            lastName = null,
            middleName = null,
            birthDateFrom = null,
            birthDateTo = null,
            deathDateFrom = null,
            deathDateTo = null,
            location = null,
            query = null,
            isPublic = null,
            sortBy = null,
            sortDirection = null,
            page = 0
        )
    }
} 