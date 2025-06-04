package ru.sevostyanov.aiscemetery.ui.genealogy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.MemorialRelation
import ru.sevostyanov.aiscemetery.models.RelationType
import kotlin.math.*

// Класс для хранения позиции узла
data class NodePosition(
    val memorial: Memorial,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val generation: Int
)

// Класс для хранения связи между узлами
data class TreeConnection(
    val from: NodePosition,
    val to: NodePosition,
    val relationType: RelationType,
    val color: Color,
    val strokeWidth: Float = 3f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenealogyTreeScreen(
    memorials: List<Memorial>,
    relations: List<MemorialRelation>
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedNode by remember { mutableStateOf<Memorial?>(null) }

    if (memorials.isEmpty()) {
        EmptyTreeMessage()
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Заголовок с управлением
        TreeHeader(
            onZoomIn = { 
                scale = (scale * 1.2f).coerceAtMost(4f)
            },
            onZoomOut = { 
                scale = (scale / 1.2f).coerceAtLeast(0.2f)
            },
            onResetView = {
                scale = 1f
                offset = Offset.Zero
            },
            memberCount = memorials.size,
            relationCount = relations.size
        )

        // Основная область с деревом
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp)
        ) {
            val canvasWidth = maxWidth
            val canvasHeight = maxHeight

            GenealogyTreeCanvas(
                memorials = memorials,
                relations = relations,
                scale = scale,
                offset = offset,
                onTransform = { newScale, newOffset ->
                    scale = newScale
                    offset = newOffset
                },
                onNodeClick = { memorial ->
                    selectedNode = memorial
                },
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight
            )
        }

        // Легенда
        TreeLegend(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )

        // Информация о выбранном узле
        selectedNode?.let { memorial ->
            SelectedNodeInfo(
                memorial = memorial,
                onDismiss = { selectedNode = null }
            )
        }
    }
}

@Composable
private fun EmptyTreeMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Генеалогическое дерево пусто",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Добавьте мемориалы и связи для создания дерева",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TreeHeader(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetView: () -> Unit,
    memberCount: Int,
    relationCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Генеалогическое дерево",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$memberCount чел. • $relationCount связей",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onZoomOut) {
                    Icon(Icons.Default.Close, contentDescription = "Уменьшить")
                }
                IconButton(onClick = onResetView) {
                    Icon(Icons.Default.Refresh, contentDescription = "Сбросить вид")
                }
                IconButton(onClick = onZoomIn) {
                    Icon(Icons.Default.Add, contentDescription = "Увеличить")
                }
            }
        }
    }
}

@Composable
private fun TreeLegend(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Типы связей",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            LegendItem(
                color = Color(0xFF4FC3F7),
                text = "Родитель → Ребенок",
                icon = Icons.Default.KeyboardArrowDown
            )
            LegendItem(
                color = Color(0xFFFF6B9D),
                text = "Супруги ♥",
                icon = Icons.Default.Favorite
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun GenealogyTreeCanvas(
    memorials: List<Memorial>,
    relations: List<MemorialRelation>,
    scale: Float,
    offset: Offset,
    onTransform: (Float, Offset) -> Unit,
    onNodeClick: (Memorial) -> Unit,
    canvasWidth: Dp,
    canvasHeight: Dp
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val nodePositions = remember(memorials, relations) {
        calculateNodePositions(memorials, relations, density)
    }

    val connections = remember(relations, nodePositions) {
        calculateTreeConnections(relations, nodePositions)
    }

    // Use rememberUpdatedState to ensure we always get the current values
    val currentOffset by rememberUpdatedState(offset)
    val currentScale by rememberUpdatedState(scale)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    val newOffset = currentOffset + pan
                    val newScale = (currentScale * zoom).coerceIn(0.2f, 4f)
                    onTransform(newScale, newOffset)
                }
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawTreeConnections(connections, scale, offset)
            drawTreeNodes(nodePositions, scale, offset, onNodeClick, textMeasurer)
        }
    }
}

