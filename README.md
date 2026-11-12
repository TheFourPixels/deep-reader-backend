# Deep Reader Backend — REST API для осознанного чтения с контекстным AI-анализом

Высоконагруженный бэкенд-сервис для мобильного приложения **Deep Reader**, спроектированный в строгом соответствии с методологией **The Twelve-Factor App (12 факторов)** и принципами Cloud Native.

Сервис предоставляет RESTful API для каталога книг, структурированных глав и интерактивных контекстных аннотаций (разъяснение сленга, авторских метафор, исторических аллюзий и скрытого подтекста художественных произведений).

---

## 🛠 Стек технологий

- **Язык и фреймворк:** Java 25, **Spring Boot 4.1.1** (Spring Data JPA, Spring Web, Validation, Actuator).
- **СУБД:** **PostgreSQL 16** с пулом соединений HikariCP.
- **Миграции базы данных:** **Liquibase** (`db.changelog-master.yaml`).
- **Контейнеризация и оркестрация:** Docker, Multi-stage Dockerfile, Docker Compose, Kubernetes (Minikube / Kustomize).
- **Сбор и централизация логов:** Logback Custom JSON Layout (MDC, `trace_id`, `span_id`), **Vector DaemonSet**, **VictoriaLogs**.
- **Метрики и мониторинг:** Micrometer, Spring Boot Actuator, **VictoriaMetrics**, **Grafana** (комплексный дашборд RED-метрик, JVM, HikariCP и SLA).
- **Алертинг:** **vmalert**, **Alertmanager** (бизнес- и системные правила алертов).
- **Тестирование:** JUnit 5, Spring Boot Test, MockMvc, Testcontainers.

---

## 🚀 Быстрый запуск

### 1. Запуск через Docker Compose (Backend + PostgreSQL)

```bash
docker compose up --build -d
```

После старта бэкенд API доступен по адресу: **http://localhost:8080/api/health**

---

### 2. Локальный запуск через Maven

1. Убедитесь, что запущен PostgreSQL и параметры подключения в `.env` корректны.
2. Запустите приложение:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Проверка доступности:
   ```bash
   curl http://localhost:8080/api/health
   ```

---

### 3. Развёртывание в Kubernetes (Kustomize)

#### Старт Minikube и сборка образа:
```bash
minikube start --driver=docker
minikube docker-env | Invoke-Expression   # PowerShell
# eval $(minikube docker-env)           # Linux / MacOS / GitBash

docker build -t deep-reader-app:latest .
```

#### Применение манифестов инфраструктуры и мониторинга:
```bash
kubectl apply -k k8s/
```

#### Проверка статуса подов:
```bash
kubectl get pods -n deep-reader
kubectl get pods -n monitoring
```

#### Проброс портов к сервисам:
```bash
# REST API бэкенда:
kubectl port-forward -n deep-reader service/deep-reader-app 8080:8080

# VictoriaLogs (vmui):
kubectl port-forward -n deep-reader service/victoria-logs 9428:9428

# Alertmanager:
kubectl port-forward -n deep-reader service/alertmanager 9093:9093

# Grafana (дашборды метрик и логов):
kubectl port-forward -n deep-reader service/grafana 3000:3000
```

---

## 📡 REST API Спецификация

Все эндпоинты API возвращают и принимают данные в формате `application/json`.

### Книги (`/api/books`)
| Метод | Эндпоинт | Описание |
| :--- | :--- | :--- |
| `GET` | `/api/books` | Получить список книг (поддерживает фильтрацию `?search=...`) |
| `GET` | `/api/books/{id}` | Получить детальную информацию о книге по ID |
| `POST` | `/api/books` | Создать новую книгу |
| `PUT` | `/api/books/{id}` | Обновить метаданные книги |
| `DELETE` | `/api/books/{id}` | Удалить книгу со всеми зависимыми главами и аннотациями |

### Главы (`/api/chapters`, `/api/books/{bookId}/chapters`)
| Метод | Эндпоинт | Описание |
| :--- | :--- | :--- |
| `GET` | `/api/books/{bookId}/chapters` | Список глав книги |
| `GET` | `/api/chapters/{id}` | Получить главу и её полный текст |
| `POST` | `/api/books/{bookId}/chapters` | Добавить главу к книге |
| `PUT` | `/api/chapters/{id}` | Обновить главу |
| `DELETE` | `/api/chapters/{id}` | Удалить главу |

### Контекстные аннотации (`/api/annotations`)
| Метод | Эндпоинт | Описание |
| :--- | :--- | :--- |
| `GET` | `/api/annotations` | Список всех аннотаций |
| `GET` | `/api/books/{bookId}/annotations` | Список аннотаций к выбранной книге |
| `GET` | `/api/chapters/{chapterId}/annotations` | Список аннотаций к выбранной главе |
| `POST` | `/api/books/{bookId}/annotations` | Добавить сноску/объяснение сленга или метафоры |
| `PUT` | `/api/annotations/{id}` | Обновить аннотацию |
| `DELETE` | `/api/annotations/{id}` | Удалить аннотацию |

### Системные и диагностические эндпоинты
| Метод | Эндпоинт | Описание |
| :--- | :--- | :--- |
| `GET` | `/api/health` | Проверка здоровья сервиса и доступности PostgreSQL (Liveness/Readiness) |
| `GET` | `/api/stats` | Агрегированная статистика базы (число книг, глав, аннотаций) |
| `GET` | `/actuator/prometheus` | Эндпоинт сбора метрик Prometheus / VictoriaMetrics |

---

## 📊 Наблюдаемость (Observability)

1. **Структурированные логи в JSON:**
   Каждое лог-сообщение содержит метки времени, уровень логирования, имя логгера, поток, MDC контекст (`trace_id`, `span_id`), HTTP-метод, URI, код ответа и время выполнения запроса. Агент **Vector** читает логи с подов и пересылает в **VictoriaLogs**.
2. **Метрики и дашборды:**
   Сервис экспортирует RED-метрики (Rate, Errors, Duration), кастомные бизнес-таймеры (длительность сохранения аннотаций), системные метрики JVM (память Heap, Non-Heap, GC, Threads) и метрики пула HikariCP. Дашборд импортируется в **Grafana** из `grafana-dashboard.json`.
3. **Алертинг:**
   **vmalert** оценивает PromQL-правила каждые 5 секунд и отправляет уведомления в **Alertmanager** при превышении порога ошибок 5xx (>1%), деградации latency (>300ms) или переполнении пула соединений БД (>80%).

---

## 🧪 Запуск тестов

```bash
./mvnw clean test
```
