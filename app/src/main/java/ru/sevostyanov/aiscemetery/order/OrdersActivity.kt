package ru.sevostyanov.aiscemetery.order

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.RetrofitClient

class OrdersActivity : AppCompatActivity() {

    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var backButton: Button
    private val orders: MutableList<OrderReport> = mutableListOf()
    private lateinit var ordersAdapter: OrdersReportAdapter
    private lateinit var apiService: RetrofitClient.ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders_report)
        backButton = findViewById(R.id.back_button)
        setupApiService()
        ordersRecyclerView = findViewById(R.id.orders_recycler_view)
        val receivedOrders = intent.getParcelableArrayListExtra<OrderReport>("orders") ?: arrayListOf()
        orders.addAll(receivedOrders)

        ordersAdapter = OrdersReportAdapter(
            orders,
            onMarkAsCompleted = { order ->
                // Просто обновляем статус в адаптере
                order.isCompleted = true
                ordersRecyclerView.adapter?.notifyItemChanged(orders.indexOf(order))
                Toast.makeText(this@OrdersActivity, "Статус обновлен", Toast.LENGTH_SHORT).show()
            },
            onDelete = { order ->
                // Просто удаляем из списка
                val position = orders.indexOf(order)
                orders.removeAt(position)
                ordersRecyclerView.adapter?.notifyItemRemoved(position)
                Toast.makeText(this@OrdersActivity, "Заказ удален", Toast.LENGTH_SHORT).show()
            }
        )

        ordersRecyclerView.adapter = ordersAdapter
        ordersRecyclerView.layoutManager = LinearLayoutManager(this)

        backButton.setOnClickListener {
            finish() // Закрываем активность
        }
    }

    private fun setupApiService() {
        apiService = RetrofitClient.getApiService()
    }

    override fun onResume() {
        super.onResume()
        loadOrders() // Запрашиваем обновленный список заказов с сервера
    }

    private fun loadOrders() {
        apiService.getAllOrders().enqueue(object : Callback<List<OrderReport>> {
            override fun onResponse(call: Call<List<OrderReport>>, response: Response<List<OrderReport>>) {
                if (response.isSuccessful) {
                    orders.clear()
                    orders.addAll(response.body() ?: emptyList())
                    ordersAdapter.notifyDataSetChanged()
                }
            }

            override fun onFailure(call: Call<List<OrderReport>>, t: Throwable) {
                Toast.makeText(this@OrdersActivity, "Ошибка загрузки: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
