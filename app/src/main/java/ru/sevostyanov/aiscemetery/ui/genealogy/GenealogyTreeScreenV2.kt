package ru.sevostyanov.aiscemetery.ui.genealogy

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

// Улучшенные классы для хранения данных узлов
data class NodePositionV2(
    val memorial: Memorial,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val generation: Int,
    val isHighlighted: Boolean = false,
    val nodeType: NodeType = NodeType.PERSON
)

enum class NodeType {
    PERSON, ROOT, SPOUSE_PAIR
}

// Улучшенный класс для связей с анимацией
data class TreeConnectionV2(
    val from: NodePositionV2,
    val to: NodePositionV2,
    val relationType: RelationType,
    val color: Color,
    val strokeWidth: Float = 3f,
    val isAnimated: Boolean = true,
    val connectionStyle: ConnectionStyle = ConnectionStyle.SMOOTH_CURVE
)

enum class ConnectionStyle {
    STRAIGHT_LINE, SMOOTH_CURVE, STEPPED, ORGANIC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenealogyTreeScreenV2(
    memorials: List<Memorial>,
    relations: List<MemorialRelation>
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var selectedNode by remember { mutableStateOf<Memorial?>(null) }
    var viewMode by remember { mutableStateOf(ViewMode.MODERN) }

    // Анимации
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "scale"
    )

    if (memorials.isEmpty()) {
        ModernEmptyTreeMessage()
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    radius = 2000f
                )
            )
    ) {
        // Современный заголовок с улучшенными элементами управления
        ModernTreeHeader(
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
            onViewModeChange = { viewMode = it },
            currentViewMode = viewMode,
            memberCount = memorials.size,
            relationCount = relations.size
        )

        // Основная область с улучшенным деревом
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp)
        ) {
            val canvasWidth = maxWidth
            val canvasHeight = maxHeight

            ModernGenealogyTreeCanvas(
                memorials = memorials,
                relations = relations,
                scale = animatedScale,
                offset = offset,
                viewMode = viewMode,
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

        // Улучшенная легенда с анимацией
        ModernTreeLegend(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            viewMode = viewMode
        )

        // Информация о выбранном узле с анимацией
        selectedNode?.let { memorial ->
            ModernSelectedNodeInfo(
                memorial = memorial,
                onDismiss = { selectedNode = null }
            )
        }
    }
}

enum class ViewMode {
    MODERN, CLASSIC, MINIMAL, ARTISTIC
}

