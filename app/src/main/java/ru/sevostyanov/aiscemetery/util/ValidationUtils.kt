package ru.sevostyanov.aiscemetery.util

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException
import android.util.Patterns

/**
 * Утилитный класс для валидации данных согласно серверным ограничениям
 */
object ValidationUtils {
    
    /**
     * Валидация логина
     */
    fun validateLogin(login: String): String? {
        return when {
            login.isBlank() -> "Введите логин"
            login.length < 3 -> "Логин должен содержать минимум 3 символа"
            login.length > 50 -> "Логин не должен превышать 50 символов"
            !login.matches(Regex("^[a-zA-Z0-9._-]+$")) -> "Логин может содержать только буквы, цифры, точки, дефисы и подчеркивания"
            else -> null
        }
    }
    
    /**
     * Валидация пароля
     */
    fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Введите пароль"
            password.length < 4 -> "Пароль должен содержать минимум 4 символа"
            password.length > 100 -> "Пароль не должен превышать 100 символов"
            else -> null
        }
    }
    
    /**
     * Проверка надежности пароля (для регистрации)
     */
    fun validatePasswordStrength(password: String): String? {
        if (password.length < 6) {
            return "Для надежности используйте пароль длиной минимум 6 символов"
        }
        return null
    }
    
    /**
     * Валидация email адреса (если используется как логин)
     */
    fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Введите email"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Введите корректный email адрес"
            else -> null
        }
    }
    
    /**
     * Валидация ФИО
     */
    fun validateFio(fio: String?): String? {
        val trimmedFio = fio?.trim() ?: ""
        
        return when {
            trimmedFio.isEmpty() -> "ФИО не может быть пустым"
            trimmedFio.length < 2 -> "ФИО должно содержать минимум 2 символа"
            trimmedFio.length > 255 -> "ФИО не должно превышать 255 символов"
            trimmedFio.split(" ").filter { it.isNotBlank() }.size < 2 -> 
                "Введите полное ФИО (минимум имя и фамилия)"
            !trimmedFio.matches(Regex("^[а-яёА-ЯЁa-zA-Z\\s-]+$")) -> 
                "ФИО может содержать только буквы, пробелы и дефисы"
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
    
    // ========== Валидация отдельных полей ФИО ==========
    
    /**
     * Валидация имени (первое имя)
     */
    fun validateFirstName(firstName: String?): String? {
        val trimmed = firstName?.trim() ?: ""
        return when {
            trimmed.isEmpty() -> "Имя не может быть пустым"
            trimmed.length < 2 -> "Имя должно содержать минимум 2 символа"
            trimmed.length > 50 -> "Имя не должно превышать 50 символов"
            !trimmed.matches(Regex("^[а-яёА-ЯЁa-zA-Z-]+$")) -> 
                "Имя может содержать только буквы и дефисы"
            else -> null
        }
    }
    
    /**
     * Валидация фамилии
     */
    fun validateLastName(lastName: String?): String? {
        val trimmed = lastName?.trim() ?: ""
        return when {
            trimmed.isEmpty() -> "Фамилия не может быть пустой"
            trimmed.length < 2 -> "Фамилия должна содержать минимум 2 символа"
            trimmed.length > 50 -> "Фамилия не должна превышать 50 символов"
            !trimmed.matches(Regex("^[а-яёА-ЯЁa-zA-Z-]+$")) -> 
                "Фамилия может содержать только буквы и дефисы"
            else -> null
        }
    }
    
    /**
     * Валидация отчества (может быть пустым)
     */
    fun validateMiddleName(middleName: String?): String? {
        val trimmed = middleName?.trim() ?: ""
        if (trimmed.isEmpty()) return null // Отчество опциональное
        
        return when {
            trimmed.length < 2 -> "Отчество должно содержать минимум 2 символа"
            trimmed.length > 50 -> "Отчество не должно превышать 50 символов"
            !trimmed.matches(Regex("^[а-яёА-ЯЁa-zA-Z-]+$")) -> 
                "Отчество может содержать только буквы и дефисы"
            else -> null
        }
    }
    
    /**
     * Валидация всех полей ФИО как группы
     */
    fun validateNameFields(firstName: String?, lastName: String?, middleName: String?): String? {
        validateFirstName(firstName)?.let { return it }
        validateLastName(lastName)?.let { return it }
        validateMiddleName(middleName)?.let { return it }
        return null
    }

    /**
     * Валидация контактов (email или телефон)
     */
    fun validateContacts(contacts: String?): String? {
        val trimmedContacts = contacts?.trim() ?: ""
        
        return when {
            trimmedContacts.isEmpty() -> "Контакты не могут быть пустыми"
            trimmedContacts.length < 5 -> "Контакты должны содержать минимум 5 символов"
            trimmedContacts.length > 255 -> "Контакты не должны превышать 255 символов"
            !isValidEmailOrPhone(trimmedContacts) -> 
                "Введите корректный email или номер телефона"
            else -> null
        }
    }

    /**
     * Проверка email или телефона
     */
    private fun isValidEmailOrPhone(contact: String): Boolean {
        // Проверка email
        if (Patterns.EMAIL_ADDRESS.matcher(contact).matches()) {
            return true
        }
        
        // Проверка телефона (российские номера)
        val phonePattern = Regex("^(\\+7|8)?[\\s\\-]?\\(?\\d{3}\\)?[\\s\\-]?\\d{3}[\\s\\-]?\\d{2}[\\s\\-]?\\d{2}$")
        if (phonePattern.matches(contact)) {
            return true
        }
        
        return false
    }

    /**
     * Проверка email
     */
    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    /**
     * Проверка телефона
     */
    fun isValidPhone(phone: String): Boolean {
        val phonePattern = Regex("^(\\+7|8)?[\\s\\-]?\\(?\\d{3}\\)?[\\s\\-]?\\d{3}[\\s\\-]?\\d{2}[\\s\\-]?\\d{2}$")
        return phonePattern.matches(phone.trim())
    }
} 