import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
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
    private val onItemClick: ((Burial) -> Unit)? = null, // Делается nullable
    private val isSelectable: Boolean = false // Режим работы адаптера
) : RecyclerView.Adapter<BurialAdapter.BurialViewHolder>() {

    class BurialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fio: TextView = itemView.findViewById(R.id.tv_fio)
        val birthDeathDates: TextView = itemView.findViewById(R.id.tv_birth_death_dates)
        val biography: TextView = itemView.findViewById(R.id.tv_biography)
        val photo: ImageView = itemView.findViewById(R.id.iv_photo)

        fun bind(
            burial: Burial,
            onItemClick: ((Burial) -> Unit)?,
            isSelectable: Boolean
        ) {
            fio.text = burial.fio

            val birthDeathText = "${burial.birthDate} - ${burial.deathDate}"
            birthDeathDates.text = birthDeathText

            biography.text = burial.biography ?: "Нет данных о биографии"

            burial.photo?.let { base64String ->
                try {
                    val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    photo.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    photo.setImageResource(R.drawable.amogus) // Плейсхолдер
                }
            } ?: run {
                photo.setImageResource(R.drawable.amogus) // Плейсхолдер, если фото нет
            }

            // Клик работает в любом режиме, если есть обработчик
            itemView.setOnClickListener {
                onItemClick?.invoke(burial)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BurialViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_burial, parent, false)
        return BurialViewHolder(view)
    }

    override fun onBindViewHolder(holder: BurialViewHolder, position: Int) {
        val burial = burials[position]
        holder.bind(burial, onItemClick, isSelectable) // Передаём режим работы
    }

    override fun getItemCount(): Int {
        return burials.size
    }
}

