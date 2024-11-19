package ru.sevostyanov.aiscemetery

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var apiService: ApiService
    private lateinit var loadOrdersButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        setupRecyclerView(view)
        setupApiService()
        setupLoadOrdersButton(view)
        loadUserData() // Загружаем данные пользователя для отображения
        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Теперь безопасно вызываем showUserData, так как view уже доступен
        val userName = arguments?.getString(LoginActivity.KEY_USER_NAME)
        val userContacts = arguments?.getString(LoginActivity.KEY_USER_CONTACTS)
        val userRegDate = arguments?.getString(LoginActivity.KEY_USER_REG_DATE)

        showUserData(userName, userContacts, userRegDate)
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view_order_history)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupApiService() {
        apiService = RetrofitClient.getApiService()
    }

    private fun setupLoadOrdersButton(view: View) {
        loadOrdersButton = view.findViewById(R.id.btn_load_orders) // Ищем кнопку
        loadOrdersButton.setOnClickListener {
            loadOrders() // Обрабатываем нажатие
        }
    }

    private fun loadOrders() {
        val guestId = getGuestIdFromPreferences()

        if (guestId == -1L) {
            Toast.makeText(requireContext(), "Ошибка: пользователь не найден.", Toast.LENGTH_SHORT).show()
        } else {
            fetchOrders(guestId)
        }
    }

    private fun getGuestIdFromPreferences(): Long {
        val sharedPreferences = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return sharedPreferences.getLong(LoginActivity.KEY_USER_ID, -1) // Используем тот же ключ, что и в LoginActivity
    }

    private fun fetchOrders(guestId: Long) {
        lifecycleScope.launch {
            try {
                val orders = apiService.getOrdersByGuest(guestId) // Получение данных с сервера
                updateRecyclerView(orders)
            } catch (e: Exception) {
                handleFetchError(e)
            }
        }
    }

    private fun updateRecyclerView(orders: List<Order>) {
        if (orders.isEmpty()) {
            Toast.makeText(requireContext(), "Заказы отсутствуют.", Toast.LENGTH_SHORT).show()
        } else {
            orderAdapter = OrderAdapter(orders)
            recyclerView.adapter = orderAdapter
        }
    }

    private fun handleFetchError(e: Exception) {
        Toast.makeText(requireContext(), "Ошибка загрузки заказов: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    }

    private fun loadUserData() {
        val sharedPreferences = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getLong(LoginActivity.KEY_USER_ID, -1)
        val userName = sharedPreferences.getString(LoginActivity.KEY_USER_NAME, "Неизвестно")
        val userContacts = sharedPreferences.getString(LoginActivity.KEY_USER_CONTACTS, "Неизвестно")
        val userRegDate = sharedPreferences.getString(LoginActivity.KEY_USER_REG_DATE, "Неизвестно")

        if (userId != -1L) {
            showUserData(userName, userContacts, userRegDate)
        } else {
            Toast.makeText(requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUserData(name: String?, contacts: String?, regDate: String?) {
        val nameTextView = view?.findViewById<TextView>(R.id.profile_name)
        val contactsTextView = view?.findViewById<TextView>(R.id.profile_contacts)
        val regDateTextView = view?.findViewById<TextView>(R.id.profile_reg_date)

        nameTextView?.text = name
        contactsTextView?.text = contacts
        regDateTextView?.text = regDate
    }
}