@Composable
private fun ModernEmptyTreeMessage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Анимированная иконка
                val infiniteTransition = rememberInfiniteTransition(label = "tree_animation")
                val iconScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "icon_scale"
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier
                            .size((64 * iconScale).dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Создайте свое генеалогическое дерево",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Добавьте мемориалы и установите связи между ними,\nчтобы увидеть красивое интерактивное дерево семьи",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { /* TODO: Navigation to add memorial */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Добавить первый мемориал",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernTreeHeader(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetView: () -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    currentViewMode: ViewMode,
    memberCount: Int,
    relationCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(6.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Информация о дереве
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Генеалогическое дерево",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$memberCount чел.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$relationCount связей",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Элементы управления
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Переключатель режимов просмотра
                ViewModeSelector(
                    currentMode = currentViewMode,
                    onModeChange = onViewModeChange
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Кнопки зума
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModernControlButton(
                        onClick = onZoomOut,
                        icon = Icons.Default.Close,
                        contentDescription = "Уменьшить"
                    )
                    ModernControlButton(
                        onClick = onResetView,
                        icon = Icons.Default.Refresh,
                        contentDescription = "Центрировать"
                    )
                    ModernControlButton(
                        onClick = onZoomIn,
                        icon = Icons.Default.Add,
                        contentDescription = "Увеличить"
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewModeSelector(
    currentMode: ViewMode,
    onModeChange: (ViewMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = getViewModeIcon(currentMode),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = getViewModeName(currentMode),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.shadow(8.dp, RoundedCornerShape(12.dp))
        ) {
            ViewMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = getViewModeIcon(mode),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (mode == currentMode) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = getViewModeName(mode),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (mode == currentMode) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    onClick = {
                        onModeChange(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun getViewModeIcon(mode: ViewMode) = when (mode) {
    ViewMode.MODERN -> Icons.Default.Add
    ViewMode.CLASSIC -> Icons.Default.Share
    ViewMode.MINIMAL -> Icons.Default.Close
    ViewMode.ARTISTIC -> Icons.Default.Refresh
}

private fun getViewModeName(mode: ViewMode) = when (mode) {
    ViewMode.MODERN -> "Современный"
    ViewMode.CLASSIC -> "Классический"
    ViewMode.MINIMAL -> "Минимальный"
    ViewMode.ARTISTIC -> "Художественный"
}

@Composable
private fun ModernControlButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    )
                )
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun ModernGenealogyTreeCanvas(
    memorials: List<Memorial>,
    relations: List<MemorialRelation>,
    scale: Float,
    offset: Offset,
    viewMode: ViewMode,
    onTransform: (Float, Offset) -> Unit,
    onNodeClick: (Memorial) -> Unit,
    canvasWidth: Dp,
    canvasHeight: Dp
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val nodePositions = remember(memorials, relations) {
        calculateModernNodePositions(memorials, relations, density)
    }

    val connections = remember(relations, nodePositions, viewMode) {
        calculateModernTreeConnections(relations, nodePositions, viewMode)
    }

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
            // Рисуем фоновые элементы
            drawModernBackground(viewMode)
            
            // Рисуем связи
            drawModernTreeConnections(connections, scale, offset, viewMode)
            
            // Рисуем узлы
            drawModernTreeNodes(nodePositions, scale, offset, onNodeClick, textMeasurer, viewMode)
        }
    }
}

private fun calculateModernNodePositions(
    memorials: List<Memorial>,
    relations: List<MemorialRelation>,
    density: androidx.compose.ui.unit.Density
): List<NodePositionV2> {
    if (memorials.isEmpty()) return emptyList()
    
    val positions = mutableListOf<NodePositionV2>()
    
    // Создаем граф связей с улучшенной логикой
    val memorialMap = memorials.associateBy { it.id!! }
    val childrenMap = mutableMapOf<Long, MutableSet<Long>>()
    val parentsMap = mutableMapOf<Long, MutableSet<Long>>()
    val spouseMap = mutableMapOf<Long, MutableSet<Long>>()

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
                spouseMap.getOrPut(sourceId) { mutableSetOf() }.add(targetId)
                spouseMap.getOrPut(targetId) { mutableSetOf() }.add(sourceId)
            }
            else -> {}
        }
    }

    // Находим корни (люди без родителей)
    val roots = memorials.filter { memorial ->
        memorial.id?.let { id -> parentsMap[id].isNullOrEmpty() } ?: false
    }

    if (roots.isEmpty()) {
        // Если нет корней, берем первого человека
        val firstPerson = memorials.firstOrNull()
        if (firstPerson != null) {
            return listOf(
                NodePositionV2(
                    memorial = firstPerson,
                    x = 0f,
                    y = 0f,
                    width = with(density) { 280.dp.toPx() },
                    height = with(density) { 120.dp.toPx() },
                    generation = 0,
                    nodeType = NodeType.ROOT
                )
            )
        }
        return emptyList()
    }

    // Определяем поколения
    val generationMap = mutableMapOf<Long, Int>()
    val queue = ArrayDeque<Pair<Long, Int>>()
    
    roots.forEach { root ->
        root.id?.let {
            queue.add(it to 0)
            generationMap[it] = 0
        }
    }

    while (queue.isNotEmpty()) {
        val (currentId, level) = queue.removeFirst()
        childrenMap[currentId]?.forEach { childId ->
            if (!generationMap.containsKey(childId)) {
                generationMap[childId] = level + 1
                queue.add(childId to level + 1)
            }
        }
    }

    // Размещаем узлы по поколениям с учетом супругов
    val nodeWidth = with(density) { 280.dp.toPx() }
    val nodeHeight = with(density) { 120.dp.toPx() }
    val levelSpacing = 220f  // Расстояние между поколениями
    val nodeSpacing = 350f   // Расстояние между группами в одном поколении  
    val spouseSpacing = 40f  // Небольшое расстояние между супругами (чтобы были рядом)

    val levels = generationMap.entries.groupBy({ it.value }, { it.key }).toSortedMap()
    val processedSpouses = mutableSetOf<Long>()
    
    levels.forEach { (level, nodeIds) ->
        val levelNodes = nodeIds.mapNotNull { memorialMap[it] }
        val y = 100f + level * levelSpacing
        
        // Группируем супругов
        val spouseGroups = mutableListOf<List<Memorial>>()
        val singles = mutableListOf<Memorial>()
        
        levelNodes.forEach { person ->
            val personId = person.id!!
            if (processedSpouses.contains(personId)) return@forEach
            
            val spouses = spouseMap[personId]?.mapNotNull { memorialMap[it] }?.filter { it in levelNodes }
            if (!spouses.isNullOrEmpty()) {
                val group = mutableListOf(person)
                spouses.forEach { spouse ->
                    group.add(spouse)
                    processedSpouses.add(spouse.id!!)
                }
                spouseGroups.add(group)
                processedSpouses.add(personId)
            } else {
                singles.add(person)
            }
        }
        
        // Размещаем группы супругов и одиночек
        val allGroups = spouseGroups + singles.map { listOf(it) }
        
        // Вычисляем общую ширину всех групп с учетом внутренних расстояний
        var totalRequiredWidth = 0f
        allGroups.forEach { group ->
            if (group.size == 1) {
                totalRequiredWidth += nodeWidth
            } else {
                // Для супружеской пары: ширина обеих карточек + маленький промежуток между ними
                totalRequiredWidth += nodeWidth * group.size + spouseSpacing * (group.size - 1)
            }
        }
        
        // Добавляем промежутки между группами
        if (allGroups.size > 1) {
            totalRequiredWidth += nodeSpacing * (allGroups.size - 1)
        }
        
        val startX = -totalRequiredWidth / 2f
        var currentX = startX
        
        allGroups.forEach { group ->
            if (group.size == 1) {
                // Одиночный узел
                positions.add(
                    NodePositionV2(
                        memorial = group[0],
                        x = currentX,
                        y = y,
                        width = nodeWidth,
                        height = nodeHeight,
                        generation = level,
                        nodeType = if (level == 0) NodeType.ROOT else NodeType.PERSON
                    )
                )
                currentX += nodeWidth + nodeSpacing
            } else {
                // Супружеская пара - размещаем рядом с небольшим промежутком
                group.forEachIndexed { spouseIndex, spouse ->
                    positions.add(
                        NodePositionV2(
                            memorial = spouse,
                            x = currentX,
                            y = y,
                            width = nodeWidth,
                            height = nodeHeight,
                            generation = level,
                            nodeType = NodeType.SPOUSE_PAIR
                        )
                    )
                    currentX += nodeWidth
                    if (spouseIndex < group.size - 1) {
                        currentX += spouseSpacing // Маленький промежуток между супругами
                    }
                }
                currentX += nodeSpacing // Промежуток до следующей группы
            }
        }
    }

    // Применяем центрирование детей под родителями
    val centeredPositions = centerChildrenUnderParents(positions, childrenMap, spouseMap, nodeWidth)
    
    // Применяем алгоритм предотвращения пересечений
    return applyCollisionAvoidance(centeredPositions, nodeWidth, nodeHeight)
}

// Центрирование детей под родителями
private fun centerChildrenUnderParents(
    positions: List<NodePositionV2>,
    childrenMap: Map<Long, Set<Long>>,
    spouseMap: Map<Long, Set<Long>>,
    nodeWidth: Float
): List<NodePositionV2> {
    val adjustedPositions = positions.toMutableList()
    val positionMap = positions.associateBy { it.memorial.id!! }
    
    // Группируем по поколениям
    val generations = positions.groupBy { it.generation }
    
    generations.forEach { (generation, nodesInGeneration) ->
        if (generation == 0) return@forEach // Пропускаем корневое поколение
        
        nodesInGeneration.forEach { child ->
            val childId = child.memorial.id!!
            val childIndex = adjustedPositions.indexOfFirst { it.memorial.id == childId }
            if (childIndex == -1) return@forEach
            
            // Находим родителей этого ребенка
            val parents = mutableListOf<NodePositionV2>()
            childrenMap.forEach { (parentId, children) ->
                if (children.contains(childId)) {
                    positionMap[parentId]?.let { parent ->
                        parents.add(parent)
                    }
                }
            }
            
            if (parents.isNotEmpty()) {
                // Если у ребенка есть родители, центрируем его под ними
                val parentsCenterX = if (parents.size == 1) {
                    // Один родитель
                    val parent = parents[0]
                    // Проверяем, есть ли у этого родителя супруг
                    val spouseId = spouseMap[parent.memorial.id!!]?.firstOrNull()
                    if (spouseId != null) {
                        val spouse = positionMap[spouseId]
                        if (spouse != null) {
                            // Центрируем между родителем и супругом
                            (parent.x + spouse.x + nodeWidth) / 2f
                        } else {
                            parent.x + nodeWidth / 2f
                        }
                    } else {
                        parent.x + nodeWidth / 2f
                    }
                } else {
                    // Несколько родителей - центрируем между ними
                    val minX = parents.minOf { it.x }
                    val maxX = parents.maxOf { it.x + nodeWidth }
                    (minX + maxX) / 2f
                }
                
                // Обновляем позицию ребенка
                val newChildX = parentsCenterX - nodeWidth / 2f
                adjustedPositions[childIndex] = adjustedPositions[childIndex].copy(x = newChildX)
            }
        }
    }
    
    return adjustedPositions
}

// Алгоритм предотвращения пересечений узлов
private fun applyCollisionAvoidance(
    positions: List<NodePositionV2>,
    nodeWidth: Float,
    nodeHeight: Float
): List<NodePositionV2> {
    val adjustedPositions = positions.toMutableList()
    val padding = 15f // Уменьшаем отступ для более компактного размещения
    
    // Группируем по поколениям для независимой обработки
    val generations = adjustedPositions.groupBy { it.generation }
    
    generations.forEach { (generation, nodesInGeneration) ->
        val sortedNodes = nodesInGeneration.sortedBy { it.x }
        
        for (i in sortedNodes.indices) {
            val currentNode = sortedNodes[i]
            val currentIndex = adjustedPositions.indexOfFirst { it.memorial.id == currentNode.memorial.id }
            
            // Проверяем пересечения с предыдущими узлами в том же поколении
            for (j in 0 until i) {
                val otherNode = sortedNodes[j]
                val otherIndex = adjustedPositions.indexOfFirst { it.memorial.id == otherNode.memorial.id }
                
                if (otherIndex != -1 && currentIndex != -1) {
                    val currentRight = adjustedPositions[currentIndex].x + nodeWidth
                    val otherRight = adjustedPositions[otherIndex].x + nodeWidth
                    val currentLeft = adjustedPositions[currentIndex].x
                    val otherLeft = adjustedPositions[otherIndex].x
                    
                    // Проверяем горизонтальное пересечение
                    if (currentLeft < otherRight + padding && currentRight > otherLeft - padding) {
                        // Сдвигаем текущий узел вправо
                        val newX = otherRight + padding
                        adjustedPositions[currentIndex] = adjustedPositions[currentIndex].copy(x = newX)
                    }
                }
            }
        }
    }
    
    // Дополнительная проверка на вертикальные пересечения
    for (i in adjustedPositions.indices) {
        for (j in i + 1 until adjustedPositions.size) {
            val node1 = adjustedPositions[i]
            val node2 = adjustedPositions[j]
            
            // Проверяем пересечение прямоугольников
            if (isOverlapping(node1, node2, nodeWidth, nodeHeight, padding)) {
                // Если узлы в разных поколениях, но пересекаются, сдвигаем один из них
                if (node1.generation != node2.generation) {
                    val deltaX = (nodeWidth + padding) - abs(node1.x - node2.x)
                    if (deltaX > 0) {
                        if (node1.x < node2.x) {
                            adjustedPositions[j] = node2.copy(x = node2.x + deltaX)
                        } else {
                            adjustedPositions[i] = node1.copy(x = node1.x + deltaX)
                        }
                    }
                }
            }
        }
    }
    
    return adjustedPositions
}

// Проверка пересечения двух прямоугольников
private fun isOverlapping(
    node1: NodePositionV2,
    node2: NodePositionV2,
    nodeWidth: Float,
    nodeHeight: Float,
    padding: Float
): Boolean {
    val left1 = node1.x - padding
    val right1 = node1.x + nodeWidth + padding
    val top1 = node1.y - padding
    val bottom1 = node1.y + nodeHeight + padding
    
    val left2 = node2.x - padding
    val right2 = node2.x + nodeWidth + padding
    val top2 = node2.y - padding
    val bottom2 = node2.y + nodeHeight + padding
    
    return !(right1 <= left2 || left1 >= right2 || bottom1 <= top2 || top1 >= bottom2)
}

private fun calculateModernTreeConnections(
    relations: List<MemorialRelation>,
    nodePositions: List<NodePositionV2>,
    viewMode: ViewMode
): List<TreeConnectionV2> {
    val positionMap = nodePositions.associateBy { it.memorial.id }
    val connections = mutableListOf<TreeConnectionV2>()
    
    relations.forEach { relation ->
        val sourcePos = positionMap[relation.sourceMemorial.id]
        val targetPos = positionMap[relation.targetMemorial.id]

        if (sourcePos != null && targetPos != null) {
            val connectionStyle = when (viewMode) {
                ViewMode.MODERN -> ConnectionStyle.SMOOTH_CURVE
                ViewMode.CLASSIC -> ConnectionStyle.STEPPED
                ViewMode.MINIMAL -> ConnectionStyle.STRAIGHT_LINE
                ViewMode.ARTISTIC -> ConnectionStyle.ORGANIC
            }

            val color = when (relation.relationType) {
                RelationType.PARENT, RelationType.CHILD -> Color(0xFF6366F1)  // Indigo
                RelationType.SPOUSE -> Color(0xFFEC4899)  // Pink
                else -> Color(0xFF8B5CF6)  // Purple
            }

            connections.add(
                TreeConnectionV2(
                    from = sourcePos,
                    to = targetPos,
                    relationType = relation.relationType,
                    color = color,
                    strokeWidth = when (viewMode) {
                        ViewMode.MODERN -> 4f
                        ViewMode.CLASSIC -> 3f
                        ViewMode.MINIMAL -> 2f
                        ViewMode.ARTISTIC -> 5f
                    },
                    connectionStyle = connectionStyle
                )
            )
        }
    }

    return connections
}

// Функции отрисовки
private fun DrawScope.drawModernBackground(viewMode: ViewMode) {
    when (viewMode) {
        ViewMode.MODERN -> {
            // Современный градиентный фон с сеткой
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E1B4B).copy(alpha = 0.03f),
                        Color(0xFF3730A3).copy(alpha = 0.01f),
                        Color.Transparent
                    ),
                    radius = size.width * 0.8f
                )
            )
            
            // Рисуем тонкую сетку
            drawModernGrid()
        }
        ViewMode.ARTISTIC -> {
            // Художественный фон с органическими формами
            drawArtisticBackground()
        }
        ViewMode.MINIMAL -> {
            // Минимальный фон - почти чистый
            drawRect(
                color = Color.White.copy(alpha = 0.02f)
            )
        }
        ViewMode.CLASSIC -> {
            // Классический фон с тонкими линиями
            drawClassicGrid()
        }
    }
}

