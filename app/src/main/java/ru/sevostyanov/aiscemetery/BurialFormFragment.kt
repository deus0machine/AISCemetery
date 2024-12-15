package ru.sevostyanov.aiscemetery

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BurialFormFragment : Fragment() {

    private lateinit var fioEditText: EditText
    private lateinit var birthDateEditText: EditText
    private lateinit var deathDateEditText: EditText
    private lateinit var biographyEditText: EditText
    private lateinit var submitButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_burial_form, container, false)
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack() // Navigate back
        }
        fioEditText = view.findViewById(R.id.fioEditText)
        birthDateEditText = view.findViewById(R.id.birthDateEditText)
        deathDateEditText = view.findViewById(R.id.deathDateEditText)
        biographyEditText = view.findViewById(R.id.biographyEditText)
        submitButton = view.findViewById(R.id.submitButton)

        submitButton.setOnClickListener {
            val fio = fioEditText.text.toString()
            val birthDate = birthDateEditText.text.toString()
            val deathDate = deathDateEditText.text.toString()
            val biography = biographyEditText.text.toString()

            if (fio.isNotEmpty() && birthDate.isNotEmpty() && deathDate.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        val guestId = getGuestIdFromPreferences()
                        val balance = getBalanceGuest(guestId) // Асинхронный запрос баланса

                        val burial = Burial(
                            fio = fio,
                            birthDate = birthDate,
                            deathDate = deathDate,
                            biography = biography,
                            guest = Guest(id = guestId, balance = balance)
                        )

                        // Отправляем данные на сервер
                        submitBurialData(burial)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Ошибка при отправке данных: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Пожалуйста, заполните все обязательные поля", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
    private fun getGuestIdFromPreferences(): Long {
        val sharedPreferences = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return sharedPreferences.getLong(LoginActivity.KEY_USER_ID, -1)
    }
    private suspend fun getBalanceGuest(id: Long): Long {
        val apiService = RetrofitClient.getApiService()
        return try {
            val guest = apiService.getGuest(id)
            guest.balance
        } catch (e: Exception) {
            // Логируйте ошибку или обработайте её
            0L // Возвращаем 0 в случае ошибки
        }
    }
    private fun submitBurialData(burial: Burial) {
        val apiService = RetrofitClient.getApiService()
        val call = apiService.registerBurial(burial)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Захоронение успешно добавлено", Toast.LENGTH_SHORT).show()
                    // Переход на другой фрагмент или действие
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, TaskFragment())
                        .addToBackStack(null)
                        .commit()
                } else {
                    Toast.makeText(context, "Ошибка при добавлении захоронения", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(context, "Ошибка сети", Toast.LENGTH_SHORT).show()
            }
        })
    }
}