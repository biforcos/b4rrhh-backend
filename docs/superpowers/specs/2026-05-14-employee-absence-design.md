# Employee Absence — Design Spec

## Goal

Implement the `employee.absence` vertical: registro de ausencias de empleado por días (preparado para horas), con catálogo de tipos en `rule_system`, upsert por clave funcional, validación de cobertura de presencia y no-solape.

---

## Architecture

Vertical nueva `employee.absence` con estructura hexagonal idéntica al resto de verticales del bounded context `employee`. Tres capas: dominio, aplicación, infraestructura.

**Tech Stack:** Java 21, Spring Boot, JPA/Hibernate, PostgreSQL, Flyway, H2 (tests).

---

## Domain Model

```java
Absence {
  Long         id                // surrogate, interno — nunca expuesto en API
  Long         employeeId        // FK employee.employee
  String       absenceTypeCode   // validated against EMPLOYEE_ABSENCE_TYPE catalog
  LocalDate    startDate
  Integer      startTime         // minutos del día [0–1439], NOT NULL, default 0
  LocalDate    endDate           // nullable — null = ausencia abierta
  Integer      endTime           // nullable — null = ausencia abierta o modo día
  LocalDateTime createdAt
  LocalDateTime updatedAt
}
```

**Business key funcional:** `(rule_system_code, employee_type_code, employee_number, absence_type_code, start_date, start_time)`

Es la clave del upsert y la identidad en la API. El `id` surrogado solo existe para JPA.

**Representación de tiempo:**
- Storage: `INTEGER` (minutos del día, 0–1439). Ejemplo: 9:00 = 540, 17:30 = 1050.
- API JSON: string `"HH:mm"`. Ejemplo: `"09:00"`, `"17:30"`.
- URL path: string `HHmm` (4 dígitos, sin separador). Ejemplo: `0900`, `1730`.
- Conversión en web mapper — el dominio nunca ve strings de hora.

**Invariantes del modelo:**
- `startTime` ∈ [0, 1439]; nunca null.
- `endTime` ∈ [0, 1439] si presente.
- `endDate >= startDate` si endDate presente.
- Si `endDate == startDate` y `endTime` informado → `endTime > startTime`.
- Para ausencias por días: `startTime = 0`, `endTime = null`.

---

## Rule System Catalog

Un único `rule_entity_type` nuevo:

```
EMPLOYEE_ABSENCE_TYPE
```

Seed baseline para **ESP** (todos con `start_date = 1900-01-01`, `active = true`):

| Código | Nombre |
|--------|--------|
| `VACATION` | Vacaciones |
| `IT_COMMON` | IT Contingencia Común |
| `IT_WORK_ACCIDENT` | IT Accidente de Trabajo / Enfermedad Profesional |
| `PARENTAL_LEAVE` | Permiso de nacimiento / adopción |
| `PAID_PERSONAL_LEAVE` | Permiso retribuido |
| `FORCE_MAJEURE` | Permiso por fuerza mayor |
| `UNPAID_LEAVE` | Excedencia / Permiso no retribuido |

---

## Use Cases

### UpsertAbsenceUseCase

```
UpsertAbsenceCommand(
  ruleSystemCode, employeeTypeCode, employeeNumber,
  absenceTypeCode,
  startDate, startTime,    ← clave funcional
  endDate, endTime         ← campos mutables (ambos nullable)
)
→ Absence
```

Busca por business key. Si existe → actualiza campos mutables (`endDate`, `endTime`). Si no → crea. Retorna `Absence` en ambos casos. El HTTP response code es siempre `200 OK`.

Validaciones en orden:

1. **Catálogo** — `absenceTypeCode` debe ser un `EMPLOYEE_ABSENCE_TYPE` activo en el rule_system. → `AbsenceCatalogValueInvalidException`
2. **Empleado activo** — el empleado debe existir y estar en estado `ACTIVE`. → `AbsenceEmployeeNotFoundException`
3. **Cobertura de presencia** — debe existir una presencia activa que cubra `startDate`:
   `presence.startDate <= absence.startDate` y (`presence.endDate == null` OR `presence.endDate >= absence.startDate`).
   Si `endDate` está informado, también debe caer dentro de la presencia. → `AbsenceOutsidePresencePeriodException`
