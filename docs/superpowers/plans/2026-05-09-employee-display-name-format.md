# Employee Display Name Format Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow each rule system (company umbrella) to configure a display name format so all employees appear with the same normative name style (e.g. "BIFORCOS AMOR, JUAN ANTONIO") instead of the current free-text `preferredName`.

**Architecture:** New `rulesystem.employeedisplaynameformat` vertical stores one format code per rule system. A new `DisplayNameComputationService` in the employee application layer uses a secondary port to look up the format and applies `DisplayNameFormatter` (pure domain logic). Both `EmployeeBusinessKeyController` and `EmployeeDirectoryPersistenceAdapter` use this service to populate `displayName` in their responses. The frontend consumes the pre-computed `displayName` and adds a format config card to the company settings page.

**Tech Stack:** Java 21, Spring Boot, JPA/Hibernate, H2 (tests), PostgreSQL (runtime), Flyway, Angular 21 signals, PrimeNG 19

---

## Codebase Context

### Backend root
`c:\Users\bifor\Documents\Proyectos\B4RRHH\b4rrhh_backend`

### Frontend root
`c:\Users\bifor\Documents\Proyectos\B4RRHH\b4rrhh_frontend`

### Key existing files (read these before touching related code)
- `src/main/java/com/b4rrhh/employee/employee/domain/model/Employee.java` — domain model with `firstName`, `lastName1`, `lastName2`, `preferredName`
- `src/main/java/com/b4rrhh/employee/employee/infrastructure/web/EmployeeBusinessKeyController.java` — GET/PUT employee by business key, `toResponse()` method
- `src/main/java/com/b4rrhh/employee/employee/infrastructure/persistence/EmployeeDirectoryPersistenceAdapter.java` — `buildDisplayName()` method to replace
- `src/main/java/com/b4rrhh/employee/employee/application/usecase/UpdateEmployeeService.java` — pattern for cross-context injection (imports `RuleSystemRepository`)
- `src/main/java/com/b4rrhh/rulesystem/companyprofile/` — pattern for a rulesystem vertical (domain model + port + use cases + persistence + web)
- `openapi/personnel-administration-api.yaml` — `EmployeeResponse` schema at line 6321, `EmployeeDirectoryItemResponse` at line 6355

### Architecture rules (from CLAUDE.md)
- Domain classes MUST NOT import Spring or JPA
- Business logic MUST NOT live in controllers or repositories
- `@Entity` classes stay in `infrastructure.persistence`
- Never edit existing Flyway migrations — always add a new file
- Last migration is `V96__fix_tax_information_count_column_types.sql`

---

## File Structure

### New backend files
```
src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/
  domain/model/
    DisplayNameFormatCode.java            — enum: FULL_TITLE_CASE, FULL_UPPER, SURNAME_FIRST_UPPER, SHORT_TITLE, SHORT_UPPER, SURNAME_ABBREV_UPPER
    DisplayNameFormatter.java             — pure static utility: format(firstName, lastName1, lastName2, formatCode) → String
    EmployeeDisplayNameFormat.java        — domain record: ruleSystemCode + formatCode
  domain/port/
    EmployeeDisplayNameFormatRepository.java  — findByRuleSystemCode + save
  application/usecase/
    GetEmployeeDisplayNameFormatUseCase.java
    GetEmployeeDisplayNameFormatService.java
    UpsertEmployeeDisplayNameFormatUseCase.java
    UpsertEmployeeDisplayNameFormatCommand.java
    UpsertEmployeeDisplayNameFormatService.java
  infrastructure/persistence/
    EmployeeDisplayNameFormatEntity.java
    EmployeeDisplayNameFormatPersistenceAdapter.java
    SpringDataEmployeeDisplayNameFormatRepository.java
  infrastructure/web/
    EmployeeDisplayNameFormatController.java
    dto/EmployeeDisplayNameFormatResponse.java
    dto/UpsertEmployeeDisplayNameFormatRequest.java

src/main/java/com/b4rrhh/employee/employee/application/
  port/DisplayNameFormatLookupPort.java   — secondary port: findFormatCodeForRuleSystem(ruleSystemCode) → Optional<DisplayNameFormatCode>
  DisplayNameComputationService.java      — compute(ruleSystemCode, firstName, lastName1, lastName2, preferredName) → String

src/main/java/com/b4rrhh/employee/employee/infrastructure/adapters/
  DisplayNameFormatLookupAdapter.java     — implements DisplayNameFormatLookupPort via EmployeeDisplayNameFormatRepository

src/main/resources/db/migration/
  V97__create_employee_display_name_format.sql
```

### Modified backend files
```
openapi/personnel-administration-api.yaml       — add displayName to EmployeeResponse + new paths/schemas
src/.../employee/infrastructure/web/dto/EmployeeResponse.java          — add displayName field
src/.../employee/infrastructure/web/EmployeeBusinessKeyController.java  — inject + use DisplayNameComputationService
src/.../employee/infrastructure/persistence/EmployeeDirectoryPersistenceAdapter.java — replace buildDisplayName() with computation service
```

### New frontend files
```
src/app/core/api/clients/employee-display-name-format.client.ts
src/app/features/company/ui/display-name-format-card.component.ts
src/app/features/company/ui/display-name-format-card.component.html
src/app/features/company/ui/display-name-format-card.component.scss
```

### Modified frontend files
```
src/app/core/api/clients/employee-read.client.ts          — add displayName to EmployeeReadApiModel
src/app/core/api/mappers/employee-detail.mapper.ts        — use source.displayName directly
src/app/features/employee/shell/components/employee-detail-header.component.html  — relabel preferredName field
src/app/features/company/ui/company-page.component.html   — add DisplayNameFormatCardComponent
src/app/features/company/ui/company-page.component.ts     — import new component
```

---

## Task 1: DisplayNameFormatCode enum + DisplayNameFormatter

**Files:**
- Create: `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/domain/model/DisplayNameFormatCode.java`
- Create: `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/domain/model/DisplayNameFormatter.java`
- Test: `src/test/java/com/b4rrhh/rulesystem/employeedisplaynameformat/domain/model/DisplayNameFormatterTest.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/b4rrhh/rulesystem/employeedisplaynameformat/domain/model/DisplayNameFormatterTest.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DisplayNameFormatterTest {

    @Test
    void fullTitleCase_titleCasesAllWords() {
        String result = DisplayNameFormatter.format("juan antonio", "biforcos", "amor", DisplayNameFormatCode.FULL_TITLE_CASE);
        assertThat(result).isEqualTo("Juan Antonio Biforcos Amor");
    }

    @Test
    void fullUpper_uppercasesEverything() {
        String result = DisplayNameFormatter.format("Juan Antonio", "Biforcos", "Amor", DisplayNameFormatCode.FULL_UPPER);
        assertThat(result).isEqualTo("JUAN ANTONIO BIFORCOS AMOR");
    }

    @Test
    void surnameFirstUpper_surnamesCommaFirstName() {
        String result = DisplayNameFormatter.format("Juan Antonio", "Biforcos", "Amor", DisplayNameFormatCode.SURNAME_FIRST_UPPER);
        assertThat(result).isEqualTo("BIFORCOS AMOR, JUAN ANTONIO");
    }

    @Test
    void shortTitle_firstNamePlusFirstSurnameOnly() {
        String result = DisplayNameFormatter.format("Juan Antonio", "Biforcos", "Amor", DisplayNameFormatCode.SHORT_TITLE);
        assertThat(result).isEqualTo("Juan Antonio Biforcos");
    }

    @Test
    void shortUpper_firstNamePlusFirstSurnameUppercase() {
        String result = DisplayNameFormatter.format("Juan Antonio", "Biforcos", "Amor", DisplayNameFormatCode.SHORT_UPPER);
        assertThat(result).isEqualTo("JUAN ANTONIO BIFORCOS");
    }

    @Test
    void surnameAbbrevUpper_surnamesPlusInitials() {
        String result = DisplayNameFormatter.format("Juan Antonio", "Biforcos", "Amor", DisplayNameFormatCode.SURNAME_ABBREV_UPPER);
        assertThat(result).isEqualTo("BIFORCOS AMOR, J.A.");
    }

    @Test
    void nullLastName2_omitsIt() {
        String result = DisplayNameFormatter.format("Juan", "Garcia", null, DisplayNameFormatCode.FULL_TITLE_CASE);
        assertThat(result).isEqualTo("Juan Garcia");
    }

    @Test
    void surnameFirstUpper_nullLastName2_noTrailingSpace() {
        String result = DisplayNameFormatter.format("Juan", "Garcia", null, DisplayNameFormatCode.SURNAME_FIRST_UPPER);
        assertThat(result).isEqualTo("GARCIA, JUAN");
    }

    @Test
    void singleWordFirstName_surnameAbbrevUpper_singleInitial() {
        String result = DisplayNameFormatter.format("Juan", "Biforcos", "Amor", DisplayNameFormatCode.SURNAME_ABBREV_UPPER);
        assertThat(result).isEqualTo("BIFORCOS AMOR, J.");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run from `b4rrhh_backend`:
```bash
mvn test -Dtest=DisplayNameFormatterTest -q 2>&1 | tail -5
```
Expected: compilation error — `DisplayNameFormatCode` and `DisplayNameFormatter` not found.

- [ ] **Step 3: Create DisplayNameFormatCode enum**

Create `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/domain/model/DisplayNameFormatCode.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model;

