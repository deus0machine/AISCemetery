package ru.sevostyanov.aiscemetery

import BurialAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.io.File

class BurialsFragment : Fragment() {
    private lateinit var apiService: RetrofitClient.ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BurialAdapter
    private lateinit var searchEditText: EditText
    private  var isMine : Boolean = false
    private var fullBurialsList: List<Burial> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_burials, container, false)
        recyclerView = view.findViewById(R.id.recycler_view_burials)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        searchEditText = view.findViewById(R.id.et_search)
        setupApiService()
        setupToolbar(view)  // Настройка Toolbar
        setupSearchListener()
        loadBurials(isMine = true)  // Загружаем только "Мои" по умолчанию
        return view
    }

    private fun setupApiService() {
        apiService = RetrofitClient.getApiService()
    }

    private fun loadBurials(isMine: Boolean, isSelectionMode: Boolean = false, onBurialSelected: ((Burial) -> Unit)? = null) {
        val guestId = getGuestIdFromPreferences()

        lifecycleScope.launch {
            try {
                val burials = if (isMine) {
                    apiService.getBurialsByGuest(guestId)  // Получаем "Мои"
                } else {
                    apiService.getAllBurials()  // Получаем "Все"
                }
                fullBurialsList = burials
                adapter = BurialAdapter(
                    burials,
                    onItemClick = if (isSelectionMode) onBurialSelected else { burial ->
                        openBurialDetails(burial, isMine) // Открываем детали
                    },
                    isSelectable = isSelectionMode
                )

                recyclerView.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка загрузки данных: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun updateRecyclerView(filteredList: List<Burial>) {
        adapter = BurialAdapter(
            filteredList,
            onItemClick = { burial ->
                openBurialDetails(burial, isMine)
            },
            isSelectable = false
        )
        recyclerView.adapter = adapter
    }
    private fun setupSearchListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                val filteredList = if (query.isEmpty()) {
                    fullBurialsList // Если пусто, показываем весь список
                } else {
                    fullBurialsList.filter { burial ->
                        burial.fio.contains(query, ignoreCase = true) // Фильтруем по FIO
                    }
                }
                updateRecyclerView(filteredList)
            }
        })
    }
    private val deleteBurialLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val isDeleted = result.data?.getBooleanExtra("isDeleted", false) ?: false
            if (isDeleted) {
                // Перезагружаем список захоронений после успешного удаления
                loadBurials(isMine = true)
            } else {
                Toast.makeText(context, "Ошибка удаления", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun openBurialDetails(burial: Burial , isMine: Boolean) {
        val intent = Intent(requireContext(), BurialDetailsActivity::class.java)
        intent.putExtra("burial_id", burial.id)
        intent.putExtra("isMine", isMine)
        intent.putExtra("guestId", getGuestIdFromPreferences())
        burial.photo?.let { photoBase64 ->
            val decodedBytes = Base64.decode(photoBase64, Base64.DEFAULT)
            val photoFile = File(requireContext().cacheDir, "burial_${burial.id}.jpg")
            photoFile.writeBytes(decodedBytes)
            intent.putExtra("burialPhotoPath", photoFile.absolutePath) // Передаем путь к файлу
        }
        deleteBurialLauncher.launch(intent)
    }

    private fun getGuestIdFromPreferences(): Long {
        val sharedPreferences = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return sharedPreferences.getLong(LoginActivity.KEY_USER_ID, -1)
    }

    // Обработка кнопок в Toolbar
    private fun setupToolbar(view: View) {
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        val btnMine = view.findViewById<MaterialButton>(R.id.btn_mine)
        val btnAll = view.findViewById<MaterialButton>(R.id.btn_all)

        // Устанавливаем начальные стили для кнопок
        btnMine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_300))  // Активный цвет
        btnAll.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_100))   // Неактивный цвет

        btnMine.setOnClickListener {
            loadBurials(isMine = true)  // Загружаем "Мои"
            // Изменяем цвет кнопок
            btnMine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_300))
            btnAll.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_100))  // Бледный цвет для "Все"
        }

        btnAll.setOnClickListener {
            loadBurials(isMine = false)  // Загружаем "Все"
            // Изменяем цвет кнопок
            btnMine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_100))  // Бледный цвет для "Мои"
            btnAll.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.purple_300))
        }

        (activity as AppCompatActivity).setSupportActionBar(toolbar)
    }

}

