import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.sevostyanov.aiscemetery.Burial
import ru.sevostyanov.aiscemetery.R

class BurialAdapter(
    private val burials: List<Burial>,
    private val onItemClick: (Burial) -> Unit
) : RecyclerView.Adapter<BurialAdapter.BurialViewHolder>() {

    class BurialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fio: TextView = itemView.findViewById(R.id.tv_fio)
        val birthDeathDates: TextView = itemView.findViewById(R.id.tv_birth_death_dates)
        val biography: TextView = itemView.findViewById(R.id.tv_biography)
        val photo: ImageView = itemView.findViewById(R.id.iv_photo)

        fun bind(burial: Burial, onItemClick: (Burial) -> Unit) {
            fio.text = burial.fio

            val birthDeathText = "${burial.birthDate} - ${burial.deathDate}"
            birthDeathDates.text = birthDeathText

            biography.text = burial.biography ?: "Нет данных о биографии"

            burial.photo?.let {
                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                photo.setImageBitmap(bitmap)
            } ?: run {
                photo.setImageResource(R.drawable.amogus) // Плейсхолдер, если фото нет
            }

            // Устанавливаем обработчик клика
            itemView.setOnClickListener { onItemClick(burial) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BurialViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_burial, parent, false)
        return BurialViewHolder(view)
    }

    override fun onBindViewHolder(holder: BurialViewHolder, position: Int) {
        val burial = burials[position]
        holder.bind(burial, onItemClick) // Передаём данные и обработчик клика
    }

    override fun getItemCount(): Int {
        return burials.size
    }
}
