# 📖 Deep Reader Backend

[![Java](https://img.shields.io/badge/Java-25-orange.svg?style=flat&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%20UNLOGGED-blue.svg?style=flat&logo=postgresql)](https://www.postgresql.org/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0.3%20(FROZEN)-green.svg?style=flat&logo=openapi-initiative)](docs/specs/02_openapi.yaml)
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal%20%2F%20Ports%20%26%20Adapters-blueviolet.svg)](docs/architecture/SYSTEM_DESIGN.md)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)]()

> **Zero-Knowledge AI-Proxy для осознанного чтения художественной литературы.**  
> Высоконагруженный микросервис для мобильных и веб-читалок книг. Предоставляет интеллектуальный разбор фразеологизмов, метафор, сленга и культурных отсылок с защитой от спойлеров, быстрый перевод и двухуровневое кэширование (L1 in-memory + L2 PostgreSQL).

---

## ⚡ Ключевые возможности

- 🛡 **Zero-Knowledge & Copyright Safety:** Тексты книг и история чтения пользователей **никогда не сохраняются в базе данных**. В БД хранятся только SHA-256 хэши и кэшированные ответы с ограниченным TTL.
- 🎭 **Интеллектуальный литературный разбор (`POST /api/v1/explain`):** Анализирует контекст произведения через LLM (OpenAI GPT-4o-mini / локальные vLLM/Ollama), выявляет тип лингвистического явления (`metaphor`, `idiom`, `allusion` и др.) и стилистическую тональность без раскрытия будущего сюжета.
- 🌐 **Контекстный перевод (`POST /api/v1/translate`):** Мгновенный перевод слов и абзацев через DeepL API с автоопределением языка источника.
- ⚡ **Двухуровневое кэширование (L1 + L2):**
  - **L1 In-Memory (Caffeine):** задержка **< 1 мс** для популярных фрагментов в оперативной памяти экземпляра.
  - **L2 PostgreSQL UNLOGGED:** задержка **10–15 мс** для общего кэша между репликами в кластере без накладных расходов на журнал WAL.
- 🛡 **Комплексная отказоустойчивость (Resilience4j):** Circuit Breaker и Retry с экспоненциальным случайным jitter для изоляции внешних сбоев и предотвращения каскадных аварий.
- 📊 **Production Observability:** Структурированные JSON-логи с MDC `trace_id`, Prometheus-метрики (Micrometer), дашборды Grafana и готовые правила алертинга для `vmalert`.

---

## 🧭 Навигация по документации (`docs/`)

Вся расширенная проектная документация структурирована по ролям:

| Документ | Для кого | Описание |
| :--- | :--- | :--- |
| 📡 [**`docs/api/API_REFERENCE.md`**](docs/api/API_REFERENCE.md) | Frontend / Android / API | **Выделенный справочник чистого REST API**: все эндпоинты, обязательные/опциональные параметры, ограничения, примеры запросов и ответов (`200` и `4xx/5xx`). |
| 📱 [**`docs/mobile/ANDROID_INTEGRATION_GUIDE.md`**](docs/mobile/ANDROID_INTEGRATION_GUIDE.md) | Mobile / Android | Контракты API, правила подготовки контекста из EPUB, спойлер-безопасность, бейджи и UX ошибок. |
| 📐 [**`docs/architecture/SYSTEM_DESIGN.md`**](docs/architecture/SYSTEM_DESIGN.md) | Backend / Архитектура | Гексагональная архитектура, Mermaid-диаграммы, алгоритм обрезки контекста (`jtokkit`) и Resilience4j. |
| 🗄 [**`docs/database/DATABASE_SCHEMA.md`**](docs/database/DATABASE_SCHEMA.md) | Database / Backend | Схема таблицы `ai_cache_entries`, выбор UNLOGGED PostgreSQL, формулы SHA-256 ключей и TTL. |
| 🚀 [**`docs/operations/DEPLOYMENT_AND_RUNBOOK.md`**](docs/operations/DEPLOYMENT_AND_RUNBOOK.md) | DevOps / SRE | Запуск в Docker Compose / K8s, мониторинг (Grafana, VictoriaMetrics), формат логов и ранбук алертов. |
| 🤖 [**`docs/AI_DEVELOPMENT_RULES.md`**](docs/AI_DEVELOPMENT_RULES.md) | Все разработчики / AI | Правила разработки для AI-ассистентов, методология SDD, стандарты Zero-Knowledge и инварианты SRE. |
| 📚 [**`docs/README.md`**](docs/README.md) | Все разработчики | Единый навигационный хаб документации. |
| ❄️ [**`docs/specs/`**](docs/specs/) | Все | Замороженные канонические спецификации SDD (`01_architecture`, `02_openapi`, `03_ai_logic`). |

---

## 🗂 Структура репозитория

