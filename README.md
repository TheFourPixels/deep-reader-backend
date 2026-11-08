# Deep Reader — Читалка с AI-анализом сленга, метафор и скрытого подтекста

Клиент-серверное веб-приложение, разработанное в строгом соответствии с методологией **The Twelve-Factor App (12 факторов)**.

## О проекте
Deep Reader решает проблему буквального словарного перевода художественной прозы. Сервис анализирует контекст произведений и предоставляет читателю интерактивные объяснения сленга, авторских метафор, идиом и скрытого подтекста при чтении оригинального текста.

### Стек технологий:
- **Backend:** Java 25, Spring Boot 4, Spring Data JPA, Hibernate 7, PostgreSQL Driver.
- **Frontend:** SPA (HTML5, CSS3, ES6 JavaScript) без тяжёлых зависимостей (Node.js/npm не требуются), раздаётся встроенным сервером приложения по протоколу HTTP/1.1 REST.
- **Database:** PostgreSQL 16.
- **Testing:** JUnit 5, Spring Boot Test, MockMvc, Testcontainers
- **Инфраструктура:** Docker, Docker Compose, Multi-stage Dockerfile, Kubernetes (Minikube).
- **Логирование и наблюдаемость:** Victoria Logs, Vector (DaemonSet), Grafana (плагин VictoriaLogs).

---

## Быстрый запуск

### Запуск через Docker Compose
Для запуска приложения и базы данных PostgreSQL одной командой:

```bash
docker compose up --build
```
После старта веб-интерфейс доступен в браузере по адресу:
**[http://localhost:8080](http://localhost:8080)**

---

### Локальный запуск

1. Убедитесь, что параметры подключения в `.env` соответствуют вашей локальной СУБД PostgreSQL
2. Запустите приложение с помощью:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Откройте в браузере: **http://localhost:8080**

---

### Развёртывание в Minikube

#### Запуск Minikube
```bash
minikube start --driver=docker
```

#### Использование Docker-демона Minikube
Для Windows PowerShell:
```powershell
minikube docker-env | Invoke-Expression
```
Для GitBash / WSL:
```bash
eval $(minikube docker-env)
```
После этого `docker build` будет создавать образ непосредственно в кластере Minikube.

#### Сборка образа
```bash
docker build -t deep-reader-app:latest .
```
*Если вы предпочитаете собрать образ локально, используйте `docker build -t deep-reader-app:latest .` и затем загрузите его в Minikube:* 
```bash
docker build -t deep-reader-app:latest .
minikube image load deep-reader-app:latest
```

#### Применение манифестов Kubernetes
```bash
kubectl apply -k k8s/
```

#### Проверка состояния
```bash
kubectl get all
kubectl get pods -w 
```

#### Масштабирование backend-приложения
```bash
kubectl scale deployment deep-reader-app --replicas=3
kubectl get pods
```
Можно увидеть три пода `deep-reader-app`

#### Доступ к UI приложения
```bash
minikube service deep-reader-app  -  откроет браузер по http://<minikube-ip>:30080

kubectl port-forward service/deep-reader-app 8080:8080
```
Приложение будет доступно по адресу **http://localhost:8080**

---

### Централизованное логирование (Victoria Logs & Vector & Grafana)

Система логирования развёрнута в том же кластере:
- **Victoria Logs:** единое высокопроизводительное хранилище логов с веб-интерфейсом `vmui`.
- **Vector (DaemonSet):** агент сбора логов с хоста Kubernetes (`/var/log/pods`), автоматический парсинг JSON из `stdout` приложения и пересылка в Victoria Logs (`/insert/jsonline`).
- **Grafana:** визуализация логов с предустановленным официальным плагином `victoriametrics-logs-datasource`.

#### Доступ к интерфейсам логирования:
```bash
# Веб-интерфейс Victoria Logs (vmui):
kubectl port-forward service/victoria-logs 9428:9428
# Открыть в браузере: http://localhost:9428/select/vmui/

# Grafana (Explore с источником VictoriaLogs):
kubectl port-forward service/grafana 3000:3000
# Открыть в браузере: http://localhost:3000/explore
```

Подробное описание архитектуры, выбор шиппера и разбор 4 запросов LogsQL приведены в [Отчёт.md](Отчёт.md).

---

## Запуск автоматических тестов
Для прогона набора модульных и интеграционных тестов:

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

---

## Примечания
- Порт, на котором слушает Spring Boot, задаётся переменной окружения `PORT`, по умолчанию `8080`. В манифесте `ConfigMap`-а задаётся `PORT: "8080"` и Service `deep-reader-app` открывается как `NodePort` 30080.
- Секреты хранятся в `Secret` `deep-reader-secret`. При работе в продакшн-окружении используются **Sealed Secrets**, но тк это тестовый проект, то пока что просто секреты :)

---
