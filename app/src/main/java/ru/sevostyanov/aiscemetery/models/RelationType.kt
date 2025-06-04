package ru.sevostyanov.aiscemetery.models

enum class RelationType {
    PARENT, // Родитель
    CHILD, // Ребенок
    SPOUSE, // Супруг/супруга
    SIBLING, // Брат/сестра
    GRANDPARENT, // Дедушка/бабушка
    GRANDCHILD, // Внук/внучка
    UNCLE_AUNT, // Дядя/тетя
    NEPHEW_NIECE, // Племянник/племянница
    PLACEHOLDER // Временная связь для мемориала без семейных связей
} 