4. **Rango de fechas** — `endDate >= startDate` si presente; si `endDate == startDate` y `endTime` informado → `endTime > startTime`. → `InvalidAbsenceDateRangeException`
5. **No solape** — ninguna otra ausencia del empleado puede solaparse con el rango `[startDate/startTime, endDate/endTime]`. En update se excluye la propia ausencia.
   Lógica de solape para modo día (`endTime = null`):
   ```
   A solapa B si:
     A.startDate <= (B.endDate ?? MAX_DATE) AND (A.endDate ?? MAX_DATE) >= B.startDate
   ```
   → `AbsenceOverlapException`

### GetAbsenceByBusinessKeyUseCase

```
GetAbsenceByBusinessKeyCommand(ruleSystemCode, employeeTypeCode, employeeNumber,
                               absenceTypeCode, startDate, startTime)
→ Absence
```

Lanza `AbsenceNotFoundException` si no existe.

### ListEmployeeAbsencesUseCase

```
ListEmployeeAbsencesCommand(ruleSystemCode, employeeTypeCode, employeeNumber)
→ List<Absence>
```

Ordenado por `startDate DESC, startTime DESC`. Sin paginación.

### DeleteAbsenceUseCase

```
DeleteAbsenceCommand(ruleSystemCode, employeeTypeCode, employeeNumber,
                     absenceTypeCode, startDate, startTime)
→ void
```

Lanza `AbsenceNotFoundException` si no existe. Hard delete.

---

## API

Base path: `/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/absences`

### Endpoints

```
PUT    .../absences/{absenceTypeCode}/{startDate}
PUT    .../absences/{absenceTypeCode}/{startDate}/{startTime}
GET    .../absences/{absenceTypeCode}/{startDate}
GET    .../absences/{absenceTypeCode}/{startDate}/{startTime}
GET    .../absences
DELETE .../absences/{absenceTypeCode}/{startDate}
DELETE .../absences/{absenceTypeCode}/{startDate}/{startTime}
```

`startTime` es opcional en el path — si omitido, se asume `0` (modo día).
Formato en path: `HHmm` (4 dígitos). Ejemplos: `0900`, `1730`.

### PUT — Upsert

Request body:
```json
{
  "endDate": "2026-05-18",
  "endTime": null
}
```

Response `200 OK`:
```json
{
  "absenceTypeCode": "VACATION",
  "startDate": "2026-05-14",
  "startTime": "00:00",
  "endDate": "2026-05-18",
  "endTime": null,
  "createdAt": "2026-05-14T09:00:00",
  "updatedAt": "2026-05-14T09:00:00"
}
```

### GET one — `200 OK` / `404`

Mismo response body que PUT.

### GET list — `200 OK`

Array de `AbsenceResponse`, ordenado `startDate DESC, startTime DESC`.

### DELETE — `204 No Content` / `404`

### Error responses

Siguen el patrón `AbsenceErrorResponse` del resto de verticales. Códigos HTTP:

| Excepción | HTTP |
|-----------|------|
| `AbsenceNotFoundException` | 404 |
| `AbsenceCatalogValueInvalidException` | 422 |
| `AbsenceEmployeeNotFoundException` | 422 |
| `AbsenceOutsidePresencePeriodException` | 422 |
| `InvalidAbsenceDateRangeException` | 422 |
| `AbsenceOverlapException` | 409 |

---

## Package Structure

