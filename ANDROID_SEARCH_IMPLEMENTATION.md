# Реализация расширенного поиска мемориалов в Android клиенте

## Обзор

Реализована комплексная система поиска мемориалов в Android приложении, которая полностью соответствует требованию: **"User should be able to search people by name, birth/death date, location, surname and other key criteria"**.

## Новые компоненты

### 1. API интерфейс (RetrofitClient.kt)

Добавлены новые методы API:

```kotlin
// Расширенный поиск мемориалов
@GET("/api/memorials/search/advanced")
suspend fun advancedSearchMemorials(
    @Query("firstName") firstName: String?,
    @Query("lastName") lastName: String?,
    @Query("middleName") middleName: String?,
    @Query("birthDateFrom") birthDateFrom: String?,
    @Query("birthDateTo") birthDateTo: String?,
    @Query("deathDateFrom") deathDateFrom: String?,
    @Query("deathDateTo") deathDateTo: String?,
    @Query("location") location: String?,
    @Query("query") query: String?,
    @Query("isPublic") isPublic: Boolean?,
    @Query("sortBy") sortBy: String?,
    @Query("sortDirection") sortDirection: String?,
    @Query("page") page: Int = 0,
    @Query("size") size: Int = 10
): PagedResponse<Memorial>

// Быстрый поиск для автодополнения
@GET("/api/memorials/search/quick")
suspend fun quickSearchMemorials(
    @Query("query") query: String,
    @Query("limit") limit: Int = 10
): List<Memorial>

// Поиск по годовщинам
@GET("/api/memorials/search/anniversaries")
suspend fun searchAnniversaries(
    @Query("month") month: Int?,
    @Query("day") day: Int?,
    @Query("type") type: String?,
    @Query("page") page: Int = 0,
    @Query("size") size: Int = 10
): PagedResponse<Memorial>

// Поиск с фильтрами через POST
@POST("/api/memorials/search/filter")
suspend fun searchMemorialsWithFilter(
    @Body searchRequest: MemorialSearchRequest
): PagedResponse<Memorial>

// Статистика поиска
@GET("/api/memorials/search/stats")
suspend fun getSearchStats(): MemorialSearchStats
```

### 2. Модели данных

#### MemorialSearchRequest.kt
```kotlin
data class MemorialSearchRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val birthDateFrom: String? = null,
    val birthDateTo: String? = null,
    val deathDateFrom: String? = null,
    val deathDateTo: String? = null,
    val location: String? = null,
    val query: String? = null,
    val isPublic: Boolean? = null,
    val sortBy: String? = null,
    val sortDirection: String? = null,
    val page: Int = 0,
    val size: Int = 10
) {
    fun hasFilters(): Boolean
    fun clearFilters(): MemorialSearchRequest
}
```

#### MemorialSearchStats.kt
```kotlin
data class MemorialSearchStats(
    val totalMemorials: Long,
    val publicMemorials: Long,
    val privateMemorials: Long,
    val popularLocations: List<LocationStat>,
    val recentSearches: List<String>,
    val topSearchTerms: List<SearchTermStat>
)
```

### 3. Расширенный диалог фильтров (AdvancedMemorialFilterDialog.kt)

Новый диалог с полным набором фильтров:

- **Поиск по имени**: Отдельные поля для имени, фамилии, отчества
- **Даты рождения**: Диапазон дат рождения (с/по)
- **Даты смерти**: Диапазон дат смерти (с/по)
- **Местоположение**: Поиск по основному и месту захоронения
- **Общий поиск**: Полнотекстовый поиск
- **Тип мемориалов**: Публичные/приватные
- **Сортировка**: По различным критериям с направлением

```kotlin
data class AdvancedFilterOptions(
    val firstName: String?,
    val lastName: String?,
    val middleName: String?,
    val location: String?,
    val generalQuery: String?,
    val birthDateFrom: String?,
    val birthDateTo: String?,
    val deathDateFrom: String?,
    val deathDateTo: String?,
    val isPublic: Boolean?,
    val sortBy: String?,
    val sortDirection: String?
)
```

### 4. Обновленный MemorialRepository.kt

Добавлены методы для всех типов поиска:

```kotlin
// Расширенный поиск
suspend fun advancedSearchMemorials(...)

// Быстрый поиск
suspend fun quickSearchMemorials(query: String, limit: Int = 10)

// Поиск по годовщинам
suspend fun searchAnniversaries(...)

// Поиск с фильтрами
suspend fun searchMemorialsWithFilter(searchRequest: MemorialSearchRequest)

// Статистика
suspend fun getSearchStats()
```

