package ru.sevostyanov.aiscemetery.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.LoginActivity
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.RetrofitClient
import ru.sevostyanov.aiscemetery.task.Task
import ru.sevostyanov.aiscemetery.task.TaskAdapter

class TaskFragment : Fragment() {

    private lateinit var apiService: RetrofitClient.ApiService
    private var selectedTask: Task? = null
    private var selectedImageUri: Uri? = null

    // ActivityResultLauncher for selecting an image
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Send the task to the server with the selected burial and image
            if (selectedTask != null) {
                sendTaskToServer(getGuestIdFromPreferences(), selectedTask!!, selectedImageUri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack() // Navigate back
        }

        setupApiService()
        loadTasks(view)

        return view
    }

    private fun setupApiService() {
        apiService = RetrofitClient.getApiService()
    }

    private fun loadTasks(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_services)
        recyclerView.layoutManager = LinearLayoutManager(context)

        lifecycleScope.launch {
            try {
                val tasks = apiService.getTasks()
                val guestId = getGuestIdFromPreferences()

                recyclerView.adapter = TaskAdapter(tasks) { task ->
                    selectedTask = task

                    // Check task type and act accordingly
                    if (task.name == "Добавить фотографию") {
                        openGalleryForImage() // Prompt user to select an image
                    } else {
                        sendTaskToServer(getGuestIdFromPreferences(), task, null) // Send task without image
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка загрузки задач: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGalleryForImage() {
        imagePickerLauncher.launch("image/*")
    }

    private fun sendTaskToServer(guestId: Long, task: Task, imageUri: Uri?) {
        lifecycleScope.launch {
            try {
                // Prepare request payload
                val requestBody = mutableMapOf(
                    "guestId" to guestId.toString(),
                    "taskId" to task.id.toString()
                )

                // Add image if available
                imageUri?.let {
                    val inputStream = requireContext().contentResolver.openInputStream(it)
                    val imageBytes = inputStream?.readBytes()
                    inputStream?.close()

                    requestBody["image"] = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                }

                // Send request to server
                val response = apiService.performTask(requestBody)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Задача выполнена успешно", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Ошибка выполнения задачи", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка отправки данных: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getGuestIdFromPreferences(): Long {
        val sharedPreferences = requireContext().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("guest_id", -1)
    }
}