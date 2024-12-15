package ru.sevostyanov.aiscemetery

import android.content.Context
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
                if (birthDate != null && deathDate != null) {
                    val burial = Burial(
                        fio = fio,
                        birthDate = birthDate,
                        deathDate = deathDate,
                        biography = biography,
                        guest = Guest(id = getGuestIdFromPreferences())
                    )

                // Отправляем данные на сервер
                submitBurialData(burial)
            } else {
                Toast.makeText(context, "Пожалуйста, заполните все обязательные поля", Toast.LENGTH_SHORT).show()
            }
            }
        }

        return view
    }
    private fun getGuestIdFromPreferences(): Long {
        val sharedPreferences = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return sharedPreferences.getLong(LoginActivity.KEY_USER_ID, -1)
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