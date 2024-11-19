package ru.sevostyanov.aiscemetery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import retrofit2.http.GET
import retrofit2.http.Path

class OrderAdapter(private val orders: List<Order>) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orderName: TextView = view.findViewById(R.id.order_name)
        val orderDescription: TextView = view.findViewById(R.id.order_description)
        val orderCost: TextView = view.findViewById(R.id.order_cost)
        val orderDate: TextView = view.findViewById(R.id.order_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.orderName.text = order.orderName
        holder.orderDescription.text = order.orderDescription
        holder.orderCost.text = "Стоимость: ${order.orderCost} руб."
        holder.orderDate.text = "Дата: ${order.orderDate}"
    }

    override fun getItemCount(): Int = orders.size
}

data class Order(
    val id: Long,
    val orderName: String,
    val orderDescription: String,
    val orderCost: Long,
    val orderDate: String // Формат: "yyyy-MM-dd"
)

interface ApiService : LoginActivity.LoginService {
    @GET("api/orders/guest/{guestId}")
    suspend fun getOrdersByGuest(@Path("guestId") guestId: Long): List<Order>
}




