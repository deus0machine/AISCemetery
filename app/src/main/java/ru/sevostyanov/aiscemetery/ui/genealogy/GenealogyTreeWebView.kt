package ru.sevostyanov.aiscemetery.ui.genealogy

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import ru.sevostyanov.aiscemetery.R
import ru.sevostyanov.aiscemetery.models.Memorial
import ru.sevostyanov.aiscemetery.models.MemorialRelation
import ru.sevostyanov.aiscemetery.models.RelationType

data class FamilyTreeNode(
    val id: Long,
    val name: String,
    val title: String,
    val gender: String,
    val img: String = "",
    val fid: Long? = null,  // father id
    val mid: Long? = null,  // mother id
    val pids: List<Long>? = null  // partner ids (spouses)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenealogyTreeWebView(
    memorials: List<Memorial>,
    relations: List<MemorialRelation>,
    onNodeClick: (Memorial) -> Unit = {}
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTheme by remember { mutableStateOf("hugo") }
    
    // Конвертируем данные в формат FamilyTree.js
    val familyTreeData = remember(memorials, relations) {
        Log.d("GenealogyTreeWebView", "Converting ${memorials.size} memorials and ${relations.size} relations")
        convertToFamilyTreeFormat(memorials, relations)
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Панель управления
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Семейное дерево",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Кнопка переключения темы
                var isDarkTheme by remember { mutableStateOf(false) }
                
                IconButton(
                    onClick = {
                        isDarkTheme = !isDarkTheme
                        val theme = if (isDarkTheme) "dark" else "light"
                        webView?.evaluateJavascript("toggleTheme('$theme');", null)
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isDarkTheme) R.drawable.sun else R.drawable.moon
                        ),
                        contentDescription = if (isDarkTheme) "Светлая тема" else "Темная тема",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // WebView с семейным деревом
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            AndroidView(
                factory = { context ->
                    createFamilyTreeWebView(context) { memorial ->
                        onNodeClick(memorial)
                    }.also { 
                        webView = it
                        // Загружаем данные после создания WebView
                        loadDataIntoWebView(it, familyTreeData)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Индикатор загрузки
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Загрузка семейного дерева...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
    
    // Скрываем индикатор загрузки через некоторое время
    LaunchedEffect(webView) {
        kotlinx.coroutines.delay(2000)
        isLoading = false
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createFamilyTreeWebView(
    context: Context,
    onNodeClick: (Memorial) -> Unit
): WebView {
    return WebView(context).apply {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(true)
            builtInZoomControls = false
            displayZoomControls = false
        }
        
        // JavaScript Interface для взаимодействия с Android
        addJavascriptInterface(
            FamilyTreeJavaScriptInterface(onNodeClick),
            "Android"
        )
        
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("GenealogyTreeWebView", "WebView page loaded")
            }
            
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }
        }
        
        // Загружаем HTML файл из assets
        loadUrl("file:///android_asset/family_tree.html")
    }
}

private fun loadDataIntoWebView(webView: WebView, data: List<FamilyTreeNode>) {
    val gson = Gson()
    val jsonData = gson.toJson(data)
    val escapedJson = jsonData.replace("'", "\\'")
    
    Log.d("GenealogyTreeWebView", "Preparing to load ${data.size} nodes into WebView")
    Log.d("GenealogyTreeWebView", "JSON data preview: ${jsonData.take(200)}...")
    
    // Загружаем данные в WebView с проверкой готовности (увеличиваем задержку)
    webView.postDelayed({
        webView.evaluateJavascript("""
            if (typeof loadFamilyData === 'function' && typeof family !== 'undefined') {
                loadFamilyData('$escapedJson');
            } else {
                setTimeout(function() {
                    if (typeof loadFamilyData === 'function' && typeof family !== 'undefined') {
                        loadFamilyData('$escapedJson');
                    } else {
                        console.error('Не удалось загрузить данные дерева');
                    }
                }, 1000);
                'Retrying data load';
            }
        """.trimIndent()) { result ->
            Log.d("GenealogyTreeWebView", "Data loading result: $result")
        }
    }, 3000)
}

// JavaScript Interface для взаимодействия между WebView и Android
private class FamilyTreeJavaScriptInterface(
    private val onNodeClick: (Memorial) -> Unit
) {
    @JavascriptInterface
    fun onTreeReady() {
        Log.d("GenealogyTreeWebView", "Family tree is ready")
    }
    
    @JavascriptInterface
    fun onNodeClick(nodeJson: String) {
        Log.d("GenealogyTreeWebView", "Node clicked: $nodeJson")
        // Здесь можно парсить данные узла и вызывать onNodeClick
    }
}

// Конвертация данных из модели приложения в формат FamilyTree.js
private fun convertToFamilyTreeFormat(
    memorials: List<Memorial>,
    relations: List<MemorialRelation>
): List<FamilyTreeNode> {
    val nodes = mutableListOf<FamilyTreeNode>()
    val memorialMap = memorials.associateBy { it.id!! }
    
    // Создаем карты связей
    val parentChildMap = mutableMapOf<Long, MutableSet<Long>>() // parent -> children
    val spouseMap = mutableMapOf<Long, MutableSet<Long>>() // person -> spouses
    
    relations.forEach { relation ->
        val sourceId = relation.sourceMemorial.id ?: return@forEach
        val targetId = relation.targetMemorial.id ?: return@forEach
        
        when (relation.relationType) {
            RelationType.PARENT -> {
                parentChildMap.getOrPut(sourceId) { mutableSetOf() }.add(targetId)
            }
            RelationType.CHILD -> {
                parentChildMap.getOrPut(targetId) { mutableSetOf() }.add(sourceId)
            }
            RelationType.SPOUSE -> {
                spouseMap.getOrPut(sourceId) { mutableSetOf() }.add(targetId)
                spouseMap.getOrPut(targetId) { mutableSetOf() }.add(sourceId)
            }
            else -> {}
        }
    }
    
    // Конвертируем каждого мемориала
    memorials.forEach { memorial ->
        val id = memorial.id!!
        
        // Определяем родителей
        var fatherId: Long? = null
        var motherId: Long? = null
        
        parentChildMap.forEach { (parentId, children) ->
            if (children.contains(id)) {
                val parent = memorialMap[parentId]
                if (parent != null) {
                    // Определяем пол родителя по ФИО
                    val parentFullName = parent.fio ?: ""
                    val parentGender = determineGender(parentFullName)
                    if (parentGender == "female") {
                        motherId = parentId
                    } else {
                        fatherId = parentId
                    }
                }
            }
        }
        
        // Получаем супругов
        val spouses = spouseMap[id]?.toList() ?: emptyList()
        
        // Формируем имя - используем поле fio из API
        val fullName = when {
            memorial.fio?.trim()?.isNotBlank() == true && memorial.fio != "null" -> memorial.fio.trim()
            memorial.firstName?.trim()?.isNotBlank() == true && memorial.firstName != "null" -> {
                val firstName = memorial.firstName.trim()
                val lastName = memorial.lastName?.trim()?.takeIf { it.isNotBlank() && it != "null" } ?: ""
                if (lastName.isNotEmpty()) "$firstName $lastName" else firstName
            }
            else -> "Имя не указано"
        }
        
        // Определяем пол по имени из ФИО
        val gender = determineGender(fullName)
        
        Log.d("GenealogyTreeWebView", "Determined gender for '$fullName': $gender")
        
        Log.d("GenealogyTreeWebView", "Processing memorial: fio='${memorial.fio}', firstName='${memorial.firstName}', lastName='${memorial.lastName}', result='$fullName'")
        
        // Формируем даты
        val birthDate = memorial.birthDate?.takeIf { it.isNotBlank() } ?: "?"
        val deathDate = memorial.deathDate?.takeIf { it.isNotBlank() } ?: ""
        val title = if (deathDate.isNotEmpty()) "$birthDate - $deathDate" else "$birthDate -"
        
        // Определяем изображение - используем фото мемориала или иконку вопроса
        val imageUrl = memorial.photoUrl?.takeIf { it.isNotBlank() } 
            ?: createQuestionMarkIcon()
        
        nodes.add(
            FamilyTreeNode(
                id = id,
                name = fullName,
                title = title,
                gender = gender,
                fid = fatherId,
                mid = motherId,
                pids = spouses.takeIf { it.isNotEmpty() },
                img = imageUrl
            )
        )
    }
    
    return nodes
}

/**
 * Определяет пол по ФИО
 */
private fun determineGender(fullName: String?): String {
    if (fullName.isNullOrBlank()) return "male"
    
    // Разбиваем ФИО на части
    val nameParts = fullName.trim().split("\\s+".toRegex())
    
    // Ищем имя (обычно второе слово)
    val firstName = when {
        nameParts.size >= 2 -> nameParts[1] // Второе слово - имя
        nameParts.size == 1 -> nameParts[0] // Если только одно слово
        else -> null
    }
    
    // Определяем пол по окончанию имени
    return if (firstName?.lowercase()?.let { name ->
        name.endsWith("а") || name.endsWith("я") || 
        name.endsWith("ия") || name.endsWith("ья") ||
        name.endsWith("на") || name.endsWith("ка") ||
        // Некоторые известные женские имена
        listOf("любовь", "нинель", "рахиль", "юдифь", "эсфирь", "мэри", "кармен").contains(name)
    } == true) {
        "female"
    } else {
        "male"
    }
}

/**
 * Создает SVG иконку знака вопроса для мемориалов без фото
 */
private fun createQuestionMarkIcon(): String {
    val svg = """
        <svg width="80" height="80" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="40" cy="40" r="40" fill="#F3F4F6"/>
            <circle cx="40" cy="40" r="35" fill="#E5E7EB" stroke="#D1D5DB" stroke-width="2"/>
            <text x="40" y="55" font-family="Arial, sans-serif" font-size="32" font-weight="bold" 
                  text-anchor="middle" fill="#6B7280">?</text>
        </svg>
    """.trimIndent()
    
    // Кодируем SVG в base64
    val encoded = android.util.Base64.encodeToString(svg.toByteArray(), android.util.Base64.NO_WRAP)
    return "data:image/svg+xml;base64,$encoded"
}