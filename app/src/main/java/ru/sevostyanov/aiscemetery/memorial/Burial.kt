package ru.sevostyanov.aiscemetery.memorial
import com.google.gson.annotations.SerializedName
import ru.sevostyanov.aiscemetery.user.Guest

data class Burial(
      @SerializedName("id")
      val id: Long? = null,

      @SerializedName("guest")
      val guest: Guest,

      @SerializedName("fio")
      val fio: String,

      @SerializedName("birthDate")
      val birthDate: String,

      @SerializedName("deathDate")
      val deathDate: String,

      @SerializedName("biography")
      val biography: String? = null,

      @SerializedName("photo")
      val photo: String? = null,

      @SerializedName("xCoord")
      val xCoord: Long? = null,

      @SerializedName("yCoord")
      val yCoord: Long? = null  )
