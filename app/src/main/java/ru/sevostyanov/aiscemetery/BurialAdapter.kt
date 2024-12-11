import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.sevostyanov.aiscemetery.Burial
import ru.sevostyanov.aiscemetery.R
import java.util.*

class BurialAdapter(
    private val burials: List<Burial>
) : RecyclerView.Adapter<BurialAdapter.BurialViewHolder>() {

    class BurialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fio: TextView = itemView.findViewById(R.id.tv_fio)
        val birthDeathDates: TextView = itemView.findViewById(R.id.tv_birth_death_dates)
        val biography: TextView = itemView.findViewById(R.id.tv_biography)
        val photo: ImageView = itemView.findViewById(R.id.iv_photo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BurialViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_burial, parent, false)
        return BurialViewHolder(view)
    }

    override fun onBindViewHolder(holder: BurialViewHolder, position: Int) {
        val burial = burials[position]
        holder.fio.text = burial.fio

        val birthDeathText = "${burial.birthDate} - ${burial.deathDate}"
        holder.birthDeathDates.text = birthDeathText

        holder.biography.text = burial.biography ?: "Нет данных о биографии"

        // Конвертация байтов фото в картинку
        burial.photo?.let {
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            holder.photo.setImageBitmap(bitmap)
        } ?: run {
            holder.photo.setImageResource(R.drawable.amogus) // Плейсхолдер, если фото нет
        }
    }

    override fun getItemCount(): Int {
        return burials.size
    }
}
