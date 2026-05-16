# Employee Absence Vertical Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the `employee.absence` vertical — a hexagonal upsert-by-business-key absence registry with type catalog, presence-coverage validation, no-overlap enforcement, and lifecycle termination integration.

**Architecture:** New vertical `com.b4rrhh.employee.absence` with three layers (domain, application, infrastructure) mirroring the existing presence/working_time/contact patterns. Business key `(employee_id, absence_type_code, start_date, start_time)` drives all operations; surrogate `id` is JPA-internal only.

**Tech Stack:** Java 21, Spring Boot, JPA/Hibernate, PostgreSQL, Flyway, H2 (tests), JUnit 5, Mockito.

---

## File Map

### New Files

**Flyway migrations:**
- `src/main/resources/db/migration/V100__create_employee_absence_table.sql`
- `src/main/resources/db/migration/V101__seed_employee_absence_type.sql`
- `src/main/resources/db/migration/V102__seed_employee_absence_type_esp.sql`

**Domain layer:**
- `src/main/java/com/b4rrhh/employee/absence/domain/model/Absence.java`
- `src/main/java/com/b4rrhh/employee/absence/domain/port/AbsenceRepository.java`
- `src/main/java/com/b4rrhh/employee/absence/domain/exception/AbsenceNotFoundException.java`
- `src/main/java/com/b4rrhh/employee/absence/domain/exception/AbsenceOverlapException.java`
- `src/main/java/com/b4rrhh/employee/absence/domain/exception/AbsenceCatalogValueInvalidException.java`
- `src/main/java/com/b4rrhh/employee/absence/domain/exception/AbsenceOutsidePresencePeriodException.java`
- `src/main/java/com/b4rrhh/employee/absence/domain/exception/AbsenceEmployeeNotFoundException.java`
- `src/main/java/com/b4rrhh/employee/absence/domain/exception/InvalidAbsenceDateRangeException.java`

**Application layer — use cases:**
- `src/main/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceUseCase.java`
- `src/main/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceCommand.java`
- `src/main/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceService.java`
- `src/main/java/com/b4rrhh/employee/absence/application/usecase/GetAbsenceByBusinessKeyUseCase.java`
- `src/main/java/com/b4rrhh/employee/absence/application/usecase/GetAbsenceByBusinessKeyCommand.java`
- `src/main/java/com/b4rrhh/employee/absence/application/usecase/GetAbsenceByBusinessKeyService.java`
- `src/main/java/com/b4rrhh/employee/absence/application/usecase/ListEmployeeAbsencesUseCase.java`
- `src/main/java/com/b4rrhh/employee/absence/application/usecase/ListEmployeeAbsencesCommand.java`
- `src/main/java/com/b4rrhh/employee/absence/application/usecase/ListEmployeeAbsencesService.java`
- `src/main/java/com/b4rrhh/employee/absence/application/usecase/DeleteAbsenceUseCase.java`
- `src/main/java/com/b4rrhh/employee/absence/application/usecase/DeleteAbsenceCommand.java`
- `src/main/java/com/b4rrhh/employee/absence/application/usecase/DeleteAbsenceService.java`
- `src/main/java/com/b4rrhh/employee/absence/application/usecase/CloseOpenAbsenceAtTerminationUseCase.java`
- `src/main/java/com/b4rrhh/employee/absence/application/usecase/CloseOpenAbsenceAtTerminationService.java`

**Infrastructure — persistence:**
- `src/main/java/com/b4rrhh/employee/absence/infrastructure/persistence/AbsenceEntity.java`
- `src/main/java/com/b4rrhh/employee/absence/infrastructure/persistence/SpringDataAbsenceRepository.java`
- `src/main/java/com/b4rrhh/employee/absence/infrastructure/persistence/AbsencePersistenceAdapter.java`

**Infrastructure — web:**
- `src/main/java/com/b4rrhh/employee/absence/infrastructure/web/AbsenceBusinessKeyController.java`
- `src/main/java/com/b4rrhh/employee/absence/infrastructure/web/AbsenceWebMapper.java`
- `src/main/java/com/b4rrhh/employee/absence/infrastructure/web/AbsenceExceptionHandler.java`
- `src/main/java/com/b4rrhh/employee/absence/infrastructure/web/dto/UpsertAbsenceRequest.java`
- `src/main/java/com/b4rrhh/employee/absence/infrastructure/web/dto/AbsenceResponse.java`
- `src/main/java/com/b4rrhh/employee/absence/infrastructure/web/dto/AbsenceErrorResponse.java`

**Lifecycle integration:**
- `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/AbsenceTerminationParticipant.java`

**Tests:**
- `src/test/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceServiceTest.java`
- `src/test/java/com/b4rrhh/employee/absence/application/usecase/GetAbsenceByBusinessKeyServiceTest.java`
- `src/test/java/com/b4rrhh/employee/absence/application/usecase/ListEmployeeAbsencesServiceTest.java`
- `src/test/java/com/b4rrhh/employee/absence/application/usecase/DeleteAbsenceServiceTest.java`
- `src/test/java/com/b4rrhh/employee/absence/application/usecase/CloseOpenAbsenceAtTerminationServiceTest.java`
- `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/AbsenceTerminationParticipantTest.java`

### Modified Files

- `src/main/resources/openapi/personnel-administration-api.yaml` — add absence endpoints
- `src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/TerminateEmployeeServiceRollbackIntegrationTest.java` — add AbsenceTerminationParticipant bean

---

## Task 1: Flyway Migrations

**Files:**
- Create: `src/main/resources/db/migration/V100__create_employee_absence_table.sql`
- Create: `src/main/resources/db/migration/V101__seed_employee_absence_type.sql`
- Create: `src/main/resources/db/migration/V102__seed_employee_absence_type_esp.sql`

- [ ] **Step 1: Create V100 — absence table**

```sql
-- V100__create_employee_absence_table.sql
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

- [ ] **Step 2: Create V101 — EMPLOYEE_ABSENCE_TYPE rule entity type**

Look up an existing V101 reference first to confirm rule_entity_type seed format:

```bash
cat src/main/resources/db/migration/V89__seed_working_time_type.sql
```

Then write V101:

```sql
-- V101__seed_employee_absence_type.sql
insert into rulesystem.rule_entity_type (code, name, created_at, updated_at)
values ('EMPLOYEE_ABSENCE_TYPE', 'Employee Absence Type', now(), now());
```

> Note: Check the actual column names/table structure against an existing rule_entity_type seed if this format differs.

- [ ] **Step 3: Create V102 — ESP absence type seed**

```sql
-- V102__seed_employee_absence_type_esp.sql
insert into rulesystem.rule_entity (rule_system_code, rule_entity_type_code, code, name, active, start_date, created_at, updated_at)
values
    ('ESP', 'EMPLOYEE_ABSENCE_TYPE', 'VACATION',              'Vacaciones',                                        true, '1900-01-01', now(), now()),
    ('ESP', 'EMPLOYEE_ABSENCE_TYPE', 'IT_COMMON',             'IT Contingencia Común',                             true, '1900-01-01', now(), now()),
    ('ESP', 'EMPLOYEE_ABSENCE_TYPE', 'IT_WORK_ACCIDENT',      'IT Accidente de Trabajo / Enfermedad Profesional',  true, '1900-01-01', now(), now()),
    ('ESP', 'EMPLOYEE_ABSENCE_TYPE', 'PARENTAL_LEAVE',        'Permiso de nacimiento / adopción',                  true, '1900-01-01', now(), now()),
    ('ESP', 'EMPLOYEE_ABSENCE_TYPE', 'PAID_PERSONAL_LEAVE',   'Permiso retribuido',                                true, '1900-01-01', now(), now()),
    ('ESP', 'EMPLOYEE_ABSENCE_TYPE', 'FORCE_MAJEURE',         'Permiso por fuerza mayor',                          true, '1900-01-01', now(), now()),
    ('ESP', 'EMPLOYEE_ABSENCE_TYPE', 'UNPAID_LEAVE',          'Excedencia / Permiso no retribuido',                true, '1900-01-01', now(), now());
```

> Note: Before writing, check V99 (`V99__seed_employee_numbering_config_esp.sql`) to confirm the exact rule_entity table column names.

- [ ] **Step 4: Verify migrations load via Maven**

```bash
mvn test -Dtest=HireEmployeeServiceRollbackIntegrationTest
```

Expected: BUILD SUCCESS (this test bootstraps H2 without Flyway, so any compilation errors surface here quickly).

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V100__create_employee_absence_table.sql
git add src/main/resources/db/migration/V101__seed_employee_absence_type.sql
git add src/main/resources/db/migration/V102__seed_employee_absence_type_esp.sql
git commit -m "feat(absence): add Flyway migrations V100-V102 (table + absence type catalog)"
```

---

