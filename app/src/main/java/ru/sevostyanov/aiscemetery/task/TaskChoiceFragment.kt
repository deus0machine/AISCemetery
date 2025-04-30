package ru.sevostyanov.aiscemetery.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.sevostyanov.aiscemetery.R

class TaskChoiceFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_choice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        // Создаем список задач с правильными типами данных
        val tasks = listOf(
            Task(1L, "Задача 1", "1000", "Описание задачи 1"),
            Task(2L, "Задача 2", "2000", "Описание задачи 2"),
            Task(3L, "Задача 3", "3000", "Описание задачи 3")
        )
        
        adapter = TaskAdapter(tasks) { task ->
            // Обработка клика по задаче
            // TODO: Добавить навигацию к деталям задачи или другую логику
        }
        recyclerView.adapter = adapter
    }
}