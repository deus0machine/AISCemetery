package ru.sevostyanov.aiscemetery.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.repository.MemorialRepository
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapFragment : Fragment() {
    private lateinit var mapView: MapView
    private val repository = MemorialRepository()
    private val DEFAULT_LATITUDE = 55.751574 // Москва
    private val DEFAULT_LONGITUDE = 37.573856
    private val DEFAULT_ZOOM = 11.0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        mapView = view.findViewById(R.id.map_view)
        
        // Устанавливаем начальное положение карты
        mapView.map.move(
            CameraPosition(
                Point(DEFAULT_LATITUDE, DEFAULT_LONGITUDE),
                DEFAULT_ZOOM,
                0.0f,
                0.0f
            ),
            Animation(Animation.Type.SMOOTH, 0.0f),
            null
        )

        // Загружаем и отображаем мемориалы на карте
        loadMemorials()
    }

    private fun loadMemorials() {
        lifecycleScope.launch {
            try {
                val memorials = repository.getPublicMemorials()
                addMemorialsToMap(memorials)
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка при загрузке мемориалов: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun addMemorialsToMap(memorials: List<Memorial>) {
        memorials.forEach { memorial ->
            memorial.burialLocation?.let { location ->
                val point = Point(location.latitude, location.longitude)
                val marker = mapView.map.mapObjects.addPlacemark(point).apply {
                    setIcon(ImageProvider.fromResource(requireContext(), R.drawable.ic_marker))
                    userData = memorial
                }
                
                marker.addTapListener { mapObject, _ ->
                    val clickedMemorial = mapObject.userData as Memorial
                    Toast.makeText(context, clickedMemorial.fio, Toast.LENGTH_SHORT).show()
                    true
                }
            }
        }
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