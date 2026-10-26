# Deep Reader — Читалка с AI-анализом сленга, метафор и скрытого подтекста

Клиент-серверное веб-приложение, разработанное в строгом соответствии с методологией **The Twelve-Factor App (12 факторов)**.

## О проекте
Deep Reader решает проблему буквального словарного перевода художественной прозы. Сервис анализирует контекст произведений и предоставляет читателю интерактивные объяснения сленга, авторских метафор, идиом и скрытого подтекста при чтении оригинального текста.

### Стек технологий:
- **Backend:** Java 25, Spring Boot 4, Spring Data JPA, Hibernate 7, PostgreSQL Driver.
- **Frontend:** SPA (HTML5, CSS3, ES6 JavaScript) без тяжелых зависимостей (Node.js/npm не требуются), раздается встроенным сервером приложения по протоколу HTTP/1.1 REST.
- **Database:** PostgreSQL 16.
- **Testing:** JUnit 5, Spring Boot Test, MockMvc, Testcontainers (PostgreSQL 16 Alpine для 100% паритета СУБД).
- **Инфраструктура:** Docker, Docker Compose, Multi-stage Dockerfile.

---

## Быстрый запуск

### Вариант 1. Запуск через Docker Compose (Рекомендуемый)
Для запуска приложения и базы данных PostgreSQL одной командой:

```bash
docker compose up --build
```

После старта веб-интерфейс доступен в браузере по адресу:
**[http://localhost:8080](http://localhost:8080)**

---

### Вариант 2. Локальный запуск
Приложение автоматически считывает переменные окружения из файла `.env` при старте (благодаря встроенному загрузчику).
1. Убедитесь, что параметры подключения в `.env` соответствуют вашей локальной СУБД PostgreSQL.
2. Запустите приложение с помощью Maven Wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Откройте в браузере: **http://localhost:8080**

---

## Запуск автоматических тестов
Для прогона набора модульных и интеграционных тестов (с автоматическим запуском PostgreSQL 16 через Testcontainers):

```bash
./mvnw test
```

---

## REST API эндпоинты

### Книги (`/api/books`)
- `GET /api/books` — получить список книг (поддерживает `?search=...`)
- `GET /api/books/{id}` — получить книгу по ID
- `POST /api/books` — создать книгу
- `PUT /api/books/{id}` — обновить книгу
- `DELETE /api/books/{id}` — удалить книгу

### Главы (`/api/chapters`, `/api/books/{bookId}/chapters`)
- `GET /api/books/{bookId}/chapters` — список глав книги
- `GET /api/chapters/{id}` — получить главу и её текст
- `POST /api/books/{bookId}/chapters` — добавить главу к книге
- `PUT /api/chapters/{id}` — обновить главу
- `DELETE /api/chapters/{id}` — удалить главу

### Контекстные подсказки к сленгу и метафорам (`/api/annotations`)
- `GET /api/annotations` — список всех аннотаций
- `GET /api/books/{bookId}/annotations` — аннотации к конкретной книге
- `GET /api/chapters/{chapterId}/annotations` — аннотации к конкретной главе
- `POST /api/annotations` — создать подсказку/разбор фразы
- `PUT /api/annotations/{id}` — обновить подсказку
- `DELETE /api/annotations/{id}` — удалить подсказку

### Системные эндпоинты
- `GET /api/health` — статус здоровья сервиса и статус подключения к PostgreSQL
- `GET /api/stats` — общая аналитика и счетчики сущностей