private fun calculateNodePositions(
    memorials: List<Memorial>,
    relations: List<MemorialRelation>,
    density: androidx.compose.ui.unit.Density
): List<NodePosition> {
    if (memorials.isEmpty()) return emptyList()

    // Создаем граф связей
    val childrenMap = mutableMapOf<Long, MutableSet<Long>>()
    val parentsMap = mutableMapOf<Long, MutableSet<Long>>()
    val spousesMap = mutableMapOf<Long, Long>()
    val memorialMap = memorials.associateBy { it.id!! }

    relations.forEach { relation ->
        val sourceId = relation.sourceMemorial.id ?: return@forEach
        val targetId = relation.targetMemorial.id ?: return@forEach

        when (relation.relationType) {
            RelationType.PARENT -> {
                childrenMap.getOrPut(sourceId) { mutableSetOf() }.add(targetId)
                parentsMap.getOrPut(targetId) { mutableSetOf() }.add(sourceId)
            }
            RelationType.CHILD -> {
                childrenMap.getOrPut(targetId) { mutableSetOf() }.add(sourceId)
                parentsMap.getOrPut(sourceId) { mutableSetOf() }.add(targetId)
            }
            RelationType.SPOUSE -> {
                spousesMap[sourceId] = targetId
                spousesMap[targetId] = sourceId
            }
            RelationType.PLACEHOLDER -> {
                // Игнорируем PLACEHOLDER связи при построении семейного дерева
                // Но мемориалы из этих связей уже включены в список memorials
            }
            else -> {}
        }
    }

    // Находим корни (узлы без родителей) - учитываваем супружеские связи
    val potentialRoots = memorials.filter { memorial ->
        memorial.id?.let { id -> parentsMap[id].isNullOrEmpty() } ?: false
    }
    
    // Исключаем людей без родителей, если их супруг уже в семейном дереве
    val actualRoots = potentialRoots.filter { memorial ->
        val id = memorial.id ?: return@filter true
        val spouseId = spousesMap[id]
        
        if (spouseId != null) {
            // Если супруг имеет родителей (не корень), то этот человек тоже не корень
            val spouseHasParents = parentsMap[spouseId]?.isNotEmpty() ?: false
            !spouseHasParents // Только если супруг тоже корень, оставляем как корень
        } else {
            true // Нет супруга - остается корнем
        }
    }
    
    // Группируем корни по супружеским парам
    val processedRoots = mutableSetOf<Long>()
    val rootGroups = mutableListOf<List<Memorial>>()
    
    actualRoots.forEach { memorial ->
        val id = memorial.id ?: return@forEach
        if (id in processedRoots) return@forEach
        
        val spouseId = spousesMap[id]
        val spouse = spouseId?.let { memorialMap[it] }
        
        if (spouse != null && spouse in actualRoots) {
            // Супружеская пара - оба корни
            rootGroups.add(listOf(memorial, spouse))
            processedRoots.add(id)
            processedRoots.add(spouseId)
        } else {
            // Одинокий корень
            rootGroups.add(listOf(memorial))
            processedRoots.add(id)
        }
    }
    
    // Рассчитываем уровни поколений
    val generationMap = mutableMapOf<Long, Int>()
    val queue = ArrayDeque<Pair<Long, Int>>()
    
    // От каждой группы корней запускаем обход
    rootGroups.forEach { rootGroup ->
        rootGroup.forEach { root ->
            root.id?.let {
                queue.add(it to 0)
                generationMap[it] = 0
            }
        }
    }

    while (queue.isNotEmpty()) {
        val (currentId, level) = queue.removeFirst()
        val nextLevel = level + 1

        childrenMap[currentId]?.forEach { childId ->
            if (!generationMap.containsKey(childId)) {
                generationMap[childId] = nextLevel
                queue.add(childId to nextLevel)
            }
        }
    }
    
    // ДОБАВЛЯЕМ СУПРУГОВ, ИСКЛЮЧЕННЫХ ИЗ КОРНЕЙ
    // Если супруг не корень, но его партнер уже в дереве - добавляем его на тот же уровень
    memorials.forEach { memorial ->
        val id = memorial.id ?: return@forEach
        
        if (!generationMap.containsKey(id)) {
            // Этот человек не в дереве, проверяем супруга
            val spouseId = spousesMap[id]
            val spouseLevel = spouseId?.let { generationMap[it] }
            
            if (spouseLevel != null) {
                // Супруг в дереве - добавляем на тот же уровень
                generationMap[id] = spouseLevel
            }
        }
    }
    
    // КОРРЕКТИРОВКА УРОВНЕЙ ДЛЯ СУПРУГОВ (исправляем позиционирование)
    // Супруги должны быть на одном уровне, даже если у одного есть родители, а у другого нет
    spousesMap.forEach { (person1Id, person2Id) ->
        val level1 = generationMap[person1Id]
        val level2 = generationMap[person2Id]
        
        if (level1 != null && level2 != null && level1 != level2) {
            // Ставим супругов на более глубокий уровень (тот, кто имеет родителей)
            val targetLevel = maxOf(level1, level2)
            generationMap[person1Id] = targetLevel
            generationMap[person2Id] = targetLevel
        } else if (level1 != null && level2 == null) {
            // Если один супруг имеет уровень, а другой нет - ставим на тот же уровень
            generationMap[person2Id] = level1
        } else if (level2 != null && level1 == null) {
            // Аналогично для второго супруга
            generationMap[person1Id] = level2
        }
    }

    // Группируем по поколениям
    val levels = generationMap.entries
        .groupBy({ it.value }, { it.key })
        .toSortedMap()

    // Рассчитываем позиции
    val positions = mutableMapOf<Long, NodePosition>()
    val levelY = mutableMapOf<Int, Float>()
    val maxLevel = levels.keys.maxOrNull() ?: 0
    val baseY = 100f // Начинаем выше
    val nodeWidth = with(density) { 280.dp.toPx() }
    val nodeHeight = with(density) { 120.dp.toPx() }

    // ДИНАМИЧЕСКИЕ РАССТОЯНИЯ для красоты
    val generationSpacing = when {
        maxLevel <= 2 -> 550f  // Для небольших деревьев - больше пространства для разветвления
        maxLevel <= 4 -> 480f  // Средние деревья - достаточно места
        else -> 420f           // Большие деревья - компактнее, но с запасом
    }

    // КРАСИВОЕ ВЕРТИКАЛЬНОЕ РАЗМЕЩЕНИЕ поколений
    for (level in 0..maxLevel) {
        levelY[level] = baseY + level * generationSpacing
    }

    // КЛАССИЧЕСКИЙ АЛГОРИТМ размещения дерева
    for (level in maxLevel downTo 0) {
        val levelNodes = levels[level]?.mapNotNull { memorialMap[it] } ?: continue
        val familyGroups = mutableListOf<List<Memorial>>()
        val processed = mutableSetOf<Long>()

        // Группируем по семьям (супруги вместе)
        for (memorial in levelNodes) {
            val id = memorial.id ?: continue
            if (id in processed) continue

            val spouseId = spousesMap[id]
            val spouse = spouseId?.let { memorialMap[it] }

            if (spouse != null) {
                familyGroups.add(listOf(memorial, spouse))
                processed.add(id)
                processed.add(spouseId)
            } else {
                familyGroups.add(listOf(memorial))
                processed.add(id)
            }
        }

        // ЕСТЕСТВЕННОЕ РАЗМЕЩЕНИЕ семей
        if (familyGroups.isNotEmpty()) {
            val spouseSpacing = 320f // Увеличиваем расстояние между супругами (было 80f)
            
            // Адаптивное расстояние между семьями
            val familySpacing = when {
                familyGroups.size == 1 -> 0f
                familyGroups.size <= 3 -> 500f  // Просторно для малых семей
                familyGroups.size <= 5 -> 400f  // Умеренно
                else -> 320f                    // Компактно для больших
            }
            
            // Рассчитываем центрированное размещение
            val totalFamilyWidth = familyGroups.sumOf { group ->
                if (group.size == 1) nodeWidth.toDouble()
                else (nodeWidth * 2 + spouseSpacing).toDouble()
            }.toFloat()
            
            val totalSpacingWidth = if (familyGroups.size > 1) {
                (familyGroups.size - 1) * familySpacing
            } else 0f
            
            val levelWidth = totalFamilyWidth + totalSpacingWidth
            val startX = -levelWidth / 2
            
            // Размещаем семьи с естественными интервалами
            var currentX = startX
            familyGroups.forEach { group ->
                // Небольшие случайные сдвиги для натуральности
                val naturalOffset = when (group.size) {
                    1 -> if (familyGroups.size > 3) (Math.random() * 40 - 20).toFloat() else 0f
                    else -> 0f
                }
                
                val familyWidth = if (group.size == 1) nodeWidth else nodeWidth * 2 + spouseSpacing
                val familyStartX = currentX + naturalOffset
                
                group.forEachIndexed { memberIndex, member ->
                    val x = if (group.size == 1) {
                        familyStartX
                    } else {
                        // Супруги размещаются рядом
                        familyStartX + memberIndex * (nodeWidth + spouseSpacing)
                    }

                    positions[member.id!!] = NodePosition(
                        memorial = member,
                        x = x,
                        y = levelY[level] ?: baseY,
                        width = nodeWidth,
                        height = nodeHeight,
                        generation = level
                    )
                }
                
                // Переходим к следующей семье
                currentX += familyWidth + familySpacing
            }
        }
    }

    // ЦЕНТРИРОВАНИЕ ДЕТЕЙ ПОД РОДИТЕЛЯМИ (исправляет позиционирование)
    val familyHierarchy = mutableMapOf<Set<Long>, MutableList<Long>>()
    
    // Собираем иерархию семей
    relations.forEach { relation ->
        if (relation.relationType == RelationType.PARENT) {
            val parentId = relation.sourceMemorial.id ?: return@forEach
            val childId = relation.targetMemorial.id ?: return@forEach
            
            // Находим супруга родителя
            val spouseId = spousesMap[parentId]
            val parentKey = if (spouseId != null) {
                setOf(parentId, spouseId)
            } else {
                setOf(parentId)
            }
            
            familyHierarchy.getOrPut(parentKey) { mutableListOf() }.add(childId)
        }
    }
    
    // Центрируем детей под родителями
    familyHierarchy.forEach { (parentIds, childIds) ->
        val parentPositions = parentIds.mapNotNull { positions[it] }
        val childPositions = childIds.mapNotNull { positions[it] }
        
        if (parentPositions.isNotEmpty() && childPositions.isNotEmpty()) {
            // Находим центр родителей
            val parentCenterX = if (parentPositions.size == 2) {
                // Центр между супругами
                val leftParent = parentPositions.minByOrNull { it.x }!!
                val rightParent = parentPositions.maxByOrNull { it.x }!!
                (leftParent.x + leftParent.width + rightParent.x) / 2f
            } else {
                // Центр одинокого родителя
                val parent = parentPositions[0]
                parent.x + parent.width / 2f
            }
            
            // Центрируем детей под родителями
            if (childPositions.size == 1) {
                // Один ребенок - строго по центру
                val child = childPositions[0]
                positions[childIds[0]] = child.copy(
                    x = parentCenterX - child.width / 2f
                )
            } else {
                // Несколько детей - равномерно распределяем
                val totalChildrenWidth = childPositions.sumOf { it.width.toDouble() }.toFloat()
                val spacingBetweenChildren = 320f // Увеличиваем расстояние между детьми (было 80f)
                val totalSpacing = (childPositions.size - 1) * spacingBetweenChildren
                val totalWidth = totalChildrenWidth + totalSpacing
                val startX = parentCenterX - totalWidth / 2f
                
                var currentX = startX
                childIds.forEachIndexed { index, childId ->
                    val child = positions[childId]!!
                    positions[childId] = child.copy(x = currentX)
                    currentX += child.width + spacingBetweenChildren
                }
            }
        }
    }

    // МЯГКАЯ КОРРЕКТИРОВКА без жестких правил
    val finalPositions = positions.toMutableMap()
    
    // Только критические проверки
    val positionsList = positions.values.toList()
    for (i in positionsList.indices) {
        for (j in i + 1 until positionsList.size) {
            val pos1 = positionsList[i]
            val pos2 = positionsList[j]
            
            // Проверяем только очевидные проблемы
            if (pos1.generation == pos2.generation) {
                val distance = abs(pos1.x - pos2.x)
                val criticalDistance = pos1.width + 30f // Минимум для читаемости
                
                if (distance < criticalDistance) {
                    // Мягкая корректировка
                    val adjustment = (criticalDistance - distance) / 2 + 20f
                    if (pos1.x < pos2.x) {
                        finalPositions[pos1.memorial.id!!] = pos1.copy(x = pos1.x - adjustment)
                        finalPositions[pos2.memorial.id!!] = pos2.copy(x = pos2.x + adjustment)
                    }
                }
            }
        }
    }

    return finalPositions.values.toList()
}

