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

class GuestListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GuestAdapter
    private lateinit var apiService: RetrofitClient.ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guest_list)

        recyclerView = findViewById(R.id.recycler_view_guests)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val backButton: Button = findViewById(R.id.btn_back)
        backButton.setOnClickListener {
            finish() // Закрывает текущую Activity и возвращает к предыдущей
        }
        apiService = RetrofitClient.getApiService()
        val guests = intent.getParcelableArrayListExtra<GuestItem>("guests") ?: mutableListOf()
        adapter = GuestAdapter(guests) { guest ->
            deleteGuest(guest.id)
        }
        recyclerView.adapter = adapter
    }

    private fun deleteGuest(guestId: Long) {
        apiService.deleteGuest(guestId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@GuestListActivity, "Гость удалён", Toast.LENGTH_SHORT).show()
                    adapter.removeGuest(guestId)
                } else {
                    Toast.makeText(this@GuestListActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@GuestListActivity, "Ошибка удаления: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