private fun DrawScope.drawModernGrid() {
    val gridSpacing = 100f
    val gridColor = Color(0xFF6366F1).copy(alpha = 0.05f)
    
    // Вертикальные линии
    var x = 0f
    while (x <= size.width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += gridSpacing
    }
    
    // Горизонтальные линии
    var y = 0f
    while (y <= size.height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += gridSpacing
    }
}

private fun DrawScope.drawArtisticBackground() {
    // Органические формы для художественного режима
    val colors = listOf(
        Color(0xFFEC4899).copy(alpha = 0.03f),
        Color(0xFF8B5CF6).copy(alpha = 0.02f),
        Color(0xFF6366F1).copy(alpha = 0.025f)
    )
    
    // Рисуем несколько органических пятен
    repeat(5) { i ->
        val centerX = size.width * (0.2f + i * 0.15f)
        val centerY = size.height * (0.3f + (i % 2) * 0.4f)
        val radius = 200f + i * 50f
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors[i % colors.size], Color.Transparent),
                radius = radius
            ),
            center = Offset(centerX, centerY),
            radius = radius
        )
    }
}

private fun DrawScope.drawClassicGrid() {
    val gridSpacing = 150f
    val gridColor = Color(0xFF374151).copy(alpha = 0.08f)
    
    // Классическая сетка с более широким шагом
    var x = 0f
    while (x <= size.width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 0.5f
        )
        x += gridSpacing
    }
    
    var y = 0f
    while (y <= size.height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 0.5f
        )
        y += gridSpacing
    }
}