public enum DisplayNameFormatCode {
    /** Juan Antonio Biforcos Amor */
    FULL_TITLE_CASE,
    /** JUAN ANTONIO BIFORCOS AMOR */
    FULL_UPPER,
    /** BIFORCOS AMOR, JUAN ANTONIO */
    SURNAME_FIRST_UPPER,
    /** Juan Antonio Biforcos  (firstName + lastName1 only, title case) */
    SHORT_TITLE,
    /** JUAN ANTONIO BIFORCOS  (firstName + lastName1 only, uppercase) */
    SHORT_UPPER,
    /** BIFORCOS AMOR, J.A.  (surnames + initials of firstName) */
    SURNAME_ABBREV_UPPER
}
```

- [ ] **Step 4: Create DisplayNameFormatter**

Create `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/domain/model/DisplayNameFormatter.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DisplayNameFormatter {

    private DisplayNameFormatter() {}

    public static String format(
            String firstName, String lastName1, String lastName2,
            DisplayNameFormatCode formatCode) {

        String fn = blank(firstName) ? "" : firstName.trim();
        String ln1 = blank(lastName1) ? "" : lastName1.trim();
        String ln2 = blank(lastName2) ? "" : lastName2.trim();

        return switch (formatCode) {
            case FULL_TITLE_CASE -> joinNonEmpty(" ", toTitleCase(fn), toTitleCase(ln1), toTitleCase(ln2));
            case FULL_UPPER -> joinNonEmpty(" ", fn, ln1, ln2).toUpperCase();
            case SURNAME_FIRST_UPPER -> surnameFirst(fn, ln1, ln2, false);
            case SHORT_TITLE -> joinNonEmpty(" ", toTitleCase(fn), toTitleCase(ln1));
            case SHORT_UPPER -> joinNonEmpty(" ", fn, ln1).toUpperCase();
            case SURNAME_ABBREV_UPPER -> surnameFirst(fn, ln1, ln2, true);
        };
    }

    private static String surnameFirst(String fn, String ln1, String ln2, boolean abbreviateFirst) {
        String surnames = joinNonEmpty(" ", ln1, ln2).toUpperCase();
        String firstPart = abbreviateFirst ? abbreviate(fn) : fn.toUpperCase();
        if (firstPart.isEmpty()) return surnames;
        if (surnames.isEmpty()) return firstPart;
        return surnames + ", " + firstPart;
    }

    /** "Juan Antonio" → "J.A." */
    private static String abbreviate(String name) {
        if (blank(name)) return "";
        return Arrays.stream(name.trim().split("\\s+"))
                .filter(w -> !w.isEmpty())
                .map(w -> Character.toUpperCase(w.charAt(0)) + ".")
                .collect(Collectors.joining());
    }

    /** Title-cases every word: "juan antonio" → "Juan Antonio" */
    private static String toTitleCase(String input) {
        if (blank(input)) return "";
        return Arrays.stream(input.trim().split("\\s+"))
                .filter(w -> !w.isEmpty())
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private static String joinNonEmpty(String sep, String... parts) {
        return Stream.of(parts)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.joining(sep));
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
mvn test -Dtest=DisplayNameFormatterTest -q 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`, 9 tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/domain/model/ \
        src/test/java/com/b4rrhh/rulesystem/employeedisplaynameformat/domain/model/
git commit -m "feat(display-name-format): add DisplayNameFormatCode enum and DisplayNameFormatter"
```

---

## Task 2: EmployeeDisplayNameFormat domain model + port

**Files:**
- Create: `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/domain/model/EmployeeDisplayNameFormat.java`
- Create: `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/domain/port/EmployeeDisplayNameFormatRepository.java`

- [ ] **Step 1: Create the domain record**

Create `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/domain/model/EmployeeDisplayNameFormat.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model;

public record EmployeeDisplayNameFormat(
        String ruleSystemCode,
        DisplayNameFormatCode formatCode
) {}
```

- [ ] **Step 2: Create the repository port**

Create `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/domain/port/EmployeeDisplayNameFormatRepository.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.domain.port;

import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.EmployeeDisplayNameFormat;
import java.util.Optional;

public interface EmployeeDisplayNameFormatRepository {
    Optional<EmployeeDisplayNameFormat> findByRuleSystemCode(String ruleSystemCode);
    EmployeeDisplayNameFormat save(EmployeeDisplayNameFormat format);
}
```

- [ ] **Step 3: Verify compilation**

```bash
mvn compile -q 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/domain/
git commit -m "feat(display-name-format): add EmployeeDisplayNameFormat domain model and port"
```

---

## Task 3: DB migration + JPA entity + Spring Data repository + persistence adapter

**Files:**
- Create: `src/main/resources/db/migration/V97__create_employee_display_name_format.sql`
- Create: `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/infrastructure/persistence/EmployeeDisplayNameFormatEntity.java`
- Create: `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/infrastructure/persistence/SpringDataEmployeeDisplayNameFormatRepository.java`
- Create: `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/infrastructure/persistence/EmployeeDisplayNameFormatPersistenceAdapter.java`

- [ ] **Step 1: Create the Flyway migration**

Create `src/main/resources/db/migration/V97__create_employee_display_name_format.sql`:

```sql
CREATE TABLE rulesystem.employee_display_name_format (
    id                       BIGSERIAL    PRIMARY KEY,
    rule_system_code         VARCHAR(5)   NOT NULL UNIQUE,
    display_name_format_code VARCHAR(50)  NOT NULL,
    created_at               TIMESTAMP    NOT NULL,
    updated_at               TIMESTAMP    NOT NULL
);
```

- [ ] **Step 2: Create the JPA entity**

Create `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/infrastructure/persistence/EmployeeDisplayNameFormatEntity.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "employee_display_name_format",
        schema = "rulesystem",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_employee_display_name_format_rule_system",
                columnNames = "rule_system_code"
        )
)
public class EmployeeDisplayNameFormatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_system_code", nullable = false, length = 5)
    private String ruleSystemCode;

    @Column(name = "display_name_format_code", nullable = false, length = 50)
    private String displayNameFormatCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleSystemCode() { return ruleSystemCode; }
    public void setRuleSystemCode(String ruleSystemCode) { this.ruleSystemCode = ruleSystemCode; }
    public String getDisplayNameFormatCode() { return displayNameFormatCode; }
    public void setDisplayNameFormatCode(String displayNameFormatCode) { this.displayNameFormatCode = displayNameFormatCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 3: Create the Spring Data repository**

Create `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/infrastructure/persistence/SpringDataEmployeeDisplayNameFormatRepository.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SpringDataEmployeeDisplayNameFormatRepository
        extends JpaRepository<EmployeeDisplayNameFormatEntity, Long> {

    Optional<EmployeeDisplayNameFormatEntity> findByRuleSystemCode(String ruleSystemCode);
}
```

- [ ] **Step 4: Create the persistence adapter**

Create `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/infrastructure/persistence/EmployeeDisplayNameFormatPersistenceAdapter.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.infrastructure.persistence;

import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.DisplayNameFormatCode;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.EmployeeDisplayNameFormat;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.port.EmployeeDisplayNameFormatRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class EmployeeDisplayNameFormatPersistenceAdapter
        implements EmployeeDisplayNameFormatRepository {

    private final SpringDataEmployeeDisplayNameFormatRepository springDataRepo;

    public EmployeeDisplayNameFormatPersistenceAdapter(
            SpringDataEmployeeDisplayNameFormatRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public Optional<EmployeeDisplayNameFormat> findByRuleSystemCode(String ruleSystemCode) {
        return springDataRepo.findByRuleSystemCode(ruleSystemCode)
                .map(this::toDomain);
    }

    @Override
    public EmployeeDisplayNameFormat save(EmployeeDisplayNameFormat format) {
        EmployeeDisplayNameFormatEntity entity = springDataRepo
                .findByRuleSystemCode(format.ruleSystemCode())
                .orElseGet(EmployeeDisplayNameFormatEntity::new);

        entity.setRuleSystemCode(format.ruleSystemCode());
        entity.setDisplayNameFormatCode(format.formatCode().name());

        return toDomain(springDataRepo.save(entity));
    }

    private EmployeeDisplayNameFormat toDomain(EmployeeDisplayNameFormatEntity entity) {
        return new EmployeeDisplayNameFormat(
                entity.getRuleSystemCode(),
                DisplayNameFormatCode.valueOf(entity.getDisplayNameFormatCode())
        );
    }
}
```

- [ ] **Step 5: Verify compilation + migration runs against H2**

```bash
mvn compile -q 2>&1 | tail -5
mvn test -Dtest=FlywayMigrationIntegrationTest -q 2>&1 | tail -10
```
Expected: both `BUILD SUCCESS`. The migration test confirms V97 runs without errors.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/db/migration/V97__create_employee_display_name_format.sql \
        src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/infrastructure/persistence/
git commit -m "feat(display-name-format): add DB migration and persistence layer"
```

---

## Task 4: Get + Upsert use cases

**Files:**
- Create: `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/usecase/GetEmployeeDisplayNameFormatUseCase.java`
- Create: `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/usecase/GetEmployeeDisplayNameFormatService.java`
- Create: `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/usecase/UpsertEmployeeDisplayNameFormatUseCase.java`
- Create: `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/usecase/UpsertEmployeeDisplayNameFormatCommand.java`
- Create: `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/usecase/UpsertEmployeeDisplayNameFormatService.java`
- Test: `src/test/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/usecase/GetEmployeeDisplayNameFormatServiceTest.java`
- Test: `src/test/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/usecase/UpsertEmployeeDisplayNameFormatServiceTest.java`

- [ ] **Step 1: Write failing tests**

Create `src/test/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/usecase/GetEmployeeDisplayNameFormatServiceTest.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.application.usecase;

import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.DisplayNameFormatCode;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.EmployeeDisplayNameFormat;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.port.EmployeeDisplayNameFormatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetEmployeeDisplayNameFormatServiceTest {

    @Mock EmployeeDisplayNameFormatRepository repository;
    @InjectMocks GetEmployeeDisplayNameFormatService service;

    @Test
    void returnsConfig_whenExists() {
        var format = new EmployeeDisplayNameFormat("RSTEST", DisplayNameFormatCode.FULL_UPPER);
        when(repository.findByRuleSystemCode("RSTEST")).thenReturn(Optional.of(format));

        var result = service.getByRuleSystemCode("RSTEST");

        assertThat(result).isPresent().hasValue(format);
    }

    @Test
    void returnsEmpty_whenNotConfigured() {
        when(repository.findByRuleSystemCode("RSTEST")).thenReturn(Optional.empty());

        var result = service.getByRuleSystemCode("RSTEST");

        assertThat(result).isEmpty();
    }

    @Test
    void normalizesRuleSystemCodeToUpperCase() {
        when(repository.findByRuleSystemCode("RSTEST")).thenReturn(Optional.empty());

        service.getByRuleSystemCode("rstest");

        verify(repository).findByRuleSystemCode("RSTEST");
    }
}
```

Create `src/test/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/usecase/UpsertEmployeeDisplayNameFormatServiceTest.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.application.usecase;

import com.b4rrhh.rulesystem.domain.model.RuleSystem;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.DisplayNameFormatCode;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.EmployeeDisplayNameFormat;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.port.EmployeeDisplayNameFormatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpsertEmployeeDisplayNameFormatServiceTest {

    @Mock EmployeeDisplayNameFormatRepository formatRepository;
    @Mock RuleSystemRepository ruleSystemRepository;
    @InjectMocks UpsertEmployeeDisplayNameFormatService service;

    @Test
    void savesFormat_whenRuleSystemExists() {
        when(ruleSystemRepository.findByCode("RSTEST")).thenReturn(Optional.of(mockRuleSystem()));
        var saved = new EmployeeDisplayNameFormat("RSTEST", DisplayNameFormatCode.FULL_UPPER);
        when(formatRepository.save(any())).thenReturn(saved);

        var result = service.upsert(new UpsertEmployeeDisplayNameFormatCommand("RSTEST", "FULL_UPPER"));

        assertThat(result.ruleSystemCode()).isEqualTo("RSTEST");
        assertThat(result.formatCode()).isEqualTo(DisplayNameFormatCode.FULL_UPPER);
    }

    @Test
    void throwsIllegalArgument_whenRuleSystemNotFound() {
        when(ruleSystemRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.upsert(new UpsertEmployeeDisplayNameFormatCommand("UNKNOWN", "FULL_UPPER")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void throwsIllegalArgument_whenFormatCodeInvalid() {
        when(ruleSystemRepository.findByCode("RSTEST")).thenReturn(Optional.of(mockRuleSystem()));

        assertThatThrownBy(() ->
                service.upsert(new UpsertEmployeeDisplayNameFormatCommand("RSTEST", "BOGUS_FORMAT")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BOGUS_FORMAT");
    }

    @Test
    void normalizesRuleSystemCodeToUpperCase() {
        when(ruleSystemRepository.findByCode("RSTEST")).thenReturn(Optional.of(mockRuleSystem()));
        when(formatRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.upsert(new UpsertEmployeeDisplayNameFormatCommand("rstest", "FULL_UPPER"));

        ArgumentCaptor<EmployeeDisplayNameFormat> captor = ArgumentCaptor.forClass(EmployeeDisplayNameFormat.class);
        verify(formatRepository).save(captor.capture());
        assertThat(captor.getValue().ruleSystemCode()).isEqualTo("RSTEST");
    }

    private RuleSystem mockRuleSystem() {
        // RuleSystem is a domain class — check its constructor. If it takes a code, use:
        return new RuleSystem("RSTEST");
    }
}
```

**Note:** If `RuleSystem` constructor differs from `new RuleSystem("RSTEST")`, check `src/main/java/com/b4rrhh/rulesystem/domain/model/RuleSystem.java` and adjust accordingly.

- [ ] **Step 2: Run tests to verify they fail**

```bash
mvn test -Dtest="GetEmployeeDisplayNameFormatServiceTest,UpsertEmployeeDisplayNameFormatServiceTest" -q 2>&1 | tail -5
```
Expected: compilation errors — classes not found.

- [ ] **Step 3: Create use case interfaces and command**

Create `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/usecase/GetEmployeeDisplayNameFormatUseCase.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.application.usecase;

import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.EmployeeDisplayNameFormat;
import java.util.Optional;

public interface GetEmployeeDisplayNameFormatUseCase {
    Optional<EmployeeDisplayNameFormat> getByRuleSystemCode(String ruleSystemCode);
}
```

Create `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/usecase/UpsertEmployeeDisplayNameFormatUseCase.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.application.usecase;

import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.EmployeeDisplayNameFormat;

public interface UpsertEmployeeDisplayNameFormatUseCase {
    EmployeeDisplayNameFormat upsert(UpsertEmployeeDisplayNameFormatCommand command);
}
```

Create `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/usecase/UpsertEmployeeDisplayNameFormatCommand.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.application.usecase;

public record UpsertEmployeeDisplayNameFormatCommand(
        String ruleSystemCode,
        String formatCode
) {}
```

- [ ] **Step 4: Create GetEmployeeDisplayNameFormatService**

Create `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/usecase/GetEmployeeDisplayNameFormatService.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.application.usecase;

import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.EmployeeDisplayNameFormat;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.port.EmployeeDisplayNameFormatRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class GetEmployeeDisplayNameFormatService implements GetEmployeeDisplayNameFormatUseCase {

    private final EmployeeDisplayNameFormatRepository repository;

    public GetEmployeeDisplayNameFormatService(EmployeeDisplayNameFormatRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<EmployeeDisplayNameFormat> getByRuleSystemCode(String ruleSystemCode) {
        return repository.findByRuleSystemCode(ruleSystemCode.trim().toUpperCase());
    }
}
```

- [ ] **Step 5: Create UpsertEmployeeDisplayNameFormatService**

Create `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/usecase/UpsertEmployeeDisplayNameFormatService.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.application.usecase;

import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.DisplayNameFormatCode;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.EmployeeDisplayNameFormat;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.port.EmployeeDisplayNameFormatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpsertEmployeeDisplayNameFormatService implements UpsertEmployeeDisplayNameFormatUseCase {

    private final EmployeeDisplayNameFormatRepository formatRepository;
    private final RuleSystemRepository ruleSystemRepository;

    public UpsertEmployeeDisplayNameFormatService(
            EmployeeDisplayNameFormatRepository formatRepository,
            RuleSystemRepository ruleSystemRepository) {
        this.formatRepository = formatRepository;
        this.ruleSystemRepository = ruleSystemRepository;
    }

    @Override
    @Transactional
    public EmployeeDisplayNameFormat upsert(UpsertEmployeeDisplayNameFormatCommand command) {
        String normalizedCode = command.ruleSystemCode().trim().toUpperCase();

        ruleSystemRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rule system not found: " + normalizedCode));

        DisplayNameFormatCode formatCode;
        try {
            formatCode = DisplayNameFormatCode.valueOf(command.formatCode().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown formatCode: " + command.formatCode() +
                    ". Valid values: FULL_TITLE_CASE, FULL_UPPER, SURNAME_FIRST_UPPER, SHORT_TITLE, SHORT_UPPER, SURNAME_ABBREV_UPPER");
        }

        return formatRepository.save(new EmployeeDisplayNameFormat(normalizedCode, formatCode));
    }
}
```

**Note:** Check `src/main/java/com/b4rrhh/rulesystem/domain/port/RuleSystemRepository.java` to confirm the `findByCode` method signature. If it differs, adjust the import and call accordingly.

- [ ] **Step 6: Run tests to verify they pass**

```bash
mvn test -Dtest="GetEmployeeDisplayNameFormatServiceTest,UpsertEmployeeDisplayNameFormatServiceTest" -q 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/ \
        src/test/java/com/b4rrhh/rulesystem/employeedisplaynameformat/application/
git commit -m "feat(display-name-format): add Get and Upsert use cases with tests"
```

---

## Task 5: Web layer + OpenAPI spec

**Files:**
- Create: `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/infrastructure/web/dto/EmployeeDisplayNameFormatResponse.java`
- Create: `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/infrastructure/web/dto/UpsertEmployeeDisplayNameFormatRequest.java`
- Create: `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/infrastructure/web/EmployeeDisplayNameFormatController.java`
- Modify: `openapi/personnel-administration-api.yaml`

- [ ] **Step 1: Create DTOs**

Create `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/infrastructure/web/dto/EmployeeDisplayNameFormatResponse.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.infrastructure.web.dto;

public record EmployeeDisplayNameFormatResponse(
        String ruleSystemCode,
        String formatCode,
        String formatLabel,
        String example
) {}
```

Create `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/infrastructure/web/dto/UpsertEmployeeDisplayNameFormatRequest.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.infrastructure.web.dto;

public record UpsertEmployeeDisplayNameFormatRequest(
        String formatCode
) {}
```

- [ ] **Step 2: Create the controller**

Create `src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/infrastructure/web/EmployeeDisplayNameFormatController.java`:

```java
package com.b4rrhh.rulesystem.employeedisplaynameformat.infrastructure.web;

import com.b4rrhh.rulesystem.employeedisplaynameformat.application.usecase.GetEmployeeDisplayNameFormatUseCase;
import com.b4rrhh.rulesystem.employeedisplaynameformat.application.usecase.UpsertEmployeeDisplayNameFormatCommand;
import com.b4rrhh.rulesystem.employeedisplaynameformat.application.usecase.UpsertEmployeeDisplayNameFormatUseCase;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.DisplayNameFormatCode;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.DisplayNameFormatter;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.EmployeeDisplayNameFormat;
import com.b4rrhh.rulesystem.employeedisplaynameformat.infrastructure.web.dto.EmployeeDisplayNameFormatResponse;
import com.b4rrhh.rulesystem.employeedisplaynameformat.infrastructure.web.dto.UpsertEmployeeDisplayNameFormatRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rule-systems/{ruleSystemCode}/employee-display-name-format")
public class EmployeeDisplayNameFormatController {

    private static final String EXAMPLE_FIRST = "Juan Antonio";
    private static final String EXAMPLE_LAST1 = "Biforcos";
    private static final String EXAMPLE_LAST2 = "Amor";

    private final GetEmployeeDisplayNameFormatUseCase getUseCase;
    private final UpsertEmployeeDisplayNameFormatUseCase upsertUseCase;

    public EmployeeDisplayNameFormatController(
            GetEmployeeDisplayNameFormatUseCase getUseCase,
            UpsertEmployeeDisplayNameFormatUseCase upsertUseCase) {
        this.getUseCase = getUseCase;
        this.upsertUseCase = upsertUseCase;
    }

    @GetMapping
    public ResponseEntity<EmployeeDisplayNameFormatResponse> get(
            @PathVariable String ruleSystemCode) {
        return getUseCase.getByRuleSystemCode(ruleSystemCode)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<EmployeeDisplayNameFormatResponse> upsert(
            @PathVariable String ruleSystemCode,
            @RequestBody UpsertEmployeeDisplayNameFormatRequest request) {
        EmployeeDisplayNameFormat format = upsertUseCase.upsert(
                new UpsertEmployeeDisplayNameFormatCommand(ruleSystemCode, request.formatCode()));
        return ResponseEntity.ok(toResponse(format));
    }

    private EmployeeDisplayNameFormatResponse toResponse(EmployeeDisplayNameFormat format) {
        String example = DisplayNameFormatter.format(
                EXAMPLE_FIRST, EXAMPLE_LAST1, EXAMPLE_LAST2, format.formatCode());
        return new EmployeeDisplayNameFormatResponse(
                format.ruleSystemCode(),
                format.formatCode().name(),
                formatLabel(format.formatCode()),
                example
        );
    }

    private String formatLabel(DisplayNameFormatCode code) {
        return switch (code) {
            case FULL_TITLE_CASE -> "Nombre completo (mayúsculas iniciales)";
            case FULL_UPPER -> "Nombre completo en mayúsculas";
            case SURNAME_FIRST_UPPER -> "Apellidos, Nombre (mayúsculas)";
            case SHORT_TITLE -> "Nombre y primer apellido (mayúsculas iniciales)";
            case SHORT_UPPER -> "Nombre y primer apellido (mayúsculas)";
            case SURNAME_ABBREV_UPPER -> "Apellidos, iniciales del nombre";
        };
    }
}
```

- [ ] **Step 3: Update OpenAPI spec — add displayName to EmployeeResponse**

Open `openapi/personnel-administration-api.yaml`. Find the `EmployeeResponse` schema (around line 6321). Add `displayName` as a required field:

```yaml
    EmployeeResponse:
      type: object
      required:
        - id
        - ruleSystemCode
        - employeeTypeCode
        - employeeNumber
        - firstName
        - lastName1
        - displayName        # ADD THIS LINE
        - status
      properties:
        id:
          type: integer
          format: int64
        ruleSystemCode:
          type: string
        employeeTypeCode:
          type: string
        employeeNumber:
          type: string
        firstName:
          type: string
        lastName1:
          type: string
        lastName2:
          type: string
          nullable: true
        preferredName:
          type: string
          nullable: true
        displayName:          # ADD THIS BLOCK
          type: string
          description: Computed display name based on the rule system's configured format.
        status:
          type: string
        photoUrl:
          type: string
          nullable: true
```

- [ ] **Step 4: Update OpenAPI spec — add new paths and schemas**

In `openapi/personnel-administration-api.yaml`, add the following to the `paths:` section (insert near the other rule-system-level paths, or at the end of the paths section before `components:`):

```yaml
  /rule-systems/{ruleSystemCode}/employee-display-name-format:
    get:
      tags:
        - Employee Display Name Format
      summary: Get the employee display name format for a rule system
      operationId: getEmployeeDisplayNameFormat
      parameters:
        - name: ruleSystemCode
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Current format configuration
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EmployeeDisplayNameFormatResponse'
        '404':
          description: No format configured for this rule system
    put:
      tags:
        - Employee Display Name Format
      summary: Set the employee display name format for a rule system
      operationId: upsertEmployeeDisplayNameFormat
      parameters:
        - name: ruleSystemCode
          in: path
          required: true
          schema:
            type: string
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpsertEmployeeDisplayNameFormatRequest'
      responses:
        '200':
          description: Updated format configuration
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EmployeeDisplayNameFormatResponse'
        '400':
          description: Invalid formatCode or rule system not found
```

In the `components.schemas:` section, add:

```yaml
    EmployeeDisplayNameFormatResponse:
      type: object
      required:
        - ruleSystemCode
        - formatCode
        - formatLabel
        - example
      properties:
        ruleSystemCode:
          type: string
        formatCode:
          type: string
          enum:
            - FULL_TITLE_CASE
            - FULL_UPPER
            - SURNAME_FIRST_UPPER
            - SHORT_TITLE
            - SHORT_UPPER
            - SURNAME_ABBREV_UPPER
        formatLabel:
          type: string
          description: Human-readable label for the format code (in Spanish)
        example:
          type: string
          description: Example of the format applied to "Juan Antonio Biforcos Amor"

    UpsertEmployeeDisplayNameFormatRequest:
      type: object
      required:
        - formatCode
      properties:
        formatCode:
          type: string
          enum:
            - FULL_TITLE_CASE
            - FULL_UPPER
            - SURNAME_FIRST_UPPER
            - SHORT_TITLE
            - SHORT_UPPER
            - SURNAME_ABBREV_UPPER
```

- [ ] **Step 5: Compile and run all tests**

```bash
mvn test -q 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/b4rrhh/rulesystem/employeedisplaynameformat/infrastructure/web/ \
        openapi/personnel-administration-api.yaml
git commit -m "feat(display-name-format): add web layer and OpenAPI spec update"
```

---

## Task 6: DisplayNameFormatLookupPort + adapter + DisplayNameComputationService

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/employee/application/port/DisplayNameFormatLookupPort.java`
- Create: `src/main/java/com/b4rrhh/employee/employee/application/DisplayNameComputationService.java`
- Create: `src/main/java/com/b4rrhh/employee/employee/infrastructure/adapters/DisplayNameFormatLookupAdapter.java`
- Test: `src/test/java/com/b4rrhh/employee/employee/application/DisplayNameComputationServiceTest.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/b4rrhh/employee/employee/application/DisplayNameComputationServiceTest.java`:

```java
package com.b4rrhh.employee.employee.application;

import com.b4rrhh.employee.employee.application.port.DisplayNameFormatLookupPort;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.DisplayNameFormatCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisplayNameComputationServiceTest {

    @Mock DisplayNameFormatLookupPort formatLookupPort;
    @InjectMocks DisplayNameComputationService service;

    @Test
    void preferredName_overridesFormat() {
        when(formatLookupPort.findFormatCodeForRuleSystem("RS1"))
                .thenReturn(Optional.of(DisplayNameFormatCode.FULL_UPPER));

        String result = service.compute("RS1", "Juan", "Garcia", null, "Juanito");

        assertThat(result).isEqualTo("Juanito");
    }

    @Test
    void appliesFormatCode_whenPreferredNameIsNull() {
        when(formatLookupPort.findFormatCodeForRuleSystem("RS1"))
                .thenReturn(Optional.of(DisplayNameFormatCode.FULL_UPPER));

        String result = service.compute("RS1", "Juan", "Garcia", "Lopez", null);

        assertThat(result).isEqualTo("JUAN GARCIA LOPEZ");
    }

    @Test
    void fallsBackToConcatenation_whenNoFormatConfigured() {
        when(formatLookupPort.findFormatCodeForRuleSystem("RS1"))
                .thenReturn(Optional.empty());

        String result = service.compute("RS1", "Juan", "Garcia", "Lopez", null);

        assertThat(result).isEqualTo("Juan Garcia Lopez");
    }

    @Test
    void fallback_skipsNullLastName2() {
        when(formatLookupPort.findFormatCodeForRuleSystem("RS1"))
                .thenReturn(Optional.empty());

        String result = service.compute("RS1", "Juan", "Garcia", null, null);

        assertThat(result).isEqualTo("Juan Garcia");
    }

    @Test
    void blankPreferredName_treatedAsNull() {
        when(formatLookupPort.findFormatCodeForRuleSystem("RS1"))
                .thenReturn(Optional.of(DisplayNameFormatCode.FULL_UPPER));

        String result = service.compute("RS1", "Juan", "Garcia", null, "   ");

        assertThat(result).isEqualTo("JUAN GARCIA");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
mvn test -Dtest=DisplayNameComputationServiceTest -q 2>&1 | tail -5
```
Expected: compilation error — classes not found.

- [ ] **Step 3: Create DisplayNameFormatLookupPort**

Create `src/main/java/com/b4rrhh/employee/employee/application/port/DisplayNameFormatLookupPort.java`:

```java
package com.b4rrhh.employee.employee.application.port;

import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.DisplayNameFormatCode;
import java.util.Optional;

public interface DisplayNameFormatLookupPort {
    Optional<DisplayNameFormatCode> findFormatCodeForRuleSystem(String ruleSystemCode);
}
```

- [ ] **Step 4: Create DisplayNameComputationService**

Create `src/main/java/com/b4rrhh/employee/employee/application/DisplayNameComputationService.java`:

```java
package com.b4rrhh.employee.employee.application;

import com.b4rrhh.employee.employee.application.port.DisplayNameFormatLookupPort;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.DisplayNameFormatter;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DisplayNameComputationService {

    private final DisplayNameFormatLookupPort formatLookupPort;

    public DisplayNameComputationService(DisplayNameFormatLookupPort formatLookupPort) {
        this.formatLookupPort = formatLookupPort;
    }

    public String compute(
            String ruleSystemCode,
            String firstName,
            String lastName1,
            String lastName2,
            String preferredName) {

        if (preferredName != null && !preferredName.isBlank()) {
            return preferredName.trim();
        }

        return formatLookupPort.findFormatCodeForRuleSystem(ruleSystemCode)
                .map(code -> DisplayNameFormatter.format(firstName, lastName1, lastName2, code))
                .orElseGet(() -> Stream.of(firstName, lastName1, lastName2)
                        .filter(s -> s != null && !s.isBlank())
                        .map(String::trim)
                        .collect(Collectors.joining(" ")));
    }
}
```

- [ ] **Step 5: Create DisplayNameFormatLookupAdapter**

Create `src/main/java/com/b4rrhh/employee/employee/infrastructure/adapters/DisplayNameFormatLookupAdapter.java`:

```java
package com.b4rrhh.employee.employee.infrastructure.adapters;

import com.b4rrhh.employee.employee.application.port.DisplayNameFormatLookupPort;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.model.DisplayNameFormatCode;
import com.b4rrhh.rulesystem.employeedisplaynameformat.domain.port.EmployeeDisplayNameFormatRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class DisplayNameFormatLookupAdapter implements DisplayNameFormatLookupPort {

    private final EmployeeDisplayNameFormatRepository formatRepository;

    public DisplayNameFormatLookupAdapter(EmployeeDisplayNameFormatRepository formatRepository) {
        this.formatRepository = formatRepository;
    }

    @Override
    public Optional<DisplayNameFormatCode> findFormatCodeForRuleSystem(String ruleSystemCode) {
        return formatRepository.findByRuleSystemCode(ruleSystemCode)
                .map(format -> format.formatCode());
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

```bash
mvn test -Dtest=DisplayNameComputationServiceTest -q 2>&1 | tail -5
```
Expected: `BUILD SUCCESS`, 5 tests pass.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/employee/application/ \
        src/main/java/com/b4rrhh/employee/employee/infrastructure/adapters/ \
        src/test/java/com/b4rrhh/employee/employee/application/
git commit -m "feat(display-name-format): add DisplayNameComputationService and lookup port/adapter"
```

---

## Task 7: Wire displayName into EmployeeResponse and EmployeeDirectoryPersistenceAdapter

**Files:**
- Modify: `src/main/java/com/b4rrhh/employee/employee/infrastructure/web/dto/EmployeeResponse.java`
- Modify: `src/main/java/com/b4rrhh/employee/employee/infrastructure/web/EmployeeBusinessKeyController.java`
- Modify: `src/main/java/com/b4rrhh/employee/employee/infrastructure/persistence/EmployeeDirectoryPersistenceAdapter.java`

- [ ] **Step 1: Add displayName to EmployeeResponse**

Open `src/main/java/com/b4rrhh/employee/employee/infrastructure/web/dto/EmployeeResponse.java`. Replace the entire file content:

```java
package com.b4rrhh.employee.employee.infrastructure.web.dto;

public record EmployeeResponse(
        Long id,
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        String firstName,
        String lastName1,
        String lastName2,
        String preferredName,
        String displayName,
        String status,
        String photoUrl
) {
}
```

- [ ] **Step 2: Update EmployeeBusinessKeyController to compute and populate displayName**

Open `src/main/java/com/b4rrhh/employee/employee/infrastructure/web/EmployeeBusinessKeyController.java`.

Add import for `DisplayNameComputationService`:
```java
import com.b4rrhh.employee.employee.application.DisplayNameComputationService;
```

Add the service as a constructor dependency:
```java
private final DisplayNameComputationService displayNameComputationService;

public EmployeeBusinessKeyController(
        GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase,
        DeleteEmployeeByBusinessKeyUseCase deleteEmployeeByBusinessKeyUseCase,
        UpdateEmployeeUseCase updateEmployeeUseCase,
        DisplayNameComputationService displayNameComputationService) {
    this.getEmployeeByBusinessKeyUseCase = getEmployeeByBusinessKeyUseCase;
    this.deleteEmployeeByBusinessKeyUseCase = deleteEmployeeByBusinessKeyUseCase;
    this.updateEmployeeUseCase = updateEmployeeUseCase;
    this.displayNameComputationService = displayNameComputationService;
}
```

Replace the `toResponse` method:
```java
private EmployeeResponse toResponse(Employee employee) {
    String displayName = displayNameComputationService.compute(
            employee.getRuleSystemCode(),
            employee.getFirstName(),
            employee.getLastName1(),
            employee.getLastName2(),
            employee.getPreferredName()
    );
    return new EmployeeResponse(
            employee.getId(),
            employee.getRuleSystemCode(),
            employee.getEmployeeTypeCode(),
            employee.getEmployeeNumber(),
            employee.getFirstName(),
            employee.getLastName1(),
            employee.getLastName2(),
            employee.getPreferredName(),
            displayName,
            employee.getStatus(),
            employee.getPhotoUrl()
    );
}
```

- [ ] **Step 3: Update EmployeeDirectoryPersistenceAdapter to use DisplayNameComputationService**

Open `src/main/java/com/b4rrhh/employee/employee/infrastructure/persistence/EmployeeDirectoryPersistenceAdapter.java`.

Add the import and constructor dependency for `DisplayNameComputationService`:
```java
import com.b4rrhh.employee.employee.application.DisplayNameComputationService;
```

Update the constructor to include the service:
```java
private final SpringDataEmployeeRepository springDataEmployeeRepository;
private final DisplayNameComputationService displayNameComputationService;

public EmployeeDirectoryPersistenceAdapter(
        SpringDataEmployeeRepository springDataEmployeeRepository,
        DisplayNameComputationService displayNameComputationService) {
    this.springDataEmployeeRepository = springDataEmployeeRepository;
    this.displayNameComputationService = displayNameComputationService;
}
```

Replace the `toDomain` and `buildDisplayName` methods — delete `buildDisplayName` entirely and replace `toDomain`:
```java
private EmployeeDirectoryItem toDomain(EmployeeDirectoryProjection projection) {
    String displayName = displayNameComputationService.compute(
            projection.ruleSystemCode(),
            projection.firstName(),
            projection.lastName1(),
            projection.lastName2(),
            projection.preferredName()
    );
    return new EmployeeDirectoryItem(
            projection.ruleSystemCode(),
            projection.employeeTypeCode(),
            projection.employeeNumber(),
            displayName,
            projection.status(),
            projection.workCenterCode()
    );
}
```

Also delete the `normalizeNamePart` private method — it is no longer needed.

- [ ] **Step 4: Run all tests**

```bash
mvn test -q 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`. All tests pass — including any existing tests for `EmployeeBusinessKeyController` or `EmployeeDirectoryPersistenceAdapter`.

**If any existing tests break:** They will fail because `EmployeeResponse` now has an extra `displayName` field. Find the test constructing `EmployeeResponse` and add `displayName` as a parameter. Similarly, if `EmployeeDirectoryPersistenceAdapter` tests mock a different constructor, update them.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/employee/infrastructure/
git commit -m "feat(display-name-format): wire displayName into EmployeeResponse and directory listing"
```

---

## Task 8: Frontend — regenerate API client + update mapper and model

**Working directory for all steps: `c:\Users\bifor\Documents\Proyectos\B4RRHH\b4rrhh_frontend`**

**Files:**
- Modify: `src/app/core/api/clients/employee-read.client.ts`
- Modify: `src/app/core/api/mappers/employee-detail.mapper.ts`
- Create: `src/app/core/api/clients/employee-display-name-format.client.ts`

- [ ] **Step 1: Regenerate the API client from the updated OpenAPI spec**

Make sure the backend is running OR the spec file is in place. Then:
```bash
npm run api:refresh
```
Expected: No errors. The generated files in `src/app/core/api/generated/` are updated — in particular `model/employee-response.ts` now has a `displayName` field.

- [ ] **Step 2: Update EmployeeReadApiModel to include displayName**

Open `src/app/core/api/clients/employee-read.client.ts`.

In the `EmployeeReadApiModel` interface, add `displayName`:
```typescript
export interface EmployeeReadApiModel {
  id: number;
  ruleSystemCode: string;
  employeeTypeCode: string;
  employeeNumber: string;
  firstName: string;
  lastName1: string;
  lastName2: string | null;
  preferredName: string | null;
  displayName: string;
  status: string;
  photoUrl: string | null;
}
```

In `toEmployeeReadApiModel`, add the mapping:
```typescript
private toEmployeeReadApiModel(source: EmployeeResponse): EmployeeReadApiModel {
  return {
    id: source.id,
    ruleSystemCode: source.ruleSystemCode,
    employeeTypeCode: source.employeeTypeCode,
    employeeNumber: source.employeeNumber,
    firstName: source.firstName,
    lastName1: source.lastName1,
    lastName2: source.lastName2 ?? null,
    preferredName: source.preferredName ?? null,
    displayName: source.displayName,
    status: source.status,
    photoUrl: source.photoUrl ?? null,
  };
}
```

- [ ] **Step 3: Update employee-detail.mapper.ts to use backend-provided displayName**

Open `src/app/core/api/mappers/employee-detail.mapper.ts`.

Change `displayName: buildDisplayName(source)` to `displayName: source.displayName` and **delete** the `buildDisplayName` function entirely. The full updated file:

```typescript
import { EmployeeReadApiModel } from '../clients/employee-read.client';

export interface EmployeeDetailReadModel {
  id: number;
  ruleSystemCode: string;
  employeeTypeCode: string;
  employeeNumber: string;
  firstName: string;
  lastName1: string;
  lastName2: string | null;
  preferredName: string | null;
  displayName: string;
  statusLabel: string;
  workCenter: string;
  photoUrl: string | null;
}

const pendingWorkCenterLabel = 'Pending assignment';

export function mapEmployeeReadApiToDetailModel(source: EmployeeReadApiModel): EmployeeDetailReadModel {
  return {
    id: source.id,
    ruleSystemCode: source.ruleSystemCode,
    employeeTypeCode: source.employeeTypeCode,
    employeeNumber: source.employeeNumber,
    firstName: source.firstName,
    lastName1: source.lastName1,
    lastName2: source.lastName2,
    preferredName: source.preferredName,
    displayName: source.displayName,
    statusLabel: source.status,
    workCenter: pendingWorkCenterLabel,
    photoUrl: source.photoUrl ?? null,
  };
}
```

- [ ] **Step 4: Create the display name format API client**

Create `src/app/core/api/clients/employee-display-name-format.client.ts`:

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EmployeeDisplayNameFormatModel {
  ruleSystemCode: string;
  formatCode: string;
  formatLabel: string;
  example: string;
}

export interface UpsertDisplayNameFormatRequest {
  formatCode: string;
}

export const DISPLAY_NAME_FORMAT_CODES = [
  { code: 'FULL_TITLE_CASE',     label: 'Nombre completo (mayúsculas iniciales)', example: 'Juan Antonio Biforcos Amor' },
  { code: 'FULL_UPPER',          label: 'Nombre completo en mayúsculas',           example: 'JUAN ANTONIO BIFORCOS AMOR' },
  { code: 'SURNAME_FIRST_UPPER', label: 'Apellidos, Nombre (mayúsculas)',           example: 'BIFORCOS AMOR, JUAN ANTONIO' },
  { code: 'SHORT_TITLE',         label: 'Nombre y primer apellido',                 example: 'Juan Antonio Biforcos' },
  { code: 'SHORT_UPPER',         label: 'Nombre y primer apellido (mayúsculas)',    example: 'JUAN ANTONIO BIFORCOS' },
  { code: 'SURNAME_ABBREV_UPPER',label: 'Apellidos, iniciales del nombre',          example: 'BIFORCOS AMOR, J.A.' },
] as const;

@Injectable({ providedIn: 'root' })
export class EmployeeDisplayNameFormatClient {
  private readonly http = inject(HttpClient);

  get(ruleSystemCode: string): Observable<EmployeeDisplayNameFormatModel> {
    return this.http.get<EmployeeDisplayNameFormatModel>(
      `/api/rule-systems/${ruleSystemCode}/employee-display-name-format`
    );
  }

  upsert(ruleSystemCode: string, request: UpsertDisplayNameFormatRequest): Observable<EmployeeDisplayNameFormatModel> {
    return this.http.put<EmployeeDisplayNameFormatModel>(
      `/api/rule-systems/${ruleSystemCode}/employee-display-name-format`,
      request
    );
  }
}
```

- [ ] **Step 5: TypeScript build check**

```bash
npx tsc --noEmit 2>&1 | head -20
```
Expected: no output (no errors).

- [ ] **Step 6: Commit**

```bash
git add src/app/core/api/
git commit -m "feat(display-name-format): update frontend API client and mapper to use backend displayName"
```

---

## Task 9: Frontend — relabel preferredName field in identity edit form

**Working directory: `c:\Users\bifor\Documents\Proyectos\B4RRHH\b4rrhh_frontend`**

**Files:**
- Modify: `src/app/features/employee/shell/components/employee-detail-header.component.html`

- [ ] **Step 1: Update the preferredName field label and hint**

Open `src/app/features/employee/shell/components/employee-detail-header.component.html`.

Find the `preferredName` form field (around line 88). Change the label and hint to clarify it overrides the computed format:

```html
<label class="employee-detail-header__field">
  <span class="employee-detail-header__field-label">{{ texts.detailHeaderPreferredNameLabel }}</span>
  <input pInputText type="text" formControlName="preferredName" autocomplete="nickname" />
  <span class="employee-detail-header__field-hint">Deja vacío para usar el formato configurado en la empresa</span>
</label>
```

- [ ] **Step 2: TypeScript build check**

```bash
npx tsc --noEmit 2>&1 | head -20
```
Expected: no output.

- [ ] **Step 3: Commit**

```bash
git add src/app/features/employee/shell/components/employee-detail-header.component.html
git commit -m "feat(display-name-format): clarify preferredName field is an override of the computed format"
```

---

## Task 10: Frontend — display name format config card in company page

**Working directory: `c:\Users\bifor\Documents\Proyectos\B4RRHH\b4rrhh_frontend`**

**Files:**
- Create: `src/app/features/company/ui/display-name-format-card.component.ts`
- Create: `src/app/features/company/ui/display-name-format-card.component.html`
- Create: `src/app/features/company/ui/display-name-format-card.component.scss`
- Modify: `src/app/features/company/ui/company-page.component.ts`
- Modify: `src/app/features/company/ui/company-page.component.html`

- [ ] **Step 1: Create the component TS**

Create `src/app/features/company/ui/display-name-format-card.component.ts`:

```typescript
import { ChangeDetectionStrategy, Component, OnChanges, SimpleChanges, inject, input, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { FormsModule } from '@angular/forms';

import {
  DISPLAY_NAME_FORMAT_CODES,
  EmployeeDisplayNameFormatClient,
  EmployeeDisplayNameFormatModel,
} from '../../../core/api/clients/employee-display-name-format.client';
import { SectionCardComponent } from '../../../shared/ui/section-card/section-card.component';

@Component({
  selector: 'app-display-name-format-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SectionCardComponent, SelectModule, ButtonModule, FormsModule],
  templateUrl: './display-name-format-card.component.html',
  styleUrl: './display-name-format-card.component.scss',
})
export class DisplayNameFormatCardComponent implements OnChanges {
  readonly ruleSystemCode = input.required<string>();

  private readonly client = inject(EmployeeDisplayNameFormatClient);

  protected readonly formatOptions = DISPLAY_NAME_FORMAT_CODES.map(f => ({
    label: `${f.label} — ${f.example}`,
    value: f.code,
  }));

  protected readonly currentFormat = signal<EmployeeDisplayNameFormatModel | null>(null);
  protected readonly selectedCode = signal<string>('FULL_TITLE_CASE');
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly saveSuccess = signal(false);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['ruleSystemCode']) {
      this.load();
    }
  }

  protected save(): void {
    const code = this.selectedCode();
    if (!code) return;
    this.saving.set(true);
    this.saveError.set(null);
    this.saveSuccess.set(false);

    this.client.upsert(this.ruleSystemCode(), { formatCode: code }).subscribe({
      next: (result) => {
        this.currentFormat.set(result);
        this.saveSuccess.set(true);
        this.saving.set(false);
        setTimeout(() => this.saveSuccess.set(false), 3000);
      },
      error: (err: HttpErrorResponse) => {
        this.saveError.set('No se pudo guardar el formato. Código: ' + err.status);
        this.saving.set(false);
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.client.get(this.ruleSystemCode()).subscribe({
      next: (format) => {
        this.currentFormat.set(format);
        this.selectedCode.set(format.formatCode);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.currentFormat.set(null);
          this.selectedCode.set('FULL_TITLE_CASE');
        }
        this.loading.set(false);
      },
    });
  }
}
```

- [ ] **Step 2: Create the component HTML**

Create `src/app/features/company/ui/display-name-format-card.component.html`:

```html
<app-section-card
  title="Formato de nombre normativo"
  description="Cómo se presenta el nombre de los empleados en toda la aplicación"
>
  <div class="dnf-card">
    @if (loading()) {
      <p class="dnf-card__loading">Cargando configuración...</p>
    } @else {
      <div class="dnf-card__form">
        <label class="dnf-card__label" for="dnf-select">Formato</label>
        <p-select
          inputId="dnf-select"
          [options]="formatOptions"
          [(ngModel)]="selectedCode"
          optionLabel="label"
          optionValue="value"
          placeholder="Selecciona un formato"
          styleClass="dnf-card__select"
        />
      </div>

      @if (currentFormat(); as fmt) {
        <div class="dnf-card__preview">
          <span class="dnf-card__preview-label">Ejemplo actual:</span>
          <span class="dnf-card__preview-value">{{ fmt.example }}</span>
        </div>
      } @else {
        <p class="dnf-card__hint">Sin formato configurado — se usará el nombre completo por defecto.</p>
      }

      <div class="dnf-card__actions">
        <button
          type="button"
          pButton
          label="Guardar formato"
          [loading]="saving()"
          [disabled]="saving()"
          (click)="save()"
        ></button>

        @if (saveSuccess()) {
          <span class="dnf-card__success">Guardado correctamente</span>
        }
        @if (saveError(); as err) {
          <span class="dnf-card__error">{{ err }}</span>
        }
      </div>
    }
  </div>
</app-section-card>
```

- [ ] **Step 3: Create the component SCSS**

Create `src/app/features/company/ui/display-name-format-card.component.scss`:

```scss
.dnf-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 4px 0;
}

.dnf-card__loading {
  font-size: 13px;
  color: var(--text-tertiary, #9ca3af);
}

.dnf-card__form {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dnf-card__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary, #6b7280);
}

.dnf-card__select {
  width: 100%;
}

.dnf-card__preview {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dnf-card__preview-label {
  font-size: 11px;
  color: var(--text-tertiary, #9ca3af);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.dnf-card__preview-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary, #111827);
  font-family: 'Courier New', monospace;
}

.dnf-card__hint {
  font-size: 12px;
  color: var(--text-tertiary, #9ca3af);
  font-style: italic;
  margin: 0;
}

.dnf-card__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dnf-card__success {
  font-size: 13px;
  color: #059669;
  font-weight: 500;
}

.dnf-card__error {
  font-size: 13px;
  color: #dc2626;
}
```

- [ ] **Step 4: Wire the card into the company page**

Open `src/app/features/company/ui/company-page.component.ts`.

Add the import:
```typescript
import { DisplayNameFormatCardComponent } from './display-name-format-card.component';
```

Add `DisplayNameFormatCardComponent` to the `imports` array in the `@Component` decorator.

- [ ] **Step 5: Add the card to the company page HTML**

Open `src/app/features/company/ui/company-page.component.html`.

Find the section inside the `@if (store.hasActiveDetail())` block, after the company detail panel (`<app-company-detail-panel ... />`). Add the format card below it:

```html
<app-display-name-format-card
  [ruleSystemCode]="store.selectedKey()!.ruleSystemCode"
/>
```

The exact location depends on how the detail slot is structured. Add it inside the `slot="detail"` section, after the existing `<app-company-detail-panel />`.

- [ ] **Step 6: TypeScript build check**

```bash
npx tsc --noEmit 2>&1 | head -20
```
Expected: no output (no errors).

- [ ] **Step 7: Run all frontend tests**

```bash
npm run test 2>&1 | tail -15
```
Expected: all previously passing tests still pass.

- [ ] **Step 8: Commit**

```bash
git add src/app/features/company/ui/display-name-format-card.component.* \
        src/app/features/company/ui/company-page.component.*
git commit -m "feat(display-name-format): add display name format config card to company settings"
```

---

## Self-Review

### Spec coverage check

| Requirement | Task |
|-------------|------|
| Company-level format config (enum) | Task 1, 5 |
| Format codes: FULL_TITLE_CASE, FULL_UPPER, SURNAME_FIRST_UPPER, SHORT_TITLE, SHORT_UPPER, SURNAME_ABBREV_UPPER | Task 1 |
| Format applied at write-time (read-time computation) | Task 6, 7 |
| `preferredName` stays as manual override | Task 6, 9 |
| `displayName` in EmployeeResponse API | Task 5 (OpenAPI), Task 7 |
| displayName in directory listing | Task 7 |
| Frontend uses backend-provided displayName | Task 8 |
| Config UI in company settings | Task 10 |
| Example preview in API response | Task 5 (controller formatLabel + example) |

### Type consistency

- `DisplayNameFormatCode` enum defined in Task 1, used in Tasks 2, 4, 5, 6
- `EmployeeDisplayNameFormat(ruleSystemCode, formatCode)` record defined in Task 2, used in Tasks 3, 4, 5
- `DisplayNameFormatLookupPort.findFormatCodeForRuleSystem` defined in Task 6, implemented in Task 6
- `DisplayNameComputationService.compute(ruleSystemCode, firstName, lastName1, lastName2, preferredName)` — 5 params, defined and tested in Task 6, called in Task 7
- `EmployeeResponse` with `displayName` field defined in Task 7, consumed in Task 8
- `EmployeeDisplayNameFormatModel` in frontend client uses `formatCode`, `formatLabel`, `example` — matches API response fields

### No placeholders scan

✅ All code blocks are complete. No "TBD" or "add appropriate handling" phrases found.
