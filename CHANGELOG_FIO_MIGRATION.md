# 📝 План миграции ФИО: от одного поля к трём отдельным

## 🎯 Цель
Разделить единое поле `fio` на три отдельных поля: `firstName`, `lastName`, `middleName` с сохранением полной обратной совместимости.

## 📋 Этапы реализации

### ✅ **Этап 1: Клиентская сторона (ЗАВЕРШЕН)**

1. **Обновлена модель Memorial**
   - Добавлены поля `firstName`, `lastName`, `middleName`
   - Сохранено поле `fio` для совместимости
   - Добавлены утилитные методы `getFullName()`, `getShortName()`, `hasSeparateNameFields()`

2. **Создан UI компонент NameInputView**
   - Переключение между простым и раздельным режимами
   - Автоматическая валидация полей
   - Синхронизация данных между режимами
   - Предпросмотр полного и краткого ФИО

3. **Обновлена валидация**
   - Добавлены методы `validateFirstName()`, `validateLastName()`, `validateMiddleName()`
   - Групповая валидация `validateNameFields()`

4. **Интегрирован в EditMemorialActivity**
   - Заменено обычное поле ФИО на NameInputView
   - Обновлена логика сохранения для работы с новыми полями

### 🔄 **Этап 2: Серверная сторона (ТРЕБУЕТСЯ)**

**На сервере необходимо добавить:**

```java
// В сущность Memorial
@Column(name = "first_name", length = 50)
private String firstName;

@Column(name = "last_name", length = 50) 
private String lastName;

@Column(name = "middle_name", length = 50)
private String middleName;

// Сохранить существующее поле для совместимости
@Column(name = "fio", length = 255)
private String fio;

// Автоматическая синхронизация
@PrePersist
@PreUpdate
public void syncFioFields() {
    // Если пришли отдельные поля - собираем fio
    if (firstName != null && lastName != null) {
        fio = buildFullName(firstName, lastName, middleName);
    }
    // Если пришло только fio - разбираем на части
    else if (fio != null && firstName == null) {
        String[] parts = parseFio(fio);
        firstName = parts[0];
        lastName = parts[1]; 
        middleName = parts[2];
    }
}

private String buildFullName(String firstName, String lastName, String middleName) {
    StringBuilder sb = new StringBuilder();
    if (lastName != null) sb.append(lastName);
    if (firstName != null) {
        if (sb.length() > 0) sb.append(" ");
        sb.append(firstName);
    }
    if (middleName != null) {
        if (sb.length() > 0) sb.append(" ");
        sb.append(middleName);
    }
    return sb.toString();
}

private String[] parseFio(String fio) {
    String[] parts = fio.trim().split("\\s+");
    switch (parts.length) {
        case 1: return new String[]{parts[0], null, null};
        case 2: return new String[]{parts[1], parts[0], null};
        case 3: return new String[]{parts[1], parts[0], parts[2]};
        default: return new String[]{parts[1], parts[0], parts[2]};
    }
}
```

### 🎮 **Использование компонента**

```kotlin
// В XML layout
<ru.sevostyanov.aiscemetery.views.NameInputView
    android:id="@+id/name_input_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />

// В коде Activity/Fragment
nameInputView.setMemorial(memorial) // Загрузка данных
nameInputView.setOnNameChangedListener { fio, firstName, lastName, middleName ->
    // Обработка изменений
}

// Получение данных
val (fio, firstName, lastName, middleName) = nameInputView.getCurrentData()

// Валидация
val error = nameInputView.validate()
if (error != null) {
    // Обработка ошибки
}
```

## ✨ **Преимущества решения**

1. **🔒 Полная обратная совместимость** - старые клиенты продолжат работать
2. **🎛️ Гибкий UX** - пользователь выбирает удобный режим ввода
3. **✅ Умная валидация** - проверка как объединенного, так и отдельных полей
4. **🔄 Автосинхронизация** - данные синхронизируются между режимами
5. **📱 Переиспользуемость** - компонент можно использовать везде

## 🚀 **Текущий статус**

- ✅ Клиентская часть готова
- ⏳ Серверная часть требует реализации
- ⏳ Тестирование после серверных изменений

## 📋 **TODO**

1. Реализовать серверную часть
2. Создать SQL миграцию для новых полей
3. Протестировать совместимость API
4. Обновить документацию API
5. Постепенно мигрировать другие формы на новый компонент

---

*Создано: $(date)* 