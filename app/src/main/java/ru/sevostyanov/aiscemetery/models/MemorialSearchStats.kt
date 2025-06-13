package ru.sevostyanov.aiscemetery.models

import com.google.gson.annotations.SerializedName

data class MemorialSearchStats(
    @SerializedName("totalMemorials")
    val totalMemorials: Long,
    
    @SerializedName("publicMemorials")
    val publicMemorials: Long,
    
    @SerializedName("privateMemorials")
    val privateMemorials: Long,
    
    @SerializedName("popularLocations")
    val popularLocations: List<LocationStat>,
    
    @SerializedName("recentSearches")
    val recentSearches: List<String>,
    
    @SerializedName("topSearchTerms")
    val topSearchTerms: List<SearchTermStat>
)

data class LocationStat(
    @SerializedName("location")
    val location: String,
    
    @SerializedName("count")
    val count: Long
)

data class SearchTermStat(
    @SerializedName("term")
    val term: String,
    
    @SerializedName("count")
    val count: Long
) 