private fun DrawScope.drawModernTreeConnections(
    connections: List<TreeConnectionV2>,
    scale: Float,
    offset: Offset,
    viewMode: ViewMode
) {
    val centerX = size.width / 2

    connections.forEach { connection ->
        // Определяем точки подключения в зависимости от типа связи
        val (fromX, fromY, toX, toY) = when (connection.relationType) {
            RelationType.SPOUSE -> {
                // Для супругов - боковые точки подключения
                val fromX = if (connection.from.x < connection.to.x) {
                    connection.from.x * scale + offset.x + centerX + connection.from.width * scale
                } else {
                    connection.from.x * scale + offset.x + centerX
                }
                val fromY = connection.from.y * scale + offset.y + connection.from.height * scale / 2
                
                val toX = if (connection.to.x < connection.from.x) {
                    connection.to.x * scale + offset.x + centerX + connection.to.width * scale
                } else {
                    connection.to.x * scale + offset.x + centerX
                }
                val toY = connection.to.y * scale + offset.y + connection.to.height * scale / 2
                
                arrayOf(fromX, fromY, toX, toY)
            }
            RelationType.PARENT, RelationType.CHILD -> {
                // Для родитель-ребенок - вертикальные точки подключения
                val fromX = connection.from.x * scale + offset.x + centerX + connection.from.width * scale / 2
                val fromY = if (connection.from.y < connection.to.y) {
                    connection.from.y * scale + offset.y + connection.from.height * scale
                } else {
                    connection.from.y * scale + offset.y
                }
                
                val toX = connection.to.x * scale + offset.x + centerX + connection.to.width * scale / 2
                val toY = if (connection.to.y > connection.from.y) {
                    connection.to.y * scale + offset.y
                } else {
                    connection.to.y * scale + offset.y + connection.to.height * scale
                }
                
                arrayOf(fromX, fromY, toX, toY)
            }
            else -> {
                // По умолчанию - центральные точки
                val fromX = connection.from.x * scale + offset.x + centerX + connection.from.width * scale / 2
                val fromY = connection.from.y * scale + offset.y + connection.from.height * scale / 2
                val toX = connection.to.x * scale + offset.x + centerX + connection.to.width * scale / 2
                val toY = connection.to.y * scale + offset.y + connection.to.height * scale / 2
                
                arrayOf(fromX, fromY, toX, toY)
            }
        }

        val start = Offset(fromX, fromY)
        val end = Offset(toX, toY)
        val strokeWidth = (connection.strokeWidth * scale).coerceAtLeast(2f)

        when (connection.connectionStyle) {
            ConnectionStyle.SMOOTH_CURVE -> {
                drawSmoothConnection(
                    start = start,
                    end = end,
                    color = connection.color,
                    strokeWidth = strokeWidth,
                    relationType = connection.relationType
                )
            }
            ConnectionStyle.STEPPED -> {
                drawSteppedConnection(
                    start = start,
                    end = end,
                    color = connection.color,
                    strokeWidth = strokeWidth
                )
            }
            ConnectionStyle.STRAIGHT_LINE -> {
                drawLine(
                    color = connection.color,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            ConnectionStyle.ORGANIC -> {
                drawOrganicConnection(
                    start = start,
                    end = end,
                    color = connection.color,
                    strokeWidth = strokeWidth
                )
            }
        }

        // Стрелки для родитель-ребенок связей
        if (connection.relationType == RelationType.PARENT || connection.relationType == RelationType.CHILD) {
            drawArrow(start, end, connection.color, strokeWidth)
        }
    }
}

private fun DrawScope.drawSmoothConnection(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
    relationType: RelationType
) {
    val path = Path()
    
    when (relationType) {
        RelationType.SPOUSE -> {
            // Прямая линия для супругов
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.8f),
                        color,
                        color.copy(alpha = 0.8f)
                    )
                ),
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            
            // Сердечко в центре
            val center = Offset((start.x + end.x) / 2, (start.y + end.y) / 2)
            drawHeart(center, 16f * (strokeWidth / 4f), color)
        }
        else -> {
            // Плавная кривая для родительских связей
            val controlPoint1 = Offset(start.x, start.y + (end.y - start.y) * 0.5f)
            val controlPoint2 = Offset(end.x, start.y + (end.y - start.y) * 0.5f)
            
            path.moveTo(start.x, start.y)
            path.cubicTo(
                controlPoint1.x, controlPoint1.y,
                controlPoint2.x, controlPoint2.y,
                end.x, end.y
            )
            
            // Тень
            drawPath(
                path = path,
                color = Color.Black.copy(alpha = 0.1f),
                style = Stroke(
                    width = strokeWidth + 2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            
            // Основная линия с градиентом
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.9f),
                        color,
                        color.copy(alpha = 0.7f)
                    )
                ),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

private fun DrawScope.drawSteppedConnection(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    val midY = (start.y + end.y) / 2
    
    val path = Path().apply {
        moveTo(start.x, start.y)
        lineTo(start.x, midY)
        lineTo(end.x, midY)
        lineTo(end.x, end.y)
    }
    
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawOrganicConnection(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    val path = Path()
    val distance = sqrt((end.x - start.x).pow(2) + (end.y - start.y).pow(2))
    val waveAmplitude = minOf(30f, distance * 0.1f)
    
    path.moveTo(start.x, start.y)
    
    // Создаем органическую волнистую линию
    val steps = maxOf(10, (distance / 20f).toInt())
    for (i in 1..steps) {
        val t = i.toFloat() / steps
        val x = start.x + (end.x - start.x) * t
        val y = start.y + (end.y - start.y) * t
        
        // Добавляем органическое колебание
        val wave = sin(t * PI * 2) * waveAmplitude * (1 - abs(t - 0.5f) * 2)
        val offsetX = x + wave.toFloat()
        
        if (i == 1) {
            path.lineTo(offsetX, y)
        } else {
            val prevT = (i - 1).toFloat() / steps
            val prevX = start.x + (end.x - start.x) * prevT + sin(prevT * PI * 2).toFloat() * waveAmplitude * (1 - abs(prevT - 0.5f) * 2)
            val prevY = start.y + (end.y - start.y) * prevT
            
            val cp1X = (prevX + offsetX) / 2
            val cp1Y = (prevY + y) / 2
            
            path.quadraticBezierTo(cp1X, cp1Y, offsetX, y)
        }
    }
    
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
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
    
    drawPath(path = path, color = color, style = Fill)
}

private fun DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    val arrowLength = 15f * (strokeWidth / 4f)
    val arrowAngle = PI / 6 // 30 градусов
    
    val angle = atan2(end.y - start.y, end.x - start.x)
    
    // Точки стрелки
    val arrowPoint1 = Offset(
        end.x - arrowLength * cos(angle - arrowAngle).toFloat(),
        end.y - arrowLength * sin(angle - arrowAngle).toFloat()
    )
    
    val arrowPoint2 = Offset(
        end.x - arrowLength * cos(angle + arrowAngle).toFloat(),
        end.y - arrowLength * sin(angle + arrowAngle).toFloat()
    )
    
    // Рисуем стрелку
    drawLine(
        color = color,
        start = end,
        end = arrowPoint1,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    
    drawLine(
        color = color,
        start = end,
        end = arrowPoint2,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawModernTreeNodes(
    nodePositions: List<NodePositionV2>,
    scale: Float,
    offset: Offset,
    onNodeClick: (Memorial) -> Unit,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    viewMode: ViewMode
) {
    val centerX = size.width / 2

    nodePositions.forEach { node ->
        val x = node.x * scale + offset.x + centerX
        val y = node.y * scale + offset.y
        val width = node.width * scale
        val height = node.height * scale

        when (viewMode) {
            ViewMode.MODERN -> drawModernNode(node, x, y, width, height, textMeasurer)
            ViewMode.CLASSIC -> drawClassicNode(node, x, y, width, height, textMeasurer)
            ViewMode.MINIMAL -> drawMinimalNode(node, x, y, width, height, textMeasurer)
            ViewMode.ARTISTIC -> drawArtisticNode(node, x, y, width, height, textMeasurer)
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawModernNode(
    node: NodePositionV2,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val cornerRadius = 24f
    val memorial = node.memorial
    
    // Тень
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.1f),
        topLeft = Offset(x + 4f, y + 4f),
        size = Size(width, height),
        cornerRadius = CornerRadius(cornerRadius)
    )
    
    // Основная карточка с градиентом
    val cardColors = when (node.nodeType) {
        NodeType.ROOT -> listOf(
            Color(0xFF6366F1),
            Color(0xFF8B5CF6)
        )
        NodeType.SPOUSE_PAIR -> listOf(
            Color(0xFFEC4899),
            Color(0xFFF97316)
        )
        else -> listOf(
            Color(0xFF10B981),
            Color(0xFF06B6D4)
        )
    }
    
    drawRoundRect(
        brush = Brush.verticalGradient(cardColors),
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(cornerRadius)
    )
    
    // Белый блик
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.3f),
                Color.Transparent
            )
        ),
        topLeft = Offset(x, y),
        size = Size(width, height * 0.3f),
        cornerRadius = CornerRadius(cornerRadius)
    )
    
    // Исправляем отображение текста - убираем null
    val textColor = Color.White
    val firstName = memorial.firstName?.takeIf { it.isNotBlank() }
    val lastName = memorial.lastName?.takeIf { it.isNotBlank() }
    
    val nameText = when {
        firstName != null && lastName != null -> "$firstName $lastName"
        firstName != null -> firstName
        lastName != null -> lastName
        else -> "Неизвестное лицо"
    }
    
    val birthDate = memorial.birthDate?.takeIf { it.isNotBlank() } ?: "?"
    val deathDate = memorial.deathDate?.takeIf { it.isNotBlank() } ?: "?"
    val datesText = "$birthDate - $deathDate"
    
    // Имя
    val nameStyle = TextStyle(
        fontSize = (16 * (width / 280f)).sp,
        fontWeight = FontWeight.Bold,
        color = textColor
    )
    
    val nameResult = textMeasurer.measure(nameText, nameStyle)
    drawText(
        textLayoutResult = nameResult,
        topLeft = Offset(
            x + (width - nameResult.size.width) / 2,
            y + height * 0.3f
        )
    )
    
    // Даты
    val dateStyle = TextStyle(
        fontSize = (12 * (width / 280f)).sp,
        color = textColor.copy(alpha = 0.9f)
    )
    
    val dateResult = textMeasurer.measure(datesText, dateStyle)
    drawText(
        textLayoutResult = dateResult,
        topLeft = Offset(
            x + (width - dateResult.size.width) / 2,
            y + height * 0.65f
        )
    )
    
    // Добавляем иконку типа узла
    when (node.nodeType) {
        NodeType.ROOT -> {
            // Корона для корневого узла
            drawCircle(
                color = Color.Yellow.copy(alpha = 0.8f),
                radius = 8f,
                center = Offset(x + width - 20f, y + 20f)
            )
        }
        NodeType.SPOUSE_PAIR -> {
            // Сердечко для супругов
            drawHeart(
                center = Offset(x + width - 20f, y + 20f),
                size = 12f,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        else -> {}
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawClassicNode(
    node: NodePositionV2,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    // Классический прямоугольный узел
    drawRect(
        color = Color.White,
        topLeft = Offset(x, y),
        size = Size(width, height)
    )
    
    drawRect(
        color = Color(0xFF374151),
        topLeft = Offset(x, y),
        size = Size(width, height),
        style = Stroke(width = 2f)
    )
    
    // Текст в классическом стиле
    val memorial = node.memorial
    val firstName = memorial.firstName?.takeIf { it.isNotBlank() }
    val lastName = memorial.lastName?.takeIf { it.isNotBlank() }
    
    val nameText = when {
        firstName != null && lastName != null -> "$firstName $lastName"
        firstName != null -> firstName
        lastName != null -> lastName
        else -> "Неизвестное лицо"
    }
    val datesText = "${memorial.birthDate ?: "?"} - ${memorial.deathDate ?: "?"}"
    
    val nameStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF1F2937)
    )
    
    val nameResult = textMeasurer.measure(nameText, nameStyle)
    drawText(
        textLayoutResult = nameResult,
        topLeft = Offset(
            x + (width - nameResult.size.width) / 2,
            y + height * 0.35f
        )
    )
    
    val dateStyle = TextStyle(
        fontSize = 11.sp,
        color = Color(0xFF6B7280)
    )
    
    val dateResult = textMeasurer.measure(datesText, dateStyle)
    drawText(
        textLayoutResult = dateResult,
        topLeft = Offset(
            x + (width - dateResult.size.width) / 2,
            y + height * 0.6f
        )
    )
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawMinimalNode(
    node: NodePositionV2,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    // Минимальный дизайн - только контур
    drawRoundRect(
        color = Color(0xFF9CA3AF),
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(8f),
        style = Stroke(width = 1f)
    )
    
    val memorial = node.memorial
    val firstName = memorial.firstName?.takeIf { it.isNotBlank() }
    val lastName = memorial.lastName?.takeIf { it.isNotBlank() }
    
    val nameText = when {
        firstName != null && lastName != null -> "$firstName $lastName"
        firstName != null -> firstName
        lastName != null -> lastName
        else -> "Неизвестное лицо"
    }
    
    val nameStyle = TextStyle(
        fontSize = 13.sp,
        color = Color(0xFF374151)
    )
    
    val nameResult = textMeasurer.measure(nameText, nameStyle)
    drawText(
        textLayoutResult = nameResult,
        topLeft = Offset(
            x + (width - nameResult.size.width) / 2,
            y + (height - nameResult.size.height) / 2
        )
    )
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawArtisticNode(
    node: NodePositionV2,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    // Художественный стиль с органическими формами
    val path = Path().apply {
        val centerX = x + width / 2
        val centerY = y + height / 2
        val radiusX = width / 2
        val radiusY = height / 2
        
        // Создаем органическую форму
        moveTo(centerX, centerY - radiusY)
        
        // Верхняя часть
        cubicTo(
            centerX + radiusX * 0.8f, centerY - radiusY * 0.5f,
            centerX + radiusX * 0.9f, centerY + radiusY * 0.2f,
            centerX + radiusX * 0.7f, centerY + radiusY * 0.8f
        )
        
        // Правая часть
        cubicTo(
            centerX + radiusX * 0.3f, centerY + radiusY,
            centerX - radiusX * 0.3f, centerY + radiusY,
            centerX - radiusX * 0.7f, centerY + radiusY * 0.8f
        )
        
        // Левая часть
        cubicTo(
            centerX - radiusX * 0.9f, centerY + radiusY * 0.2f,
            centerX - radiusX * 0.8f, centerY - radiusY * 0.5f,
            centerX, centerY - radiusY
        )
        
        close()
    }
    
    // Тень
    val shadowPath = Path().apply {
        addPath(path, Offset(3f, 3f))
    }
    drawPath(
        path = shadowPath,
        color = Color.Black.copy(alpha = 0.15f)
    )
    
    // Основная форма
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFEC4899).copy(alpha = 0.8f),
                Color(0xFF8B5CF6).copy(alpha = 0.9f),
                Color(0xFF6366F1)
            ),
            center = Offset(x + width / 2, y + height / 2),
            radius = width / 2
        )
    )
    
    // Текст
    val memorial = node.memorial
    val firstName = memorial.firstName?.takeIf { it.isNotBlank() }
    val lastName = memorial.lastName?.takeIf { it.isNotBlank() }
    
    val nameText = when {
        firstName != null && lastName != null -> "$firstName\n$lastName"
        firstName != null -> firstName
        lastName != null -> lastName
        else -> "Неизвестное\nлицо"
    }
    
    val nameStyle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        textAlign = TextAlign.Center
    )
    
    val nameResult = textMeasurer.measure(nameText, nameStyle)
    drawText(
        textLayoutResult = nameResult,
        topLeft = Offset(
            x + (width - nameResult.size.width) / 2,
            y + (height - nameResult.size.height) / 2
        )
    )
}

