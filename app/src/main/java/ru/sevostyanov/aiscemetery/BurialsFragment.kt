package ru.sevostyanov.aiscemetery

import BurialAdapter
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BurialsFragment : Fragment() {
    private lateinit var apiService: RetrofitClient.ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BurialAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_burials, container, false)
        recyclerView = view.findViewById(R.id.recycler_view_burials)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        setupApiService()
        setupToolbar(view)  // Настройка Toolbar
        loadBurials(isMine = true)  // Загружаем только "Мои" по умолчанию
        return view
    }

    private fun setupApiService() {
        apiService = RetrofitClient.getApiService()
    }

    private fun loadBurials(isMine: Boolean) {
        val guestId = getGuestIdFromPreferences()

        lifecycleScope.launch {
            try {
                val burials = if (isMine) {
                    apiService.getBurialsByGuest(guestId)  // Получаем "Мои"
                } else {
                    apiService.getAllBurials()  // Получаем "Все"
                }
                adapter = BurialAdapter(burials)
                recyclerView.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка загрузки данных: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getGuestIdFromPreferences(): Long {
        val sharedPreferences = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return sharedPreferences.getLong(LoginActivity.KEY_USER_ID, -1)
    }

    // Обработка кнопок в Toolbar
    private fun setupToolbar(view: View) {
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        val btnMine = view.findViewById<MaterialButton>(R.id.btn_mine)
        val btnAll = view.findViewById<MaterialButton>(R.id.btn_all)

        // Устанавливаем начальные стили для кнопок
        btnMine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_300))  // Активный цвет
        btnAll.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_100))   // Неактивный цвет

        btnMine.setOnClickListener {
            loadBurials(isMine = true)  // Загружаем "Мои"
            // Изменяем цвет кнопок
            btnMine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_300))
            btnAll.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_100))  // Бледный цвет для "Все"
        }

        btnAll.setOnClickListener {
            loadBurials(isMine = false)  // Загружаем "Все"
            // Изменяем цвет кнопок
            btnMine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_100))  // Бледный цвет для "Мои"
            btnAll.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_300))
        }

        (activity as AppCompatActivity).setSupportActionBar(toolbar)
    }
}

