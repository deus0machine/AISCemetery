# Руководство по миграции с H2 на PostgreSQL

## Обзор

Данное руководство поможет вам безболезненно перейти с базы данных H2 на PostgreSQL в системе управления кладбищем.

## Предварительные требования

1. **PostgreSQL 12+** установлен и запущен
2. **Java 17+** 
3. **Maven 3.6+**
4. Права администратора для создания базы данных

## Шаг 1: Установка и настройка PostgreSQL

### Windows:
1. Скачайте PostgreSQL с официального сайта: https://www.postgresql.org/download/windows/
2. Установите PostgreSQL с настройками по умолчанию
3. Запомните пароль для пользователя `postgres`

### Linux (Ubuntu/Debian):
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

## Шаг 2: Создание базы данных и пользователя

Подключитесь к PostgreSQL как суперпользователь:

```bash
psql -U postgres
```

Выполните следующие команды:

```sql
-- Создание базы данных
CREATE DATABASE cemetery_db;

-- Создание пользователя
CREATE USER cemetery_user WITH PASSWORD 'cemetery_password';

-- Предоставление прав
GRANT ALL PRIVILEGES ON DATABASE cemetery_db TO cemetery_user;

-- Подключение к созданной базе
\c cemetery_db

-- Предоставление прав на схему
GRANT ALL ON SCHEMA public TO cemetery_user;
```

## Шаг 3: Выполнение SQL-скрипта миграции

Выполните SQL-скрипт для создания структуры базы данных:

```bash
psql -U cemetery_user -d cemetery_db -f postgresql_migration.sql
```

## Шаг 4: Обновление конфигурации приложения

Используйте готовый файл `application-postgresql.properties` и запускайте приложение с профилем:

```bash
java -jar CemeterySystem.jar --spring.profiles.active=postgresql
```

## Шаг 5: Обновление зависимостей

PostgreSQL драйвер уже добавлен в `pom.xml`. Пересоберите проект:

```bash
cd CemeterySystem
mvn clean install
```

## Шаг 6: Тестирование

1. **Запустите приложение:**
   ```bash
   mvn spring-boot:run --spring.profiles.active=postgresql
   ```

2. **Тестовые данные:**
   - Логин: `admin`, пароль: `password`
   - Логин: `testuser`, пароль: `password`

## Возможные проблемы и решения

### Проблема: "relation does not exist"
**Решение:** Убедитесь, что SQL-скрипт выполнен полностью и все таблицы созданы.

### Проблема: "password authentication failed"
**Решение:** Проверьте правильность пароля и настройки `pg_hba.conf`.

### Проблема: "connection refused"
**Решение:** Убедитесь, что PostgreSQL запущен и слушает на порту 5432.

## Резервное копирование

```bash
pg_dump -U cemetery_user cemetery_db > backup_$(date +%Y%m%d).sql
```

## Заключение

После выполнения всех шагов ваше приложение будет работать с PostgreSQL вместо H2. 