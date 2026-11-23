# Core Metrics — End-to-End Audit and Repair Report

## Scope

Audited the active Spring Boot application under `CoreMetricsAPI/`, the vanilla frontend under `frontend/`, and `database/schema.sql`. The legacy root `src/` CLI application was intentionally excluded. The delivered project omits `.git/` and stale `CoreMetricsAPI/target/` output so the recipient's source tree is not polluted with generated state.

## Findings and fixes by file

### `CoreMetricsAPI/src/main/java/com/coremetrics/api/controller/fitnessController.java`

- Fixed the invalid raw generic `List>` return type on the exercise endpoint, which prevented compilation.
- Added/standardized REST endpoints for user create/read/update/delete, exercise dictionary access, strength, cardio, diet insights, routines, SQL-object insights, and analytics.
- Added full profile update (`weightKg`, `heightCm`, `age`, `gender`) while retaining the weight-only endpoint.
- Validated user ownership/existence and exercise type before accepting strength/cardio logs.
- Added null-body checks and numeric/range validation so malformed payloads produce a client error rather than a `NullPointerException` or misleading success.
- Aligned the cardio calorie maximum with the schema's `DECIMAL(6,2)` range (`0`–`9999.99`).
- Made analytics accept only 1, 7, or 30 days and use exactly that many calendar days including today; the previous end boundary could count an extra day while multiplying BMR by the requested number of days.
- Exposed the stored function, weekly summary procedure, nutrition view, and cursor procedure through `GET /api/sql-insights/{userId}`.

### `CoreMetricsAPI/src/main/java/coremetrics/dao/ExerciseDAO.java`

- Replaced the malformed generic collection type with `List<Map<String,Object>>` for exercise dictionary and summary data.
- Kept SQL result names aligned with the frontend camel-case properties (`exerciseId`, `metValue`, `createdByUid`, `maxWeightKg`).
- Preserved SQL `NULL` for an exercise with no logged max; this distinguishes “No logs” from a real logged 0 kg set.
- Uses shared JDBC connection configuration and propagates database errors instead of returning misleading empty data.

### `CoreMetricsAPI/src/main/java/coremetrics/dao/UserDAO.java`

- Centralized JDBC connection handling and added complete profile CRUD operations.
- Added invocation of `CalculateUserBMR` for the SQL-insights view.
- Uses generated keys and explicit result-column mappings; SQL failures now propagate to the API error handler.

### `CoreMetricsAPI/src/main/java/coremetrics/dao/WorkoutDAO.java`

- Uses generated keys for saved sets and orders workout history by timestamp and log ID.
- Groups max lifts by exercise ID as well as name, avoiding accidental merging of distinct exercises with identical names.
- Added the callable cursor-procedure query for all-time strength volume.

### `CoreMetricsAPI/src/main/java/coremetrics/dao/CardioLogDAO.java`

- Removed fallback MET/body-weight values that could silently record inaccurate calorie burns when lookups failed.
- Validates the computed/manual calories against the schema's `DECIMAL(6,2)` limit before insert.
- Orders cardio history chronologically newest-first and propagates JDBC failures.

### `CoreMetricsAPI/src/main/java/coremetrics/dao/DietLogDAO.java`

- Added structured access to the daily nutrition view and weekly-summary stored procedure.
- Orders diet history; provides above-average meals, high-calorie days, range intake, net calories, and deficit queries.
- Uses `long` for SQL calorie sums to avoid truncating aggregate values to a 32-bit integer.
- Propagates database errors instead of presenting query failures as empty history.

### `CoreMetricsAPI/src/main/java/coremetrics/dao/RoutineDAO.java`

- Returns structured `RoutineExercise` records instead of formatted strings, enabling the frontend to render defaults by exercise type.
- Uses generated routine IDs, supports atomic create-plus-first-mapping, and propagates failed inserts/deletes.
- Preserves the schema's cascade behavior for deleting a routine and its mappings.

### New shared backend files

- `coremetrics/dao/DatabaseConnection.java`: one JDBC configuration for all six DAOs. Defaults to `localhost:3306/core_metrics`, `root`, and `181106`; supports environment-variable and JVM-property overrides.
- `coremetrics/dao/DatabaseException.java`: unchecked wrapper so JDBC failures reach the REST exception handler.
- `com/coremetrics/api/ApiExceptionHandler.java`: returns clear 400 responses for invalid inputs and 409 for integrity conflicts; logs unexpected database failures server-side.
- `com/coremetrics/api/config/CorsConfig.java`: limits local development CORS to `127.0.0.1:5500` and `localhost:5500`, allowing `GET`, `POST`, `PUT`, `DELETE`, and preflight `OPTIONS`.
- `coremetrics/models/RoutineExercise.java`: structured mapping response with camel-case getters/setters.