## Task 2: Domain Layer

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/absence/domain/model/Absence.java`
- Create: `src/main/java/com/b4rrhh/employee/absence/domain/port/AbsenceRepository.java`
- Create: 6 exception files in `src/main/java/com/b4rrhh/employee/absence/domain/exception/`

- [ ] **Step 1: Write failing test for Absence domain model**

```java
// src/test/java/com/b4rrhh/employee/absence/domain/model/AbsenceTest.java
package com.b4rrhh.employee.absence.domain.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class AbsenceTest {

    @Test
    void createSetsFieldsAndNullsId() {
        Absence a = Absence.create(1L, "VACATION", LocalDate.of(2026, 5, 14), 0, null, null);
        assertNull(a.getId());
        assertEquals(1L, a.getEmployeeId());
        assertEquals("VACATION", a.getAbsenceTypeCode());
        assertEquals(LocalDate.of(2026, 5, 14), a.getStartDate());
        assertEquals(0, a.getStartTime());
        assertNull(a.getEndDate());
        assertNull(a.getEndTime());
        assertNotNull(a.getCreatedAt());
        assertNotNull(a.getUpdatedAt());
    }

    @Test
    void rehydratePreservesAllFields() {
        LocalDateTime ts = LocalDateTime.of(2026, 5, 1, 9, 0);
        Absence a = Absence.rehydrate(42L, 1L, "VACATION",
            LocalDate.of(2026, 5, 14), 540,
            LocalDate.of(2026, 5, 18), null, ts, ts);
        assertEquals(42L, a.getId());
        assertEquals(540, a.getStartTime());
        assertEquals(LocalDate.of(2026, 5, 18), a.getEndDate());
        assertEquals(ts, a.getCreatedAt());
    }

    @Test
    void updateMutatesEndDateAndEndTime() {
        Absence a = Absence.create(1L, "VACATION", LocalDate.of(2026, 5, 14), 0, null, null);
        a.update(LocalDate.of(2026, 5, 20), null);
        assertEquals(LocalDate.of(2026, 5, 20), a.getEndDate());
        assertNull(a.getEndTime());
    }

    @Test
    void closeAtSetsEndDateAndNullsEndTime() {
        Absence a = Absence.create(1L, "VACATION", LocalDate.of(2026, 5, 14), 0,
            null, null);
        a.closeAt(LocalDate.of(2026, 5, 31));
        assertEquals(LocalDate.of(2026, 5, 31), a.getEndDate());
        assertNull(a.getEndTime());
    }

    @Test
    void isOpenReturnsTrueWhenEndDateNull() {
        Absence a = Absence.create(1L, "VACATION", LocalDate.of(2026, 5, 14), 0, null, null);
        assertTrue(a.isOpen());
    }

    @Test
    void isOpenReturnsFalseWhenEndDateSet() {
        Absence a = Absence.create(1L, "VACATION", LocalDate.of(2026, 5, 14), 0,
            LocalDate.of(2026, 5, 18), null);
        assertFalse(a.isOpen());
    }
}
```

- [ ] **Step 2: Run test — verify it fails with class not found**

```bash
mvn test -Dtest=AbsenceTest
```

Expected: FAIL — `Absence` class does not exist.

- [ ] **Step 3: Create Absence domain model**

```java
// src/main/java/com/b4rrhh/employee/absence/domain/model/Absence.java
package com.b4rrhh.employee.absence.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Absence {

    private final Long id;
    private final Long employeeId;
    private final String absenceTypeCode;
    private final LocalDate startDate;
    private final int startTime;
    private LocalDate endDate;
    private Integer endTime;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Absence(Long id, Long employeeId, String absenceTypeCode,
                    LocalDate startDate, int startTime,
                    LocalDate endDate, Integer endTime,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.absenceTypeCode = absenceTypeCode;
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Absence create(Long employeeId, String absenceTypeCode,
                                  LocalDate startDate, int startTime,
                                  LocalDate endDate, Integer endTime) {
        LocalDateTime now = LocalDateTime.now();
        return new Absence(null, employeeId, absenceTypeCode,
            startDate, startTime, endDate, endTime, now, now);
    }

    public static Absence rehydrate(Long id, Long employeeId, String absenceTypeCode,
                                     LocalDate startDate, int startTime,
                                     LocalDate endDate, Integer endTime,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Absence(id, employeeId, absenceTypeCode,
            startDate, startTime, endDate, endTime, createdAt, updatedAt);
    }

    public void update(LocalDate endDate, Integer endTime) {
        this.endDate = endDate;
        this.endTime = endTime;
        this.updatedAt = LocalDateTime.now();
    }

    public void closeAt(LocalDate terminationDate) {
        this.endDate = terminationDate;
        this.endTime = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOpen() {
        return endDate == null;
    }

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public String getAbsenceTypeCode() { return absenceTypeCode; }
    public LocalDate getStartDate() { return startDate; }
    public int getStartTime() { return startTime; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getEndTime() { return endTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 4: Run test — verify it passes**

```bash
mvn test -Dtest=AbsenceTest
```

Expected: BUILD SUCCESS, 6 tests passing.

- [ ] **Step 5: Create AbsenceRepository port**

```java
// src/main/java/com/b4rrhh/employee/absence/domain/port/AbsenceRepository.java
package com.b4rrhh.employee.absence.domain.port;

import com.b4rrhh.employee.absence.domain.model.Absence;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AbsenceRepository {
    Optional<Absence> findByKey(Long employeeId, String absenceTypeCode,
                                LocalDate startDate, int startTime);
    List<Absence> findByEmployeeIdOrderByStartDateDescStartTimeDesc(Long employeeId);
    boolean existsOverlappingAbsence(Long employeeId, LocalDate startDate, LocalDate effectiveEndDate);
    boolean existsOverlappingAbsenceExcluding(Long employeeId, LocalDate startDate,
                                               LocalDate effectiveEndDate, Long excludeId);
    Absence save(Absence absence);
    void deleteByKey(Long employeeId, String absenceTypeCode, LocalDate startDate, int startTime);
}
```

- [ ] **Step 6: Create the 6 domain exceptions**

```java
// AbsenceNotFoundException.java
package com.b4rrhh.employee.absence.domain.exception;
public class AbsenceNotFoundException extends RuntimeException {
    public AbsenceNotFoundException(String message) { super(message); }
}

// AbsenceOverlapException.java
package com.b4rrhh.employee.absence.domain.exception;
public class AbsenceOverlapException extends RuntimeException {
    public AbsenceOverlapException(String message) { super(message); }
}

// AbsenceCatalogValueInvalidException.java
package com.b4rrhh.employee.absence.domain.exception;
public class AbsenceCatalogValueInvalidException extends RuntimeException {
    public AbsenceCatalogValueInvalidException(String field, String value) {
        super("Invalid absence catalog value for field '" + field + "': " + value);
    }
}

// AbsenceOutsidePresencePeriodException.java
package com.b4rrhh.employee.absence.domain.exception;
public class AbsenceOutsidePresencePeriodException extends RuntimeException {
    public AbsenceOutsidePresencePeriodException(String message) { super(message); }
}

// AbsenceEmployeeNotFoundException.java
package com.b4rrhh.employee.absence.domain.exception;
public class AbsenceEmployeeNotFoundException extends RuntimeException {
    public AbsenceEmployeeNotFoundException(String message) { super(message); }
}

// InvalidAbsenceDateRangeException.java
package com.b4rrhh.employee.absence.domain.exception;
public class InvalidAbsenceDateRangeException extends RuntimeException {
    public InvalidAbsenceDateRangeException(String message) { super(message); }
}
```

- [ ] **Step 7: Run full test suite to ensure no breakage**

```bash
mvn test
```

Expected: BUILD SUCCESS, all existing tests plus new AbsenceTest pass.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/absence/domain/
git add src/test/java/com/b4rrhh/employee/absence/domain/
git commit -m "feat(absence): add Absence domain model, AbsenceRepository port, and domain exceptions"
```

---

## Task 3: UpsertAbsenceService (TDD)

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceUseCase.java`
- Create: `src/main/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceCommand.java`
- Create: `src/main/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceService.java`
- Create: `src/test/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceServiceTest.java`

The UpsertAbsenceService validates in this order:
1. Catalog — `absenceTypeCode` is an active `EMPLOYEE_ABSENCE_TYPE` → `AbsenceCatalogValueInvalidException`
2. Employee active — employee exists and status is `ACTIVE` → `AbsenceEmployeeNotFoundException`
3. Presence coverage — startDate (and endDate if set) covered by an active presence → `AbsenceOutsidePresencePeriodException`
4. Date range — `endDate >= startDate`; if same day in hour-mode `endTime > startTime` → `InvalidAbsenceDateRangeException`
5. No overlap — no other absence spans the requested range → `AbsenceOverlapException`

Dependencies:
- `RuleEntityRepository` (already exists in `com.b4rrhh.rulesystem.domain.port`)
- `GetEmployeeByBusinessKeyUseCase` (already exists)
- `ListEmployeePresencesUseCase` (already exists)
- `AbsenceRepository` (new)

- [ ] **Step 1: Create UpsertAbsenceCommand**

```java
// src/main/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceCommand.java
package com.b4rrhh.employee.absence.application.usecase;

import java.time.LocalDate;

public record UpsertAbsenceCommand(
    String ruleSystemCode,
    String employeeTypeCode,
    String employeeNumber,
    String absenceTypeCode,
    LocalDate startDate,
    int startTime,
    LocalDate endDate,
    Integer endTime
) {}
```

- [ ] **Step 2: Create UpsertAbsenceUseCase interface**

```java
// src/main/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceUseCase.java
package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.model.Absence;

public interface UpsertAbsenceUseCase {
    Absence upsert(UpsertAbsenceCommand command);
}
```

- [ ] **Step 3: Write the failing test**

```java
// src/test/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceServiceTest.java
package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.exception.*;
import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpsertAbsenceServiceTest {

    @Mock private RuleEntityRepository ruleEntityRepository;
    @Mock private GetEmployeeByBusinessKeyUseCase getEmployee;
    @Mock private ListEmployeePresencesUseCase listPresences;
    @Mock private AbsenceRepository absenceRepository;

    @InjectMocks
    private UpsertAbsenceService service;

    private static final LocalDate MAY_14 = LocalDate.of(2026, 5, 14);
    private static final LocalDate MAY_18 = LocalDate.of(2026, 5, 18);

    private RuleEntity activeAbsenceType() {
        return new RuleEntity(1L, "ESP", "EMPLOYEE_ABSENCE_TYPE", "VACATION", "Vacaciones",
            null, true, LocalDate.of(1900, 1, 1), null, LocalDateTime.now(), LocalDateTime.now());
    }

    private Employee activeEmployee() {
        return new Employee(1L, "ESP", "INTERNAL", "EMP001",
            "Ana", "Lopez", null, null, "ACTIVE",
            LocalDateTime.now(), LocalDateTime.now(), null);
    }

    private Presence coveringPresence() {
        return new Presence(10L, 1L, 1, "COMP", "HIRE", null,
            LocalDate.of(2026, 1, 1), null, LocalDateTime.now(), LocalDateTime.now());
    }

    private UpsertAbsenceCommand command(LocalDate endDate, Integer endTime) {
        return new UpsertAbsenceCommand("ESP", "INTERNAL", "EMP001",
            "VACATION", MAY_14, 0, endDate, endTime);
    }

    @Test
    void throwsWhenAbsenceTypeCodeInvalid() {
        when(ruleEntityRepository.findByBusinessKey("ESP", "EMPLOYEE_ABSENCE_TYPE", "VACATION"))
            .thenReturn(Optional.empty());

        assertThrows(AbsenceCatalogValueInvalidException.class,
            () -> service.upsert(command(null, null)));
    }

    @Test
    void throwsWhenEmployeeNotFound() {
        when(ruleEntityRepository.findByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(activeAbsenceType()));
        when(getEmployee.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
            .thenReturn(Optional.empty());

        assertThrows(AbsenceEmployeeNotFoundException.class,
            () -> service.upsert(command(null, null)));
    }

    @Test
    void throwsWhenEmployeeNotActive() {
        Employee terminated = new Employee(1L, "ESP", "INTERNAL", "EMP001",
            "Ana", "Lopez", null, null, "TERMINATED",
            LocalDateTime.now(), LocalDateTime.now(), null);
        when(ruleEntityRepository.findByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(activeAbsenceType()));
        when(getEmployee.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
            .thenReturn(Optional.of(terminated));

        assertThrows(AbsenceEmployeeNotFoundException.class,
            () -> service.upsert(command(null, null)));
    }

    @Test
    void throwsWhenStartDateNotCoveredByPresence() {
        when(ruleEntityRepository.findByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(activeAbsenceType()));
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(activeEmployee()));
        Presence oldPresence = new Presence(10L, 1L, 1, "COMP", "HIRE", "QUIT",
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
            LocalDateTime.now(), LocalDateTime.now());
        when(listPresences.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
            .thenReturn(List.of(oldPresence));

        assertThrows(AbsenceOutsidePresencePeriodException.class,
            () -> service.upsert(command(null, null)));
    }

    @Test
    void throwsWhenEndDateBeforeStartDate() {
        when(ruleEntityRepository.findByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(activeAbsenceType()));
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(activeEmployee()));
        when(listPresences.listByEmployeeBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(List.of(coveringPresence()));

        UpsertAbsenceCommand badRange = new UpsertAbsenceCommand(
            "ESP", "INTERNAL", "EMP001", "VACATION",
            MAY_14, 0, LocalDate.of(2026, 5, 10), null);

        assertThrows(InvalidAbsenceDateRangeException.class, () -> service.upsert(badRange));
    }

    @Test
    void throwsWhenSameDayHourModeEndTimeNotAfterStartTime() {
        when(ruleEntityRepository.findByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(activeAbsenceType()));
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(activeEmployee()));
        when(listPresences.listByEmployeeBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(List.of(coveringPresence()));

        // startTime=540 (9:00), endDate=same day, endTime=480 (8:00) — invalid
        UpsertAbsenceCommand badTime = new UpsertAbsenceCommand(
            "ESP", "INTERNAL", "EMP001", "VACATION",
            MAY_14, 540, MAY_14, 480);

        assertThrows(InvalidAbsenceDateRangeException.class, () -> service.upsert(badTime));
    }

    @Test
    void throwsWhenOverlapExists() {
        when(ruleEntityRepository.findByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(activeAbsenceType()));
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(activeEmployee()));
        when(listPresences.listByEmployeeBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(List.of(coveringPresence()));
        when(absenceRepository.findByKey(anyLong(), anyString(), any(), anyInt()))
            .thenReturn(Optional.empty());
        when(absenceRepository.existsOverlappingAbsence(anyLong(), any(), any()))
            .thenReturn(true);

        assertThrows(AbsenceOverlapException.class, () -> service.upsert(command(MAY_18, null)));
    }

    @Test
    void createsAbsenceWhenKeyNotFound() {
        when(ruleEntityRepository.findByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(activeAbsenceType()));
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(activeEmployee()));
        when(listPresences.listByEmployeeBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(List.of(coveringPresence()));
        when(absenceRepository.findByKey(anyLong(), anyString(), any(), anyInt()))
            .thenReturn(Optional.empty());
        when(absenceRepository.existsOverlappingAbsence(anyLong(), any(), any()))
            .thenReturn(false);
        Absence saved = Absence.rehydrate(1L, 1L, "VACATION", MAY_14, 0, MAY_18, null,
            LocalDateTime.now(), LocalDateTime.now());
        when(absenceRepository.save(any())).thenReturn(saved);

        Absence result = service.upsert(command(MAY_18, null));

        assertNotNull(result);
        verify(absenceRepository).save(any());
    }

    @Test
    void updatesExistingAbsenceWhenKeyFound() {
        Absence existing = Absence.rehydrate(99L, 1L, "VACATION", MAY_14, 0,
            null, null, LocalDateTime.now(), LocalDateTime.now());
        when(ruleEntityRepository.findByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(activeAbsenceType()));
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(activeEmployee()));
        when(listPresences.listByEmployeeBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(List.of(coveringPresence()));
        when(absenceRepository.findByKey(anyLong(), anyString(), any(), anyInt()))
            .thenReturn(Optional.of(existing));
        when(absenceRepository.existsOverlappingAbsenceExcluding(anyLong(), any(), any(), eq(99L)))
            .thenReturn(false);
        when(absenceRepository.save(any())).thenReturn(existing);

        Absence result = service.upsert(command(MAY_18, null));

        assertNotNull(result);
        verify(absenceRepository, never()).save(argThat(a -> a.getId() == null));
    }
}
```

- [ ] **Step 4: Run test — verify it fails**

```bash
mvn test -Dtest=UpsertAbsenceServiceTest
```

Expected: FAIL — `UpsertAbsenceService` does not exist.

- [ ] **Step 5: Implement UpsertAbsenceService**

```java
// src/main/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceService.java
package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.exception.*;
import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class UpsertAbsenceService implements UpsertAbsenceUseCase {

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final RuleEntityRepository ruleEntityRepository;
    private final GetEmployeeByBusinessKeyUseCase getEmployee;
    private final ListEmployeePresencesUseCase listPresences;
    private final AbsenceRepository absenceRepository;

    public UpsertAbsenceService(RuleEntityRepository ruleEntityRepository,
                                 GetEmployeeByBusinessKeyUseCase getEmployee,
                                 ListEmployeePresencesUseCase listPresences,
                                 AbsenceRepository absenceRepository) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.getEmployee = getEmployee;
        this.listPresences = listPresences;
        this.absenceRepository = absenceRepository;
    }

    @Override
    public Absence upsert(UpsertAbsenceCommand cmd) {
        // 1. Catalog validation
        ruleEntityRepository.findByBusinessKey(cmd.ruleSystemCode(), "EMPLOYEE_ABSENCE_TYPE", cmd.absenceTypeCode())
            .orElseThrow(() -> new AbsenceCatalogValueInvalidException("absenceTypeCode", cmd.absenceTypeCode()));

        // 2. Employee active validation
        Employee employee = getEmployee.getByBusinessKey(cmd.ruleSystemCode(), cmd.employeeTypeCode(), cmd.employeeNumber())
            .filter(e -> "ACTIVE".equals(e.getStatus()))
            .orElseThrow(() -> new AbsenceEmployeeNotFoundException(
                "Employee not found or not active: " + cmd.ruleSystemCode() + "/" + cmd.employeeTypeCode() + "/" + cmd.employeeNumber()));

        // 3. Presence coverage validation
        validatePresenceCoverage(cmd, employee.getId());

        // 4. Date range validation
        validateDateRange(cmd);

        // 5. Overlap check and upsert
        LocalDate effectiveEndDate = cmd.endDate() != null ? cmd.endDate() : MAX_DATE;
        Optional<Absence> existing = absenceRepository.findByKey(
            employee.getId(), cmd.absenceTypeCode(), cmd.startDate(), cmd.startTime());

        if (existing.isPresent()) {
            Absence absence = existing.get();
            if (absenceRepository.existsOverlappingAbsenceExcluding(
                    employee.getId(), cmd.startDate(), effectiveEndDate, absence.getId())) {
                throw new AbsenceOverlapException("Absence overlaps with an existing absence record.");
            }
            absence.update(cmd.endDate(), cmd.endTime());
            return absenceRepository.save(absence);
        } else {
            if (absenceRepository.existsOverlappingAbsence(
                    employee.getId(), cmd.startDate(), effectiveEndDate)) {
                throw new AbsenceOverlapException("Absence overlaps with an existing absence record.");
            }
            Absence absence = Absence.create(employee.getId(), cmd.absenceTypeCode(),
                cmd.startDate(), cmd.startTime(), cmd.endDate(), cmd.endTime());
            return absenceRepository.save(absence);
        }
    }

    private void validatePresenceCoverage(UpsertAbsenceCommand cmd, Long employeeId) {
        List<Presence> presences = listPresences.listByEmployeeBusinessKey(
            cmd.ruleSystemCode(), cmd.employeeTypeCode(), cmd.employeeNumber());

        boolean startCovered = presences.stream().anyMatch(p ->
            !p.getStartDate().isAfter(cmd.startDate()) &&
            (p.getEndDate() == null || !p.getEndDate().isBefore(cmd.startDate())));

        if (!startCovered) {
            throw new AbsenceOutsidePresencePeriodException(
                "No active presence covers startDate " + cmd.startDate());
        }

        if (cmd.endDate() != null) {
            boolean endCovered = presences.stream().anyMatch(p ->
                !p.getStartDate().isAfter(cmd.endDate()) &&
                (p.getEndDate() == null || !p.getEndDate().isBefore(cmd.endDate())));
            if (!endCovered) {
                throw new AbsenceOutsidePresencePeriodException(
                    "No active presence covers endDate " + cmd.endDate());
            }
        }
    }

    private void validateDateRange(UpsertAbsenceCommand cmd) {
        if (cmd.endDate() != null && cmd.endDate().isBefore(cmd.startDate())) {
            throw new InvalidAbsenceDateRangeException(
                "endDate must be >= startDate");
        }
        if (cmd.endDate() != null && cmd.endDate().equals(cmd.startDate())
                && cmd.endTime() != null && cmd.endTime() <= cmd.startTime()) {
            throw new InvalidAbsenceDateRangeException(
                "endTime must be > startTime when endDate equals startDate");
        }
    }
}
```

- [ ] **Step 6: Run test — verify all pass**

```bash
mvn test -Dtest=UpsertAbsenceServiceTest
```

Expected: BUILD SUCCESS, 9 tests passing.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceUseCase.java
git add src/main/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceCommand.java
git add src/main/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceService.java
git add src/test/java/com/b4rrhh/employee/absence/application/usecase/UpsertAbsenceServiceTest.java
git commit -m "feat(absence): add UpsertAbsenceService with full validation chain (TDD)"
```

---

## Task 4: Get, List, Delete, and CloseAtTermination Services (TDD)

**Files:**
- Create: 4 use case interfaces + commands + services
- Create: 4 test files
- Create: `CloseOpenAbsenceAtTerminationUseCase.java`
- Create: `CloseOpenAbsenceAtTerminationService.java`
- Create: `src/test/java/com/b4rrhh/employee/absence/application/usecase/CloseOpenAbsenceAtTerminationServiceTest.java`

- [ ] **Step 1: Create commands, interfaces, and write failing tests**

```java
// GetAbsenceByBusinessKeyCommand.java
package com.b4rrhh.employee.absence.application.usecase;
import java.time.LocalDate;
public record GetAbsenceByBusinessKeyCommand(
    String ruleSystemCode, String employeeTypeCode, String employeeNumber,
    String absenceTypeCode, LocalDate startDate, int startTime) {}

// GetAbsenceByBusinessKeyUseCase.java
package com.b4rrhh.employee.absence.application.usecase;
import com.b4rrhh.employee.absence.domain.model.Absence;
public interface GetAbsenceByBusinessKeyUseCase {
    Absence getByBusinessKey(GetAbsenceByBusinessKeyCommand command);
}

// ListEmployeeAbsencesCommand.java
package com.b4rrhh.employee.absence.application.usecase;
public record ListEmployeeAbsencesCommand(
    String ruleSystemCode, String employeeTypeCode, String employeeNumber) {}

// ListEmployeeAbsencesUseCase.java
package com.b4rrhh.employee.absence.application.usecase;
import com.b4rrhh.employee.absence.domain.model.Absence;
import java.util.List;
public interface ListEmployeeAbsencesUseCase {
    List<Absence> listByEmployeeBusinessKey(ListEmployeeAbsencesCommand command);
}

// DeleteAbsenceCommand.java
package com.b4rrhh.employee.absence.application.usecase;
import java.time.LocalDate;
public record DeleteAbsenceCommand(
    String ruleSystemCode, String employeeTypeCode, String employeeNumber,
    String absenceTypeCode, LocalDate startDate, int startTime) {}

// DeleteAbsenceUseCase.java
package com.b4rrhh.employee.absence.application.usecase;
public interface DeleteAbsenceUseCase {
    void delete(DeleteAbsenceCommand command);
}

// CloseOpenAbsenceAtTerminationUseCase.java
package com.b4rrhh.employee.absence.application.usecase;
import java.time.LocalDate;
public interface CloseOpenAbsenceAtTerminationUseCase {
    void closeIfOpen(String ruleSystemCode, String employeeTypeCode,
                     String employeeNumber, LocalDate terminationDate);
}
```

- [ ] **Step 2: Write tests for all four services**

```java
// GetAbsenceByBusinessKeyServiceTest.java
package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.exception.AbsenceNotFoundException;
import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAbsenceByBusinessKeyServiceTest {

    @Mock private GetEmployeeByBusinessKeyUseCase getEmployee;
    @Mock private AbsenceRepository absenceRepository;
    @InjectMocks private GetAbsenceByBusinessKeyService service;

    private static final LocalDate MAY_14 = LocalDate.of(2026, 5, 14);

    private Employee employee() {
        return new Employee(1L, "ESP", "INTERNAL", "EMP001",
            "Ana", "Lopez", null, null, "ACTIVE",
            LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void returnsAbsenceWhenFound() {
        when(getEmployee.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
            .thenReturn(Optional.of(employee()));
        Absence absence = Absence.rehydrate(1L, 1L, "VACATION", MAY_14, 0, null, null,
            LocalDateTime.now(), LocalDateTime.now());
        when(absenceRepository.findByKey(1L, "VACATION", MAY_14, 0))
            .thenReturn(Optional.of(absence));

        Absence result = service.getByBusinessKey(
            new GetAbsenceByBusinessKeyCommand("ESP", "INTERNAL", "EMP001", "VACATION", MAY_14, 0));

        assertNotNull(result);
        assertEquals("VACATION", result.getAbsenceTypeCode());
    }

    @Test
    void throwsAbsenceNotFoundWhenEmployeeNotFound() {
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.empty());

        assertThrows(AbsenceNotFoundException.class, () -> service.getByBusinessKey(
            new GetAbsenceByBusinessKeyCommand("ESP", "INTERNAL", "EMP001", "VACATION", MAY_14, 0)));
    }

    @Test
    void throwsAbsenceNotFoundWhenAbsenceMissing() {
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(employee()));
        when(absenceRepository.findByKey(anyLong(), anyString(), any(), anyInt()))
            .thenReturn(Optional.empty());

        assertThrows(AbsenceNotFoundException.class, () -> service.getByBusinessKey(
            new GetAbsenceByBusinessKeyCommand("ESP", "INTERNAL", "EMP001", "VACATION", MAY_14, 0)));
    }
}
```

```java
// ListEmployeeAbsencesServiceTest.java
package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListEmployeeAbsencesServiceTest {

    @Mock private GetEmployeeByBusinessKeyUseCase getEmployee;
    @Mock private AbsenceRepository absenceRepository;
    @InjectMocks private ListEmployeeAbsencesService service;

    @Test
    void returnsEmptyListWhenNoAbsences() {
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(new Employee(1L, "ESP", "INTERNAL", "EMP001",
                "Ana", "Lopez", null, null, "ACTIVE", LocalDateTime.now(), LocalDateTime.now(), null)));
        when(absenceRepository.findByEmployeeIdOrderByStartDateDescStartTimeDesc(1L))
            .thenReturn(List.of());

        List<Absence> result = service.listByEmployeeBusinessKey(
            new ListEmployeeAbsencesCommand("ESP", "INTERNAL", "EMP001"));

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsAbsencesWhenEmployeeFound() {
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(new Employee(1L, "ESP", "INTERNAL", "EMP001",
                "Ana", "Lopez", null, null, "ACTIVE", LocalDateTime.now(), LocalDateTime.now(), null)));
        Absence a = Absence.rehydrate(1L, 1L, "VACATION", LocalDate.of(2026, 5, 14), 0,
            null, null, LocalDateTime.now(), LocalDateTime.now());
        when(absenceRepository.findByEmployeeIdOrderByStartDateDescStartTimeDesc(1L))
            .thenReturn(List.of(a));

        List<Absence> result = service.listByEmployeeBusinessKey(
            new ListEmployeeAbsencesCommand("ESP", "INTERNAL", "EMP001"));

        assertEquals(1, result.size());
    }

    @Test
    void returnsEmptyListWhenEmployeeNotFound() {
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.empty());

        List<Absence> result = service.listByEmployeeBusinessKey(
            new ListEmployeeAbsencesCommand("ESP", "INTERNAL", "UNKNOWN"));

        assertTrue(result.isEmpty());
    }
}
```

```java
// DeleteAbsenceServiceTest.java
package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.exception.AbsenceNotFoundException;
import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteAbsenceServiceTest {

    @Mock private GetEmployeeByBusinessKeyUseCase getEmployee;
    @Mock private AbsenceRepository absenceRepository;
    @InjectMocks private DeleteAbsenceService service;

    private static final LocalDate MAY_14 = LocalDate.of(2026, 5, 14);

    private Employee employee() {
        return new Employee(1L, "ESP", "INTERNAL", "EMP001",
            "Ana", "Lopez", null, null, "ACTIVE",
            LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void deletesWhenFound() {
        when(getEmployee.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
            .thenReturn(Optional.of(employee()));
        Absence absence = Absence.rehydrate(1L, 1L, "VACATION", MAY_14, 0,
            null, null, LocalDateTime.now(), LocalDateTime.now());
        when(absenceRepository.findByKey(1L, "VACATION", MAY_14, 0))
            .thenReturn(Optional.of(absence));

        service.delete(new DeleteAbsenceCommand("ESP", "INTERNAL", "EMP001", "VACATION", MAY_14, 0));

        verify(absenceRepository).deleteByKey(1L, "VACATION", MAY_14, 0);
    }

    @Test
    void throwsWhenNotFound() {
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(employee()));
        when(absenceRepository.findByKey(anyLong(), anyString(), any(), anyInt()))
            .thenReturn(Optional.empty());

        assertThrows(AbsenceNotFoundException.class, () ->
            service.delete(new DeleteAbsenceCommand("ESP", "INTERNAL", "EMP001", "VACATION", MAY_14, 0)));
    }

    @Test
    void throwsWhenEmployeeNotFound() {
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.empty());

        assertThrows(AbsenceNotFoundException.class, () ->
            service.delete(new DeleteAbsenceCommand("ESP", "INTERNAL", "UNKNOWN", "VACATION", MAY_14, 0)));
    }
}
```

```java
// CloseOpenAbsenceAtTerminationServiceTest.java
package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloseOpenAbsenceAtTerminationServiceTest {

    @Mock private GetEmployeeByBusinessKeyUseCase getEmployee;
    @Mock private AbsenceRepository absenceRepository;
    @InjectMocks private CloseOpenAbsenceAtTerminationService service;

    private static final LocalDate MAY_1 = LocalDate.of(2026, 5, 1);
    private static final LocalDate MAY_31 = LocalDate.of(2026, 5, 31);

    private Employee employee() {
        return new Employee(1L, "ESP", "INTERNAL", "EMP001",
            "Ana", "Lopez", null, null, "ACTIVE",
            LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void closesOpenAbsenceAtTerminationDate() {
        when(getEmployee.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
            .thenReturn(Optional.of(employee()));
        Absence openAbsence = Absence.rehydrate(1L, 1L, "VACATION", MAY_1, 0,
            null, null, LocalDateTime.now(), LocalDateTime.now());
        when(absenceRepository.findByEmployeeIdOrderByStartDateDescStartTimeDesc(1L))
            .thenReturn(List.of(openAbsence));

        service.closeIfOpen("ESP", "INTERNAL", "EMP001", MAY_31);

        ArgumentCaptor<Absence> captor = ArgumentCaptor.forClass(Absence.class);
        verify(absenceRepository).save(captor.capture());
        assertEquals(MAY_31, captor.getValue().getEndDate());
        assertNull(captor.getValue().getEndTime());
    }

    @Test
    void doesNothingWhenNoOpenAbsence() {
        when(getEmployee.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
            .thenReturn(Optional.of(employee()));
        Absence closed = Absence.rehydrate(1L, 1L, "VACATION", MAY_1, 0,
            LocalDate.of(2026, 5, 15), null, LocalDateTime.now(), LocalDateTime.now());
        when(absenceRepository.findByEmployeeIdOrderByStartDateDescStartTimeDesc(1L))
            .thenReturn(List.of(closed));

        service.closeIfOpen("ESP", "INTERNAL", "EMP001", MAY_31);

        verify(absenceRepository, never()).save(any());
    }

    @Test
    void doesNothingWhenEmployeeNotFound() {
        when(getEmployee.getByBusinessKey(anyString(), anyString(), anyString()))
            .thenReturn(Optional.empty());

        service.closeIfOpen("ESP", "INTERNAL", "UNKNOWN", MAY_31);

        verify(absenceRepository, never()).save(any());
    }
}
```

- [ ] **Step 3: Run tests — verify they fail**

```bash
mvn test -Dtest=GetAbsenceByBusinessKeyServiceTest,ListEmployeeAbsencesServiceTest,DeleteAbsenceServiceTest,CloseOpenAbsenceAtTerminationServiceTest
```

Expected: FAIL — service classes do not exist.

- [ ] **Step 4: Implement all four services**

```java
// GetAbsenceByBusinessKeyService.java
package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.exception.AbsenceNotFoundException;
import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import org.springframework.stereotype.Service;

@Service
public class GetAbsenceByBusinessKeyService implements GetAbsenceByBusinessKeyUseCase {

    private final GetEmployeeByBusinessKeyUseCase getEmployee;
    private final AbsenceRepository absenceRepository;

    public GetAbsenceByBusinessKeyService(GetEmployeeByBusinessKeyUseCase getEmployee,
                                           AbsenceRepository absenceRepository) {
        this.getEmployee = getEmployee;
        this.absenceRepository = absenceRepository;
    }

    @Override
    public Absence getByBusinessKey(GetAbsenceByBusinessKeyCommand command) {
        Long employeeId = getEmployee.getByBusinessKey(
                command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber())
            .map(e -> e.getId())
            .orElseThrow(() -> new AbsenceNotFoundException(
                "Absence not found for employee: " + command.employeeNumber()));

        return absenceRepository.findByKey(
                employeeId, command.absenceTypeCode(), command.startDate(), command.startTime())
            .orElseThrow(() -> new AbsenceNotFoundException(
                "Absence not found: " + command.absenceTypeCode() + "/" + command.startDate()));
    }
}
```

```java
// ListEmployeeAbsencesService.java
package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListEmployeeAbsencesService implements ListEmployeeAbsencesUseCase {

    private final GetEmployeeByBusinessKeyUseCase getEmployee;
    private final AbsenceRepository absenceRepository;

    public ListEmployeeAbsencesService(GetEmployeeByBusinessKeyUseCase getEmployee,
                                        AbsenceRepository absenceRepository) {
        this.getEmployee = getEmployee;
        this.absenceRepository = absenceRepository;
    }

    @Override
    public List<Absence> listByEmployeeBusinessKey(ListEmployeeAbsencesCommand command) {
        return getEmployee.getByBusinessKey(
                command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber())
            .map(e -> absenceRepository.findByEmployeeIdOrderByStartDateDescStartTimeDesc(e.getId()))
            .orElse(List.of());
    }
}
```

```java
// DeleteAbsenceService.java
package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.exception.AbsenceNotFoundException;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import org.springframework.stereotype.Service;

@Service
public class DeleteAbsenceService implements DeleteAbsenceUseCase {

    private final GetEmployeeByBusinessKeyUseCase getEmployee;
    private final AbsenceRepository absenceRepository;

    public DeleteAbsenceService(GetEmployeeByBusinessKeyUseCase getEmployee,
                                 AbsenceRepository absenceRepository) {
        this.getEmployee = getEmployee;
        this.absenceRepository = absenceRepository;
    }

    @Override
    public void delete(DeleteAbsenceCommand command) {
        Long employeeId = getEmployee.getByBusinessKey(
                command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber())
            .map(e -> e.getId())
            .orElseThrow(() -> new AbsenceNotFoundException("Employee not found: " + command.employeeNumber()));

        absenceRepository.findByKey(employeeId, command.absenceTypeCode(), command.startDate(), command.startTime())
            .orElseThrow(() -> new AbsenceNotFoundException(
                "Absence not found: " + command.absenceTypeCode() + "/" + command.startDate()));

        absenceRepository.deleteByKey(employeeId, command.absenceTypeCode(), command.startDate(), command.startTime());
    }
}
```

```java
// CloseOpenAbsenceAtTerminationService.java
package com.b4rrhh.employee.absence.application.usecase;

import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CloseOpenAbsenceAtTerminationService implements CloseOpenAbsenceAtTerminationUseCase {

    private final GetEmployeeByBusinessKeyUseCase getEmployee;
    private final AbsenceRepository absenceRepository;

    public CloseOpenAbsenceAtTerminationService(GetEmployeeByBusinessKeyUseCase getEmployee,
                                                 AbsenceRepository absenceRepository) {
        this.getEmployee = getEmployee;
        this.absenceRepository = absenceRepository;
    }

    @Override
    public void closeIfOpen(String ruleSystemCode, String employeeTypeCode,
                             String employeeNumber, LocalDate terminationDate) {
        getEmployee.getByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
            .ifPresent(employee -> absenceRepository
                .findByEmployeeIdOrderByStartDateDescStartTimeDesc(employee.getId())
                .stream()
                .filter(Absence::isOpen)
                .findFirst()
                .ifPresent(absence -> {
                    absence.closeAt(terminationDate);
                    absenceRepository.save(absence);
                }));
    }
}
```

- [ ] **Step 5: Run tests — verify all pass**

```bash
mvn test -Dtest=GetAbsenceByBusinessKeyServiceTest,ListEmployeeAbsencesServiceTest,DeleteAbsenceServiceTest,CloseOpenAbsenceAtTerminationServiceTest
```

Expected: BUILD SUCCESS, all tests passing.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/absence/application/
git add src/test/java/com/b4rrhh/employee/absence/application/
git commit -m "feat(absence): add Get, List, Delete, and CloseAtTermination use cases (TDD)"
```

---

## Task 5: Persistence Layer

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/absence/infrastructure/persistence/AbsenceEntity.java`
- Create: `src/main/java/com/b4rrhh/employee/absence/infrastructure/persistence/SpringDataAbsenceRepository.java`
- Create: `src/main/java/com/b4rrhh/employee/absence/infrastructure/persistence/AbsencePersistenceAdapter.java`

No unit tests for persistence adapter — the application-layer tests already mock the repository. Integration with H2 is validated indirectly by the web layer test (Task 6).

- [ ] **Step 1: Create AbsenceEntity**

```java
// src/main/java/com/b4rrhh/employee/absence/infrastructure/persistence/AbsenceEntity.java
package com.b4rrhh.employee.absence.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_absence", schema = "employee")
public class AbsenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "absence_type_code", nullable = false, length = 50)
    private String absenceTypeCode;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "start_time", nullable = false)
    private int startTime;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "end_time")
    private Integer endTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getAbsenceTypeCode() { return absenceTypeCode; }
    public void setAbsenceTypeCode(String absenceTypeCode) { this.absenceTypeCode = absenceTypeCode; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public int getStartTime() { return startTime; }
    public void setStartTime(int startTime) { this.startTime = startTime; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getEndTime() { return endTime; }
    public void setEndTime(Integer endTime) { this.endTime = endTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 2: Create SpringDataAbsenceRepository**

```java
// src/main/java/com/b4rrhh/employee/absence/infrastructure/persistence/SpringDataAbsenceRepository.java
package com.b4rrhh.employee.absence.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface SpringDataAbsenceRepository extends JpaRepository<AbsenceEntity, Long> {

    Optional<AbsenceEntity> findByEmployeeIdAndAbsenceTypeCodeAndStartDateAndStartTime(
        Long employeeId, String absenceTypeCode, LocalDate startDate, int startTime);

    List<AbsenceEntity> findByEmployeeIdOrderByStartDateDescStartTimeDesc(Long employeeId);

    void deleteByEmployeeIdAndAbsenceTypeCodeAndStartDateAndStartTime(
        Long employeeId, String absenceTypeCode, LocalDate startDate, int startTime);

    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
        FROM AbsenceEntity a
        WHERE a.employeeId = :employeeId
          AND a.startDate <= :effectiveEndDate
          AND COALESCE(a.endDate, :maxDate) >= :startDate
        """)
    boolean existsOverlappingAbsence(
        @Param("employeeId") Long employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("effectiveEndDate") LocalDate effectiveEndDate,
        @Param("maxDate") LocalDate maxDate);

    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
        FROM AbsenceEntity a
        WHERE a.employeeId = :employeeId
          AND a.startDate <= :effectiveEndDate
          AND COALESCE(a.endDate, :maxDate) >= :startDate
          AND a.id != :excludeId
        """)
    boolean existsOverlappingAbsenceExcluding(
        @Param("employeeId") Long employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("effectiveEndDate") LocalDate effectiveEndDate,
        @Param("excludeId") Long excludeId,
        @Param("maxDate") LocalDate maxDate);
}
```

- [ ] **Step 3: Create AbsencePersistenceAdapter**

```java
// src/main/java/com/b4rrhh/employee/absence/infrastructure/persistence/AbsencePersistenceAdapter.java
package com.b4rrhh.employee.absence.infrastructure.persistence;

import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.domain.port.AbsenceRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class AbsencePersistenceAdapter implements AbsenceRepository {

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final SpringDataAbsenceRepository springRepo;

    public AbsencePersistenceAdapter(SpringDataAbsenceRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<Absence> findByKey(Long employeeId, String absenceTypeCode,
                                        LocalDate startDate, int startTime) {
        return springRepo.findByEmployeeIdAndAbsenceTypeCodeAndStartDateAndStartTime(
            employeeId, absenceTypeCode, startDate, startTime).map(this::toDomain);
    }

    @Override
    public List<Absence> findByEmployeeIdOrderByStartDateDescStartTimeDesc(Long employeeId) {
        return springRepo.findByEmployeeIdOrderByStartDateDescStartTimeDesc(employeeId)
            .stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsOverlappingAbsence(Long employeeId, LocalDate startDate, LocalDate effectiveEndDate) {
        return springRepo.existsOverlappingAbsence(employeeId, startDate, effectiveEndDate, MAX_DATE);
    }

    @Override
    public boolean existsOverlappingAbsenceExcluding(Long employeeId, LocalDate startDate,
                                                      LocalDate effectiveEndDate, Long excludeId) {
        return springRepo.existsOverlappingAbsenceExcluding(employeeId, startDate, effectiveEndDate, excludeId, MAX_DATE);
    }

    @Override
    public Absence save(Absence absence) {
        AbsenceEntity entity = toEntity(absence);
        return toDomain(springRepo.save(entity));
    }

    @Override
    public void deleteByKey(Long employeeId, String absenceTypeCode, LocalDate startDate, int startTime) {
        springRepo.deleteByEmployeeIdAndAbsenceTypeCodeAndStartDateAndStartTime(
            employeeId, absenceTypeCode, startDate, startTime);
    }

    private Absence toDomain(AbsenceEntity e) {
        return Absence.rehydrate(e.getId(), e.getEmployeeId(), e.getAbsenceTypeCode(),
            e.getStartDate(), e.getStartTime(), e.getEndDate(), e.getEndTime(),
            e.getCreatedAt(), e.getUpdatedAt());
    }

    private AbsenceEntity toEntity(Absence a) {
        AbsenceEntity e = new AbsenceEntity();
        e.setId(a.getId());
        e.setEmployeeId(a.getEmployeeId());
        e.setAbsenceTypeCode(a.getAbsenceTypeCode());
        e.setStartDate(a.getStartDate());
        e.setStartTime(a.getStartTime());
        e.setEndDate(a.getEndDate());
        e.setEndTime(a.getEndTime());
        e.setCreatedAt(a.getCreatedAt());
        e.setUpdatedAt(a.getUpdatedAt());
        return e;
    }
}
```

- [ ] **Step 4: Verify full suite compiles and passes**

```bash
mvn test
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/absence/infrastructure/persistence/
git commit -m "feat(absence): add AbsenceEntity, SpringDataAbsenceRepository, and AbsencePersistenceAdapter"
```

---

## Task 6: Web Layer

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/absence/infrastructure/web/dto/UpsertAbsenceRequest.java`
- Create: `src/main/java/com/b4rrhh/employee/absence/infrastructure/web/dto/AbsenceResponse.java`
- Create: `src/main/java/com/b4rrhh/employee/absence/infrastructure/web/dto/AbsenceErrorResponse.java`
- Create: `src/main/java/com/b4rrhh/employee/absence/infrastructure/web/AbsenceWebMapper.java`
- Create: `src/main/java/com/b4rrhh/employee/absence/infrastructure/web/AbsenceExceptionHandler.java`
- Create: `src/main/java/com/b4rrhh/employee/absence/infrastructure/web/AbsenceBusinessKeyController.java`

Time format rules:
- Domain: `int` (minutes [0–1439])
- JSON response: `"HH:mm"` string (e.g., `"09:00"`)
- JSON request `endTime`: `"HH:mm"` string (e.g., `"17:30"`)
- URL path segment: `HHmm` 4-digit string (e.g., `"0900"`)

- [ ] **Step 1: Create DTOs**

```java
// UpsertAbsenceRequest.java
package com.b4rrhh.employee.absence.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record UpsertAbsenceRequest(
    @JsonProperty("endDate") LocalDate endDate,
    @JsonProperty("endTime") String endTime  // "HH:mm" or null
) {}

// AbsenceResponse.java
package com.b4rrhh.employee.absence.infrastructure.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AbsenceResponse(
    String absenceTypeCode,
    LocalDate startDate,
    String startTime,   // "HH:mm"
    LocalDate endDate,
    String endTime,     // "HH:mm" or null
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

// AbsenceErrorResponse.java
package com.b4rrhh.employee.absence.infrastructure.web.dto;

public record AbsenceErrorResponse(String error, String message) {}
```

- [ ] **Step 2: Create AbsenceWebMapper**

```java
// src/main/java/com/b4rrhh/employee/absence/infrastructure/web/AbsenceWebMapper.java
package com.b4rrhh.employee.absence.infrastructure.web;

import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.infrastructure.web.dto.AbsenceResponse;
import org.springframework.stereotype.Component;

@Component
public class AbsenceWebMapper {

    public AbsenceResponse toResponse(Absence absence) {
        return new AbsenceResponse(
            absence.getAbsenceTypeCode(),
            absence.getStartDate(),
            minutesToHHmm(absence.getStartTime()),
            absence.getEndDate(),
            absence.getEndTime() != null ? minutesToHHmm(absence.getEndTime()) : null,
            absence.getCreatedAt(),
            absence.getUpdatedAt()
        );
    }

    /** "HH:mm" → minutes of day */
    public int parseHHmmToMinutes(String hhMm) {
        String[] parts = hhMm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    /** 4-digit path "HHmm" → minutes of day */
    public int parsePathTimeToMinutes(String hhmm) {
        int hours = Integer.parseInt(hhmm.substring(0, 2));
        int minutes = Integer.parseInt(hhmm.substring(2, 4));
        return hours * 60 + minutes;
    }

    /** minutes → "HH:mm" */
    private String minutesToHHmm(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}
```

- [ ] **Step 3: Create AbsenceExceptionHandler**

```java
// src/main/java/com/b4rrhh/employee/absence/infrastructure/web/AbsenceExceptionHandler.java
package com.b4rrhh.employee.absence.infrastructure.web;

import com.b4rrhh.employee.absence.domain.exception.*;
import com.b4rrhh.employee.absence.infrastructure.web.dto.AbsenceErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AbsenceBusinessKeyController.class)
public class AbsenceExceptionHandler {

    @ExceptionHandler(AbsenceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public AbsenceErrorResponse handleNotFound(AbsenceNotFoundException ex) {
        return new AbsenceErrorResponse("ABSENCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(AbsenceCatalogValueInvalidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public AbsenceErrorResponse handleCatalogInvalid(AbsenceCatalogValueInvalidException ex) {
        return new AbsenceErrorResponse("ABSENCE_CATALOG_VALUE_INVALID", ex.getMessage());
    }

    @ExceptionHandler(AbsenceEmployeeNotFoundException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public AbsenceErrorResponse handleEmployeeNotFound(AbsenceEmployeeNotFoundException ex) {
        return new AbsenceErrorResponse("ABSENCE_EMPLOYEE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(AbsenceOutsidePresencePeriodException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public AbsenceErrorResponse handleOutsidePresence(AbsenceOutsidePresencePeriodException ex) {
        return new AbsenceErrorResponse("ABSENCE_OUTSIDE_PRESENCE_PERIOD", ex.getMessage());
    }

    @ExceptionHandler(InvalidAbsenceDateRangeException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public AbsenceErrorResponse handleInvalidDateRange(InvalidAbsenceDateRangeException ex) {
        return new AbsenceErrorResponse("INVALID_ABSENCE_DATE_RANGE", ex.getMessage());
    }

    @ExceptionHandler(AbsenceOverlapException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public AbsenceErrorResponse handleOverlap(AbsenceOverlapException ex) {
        return new AbsenceErrorResponse("ABSENCE_OVERLAP", ex.getMessage());
    }
}
```

- [ ] **Step 4: Create AbsenceBusinessKeyController**

```java
// src/main/java/com/b4rrhh/employee/absence/infrastructure/web/AbsenceBusinessKeyController.java
package com.b4rrhh.employee.absence.infrastructure.web;

import com.b4rrhh.employee.absence.application.usecase.*;
import com.b4rrhh.employee.absence.domain.model.Absence;
import com.b4rrhh.employee.absence.infrastructure.web.dto.AbsenceResponse;
import com.b4rrhh.employee.absence.infrastructure.web.dto.UpsertAbsenceRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/absences")
public class AbsenceBusinessKeyController {

    private final UpsertAbsenceUseCase upsert;
    private final GetAbsenceByBusinessKeyUseCase get;
    private final ListEmployeeAbsencesUseCase list;
    private final DeleteAbsenceUseCase delete;
    private final AbsenceWebMapper mapper;

    public AbsenceBusinessKeyController(UpsertAbsenceUseCase upsert,
                                         GetAbsenceByBusinessKeyUseCase get,
                                         ListEmployeeAbsencesUseCase list,
                                         DeleteAbsenceUseCase delete,
                                         AbsenceWebMapper mapper) {
        this.upsert = upsert;
        this.get = get;
        this.list = list;
        this.delete = delete;
        this.mapper = mapper;
    }

    // PUT — day mode (startTime defaults to 0)
    @PutMapping("/{absenceTypeCode}/{startDate}")
    public AbsenceResponse upsertDayMode(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String absenceTypeCode,
            @PathVariable LocalDate startDate,
            @RequestBody UpsertAbsenceRequest request) {
        return doUpsert(ruleSystemCode, employeeTypeCode, employeeNumber,
            absenceTypeCode, startDate, 0, request);
    }

    // PUT — hour mode (startTime from HHmm path segment)
    @PutMapping("/{absenceTypeCode}/{startDate}/{startTime}")
    public AbsenceResponse upsertHourMode(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String absenceTypeCode,
            @PathVariable LocalDate startDate,
            @PathVariable String startTime,
            @RequestBody UpsertAbsenceRequest request) {
        return doUpsert(ruleSystemCode, employeeTypeCode, employeeNumber,
            absenceTypeCode, startDate, mapper.parsePathTimeToMinutes(startTime), request);
    }

    private AbsenceResponse doUpsert(String rs, String et, String en, String typeCode,
                                      LocalDate startDate, int startTimeMinutes,
                                      UpsertAbsenceRequest request) {
        Integer endTimeMinutes = request.endTime() != null
            ? mapper.parseHHmmToMinutes(request.endTime()) : null;
        UpsertAbsenceCommand cmd = new UpsertAbsenceCommand(rs, et, en, typeCode,
            startDate, startTimeMinutes, request.endDate(), endTimeMinutes);
        Absence absence = upsert.upsert(cmd);
        return mapper.toResponse(absence);
    }

    // GET one — day mode
    @GetMapping("/{absenceTypeCode}/{startDate}")
    public AbsenceResponse getDayMode(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String absenceTypeCode,
            @PathVariable LocalDate startDate) {
        return doGet(ruleSystemCode, employeeTypeCode, employeeNumber, absenceTypeCode, startDate, 0);
    }

    // GET one — hour mode
    @GetMapping("/{absenceTypeCode}/{startDate}/{startTime}")
    public AbsenceResponse getHourMode(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String absenceTypeCode,
            @PathVariable LocalDate startDate,
            @PathVariable String startTime) {
        return doGet(ruleSystemCode, employeeTypeCode, employeeNumber, absenceTypeCode,
            startDate, mapper.parsePathTimeToMinutes(startTime));
    }

    private AbsenceResponse doGet(String rs, String et, String en, String typeCode,
                                   LocalDate startDate, int startTimeMinutes) {
        Absence absence = get.getByBusinessKey(
            new GetAbsenceByBusinessKeyCommand(rs, et, en, typeCode, startDate, startTimeMinutes));
        return mapper.toResponse(absence);
    }

    // GET list
    @GetMapping
    public List<AbsenceResponse> listAbsences(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber) {
        return list.listByEmployeeBusinessKey(
                new ListEmployeeAbsencesCommand(ruleSystemCode, employeeTypeCode, employeeNumber))
            .stream().map(mapper::toResponse).toList();
    }

    // DELETE — day mode
    @DeleteMapping("/{absenceTypeCode}/{startDate}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDayMode(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String absenceTypeCode,
            @PathVariable LocalDate startDate) {
        doDelete(ruleSystemCode, employeeTypeCode, employeeNumber, absenceTypeCode, startDate, 0);
    }

    // DELETE — hour mode
    @DeleteMapping("/{absenceTypeCode}/{startDate}/{startTime}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHourMode(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String absenceTypeCode,
            @PathVariable LocalDate startDate,
            @PathVariable String startTime) {
        doDelete(ruleSystemCode, employeeTypeCode, employeeNumber, absenceTypeCode,
            startDate, mapper.parsePathTimeToMinutes(startTime));
    }

    private void doDelete(String rs, String et, String en, String typeCode,
                           LocalDate startDate, int startTimeMinutes) {
        delete.delete(new DeleteAbsenceCommand(rs, et, en, typeCode, startDate, startTimeMinutes));
    }
}
```

- [ ] **Step 5: Run full test suite**

```bash
mvn test
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/absence/infrastructure/web/
git commit -m "feat(absence): add web layer — controller, mapper, exception handler, and DTOs"
```

---

## Task 7: Lifecycle Integration

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/AbsenceTerminationParticipant.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/AbsenceTerminationParticipantTest.java`
- Modify: `src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/TerminateEmployeeServiceRollbackIntegrationTest.java`

`AbsenceTerminationParticipant` has order=25, between WorkCenter (20) and CostCenter (30). It delegates to `CloseOpenAbsenceAtTerminationUseCase`. It never throws — if no open absence exists or employee not found, it does nothing.

- [ ] **Step 1: Write failing test**

```java
// src/test/java/com/b4rrhh/employee/lifecycle/application/participant/AbsenceTerminationParticipantTest.java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.absence.application.usecase.CloseOpenAbsenceAtTerminationUseCase;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbsenceTerminationParticipantTest {

    @Mock private CloseOpenAbsenceAtTerminationUseCase closeIfOpen;
    @InjectMocks private AbsenceTerminationParticipant participant;

    @Test
    void orderIs25() {
        assertEquals(25, participant.order());
    }

    @Test
    void delegatesToCloseIfOpen() {
        TerminationContext ctx = mock(TerminationContext.class);
        when(ctx.ruleSystemCode()).thenReturn("ESP");
        when(ctx.employeeTypeCode()).thenReturn("INTERNAL");
        when(ctx.employeeNumber()).thenReturn("EMP001");
        when(ctx.terminationDate()).thenReturn(LocalDate.of(2026, 5, 31));

        participant.participate(ctx);

        verify(closeIfOpen).closeIfOpen("ESP", "INTERNAL", "EMP001", LocalDate.of(2026, 5, 31));
    }
}
```

- [ ] **Step 2: Run test — verify it fails**

```bash
mvn test -Dtest=AbsenceTerminationParticipantTest
```

Expected: FAIL — `AbsenceTerminationParticipant` does not exist.

- [ ] **Step 3: Implement AbsenceTerminationParticipant**

```java
// src/main/java/com/b4rrhh/employee/lifecycle/application/participant/AbsenceTerminationParticipant.java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.absence.application.usecase.CloseOpenAbsenceAtTerminationUseCase;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import org.springframework.stereotype.Component;

@Component
public class AbsenceTerminationParticipant implements TerminationParticipant {

    private final CloseOpenAbsenceAtTerminationUseCase closeIfOpen;

    public AbsenceTerminationParticipant(CloseOpenAbsenceAtTerminationUseCase closeIfOpen) {
        this.closeIfOpen = closeIfOpen;
    }

    @Override
    public int order() {
        return 25;
    }

    @Override
    public void participate(TerminationContext ctx) {
        closeIfOpen.closeIfOpen(ctx.ruleSystemCode(), ctx.employeeTypeCode(),
            ctx.employeeNumber(), ctx.terminationDate());
    }
}
```

- [ ] **Step 4: Run test — verify it passes**

```bash
mvn test -Dtest=AbsenceTerminationParticipantTest
```

Expected: BUILD SUCCESS, 2 tests passing.

- [ ] **Step 5: Update TerminateEmployeeServiceRollbackIntegrationTest**

Open `src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/TerminateEmployeeServiceRollbackIntegrationTest.java`.

Add two new beans inside the `TerminationRollbackTestConfig` inner class (at the end, before the closing `}`):

```java
@Bean
CloseOpenAbsenceAtTerminationUseCase closeOpenAbsenceAtTerminationUseCase() {
    return (ruleSystemCode, employeeTypeCode, employeeNumber, terminationDate) -> {};
}

@Bean
AbsenceTerminationParticipant absenceTerminationParticipant(
        CloseOpenAbsenceAtTerminationUseCase closeOpenAbsenceAtTerminationUseCase) {
    return new AbsenceTerminationParticipant(closeOpenAbsenceAtTerminationUseCase);
}
```

Also add these imports to the rollback test file:
```java
import com.b4rrhh.employee.absence.application.usecase.CloseOpenAbsenceAtTerminationUseCase;
import com.b4rrhh.employee.lifecycle.application.participant.AbsenceTerminationParticipant;
```

- [ ] **Step 6: Run rollback integration test**

```bash
mvn test -Dtest=TerminateEmployeeServiceRollbackIntegrationTest
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Run full suite**

```bash
mvn test
```

Expected: BUILD SUCCESS, all tests passing (previous count + new absence and participant tests).

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/lifecycle/application/participant/AbsenceTerminationParticipant.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/participant/AbsenceTerminationParticipantTest.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/TerminateEmployeeServiceRollbackIntegrationTest.java
git commit -m "feat(absence): add AbsenceTerminationParticipant (order=25) and update rollback integration test"
```

---

## Task 8: OpenAPI Spec Update

**Files:**
- Modify: `openapi/personnel-administration-api.yaml`

- [ ] **Step 1: Find the absence endpoints section insertion point**

Open `openapi/personnel-administration-api.yaml` and locate the paths section for other employee sub-resources (e.g., `/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/presences`). The absence paths go after the last existing employee sub-resource path.

- [ ] **Step 2: Add absence paths**

Add these 7 paths to the `paths:` section:

```yaml
  /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/absences:
    get:
      summary: List all absences for an employee
      operationId: listEmployeeAbsences
      tags: [Employee Absences]
      parameters:
        - $ref: '#/components/parameters/ruleSystemCode'
        - $ref: '#/components/parameters/employeeTypeCode'
        - $ref: '#/components/parameters/employeeNumber'
      responses:
        '200':
          description: List of absences ordered by startDate DESC, startTime DESC
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/AbsenceResponse'

  /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/absences/{absenceTypeCode}/{startDate}:
    put:
      summary: Upsert absence (day mode — startTime defaults to 00:00)
      operationId: upsertAbsenceDayMode
      tags: [Employee Absences]
      parameters:
        - $ref: '#/components/parameters/ruleSystemCode'
        - $ref: '#/components/parameters/employeeTypeCode'
        - $ref: '#/components/parameters/employeeNumber'
        - name: absenceTypeCode
          in: path
          required: true
          schema:
            type: string
        - name: startDate
          in: path
          required: true
          schema:
            type: string
            format: date
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpsertAbsenceRequest'
      responses:
        '200':
          description: Absence upserted successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AbsenceResponse'
        '409':
          description: Absence overlaps with an existing absence
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AbsenceErrorResponse'
        '422':
          description: Validation error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AbsenceErrorResponse'
    get:
      summary: Get absence by business key (day mode)
      operationId: getAbsenceDayMode
      tags: [Employee Absences]
      parameters:
        - $ref: '#/components/parameters/ruleSystemCode'
        - $ref: '#/components/parameters/employeeTypeCode'
        - $ref: '#/components/parameters/employeeNumber'
        - name: absenceTypeCode
          in: path
          required: true
          schema:
            type: string
        - name: startDate
          in: path
          required: true
          schema:
            type: string
            format: date
      responses:
        '200':
          description: Absence found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AbsenceResponse'
        '404':
          description: Absence not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AbsenceErrorResponse'
    delete:
      summary: Delete absence (day mode)
      operationId: deleteAbsenceDayMode
      tags: [Employee Absences]
      parameters:
        - $ref: '#/components/parameters/ruleSystemCode'
        - $ref: '#/components/parameters/employeeTypeCode'
        - $ref: '#/components/parameters/employeeNumber'
        - name: absenceTypeCode
          in: path
          required: true
          schema:
            type: string
        - name: startDate
          in: path
          required: true
          schema:
            type: string
            format: date
      responses:
        '204':
          description: Absence deleted
        '404':
          description: Absence not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AbsenceErrorResponse'

  /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/absences/{absenceTypeCode}/{startDate}/{startTime}:
    put:
      summary: Upsert absence (hour mode — startTime as HHmm e.g. 0900)
      operationId: upsertAbsenceHourMode
      tags: [Employee Absences]
      parameters:
        - $ref: '#/components/parameters/ruleSystemCode'
        - $ref: '#/components/parameters/employeeTypeCode'
        - $ref: '#/components/parameters/employeeNumber'
        - name: absenceTypeCode
          in: path
          required: true
          schema:
            type: string
        - name: startDate
          in: path
          required: true
          schema:
            type: string
            format: date
        - name: startTime
          in: path
          required: true
          schema:
            type: string
            pattern: '^\d{4}$'
            description: Time as HHmm (e.g. 0900 for 09:00)
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpsertAbsenceRequest'
      responses:
        '200':
          description: Absence upserted successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AbsenceResponse'
        '409':
          description: Overlap conflict
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AbsenceErrorResponse'
        '422':
          description: Validation error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AbsenceErrorResponse'
    get:
      summary: Get absence by business key (hour mode)
      operationId: getAbsenceHourMode
      tags: [Employee Absences]
      parameters:
        - $ref: '#/components/parameters/ruleSystemCode'
        - $ref: '#/components/parameters/employeeTypeCode'
        - $ref: '#/components/parameters/employeeNumber'
        - name: absenceTypeCode
          in: path
          required: true
          schema:
            type: string
        - name: startDate
          in: path
          required: true
          schema:
            type: string
            format: date
        - name: startTime
          in: path
          required: true
          schema:
            type: string
            pattern: '^\d{4}$'
      responses:
        '200':
          description: Absence found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AbsenceResponse'
        '404':
          description: Absence not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AbsenceErrorResponse'
    delete:
      summary: Delete absence (hour mode)
      operationId: deleteAbsenceHourMode
      tags: [Employee Absences]
      parameters:
        - $ref: '#/components/parameters/ruleSystemCode'
        - $ref: '#/components/parameters/employeeTypeCode'
        - $ref: '#/components/parameters/employeeNumber'
        - name: absenceTypeCode
          in: path
          required: true
          schema:
            type: string
        - name: startDate
          in: path
          required: true
          schema:
            type: string
            format: date
        - name: startTime
          in: path
          required: true
          schema:
            type: string
            pattern: '^\d{4}$'
      responses:
        '204':
          description: Absence deleted
        '404':
          description: Absence not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AbsenceErrorResponse'
```

- [ ] **Step 3: Add schemas and error response to components**

In the `components/schemas:` section, add:

```yaml
    UpsertAbsenceRequest:
      type: object
      properties:
        endDate:
          type: string
          format: date
          nullable: true
          example: "2026-05-18"
        endTime:
          type: string
          nullable: true
          pattern: '^\d{2}:\d{2}$'
          example: "17:30"

    AbsenceResponse:
      type: object
      properties:
        absenceTypeCode:
          type: string
          example: "VACATION"
        startDate:
          type: string
          format: date
          example: "2026-05-14"
        startTime:
          type: string
          example: "00:00"
        endDate:
          type: string
          format: date
          nullable: true
          example: "2026-05-18"
        endTime:
          type: string
          nullable: true
          example: null
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time

    AbsenceErrorResponse:
      type: object
      properties:
        error:
          type: string
        message:
          type: string
```

- [ ] **Step 4: Run full test suite**

```bash
mvn test
```

Expected: BUILD SUCCESS, all tests passing.

- [ ] **Step 5: Commit**

```bash
git add openapi/personnel-administration-api.yaml
git commit -m "feat(absence): add absence endpoints and schemas to OpenAPI spec"
```

---

## Done

After all 8 tasks complete:

1. Run the full test suite one final time: `mvn test`
2. Start the backend and manually test:
   ```
   PUT  /employees/ESP/INTERNAL/EMP001/absences/VACATION/2026-05-14
   GET  /employees/ESP/INTERNAL/EMP001/absences/VACATION/2026-05-14
   GET  /employees/ESP/INTERNAL/EMP001/absences
   DELETE /employees/ESP/INTERNAL/EMP001/absences/VACATION/2026-05-14
   ```
3. Use `superpowers:finishing-a-development-branch` to wrap up.
