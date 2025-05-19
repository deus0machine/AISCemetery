package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
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
    private var searchManager: SearchManager? = null
    private var searchSession: Session? = null
    private var selectedPoint: Point? = null
    private var selectedAddress: String? = null

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

        // Инициализация поиска
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)

        // Обработчик клика по карте
        mapView.mapWindow.map.addInputListener(object : com.yandex.mapkit.map.InputListener {
            override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
                selectedPoint = point
                // Здесь можно добавить обратный геокодинг для получения адреса
                updateSelectedLocation()
            }

            override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {
                // Не используется
            }
        })

        // Обработчик поиска
        searchButton.setOnClickListener {
            val searchText = searchEditText.text.toString()
            if (searchText.isNotEmpty()) {
                searchLocation(searchText)
            }
        }

        // Обработчик подтверждения выбора
        confirmButton.setOnClickListener {
            selectedPoint?.let { point ->
                val location = Location(
                    latitude = point.latitude,
                    longitude = point.longitude,
                    address = selectedAddress
                )
                // Возвращаем результат
                parentFragmentManager.setFragmentResult(
                    "location_picker_result",
                    Bundle().apply {
                        putParcelable("location", location)
                        putBoolean("is_main_location", arguments?.getBoolean("is_main_location", true) ?: true)
                    }
                )
                dismiss()
            }
        }
    }

    private fun searchLocation(query: String) {
        val searchOptions = SearchOptions().apply {
            resultPageSize = 1
        }
        // Создаем Geometry для поиска в пределах видимой области карты
        val visibleRegion = mapView.mapWindow.map.visibleRegion
        val boundingBox = BoundingBox(
            visibleRegion.bottomLeft,
            visibleRegion.topRight
        )
        val geometry = Geometry.fromBoundingBox(boundingBox)
        searchSession = searchManager?.submit(query, geometry, searchOptions, this)
    }

    private fun updateSelectedLocation() {
        selectedPoint?.let { point ->
            // Здесь можно добавить обратный геокодинг для получения адреса
            // Пока просто обновляем камеру
            mapView.mapWindow.map.move(
                CameraPosition(point, 15.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 1f),
                null
            )
        }
    }

    override fun onSearchResponse(response: com.yandex.mapkit.search.Response) {
        val results = response.collection.children
        if (results.isNotEmpty()) {
            val firstResult = results[0].obj
            val geometry = firstResult?.geometry?.firstOrNull()
            if (geometry?.point != null) {
                selectedPoint = geometry.point
                // Получаем адрес из топонима
                val toponymMetadata = firstResult.metadataContainer
                    .getItem<com.yandex.mapkit.search.ToponymObjectMetadata>(com.yandex.mapkit.search.ToponymObjectMetadata::class.java)
                
                selectedAddress = toponymMetadata?.address?.formattedAddress ?: firstResult.name
                
                selectedPoint?.let { updateSelectedLocation() }
            }
        }
    }


    override fun onSearchError(error: Error) {
        val errorMessage = when (error) {
            is RemoteError -> "Ошибка сервера"
            is NetworkError -> "Ошибка сети"
            else -> "Неизвестная ошибка"
        }
        // Показать сообщение об ошибке
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
} 