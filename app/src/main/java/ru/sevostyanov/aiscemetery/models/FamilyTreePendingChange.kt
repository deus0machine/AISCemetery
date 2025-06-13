package ru.sevostyanov.aiscemetery.models

import com.google.gson.annotations.SerializedName

data class FamilyTreePendingChange(
    @SerializedName("id")
    val id: Long? = null,
    
    @SerializedName("familyTreeId")
    val familyTreeId: Long,
    
    @SerializedName("editorId")
    val editorId: Long,
    
    @SerializedName("changeType")
    val changeType: String, // "ADD_MEMORIAL", "REMOVE_MEMORIAL", "ADD_RELATION", "MODIFY_RELATION", "REMOVE_RELATION"
    
    @SerializedName("memorialId")
    val memorialId: Long? = null,
    
    @SerializedName("sourceMemorialId")
    val sourceMemorialId: Long? = null,
    
    @SerializedName("targetMemorialId")
    val targetMemorialId: Long? = null,
    
    @SerializedName("relationType")
    val relationType: String? = null,
    
    @SerializedName("existingRelationId")
    val existingRelationId: Long? = null,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("status")
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    
    @SerializedName("reviewedAt")
    val reviewedAt: String? = null,
    
    @SerializedName("reviewedBy")
    val reviewedBy: Long? = null,
    
    @SerializedName("reviewComment")
    val reviewComment: String? = null
) 