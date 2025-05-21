package ru.sevostyanov.aiscemetery.ui.genealogy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.MemorialRelation
import ru.sevostyanov.aiscemetery.models.RelationType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawBehind
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun GenealogyTreeScreen(
    memorials: List<Memorial>,
    relations: List<MemorialRelation>
) {
    Box(Modifier.fillMaxSize()) {
        GenealogyTreeCanvas(memorials, relations)
    }
}

@Composable
fun GenealogyTreeCanvas(
    memorials: List<Memorial>,
    relations: List<MemorialRelation>
) {
    val nodeRadius = 48f
    val nodeSpacing = 180f
    val verticalSpacing = 220f
    // 1. Группируем мемориалы по ролям (родители, дети, супруги)
    val parentIds = relations.filter { it.relationType == RelationType.PARENT }.map { it.sourceMemorial.id }
    val childIds = relations.filter { it.relationType == RelationType.CHILD }.map { it.targetMemorial.id }
    val spousePairs = relations.filter { it.relationType == RelationType.SPOUSE }
    val parentLevel = memorials.filter { it.id in parentIds }.distinctBy { it.id }
    val childLevel = memorials.filter { it.id in childIds }.distinctBy { it.id }
    val spouseLevel = memorials.filter { m -> spousePairs.any { it.sourceMemorial.id == m.id || it.targetMemorial.id == m.id } }.distinctBy { it.id }
    val otherLevel = memorials.filter { it.id !in parentIds && it.id !in childIds && spousePairs.none { it.sourceMemorial.id == it.id || it.targetMemorial.id == it.id } }

    // 2. Располагаем уровни: родители — сверху, дети — снизу, супруги и остальные — посередине
    val levels = listOf(parentLevel, spouseLevel + otherLevel, childLevel)
    val yOffsets = listOf(100f, 100f + verticalSpacing, 100f + 2 * verticalSpacing)
    // 3. Считаем позиции для каждого мемориала
    val nodePositions = mutableMapOf<Long?, Offset>()
    levels.forEachIndexed { levelIdx, levelList ->
        val count = levelList.size
        val totalWidth = (count - 1) * nodeSpacing
        levelList.forEachIndexed { idx, memorial ->
            val x = (nodeSpacing * idx) + (800f - totalWidth) / 2 // 800f — примерная ширина экрана
            val y = yOffsets[levelIdx]
            nodePositions[memorial.id] = Offset(x, y)
        }
    }
    Box(Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 4. Нарисовать связи (стрелки с цветом)
            relations.forEach { rel ->
                val source = nodePositions[rel.sourceMemorial.id]
                val target = nodePositions[rel.targetMemorial.id]
                if (source != null && target != null) {
                    val (color, arrowSize) = when (rel.relationType) {
                        RelationType.PARENT -> Color(0xFF1976D2) to 18f
                        RelationType.CHILD -> Color(0xFF388E3C) to 18f
                        RelationType.SPOUSE -> Color(0xFFFFA000) to 0f // без стрелки
                        else -> Color.Gray to 18f
                    }
                    // Линия
                    drawLine(
                        color = color,
                        start = source,
                        end = target,
                        strokeWidth = 6f
                    )
                    // Стрелка (кроме SPOUSE)
                    if (arrowSize > 0f) {
                        val angle = atan2(target.y - source.y, target.x - source.x)
                        val arrowX = target.x - nodeRadius * cos(angle)
                        val arrowY = target.y - nodeRadius * sin(angle)
                        val arrowTip = Offset(arrowX, arrowY)
                        val left = Offset(
                            arrowX - arrowSize * cos(angle - 0.4f),
                            arrowY - arrowSize * sin(angle - 0.4f)
                        )
                        val right = Offset(
                            arrowX - arrowSize * cos(angle + 0.4f),
                            arrowY - arrowSize * sin(angle + 0.4f)
                        )
                        drawLine(color, left, arrowTip, strokeWidth = 6f)
                        drawLine(color, right, arrowTip, strokeWidth = 6f)
                    }
                }
            }
            // 5. Нарисовать узлы (кружки)
            nodePositions.forEach { (_, pos) ->
                drawCircle(
                    color = Color(0xFF90CAF9),
                    center = pos,
                    radius = nodeRadius
                )
            }
        }
        // 6. Подписи к узлам (поверх Canvas)
        nodePositions.forEach { (id, pos) ->
            val memorial = memorials.find { it.id == id }
            if (memorial != null) {
                Box(
                    Modifier
                        .offset { IntOffset((pos.x - nodeRadius).toInt(), (pos.y + nodeRadius + 8f).toInt()) }
                        .width((nodeRadius * 2).dp)
                ) {
                    Text(
                        text = memorial.fio,
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
} 