package ru.sevostyanov.aiscemetery.task

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.sevostyanov.aiscemetery.R


class TaskAdapter(
    private val tasks: List<Task>,
    private val onTaskClick: (Task) -> Unit // Обработчик клика на задачу
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // ViewHolder для хранения ссылок на виджеты элемента
    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.task_name)
        val costTextView: TextView = itemView.findViewById(R.id.task_cost)
        val descriptionTextView: TextView = itemView.findViewById(R.id.task_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        // Привязка данных задачи к элементу списка
        holder.nameTextView.text = task.name
        holder.costTextView.text = "Цена: ${task.cost}" // Форматирование цены
        holder.descriptionTextView.text = task.description

        // Установка обработчика клика
        holder.itemView.setOnClickListener { onTaskClick(task) }
    }

    override fun getItemCount(): Int = tasks.size
}