```
com.b4rrhh.employee.absence
├── domain
│   ├── model/
│   │   └── Absence.java
│   ├── port/
│   │   └── AbsenceRepository.java
│   └── exception/
│       ├── AbsenceNotFoundException.java
│       ├── AbsenceOverlapException.java
│       ├── AbsenceCatalogValueInvalidException.java
│       ├── AbsenceOutsidePresencePeriodException.java
│       ├── AbsenceEmployeeNotFoundException.java
│       └── InvalidAbsenceDateRangeException.java
├── application
│   ├── usecase/
│   │   ├── UpsertAbsenceUseCase.java
│   │   ├── UpsertAbsenceService.java
│   │   ├── UpsertAbsenceCommand.java
│   │   ├── GetAbsenceByBusinessKeyUseCase.java
│   │   ├── GetAbsenceByBusinessKeyService.java
│   │   ├── GetAbsenceByBusinessKeyCommand.java
│   │   ├── ListEmployeeAbsencesUseCase.java
│   │   ├── ListEmployeeAbsencesService.java
│   │   ├── ListEmployeeAbsencesCommand.java
│   │   ├── DeleteAbsenceUseCase.java
│   │   ├── DeleteAbsenceService.java
│   │   └── DeleteAbsenceCommand.java
│   └── port/
│       ├── EmployeeAbsenceContext.java
│       └── EmployeeAbsenceLookupPort.java
└── infrastructure
    ├── persistence/
    │   ├── AbsenceEntity.java
    │   ├── SpringDataAbsenceRepository.java
    │   ├── AbsencePersistenceAdapter.java
    │   └── EmployeeAbsenceLookupAdapter.java
    └── web/
        ├── AbsenceBusinessKeyController.java
        ├── AbsenceWebMapper.java
        ├── AbsenceExceptionHandler.java
        └── dto/
            ├── UpsertAbsenceRequest.java
            ├── AbsenceResponse.java
            └── AbsenceErrorResponse.java
```

---

## Secondary Ports (UpsertAbsenceService dependencies)

- `GetEmployeeByBusinessKeyUseCase` — validar que el empleado existe y está activo
- `EmployeePresenceLookupPort` *(vertical presence, ya existe)* — obtener presencia activa que cubra `startDate`
- `RuleEntityRepository` — validar `absenceTypeCode` en catálogo `EMPLOYEE_ABSENCE_TYPE`

`EmployeeAbsenceLookupPort` y `EmployeeAbsenceContext` definidos en `absence.application.port` son ports que el vertical de ausencia *expone* hacia otros verticales — en particular, hacia `AbsenceTerminationParticipant` en lifecycle.

---

## Flyway Migrations

| Versión | Contenido |
|---------|-----------|
| `V100` | Tabla `employee.employee_absence` — clave única `(employee_id, absence_type_code, start_date, start_time)` |
| `V101` | `rule_entity_type`: `EMPLOYEE_ABSENCE_TYPE` |
| `V102` | Seed ESP: 7 tipos de ausencia |

### V100 — esquema de tabla

```sql
create table employee.employee_absence (
    id                bigint generated by default as identity primary key,
    employee_id       bigint not null references employee.employee(id) on delete cascade,
    absence_type_code varchar(50) not null,
    start_date        date not null,
    start_time        integer not null default 0,
    end_date          date,
    end_time          integer,
    created_at        timestamp not null,
    updated_at        timestamp not null,
    constraint uk_employee_absence_business_key
        unique (employee_id, absence_type_code, start_date, start_time),
    constraint chk_absence_start_time check (start_time >= 0 and start_time <= 1439),
    constraint chk_absence_end_time   check (end_time is null or (end_time >= 0 and end_time <= 1439)),
    constraint chk_absence_end_date   check (end_date is null or end_date >= start_date)
);
```

---

## Lifecycle Integration — AbsenceTerminationParticipant

Al terminar un empleado, si tiene una ausencia abierta (`endDate = null`) se cierra automáticamente con `endDate = terminationDate`.

Se añade en `lifecycle.application.participant` como `AbsenceTerminationParticipant` con **order = 25** (entre WorkCenter=20 y CostCenter=30).

Sigue el patrón opcional: si no hay ausencia abierta, no hace nada. No almacena resultado en `TerminationContext` (ausencia cerrada no forma parte de `TerminateEmployeeResult`).

---

## What Does NOT Change

- Ningún endpoint existente se modifica.
- El patrón de `TerminateEmployeeService` y sus participantes existentes no se tocan hasta añadir `AbsenceTerminationParticipant`.
- `TerminateEmployeeResult` no incluye `closedAbsence` — la ausencia se cierra como efecto secundario, sin necesitar ser parte del resultado de lifecycle.
