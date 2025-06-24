# Руководство по миграции с H2 на PostgreSQL

## Обзор
Данное руководство поможет вам безболезненно перейти с базы данных H2 на PostgreSQL.

## Шаг 1: Установка PostgreSQL

### Windows:
1. Скачайте PostgreSQL: https://www.postgresql.org/download/windows/
2. Установите с настройками по умолчанию
3. Запомните пароль для пользователя `postgres`

### Linux:
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

## Шаг 2: Создание базы данных

```bash
psql -U postgres
```

```sql
CREATE DATABASE cemetery_db;
CREATE USER cemetery_user WITH PASSWORD 'cemetery_password';
GRANT ALL PRIVILEGES ON DATABASE cemetery_db TO cemetery_user;
\c cemetery_db
GRANT ALL ON SCHEMA public TO cemetery_user;
\q
```

## Шаг 3: Выполнение миграции

```bash
psql -U cemetery_user -d cemetery_db -f postgresql_migration.sql
```

## Шаг 4: Настройка приложения

Используйте профиль PostgreSQL:

```bash
mvn spring-boot:run -Dspring.profiles.active=postgresql
```

Или установите переменную окружения:
```bash
set SPRING_PROFILES_ACTIVE=postgresql
```

## Шаг 5: Тестирование

Тестовые пользователи:
- admin / password
- testuser / password

## Готово!

Ваше приложение теперь работает с PostgreSQL. 