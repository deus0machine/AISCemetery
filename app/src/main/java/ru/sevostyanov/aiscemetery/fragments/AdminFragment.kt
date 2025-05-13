package ru.sevostyanov.aiscemetery.fragments

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.sevostyanov.aiscemetery.user.Guest
import ru.sevostyanov.aiscemetery.user.GuestListActivity
import ru.sevostyanov.aiscemetery.order.Order
import ru.sevostyanov.aiscemetery.order.OrderReport
import ru.sevostyanov.aiscemetery.order.OrdersActivity
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.RetrofitClient

class AdminFragment : Fragment() {
    private lateinit var apiService: RetrofitClient.ApiService
    private lateinit var generateReportButton: Button
    private lateinit var viewGuestButton: Button
    private lateinit var viewOrdersButton: Button
    private lateinit var sendRequestButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin, container, false)
        viewGuestButton = view.findViewById(R.id.view_guest_button)
        viewOrdersButton = view.findViewById(R.id.view_orders_button)
        generateReportButton = view.findViewById(R.id.generate_report_button)
        sendRequestButton = view.findViewById(R.id.send_request_button)
        viewGuestButton.setOnClickListener{
            setupApiService()
            openListOfGuest()
        }
        viewOrdersButton.setOnClickListener{
            setupApiService()
            openListOfOrders()
        }
        generateReportButton.setOnClickListener {
            openDateRangePicker()
        }
        sendRequestButton.setOnClickListener {
            setupApiService()
            sendEmailToServer()
        }

        return view
    }
    private fun sendEmailToServer(){
        val editText = EditText(context).apply { hint = "Введите email" }
        AlertDialog.Builder(requireContext())
            .setTitle("Отправка запроса")
            .setView(editText)
            .setPositiveButton("ОК") { _, _ ->
                val email = editText.text.toString()
                if (email.isNotEmpty()) {
                    apiService.sendRequest(email).enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            if (response.isSuccessful) {
                                Toast.makeText(context, "Запрос отправлен", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Ошибка отправки", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Toast.makeText(context, "Ошибка: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    Toast.makeText(context, "Введите email", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    private fun openListOfGuest() {
        apiService.getAllGuests().enqueue(object : Callback<List<Guest>> {
            override fun onResponse(call: Call<List<Guest>>, response: Response<List<Guest>>) {
                response.body()?.takeIf { response.isSuccessful }?.let { guests ->
                    val intent = Intent(context, GuestListActivity::class.java).apply {
                        putParcelableArrayListExtra("guests", ArrayList(guests))
                    }
                    startActivity(intent)
                } ?: run {
                    Toast.makeText(context, "Ошибка загрузки гостей", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Guest>>, t: Throwable) {
                Toast.makeText(context, "Ошибка загрузки гостей: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun openListOfOrders() {
        apiService.getAllOrders().enqueue(object : Callback<List<OrderReport>> {
            override fun onResponse(call: Call<List<OrderReport>>, response: Response<List<OrderReport>>) {
                response.body()?.takeIf { response.isSuccessful }?.let { orders ->
                    val intent = Intent(context, OrdersActivity::class.java).apply {
                        putParcelableArrayListExtra("orders", ArrayList(orders))
                    }
                    startActivity(intent)
                } ?: run {
                    Toast.makeText(context, "Ошибка загрузки заказов", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<OrderReport>>, t: Throwable) {
                Toast.makeText(context, "Ошибка загрузки заказов: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupApiService() {
        apiService = RetrofitClient.getApiService()
    }
    private fun openDateRangePicker() {
        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Выберите диапазон дат")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second
            if (startDate != null && endDate != null) {
                fetchOrdersAndGenerateReport(startDate, endDate)
            } else {
                Toast.makeText(requireContext(), "Выберите корректный диапазон дат", Toast.LENGTH_SHORT).show()
            }
        }

        datePicker.show(parentFragmentManager, "date_range_picker")
    }

    private fun fetchOrdersAndGenerateReport(startDate: Long, endDate: Long) {
        val apiService = RetrofitClient.getApiService()

        apiService.getOrdersBetweenDates(startDate, endDate).enqueue(object :
            Callback<List<Order>> {
            override fun onResponse(call: Call<List<Order>>, response: Response<List<Order>>) {
                if (response.isSuccessful) {
                    val orders = response.body() ?: emptyList()
                    if (orders.isNotEmpty()) {
                        generatePdfReport(orders)
                    } else {
                        Toast.makeText(requireContext(), "Нет данных за выбранный период", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Ошибка при получении данных", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Order>>, t: Throwable) {
                Toast.makeText(requireContext(), "Ошибка подключения: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun generatePdfReport(orders: List<Order>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint()

        paint.textSize = 12f
        canvas.drawText("Отчет по заказам", 50f, 50f, paint)

        var yOffset = 100f
        orders.forEach { order ->
            val orderDetails = "ID: ${order.id}, Дата: ${order.orderDate}, Клиент: , Захоронение:  Заказ: ${order.orderName}, Сумма: ${order.orderCost}"
            canvas.drawText(orderDetails, 50f, yOffset, paint)
            yOffset += 20f
        }

        pdfDocument.finishPage(page)

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "OrdersReport.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }

        val resolver = requireContext().contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)

        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                Toast.makeText(requireContext(), "Отчет сохранен в Documents", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка сохранения отчета: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(requireContext(), "Ошибка создания файла", Toast.LENGTH_SHORT).show()
        }

        pdfDocument.close()
    }
}
