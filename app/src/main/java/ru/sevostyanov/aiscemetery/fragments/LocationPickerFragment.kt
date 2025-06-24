package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.runtime.Error
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.Location

class LocationPickerFragment : DialogFragment(), Session.SearchListener {
    private lateinit var mapView: MapView
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var confirmButton: Button
    private lateinit var selectedLocationText: TextView
    private var searchManager: SearchManager? = null
    private var searchSession: Session? = null
    private var selectedPoint: Point? = null
    private var selectedAddress: String? = null
    private var mapObjects: MapObjectCollection? = null
    private var mapInputListener: InputListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)
        searchEditText = view.findViewById(R.id.searchEditText)
        searchButton = view.findViewById(R.id.searchButton)
        confirmButton = view.findViewById(R.id.confirmButton)
        selectedLocationText = view.findViewById(R.id.selectedLocationText)
        
        // Get map objects collection for placing markers
        mapObjects = mapView.mapWindow.map.mapObjects.addCollection()

        // Initialize search
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)

        // Move to Moscow by default (or another central position)
        val moscowPoint = Point(55.753215, 37.622504) // Moscow coordinates
        moveCamera(moscowPoint, 10.0f)

        // Create the map input listener
        setupMapClickHandler()

        // Search button handler
        searchButton.setOnClickListener {
            val searchText = searchEditText.text.toString().trim()
            if (searchText.isNotEmpty()) {
                // Сбрасываем предыдущий выбор для нового поиска
                selectedPoint = null
                selectedAddress = null
                mapObjects?.clear()
                
                searchLocation(searchText)
                selectedLocationText.text = "Поиск..."
            } else {
                Toast.makeText(context, "Введите текст для поиска", Toast.LENGTH_SHORT).show()
            }
        }

        // Поддержка поиска по нажатию Enter
        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)) {
                searchButton.performClick()
                true
            } else {
                false
            }
        }

        // Confirm button handler
        confirmButton.setOnClickListener {
            selectedPoint?.let { point ->
                val location = Location(
                    latitude = point.latitude,
                    longitude = point.longitude,
                    address = selectedAddress
                )
                // Return result
                parentFragmentManager.setFragmentResult(
                    "location_picker_result",
                    Bundle().apply {
                        putParcelable("location", location)
                        putBoolean("is_main_location", arguments?.getBoolean("is_main_location", true) ?: true)
                    }
                )
                dismiss()
            } ?: run {
                Toast.makeText(context, "Выберите местоположение на карте", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMapClickHandler() {
        // Удаляем предыдущий слушатель, если он был
        if (mapInputListener != null) {
            try {
                mapView.mapWindow.map.removeInputListener(mapInputListener!!)
            } catch (e: Exception) {
                println("Ошибка при удалении предыдущего слушателя: ${e.message}")
            }
        }
        
        // Создаем новый слушатель
        val newInputListener = object : InputListener {
            override fun onMapTap(map: Map, point: Point) {
                println("Карта нажата в точке: ${point.latitude}, ${point.longitude}")
                selectedPoint = point
                performReverseGeocoding(point)
                updateSelectedLocation(null)
            }

            override fun onMapLongTap(map: Map, point: Point) {
                // Не используется
            }
        }
        
        // Сохраняем и добавляем новый слушатель
        mapInputListener = newInputListener
        mapView.mapWindow.map.addInputListener(newInputListener)
    }

    private fun searchLocation(query: String) {
        // Отменяем предыдущий поиск если он выполняется
        searchSession?.cancel()
        
        val searchOptions = SearchOptions().apply {
            resultPageSize = 10 // Увеличиваем количество результатов
            searchTypes = com.yandex.mapkit.search.SearchType.GEO.value // Только географические объекты
        }
        
        // Используем глобальный поиск без ограничения по видимой области
        // Создаем геометрию для всего мира
        val worldGeometry = Geometry.fromBoundingBox(
            BoundingBox(
                Point(-90.0, -180.0), // Южно-западный угол
                Point(90.0, 180.0)    // Северо-восточный угол
            )
        )
        searchSession = searchManager?.submit(query, worldGeometry, searchOptions, this)
    }

    private fun updateSelectedLocation(address: String?) {
        // Clear previous map objects
        mapObjects?.clear()
        
        selectedPoint?.let { point ->
            // Add marker at the selected point
            mapObjects?.addPlacemark(point)?.apply {
                setIcon(com.yandex.runtime.image.ImageProvider.fromResource(context, R.drawable.ic_location_pin))
            }
            
            // Move camera to the point
            moveCamera(point, 15.0f)
            
            // Update text display
            if (address != null) {
                selectedAddress = address
                selectedLocationText.text = address
            } else {
                selectedLocationText.text = "Загрузка адреса..."
            }
        }
    }
    
    private fun moveCamera(point: Point, zoom: Float) {
        mapView.mapWindow.map.move(
            CameraPosition(point, zoom, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 1f),
            null
        )
    }
    
    private fun performReverseGeocoding(point: Point) {
        val searchOptions = SearchOptions()
        searchManager?.submit(
            point,
            16, // zoom level
            searchOptions,
            this
        )
    }

    override fun onSearchResponse(response: com.yandex.mapkit.search.Response) {
        val results = response.collection.children
        if (results.isNotEmpty()) {
            // Если это результат поиска по тексту (не обратное геокодирование)
            if (selectedPoint == null) {
                // Показываем список результатов для выбора
                val geoObjects = results.mapNotNull { it.obj }
                showSearchResults(geoObjects)
            } else {
                // Это обратное геокодирование - обрабатываем как раньше
                val firstResult = results[0].obj
                val toponymMetadata = firstResult?.metadataContainer
                    ?.getItem<com.yandex.mapkit.search.ToponymObjectMetadata>(com.yandex.mapkit.search.ToponymObjectMetadata::class.java)
                
                val address = toponymMetadata?.address?.formattedAddress ?: firstResult?.name ?: "Адрес не найден"
                updateSelectedLocation(address)
            }
        } else {
            selectedLocationText.text = "Адрес не найден"
        }
    }
    
    private fun showSearchResults(results: List<com.yandex.mapkit.GeoObject>) {
        // Берем первый результат автоматически
        val firstResult = results[0]
        val point = firstResult.geometry?.firstOrNull()?.point
        
        if (point != null) {
            selectedPoint = point
            
            // Получаем адрес
            val toponymMetadata = firstResult.metadataContainer
                ?.getItem<com.yandex.mapkit.search.ToponymObjectMetadata>(com.yandex.mapkit.search.ToponymObjectMetadata::class.java)
            
            val address = toponymMetadata?.address?.formattedAddress ?: firstResult.name ?: "Адрес не найден"
            
            // Показываем информацию о результатах поиска
            val resultInfo = if (results.size > 1) {
                "$address\n(найдено ${results.size} результатов)"
            } else {
                address
            }
            
            updateSelectedLocation(resultInfo)
            
            // Логируем все найденные результаты для отладки
            results.forEachIndexed { index, geoObject ->
                val resultPoint = geoObject.geometry?.firstOrNull()?.point
                val resultMetadata = geoObject.metadataContainer
                    ?.getItem<com.yandex.mapkit.search.ToponymObjectMetadata>(com.yandex.mapkit.search.ToponymObjectMetadata::class.java)
                val resultAddress = resultMetadata?.address?.formattedAddress ?: geoObject.name ?: "Неизвестно"
                
                println("Результат поиска $index: $resultAddress (${resultPoint?.latitude}, ${resultPoint?.longitude})")
            }
            
        } else {
            selectedLocationText.text = "Не удалось получить координаты"
        }
    }

    override fun onSearchError(error: Error) {
        val errorMessage = when (error) {
            is RemoteError -> "Ошибка сервера поиска"
            is NetworkError -> "Проверьте подключение к интернету"
            else -> "Ошибка поиска: ${error.javaClass.simpleName}"
        }
        selectedLocationText.text = "Ошибка: $errorMessage"
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        
        // Логируем ошибку для отладки
        println("Ошибка поиска: $error")
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
        
        // Ensure map click handler is set up properly when fragment starts
        setupMapClickHandler()
    }

    override fun onResume() {
        super.onResume()
        // Check and reset map click handler when fragment resumes
        setupMapClickHandler()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
} 