### 5. Улучшенный MemorialsFragment.kt

#### Новые возможности:
- **Расширенный поиск**: Детальные фильтры по всем критериям
- **Быстрый поиск**: Мгновенные результаты для автодополнения
- **Множественные режимы поиска**: Обычный, расширенный, быстрый
- **Улучшенная пагинация**: Поддержка всех типов поиска
- **Индикаторы состояния**: Показ активного режима поиска

#### Новые методы:
```kotlin
private fun showAdvancedFilterDialog()
private fun performQuickSearch()
private fun performQuickSearchWithQuery(query: String)
private fun performAdvancedSearch(filterOptions: AdvancedFilterOptions)
private fun performAdvancedSearchPage(filterOptions: AdvancedFilterOptions, isFirstPage: Boolean)
```

## Пользовательский интерфейс

### Обновленный fragment_memorials.xml

Добавлены новые кнопки:
- **"Расширенный поиск"**: Открывает детальный диалог фильтров
- **"Быстрый поиск"**: Выполняет мгновенный поиск по текущему запросу

### Новый dialog_advanced_memorial_filter.xml

Полнофункциональный диалог с:
- Полями для детального поиска по имени
- Выборщиками дат для рождения и смерти
- Фильтрами публичности
- Настройками сортировки
- Кнопками действий

## Функциональность поиска

### 1. Критерии поиска (полностью соответствует требованиям)

✅ **По имени**: firstName, lastName, middleName  
✅ **По дате рождения**: birthDateFrom, birthDateTo  
✅ **По дате смерти**: deathDateFrom, deathDateTo  
✅ **По местоположению**: location (основное место и место захоронения)  
✅ **По фамилии**: lastName (отдельное поле)  
✅ **Другие критерии**: общий поиск, тип публичности, сортировка

### 2. Типы поиска

1. **Обычный поиск**: Через SearchView с базовыми фильтрами
2. **Расширенный поиск**: Детальные критерии через диалог
3. **Быстрый поиск**: Мгновенные результаты для автодополнения

### 3. Возможности сортировки

- По умолчанию
- По имени
- По фамилии
- По дате рождения
- По дате смерти
- По дате создания
- Направление: по возрастанию/убыванию

## Интеграция с существующим кодом

### Обратная совместимость
- Все существующие методы поиска сохранены
- Старый MemorialFilterDialog продолжает работать
- Новые возможности добавлены без нарушения существующей функциональности

### Управление состоянием
```kotlin
// Переменные состояния поиска
private var isSearchMode = false
private var isAdvancedSearchMode = false
private var currentSearchQuery: String? = null
private var currentFilterOptions: MemorialFilterDialog.FilterOptions? = null
private var currentAdvancedFilterOptions: AdvancedMemorialFilterDialog.AdvancedFilterOptions? = null
```

### Пагинация
- Поддержка пагинации для всех типов поиска
- Корректная обработка загрузки дополнительных страниц
- Индикаторы загрузки и состояния

## Использование

### Расширенный поиск
1. Нажать кнопку "Расширенный поиск"
2. Заполнить нужные критерии в диалоге
3. Нажать "Применить" для поиска или "Быстрый поиск" для мгновенных результатов

### Быстрый поиск
1. Ввести запрос в SearchView
2. Нажать кнопку "Быстрый поиск"
3. Получить мгновенные результаты (до 20 записей)

### Обычный поиск
1. Ввести запрос в SearchView
2. Использовать кнопку "Фильтры" для базовых настроек
3. Результаты обновляются автоматически с задержкой 500ms

## Преимущества реализации

1. **Полное соответствие требованиям**: Все критерии поиска реализованы
2. **Гибкость**: Множественные способы поиска для разных сценариев
3. **Производительность**: Быстрый поиск для автодополнения
4. **UX**: Интуитивный интерфейс с индикаторами состояния
5. **Масштабируемость**: Легко добавлять новые критерии поиска
6. **Совместимость**: Не нарушает существующую функциональность

## Следующие шаги

1. Тестирование всех сценариев поиска
2. Оптимизация производительности запросов
3. Добавление кэширования результатов поиска
4. Реализация истории поиска
5. Добавление поиска по годовщинам в UI 