# Employee Tax Information Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the `employee.tax_information` vertical — a `valid_from` temporal record that captures Modelo 145 data (family situation, dependants, disability, IRPF territory) per employee, with full CRUD and a payroll context snapshot.

**Architecture:** Hexagonal under `com.b4rrhh.employee.tax_information`. Immutable domain model with `create()` / `correct()` / `rehydrate()` / `DEFAULT`. Three Java enums enforce input strictness. A payroll lookup port (defined in `payroll.application.port`, adapter in `employee.tax_information.infrastructure.persistence`) captures a `EMPLOYEE_TAX_INFORMATION` snapshot during calculation.

**Tech Stack:** Java 21, Spring Boot, JPA, H2 (tests), Flyway V95, Angular 21, PrimeNG.

---

## File Map

**Create — backend:**
```
com.b4rrhh.employee.tax_information.domain.model/
  FamilySituation.java  DisabilityDegree.java  TaxTerritory.java
  EmployeeTaxInformation.java
domain.port/   EmployeeTaxInformationRepository.java
domain.exception/
  EmployeeTaxInformationAlreadyExistsException.java
  EmployeeTaxInformationNotFoundException.java
  EmployeeTaxInformationEmployeeNotFoundException.java
  EmployeeTaxInformationInvalidValidFromException.java
application.port/
  EmployeeForTaxInfoLookupPort.java  TaxInfoPresenceLookupPort.java
application.usecase/
  Create{UseCase,Command,Service}.java
  Correct{UseCase,Command,Service}.java
  Delete{UseCase,Command,Service}.java
  Get{UseCase,Command,Service}.java
  List{UseCase,Command,Service}.java
infrastructure.persistence/
  EmployeeTaxInformationEntity.java
  SpringDataEmployeeTaxInformationRepository.java
  EmployeeTaxInformationPersistenceAdapter.java
  EmployeeForTaxInfoLookupAdapter.java
  TaxInfoPresenceLookupAdapter.java
  EmployeeTaxInfoPayrollLookupAdapter.java
infrastructure.web/
  EmployeeTaxInformationBusinessKeyController.java
  EmployeeTaxInformationExceptionHandler.java
  assembler/EmployeeTaxInformationAssembler.java
  dto/{Create,Correct}EmployeeTaxInformationRequest.java
  dto/EmployeeTaxInformation{Response,ErrorResponse}.java

com.b4rrhh.payroll.application.port/
  EmployeeTaxInfoContext.java  EmployeeTaxInfoPayrollLookupPort.java

db/migration/V95__create_employee_tax_information.sql
```

**Modify — backend:**
- `CalculatePayrollUnitService.java` — inject port, add snapshot builder
- `openapi/personnel-administration-api.yaml` — 5 new paths + schemas

**Create — frontend:**
```
core/api/clients/employee-tax-information.client.ts
core/api/mappers/employee-tax-information.mapper.ts
features/employee/tax-information/
  models/employee-tax-information.model.ts
  data-access/employee-tax-information.{gateway,store}.ts
  components/employee-tax-information-section.component.{ts,html}
```

---