```text
.
├── docs/                                  # Полная проектная документация и спецификации SDD
│   ├── AI_DEVELOPMENT_RULES.md            # 🤖 Правила разработки, методология SDD и Zero-Knowledge
│   ├── api/                               # 📡 Справочник чистого REST API (API_REFERENCE.md)
│   ├── mobile/                            # Руководство по интеграции мобильного приложения
│   ├── architecture/                      # Системный дизайн, диаграммы и карта пакетов
│   ├── database/                          # Схема БД, двухуровневый кэш и Liquibase
│   ├── operations/                        # Инструкции по деплою, мониторингу и SRE-ранбук
│   └── specs/                             # ❄️ Замороженные контракты (Architecture, OpenAPI, AI Logic)
│
├── src/
│   ├── main/java/com/teamfourpixels/deepreaderbackend/
│   │   ├── domain/                        # Чистое ядро: модели, исключения, сервисы (BPE Tokenizer, Cache Key)
│   │   ├── port/                          # Порты: Inbound (Use Cases) и Outbound (SPI)
│   │   ├── application/                   # Сервисы сценариев: ExplainUseCaseImpl, TranslateUseCaseImpl
│   │   ├── adapter/                       # Адаптеры: Web REST, Caffeine L1, Postgres L2, OpenAI, DeepL
│   │   └── infrastructure/                # Метрики Micrometer, конфиги Resilience4j и RestClient
│   └── main/resources/                    # application.yaml, Liquibase changelog миграции
│
├── k8s/                                   # Манифесты Kubernetes (Kustomize, vmalert, VictoriaMetrics)
│   └── monitoring/                        # Дашборды Grafana (grafana-dashboard.json, postgres-dashboard.json)
├── docker-compose.yml                     # Локальный запуск сервиса и PostgreSQL
├── Dockerfile                             # Multi-stage сборка компактного контейнера
├── .env.example                           # Шаблон переменных окружения
└── pom.xml                                # Зависимости проекта (Spring Boot 4.1.1, Java 25)
```

---

## 🚀 Быстрый старт за 5 минут

### Вариант 1: Запуск через Docker Compose (Рекомендуемый)

```bash
# 1. Запуск сервиса и базы данных в фоне:
docker compose up --build -d

# 2. Проверка здоровья сервиса:
curl http://localhost:8080/api/v1/health
```

> [!TIP]
> **Хотите протестировать без API-ключей?**  
> Запустите в режиме заглушек: `AI_LLM_PROVIDER=mock AI_TRANSLATOR_PROVIDER=mock docker compose up -d`. Сервис будет использовать локальные Mock-адаптеры.

---

### Вариант 2: Локальный запуск через Maven

```bash
# 1. Скопируйте шаблон переменных окружения:
cp .env.example .env

# 2. Запустите приложение:
./mvnw spring-boot:run

# 3. Запуск тестов:
./mvnw clean test
```

---

## 📡 Примеры API-запросов (cURL)

Все контракты строго соответствуют спецификации [`02_openapi.yaml`](docs/specs/02_openapi.yaml). Подробные таблицы всех параметров и ошибок доступны в [**`docs/api/API_REFERENCE.md`**](docs/api/API_REFERENCE.md).

### 1. Литературный анализ фразы (`POST /api/v1/explain`)

```bash
curl -X POST http://localhost:8080/api/v1/explain \
  -H "Content-Type: application/json" \
  -d '{
    "book_id": "1984-george-orwell",
    "selected_text": "clocks were striking thirteen",
    "current_sentence": "It was a bright cold day in April, and the clocks were striking thirteen.",
    "surrounding_context": {
      "sentences_before": ["The hallway smelt of boiled cabbage and old rag mats."],
      "sentences_after": ["Winston Smith slipped quickly through the glass doors."],
      "paragraph": "It was a bright cold day in April, and the clocks were striking thirteen. Winston Smith slipped quickly through the glass doors of Victory Mansions.",
      "chapter_context": "Chapter 1"
    },
    "target_language": "ru"
  }'
```

**Ответ `200 OK`:**
```json
{
  "selected_text": "clocks were striking thirteen",
  "contextual_meaning": "Указание на антиутопическую искажённую реальность и 24-часовой формат времени.",
  "phenomenon_type": "metaphor",
  "explanation": "В реальности механические настенные часы отбивают максимум 12 ударов. Удар тринадцатого часа погружает читателя в тревожный мир тоталитарного контроля, где нарушены даже законы природы.",
  "stylistic_coloring": "literary",
  "spoiler_alert": false,
  "cached": false,
  "processing_time_ms": 1140
}
```

---

### 2. Контекстный перевод (`POST /api/v1/translate`)

```bash
curl -X POST http://localhost:8080/api/v1/translate \
  -H "Content-Type: application/json" \
  -d '{
    "text": "clocks were striking thirteen",
    "source_language": "en",
    "target_language": "ru"
  }'
```

**Ответ `200 OK`:**
```json
{
  "translated_text": "часы били тринадцать",
  "source_language": "en",
  "target_language": "ru",
  "cached": false
}
```

---

### 3. Проверка статуса сервиса (`GET /api/v1/health`)

```bash
curl -X GET http://localhost:8080/api/v1/health
```

**Ответ `200 OK`:**
```json
{
  "status": "UP",
  "service": "deep-reader-backend",
  "timestamp": "2026-11-13T12:00:00Z",
  "cache": "CONNECTED",
  "active_llm_provider": "OpenAiAdapter (gpt-4o-mini)"
}
```

---

## 🛠 Технологический стек

- **Core:** Java 25, Spring Boot 4.1.1 (Spring Web, Spring Data JPA, Validation, Actuator).
- **Database & Cache:** PostgreSQL 16 (UNLOGGED Table), Caffeine Cache, Liquibase.
- **External AI Integrations:** OpenAI API (`gpt-4o-mini`, JSON Mode), DeepL API.
- **Resilience:** Resilience4j (CircuitBreaker, Retry с экспоненциальным jitter).
- **Tokenizer:** BPE-токенизатор `com.knuddels:jtokkit` (O200K_BASE, CL100K_BASE).
- **Observability:** Micrometer, Prometheus, VictoriaMetrics, VictoriaLogs, Logback JSON Layout.
- **Testing:** JUnit 5, Mockito, MockMvc, MockRestServiceServer, Testcontainers (PostgreSQL 16).
