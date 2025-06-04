package ru.sevostyanov.aiscemetery.util

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException

/**
 * Утилитный класс для валидации данных согласно серверным ограничениям
 */
object ValidationUtils {
    
    /**
     * Валидация ФИО
     */
    fun validateFio(fio: String): String? {
        val trimmed = fio.trim()
        return when {
            trimmed.isEmpty() -> "ФИО не может быть пустым"
            trimmed.length < 2 -> "ФИО должно содержать не менее 2 символов"
            trimmed.length > 255 -> "ФИО не должно превышать 255 символов"
            !trimmed.matches(Regex("^[a-zA-Zа-яА-ЯёЁ\\s\\-\\.]+$")) -> "ФИО может содержать только буквы, пробелы, дефисы и точки"
            else -> null
        }
    }
    
    /**
     * Валидация дат рождения и смерти
     */
    fun validateDates(birthDateStr: String?, deathDateStr: String?): String? {
        if (birthDateStr.isNullOrBlank()) {
            return "Дата рождения обязательна для заполнения"
        }
        
        val birthDate = try {
            LocalDate.parse(birthDateStr)
        } catch (e: DateTimeParseException) {
            return "Некорректная дата рождения"
        }
        
        val currentDate = LocalDate.now()
        
        // Проверка даты рождения
        if (birthDate.isAfter(currentDate)) {
            return "Дата рождения не может быть в будущем"
        }
        
        if (birthDate.isBefore(LocalDate.of(1800, 1, 1))) {
            return "Дата рождения слишком давняя"
        }
        
        // Проверка даты смерти, если указана
        if (!deathDateStr.isNullOrBlank()) {
            val deathDate = try {
                LocalDate.parse(deathDateStr)
            } catch (e: DateTimeParseException) {
                return "Некорректная дата смерти"
            }
            
            if (deathDate.isAfter(currentDate)) {
                return "Дата смерти не может быть в будущем"
            }
            
            if (deathDate.isBefore(birthDate)) {
                return "Дата смерти не может быть раньше даты рождения"
            }
            
            val age = Period.between(birthDate, deathDate).years
            if (age > 150) {
                return "Возраст человека не может превышать 150 лет"
            }
        }
        
        return null
    }
    
    /**
     * Валидация биографии
     */
    fun validateBiography(biography: String): String? {
        return when {
            biography.length > 5000 -> "Биография не должна превышать 5000 символов"
            else -> null
        }
    }
    
    /**
     * Валидация названия семейного дерева
     */
    fun validateTreeName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Название дерева не может быть пустым"
            trimmed.length < 2 -> "Название должно содержать не менее 2 символов"
            trimmed.length > 100 -> "Название не должно превышать 100 символов"
            else -> null
        }
    }
    
    /**
     * Валидация описания семейного дерева
     */
    fun validateTreeDescription(description: String): String? {
        return when {
            description.length > 1000 -> "Описание не должно превышать 1000 символов"
            else -> null
        }
    }
    
    /**
     * Валидация возрастной совместимости для семейных связей
     */
    fun validateRelationAgeCompatibility(
        sourceBirthDate: String?,
        targetBirthDate: String?,
        relationType: ru.sevostyanov.aiscemetery.models.RelationType
    ): String? {
        if (sourceBirthDate.isNullOrBlank() || targetBirthDate.isNullOrBlank()) {
            return null // Пропускаем проверку, если даты не указаны
        }
        
        val sourceBirth = try {
            LocalDate.parse(sourceBirthDate)
        } catch (e: DateTimeParseException) {
            return null
        }
        
        val targetBirth = try {
            LocalDate.parse(targetBirthDate)
        } catch (e: DateTimeParseException) {
            return null
        }
        
        val ageDifferenceYears = Period.between(sourceBirth, targetBirth).years
        
        return when (relationType) {
            ru.sevostyanov.aiscemetery.models.RelationType.PARENT -> {
                when {
                    ageDifferenceYears < 12 -> "Родитель должен быть старше ребенка минимум на 12 лет. Разница: ${kotlin.math.abs(ageDifferenceYears)} лет"
                    ageDifferenceYears > 80 -> "Слишком большая разница в возрасте для родственных отношений (${ageDifferenceYears} лет)"
                    else -> null
                }
            }
            
            ru.sevostyanov.aiscemetery.models.RelationType.CHILD -> {
                when {
                    ageDifferenceYears > -12 -> "Ребенок должен быть младше родителя минимум на 12 лет. Разница: ${kotlin.math.abs(ageDifferenceYears)} лет"
                    ageDifferenceYears < -80 -> "Слишком большая разница в возрасте для родственных отношений (${kotlin.math.abs(ageDifferenceYears)} лет)"
                    else -> null
                }
            }
            
            ru.sevostyanov.aiscemetery.models.RelationType.SPOUSE -> {
                when {
                    kotlin.math.abs(ageDifferenceYears) > 50 -> "Слишком большая разница в возрасте для супругов (${kotlin.math.abs(ageDifferenceYears)} лет)"
                    else -> null
                }
            }
            
            ru.sevostyanov.aiscemetery.models.RelationType.SIBLING -> {
                when {
                    kotlin.math.abs(ageDifferenceYears) > 30 -> "Слишком большая разница в возрасте для братьев/сестер (${kotlin.math.abs(ageDifferenceYears)} лет)"
                    else -> null
                }
            }
            
            ru.sevostyanov.aiscemetery.models.RelationType.GRANDPARENT -> {
                when {
                    ageDifferenceYears < 30 -> "Дедушка/бабушка должны быть старше внука/внучки минимум на 30 лет. Разница: ${kotlin.math.abs(ageDifferenceYears)} лет"
                    else -> null
                }
            }
            
            ru.sevostyanov.aiscemetery.models.RelationType.GRANDCHILD -> {
                when {
                    ageDifferenceYears > -30 -> "Внук/внучка должны быть младше дедушки/бабушки минимум на 30 лет. Разница: ${kotlin.math.abs(ageDifferenceYears)} лет"
                    else -> null
                }
            }
            
            else -> null // Для остальных типов связей не проверяем
        }
    }
} 