private fun calculateTreeConnections(
    relations: List<MemorialRelation>,
    nodePositions: List<NodePosition>
): List<TreeConnection> {
    val positionMap = nodePositions.associateBy { it.memorial.id }
    val connections = mutableListOf<TreeConnection>()
    
    // Создаем карту супружеских связей
    val spouseMap = mutableMapOf<Long, Long>()
    relations.forEach { relation ->
        if (relation.relationType == RelationType.SPOUSE) {
            val sourceId = relation.sourceMemorial.id
            val targetId = relation.targetMemorial.id
            if (sourceId != null && targetId != null) {
                spouseMap[sourceId] = targetId
                spouseMap[targetId] = sourceId
            }
        }
    }

    // Группируем детей по их родителям
    val childrenByParents = mutableMapOf<Set<Long>, MutableList<Long>>()
    
    relations.forEach { relation ->
        when (relation.relationType) {
            RelationType.PARENT -> {
                val parentId = relation.sourceMemorial.id ?: return@forEach
                val childId = relation.targetMemorial.id ?: return@forEach
                
                // Находим супруга родителя (если есть)
                val spouseId = spouseMap[parentId]
                val parentKey = if (spouseId != null) {
                    setOf(parentId, spouseId)
                } else {
                    setOf(parentId)
                }
                
                childrenByParents.getOrPut(parentKey) { mutableListOf() }.add(childId)
            }
            RelationType.CHILD -> {
                val childId = relation.sourceMemorial.id ?: return@forEach
                val parentId = relation.targetMemorial.id ?: return@forEach
                
                // Находим супруга родителя (если есть)
                val spouseId = spouseMap[parentId]
                val parentKey = if (spouseId != null) {
                    setOf(parentId, spouseId)
                } else {
                    setOf(parentId)
                }
                
                childrenByParents.getOrPut(parentKey) { mutableListOf() }.add(childId)
            }
            RelationType.PLACEHOLDER -> {
                // Игнорируем PLACEHOLDER связи при построении семейного дерева
                // Но мемориалы из этих связей уже включены в список memorials
            }
            else -> {}
        }
    }

    // Добавляем супружеские связи
    relations.forEach { relation ->
        if (relation.relationType == RelationType.SPOUSE) {
            val sourcePos = positionMap[relation.sourceMemorial.id]
            val targetPos = positionMap[relation.targetMemorial.id]

            if (sourcePos != null && targetPos != null) {
                // Определяем, какая карточка слева, какая справа
                val leftCard = if (sourcePos.x < targetPos.x) sourcePos else targetPos
                val rightCard = if (sourcePos.x < targetPos.x) targetPos else sourcePos
                
                // Рассчитываем точки подключения к краям карточек
                val leftConnectionX = leftCard.x + leftCard.width // Правый край левой карточки
                val rightConnectionX = rightCard.x // Левый край правой карточки
                val connectionY = leftCard.y + leftCard.height * 0.25f // Чуть выше центра карточки
                
                connections.add(TreeConnection(
                    from = NodePosition(
                        memorial = leftCard.memorial,
                        x = leftConnectionX,
                        y = connectionY,
                        width = 0f,
                        height = 0f,
                        generation = leftCard.generation
                    ),
                    to = NodePosition(
                        memorial = rightCard.memorial,
                        x = rightConnectionX,
                        y = connectionY,
                        width = 0f,
                        height = 0f,
                        generation = rightCard.generation
                    ),
                    relationType = relation.relationType,
                    color = Color(0xFFFF6B9D)
                ))
            }
        }
    }

    // Добавляем родительские связи от точки соединения
    childrenByParents.forEach { (parentIds, childIds) ->
        val parentPositions = parentIds.mapNotNull { positionMap[it] }
        
        if (parentPositions.isNotEmpty() && childIds.isNotEmpty()) {
            // Рассчитываем ПРАВИЛЬНУЮ точку соединения родителей
            val unionPoint = if (parentPositions.size == 2) {
                // Точка между супругами - от центра их супружеской связи
                val parent1 = parentPositions[0]
                val parent2 = parentPositions[1]
                
                // Находим левую и правую карточки
                val leftParent = if (parent1.x < parent2.x) parent1 else parent2
                val rightParent = if (parent1.x < parent2.x) parent2 else parent1
                
                // Центр супружеской линии (между краями карточек)
                val leftEdge = leftParent.x + leftParent.width
                val rightEdge = rightParent.x
                val spouseLineCenterX = (leftEdge + rightEdge) / 2f
                val spouseLineY = leftParent.y + leftParent.height * 0.25f
                
                NodePosition(
                    memorial = parent1.memorial,
                    x = spouseLineCenterX,
                    y = spouseLineY,
                    width = 0f,
                    height = 0f,
                    generation = parent1.generation
                )
            } else {
                // Единственный родитель - от нижнего края карточки
                val parent = parentPositions[0]
                NodePosition(
                    memorial = parent.memorial,
                    x = parent.x + parent.width / 2,
                    y = parent.y + parent.height, // От нижнего края
                    width = 0f,
                    height = 0f,
                    generation = parent.generation
                )
            }
            
            // СИСТЕМА ДЕРЕВА: главный ствол с поочередными ответвлениями
            val childPositions = childIds.mapNotNull { positionMap[it] }
            if (childPositions.isNotEmpty()) {
                if (childPositions.size == 1) {
                    // ОДИНОЧНЫЙ РЕБЕНОК: прямая вертикальная линия
                    val child = childPositions[0]
                    val childCenterX = child.x + child.width / 2
                    connections.add(TreeConnection(
                        from = NodePosition(
                            memorial = unionPoint.memorial,
                            x = unionPoint.x,
                            y = unionPoint.y + 50f,
                            width = 0f,
                            height = 0f,
                            generation = unionPoint.generation
                        ),
                        to = NodePosition(
                            memorial = child.memorial,
                            x = childCenterX,
                            y = child.y,
                            width = 0f,
                            height = 0f,
                            generation = child.generation
                        ),
                        relationType = RelationType.PARENT,
                        color = Color(0xFF4FC3F7)
                    ))
                } else {
                    // НЕСКОЛЬКО ДЕТЕЙ: классическая T-образная структура дерева
                    
                    // 1. Рассчитываем границы детей для горизонтальной линии
                    val leftmostChild = childPositions.minByOrNull { it.x + it.width / 2 }!!
                    val rightmostChild = childPositions.maxByOrNull { it.x + it.width / 2 }!!
                    val leftChildX = leftmostChild.x + leftmostChild.width / 2
                    val rightChildX = rightmostChild.x + rightmostChild.width / 2
                    
                    // 2. Рассчитываем уровень разветвления
                    val minChildY = childPositions.minByOrNull { it.y }?.y ?: 0f
                    val trunkStartY = unionPoint.y + 50f
                    val branchingY = trunkStartY + (minChildY - trunkStartY) * 0.6f
                    
                    // 3. Главная вертикальная линия (ствол) от родителей до разветвления
                    connections.add(TreeConnection(
                        from = NodePosition(
                            memorial = unionPoint.memorial,
                            x = unionPoint.x,
                            y = trunkStartY,
                            width = 0f,
                            height = 0f,
                            generation = unionPoint.generation
                        ),
                        to = NodePosition(
                            memorial = unionPoint.memorial,
                            x = unionPoint.x,
                            y = branchingY,
                            width = 0f,
                            height = 0f,
                            generation = unionPoint.generation
                        ),
                        relationType = RelationType.PARENT,
                        color = Color(0xFF1976D2)
                    ))
                    
                    // 4. Горизонтальная линия разветвления (от левого до правого ребенка)
                    connections.add(TreeConnection(
                        from = NodePosition(
                            memorial = unionPoint.memorial,
                            x = leftChildX,
                            y = branchingY,
                            width = 0f,
                            height = 0f,
                            generation = unionPoint.generation
                        ),
                        to = NodePosition(
                            memorial = unionPoint.memorial,
                            x = rightChildX,
                            y = branchingY,
                            width = 0f,
                            height = 0f,
                            generation = unionPoint.generation
                        ),
                        relationType = RelationType.PARENT,
                        color = Color(0xFF4FC3F7)
                    ))
                    
                    // 5. Вертикальные линии от разветвления к каждому ребенку
                    childPositions.forEach { child ->
                        val childCenterX = child.x + child.width / 2
                        connections.add(TreeConnection(
                            from = NodePosition(
                                memorial = unionPoint.memorial,
                                x = childCenterX,
                                y = branchingY,
                                width = 0f,
                                height = 0f,
                                generation = unionPoint.generation
                            ),
                            to = NodePosition(
                                memorial = child.memorial,
                                x = childCenterX,
                                y = child.y,
                                width = 0f,
                                height = 0f,
                                generation = child.generation
                            ),
                            relationType = RelationType.PARENT,
                            color = Color(0xFF4FC3F7)
                        ))
                    }
                }
            }
        }
    }

    return connections
}

