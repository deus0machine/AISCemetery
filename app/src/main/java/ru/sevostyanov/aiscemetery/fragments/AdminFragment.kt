package ru.sevostyanov.aiscemetery.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.sevostyanov.aiscemetery.user.Guest
import ru.sevostyanov.aiscemetery.user.GuestListActivity
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.RetrofitClient

class AdminFragment : Fragment() {
    private lateinit var apiService: RetrofitClient.ApiService
    private lateinit var viewGuestsButton: Button
    private lateinit var sendRequestButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin, container, false)
        viewGuestsButton = view.findViewById(R.id.view_guests_button)
        sendRequestButton = view.findViewById(R.id.send_request_button)
        
        viewGuestsButton.setOnClickListener {
            setupApiService()
            openListOfGuests()
        }
        
        sendRequestButton.setOnClickListener {
            setupApiService()
            sendEmailToServer()
        }

        return view
    }

    private fun sendEmailToServer() {
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

    private fun openListOfGuests() {
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

    private fun setupApiService() {
        apiService = RetrofitClient.getApiService()
    }
}