## Task 1 — Domain Enums + Model

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/tax_information/domain/model/FamilySituation.java`
- Create: `src/main/java/com/b4rrhh/employee/tax_information/domain/model/DisabilityDegree.java`
- Create: `src/main/java/com/b4rrhh/employee/tax_information/domain/model/TaxTerritory.java`
- Create: `src/main/java/com/b4rrhh/employee/tax_information/domain/model/EmployeeTaxInformation.java`
- Test: `src/test/java/com/b4rrhh/employee/tax_information/domain/model/EmployeeTaxInformationTest.java`

- [ ] **Write the failing test**

```java
package com.b4rrhh.employee.tax_information.domain.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class EmployeeTaxInformationTest {

    @Test
    void create_buildsCorrectInstance() {
        var result = EmployeeTaxInformation.create(1L, LocalDate.of(2025,1,1),
            FamilySituation.SINGLE_OR_OTHER, 2, 0,
            DisabilityDegree.NONE, false, false, false, TaxTerritory.COMUN);
        assertNull(result.getId());
        assertEquals(FamilySituation.SINGLE_OR_OTHER, result.getFamilySituation());
        assertEquals(2, result.getDescendantsCount());
        assertEquals(TaxTerritory.COMUN, result.getTaxTerritory());
    }

    @Test
    void correct_returnsNewInstancePreservingIdentity() {
        var original = EmployeeTaxInformation.rehydrate(7L, 1L, LocalDate.of(2025,1,1),
            FamilySituation.SINGLE_OR_OTHER, 0, 0,
            DisabilityDegree.NONE, false, false, false, TaxTerritory.COMUN, null, null);
        var corrected = original.correct(
            FamilySituation.MARRIED_DEPENDENT_SPOUSE, 3, 1,
            DisabilityDegree.MODERATE, true, false, true, TaxTerritory.BIZKAIA);
        assertEquals(7L, corrected.getId());
        assertEquals(LocalDate.of(2025,1,1), corrected.getValidFrom());
        assertEquals(FamilySituation.MARRIED_DEPENDENT_SPOUSE, corrected.getFamilySituation());
        assertEquals(TaxTerritory.BIZKAIA, corrected.getTaxTerritory());
    }

    @Test
    void create_throwsOnNegativeCounts() {
        assertThrows(IllegalArgumentException.class, () ->
            EmployeeTaxInformation.create(1L, LocalDate.of(2025,1,1),
                FamilySituation.SINGLE_OR_OTHER, -1, 0,
                DisabilityDegree.NONE, false, false, false, TaxTerritory.COMUN));
    }

    @Test
    void default_hasExpectedValues() {
        assertEquals(FamilySituation.SINGLE_OR_OTHER, EmployeeTaxInformation.DEFAULT.getFamilySituation());
        assertEquals(TaxTerritory.COMUN, EmployeeTaxInformation.DEFAULT.getTaxTerritory());
        assertEquals(DisabilityDegree.NONE, EmployeeTaxInformation.DEFAULT.getDisabilityDegree());
    }
}
```

- [ ] **Run test — verify it fails**

```
mvn test -Dtest=EmployeeTaxInformationTest
```
Expected: compilation error (classes don't exist yet).

- [ ] **Implement enums and domain model**

`FamilySituation.java`:
```java
package com.b4rrhh.employee.tax_information.domain.model;
public enum FamilySituation {
    SINGLE_OR_OTHER,
    MARRIED_DEPENDENT_SPOUSE,
    SEPARATED_WITH_CHILDREN
}
```

`DisabilityDegree.java`:
```java
package com.b4rrhh.employee.tax_information.domain.model;
public enum DisabilityDegree { NONE, MODERATE, SEVERE }
```

`TaxTerritory.java`:
```java
package com.b4rrhh.employee.tax_information.domain.model;
public enum TaxTerritory { COMUN, ARABA, GIPUZKOA, BIZKAIA, NAVARRA }
```

`EmployeeTaxInformation.java`:
```java
package com.b4rrhh.employee.tax_information.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeTaxInformation {

    public static final EmployeeTaxInformation DEFAULT = new EmployeeTaxInformation(
        null, null, null,
        FamilySituation.SINGLE_OR_OTHER, 0, 0,
        DisabilityDegree.NONE, false, false, false, TaxTerritory.COMUN,
        null, null);

    private final Long id;
    private final Long employeeId;
    private final LocalDate validFrom;
    private final FamilySituation familySituation;
    private final int descendantsCount;
    private final int ascendantsCount;
    private final DisabilityDegree disabilityDegree;
    private final boolean pensionCompensatoria;
    private final boolean geographicMobility;
    private final boolean habitualResidenceLoan;
    private final TaxTerritory taxTerritory;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private EmployeeTaxInformation(Long id, Long employeeId, LocalDate validFrom,
            FamilySituation familySituation, int descendantsCount, int ascendantsCount,
            DisabilityDegree disabilityDegree, boolean pensionCompensatoria,
            boolean geographicMobility, boolean habitualResidenceLoan, TaxTerritory taxTerritory,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.validFrom = validFrom;
        this.familySituation = familySituation;
        this.descendantsCount = descendantsCount;
        this.ascendantsCount = ascendantsCount;
        this.disabilityDegree = disabilityDegree;
        this.pensionCompensatoria = pensionCompensatoria;
        this.geographicMobility = geographicMobility;
        this.habitualResidenceLoan = habitualResidenceLoan;
        this.taxTerritory = taxTerritory;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EmployeeTaxInformation create(Long employeeId, LocalDate validFrom,
            FamilySituation familySituation, int descendantsCount, int ascendantsCount,
            DisabilityDegree disabilityDegree, boolean pensionCompensatoria,
            boolean geographicMobility, boolean habitualResidenceLoan, TaxTerritory taxTerritory) {
        if (descendantsCount < 0) throw new IllegalArgumentException("descendantsCount must be >= 0");
        if (ascendantsCount < 0) throw new IllegalArgumentException("ascendantsCount must be >= 0");
        return new EmployeeTaxInformation(null, employeeId, validFrom,
            familySituation, descendantsCount, ascendantsCount,
            disabilityDegree, pensionCompensatoria, geographicMobility, habitualResidenceLoan,
            taxTerritory, null, null);
    }

    public EmployeeTaxInformation correct(FamilySituation familySituation, int descendantsCount,
            int ascendantsCount, DisabilityDegree disabilityDegree, boolean pensionCompensatoria,
            boolean geographicMobility, boolean habitualResidenceLoan, TaxTerritory taxTerritory) {
        if (descendantsCount < 0) throw new IllegalArgumentException("descendantsCount must be >= 0");
        if (ascendantsCount < 0) throw new IllegalArgumentException("ascendantsCount must be >= 0");
        return new EmployeeTaxInformation(this.id, this.employeeId, this.validFrom,
            familySituation, descendantsCount, ascendantsCount,
            disabilityDegree, pensionCompensatoria, geographicMobility, habitualResidenceLoan,
            taxTerritory, this.createdAt, null);
    }

    public static EmployeeTaxInformation rehydrate(Long id, Long employeeId, LocalDate validFrom,
            FamilySituation familySituation, int descendantsCount, int ascendantsCount,
            DisabilityDegree disabilityDegree, boolean pensionCompensatoria,
            boolean geographicMobility, boolean habitualResidenceLoan, TaxTerritory taxTerritory,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new EmployeeTaxInformation(id, employeeId, validFrom,
            familySituation, descendantsCount, ascendantsCount,
            disabilityDegree, pensionCompensatoria, geographicMobility, habitualResidenceLoan,
            taxTerritory, createdAt, updatedAt);
    }

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public LocalDate getValidFrom() { return validFrom; }
    public FamilySituation getFamilySituation() { return familySituation; }
    public int getDescendantsCount() { return descendantsCount; }
    public int getAscendantsCount() { return ascendantsCount; }
    public DisabilityDegree getDisabilityDegree() { return disabilityDegree; }
    public boolean isPensionCompensatoria() { return pensionCompensatoria; }
    public boolean isGeographicMobility() { return geographicMobility; }
    public boolean isHabitualResidenceLoan() { return habitualResidenceLoan; }
    public TaxTerritory getTaxTerritory() { return taxTerritory; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Run test — verify it passes**

```
mvn test -Dtest=EmployeeTaxInformationTest
```
Expected: 4 tests passing.

- [ ] **Commit**

```
git add src/main/java/com/b4rrhh/employee/tax_information/domain/
git add src/test/java/com/b4rrhh/employee/tax_information/domain/
git commit -m "feat(tax-information): domain model — enums, EmployeeTaxInformation, tests"
```

---

## Task 2 — Domain Port, Exceptions, Flyway V95

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/tax_information/domain/port/EmployeeTaxInformationRepository.java`
- Create: `src/main/java/com/b4rrhh/employee/tax_information/domain/exception/EmployeeTaxInformationAlreadyExistsException.java`
- Create: `src/main/java/com/b4rrhh/employee/tax_information/domain/exception/EmployeeTaxInformationNotFoundException.java`
- Create: `src/main/java/com/b4rrhh/employee/tax_information/domain/exception/EmployeeTaxInformationEmployeeNotFoundException.java`
- Create: `src/main/java/com/b4rrhh/employee/tax_information/domain/exception/EmployeeTaxInformationInvalidValidFromException.java`
- Create: `src/main/resources/db/migration/V95__create_employee_tax_information.sql`

- [ ] **Implement port and exceptions**

`EmployeeTaxInformationRepository.java`:
```java
package com.b4rrhh.employee.tax_information.domain.port;

import com.b4rrhh.employee.tax_information.domain.model.EmployeeTaxInformation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeTaxInformationRepository {
    EmployeeTaxInformation save(EmployeeTaxInformation taxInformation);
    Optional<EmployeeTaxInformation> findByEmployeeIdAndValidFrom(Long employeeId, LocalDate validFrom);
    List<EmployeeTaxInformation> findAllByEmployeeIdOrderByValidFromDesc(Long employeeId);
    Optional<EmployeeTaxInformation> findLatestOnOrBefore(Long employeeId, LocalDate referenceDate);
    void deleteByEmployeeIdAndValidFrom(Long employeeId, LocalDate validFrom);
}
```

`EmployeeTaxInformationAlreadyExistsException.java`:
```java
package com.b4rrhh.employee.tax_information.domain.exception;
import java.time.LocalDate;
public class EmployeeTaxInformationAlreadyExistsException extends RuntimeException {
    public EmployeeTaxInformationAlreadyExistsException(Long employeeId, LocalDate validFrom) {
        super("Tax information already exists for employee " + employeeId + " valid from " + validFrom);
    }
}
```

`EmployeeTaxInformationNotFoundException.java`:
```java
package com.b4rrhh.employee.tax_information.domain.exception;
import java.time.LocalDate;
public class EmployeeTaxInformationNotFoundException extends RuntimeException {
    public EmployeeTaxInformationNotFoundException(Long employeeId, LocalDate validFrom) {
        super("Tax information not found for employee " + employeeId + " valid from " + validFrom);
    }
}
```

`EmployeeTaxInformationEmployeeNotFoundException.java`:
```java
package com.b4rrhh.employee.tax_information.domain.exception;
public class EmployeeTaxInformationEmployeeNotFoundException extends RuntimeException {
    public EmployeeTaxInformationEmployeeNotFoundException(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        super("Employee not found: " + ruleSystemCode + "/" + employeeTypeCode + "/" + employeeNumber);
    }
}
```

`EmployeeTaxInformationInvalidValidFromException.java`:
```java
package com.b4rrhh.employee.tax_information.domain.exception;
import java.time.LocalDate;
public class EmployeeTaxInformationInvalidValidFromException extends RuntimeException {
    public EmployeeTaxInformationInvalidValidFromException(LocalDate validFrom) {
        super("valid_from " + validFrom + " must be the first day of a month or a presence start date");
    }
}
```

- [ ] **Create Flyway migration V95**

`V95__create_employee_tax_information.sql`:
```sql
CREATE TABLE employee.employee_tax_information (
    id                      BIGSERIAL    PRIMARY KEY,
    employee_id             BIGINT       NOT NULL REFERENCES employee.employee(id),
    valid_from              DATE         NOT NULL,
    family_situation        VARCHAR(40)  NOT NULL
        CHECK (family_situation IN ('SINGLE_OR_OTHER','MARRIED_DEPENDENT_SPOUSE','SEPARATED_WITH_CHILDREN')),
    descendants_count       SMALLINT     NOT NULL DEFAULT 0 CHECK (descendants_count >= 0),
    ascendants_count        SMALLINT     NOT NULL DEFAULT 0 CHECK (ascendants_count >= 0),
    disability_degree       VARCHAR(20)  NOT NULL
        CHECK (disability_degree IN ('NONE','MODERATE','SEVERE')),
    pension_compensatoria   BOOLEAN      NOT NULL DEFAULT FALSE,
    geographic_mobility     BOOLEAN      NOT NULL DEFAULT FALSE,
    habitual_residence_loan BOOLEAN      NOT NULL DEFAULT FALSE,
    tax_territory           VARCHAR(20)  NOT NULL
        CHECK (tax_territory IN ('COMUN','ARABA','GIPUZKOA','BIZKAIA','NAVARRA')),
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tax_info_employee_valid_from UNIQUE (employee_id, valid_from)
);

CREATE INDEX idx_tax_info_employee_id ON employee.employee_tax_information(employee_id);
```

- [ ] **Commit**

```
git add src/main/java/com/b4rrhh/employee/tax_information/domain/
git add src/main/resources/db/migration/V95__create_employee_tax_information.sql
git commit -m "feat(tax-information): domain port, exceptions, Flyway V95 migration"
```

---

## Task 3 — JPA Persistence Layer

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/tax_information/infrastructure/persistence/EmployeeTaxInformationEntity.java`
- Create: `src/main/java/com/b4rrhh/employee/tax_information/infrastructure/persistence/SpringDataEmployeeTaxInformationRepository.java`
- Create: `src/main/java/com/b4rrhh/employee/tax_information/infrastructure/persistence/EmployeeTaxInformationPersistenceAdapter.java`
- Test: `src/test/java/com/b4rrhh/employee/tax_information/infrastructure/persistence/EmployeeTaxInformationPersistenceAdapterTest.java`

- [ ] **Write the failing test**

```java
package com.b4rrhh.employee.tax_information.infrastructure.persistence;

import com.b4rrhh.employee.tax_information.domain.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeTaxInformationPersistenceAdapterTest {

    @Mock SpringDataEmployeeTaxInformationRepository springDataRepo;
    @InjectMocks EmployeeTaxInformationPersistenceAdapter adapter;

    @Test
    void findLatestOnOrBefore_returnsEmptyWhenNoneExist() {
        when(springDataRepo.findFirstByEmployeeIdAndValidFromLessThanEqualOrderByValidFromDesc(any(), any()))
            .thenReturn(Optional.empty());
        assertTrue(adapter.findLatestOnOrBefore(1L, LocalDate.of(2025,1,1)).isEmpty());
    }

    @Test
    void findLatestOnOrBefore_mapsEntityToDomain() {
        EmployeeTaxInformationEntity entity = new EmployeeTaxInformationEntity();
        entity.setId(5L);
        entity.setEmployeeId(1L);
        entity.setValidFrom(LocalDate.of(2025,1,1));
        entity.setFamilySituation(FamilySituation.SINGLE_OR_OTHER);
        entity.setDescendantsCount(0);
        entity.setAscendantsCount(0);
        entity.setDisabilityDegree(DisabilityDegree.NONE);
        entity.setPensionCompensatoria(false);
        entity.setGeographicMobility(false);
        entity.setHabitualResidenceLoan(false);
        entity.setTaxTerritory(TaxTerritory.COMUN);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        when(springDataRepo.findFirstByEmployeeIdAndValidFromLessThanEqualOrderByValidFromDesc(any(), any()))
            .thenReturn(Optional.of(entity));

        var result = adapter.findLatestOnOrBefore(1L, LocalDate.of(2025,1,1));
        assertTrue(result.isPresent());
        assertEquals(5L, result.get().getId());
        assertEquals(FamilySituation.SINGLE_OR_OTHER, result.get().getFamilySituation());
    }
}
```

- [ ] **Run test — verify it fails**

```
mvn test -Dtest=EmployeeTaxInformationPersistenceAdapterTest
```

- [ ] **Implement persistence layer**

`EmployeeTaxInformationEntity.java`:
```java
package com.b4rrhh.employee.tax_information.infrastructure.persistence;

import com.b4rrhh.employee.tax_information.domain.model.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(schema = "employee", name = "employee_tax_information",
    uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id","valid_from"}))
public class EmployeeTaxInformationEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "employee_id", nullable = false) private Long employeeId;
    @Column(name = "valid_from", nullable = false) private LocalDate validFrom;
    @Enumerated(EnumType.STRING) @Column(name = "family_situation", nullable = false)
    private FamilySituation familySituation;
    @Column(name = "descendants_count", nullable = false) private int descendantsCount;
    @Column(name = "ascendants_count", nullable = false) private int ascendantsCount;
    @Enumerated(EnumType.STRING) @Column(name = "disability_degree", nullable = false)
    private DisabilityDegree disabilityDegree;
    @Column(name = "pension_compensatoria", nullable = false) private boolean pensionCompensatoria;
    @Column(name = "geographic_mobility", nullable = false) private boolean geographicMobility;
    @Column(name = "habitual_residence_loan", nullable = false) private boolean habitualResidenceLoan;
    @Enumerated(EnumType.STRING) @Column(name = "tax_territory", nullable = false)
    private TaxTerritory taxTerritory;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    // getters and setters for all fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public FamilySituation getFamilySituation() { return familySituation; }
    public void setFamilySituation(FamilySituation familySituation) { this.familySituation = familySituation; }
    public int getDescendantsCount() { return descendantsCount; }
    public void setDescendantsCount(int descendantsCount) { this.descendantsCount = descendantsCount; }
    public int getAscendantsCount() { return ascendantsCount; }
    public void setAscendantsCount(int ascendantsCount) { this.ascendantsCount = ascendantsCount; }
    public DisabilityDegree getDisabilityDegree() { return disabilityDegree; }
    public void setDisabilityDegree(DisabilityDegree disabilityDegree) { this.disabilityDegree = disabilityDegree; }
    public boolean isPensionCompensatoria() { return pensionCompensatoria; }
    public void setPensionCompensatoria(boolean pensionCompensatoria) { this.pensionCompensatoria = pensionCompensatoria; }
    public boolean isGeographicMobility() { return geographicMobility; }
    public void setGeographicMobility(boolean geographicMobility) { this.geographicMobility = geographicMobility; }
    public boolean isHabitualResidenceLoan() { return habitualResidenceLoan; }
    public void setHabitualResidenceLoan(boolean habitualResidenceLoan) { this.habitualResidenceLoan = habitualResidenceLoan; }
    public TaxTerritory getTaxTerritory() { return taxTerritory; }
    public void setTaxTerritory(TaxTerritory taxTerritory) { this.taxTerritory = taxTerritory; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

`SpringDataEmployeeTaxInformationRepository.java`:
```java
package com.b4rrhh.employee.tax_information.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataEmployeeTaxInformationRepository
        extends JpaRepository<EmployeeTaxInformationEntity, Long> {
    List<EmployeeTaxInformationEntity> findByEmployeeIdOrderByValidFromDesc(Long employeeId);
    Optional<EmployeeTaxInformationEntity> findByEmployeeIdAndValidFrom(Long employeeId, LocalDate validFrom);
    Optional<EmployeeTaxInformationEntity> findFirstByEmployeeIdAndValidFromLessThanEqualOrderByValidFromDesc(
            Long employeeId, LocalDate date);
    void deleteByEmployeeIdAndValidFrom(Long employeeId, LocalDate validFrom);
}
```

`EmployeeTaxInformationPersistenceAdapter.java`:
```java
package com.b4rrhh.employee.tax_information.infrastructure.persistence;

import com.b4rrhh.employee.tax_information.domain.model.EmployeeTaxInformation;
import com.b4rrhh.employee.tax_information.domain.port.EmployeeTaxInformationRepository;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class EmployeeTaxInformationPersistenceAdapter implements EmployeeTaxInformationRepository {

    private final SpringDataEmployeeTaxInformationRepository springDataRepo;

    public EmployeeTaxInformationPersistenceAdapter(SpringDataEmployeeTaxInformationRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public EmployeeTaxInformation save(EmployeeTaxInformation domain) {
        EmployeeTaxInformationEntity entity = toEntity(domain);
        if (entity.getCreatedAt() == null) entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return toDomain(springDataRepo.save(entity));
    }

    @Override
    public Optional<EmployeeTaxInformation> findByEmployeeIdAndValidFrom(Long employeeId, LocalDate validFrom) {
        return springDataRepo.findByEmployeeIdAndValidFrom(employeeId, validFrom).map(this::toDomain);
    }

    @Override
    public List<EmployeeTaxInformation> findAllByEmployeeIdOrderByValidFromDesc(Long employeeId) {
        return springDataRepo.findByEmployeeIdOrderByValidFromDesc(employeeId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<EmployeeTaxInformation> findLatestOnOrBefore(Long employeeId, LocalDate referenceDate) {
        return springDataRepo
                .findFirstByEmployeeIdAndValidFromLessThanEqualOrderByValidFromDesc(employeeId, referenceDate)
                .map(this::toDomain);
    }

    @Override
    public void deleteByEmployeeIdAndValidFrom(Long employeeId, LocalDate validFrom) {
        springDataRepo.deleteByEmployeeIdAndValidFrom(employeeId, validFrom);
    }

    private EmployeeTaxInformation toDomain(EmployeeTaxInformationEntity e) {
        return EmployeeTaxInformation.rehydrate(e.getId(), e.getEmployeeId(), e.getValidFrom(),
            e.getFamilySituation(), e.getDescendantsCount(), e.getAscendantsCount(),
            e.getDisabilityDegree(), e.isPensionCompensatoria(), e.isGeographicMobility(),
            e.isHabitualResidenceLoan(), e.getTaxTerritory(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private EmployeeTaxInformationEntity toEntity(EmployeeTaxInformation d) {
        EmployeeTaxInformationEntity e = new EmployeeTaxInformationEntity();
        e.setId(d.getId());
        e.setEmployeeId(d.getEmployeeId());
        e.setValidFrom(d.getValidFrom());
        e.setFamilySituation(d.getFamilySituation());
        e.setDescendantsCount(d.getDescendantsCount());
        e.setAscendantsCount(d.getAscendantsCount());
        e.setDisabilityDegree(d.getDisabilityDegree());
        e.setPensionCompensatoria(d.isPensionCompensatoria());
        e.setGeographicMobility(d.isGeographicMobility());
        e.setHabitualResidenceLoan(d.isHabitualResidenceLoan());
        e.setTaxTerritory(d.getTaxTerritory());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }
}
```

- [ ] **Run test — verify it passes**

```
mvn test -Dtest=EmployeeTaxInformationPersistenceAdapterTest
```

- [ ] **Commit**

```
git add src/main/java/com/b4rrhh/employee/tax_information/infrastructure/persistence/
git add src/test/java/com/b4rrhh/employee/tax_information/infrastructure/persistence/
git commit -m "feat(tax-information): JPA entity, SpringData repo, persistence adapter"
```

---

## Task 4 — Application Use Cases

**Files:**
- Create (all under `application/usecase/`):
  - `CreateEmployeeTaxInformationUseCase.java`, `CreateEmployeeTaxInformationCommand.java`, `CreateEmployeeTaxInformationService.java`
  - `CorrectEmployeeTaxInformationUseCase.java`, `CorrectEmployeeTaxInformationCommand.java`, `CorrectEmployeeTaxInformationService.java`
  - `DeleteEmployeeTaxInformationUseCase.java`, `DeleteEmployeeTaxInformationCommand.java`, `DeleteEmployeeTaxInformationService.java`
  - `GetEmployeeTaxInformationUseCase.java`, `GetEmployeeTaxInformationCommand.java`, `GetEmployeeTaxInformationService.java`
  - `ListEmployeeTaxInformationUseCase.java`, `ListEmployeeTaxInformationCommand.java`, `ListEmployeeTaxInformationService.java`
- Create (under `application/port/`):
  - `EmployeeForTaxInfoLookupPort.java`, `TaxInfoPresenceLookupPort.java`
- Create (adapters under `infrastructure/persistence/`):
  - `EmployeeForTaxInfoLookupAdapter.java`, `TaxInfoPresenceLookupAdapter.java`
- Test: `src/test/java/com/b4rrhh/employee/tax_information/application/usecase/CreateEmployeeTaxInformationServiceTest.java`
- Test: `src/test/java/com/b4rrhh/employee/tax_information/application/usecase/CorrectEmployeeTaxInformationServiceTest.java`

- [ ] **Write failing tests**

`CreateEmployeeTaxInformationServiceTest.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;

import com.b4rrhh.employee.tax_information.application.port.EmployeeForTaxInfoLookupPort;
import com.b4rrhh.employee.tax_information.application.port.TaxInfoPresenceLookupPort;
import com.b4rrhh.employee.tax_information.domain.exception.EmployeeTaxInformationAlreadyExistsException;
import com.b4rrhh.employee.tax_information.domain.exception.EmployeeTaxInformationEmployeeNotFoundException;
import com.b4rrhh.employee.tax_information.domain.exception.EmployeeTaxInformationInvalidValidFromException;
import com.b4rrhh.employee.tax_information.domain.model.*;
import com.b4rrhh.employee.tax_information.domain.port.EmployeeTaxInformationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateEmployeeTaxInformationServiceTest {

    @Mock EmployeeTaxInformationRepository repo;
    @Mock EmployeeForTaxInfoLookupPort employeeLookupPort;
    @Mock TaxInfoPresenceLookupPort presenceLookupPort;

    private CreateEmployeeTaxInformationService service;

    @BeforeEach
    void setUp() {
        service = new CreateEmployeeTaxInformationService(repo, employeeLookupPort, presenceLookupPort);
    }

    private CreateEmployeeTaxInformationCommand cmdForFirstOfMonth() {
        return new CreateEmployeeTaxInformationCommand("ESP", "INTERNAL", "EMP001",
            LocalDate.of(2025,1,1), FamilySituation.SINGLE_OR_OTHER, 0, 0,
            DisabilityDegree.NONE, false, false, false, TaxTerritory.COMUN);
    }

    @Test
    void create_savesRecord_whenValidFromIsFirstOfMonth() {
        when(employeeLookupPort.findEmployeeId("ESP","INTERNAL","EMP001")).thenReturn(Optional.of(42L));
        when(repo.findByEmployeeIdAndValidFrom(any(), any())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.create(cmdForFirstOfMonth());

        assertEquals(FamilySituation.SINGLE_OR_OTHER, result.getFamilySituation());
        verify(repo).save(any());
    }

    @Test
    void create_throwsEmployeeNotFound_whenEmployeeMissing() {
        when(employeeLookupPort.findEmployeeId(any(),any(),any())).thenReturn(Optional.empty());
        assertThrows(EmployeeTaxInformationEmployeeNotFoundException.class,
            () -> service.create(cmdForFirstOfMonth()));
        verify(repo, never()).save(any());
    }

    @Test
    void create_throwsAlreadyExists_whenDuplicateValidFrom() {
        when(employeeLookupPort.findEmployeeId(any(),any(),any())).thenReturn(Optional.of(42L));
        when(repo.findByEmployeeIdAndValidFrom(any(), any())).thenReturn(Optional.of(EmployeeTaxInformation.DEFAULT));
        assertThrows(EmployeeTaxInformationAlreadyExistsException.class,
            () -> service.create(cmdForFirstOfMonth()));
    }

    @Test
    void create_throwsInvalidValidFrom_whenMidMonthAndNotPresenceStart() {
        when(employeeLookupPort.findEmployeeId(any(),any(),any())).thenReturn(Optional.of(42L));
        when(presenceLookupPort.isPresenceStartDate(42L, LocalDate.of(2025,1,15))).thenReturn(false);

        var cmd = new CreateEmployeeTaxInformationCommand("ESP","INTERNAL","EMP001",
            LocalDate.of(2025,1,15), FamilySituation.SINGLE_OR_OTHER, 0, 0,
            DisabilityDegree.NONE, false, false, false, TaxTerritory.COMUN);
        assertThrows(EmployeeTaxInformationInvalidValidFromException.class, () -> service.create(cmd));
    }

    @Test
    void create_allowsMidMonthValidFrom_whenMatchesPresenceStart() {
        when(employeeLookupPort.findEmployeeId(any(),any(),any())).thenReturn(Optional.of(42L));
        when(presenceLookupPort.isPresenceStartDate(42L, LocalDate.of(2025,1,15))).thenReturn(true);
        when(repo.findByEmployeeIdAndValidFrom(any(), any())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        var cmd = new CreateEmployeeTaxInformationCommand("ESP","INTERNAL","EMP001",
            LocalDate.of(2025,1,15), FamilySituation.SINGLE_OR_OTHER, 0, 0,
            DisabilityDegree.NONE, false, false, false, TaxTerritory.COMUN);
        assertDoesNotThrow(() -> service.create(cmd));
    }
}
```

`CorrectEmployeeTaxInformationServiceTest.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;

import com.b4rrhh.employee.tax_information.application.port.EmployeeForTaxInfoLookupPort;
import com.b4rrhh.employee.tax_information.domain.exception.EmployeeTaxInformationNotFoundException;
import com.b4rrhh.employee.tax_information.domain.model.*;
import com.b4rrhh.employee.tax_information.domain.port.EmployeeTaxInformationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrectEmployeeTaxInformationServiceTest {

    @Mock EmployeeTaxInformationRepository repo;
    @Mock EmployeeForTaxInfoLookupPort employeeLookupPort;

    private CorrectEmployeeTaxInformationService service;

    @BeforeEach
    void setUp() {
        service = new CorrectEmployeeTaxInformationService(repo, employeeLookupPort);
    }

    @Test
    void correct_updatesFields_preservingValidFrom() {
        when(employeeLookupPort.findEmployeeId(any(),any(),any())).thenReturn(Optional.of(42L));
        var existing = EmployeeTaxInformation.rehydrate(7L, 42L, LocalDate.of(2025,1,1),
            FamilySituation.SINGLE_OR_OTHER, 0, 0,
            DisabilityDegree.NONE, false, false, false, TaxTerritory.COMUN,
            LocalDateTime.now(), LocalDateTime.now());
        when(repo.findByEmployeeIdAndValidFrom(42L, LocalDate.of(2025,1,1)))
            .thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        var cmd = new CorrectEmployeeTaxInformationCommand("ESP","INTERNAL","EMP001",
            LocalDate.of(2025,1,1), FamilySituation.MARRIED_DEPENDENT_SPOUSE, 2, 0,
            DisabilityDegree.NONE, false, false, false, TaxTerritory.BIZKAIA);

        var result = service.correct(cmd);

        assertEquals(7L, result.getId());
        assertEquals(LocalDate.of(2025,1,1), result.getValidFrom());
        assertEquals(FamilySituation.MARRIED_DEPENDENT_SPOUSE, result.getFamilySituation());
        assertEquals(TaxTerritory.BIZKAIA, result.getTaxTerritory());
    }

    @Test
    void correct_throwsNotFound_whenRecordMissing() {
        when(employeeLookupPort.findEmployeeId(any(),any(),any())).thenReturn(Optional.of(42L));
        when(repo.findByEmployeeIdAndValidFrom(any(), any())).thenReturn(Optional.empty());
        var cmd = new CorrectEmployeeTaxInformationCommand("ESP","INTERNAL","EMP001",
            LocalDate.of(2025,1,1), FamilySituation.SINGLE_OR_OTHER, 0, 0,
            DisabilityDegree.NONE, false, false, false, TaxTerritory.COMUN);
        assertThrows(EmployeeTaxInformationNotFoundException.class, () -> service.correct(cmd));
    }
}
```

- [ ] **Run tests — verify they fail**

```
mvn test -Dtest=CreateEmployeeTaxInformationServiceTest,CorrectEmployeeTaxInformationServiceTest
```

- [ ] **Implement secondary ports**

`EmployeeForTaxInfoLookupPort.java`:
```java
package com.b4rrhh.employee.tax_information.application.port;
import java.util.Optional;
public interface EmployeeForTaxInfoLookupPort {
    Optional<Long> findEmployeeId(String ruleSystemCode, String employeeTypeCode, String employeeNumber);
}
```

`TaxInfoPresenceLookupPort.java`:
```java
package com.b4rrhh.employee.tax_information.application.port;
import java.time.LocalDate;
public interface TaxInfoPresenceLookupPort {
    boolean isPresenceStartDate(Long employeeId, LocalDate date);
}
```

`EmployeeForTaxInfoLookupAdapter.java`:
```java
package com.b4rrhh.employee.tax_information.infrastructure.persistence;

import com.b4rrhh.employee.tax_information.application.port.EmployeeForTaxInfoLookupPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class EmployeeForTaxInfoLookupAdapter implements EmployeeForTaxInfoLookupPort {

    private final JdbcTemplate jdbc;

    public EmployeeForTaxInfoLookupAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Long> findEmployeeId(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        var ids = jdbc.queryForList(
            "SELECT id FROM employee.employee WHERE rule_system_code = ? AND employee_type_code = ? AND employee_number = ?",
            Long.class, ruleSystemCode, employeeTypeCode, employeeNumber);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }
}
```

`TaxInfoPresenceLookupAdapter.java`:
```java
package com.b4rrhh.employee.tax_information.infrastructure.persistence;

import com.b4rrhh.employee.tax_information.application.port.TaxInfoPresenceLookupPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class TaxInfoPresenceLookupAdapter implements TaxInfoPresenceLookupPort {

    private final JdbcTemplate jdbc;

    public TaxInfoPresenceLookupAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isPresenceStartDate(Long employeeId, LocalDate date) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM employee.presence WHERE employee_id = ? AND start_date = ?",
            Integer.class, employeeId, date);
        return count != null && count > 0;
    }
}
```

- [ ] **Implement use case interfaces and commands**

`CreateEmployeeTaxInformationCommand.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;

import com.b4rrhh.employee.tax_information.domain.model.*;
import java.time.LocalDate;

public record CreateEmployeeTaxInformationCommand(
    String ruleSystemCode, String employeeTypeCode, String employeeNumber,
    LocalDate validFrom,
    FamilySituation familySituation, int descendantsCount, int ascendantsCount,
    DisabilityDegree disabilityDegree,
    boolean pensionCompensatoria, boolean geographicMobility, boolean habitualResidenceLoan,
    TaxTerritory taxTerritory
) {}
```

`CreateEmployeeTaxInformationUseCase.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;

import com.b4rrhh.employee.tax_information.domain.model.EmployeeTaxInformation;

public interface CreateEmployeeTaxInformationUseCase {
    EmployeeTaxInformation create(CreateEmployeeTaxInformationCommand command);
}
```

`CorrectEmployeeTaxInformationCommand.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;

import com.b4rrhh.employee.tax_information.domain.model.*;
import java.time.LocalDate;

public record CorrectEmployeeTaxInformationCommand(
    String ruleSystemCode, String employeeTypeCode, String employeeNumber,
    LocalDate validFrom,
    FamilySituation familySituation, int descendantsCount, int ascendantsCount,
    DisabilityDegree disabilityDegree,
    boolean pensionCompensatoria, boolean geographicMobility, boolean habitualResidenceLoan,
    TaxTerritory taxTerritory
) {}
```

`CorrectEmployeeTaxInformationUseCase.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;

import com.b4rrhh.employee.tax_information.domain.model.EmployeeTaxInformation;

public interface CorrectEmployeeTaxInformationUseCase {
    EmployeeTaxInformation correct(CorrectEmployeeTaxInformationCommand command);
}
```

`DeleteEmployeeTaxInformationCommand.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;
import java.time.LocalDate;
public record DeleteEmployeeTaxInformationCommand(
    String ruleSystemCode, String employeeTypeCode, String employeeNumber, LocalDate validFrom) {}
```

`DeleteEmployeeTaxInformationUseCase.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;
public interface DeleteEmployeeTaxInformationUseCase {
    void delete(DeleteEmployeeTaxInformationCommand command);
}
```

`GetEmployeeTaxInformationCommand.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;
import java.time.LocalDate;
public record GetEmployeeTaxInformationCommand(
    String ruleSystemCode, String employeeTypeCode, String employeeNumber, LocalDate validFrom) {}
```

`GetEmployeeTaxInformationUseCase.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;
import com.b4rrhh.employee.tax_information.domain.model.EmployeeTaxInformation;
public interface GetEmployeeTaxInformationUseCase {
    EmployeeTaxInformation get(GetEmployeeTaxInformationCommand command);
}
```

`ListEmployeeTaxInformationCommand.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;
public record ListEmployeeTaxInformationCommand(
    String ruleSystemCode, String employeeTypeCode, String employeeNumber) {}
```

`ListEmployeeTaxInformationUseCase.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;
import com.b4rrhh.employee.tax_information.domain.model.EmployeeTaxInformation;
import java.util.List;
public interface ListEmployeeTaxInformationUseCase {
    List<EmployeeTaxInformation> list(ListEmployeeTaxInformationCommand command);
}
```

- [ ] **Implement service classes**

`CreateEmployeeTaxInformationService.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;

import com.b4rrhh.employee.tax_information.application.port.EmployeeForTaxInfoLookupPort;
import com.b4rrhh.employee.tax_information.application.port.TaxInfoPresenceLookupPort;
import com.b4rrhh.employee.tax_information.domain.exception.*;
import com.b4rrhh.employee.tax_information.domain.model.EmployeeTaxInformation;
import com.b4rrhh.employee.tax_information.domain.port.EmployeeTaxInformationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateEmployeeTaxInformationService implements CreateEmployeeTaxInformationUseCase {

    private final EmployeeTaxInformationRepository repo;
    private final EmployeeForTaxInfoLookupPort employeeLookupPort;
    private final TaxInfoPresenceLookupPort presenceLookupPort;

    public CreateEmployeeTaxInformationService(EmployeeTaxInformationRepository repo,
            EmployeeForTaxInfoLookupPort employeeLookupPort,
            TaxInfoPresenceLookupPort presenceLookupPort) {
        this.repo = repo;
        this.employeeLookupPort = employeeLookupPort;
        this.presenceLookupPort = presenceLookupPort;
    }

    @Override
    @Transactional
    public EmployeeTaxInformation create(CreateEmployeeTaxInformationCommand cmd) {
        String rs = cmd.ruleSystemCode().trim().toUpperCase();
        String type = cmd.employeeTypeCode().trim().toUpperCase();
        String num = cmd.employeeNumber().trim();

        Long employeeId = employeeLookupPort.findEmployeeId(rs, type, num)
            .orElseThrow(() -> new EmployeeTaxInformationEmployeeNotFoundException(rs, type, num));

        if (cmd.validFrom().getDayOfMonth() != 1
                && !presenceLookupPort.isPresenceStartDate(employeeId, cmd.validFrom())) {
            throw new EmployeeTaxInformationInvalidValidFromException(cmd.validFrom());
        }

        if (repo.findByEmployeeIdAndValidFrom(employeeId, cmd.validFrom()).isPresent()) {
            throw new EmployeeTaxInformationAlreadyExistsException(employeeId, cmd.validFrom());
        }

        return repo.save(EmployeeTaxInformation.create(employeeId, cmd.validFrom(),
            cmd.familySituation(), cmd.descendantsCount(), cmd.ascendantsCount(),
            cmd.disabilityDegree(), cmd.pensionCompensatoria(),
            cmd.geographicMobility(), cmd.habitualResidenceLoan(), cmd.taxTerritory()));
    }
}
```

`CorrectEmployeeTaxInformationService.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;

import com.b4rrhh.employee.tax_information.application.port.EmployeeForTaxInfoLookupPort;
import com.b4rrhh.employee.tax_information.domain.exception.*;
import com.b4rrhh.employee.tax_information.domain.model.EmployeeTaxInformation;
import com.b4rrhh.employee.tax_information.domain.port.EmployeeTaxInformationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorrectEmployeeTaxInformationService implements CorrectEmployeeTaxInformationUseCase {

    private final EmployeeTaxInformationRepository repo;
    private final EmployeeForTaxInfoLookupPort employeeLookupPort;

    public CorrectEmployeeTaxInformationService(EmployeeTaxInformationRepository repo,
            EmployeeForTaxInfoLookupPort employeeLookupPort) {
        this.repo = repo;
        this.employeeLookupPort = employeeLookupPort;
    }

    @Override
    @Transactional
    public EmployeeTaxInformation correct(CorrectEmployeeTaxInformationCommand cmd) {
        String rs = cmd.ruleSystemCode().trim().toUpperCase();
        String type = cmd.employeeTypeCode().trim().toUpperCase();
        String num = cmd.employeeNumber().trim();

        Long employeeId = employeeLookupPort.findEmployeeId(rs, type, num)
            .orElseThrow(() -> new EmployeeTaxInformationEmployeeNotFoundException(rs, type, num));

        EmployeeTaxInformation existing = repo.findByEmployeeIdAndValidFrom(employeeId, cmd.validFrom())
            .orElseThrow(() -> new EmployeeTaxInformationNotFoundException(employeeId, cmd.validFrom()));

        return repo.save(existing.correct(cmd.familySituation(), cmd.descendantsCount(),
            cmd.ascendantsCount(), cmd.disabilityDegree(), cmd.pensionCompensatoria(),
            cmd.geographicMobility(), cmd.habitualResidenceLoan(), cmd.taxTerritory()));
    }
}
```

`DeleteEmployeeTaxInformationService.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;

import com.b4rrhh.employee.tax_information.application.port.EmployeeForTaxInfoLookupPort;
import com.b4rrhh.employee.tax_information.domain.exception.*;
import com.b4rrhh.employee.tax_information.domain.port.EmployeeTaxInformationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteEmployeeTaxInformationService implements DeleteEmployeeTaxInformationUseCase {

    private final EmployeeTaxInformationRepository repo;
    private final EmployeeForTaxInfoLookupPort employeeLookupPort;

    public DeleteEmployeeTaxInformationService(EmployeeTaxInformationRepository repo,
            EmployeeForTaxInfoLookupPort employeeLookupPort) {
        this.repo = repo;
        this.employeeLookupPort = employeeLookupPort;
    }

    @Override
    @Transactional
    public void delete(DeleteEmployeeTaxInformationCommand cmd) {
        String rs = cmd.ruleSystemCode().trim().toUpperCase();
        String type = cmd.employeeTypeCode().trim().toUpperCase();
        String num = cmd.employeeNumber().trim();

        Long employeeId = employeeLookupPort.findEmployeeId(rs, type, num)
            .orElseThrow(() -> new EmployeeTaxInformationEmployeeNotFoundException(rs, type, num));

        if (repo.findByEmployeeIdAndValidFrom(employeeId, cmd.validFrom()).isEmpty()) {
            throw new EmployeeTaxInformationNotFoundException(employeeId, cmd.validFrom());
        }

        repo.deleteByEmployeeIdAndValidFrom(employeeId, cmd.validFrom());
    }
}
```

`GetEmployeeTaxInformationService.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;

import com.b4rrhh.employee.tax_information.application.port.EmployeeForTaxInfoLookupPort;
import com.b4rrhh.employee.tax_information.domain.exception.*;
import com.b4rrhh.employee.tax_information.domain.model.EmployeeTaxInformation;
import com.b4rrhh.employee.tax_information.domain.port.EmployeeTaxInformationRepository;
import org.springframework.stereotype.Service;

@Service
public class GetEmployeeTaxInformationService implements GetEmployeeTaxInformationUseCase {

    private final EmployeeTaxInformationRepository repo;
    private final EmployeeForTaxInfoLookupPort employeeLookupPort;

    public GetEmployeeTaxInformationService(EmployeeTaxInformationRepository repo,
            EmployeeForTaxInfoLookupPort employeeLookupPort) {
        this.repo = repo;
        this.employeeLookupPort = employeeLookupPort;
    }

    @Override
    public EmployeeTaxInformation get(GetEmployeeTaxInformationCommand cmd) {
        String rs = cmd.ruleSystemCode().trim().toUpperCase();
        String type = cmd.employeeTypeCode().trim().toUpperCase();
        String num = cmd.employeeNumber().trim();

        Long employeeId = employeeLookupPort.findEmployeeId(rs, type, num)
            .orElseThrow(() -> new EmployeeTaxInformationEmployeeNotFoundException(rs, type, num));

        return repo.findByEmployeeIdAndValidFrom(employeeId, cmd.validFrom())
            .orElseThrow(() -> new EmployeeTaxInformationNotFoundException(employeeId, cmd.validFrom()));
    }
}
```

`ListEmployeeTaxInformationService.java`:
```java
package com.b4rrhh.employee.tax_information.application.usecase;

import com.b4rrhh.employee.tax_information.application.port.EmployeeForTaxInfoLookupPort;
import com.b4rrhh.employee.tax_information.domain.exception.EmployeeTaxInformationEmployeeNotFoundException;
import com.b4rrhh.employee.tax_information.domain.model.EmployeeTaxInformation;
import com.b4rrhh.employee.tax_information.domain.port.EmployeeTaxInformationRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ListEmployeeTaxInformationService implements ListEmployeeTaxInformationUseCase {

    private final EmployeeTaxInformationRepository repo;
    private final EmployeeForTaxInfoLookupPort employeeLookupPort;

    public ListEmployeeTaxInformationService(EmployeeTaxInformationRepository repo,
            EmployeeForTaxInfoLookupPort employeeLookupPort) {
        this.repo = repo;
        this.employeeLookupPort = employeeLookupPort;
    }

    @Override
    public List<EmployeeTaxInformation> list(ListEmployeeTaxInformationCommand cmd) {
        String rs = cmd.ruleSystemCode().trim().toUpperCase();
        String type = cmd.employeeTypeCode().trim().toUpperCase();
        String num = cmd.employeeNumber().trim();

        Long employeeId = employeeLookupPort.findEmployeeId(rs, type, num)
            .orElseThrow(() -> new EmployeeTaxInformationEmployeeNotFoundException(rs, type, num));

        return repo.findAllByEmployeeIdOrderByValidFromDesc(employeeId);
    }
}
```

- [ ] **Run tests — verify they pass**

```
mvn test -Dtest=CreateEmployeeTaxInformationServiceTest,CorrectEmployeeTaxInformationServiceTest
```
Expected: 7 tests passing.

- [ ] **Commit**

```
git add src/main/java/com/b4rrhh/employee/tax_information/application/
git add src/main/java/com/b4rrhh/employee/tax_information/infrastructure/persistence/EmployeeForTaxInfoLookupAdapter.java
git add src/main/java/com/b4rrhh/employee/tax_information/infrastructure/persistence/TaxInfoPresenceLookupAdapter.java
git add src/test/java/com/b4rrhh/employee/tax_information/application/
git commit -m "feat(tax-information): use cases — create, correct, delete, get, list + lookup adapters"
```

---

## Task 5 — Web Layer

**Files:**
- Create: `infrastructure/web/dto/CreateEmployeeTaxInformationRequest.java`
- Create: `infrastructure/web/dto/CorrectEmployeeTaxInformationRequest.java`
- Create: `infrastructure/web/dto/EmployeeTaxInformationResponse.java`
- Create: `infrastructure/web/dto/EmployeeTaxInformationErrorResponse.java`
- Create: `infrastructure/web/assembler/EmployeeTaxInformationAssembler.java`
- Create: `infrastructure/web/EmployeeTaxInformationBusinessKeyController.java`
- Create: `infrastructure/web/EmployeeTaxInformationExceptionHandler.java`
- Test: `src/test/java/com/b4rrhh/employee/tax_information/infrastructure/web/assembler/EmployeeTaxInformationAssemblerTest.java`

- [ ] **Write failing test**

```java
package com.b4rrhh.employee.tax_information.infrastructure.web.assembler;

import com.b4rrhh.employee.tax_information.domain.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class EmployeeTaxInformationAssemblerTest {

    private final EmployeeTaxInformationAssembler assembler = new EmployeeTaxInformationAssembler();

    @Test
    void toResponse_mapsAllFields() {
        var domain = EmployeeTaxInformation.rehydrate(1L, 42L, LocalDate.of(2025,1,1),
            FamilySituation.MARRIED_DEPENDENT_SPOUSE, 2, 1,
            DisabilityDegree.MODERATE, true, false, true, TaxTerritory.BIZKAIA,
            LocalDateTime.now(), LocalDateTime.now());

        var response = assembler.toResponse(domain);

        assertEquals("2025-01-01", response.validFrom());
        assertEquals("MARRIED_DEPENDENT_SPOUSE", response.familySituation());
        assertEquals(2, response.descendantsCount());
        assertEquals(1, response.ascendantsCount());
        assertEquals("MODERATE", response.disabilityDegree());
        assertTrue(response.pensionCompensatoria());
        assertFalse(response.geographicMobility());
        assertTrue(response.habitualResidenceLoan());
        assertEquals("BIZKAIA", response.taxTerritory());
    }
}
```

- [ ] **Run test — verify it fails**

```
mvn test -Dtest=EmployeeTaxInformationAssemblerTest
```

- [ ] **Implement web layer**

`EmployeeTaxInformationResponse.java`:
```java
package com.b4rrhh.employee.tax_information.infrastructure.web.dto;

public record EmployeeTaxInformationResponse(
    String validFrom,
    String familySituation,
    int descendantsCount,
    int ascendantsCount,
    String disabilityDegree,
    boolean pensionCompensatoria,
    boolean geographicMobility,
    boolean habitualResidenceLoan,
    String taxTerritory
) {}
```

`EmployeeTaxInformationErrorResponse.java`:
```java
package com.b4rrhh.employee.tax_information.infrastructure.web.dto;
import java.util.Map;
public record EmployeeTaxInformationErrorResponse(String code, String message, Map<String,Object> details) {}
```

`CreateEmployeeTaxInformationRequest.java`:
```java
package com.b4rrhh.employee.tax_information.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.time.LocalDate;

public class CreateEmployeeTaxInformationRequest {
    private LocalDate validFrom;
    private String familySituation;
    private int descendantsCount;
    private int ascendantsCount;
    private String disabilityDegree;
    private boolean pensionCompensatoria;
    private boolean geographicMobility;
    private boolean habitualResidenceLoan;
    private String taxTerritory;

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("Unexpected field: " + fieldName);
    }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public String getFamilySituation() { return familySituation; }
    public void setFamilySituation(String familySituation) { this.familySituation = familySituation; }
    public int getDescendantsCount() { return descendantsCount; }
    public void setDescendantsCount(int descendantsCount) { this.descendantsCount = descendantsCount; }
    public int getAscendantsCount() { return ascendantsCount; }
    public void setAscendantsCount(int ascendantsCount) { this.ascendantsCount = ascendantsCount; }
    public String getDisabilityDegree() { return disabilityDegree; }
    public void setDisabilityDegree(String disabilityDegree) { this.disabilityDegree = disabilityDegree; }
    public boolean isPensionCompensatoria() { return pensionCompensatoria; }
    public void setPensionCompensatoria(boolean pensionCompensatoria) { this.pensionCompensatoria = pensionCompensatoria; }
    public boolean isGeographicMobility() { return geographicMobility; }
    public void setGeographicMobility(boolean geographicMobility) { this.geographicMobility = geographicMobility; }
    public boolean isHabitualResidenceLoan() { return habitualResidenceLoan; }
    public void setHabitualResidenceLoan(boolean habitualResidenceLoan) { this.habitualResidenceLoan = habitualResidenceLoan; }
    public String getTaxTerritory() { return taxTerritory; }
    public void setTaxTerritory(String taxTerritory) { this.taxTerritory = taxTerritory; }
}
```

`CorrectEmployeeTaxInformationRequest.java` — identical structure to `CreateEmployeeTaxInformationRequest` but without `validFrom` (it comes from the path):
```java
package com.b4rrhh.employee.tax_information.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public class CorrectEmployeeTaxInformationRequest {
    private String familySituation;
    private int descendantsCount;
    private int ascendantsCount;
    private String disabilityDegree;
    private boolean pensionCompensatoria;
    private boolean geographicMobility;
    private boolean habitualResidenceLoan;
    private String taxTerritory;

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("Unexpected field: " + fieldName);
    }

    public String getFamilySituation() { return familySituation; }
    public void setFamilySituation(String familySituation) { this.familySituation = familySituation; }
    public int getDescendantsCount() { return descendantsCount; }
    public void setDescendantsCount(int descendantsCount) { this.descendantsCount = descendantsCount; }
    public int getAscendantsCount() { return ascendantsCount; }
    public void setAscendantsCount(int ascendantsCount) { this.ascendantsCount = ascendantsCount; }
    public String getDisabilityDegree() { return disabilityDegree; }
    public void setDisabilityDegree(String disabilityDegree) { this.disabilityDegree = disabilityDegree; }
    public boolean isPensionCompensatoria() { return pensionCompensatoria; }
    public void setPensionCompensatoria(boolean pensionCompensatoria) { this.pensionCompensatoria = pensionCompensatoria; }
    public boolean isGeographicMobility() { return geographicMobility; }
    public void setGeographicMobility(boolean geographicMobility) { this.geographicMobility = geographicMobility; }
    public boolean isHabitualResidenceLoan() { return habitualResidenceLoan; }
    public void setHabitualResidenceLoan(boolean habitualResidenceLoan) { this.habitualResidenceLoan = habitualResidenceLoan; }
    public String getTaxTerritory() { return taxTerritory; }
    public void setTaxTerritory(String taxTerritory) { this.taxTerritory = taxTerritory; }
}
```

`EmployeeTaxInformationAssembler.java`:
```java
package com.b4rrhh.employee.tax_information.infrastructure.web.assembler;

import com.b4rrhh.employee.tax_information.application.usecase.*;
import com.b4rrhh.employee.tax_information.domain.model.*;
import com.b4rrhh.employee.tax_information.infrastructure.web.dto.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class EmployeeTaxInformationAssembler {

    public EmployeeTaxInformationResponse toResponse(EmployeeTaxInformation domain) {
        return new EmployeeTaxInformationResponse(
            domain.getValidFrom().toString(),
            domain.getFamilySituation().name(),
            domain.getDescendantsCount(),
            domain.getAscendantsCount(),
            domain.getDisabilityDegree().name(),
            domain.isPensionCompensatoria(),
            domain.isGeographicMobility(),
            domain.isHabitualResidenceLoan(),
            domain.getTaxTerritory().name()
        );
    }

    public List<EmployeeTaxInformationResponse> toResponseList(List<EmployeeTaxInformation> list) {
        return list.stream().map(this::toResponse).toList();
    }

    public CreateEmployeeTaxInformationCommand toCreateCommand(
            String ruleSystemCode, String employeeTypeCode, String employeeNumber,
            CreateEmployeeTaxInformationRequest req) {
        return new CreateEmployeeTaxInformationCommand(
            ruleSystemCode, employeeTypeCode, employeeNumber,
            req.getValidFrom(),
            FamilySituation.valueOf(req.getFamilySituation()),
            req.getDescendantsCount(), req.getAscendantsCount(),
            DisabilityDegree.valueOf(req.getDisabilityDegree()),
            req.isPensionCompensatoria(), req.isGeographicMobility(),
            req.isHabitualResidenceLoan(),
            TaxTerritory.valueOf(req.getTaxTerritory())
        );
    }

    public CorrectEmployeeTaxInformationCommand toCorrectCommand(
            String ruleSystemCode, String employeeTypeCode, String employeeNumber,
            java.time.LocalDate validFrom,
            CorrectEmployeeTaxInformationRequest req) {
        return new CorrectEmployeeTaxInformationCommand(
            ruleSystemCode, employeeTypeCode, employeeNumber,
            validFrom,
            FamilySituation.valueOf(req.getFamilySituation()),
            req.getDescendantsCount(), req.getAscendantsCount(),
            DisabilityDegree.valueOf(req.getDisabilityDegree()),
            req.isPensionCompensatoria(), req.isGeographicMobility(),
            req.isHabitualResidenceLoan(),
            TaxTerritory.valueOf(req.getTaxTerritory())
        );
    }
}
```

`EmployeeTaxInformationBusinessKeyController.java`:
```java
package com.b4rrhh.employee.tax_information.infrastructure.web;

import com.b4rrhh.employee.tax_information.application.usecase.*;
import com.b4rrhh.employee.tax_information.infrastructure.web.assembler.EmployeeTaxInformationAssembler;
import com.b4rrhh.employee.tax_information.infrastructure.web.dto.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/tax-information")
public class EmployeeTaxInformationBusinessKeyController {

    private final CreateEmployeeTaxInformationUseCase createUseCase;
    private final CorrectEmployeeTaxInformationUseCase correctUseCase;
    private final DeleteEmployeeTaxInformationUseCase deleteUseCase;
    private final GetEmployeeTaxInformationUseCase getUseCase;
    private final ListEmployeeTaxInformationUseCase listUseCase;
    private final EmployeeTaxInformationAssembler assembler;

    public EmployeeTaxInformationBusinessKeyController(
            CreateEmployeeTaxInformationUseCase createUseCase,
            CorrectEmployeeTaxInformationUseCase correctUseCase,
            DeleteEmployeeTaxInformationUseCase deleteUseCase,
            GetEmployeeTaxInformationUseCase getUseCase,
            ListEmployeeTaxInformationUseCase listUseCase,
            EmployeeTaxInformationAssembler assembler) {
        this.createUseCase = createUseCase;
        this.correctUseCase = correctUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.assembler = assembler;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeTaxInformationResponse create(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody CreateEmployeeTaxInformationRequest request) {
        return assembler.toResponse(createUseCase.create(
            assembler.toCreateCommand(ruleSystemCode, employeeTypeCode, employeeNumber, request)));
    }

    @GetMapping
    public List<EmployeeTaxInformationResponse> list(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber) {
        return assembler.toResponseList(listUseCase.list(
            new ListEmployeeTaxInformationCommand(ruleSystemCode, employeeTypeCode, employeeNumber)));
    }

    @GetMapping("/{validFrom}")
    public EmployeeTaxInformationResponse get(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validFrom) {
        return assembler.toResponse(getUseCase.get(
            new GetEmployeeTaxInformationCommand(ruleSystemCode, employeeTypeCode, employeeNumber, validFrom)));
    }

    @PutMapping("/{validFrom}")
    public EmployeeTaxInformationResponse correct(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validFrom,
            @RequestBody CorrectEmployeeTaxInformationRequest request) {
        return assembler.toResponse(correctUseCase.correct(
            assembler.toCorrectCommand(ruleSystemCode, employeeTypeCode, employeeNumber, validFrom, request)));
    }

    @DeleteMapping("/{validFrom}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validFrom) {
        deleteUseCase.delete(
            new DeleteEmployeeTaxInformationCommand(ruleSystemCode, employeeTypeCode, employeeNumber, validFrom));
    }
}
```

`EmployeeTaxInformationExceptionHandler.java`:
```java
package com.b4rrhh.employee.tax_information.infrastructure.web;

import com.b4rrhh.employee.tax_information.domain.exception.*;
import com.b4rrhh.employee.tax_information.infrastructure.web.dto.EmployeeTaxInformationErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = EmployeeTaxInformationBusinessKeyController.class)
public class EmployeeTaxInformationExceptionHandler {

    @ExceptionHandler({EmployeeTaxInformationNotFoundException.class, EmployeeTaxInformationEmployeeNotFoundException.class})
    public ResponseEntity<EmployeeTaxInformationErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new EmployeeTaxInformationErrorResponse("TAX_INFORMATION_NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(EmployeeTaxInformationAlreadyExistsException.class)
    public ResponseEntity<EmployeeTaxInformationErrorResponse> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new EmployeeTaxInformationErrorResponse("TAX_INFORMATION_ALREADY_EXISTS", ex.getMessage(), null));
    }

    @ExceptionHandler({EmployeeTaxInformationInvalidValidFromException.class, IllegalArgumentException.class})
    public ResponseEntity<EmployeeTaxInformationErrorResponse> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new EmployeeTaxInformationErrorResponse("TAX_INFORMATION_INVALID_INPUT", ex.getMessage(), null));
    }
}
```

- [ ] **Run test — verify it passes**

```
mvn test -Dtest=EmployeeTaxInformationAssemblerTest
```

- [ ] **Commit**

```
git add src/main/java/com/b4rrhh/employee/tax_information/infrastructure/web/
git add src/test/java/com/b4rrhh/employee/tax_information/infrastructure/web/
git commit -m "feat(tax-information): web layer — controller, assembler, DTOs, exception handler"
```

---

## Task 6 — OpenAPI Spec

**Files:**
- Modify: `openapi/personnel-administration-api.yaml`

- [ ] **Add paths and schemas**

Add after the last `/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/...` path block:

```yaml
  /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/tax-information:
    post:
      summary: Register employee tax information (Modelo 145)
      tags: [Employee Tax Information]
      parameters:
        - $ref: '#/components/parameters/RuleSystemCode'
        - $ref: '#/components/parameters/EmployeeTypeCode'
        - $ref: '#/components/parameters/EmployeeNumber'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateEmployeeTaxInformationRequest'
      responses:
        '201':
          description: Created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EmployeeTaxInformationResponse'
        '400':
          description: Invalid input or validFrom date
        '404':
          description: Employee not found
        '409':
          description: Tax information already exists for this validFrom
    get:
      summary: List all tax information records for an employee
      tags: [Employee Tax Information]
      parameters:
        - $ref: '#/components/parameters/RuleSystemCode'
        - $ref: '#/components/parameters/EmployeeTypeCode'
        - $ref: '#/components/parameters/EmployeeNumber'
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/EmployeeTaxInformationResponse'

  /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/tax-information/{validFrom}:
    get:
      summary: Get a specific tax information record by validFrom date
      tags: [Employee Tax Information]
      parameters:
        - $ref: '#/components/parameters/RuleSystemCode'
        - $ref: '#/components/parameters/EmployeeTypeCode'
        - $ref: '#/components/parameters/EmployeeNumber'
        - name: validFrom
          in: path
          required: true
          schema:
            type: string
            format: date
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EmployeeTaxInformationResponse'
        '404':
          description: Not found
    put:
      summary: Correct (replace all fields of) a tax information record
      tags: [Employee Tax Information]
      parameters:
        - $ref: '#/components/parameters/RuleSystemCode'
        - $ref: '#/components/parameters/EmployeeTypeCode'
        - $ref: '#/components/parameters/EmployeeNumber'
        - name: validFrom
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
              $ref: '#/components/schemas/CorrectEmployeeTaxInformationRequest'
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EmployeeTaxInformationResponse'
        '404':
          description: Not found
    delete:
      summary: Delete a tax information record
      tags: [Employee Tax Information]
      parameters:
        - $ref: '#/components/parameters/RuleSystemCode'
        - $ref: '#/components/parameters/EmployeeTypeCode'
        - $ref: '#/components/parameters/EmployeeNumber'
        - name: validFrom
          in: path
          required: true
          schema:
            type: string
            format: date
      responses:
        '204':
          description: Deleted
        '404':
          description: Not found
```

Add to `components/schemas`:

```yaml
    CreateEmployeeTaxInformationRequest:
      type: object
      required: [validFrom, familySituation, descendantsCount, ascendantsCount, disabilityDegree, pensionCompensatoria, geographicMobility, habitualResidenceLoan, taxTerritory]
      properties:
        validFrom:
          type: string
          format: date
        familySituation:
          type: string
          enum: [SINGLE_OR_OTHER, MARRIED_DEPENDENT_SPOUSE, SEPARATED_WITH_CHILDREN]
        descendantsCount:
          type: integer
          minimum: 0
        ascendantsCount:
          type: integer
          minimum: 0
        disabilityDegree:
          type: string
          enum: [NONE, MODERATE, SEVERE]
        pensionCompensatoria:
          type: boolean
        geographicMobility:
          type: boolean
        habitualResidenceLoan:
          type: boolean
        taxTerritory:
          type: string
          enum: [COMUN, ARABA, GIPUZKOA, BIZKAIA, NAVARRA]

    CorrectEmployeeTaxInformationRequest:
      type: object
      required: [familySituation, descendantsCount, ascendantsCount, disabilityDegree, pensionCompensatoria, geographicMobility, habitualResidenceLoan, taxTerritory]
      properties:
        familySituation:
          type: string
          enum: [SINGLE_OR_OTHER, MARRIED_DEPENDENT_SPOUSE, SEPARATED_WITH_CHILDREN]
        descendantsCount:
          type: integer
          minimum: 0
        ascendantsCount:
          type: integer
          minimum: 0
        disabilityDegree:
          type: string
          enum: [NONE, MODERATE, SEVERE]
        pensionCompensatoria:
          type: boolean
        geographicMobility:
          type: boolean
        habitualResidenceLoan:
          type: boolean
        taxTerritory:
          type: string
          enum: [COMUN, ARABA, GIPUZKOA, BIZKAIA, NAVARRA]

    EmployeeTaxInformationResponse:
      type: object
      properties:
        validFrom:
          type: string
          format: date
        familySituation:
          type: string
        descendantsCount:
          type: integer
        ascendantsCount:
          type: integer
        disabilityDegree:
          type: string
        pensionCompensatoria:
          type: boolean
        geographicMobility:
          type: boolean
        habitualResidenceLoan:
          type: boolean
        taxTerritory:
          type: string
```

- [ ] **Build to verify no compilation errors**

```
mvn compile
```

- [ ] **Commit**

```
git add openapi/personnel-administration-api.yaml
git commit -m "feat(tax-information): OpenAPI spec — 5 endpoints, 3 schemas"
```

---

## Task 7 — Payroll Snapshot Integration

**Files:**
- Create: `src/main/java/com/b4rrhh/payroll/application/port/EmployeeTaxInfoContext.java`
- Create: `src/main/java/com/b4rrhh/payroll/application/port/EmployeeTaxInfoPayrollLookupPort.java`
- Create: `src/main/java/com/b4rrhh/employee/tax_information/infrastructure/persistence/EmployeeTaxInfoPayrollLookupAdapter.java`
- Modify: `src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java`
- Test: `src/test/java/com/b4rrhh/employee/tax_information/infrastructure/persistence/EmployeeTaxInfoPayrollLookupAdapterTest.java`

- [ ] **Write failing test**

```java
package com.b4rrhh.employee.tax_information.infrastructure.persistence;

import com.b4rrhh.employee.tax_information.domain.model.*;
import com.b4rrhh.payroll.application.port.EmployeeTaxInfoContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeTaxInfoPayrollLookupAdapterTest {

    @Mock SpringDataEmployeeTaxInformationRepository springDataRepo;
    @Mock EmployeeForTaxInfoLookupAdapter employeeLookupAdapter;
    @InjectMocks EmployeeTaxInfoPayrollLookupAdapter adapter;

    @Test
    void returnsDefault_whenNoRecordFound() {
        when(employeeLookupAdapter.findEmployeeId(any(),any(),any())).thenReturn(Optional.of(1L));
        when(springDataRepo.findFirstByEmployeeIdAndValidFromLessThanEqualOrderByValidFromDesc(any(), any()))
            .thenReturn(Optional.empty());

        var result = adapter.findLatestOnOrBefore("ESP", "INTERNAL", "EMP001", LocalDate.of(2025,1,1));

        assertEquals("SINGLE_OR_OTHER", result.familySituation());
        assertEquals("COMUN", result.taxTerritory());
        assertEquals(0, result.descendantsCount());
    }

    @Test
    void returnsMappedContext_whenRecordFound() {
        when(employeeLookupAdapter.findEmployeeId(any(),any(),any())).thenReturn(Optional.of(1L));

        var entity = new EmployeeTaxInformationEntity();
        entity.setId(3L); entity.setEmployeeId(1L);
        entity.setValidFrom(LocalDate.of(2025,1,1));
        entity.setFamilySituation(FamilySituation.MARRIED_DEPENDENT_SPOUSE);
        entity.setDescendantsCount(2); entity.setAscendantsCount(0);
        entity.setDisabilityDegree(DisabilityDegree.NONE);
        entity.setPensionCompensatoria(false); entity.setGeographicMobility(true);
        entity.setHabitualResidenceLoan(false);
        entity.setTaxTerritory(TaxTerritory.BIZKAIA);
        entity.setCreatedAt(LocalDateTime.now()); entity.setUpdatedAt(LocalDateTime.now());

        when(springDataRepo.findFirstByEmployeeIdAndValidFromLessThanEqualOrderByValidFromDesc(any(), any()))
            .thenReturn(Optional.of(entity));

        var result = adapter.findLatestOnOrBefore("ESP", "INTERNAL", "EMP001", LocalDate.of(2025,1,15));

        assertEquals("MARRIED_DEPENDENT_SPOUSE", result.familySituation());
        assertEquals("BIZKAIA", result.taxTerritory());
        assertTrue(result.geographicMobility());
    }
}
```

- [ ] **Run test — verify it fails**

```
mvn test -Dtest=EmployeeTaxInfoPayrollLookupAdapterTest
```

- [ ] **Implement port, context, adapter**

`EmployeeTaxInfoContext.java`:
```java
package com.b4rrhh.payroll.application.port;

public record EmployeeTaxInfoContext(
    String familySituation,
    int descendantsCount,
    int ascendantsCount,
    String disabilityDegree,
    boolean pensionCompensatoria,
    boolean geographicMobility,
    boolean habitualResidenceLoan,
    String taxTerritory
) {
    public static EmployeeTaxInfoContext ofDefault() {
        return new EmployeeTaxInfoContext("SINGLE_OR_OTHER", 0, 0, "NONE", false, false, false, "COMUN");
    }
}
```

`EmployeeTaxInfoPayrollLookupPort.java`:
```java
package com.b4rrhh.payroll.application.port;

import java.time.LocalDate;

public interface EmployeeTaxInfoPayrollLookupPort {
    EmployeeTaxInfoContext findLatestOnOrBefore(
        String ruleSystemCode, String employeeTypeCode, String employeeNumber, LocalDate referenceDate);
}
```

`EmployeeTaxInfoPayrollLookupAdapter.java`:
```java
package com.b4rrhh.employee.tax_information.infrastructure.persistence;

import com.b4rrhh.payroll.application.port.EmployeeTaxInfoContext;
import com.b4rrhh.payroll.application.port.EmployeeTaxInfoPayrollLookupPort;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class EmployeeTaxInfoPayrollLookupAdapter implements EmployeeTaxInfoPayrollLookupPort {

    private final SpringDataEmployeeTaxInformationRepository springDataRepo;
    private final EmployeeForTaxInfoLookupAdapter employeeLookupAdapter;

    public EmployeeTaxInfoPayrollLookupAdapter(
            SpringDataEmployeeTaxInformationRepository springDataRepo,
            EmployeeForTaxInfoLookupAdapter employeeLookupAdapter) {
        this.springDataRepo = springDataRepo;
        this.employeeLookupAdapter = employeeLookupAdapter;
    }

    @Override
    public EmployeeTaxInfoContext findLatestOnOrBefore(
            String ruleSystemCode, String employeeTypeCode, String employeeNumber, LocalDate referenceDate) {
        return employeeLookupAdapter.findEmployeeId(ruleSystemCode, employeeTypeCode, employeeNumber)
            .flatMap(employeeId -> springDataRepo
                .findFirstByEmployeeIdAndValidFromLessThanEqualOrderByValidFromDesc(employeeId, referenceDate))
            .map(e -> new EmployeeTaxInfoContext(
                e.getFamilySituation().name(),
                e.getDescendantsCount(),
                e.getAscendantsCount(),
                e.getDisabilityDegree().name(),
                e.isPensionCompensatoria(),
                e.isGeographicMobility(),
                e.isHabitualResidenceLoan(),
                e.getTaxTerritory().name()))
            .orElse(EmployeeTaxInfoContext.ofDefault());
    }
}
```

- [ ] **Wire into CalculatePayrollUnitService**

In `CalculatePayrollUnitService.java`:

1. Add field after `employeePayrollInputLookupPort`:
```java
private final EmployeeTaxInfoPayrollLookupPort employeeTaxInfoLookupPort;
```

2. Add import:
```java
import com.b4rrhh.payroll.application.port.EmployeeTaxInfoPayrollLookupPort;
import com.b4rrhh.payroll.application.port.EmployeeTaxInfoContext;
```

3. Add to constructor parameters (last param):
```java
EmployeeTaxInfoPayrollLookupPort employeeTaxInfoLookupPort
```

4. Add assignment in constructor body:
```java
this.employeeTaxInfoLookupPort = employeeTaxInfoLookupPort;
```

5. Add `buildTaxInfoSnapshot` method near the other `buildXxxSnapshot` methods:
```java
private PayrollContextSnapshot buildTaxInfoSnapshot(
        CalculatePayrollUnitCommand command,
        PayrollLaunchEligibleInputContext input) {
    LocalDate referenceDate = input.presenceStartDate() != null
        ? input.presenceStartDate()
        : command.periodStart();

    EmployeeTaxInfoContext ctx = employeeTaxInfoLookupPort.findLatestOnOrBefore(
        command.ruleSystemCode(), command.employeeTypeCode(), command.employeeNumber(), referenceDate);

    java.util.Map<String, Object> sourceKey = new java.util.LinkedHashMap<>();
    sourceKey.put("ruleSystemCode", command.ruleSystemCode());
    sourceKey.put("employeeTypeCode", command.employeeTypeCode());
    sourceKey.put("employeeNumber", command.employeeNumber());
    sourceKey.put("referenceDate", referenceDate.toString());

    java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
    payload.put("familySituation", ctx.familySituation());
    payload.put("descendantsCount", ctx.descendantsCount());
    payload.put("ascendantsCount", ctx.ascendantsCount());
    payload.put("disabilityDegree", ctx.disabilityDegree());
    payload.put("pensionCompensatoria", ctx.pensionCompensatoria());
    payload.put("geographicMobility", ctx.geographicMobility());
    payload.put("habitualResidenceLoan", ctx.habitualResidenceLoan());
    payload.put("taxTerritory", ctx.taxTerritory());

    return new PayrollContextSnapshot("EMPLOYEE_TAX_INFORMATION", "EMPLOYEE",
        toJson(sourceKey), toJson(payload));
}
```

6. In `buildSnapshots()`, add after `eligibleRealSnapshot`:
```java
snapshots.add(buildTaxInfoSnapshot(command, input));
```

> **Note on `command.periodStart()`:** Check `CalculatePayrollUnitCommand` for the exact method name that returns the period start date. It may be `periodStart()`, `getPeriodStart()`, or derived from `payrollPeriodCode`. Use whatever is available to get a sensible reference date fallback.

- [ ] **Run tests — verify they pass**

```
mvn test -Dtest=EmployeeTaxInfoPayrollLookupAdapterTest
mvn test -Dtest=LaunchPayrollCalculationEligibleRealEndToEndIntegrationTest
```
The E2E test must still pass (it checks for `EMPLOYEE_PAYROLL_CONTEXT` snapshot — our new `EMPLOYEE_TAX_INFORMATION` snapshot is additive).

- [ ] **Commit**

```
git add src/main/java/com/b4rrhh/payroll/application/port/EmployeeTaxInfoContext.java
git add src/main/java/com/b4rrhh/payroll/application/port/EmployeeTaxInfoPayrollLookupPort.java
git add src/main/java/com/b4rrhh/employee/tax_information/infrastructure/persistence/EmployeeTaxInfoPayrollLookupAdapter.java
git add src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java
git add src/test/java/com/b4rrhh/employee/tax_information/infrastructure/persistence/EmployeeTaxInfoPayrollLookupAdapterTest.java
git commit -m "feat(tax-information): payroll snapshot integration — EMPLOYEE_TAX_INFORMATION context captured at calculation time"
```

---

## Task 8 — Frontend

**Files (in `b4rrhh_frontend`):**
- Create: `src/app/core/api/clients/employee-tax-information.client.ts`
- Create: `src/app/core/api/mappers/employee-tax-information.mapper.ts`
- Create: `src/app/features/employee/tax-information/models/employee-tax-information.model.ts`
- Create: `src/app/features/employee/tax-information/data-access/employee-tax-information.gateway.ts`
- Create: `src/app/features/employee/tax-information/data-access/employee-tax-information.store.ts`
- Create: `src/app/features/employee/tax-information/components/employee-tax-information-section.component.ts`
- Create: `src/app/features/employee/tax-information/components/employee-tax-information-section.component.html`

- [ ] **Implement frontend files**

`employee-tax-information.model.ts`:
```typescript
export type FamilySituation = 'SINGLE_OR_OTHER' | 'MARRIED_DEPENDENT_SPOUSE' | 'SEPARATED_WITH_CHILDREN';
export type DisabilityDegree = 'NONE' | 'MODERATE' | 'SEVERE';
export type TaxTerritory = 'COMUN' | 'ARABA' | 'GIPUZKOA' | 'BIZKAIA' | 'NAVARRA';

export interface EmployeeTaxInformation {
  validFrom: string;
  familySituation: FamilySituation;
  descendantsCount: number;
  ascendantsCount: number;
  disabilityDegree: DisabilityDegree;
  pensionCompensatoria: boolean;
  geographicMobility: boolean;
  habitualResidenceLoan: boolean;
  taxTerritory: TaxTerritory;
}

export interface CreateEmployeeTaxInformationRequest extends EmployeeTaxInformation {}
export interface CorrectEmployeeTaxInformationRequest extends Omit<EmployeeTaxInformation, 'validFrom'> {}
```

`employee-tax-information.client.ts`:
```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EmployeeTaxInformation, CreateEmployeeTaxInformationRequest, CorrectEmployeeTaxInformationRequest } from '../../features/employee/tax-information/models/employee-tax-information.model';

@Injectable({ providedIn: 'root' })
export class EmployeeTaxInformationClient {
  private readonly http = inject(HttpClient);

  private basePath(rs: string, type: string, num: string): string {
    return `/api/employees/${rs}/${type}/${num}/tax-information`;
  }

  list(rs: string, type: string, num: string): Observable<EmployeeTaxInformation[]> {
    return this.http.get<EmployeeTaxInformation[]>(this.basePath(rs, type, num));
  }

  get(rs: string, type: string, num: string, validFrom: string): Observable<EmployeeTaxInformation> {
    return this.http.get<EmployeeTaxInformation>(`${this.basePath(rs, type, num)}/${validFrom}`);
  }

  create(rs: string, type: string, num: string, body: CreateEmployeeTaxInformationRequest): Observable<EmployeeTaxInformation> {
    return this.http.post<EmployeeTaxInformation>(this.basePath(rs, type, num), body);
  }

  correct(rs: string, type: string, num: string, validFrom: string, body: CorrectEmployeeTaxInformationRequest): Observable<EmployeeTaxInformation> {
    return this.http.put<EmployeeTaxInformation>(`${this.basePath(rs, type, num)}/${validFrom}`, body);
  }

  delete(rs: string, type: string, num: string, validFrom: string): Observable<void> {
    return this.http.delete<void>(`${this.basePath(rs, type, num)}/${validFrom}`);
  }
}
```

`employee-tax-information.mapper.ts`:
```typescript
import { EmployeeTaxInformation } from '../../features/employee/tax-information/models/employee-tax-information.model';

export function mapTaxInformationFromApi(raw: EmployeeTaxInformation): EmployeeTaxInformation {
  return { ...raw };
}
```

`employee-tax-information.gateway.ts`:
```typescript
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { EmployeeTaxInformationClient } from '../../../core/api/clients/employee-tax-information.client';
import { mapTaxInformationFromApi } from '../../../core/api/mappers/employee-tax-information.mapper';
import { EmployeeTaxInformation, CreateEmployeeTaxInformationRequest, CorrectEmployeeTaxInformationRequest } from '../models/employee-tax-information.model';

@Injectable({ providedIn: 'root' })
export class EmployeeTaxInformationGateway {
  private readonly client = inject(EmployeeTaxInformationClient);

  list(rs: string, type: string, num: string): Observable<EmployeeTaxInformation[]> {
    return this.client.list(rs, type, num).pipe(map(items => items.map(mapTaxInformationFromApi)));
  }

  create(rs: string, type: string, num: string, req: CreateEmployeeTaxInformationRequest): Observable<EmployeeTaxInformation> {
    return this.client.create(rs, type, num, req).pipe(map(mapTaxInformationFromApi));
  }

  correct(rs: string, type: string, num: string, validFrom: string, req: CorrectEmployeeTaxInformationRequest): Observable<EmployeeTaxInformation> {
    return this.client.correct(rs, type, num, validFrom, req).pipe(map(mapTaxInformationFromApi));
  }

  delete(rs: string, type: string, num: string, validFrom: string): Observable<void> {
    return this.client.delete(rs, type, num, validFrom);
  }
}
```

`employee-tax-information.store.ts`:
```typescript
import { Injectable, signal, computed, inject } from '@angular/core';
import { EmployeeTaxInformationGateway } from './employee-tax-information.gateway';
import { EmployeeTaxInformation, CreateEmployeeTaxInformationRequest, CorrectEmployeeTaxInformationRequest } from '../models/employee-tax-information.model';

@Injectable({ providedIn: 'root' })
export class EmployeeTaxInformationStore {
  private readonly gateway = inject(EmployeeTaxInformationGateway);

  private readonly _records = signal<EmployeeTaxInformation[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly records = this._records.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly latest = computed(() => this._records()[0] ?? null);

  load(rs: string, type: string, num: string): void {
    this._loading.set(true);
    this._error.set(null);
    this.gateway.list(rs, type, num).subscribe({
      next: records => { this._records.set(records); this._loading.set(false); },
      error: err => { this._error.set(err.message); this._loading.set(false); }
    });
  }

  create(rs: string, type: string, num: string, req: CreateEmployeeTaxInformationRequest): void {
    this.gateway.create(rs, type, num, req).subscribe({
      next: () => this.load(rs, type, num),
      error: err => this._error.set(err.message)
    });
  }

  correct(rs: string, type: string, num: string, validFrom: string, req: CorrectEmployeeTaxInformationRequest): void {
    this.gateway.correct(rs, type, num, validFrom, req).subscribe({
      next: () => this.load(rs, type, num),
      error: err => this._error.set(err.message)
    });
  }

  delete(rs: string, type: string, num: string, validFrom: string): void {
    this.gateway.delete(rs, type, num, validFrom).subscribe({
      next: () => this.load(rs, type, num),
      error: err => this._error.set(err.message)
    });
  }
}
```

`employee-tax-information-section.component.ts`:
```typescript
import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EmployeeTaxInformationStore } from '../data-access/employee-tax-information.store';

@Component({
  selector: 'app-employee-tax-information-section',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './employee-tax-information-section.component.html'
})
export class EmployeeTaxInformationSectionComponent implements OnInit {
  @Input({ required: true }) ruleSystemCode!: string;
  @Input({ required: true }) employeeTypeCode!: string;
  @Input({ required: true }) employeeNumber!: string;

  readonly store = inject(EmployeeTaxInformationStore);

  ngOnInit(): void {
    this.store.load(this.ruleSystemCode, this.employeeTypeCode, this.employeeNumber);
  }
}
```

`employee-tax-information-section.component.html`:
```html
<div class="tax-information-section">
  <h3>Información Fiscal (Modelo 145)</h3>

  @if (store.loading()) {
    <p>Cargando...</p>
  } @else if (store.error()) {
    <p class="error">{{ store.error() }}</p>
  } @else if (store.latest()) {
    <table>
      <tr><th>Vigente desde</th><td>{{ store.latest()!.validFrom }}</td></tr>
      <tr><th>Situación familiar</th><td>{{ store.latest()!.familySituation }}</td></tr>
      <tr><th>Descend. con derecho</th><td>{{ store.latest()!.descendantsCount }}</td></tr>
      <tr><th>Ascend. a cargo</th><td>{{ store.latest()!.ascendantsCount }}</td></tr>
      <tr><th>Grado discapacidad</th><td>{{ store.latest()!.disabilityDegree }}</td></tr>
      <tr><th>Pensión compensatoria</th><td>{{ store.latest()!.pensionCompensatoria ? 'Sí' : 'No' }}</td></tr>
      <tr><th>Movilidad geográfica</th><td>{{ store.latest()!.geographicMobility ? 'Sí' : 'No' }}</td></tr>
      <tr><th>Préstamo vivienda</th><td>{{ store.latest()!.habitualResidenceLoan ? 'Sí' : 'No' }}</td></tr>
      <tr><th>Territorio fiscal</th><td>{{ store.latest()!.taxTerritory }}</td></tr>
    </table>
  } @else {
    <p>Sin información fiscal registrada. Se aplican valores por defecto (soltero, sin cargas, territorio común).</p>
  }
</div>
```

- [ ] **Build frontend to verify no compilation errors**

```
cd b4rrhh_frontend && npm run build
```

- [ ] **Commit**

```
cd b4rrhh_frontend
git add src/app/core/api/clients/employee-tax-information.client.ts
git add src/app/core/api/mappers/employee-tax-information.mapper.ts
git add src/app/features/employee/tax-information/
git commit -m "feat(tax-information): Angular vertical — model, client, gateway, store, section component"
```

---

## Final check

- [ ] **Run full backend test suite**

```
cd b4rrhh_backend && mvn test
```
Expected: all new tests green; pre-existing `ContractTest` and `HireEmployeeServiceRollbackIntegrationTest` failures are unchanged (pre-existing, not introduced by this feature).
