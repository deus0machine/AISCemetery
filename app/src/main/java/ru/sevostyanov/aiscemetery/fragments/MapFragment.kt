package ru.sevostyanov.aiscemetery.fragments

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.launch
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.activities.EditMemorialActivity
import ru.sevostyanov.aiscemetery.activities.ViewMemorialActivity
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.repository.MemorialRepository
import ru.sevostyanov.aiscemetery.util.GlideHelper
import ru.sevostyanov.aiscemetery.util.NetworkUtil
import dagger.hilt.android.AndroidEntryPoint
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.view.GestureDetector
import android.view.MotionEvent

@AndroidEntryPoint
class MapFragment : Fragment(), MapObjectTapListener {
    private lateinit var mapView: MapView
    private lateinit var infoCardView: CardView
    private lateinit var memorialPhoto: ImageView
    private lateinit var memorialName: TextView
    private lateinit var memorialDates: TextView
    private lateinit var memorialAddress: TextView
    private lateinit var infoCardLayout: LinearLayout
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var legendCard: CardView
    private lateinit var legendButton: FloatingActionButton
    private lateinit var legendCloseButton: ImageButton
    private lateinit var infoCloseButton: ImageButton
    
    private val repository = MemorialRepository()
    private var clusterizedCollection: ClusterizedPlacemarkCollection? = null
    private var markerMemorialMap = mutableMapOf<PlacemarkMapObject, Memorial>()
    private var markerTypeMap = mutableMapOf<PlacemarkMapObject, Boolean>() // true для основного местоположения, false для захоронения
    private var allMemorials = listOf<Memorial>()
    
    private val DEFAULT_LATITUDE = 55.751574 // Москва
    private val DEFAULT_LONGITUDE = 37.573856
    private val DEFAULT_ZOOM = 9.0f
    
    // Создаем простую цветную иконку маркера программно с обводкой
    private fun createPlaceholderBitmap(size: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Заливка круга
        val paintFill = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Обводка круга
        val paintStroke = Paint().apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = size / 10f
            isAntiAlias = true
        }
        
        // Рисуем круг с заливкой и обводкой
        val radius = size / 2f
        val center = size / 2f
        canvas.drawCircle(center, center, radius - paintStroke.strokeWidth / 2, paintFill)
        canvas.drawCircle(center, center, radius - paintStroke.strokeWidth / 2, paintStroke)
        