### Models: `User.java`, `Workout.java`, `CardioLog.java`, `DietLog.java`, `Routine.java`

- No Jackson constructor/property mismatch was found in these five original models. Each already had a public no-argument constructor, getters/setters, and camel-case names consistent with the JSON/UI contract, so they were retained unchanged.

### `CoreMetricsAPI/src/main/java/coremetrics/utils/FitnessMath.java`

- Centralizes and validates the Mifflin–St Jeor BMR, Epley 1RM, and MET calorie calculations used by the backend.

### `CoreMetricsAPI/src/main/java/com/coremetrics/api/CoreMetricsApiApplication.java` and `src/main/resources/application.properties`

- Removed the obsolete root/hello-style behavior and kept the application focused on `/api` routes.
- Excluded Spring's datasource auto-configuration because the DAOs deliberately use raw `DriverManager` JDBC connections rather than an injected `DataSource`.
- Set the service port to 8080 and configured predictable Jackson date serialization.

### `database/schema.sql`

- Corrected the BMR function declaration: it reads current profile data, so it is not deterministic; added a `NOT FOUND` handler so an unknown user returns `NULL` instead of failing the function call.
- Aligned the strength table's `weight_kg` check with the controller and trigger, both of which permit `0 kg` sets.
- Moved the destructive-reset warning before the first `DROP` operation. **This script still drops and recreates project tables and SQL objects; back up existing data before running it.**
- The DAO SQL column names were checked against the table/view definitions. The trigger runs automatically on strength inserts; the other SQL objects are called/read by the new SQL-insights endpoint.

### `frontend/index.html`

- Added the missing Net Calories KPI.
- Expanded the edit-profile modal from weight-only to weight, height, age, and gender.
- Added visible SQL function/procedure/cursor results and the daily nutrition view table.
- Kept existing element IDs used by JavaScript and verified that all queried IDs exist uniquely.

### `frontend/app.js`

- Added consistent HTTP-status checking and API error display so failed saves are not reported as successful.
- Handles zero users by clearing user-specific KPIs, histories, routines, and charts and disabling user-dependent controls.
- Populates strength/cardio/routine dropdowns from `exercise_dict` and refreshes them after custom exercise creation.
- Sends complete profile updates and supports user deletion/switching.
- Renders separate Epley 1RM chart datasets per exercise, sorted chronologically; destroys old charts before redraw.
- Renders routine exercise defaults as structured fields, diet SQL insights, and all five analytics values.
- Escapes dynamic strings before inserting them into HTML and avoids stale routine data overwriting the current profile after a rapid user switch.

### `frontend/test.html`, `frontend/fake_api_response.json`, and `CoreMetricsAPI/Requests.http`

- Replaced the test page's dependency on the full dashboard script with an independent API smoke test, avoiding null-element errors when opened by itself.
- Changed the sample response keys to camel case so they reflect the API models.
- Updated HTTP client examples to the actual `/api` routes.

### Credentials

- No `Keshav@123` placeholder was found in the six active DAO files in the supplied archive; their configured password was already `181106`. The fix consolidates that setting in one shared helper so the DAOs cannot drift independently.

## Verification performed

- Compiled all active Java source files with `javac` against lightweight Spring API stubs to check Java syntax and type references. The only warning came from a temporary stub class, not project source.
- `node --check frontend/app.js` passed.
- Parsed `frontend/fake_api_response.json` and `CoreMetricsAPI/pom.xml` successfully.
- Checked `index.html` and `test.html`: no duplicate IDs; all `getElementById` references in `app.js` resolve to an element in `index.html`.
- Confirmed the active source contains no `Keshav@123` placeholder and no malformed `List>` type.

## Verification limits

A complete Maven build and live SQL integration test could not be run in this environment: Maven is not installed, the Maven Wrapper could not download its distribution in this sandbox, and no MySQL server is available here. The local JDK used for the source-level compile was Java 21, not the target machine's Java 25. Run the supplied clean Maven command and apply `database/schema.sql` against a backed-up MySQL instance on the Windows machine for final runtime verification.

## Calorie-estimate note

Cardio calories are estimated with `MET × body weight (kg) × duration (hours)`. Incline is stored with the session but is not an extra multiplier in that simple estimate. The analytics “deficit” is `BMR × days + cardio calories burned − calorie intake`; it is a BMR-based estimate, not a complete TDEE calculation.
