# Core Metrics — Fitness & Calorie Manager

## Requirements

- MySQL 8.0 with a database named `core_metrics`
- JDK 21 or newer (the Maven project targets Java 21 bytecode; it also runs on JDK 25)
- The included Maven Wrapper
- A browser and VS Code Live Server for the vanilla-JS frontend

## 1. Create the database

Run `database/schema.sql` once against your MySQL server, for example from PowerShell:

```powershell
Get-Content .\database\schema.sql | mysql -u root -p
```

**Important:** `schema.sql` drops and recreates the project's tables, view, function, procedures, and trigger. It deletes existing project data. Back up the database before running it again.

The active DAOs share one JDBC connection configuration. Defaults match the local setup supplied for this project:

- URL: `jdbc:mysql://localhost:3306/core_metrics?useSSL=false&allowPublicKeyRetrieval=true`
- Username: `root`
- Password: `181106`

To override the defaults in PowerShell before starting the backend:

```powershell
$env:CORE_METRICS_DB_URL = "jdbc:mysql://localhost:3306/core_metrics?useSSL=false&allowPublicKeyRetrieval=true"
$env:CORE_METRICS_DB_USERNAME = "root"
$env:CORE_METRICS_DB_PASSWORD = "181106"
```

You can also override them with JVM system properties named `coremetrics.db.url`, `coremetrics.db.username`, and `coremetrics.db.password`.

## 2. Start the Spring Boot API

Open PowerShell in `CoreMetricsAPI` (the folder containing `mvnw.cmd`) and run:

```powershell
.\mvnw.cmd clean spring-boot:run
```

The API listens at `http://localhost:8080/api`. The `clean` goal removes stale files from `target/` before compiling the current Java sources.

## 3. Start the frontend

Open the `frontend` folder in VS Code, start Live Server on `index.html`, and browse to:

```text
http://127.0.0.1:5500
```

The backend CORS configuration permits both `http://127.0.0.1:5500` and `http://localhost:5500` for `GET`, `POST`, `PUT`, `DELETE`, and preflight `OPTIONS` requests.

`frontend/test.html` is a separate API smoke-test page. It does not load the dashboard's `app.js` and can be opened directly to check the `/users` and `/exercises` endpoints.

## API surface

| Feature | Endpoints |
|---|---|
| Users | `GET/POST /users`, `GET/PUT/DELETE /users/{id}`, `PUT /users/{id}/weight` |
| Exercise dictionary | `GET/POST /exercises`, `GET /exercises/{userId}/summary` |
| Strength | `POST /workouts`, `GET /workouts/{userId}`, `GET /workouts/{userId}/max` |
| Cardio | `POST /cardio`, `GET /cardio/{userId}` |
| Diet | `POST /diet`, `GET /diet/{userId}`, `GET /diet/{userId}/above-average`, `GET /diet/{userId}/high-calorie-days?min=1800` |
| Routines | `POST /routines`, `GET /routines/{userId}`, `GET/POST /routines/{routineId}/exercises`, `DELETE /routines/{routineId}` |
| Analytics | `GET /analytics/{userId}?days=1`, `?days=7`, or `?days=30` |
| SQL insights | `GET /sql-insights/{userId}` (SQL function, weekly-summary procedure, nutrition view, cursor procedure) |

Analytics windows are calendar-day windows including today. Caloric deficit is estimated as `BMR × selected days + cardio calories burned − calorie intake`; this is a BMR-based estimate, not a complete TDEE calculation.
