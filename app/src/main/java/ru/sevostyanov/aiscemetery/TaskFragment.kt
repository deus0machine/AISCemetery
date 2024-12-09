package ru.sevostyanov.aiscemetery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class TaskFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack() // Возврат назад
        }

        // loadTasks(view)

        return view
    }

//    private fun loadTasks(view: View) {
//        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_services)
//        recyclerView.layoutManager = LinearLayoutManager(context)
//
//        // Эмуляция загрузки из базы
//        val apiService = RetrofitClient.getApiService()
//        lifecycleScope.launch {
//            try {
//                val services = apiService.getServices() // Подгрузка услуг из API
//                recyclerView.adapter = ServiceAdapter(services)
//            } catch (e: Exception) {
//                Toast.makeText(context, "Не можем загрузить услуги", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
}