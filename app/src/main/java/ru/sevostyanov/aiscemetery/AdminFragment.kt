package ru.sevostyanov.aiscemetery

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class AdminFragment : Fragment() {

    private lateinit var generateReportButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin, container, false)

        generateReportButton = view.findViewById(R.id.generate_report_button)

        generateReportButton.setOnClickListener {
            openDateRangePicker()
        }

        return view
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
            val orderDetails = "ID: ${order.id}, Дата: ${order.orderDate}, Клиент: ${order.orderName}, Сумма: ${order.orderCost}"
            canvas.drawText(orderDetails, 50f, yOffset, paint) //ПЕРЕДЕЛАТЬ ЛВЫАОДЫВЛАДВОД!!
            yOffset += 20f
        }

        pdfDocument.finishPage(page)

        val filePath = requireContext().getExternalFilesDir(null)?.absolutePath + "/OrdersReport.pdf"
        val file = File(filePath)
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        Toast.makeText(requireContext(), "Отчет сохранён: $filePath", Toast.LENGTH_LONG).show()
    }
}