private fun DrawScope.drawTreeConnections(
    connections: List<TreeConnection>,
    scale: Float,
    offset: Offset
) {
    val centerX = size.width / 2
    
    // Рассчитываем маршруты для всех связей
    val connectionRoutes = mutableMapOf<TreeConnection, List<Offset>>()
    
    // Сначала рассчитываем маршруты для всех связей
    connections.forEach { connection ->
        // ИСПОЛЬЗУЕМ УЖЕ РАССЧИТАННЫЕ координаты из connection
        val fromX = connection.from.x * scale + offset.x + centerX
        val fromY = connection.from.y * scale + offset.y
        
        val toX = connection.to.x * scale + offset.x + centerX  
        val toY = connection.to.y * scale + offset.y
        
        when (connection.relationType) {
            RelationType.SPOUSE -> {
                // Для супругов - используем Y из connection.from (уже рассчитанный)
                val startPoint = Offset(fromX, fromY)
                val endPoint = Offset(toX, fromY) // Та же высота
                connectionRoutes[connection] = listOf(startPoint, endPoint)
            }
            
            else -> {
                // Для родитель-ребенок - используем прямые линии по рассчитанным точкам
                val startPoint = Offset(fromX, fromY)
                val endPoint = Offset(toX, toY)
                connectionRoutes[connection] = listOf(startPoint, endPoint)
            }
        }
    }

    // Теперь рисуем все маршруты В ПРАВИЛЬНОМ ПОРЯДКЕ
    // 1. Сначала родительские связи (на заднем плане)
    connectionRoutes.filter { (connection, _) -> 
        connection.relationType != RelationType.SPOUSE 
    }.forEach { (connection, route) ->
        // Рисуем прямые линии для связей родитель-ребенок
        if (route.size >= 2) {
            val startPoint = route[0]
            val endPoint = route[1]
            
            // Тень для объема
            drawLine(
                color = Color.Black.copy(alpha = 0.15f),
                start = startPoint,
                end = endPoint,
                strokeWidth = 8f * scale,
                cap = StrokeCap.Round
            )

            // Основная линия с градиентом
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4FC3F7),
                        Color(0xFF29B6F6),
                        Color(0xFF03A9F4),
                        Color(0xFF0288D1)
                    ),
                    start = startPoint,
                    end = endPoint
                ),
                start = startPoint,
                end = endPoint,
                strokeWidth = 4f * scale,
                cap = StrokeCap.Round
            )

            // Стрелка на конце (только для вертикальных линий к детям)
            if (abs(startPoint.x - endPoint.x) < 5f && endPoint.y > startPoint.y) {
                val arrowSize = 10f * scale
                val arrowPath = Path().apply {
                    moveTo(endPoint.x, endPoint.y)
                    lineTo(endPoint.x - arrowSize, endPoint.y - arrowSize * 1.8f)
                    lineTo(endPoint.x + arrowSize, endPoint.y - arrowSize * 1.8f)
                    close()
                }
                
                drawPath(
                    path = arrowPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1976D2),
                            Color(0xFF1565C0)
                        )
                    )
                )
            }
            
            // Узловые точки на ответвлениях от ствола
            if (abs(startPoint.y - endPoint.y) < 5f && abs(startPoint.x - endPoint.x) > 20f) {
                // Это горизонтальная линия разветвления - рисуем точки на концах
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1976D2),
                            Color(0xFF1565C0)
                        )
                    ),
                    center = startPoint,
                    radius = 6f * scale
                )
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1976D2),
                            Color(0xFF1565C0)
                        )
                    ),
                    center = endPoint,
                    radius = 6f * scale
                )
                
                // Белые блики
                drawCircle(
                    color = Color.White.copy(alpha = 0.7f),
                    center = Offset(startPoint.x - 1.5f * scale, startPoint.y - 1.5f * scale),
                    radius = 2.5f * scale
                )
                
                drawCircle(
                    color = Color.White.copy(alpha = 0.7f),
                    center = Offset(endPoint.x - 1.5f * scale, endPoint.y - 1.5f * scale),
                    radius = 2.5f * scale
                )
            }
        }
    }
    
    // 2. Потом супружеские связи (на переднем плане)
    connectionRoutes.filter { (connection, _) -> 
        connection.relationType == RelationType.SPOUSE 
    }.forEach { (connection, route) ->
        // Рисуем линию супругов
        val startPoint = route[0]
        val endPoint = route[1]
        
        // Основная линия с прямыми углами
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFF6B9D),
                    Color(0xFFFF8A9B),
                    Color(0xFFFF6B9D)
                )
            ),
            start = startPoint,
            end = endPoint,
            strokeWidth = 4f * scale,
            cap = StrokeCap.Square
        )
        
        // Тень для объема
        drawLine(
            color = Color(0xFFFF6B9D).copy(alpha = 0.3f),
            start = startPoint.copy(y = startPoint.y + 2f * scale),
            end = endPoint.copy(y = endPoint.y + 2f * scale),
            strokeWidth = 6f * scale,
            cap = StrokeCap.Square
        )

        // Сердечко в центре
        val center = (startPoint + endPoint) / 2f
        drawHeart(
            center = center,
            size = 20f * scale,
            color = Color(0xFFFF1744)
        )
    }
}

