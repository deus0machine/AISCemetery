package ru.sevostyanov.aiscemetery

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OrdersReportAdapter(
    private val orders: List<OrderReport>,
    private val onMarkAsCompleted: (OrderReport) -> Unit,
    val onDelete: (OrderReport) -> Unit
) : RecyclerView.Adapter<OrdersReportAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderName: TextView = itemView.findViewById(R.id.order_name)
        val orderDescription: TextView = itemView.findViewById(R.id.order_description)
        val orderCost: TextView = itemView.findViewById(R.id.order_cost)
        val markAsCompletedButton: Button = itemView.findViewById(R.id.mark_as_completed_button)
        val deleteButton: Button = itemView.findViewById(R.id.delete_button)
        val guestName: TextView = itemView.findViewById(R.id.guest_name)
        val burialName: TextView = itemView.findViewById(R.id.burial_name)

        fun bind(order: OrderReport) {
            orderName.text = "${order.orderName} (${order.guestName} -> ${order.burialName})"
            orderDescription.text = order.orderDescription
            orderCost.text = "Стоимость: ${order.orderCost} руб."

            itemView.setBackgroundColor(
                if (order.isCompleted) Color.LTGRAY else Color.WHITE
            )

            markAsCompletedButton.setOnClickListener { onMarkAsCompleted(order) }
            deleteButton.setOnClickListener { onDelete(order) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_report, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.orderName.text = order.orderName
        holder.orderDescription.text = order.orderDescription
        holder.orderCost.text = "Стоимость: ${order.orderCost} ₽"
        holder.guestName.text = "Гость: ${order.guestName}"
        holder.burialName.text = "Погребение: ${order.burialName}"
        holder.itemView.setBackgroundColor(
            if (order.isCompleted) Color.GREEN else Color.WHITE
        )
        holder.markAsCompletedButton.setOnClickListener {
            onMarkAsCompleted(order)
        }
        holder.deleteButton.setOnClickListener {
            onDelete(order)
        }
    }

    override fun getItemCount() = orders.size
}

