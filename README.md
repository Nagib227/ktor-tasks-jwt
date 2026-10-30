# Сервер задач на Ktor с JWT

Backend для списка задач: публичное чтение, запись только с JWT. Пользователи регистрируются по логину и паролю, пароль хранится как BCrypt-хэш. Данные живут в памяти и сбрасываются при перезапуске. При старте уже есть три задачи, пользователей нет.

## Требования

- JDK 17 или новее
- Интернет при первом запуске: Gradle скачает зависимости

## Запуск

В каталоге проекта:

```bat
gradlew.bat run
```

На Linux и macOS:

```bash
./gradlew run
```

Сервер слушает [http://localhost:8080](http://localhost:8080). Другой порт задаётся переменной `PORT`. Остановка: `Ctrl+C`.

Проверка без запуска сервера:

```bat
gradlew.bat test
```

## Маршруты

| Метод | Путь | Токен | Успех |
| --- | --- | --- | --- |
| POST | `/register` | не нужен | 201 |
| POST | `/login` | не нужен | 200 и JWT |
| GET | `/tasks` | не нужен | 200 |
| GET | `/tasks/{id}` | не нужен | 200 |
| POST | `/tasks` | `Authorization: Bearer` | 201 |
| PUT | `/tasks/{id}` | `Authorization: Bearer` | 200 |
| DELETE | `/tasks/{id}` | `Authorization: Bearer` | 204 |

JSON подключён через `ContentNegotiation` и `kotlinx.serialization`. Защищённые маршруты проверяет плагин `Authentication` с JWT-провайдером.

### Регистрация

```json
{ "login": "student", "password": "secret12" }
```

Логин: 3–32 символа, латиница, цифры, `_` и `-`. Регистр не важен, сохраняется в нижнем. Пароль: от 6 до 72 символов. В хранилище попадает только BCrypt-хэш. Ответ не содержит пароль:

```json
{ "id": 1, "login": "student" }
```

Повтор логина — `409`.

### Вход

Те же поля, что у регистрации. Пароль проверяется функцией `BCrypt.verifyer`, а не сравнением строк. В токене есть срок действия и claims `userId` и `login`. Claim `userId` сверяется с существующим пользователем.

```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": 1
}
```

Неверный логин или пароль — `401`. Токен действует 1 час.

### Задачи

Создание, без `id` — его назначает сервер:

```json
{ "title": "Проверить API", "done": false, "priority": "LOW" }
```

`done` по умолчанию `false`, `priority` по умолчанию `MEDIUM`. Значения приоритета: `LOW`, `MEDIUM`, `HIGH`.

Обновление заменяет все три поля, `id` в теле не передаётся:

```json
{ "title": "Готово", "done": true, "priority": "HIGH" }
```

`id` в пути — положительное целое. Пустое название — `400`. Нет задачи — `404`. Такое название уже есть — `409`.

## Коды ответов

| Код | Когда |
| --- | --- |
| 200 | Список, одна задача, вход, успешное обновление |
| 201 | Пользователь или задача созданы |
| 204 | Задача удалена, тела нет |
| 400 | Пустые поля, битый JSON, неверный `id` |
| 401 | Неверный пароль, нет токена, токен просрочен или без действующего `userId` |
| 404 | Нет задачи или маршрута |
| 409 | Логин или название уже заняты |

Ошибка:

```json
{ "error": "unauthorized", "message": "Нужен действительный токен в заголовке Authorization: Bearer" }
```

## Примеры

Подставьте свой токен вместо `TOKEN`.

PowerShell:

```powershell
curl.exe -X POST http://localhost:8080/register -H "Content-Type: application/json" -d '{"login":"student","password":"secret12"}'
curl.exe -X POST http://localhost:8080/login -H "Content-Type: application/json" -d '{"login":"student","password":"secret12"}'
curl.exe http://localhost:8080/tasks
curl.exe http://localhost:8080/tasks/1
curl.exe -X POST http://localhost:8080/tasks -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" -d '{"title":"Проверить API","priority":"LOW"}'
curl.exe -X PUT http://localhost:8080/tasks/4 -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" -d '{"title":"Готово","done":true,"priority":"HIGH"}'
curl.exe -X DELETE http://localhost:8080/tasks/4 -H "Authorization: Bearer TOKEN"
```

Linux и macOS:

```bash
curl -X POST http://localhost:8080/register -H "Content-Type: application/json" -d '{"login":"student","password":"secret12"}'
curl -X POST http://localhost:8080/login -H "Content-Type: application/json" -d '{"login":"student","password":"secret12"}'
curl http://localhost:8080/tasks
curl http://localhost:8080/tasks/1
curl -X POST http://localhost:8080/tasks -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" -d '{"title":"Проверить API","priority":"LOW"}'
curl -X PUT http://localhost:8080/tasks/4 -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" -d '{"title":"Готово","done":true,"priority":"HIGH"}'
curl -X DELETE http://localhost:8080/tasks/4 -H "Authorization: Bearer TOKEN"
```

Запрос записи без заголовка `Authorization` возвращает `401`. Чтение списка и задачи по `id` работает без токена.

## Стартовые задачи

| id | Название | done | priority |
| --- | --- | --- | --- |
| 1 | Изучить маршруты Ktor | false | HIGH |
| 2 | Подключить JSON-сериализацию | true | MEDIUM |
| 3 | Написать README | false | LOW |

## Структура

```text
ktor-tasks-jwt/
├── src/main/kotlin/ru/ithub/tasks/
│   ├── Application.kt
│   ├── model/            DTO задач, пользователей и ошибок
│   ├── routes/           маршруты CRUD, регистрации и входа
│   ├── security/         BCrypt, JWT, плагин Authentication
│   └── storage/          хранилища в памяти
└── src/test/kotlin/      проверки кодов, хэша и токена
```
