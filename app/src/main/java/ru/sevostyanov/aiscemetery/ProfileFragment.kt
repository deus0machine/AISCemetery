package ru.sevostyanov.aiscemetery

import android.content.Context
import android.content.Intent
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
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var apiService: ApiService
    private lateinit var loadOrdersButton: Button
    private lateinit var updateBalanceButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        setupRecyclerView(view)
        setupApiService()
        setupLoadOrdersButton(view)
        setupUpdateBalanceButton(view)
        setupLogoutButton(view)
        loadUserData() // Загружаем данные пользователя для отображения
        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Загружаем данные пользователя из SharedPreferences
        val sharedPreferences = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString(LoginActivity.KEY_USER_NAME, "Неизвестно")
        val userContacts = sharedPreferences.getString(LoginActivity.KEY_USER_CONTACTS, "Неизвестно")
        val userRegDate = sharedPreferences.getString(LoginActivity.KEY_USER_REG_DATE, "Неизвестно")
        val userBalance = sharedPreferences.getLong(LoginActivity.KEY_USER_BALANCE, -1)

        showUserData(userName, userContacts, userRegDate, userBalance.toString())
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
    private fun setupUpdateBalanceButton(view: View) {
        updateBalanceButton = view.findViewById(R.id.btn_update_balance) // Ищем кнопку
        updateBalanceButton.setOnClickListener {
            updateBalance() // Обрабатываем нажатие
        }
    }
    private fun updateBalance() {
        val guestId = getGuestIdFromPreferences()
        if (guestId == -1L) {
            Toast.makeText(requireContext(), "Ошибка: пользователь не найден.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val guest = apiService.getGuest(guestId)
                val textBalance = view?.findViewById<TextView>(R.id.profile_balance)
                textBalance?.text = "Баланс: ${guest.balance} рублей"
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка загрузки данных: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
        val userBalance = sharedPreferences.getLong(LoginActivity.KEY_USER_BALANCE, -1)

        if (userId != -1L) {
            showUserData(userName, userContacts, userRegDate, userBalance.toString())
        } else {
            Toast.makeText(requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUserData(name: String?, contacts: String?, regDate: String?, balance: String?) {
        val nameTextView = view?.findViewById<TextView>(R.id.profile_name)
        val contactsTextView = view?.findViewById<TextView>(R.id.profile_contacts)
        val regDateTextView = view?.findViewById<TextView>(R.id.profile_reg_date)
        val balanceTextView = view?.findViewById<TextView>(R.id.profile_balance)  // Баланс

        nameTextView?.text = name
        contactsTextView?.text = contacts

        val formattedRegDate = regDate?.substringBefore("T") // Если формат ISO 8601 (yyyy-MM-ddTHH:mm:ss)
            ?: regDate?.takeWhile { it.isDigit() || it == '-' } // Общий случай, оставляем только часть с датой

        regDateTextView?.text = formattedRegDate
        balanceTextView?.text = "Баланс: $balance рублей" // Обновляем текст с балансом
    }
    private fun setupLogoutButton(view: View) {
        val logoutButton = view.findViewById<Button>(R.id.btn_logout)
        logoutButton.setOnClickListener {
            val sharedPreferences = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply() // Очищаем данные пользователя

            // Перенаправляем пользователя на экран авторизации
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish() // Завершаем текущую Activity, чтобы предотвратить возврат назад
        }
    }
}