private fun DrawScope.drawHeart(
    center: Offset,
    size: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(center.x, center.y + size / 4)

        cubicTo(
            center.x + size / 2, center.y - size / 2,
            center.x + size, center.y + size / 3,
            center.x, center.y + size
        )

        cubicTo(
            center.x - size, center.y + size / 3,
            center.x - size / 2, center.y - size / 2,
            center.x, center.y + size / 4
        )
        close()
    }

    drawPath(
        path = path,
        color = color,
        style = Fill
    )
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawTreeNodes(
    nodePositions: List<NodePosition>,
    scale: Float,
    offset: Offset,
    onNodeClick: (Memorial) -> Unit,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val centerX = size.width / 2

    nodePositions.forEach { nodePos ->
        val screenX = nodePos.x * scale + offset.x + centerX
        val screenY = nodePos.y * scale + offset.y
        val width = nodePos.width * scale
        val height = nodePos.height * scale

        if (screenX + width < 0 || screenX > size.width || screenY + height < 0 || screenY > size.height) {
            return@forEach
        }

        // Красивая многослойная тень
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.3f),
            topLeft = Offset(screenX + 10f * scale, screenY + 10f * scale),
            size = Size(width, height),
            cornerRadius = CornerRadius(18f * scale, 18f * scale)
        )

        drawRoundRect(
            color = Color.Black.copy(alpha = 0.2f),
            topLeft = Offset(screenX + 5f * scale, screenY + 5f * scale),
            size = Size(width, height),
            cornerRadius = CornerRadius(18f * scale, 18f * scale)
        )

        // Основной фон - яркий и красивый
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF0F7FF),
                    Color(0xFFE1F0FF),
                    Color(0xFFD6EAFF)
                ),
                startY = screenY,
                endY = screenY + height
            ),
            topLeft = Offset(screenX, screenY),
            size = Size(width, height),
            cornerRadius = CornerRadius(18f * scale, 18f * scale)
        )

        // Яркая верхняя полоска
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF1565C0),
                    Color(0xFF1976D2),
                    Color(0xFF42A5F5),
                    Color(0xFF1976D2),
                    Color(0xFF1565C0)
                )
            ),
            topLeft = Offset(screenX, screenY),
            size = Size(width, 10f * scale),
            cornerRadius = CornerRadius(18f * scale, 0f)
        )

        // Красивый контрастный бордер
        drawRoundRect(
            color = Color(0xFF1976D2).copy(alpha = 0.4f),
            topLeft = Offset(screenX, screenY),
            size = Size(width, height),
            cornerRadius = CornerRadius(18f * scale, 18f * scale),
            style = Stroke(width = 2f * scale)
        )

        // Красивая иконка человека
        val iconSize = 24f * scale
        val iconX = screenX + 15f * scale
        val iconY = screenY + 20f * scale
        
        // Градиентный фон для иконки
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF42A5F5).copy(alpha = 0.2f),
                    Color(0xFF1976D2).copy(alpha = 0.1f)
                ),
                radius = iconSize/2 + 8f * scale
            ),
            center = Offset(iconX + iconSize/2, iconY + iconSize/2),
            radius = iconSize/2 + 8f * scale
        )
        
        // Основная иконка
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1976D2),
                    Color(0xFF1565C0)
                )
            ),
            center = Offset(iconX + iconSize/2, iconY + iconSize/2),
            radius = iconSize/2
        )
        
        // Блик на иконке
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            center = Offset(iconX + iconSize/3, iconY + iconSize/3),
            radius = iconSize/4
        )

        // ФИО - в верхней части
        val nameStyle = TextStyle(
            color = Color(0xFF1A1A1A),
            fontSize = if (scale >= 0.8f) (14 * scale).sp else 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start
        )

        val fioLayout = textMeasurer.measure(
            text = nodePos.memorial.fio,
            style = nameStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        val fioX = screenX + iconSize + 25f * scale
        val fioY = screenY + 15f * scale

        drawText(
            textLayoutResult = fioLayout,
            topLeft = Offset(fioX, fioY)
        )

        // Даты - сразу под ФИО, БЕЗ огромного отступа
        val dates = buildString {
            nodePos.memorial.birthDate?.let { birthDate ->
                append("Рождение: $birthDate")
            }
            nodePos.memorial.deathDate?.let { deathDate ->
                if (nodePos.memorial.birthDate != null) {
                    append("\n")
                }
                append("Смерть: $deathDate")
            }
        }

        if (dates.isNotEmpty()) {
            val datesStyle = TextStyle(
                color = Color(0xFF555555),
                fontSize = if (scale >= 0.8f) (11 * scale).sp else 9.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start
            )

            val datesLayout = textMeasurer.measure(
                text = dates,
                style = datesStyle,
                maxLines = 2
            )

            // Размещаю даты сразу под ФИО
            val datesX = screenX + 20f * scale
            val datesY = screenY + 50f * scale

            drawText(
                textLayoutResult = datesLayout,
                topLeft = Offset(datesX, datesY)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedNodeInfo(
    memorial: Memorial,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = memorial.fio,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (memorial.birthDate != null) {
                    Column {
                        Text(
                            text = "Рождение",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = memorial.birthDate,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (memorial.deathDate != null) {
                    Column {
                        Text(
                            text = "Смерть",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = memorial.deathDate,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (!memorial.biography.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Биография",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = memorial.biography,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
} 