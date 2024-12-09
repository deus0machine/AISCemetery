package ru.sevostyanov.aiscemetery
import java.util.Date
import com.google.gson.annotations.SerializedName

data class Burial(
      @SerializedName("id")
      val id: Long? = null,

      @SerializedName("fio")
      val fio: String,

      @SerializedName("birth_date")
      val birthDate: Date,

      @SerializedName("death_date")
      val deathDate: Date,

      @SerializedName("biography")
      val biography: String? = null,  // Biography может быть пустым

      @SerializedName("photo")
      val photo: ByteArray? = null,  // Фото может быть пустым

      @SerializedName("xCoord")
      val xCoord: Long? = null,  // Координаты могут быть пустыми

      @SerializedName("yCoord")
      val yCoord: Long? = null  )
