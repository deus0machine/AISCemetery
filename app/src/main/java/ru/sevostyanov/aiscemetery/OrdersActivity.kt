package ru.sevostyanov.aiscemetery

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
                // Тут обновляем статус в адаптере и на сервере
                updateOrderStatus(order, true)
            },
            onDelete = { order ->
                // Тут удаляем элемент из списка и отправляем запрос на сервер
                deleteOrder(order)
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
    private fun updateOrderStatus(order: OrderReport, isCompleted: Boolean) {
        apiService.updateOrderStatus(order.id, isCompleted).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    order.isCompleted = isCompleted
                    ordersRecyclerView.adapter?.notifyItemChanged(orders.indexOf(order))
                    Toast.makeText(this@OrdersActivity, "Статус обновлен", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@OrdersActivity, "Ошибка обновления", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@OrdersActivity, "Ошибка: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
    private fun deleteOrder(order: OrderReport) {
        apiService.deleteOrder(order.id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val position = orders.indexOf(order)
                    orders.removeAt(position)
                    ordersRecyclerView.adapter?.notifyItemRemoved(position)
                    Toast.makeText(this@OrdersActivity, "Заказ удален", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@OrdersActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@OrdersActivity, "Ошибка: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