        return bitmap
    }

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
        infoCardView = view.findViewById(R.id.info_card_view)
        memorialPhoto = view.findViewById(R.id.memorial_photo)
        memorialName = view.findViewById(R.id.memorial_name)
        memorialDates = view.findViewById(R.id.memorial_dates)
        memorialAddress = view.findViewById(R.id.memorial_address)
        infoCardLayout = view.findViewById(R.id.info_card_layout)
        searchEditText = view.findViewById(R.id.search_edit_text)
        searchButton = view.findViewById(R.id.search_button)
        legendCard = view.findViewById(R.id.legend_card)
        legendButton = view.findViewById(R.id.legend_button)
        legendCloseButton = view.findViewById(R.id.legend_close_button)
        infoCloseButton = view.findViewById(R.id.info_close_button)
        
        // Настраиваем поисковую строку
        setupSearch()
        
        // Настраиваем обработчики кнопок
        setupButtonHandlers()
        
        // Скрываем информационную карточку при запуске
        infoCardView.visibility = View.GONE
        
        println("DEBUG_MAP: Инициализируем clusterizedCollection")
        
        // Настраиваем кластеризацию маркеров
        clusterizedCollection = mapView.map.mapObjects.addClusterizedPlacemarkCollection { cluster ->
            // Настройка отображения кластера с программно созданной иконкой
            val clusterIcon = createPlaceholderBitmap(40, Color.GREEN)
            cluster.appearance.setIcon(
                ImageProvider.fromBitmap(clusterIcon),
                IconStyle().setScale(1.5f)
            )
            cluster.addClusterTapListener { selectedCluster ->
                // При нажатии на кластер приближаем карту к нему
                mapView.map.move(
                    CameraPosition(selectedCluster.appearance.geometry, 
                    mapView.map.cameraPosition.zoom + 2, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 1f),
                    null
                )
                true
            }
        }
        
        // Добавляем индикатор масштаба
        mapView.map.isZoomGesturesEnabled = true
        mapView.map.isRotateGesturesEnabled = true
        
        // Устанавливаем начальное положение карты
        println("DEBUG_MAP: Устанавливаем начальное положение карты")
        mapView.map.move(
            CameraPosition(
                Point(DEFAULT_LATITUDE, DEFAULT_LONGITUDE),
                DEFAULT_ZOOM,
                0.0f,
                0.0f
            ),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )

        // Инициализируем клики по карте для скрытия инфо-карточки
        mapView.map.addInputListener(object : com.yandex.mapkit.map.InputListener {
            override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
                hideMemorialInfo()
            }

            override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {
                // Ничего не делаем
            }
        })

        // Устанавливаем обработчик клика по карточке мемориала
        infoCardLayout.setOnClickListener {
            val memorial = infoCardView.tag as? Memorial
            memorial?.let {
                ViewMemorialActivity.start(requireActivity(), it)
            }
        }

        // Загружаем и отображаем мемориалы на карте
        println("DEBUG_MAP: Вызываем loadMemorials()")
        loadMemorials()
    }

    private fun loadMemorials() {
        if (!NetworkUtil.checkInternetAndShowMessage(requireContext())) {
            println("DEBUG_MAP: Нет интернет-соединения")
            return
        }
        
        println("DEBUG_MAP: Начинаем загрузку мемориалов")
        
        // Проверяем, правильно ли инициализирована Яндекс Карта
        if (!this::mapView.isInitialized) {
            println("DEBUG_MAP: Ошибка - mapView не инициализирован")
            return
        }
        
        if (clusterizedCollection == null) {
            println("DEBUG_MAP: Ошибка - clusterizedCollection не инициализирован")
            return
        }
        
        lifecycleScope.launch {
            try {
                val memorials = repository.getPublicMemorials()
                println("DEBUG_MAP: Получено мемориалов: ${memorials.size}")
                
                // Сохраняем мемориалы для поиска
                allMemorials = memorials
                
                // Выводим информацию о каждом мемориале
                memorials.forEach { memorial ->
                    println("DEBUG_MAP: Мемориал: ${memorial.id} - ${memorial.fio}")
                    println("DEBUG_MAP: mainLocation: ${memorial.mainLocation}")
                    println("DEBUG_MAP: burialLocation: ${memorial.burialLocation}")
                }
                
                addMemorialsToMap(memorials)
            } catch (e: Exception) {
                println("DEBUG_MAP: Ошибка при загрузке мемориалов: ${e.message}")
                e.printStackTrace()
                Toast.makeText(context, "Ошибка при загрузке мемориалов: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun addMemorialsToMap(memorials: List<Memorial>) {
        // Очищаем все существующие метки
        println("DEBUG_MAP: Очищаем существующие метки")
        clusterizedCollection?.clear()
        markerMemorialMap.clear()
        
        var markersAdded = 0
        
        memorials.forEach { memorial ->
            // Добавляем и основное местоположение, и место захоронения, если они указаны
            memorial.mainLocation?.let { location ->
                println("DEBUG_MAP: Добавляем маркер для mainLocation: ${location.latitude}, ${location.longitude}")
                addMemorialMarker(memorial, location.latitude, location.longitude, true)
                markersAdded++
            }
            
            memorial.burialLocation?.let { location ->
                // Если местоположения совпадают, не добавляем дублирующий маркер
                if (memorial.mainLocation == null || 
                    memorial.mainLocation?.latitude != location.latitude || 
                    memorial.mainLocation?.longitude != location.longitude) {
                    println("DEBUG_MAP: Добавляем маркер для burialLocation: ${location.latitude}, ${location.longitude}")
                    addMemorialMarker(memorial, location.latitude, location.longitude, false)
                    markersAdded++
                }
            }
        }
        
        println("DEBUG_MAP: Всего добавлено маркеров: $markersAdded")
        
        // Если нет ни одного маркера, показываем уведомление
        if (markersAdded == 0) {
            println("DEBUG_MAP: Нет мемориалов с указанным местоположением")
            Toast.makeText(context, "Нет мемориалов с указанным местоположением", Toast.LENGTH_LONG).show()
        } else {
            println("DEBUG_MAP: Применяем кластеризацию")
            // Применяем кластеризацию
            clusterizedCollection?.clusterPlacemarks(60.0, 15)
            
            // Центрируем карту на первом маркере с координатами
            memorials.firstOrNull { it.mainLocation != null }?.let { memorial ->
                memorial.mainLocation?.let { location ->
                    println("DEBUG_MAP: Центрируем карту на: ${location.latitude}, ${location.longitude}")
                    mapView.map.move(
                        CameraPosition(
                            Point(location.latitude, location.longitude),
                            DEFAULT_ZOOM,
                            0.0f,
                            0.0f
                        ),
                        Animation(Animation.Type.SMOOTH, 0.5f),
                        null
                    )
                }
            }
        }
    }
    
    private fun addMemorialMarker(memorial: Memorial, latitude: Double, longitude: Double, isMainLocation: Boolean) {
        val point = Point(latitude, longitude)
        println("DEBUG_MAP: Создаем маркер в точке: $latitude, $longitude, isMainLocation: $isMainLocation")
        
        try {
            // Создаем bitmap маркера программно
            val markerSize = if (isMainLocation) 40 else 30
            val markerColor = if (isMainLocation) Color.RED else Color.BLUE
            val markerBitmap = createPlaceholderBitmap(markerSize, markerColor)
            
            val marker = clusterizedCollection?.addPlacemark(point)?.apply {
                println("DEBUG_MAP: Устанавливаем программно созданную иконку")
                setIcon(ImageProvider.fromBitmap(markerBitmap))
                
                // Настройка внешнего вида маркера
                setIconStyle(IconStyle().apply {
                    scale = 1.0f
                    anchor = PointF(0.5f, 0.5f) // Центрируем маркер
                })
                
                userData = memorial
                addTapListener(this@MapFragment)
                println("DEBUG_MAP: Маркер успешно создан")
            }
            
            marker?.let { 
                markerMemorialMap[it] = memorial 
                markerTypeMap[it] = isMainLocation
                println("DEBUG_MAP: Маркер добавлен в map")
            } ?: println("DEBUG_MAP: Ошибка: marker is null")
        } catch (e: Exception) {
            println("DEBUG_MAP: Ошибка при создании маркера: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onMapObjectTap(mapObject: MapObject, point: Point): Boolean {
        println("DEBUG_MAP: onMapObjectTap вызван, mapObject: $mapObject")
        if (mapObject is PlacemarkMapObject) {
            println("DEBUG_MAP: Найден PlacemarkMapObject")
            val memorial = markerMemorialMap[mapObject]
            val isMainLocation = markerTypeMap[mapObject] ?: true
            
            memorial?.let {
                println("DEBUG_MAP: Memorial найден в markerMemorialMap: ${it.fio}, isMainLocation: $isMainLocation")
                showMemorialInfo(it, point, isMainLocation)
                return true
            } ?: println("DEBUG_MAP: Memorial не найден в markerMemorialMap")
        } else {
            println("DEBUG_MAP: Объект не является PlacemarkMapObject: ${mapObject.javaClass.simpleName}")
        }
        return false
    }
    
    private fun showMemorialInfo(memorial: Memorial, tappedPoint: Point, isMainLocation: Boolean) {
        println("DEBUG_MAP: showMemorialInfo для мемориала: ${memorial.fio}")
        
        // Заполняем данные
        memorialName.text = memorial.fio
        
        // Форматируем даты
        val birthYear = memorial.birthDate?.substring(0, 4) ?: "?"
        val deathYear = memorial.deathDate?.substring(0, 4) ?: "?"
        memorialDates.text = "$birthYear - $deathYear"
        
        // Определяем адрес из mainLocation или burialLocation
        val address = if (isMainLocation) {
            memorial.mainLocation?.address
        } else {
            memorial.burialLocation?.address
        } ?: memorial.mainLocation?.address ?: memorial.burialLocation?.address ?: "Адрес не указан"
        
        memorialAddress.text = address
        
        println("DEBUG_MAP: Загружаем фото: ${memorial.photoUrl}")
        // Загружаем фото
        GlideHelper.loadImage(
            requireContext(),
            memorial.photoUrl ?: "",
            memorialPhoto,
            R.drawable.placeholder_photo,
            R.drawable.placeholder_photo
        )
        
        // Сохраняем ссылку на мемориал
        infoCardView.tag = memorial
        
        // Показываем карточку
        infoCardView.visibility = View.VISIBLE
        println("DEBUG_MAP: Показываем infoCardView")
        
        // Всегда центрируем карту на точке, по которой был произведен тап
        println("DEBUG_MAP: Перемещаем камеру к точке нажатия: ${tappedPoint.latitude}, ${tappedPoint.longitude}")
        mapView.map.move(
            CameraPosition(
                tappedPoint,
                mapView.map.cameraPosition.zoom,
                0.0f,
                0.0f
            ),
            Animation(Animation.Type.SMOOTH, 0.3f),
            null
        )
    }

    override fun onStart() {
        super.onStart()
        println("DEBUG_MAP: onStart вызван")
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
        println("DEBUG_MAP: MapKit запущен")
    }

    override fun onStop() {
        println("DEBUG_MAP: onStop вызван")
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        println("DEBUG_MAP: MapKit остановлен")
        super.onStop()
    }
    
    override fun onResume() {
        super.onResume()
        // Обновляем данные при возвращении к фрагменту
        loadMemorials()
    }

    // Настраиваем поисковую строку
    private fun setupSearch() {
        // Обработчик кнопки поиска
        searchButton.setOnClickListener {
            performSearch()
        }
        
        // Обработчик нажатия Enter на клавиатуре
        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                performSearch()
                true
            } else {
                false
            }
        }
    }
    
    // Настраиваем обработчики кнопок
    private fun setupButtonHandlers() {
        // Кнопка показа легенды
        legendButton.setOnClickListener {
            showLegend()
        }
        
        // Кнопка закрытия легенды
        legendCloseButton.setOnClickListener {
            hideLegend()
        }
        
        // Кнопка закрытия информационной карточки
        infoCloseButton.setOnClickListener {
            hideMemorialInfo()
        }
        
        // Клик по фотографии для увеличенного просмотра
        memorialPhoto.setOnClickListener {
            showEnlargedPhoto()
        }
        
        // Настраиваем свайп для закрытия информационной карточки
        setupSwipeGesture()
    }
    
    // Показать легенду
    private fun showLegend() {
        legendCard.visibility = View.VISIBLE
        legendButton.visibility = View.GONE
    }
    
    // Скрыть легенду
    private fun hideLegend() {
        legendCard.visibility = View.GONE
        legendButton.visibility = View.VISIBLE
    }
    
    // Скрыть информационную карточку мемориала
    private fun hideMemorialInfo() {
        infoCardView.visibility = View.GONE
    }
    
    // Настраиваем жест свайпа для закрытия информационной карточки
    private fun setupSwipeGesture() {
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 != null && e2 != null) {
                    val diffY = e2.y - e1.y
                    val diffX = kotlin.math.abs(e2.x - e1.x)
                    
                    // Проверяем, что это свайп вниз и карточка видима
                    if (diffY > 100 && diffX < 200 && infoCardView.visibility == View.VISIBLE) {
                        hideMemorialInfo()
                        return true
                    }
                }
                return false
            }
        })
        
        infoCardView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false // Позволяем другим обработчикам получить событие
        }
    }
    
    // Выполняем поиск
    private fun performSearch() {
        val query = searchEditText.text.toString().trim().lowercase()
        
        // Скрываем клавиатуру
        val imm = requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
        
        if (query.isEmpty()) {
            Toast.makeText(context, "Введите текст для поиска", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Отладочный вывод для проверки данных
        println("DEBUG_SEARCH: Выполняем поиск по запросу: '$query'")
        println("DEBUG_SEARCH: Доступно мемориалов для поиска: ${allMemorials.size}")
        
        // Для тестирования выводим все мемориалы, доступные для поиска
        allMemorials.forEach { memorial ->
            println("DEBUG_SEARCH: Мемориал для поиска: ${memorial.id} - ${memorial.fio}, " +
                    "mainLocation: ${memorial.mainLocation}, " +
                    "burialLocation: ${memorial.burialLocation}")
        }
        
        // Сначала ищем мемориал по имени (с подробным логированием)
        val foundMemorial = allMemorials.find { 
            val containsName = it.fio.lowercase().contains(query)
            println("DEBUG_SEARCH: Проверяем '${it.fio}' - содержит '$query': $containsName")
            containsName
        }
        
        if (foundMemorial != null && (foundMemorial.mainLocation != null || foundMemorial.burialLocation != null)) {
            println("DEBUG_SEARCH: Найден мемориал по имени: ${foundMemorial.fio}")
            // Определяем местоположение для фокуса (предпочитаем основное)
            val location = foundMemorial.mainLocation ?: foundMemorial.burialLocation
            location?.let {
                val point = Point(it.latitude, it.longitude)
                println("DEBUG_SEARCH: Перемещаем карту на: ${it.latitude}, ${it.longitude}")
                // Центрируем карту и приближаем к найденному мемориалу
                moveCamera(point, 15f)
                // Показываем информацию о мемориале, используя точную точку маркера
                val isMainLocation = location == foundMemorial.mainLocation
                showMemorialInfo(foundMemorial, point, isMainLocation)
                return
            }
        } 
        
        println("DEBUG_SEARCH: Мемориал по имени не найден, ищем по адресу")
        
        // Если мемориал не найден, пробуем искать по адресу (с подробным логированием)
        val memorialWithAddress = allMemorials.find { memorial ->
            val mainLocationContains = memorial.mainLocation?.address?.lowercase()?.contains(query) == true
            val burialLocationContains = memorial.burialLocation?.address?.lowercase()?.contains(query) == true
            
            println("DEBUG_SEARCH: Проверяем адрес '${memorial.mainLocation?.address}' - содержит '$query': $mainLocationContains")
            println("DEBUG_SEARCH: Проверяем адрес '${memorial.burialLocation?.address}' - содержит '$query': $burialLocationContains")
            
            mainLocationContains || burialLocationContains
        }
        
        if (memorialWithAddress != null) {
            println("DEBUG_SEARCH: Найден мемориал по адресу: ${memorialWithAddress.fio}")
            // Определяем местоположение для фокуса
            val isMainLocation = memorialWithAddress.mainLocation?.address?.lowercase()?.contains(query) == true
            val location = if (isMainLocation) 
                memorialWithAddress.mainLocation 
            else 
                memorialWithAddress.burialLocation
                
            location?.let {
                val point = Point(it.latitude, it.longitude)
                println("DEBUG_SEARCH: Перемещаем карту на: ${it.latitude}, ${it.longitude}")
                moveCamera(point, 15f)
                val isMainLocation = location == memorialWithAddress.mainLocation
                showMemorialInfo(memorialWithAddress, point, isMainLocation)
                return
            }
        }
        
        println("DEBUG_SEARCH: Ничего не найдено по запросу: '$query'")
        Toast.makeText(context, "Ничего не найдено", Toast.LENGTH_SHORT).show()
    }
    
    // Перемещение камеры к определенной точке с заданным масштабом
    private fun moveCamera(point: Point, zoom: Float) {
        mapView.map.move(
            CameraPosition(
                point,
                zoom,
                0.0f,
                0.0f
            ),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )
    }

    // Показ увеличенного изображения в диалоге
    private fun showEnlargedPhoto() {
        val memorial = infoCardView.tag as? Memorial ?: return
        
        // Если фото отсутствует, показываем сообщение
        if (memorial.photoUrl.isNullOrBlank()) {
            Toast.makeText(context, "Фотография недоступна", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Создаем диалог
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_photo_viewer)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        
        val enlargedPhoto = dialog.findViewById<ru.sevostyanov.aiscemetery.views.ZoomableImageView>(R.id.enlarged_photo)
        val closeButton = dialog.findViewById<ImageView>(R.id.close_button)
        
        // Загружаем изображение
        GlideHelper.loadImage(
            requireContext(),
            memorial.photoUrl ?: "",
            enlargedPhoto,
            R.drawable.placeholder_photo,
            R.drawable.placeholder_photo
        )
        
        // Обработчик закрытия диалога
        closeButton.setOnClickListener { dialog.dismiss() }
        
        // Показываем диалог
        dialog.show()
    }
} 