@Composable
private fun ModernTreeLegend(
    modifier: Modifier = Modifier,
    viewMode: ViewMode
) {
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Типы связей",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            ModernLegendItem(
                color = Color(0xFF6366F1),
                text = "Родитель → Ребенок",
                icon = Icons.Default.KeyboardArrowDown
            )
            
            ModernLegendItem(
                color = Color(0xFFEC4899),
                text = "Супруги ♥",
                icon = Icons.Default.Favorite
            )
            
            if (viewMode == ViewMode.MODERN) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Режим: ${getViewModeName(viewMode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModernLegendItem(
    color: Color,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ModernSelectedNodeInfo(
    memorial: Memorial,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Информация о персоне",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val firstName = memorial.firstName?.takeIf { it.isNotBlank() }
            val lastName = memorial.lastName?.takeIf { it.isNotBlank() }
            
            val fullName = when {
                firstName != null && lastName != null -> "$firstName $lastName"
                firstName != null -> firstName
                lastName != null -> lastName
                else -> "Неизвестное лицо"
            }
            
            Text(
                text = fullName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            memorial.birthDate?.let {
                Text(
                    text = "Дата рождения: $it",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            memorial.deathDate?.let {
                Text(
                    text = "Дата смерти: $it",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
                         memorial.biography?.takeIf { it.isNotBlank() }?.let { bio ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Биография:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 