# Employee Type Catalog Validation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `"EMP"` placeholder with `"INTERNAL"` as the default employee type code, and add a `EmployeeTypeCatalogValidator` component that validates `employeeTypeCode` against the `EMPLOYEE_TYPE` rule entity catalog — exactly like `ContractCatalogValidator` does for contracts.

**Architecture:** New `EmployeeTypeCatalogValidator @Component` in `com.b4rrhh.employee.employee.application.service`, wired into both `HireEmployeeService` (10th constructor param) and `RehireEmployeeService` (15th constructor param). New `EmployeeTypeInvalidException` in `com.b4rrhh.employee.employee.domain.exception`. `HireEmployeeDefaultValues.DEFAULT_EMPLOYEE_TYPE_CODE` changes from `"EMP"` to `"INTERNAL"`. New V93 migration registers the field in `resource_field_catalog_binding`. Frontend default `HIRE_EMPLOYEE_DEFAULTS.employeeTypeCode` changes from `'EMP'` to `'INTERNAL'`.

**Tech Stack:** Java 21 / Spring Boot, JUnit 5 / Mockito, Angular 21 / Vitest, Flyway PostgreSQL migrations.

---

## File Map

| Action | Path |
|--------|------|
| Create | `src/main/java/com/b4rrhh/employee/employee/domain/exception/EmployeeTypeInvalidException.java` |
| Create | `src/test/java/com/b4rrhh/employee/employee/application/service/EmployeeTypeCatalogValidatorTest.java` |
| Create | `src/main/java/com/b4rrhh/employee/employee/application/service/EmployeeTypeCatalogValidator.java` |
| Modify | `src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/HireEmployeeServiceTest.java` |
| Modify | `src/main/java/com/b4rrhh/employee/lifecycle/application/usecase/HireEmployeeService.java` |
| Modify | `src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/RehireEmployeeServiceTest.java` |
| Modify | `src/main/java/com/b4rrhh/employee/lifecycle/application/usecase/RehireEmployeeService.java` |
| Modify | `src/main/java/com/b4rrhh/employee/lifecycle/application/model/HireEmployeeDefaultValues.java` |
| Modify | `src/test/java/com/b4rrhh/employee/lifecycle/infrastructure/rest/HireEmployeeDefaultingTest.java` |
| Create | `src/main/resources/db/migration/V93__seed_employee_type_catalog_binding.sql` |
| Modify | `b4rrhh_frontend/src/app/features/employee/models/hire-employee.defaults.ts` |
| Modify | Several frontend `*.spec.ts` files (see Task 8) |

---

## Task 1: `EmployeeTypeInvalidException`

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/employee/domain/exception/EmployeeTypeInvalidException.java`

- [ ] **Step 1: Create the exception**

```java
package com.b4rrhh.employee.employee.domain.exception;

public class EmployeeTypeInvalidException extends RuntimeException {
    public EmployeeTypeInvalidException(String code) {
        super("Invalid employeeTypeCode: '" + code + "'");
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/employee/domain/exception/EmployeeTypeInvalidException.java
git commit -m "feat(employee-type): add EmployeeTypeInvalidException domain exception"
```

---

## Task 2: `EmployeeTypeCatalogValidatorTest` — write failing tests

**Files:**
- Create: `src/test/java/com/b4rrhh/employee/employee/application/service/EmployeeTypeCatalogValidatorTest.java`

- [ ] **Step 1: Write the failing test class**

```java
package com.b4rrhh.employee.employee.application.service;

import com.b4rrhh.employee.employee.domain.exception.EmployeeTypeInvalidException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeTypeCatalogValidatorTest {

    @Mock
    private RuleEntityRepository ruleEntityRepository;

    private EmployeeTypeCatalogValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EmployeeTypeCatalogValidator(ruleEntityRepository);
    }

    @Test
    void acceptsActiveCodeWithinDateRange() {
        RuleEntity entity = mock(RuleEntity.class);
        when(entity.isActive()).thenReturn(true);
        when(entity.getStartDate()).thenReturn(LocalDate.of(1900, 1, 1));
        when(entity.getEndDate()).thenReturn(null);
        when(ruleEntityRepository.findByBusinessKey("ESP", "EMPLOYEE_TYPE", "INTERNAL"))
                .thenReturn(Optional.of(entity));

        assertDoesNotThrow(() ->
                validator.validateEmployeeTypeCode("ESP", "INTERNAL", LocalDate.of(2026, 1, 1)));
    }

    @Test
    void rejectsCodeNotInCatalog() {
        when(ruleEntityRepository.findByBusinessKey("ESP", "EMPLOYEE_TYPE", "BAD"))
                .thenReturn(Optional.empty());

        assertThrows(EmployeeTypeInvalidException.class, () ->
                validator.validateEmployeeTypeCode("ESP", "BAD", LocalDate.of(2026, 1, 1)));
    }

    @Test
    void rejectsInactiveCode() {
        RuleEntity entity = mock(RuleEntity.class);
        when(entity.isActive()).thenReturn(false);
        when(ruleEntityRepository.findByBusinessKey("ESP", "EMPLOYEE_TYPE", "INTERNAL"))
                .thenReturn(Optional.of(entity));

        assertThrows(EmployeeTypeInvalidException.class, () ->
                validator.validateEmployeeTypeCode("ESP", "INTERNAL", LocalDate.of(2026, 1, 1)));
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

Run: `mvn test -Dtest=EmployeeTypeCatalogValidatorTest -q`
Expected: COMPILATION ERROR — `EmployeeTypeCatalogValidator` does not exist yet

- [ ] **Step 3: Commit the test**

```bash
git add src/test/java/com/b4rrhh/employee/employee/application/service/EmployeeTypeCatalogValidatorTest.java
git commit -m "test(employee-type): write failing tests for EmployeeTypeCatalogValidator"
```

---

## Task 3: `EmployeeTypeCatalogValidator` — make tests pass

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/employee/application/service/EmployeeTypeCatalogValidator.java`

- [ ] **Step 1: Implement the validator**

```java
package com.b4rrhh.employee.employee.application.service;

import com.b4rrhh.employee.employee.domain.exception.EmployeeTypeInvalidException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class EmployeeTypeCatalogValidator {

    private final RuleEntityRepository ruleEntityRepository;

    public EmployeeTypeCatalogValidator(RuleEntityRepository ruleEntityRepository) {
        this.ruleEntityRepository = ruleEntityRepository;
    }

    public void validateEmployeeTypeCode(
            String ruleSystemCode,
            String employeeTypeCode,
            LocalDate referenceDate
    ) {
        RuleEntity ruleEntity = ruleEntityRepository
                .findByBusinessKey(ruleSystemCode, "EMPLOYEE_TYPE", employeeTypeCode)
                .orElseThrow(() -> new EmployeeTypeInvalidException(employeeTypeCode));

        if (!ruleEntity.isActive() || !isDateApplicable(ruleEntity, referenceDate)) {
            throw new EmployeeTypeInvalidException(employeeTypeCode);
        }
    }

    private boolean isDateApplicable(RuleEntity ruleEntity, LocalDate referenceDate) {
        if (referenceDate == null) return true;
        boolean startsBeforeOrOnDate = !referenceDate.isBefore(ruleEntity.getStartDate());
        boolean endsAfterOrOnDate = ruleEntity.getEndDate() == null || !referenceDate.isAfter(ruleEntity.getEndDate());
        return startsBeforeOrOnDate && endsAfterOrOnDate;
    }
}
```

- [ ] **Step 2: Run tests to confirm they pass**

Run: `mvn test -Dtest=EmployeeTypeCatalogValidatorTest -q`
Expected: BUILD SUCCESS, 3 tests passing

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/employee/application/service/EmployeeTypeCatalogValidator.java
git commit -m "feat(employee-type): implement EmployeeTypeCatalogValidator"
```

---

## Task 4: Wire `EmployeeTypeCatalogValidator` into `HireEmployeeService`

**Files:**
- Modify: `src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/HireEmployeeServiceTest.java`
- Modify: `src/main/java/com/b4rrhh/employee/lifecycle/application/usecase/HireEmployeeService.java`

### Step 1 — Add mock and new test in `HireEmployeeServiceTest`

- [ ] **Add `@Mock EmployeeTypeCatalogValidator` field** (after the existing `WorkCenterCompanyLookupPort` mock):

```java
@Mock
private EmployeeTypeCatalogValidator employeeTypeCatalogValidator;
```

Also add this import at the top:
```java
import com.b4rrhh.employee.employee.application.service.EmployeeTypeCatalogValidator;
import com.b4rrhh.employee.employee.domain.exception.EmployeeTypeInvalidException;
import static org.mockito.Mockito.doThrow;
```

- [ ] **Update `setUp()` to add the 10th constructor param:**

Replace:
```java
        service = new HireEmployeeService(
                employeeRepository,
                createEmployeeUseCase,
                createPresenceUseCase,
                createLaborClassificationUseCase,
                createContractUseCase,
                createWorkCenterUseCase,
                createCostCenterDistributionUseCase,
                createWorkingTimeUseCase,
                workCenterCompanyValidator
        );
```
With:
```java
        service = new HireEmployeeService(
                employeeRepository,
                createEmployeeUseCase,
                createPresenceUseCase,
                createLaborClassificationUseCase,
                createContractUseCase,
                createWorkCenterUseCase,
                createCostCenterDistributionUseCase,
                createWorkingTimeUseCase,
                workCenterCompanyValidator,
                employeeTypeCatalogValidator
        );
```

- [ ] **Add failing test for new validation:**

Add this test method at the end of the class (before the closing `}`):
```java
    @Test
    void mapsInvalidEmployeeTypeToLifecycleException() {
        HireEmployeeCommand command = validCommand();
        doThrow(new EmployeeTypeInvalidException("INTERNAL"))
                .when(employeeTypeCatalogValidator)
                .validateEmployeeTypeCode("ESP", "INTERNAL", LocalDate.of(2026, 3, 23));

        assertThrows(HireEmployeeCatalogValueInvalidException.class, () -> service.hire(command));
        verify(createEmployeeUseCase, never()).create(any(CreateEmployeeCommand.class));
    }
```

- [ ] **Run to confirm new test fails (service constructor won't compile yet):**

Run: `mvn test -Dtest=HireEmployeeServiceTest -q`
Expected: COMPILATION ERROR — `HireEmployeeService` constructor has no 10th param yet

### Step 2 — Wire into `HireEmployeeService`

- [ ] **Add import and field** to `HireEmployeeService.java`:

Add import:
```java
import com.b4rrhh.employee.employee.application.service.EmployeeTypeCatalogValidator;
import com.b4rrhh.employee.employee.domain.exception.EmployeeTypeInvalidException;
```

Add field after `private final WorkCenterCompanyValidator workCenterCompanyValidator;` (line 69):
```java
    private final EmployeeTypeCatalogValidator employeeTypeCatalogValidator;
```

- [ ] **Add constructor parameter and assignment:**

Replace the constructor signature ending:
```java
            WorkCenterCompanyValidator workCenterCompanyValidator
    ) {
        ...
        this.workCenterCompanyValidator = workCenterCompanyValidator;
    }
```
With:
```java
            WorkCenterCompanyValidator workCenterCompanyValidator,
            EmployeeTypeCatalogValidator employeeTypeCatalogValidator
    ) {
        ...
        this.workCenterCompanyValidator = workCenterCompanyValidator;
        this.employeeTypeCatalogValidator = employeeTypeCatalogValidator;
    }
```

- [ ] **Add validation call** after the last `normalizeRequiredWorkingTime` line and before the duplicate check (`if (employeeRepository.findByRuleSystem...`):

Insert between those two existing lines:
```java
        employeeTypeCatalogValidator.validateEmployeeTypeCode(ruleSystemCode, employeeTypeCode, hireDate);
```

- [ ] **Add `EmployeeTypeInvalidException` to catch block:**

Change:
```java
        } catch (PresenceCatalogValueInvalidException
                 | LaborClassificationAgreementInvalidException
                 | LaborClassificationCategoryInvalidException
                 | ContractInvalidException
                 | ContractSubtypeInvalidException
                 | WorkCenterCatalogValueInvalidException
                 | CostCenterCatalogValueInvalidException
                 | CostCenterDistributionInvalidException ex) {
            throw new HireEmployeeCatalogValueInvalidException(ex.getMessage(), ex);
```
To:
```java
        } catch (EmployeeTypeInvalidException
                 | PresenceCatalogValueInvalidException
                 | LaborClassificationAgreementInvalidException
                 | LaborClassificationCategoryInvalidException
                 | ContractInvalidException
                 | ContractSubtypeInvalidException
                 | WorkCenterCatalogValueInvalidException
                 | CostCenterCatalogValueInvalidException
                 | CostCenterDistributionInvalidException ex) {
            throw new HireEmployeeCatalogValueInvalidException(ex.getMessage(), ex);
```

- [ ] **Run all HireEmployeeService tests to confirm they pass:**

Run: `mvn test -Dtest=HireEmployeeServiceTest -q`
Expected: BUILD SUCCESS, all tests passing (including the new `mapsInvalidEmployeeTypeToLifecycleException`)

- [ ] **Commit:**

```bash
git add src/main/java/com/b4rrhh/employee/lifecycle/application/usecase/HireEmployeeService.java \
        src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/HireEmployeeServiceTest.java
git commit -m "feat(employee-type): wire EmployeeTypeCatalogValidator into HireEmployeeService"
```

---

## Task 5: Wire `EmployeeTypeCatalogValidator` into `RehireEmployeeService`

**Files:**
- Modify: `src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/RehireEmployeeServiceTest.java`
- Modify: `src/main/java/com/b4rrhh/employee/lifecycle/application/usecase/RehireEmployeeService.java`

### Step 1 — Add mock and new test in `RehireEmployeeServiceTest`

- [ ] **Add `@Mock EmployeeTypeCatalogValidator` field** after the `WorkCenterCompanyLookupPort` mock:

```java
    @Mock
    private EmployeeTypeCatalogValidator employeeTypeCatalogValidator;
```

Add imports:
```java
import com.b4rrhh.employee.employee.application.service.EmployeeTypeCatalogValidator;
import com.b4rrhh.employee.employee.domain.exception.EmployeeTypeInvalidException;
import static org.mockito.Mockito.doThrow;
```

- [ ] **Update `setUp()` to add the 15th constructor param:**

Replace:
```java
        service = new RehireEmployeeService(
                getEmployeeByBusinessKeyUseCase,
                employeeRepository,
                listEmployeePresencesUseCase,
                listEmployeeContractsUseCase,
                listEmployeeLaborClassificationsUseCase,
                listEmployeeWorkCentersUseCase,
                listEmployeeWorkingTimesUseCase,
                createPresenceUseCase,
                createLaborClassificationUseCase,
                createContractUseCase,
                createWorkCenterUseCase,
                createCostCenterDistributionUseCase,
                createWorkingTimeUseCase,
                workCenterCompanyValidator
        );
```
With:
```java
        service = new RehireEmployeeService(
                getEmployeeByBusinessKeyUseCase,
                employeeRepository,
                listEmployeePresencesUseCase,
                listEmployeeContractsUseCase,
                listEmployeeLaborClassificationsUseCase,
                listEmployeeWorkCentersUseCase,
                listEmployeeWorkingTimesUseCase,
                createPresenceUseCase,
                createLaborClassificationUseCase,
                createContractUseCase,
                createWorkCenterUseCase,
                createCostCenterDistributionUseCase,
                createWorkingTimeUseCase,
                workCenterCompanyValidator,
                employeeTypeCatalogValidator
        );
```

- [ ] **Add failing test for new validation** (add at the end of the class):

```java
    @Test
    void mapsInvalidEmployeeTypeToLifecycleException() {
        RehireEmployeeCommand command = validCommand();
        doThrow(new EmployeeTypeInvalidException("INTERNAL"))
                .when(employeeTypeCatalogValidator)
                .validateEmployeeTypeCode("ESP", "INTERNAL", command.rehireDate());

        assertThrows(RehireEmployeeCatalogValueInvalidException.class, () -> service.rehire(command));
    }
```

Also add import:
```java
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeCatalogValueInvalidException;
```

- [ ] **Run to confirm new test fails:**

Run: `mvn test -Dtest=RehireEmployeeServiceTest -q`
Expected: COMPILATION ERROR — constructor has no 15th param yet

### Step 2 — Wire into `RehireEmployeeService`

- [ ] **Add import and field** to `RehireEmployeeService.java`:

Add imports:
```java
import com.b4rrhh.employee.employee.application.service.EmployeeTypeCatalogValidator;
import com.b4rrhh.employee.employee.domain.exception.EmployeeTypeInvalidException;
```

Add field after `private final WorkCenterCompanyValidator workCenterCompanyValidator;` (line 99):
```java
    private final EmployeeTypeCatalogValidator employeeTypeCatalogValidator;
```

- [ ] **Add constructor parameter and assignment:**

Change the constructor signature end from:
```java
            WorkCenterCompanyValidator workCenterCompanyValidator
    ) {
        ...
        this.workCenterCompanyValidator = workCenterCompanyValidator;
    }
```
To:
```java
            WorkCenterCompanyValidator workCenterCompanyValidator,
            EmployeeTypeCatalogValidator employeeTypeCatalogValidator
    ) {
        ...
        this.workCenterCompanyValidator = workCenterCompanyValidator;
        this.employeeTypeCatalogValidator = employeeTypeCatalogValidator;
    }
```

- [ ] **Add validation call** after `normalizeRequiredWorkingTime` (line 151) and before `getEmployeeByBusinessKeyUseCase.getByBusinessKey` (line 154):

Insert between those two existing lines:
```java
        employeeTypeCatalogValidator.validateEmployeeTypeCode(ruleSystemCode, employeeTypeCode, rehireDate);
```

- [ ] **Add `EmployeeTypeInvalidException` to catch block:**

Change:
```java
        } catch (PresenceCatalogValueInvalidException
                 | LaborClassificationAgreementInvalidException
                 | LaborClassificationCategoryInvalidException
                 | ContractInvalidException
                 | ContractSubtypeInvalidException
                 | WorkCenterCatalogValueInvalidException
                 | CostCenterCatalogValueInvalidException ex) {
            throw new RehireEmployeeCatalogValueInvalidException(ex.getMessage(), ex);
```
To:
```java
        } catch (EmployeeTypeInvalidException
                 | PresenceCatalogValueInvalidException
                 | LaborClassificationAgreementInvalidException
                 | LaborClassificationCategoryInvalidException
                 | ContractInvalidException
                 | ContractSubtypeInvalidException
                 | WorkCenterCatalogValueInvalidException
                 | CostCenterCatalogValueInvalidException ex) {
            throw new RehireEmployeeCatalogValueInvalidException(ex.getMessage(), ex);
```

- [ ] **Run all RehireEmployeeService tests:**

Run: `mvn test -Dtest=RehireEmployeeServiceTest -q`
Expected: BUILD SUCCESS, all tests passing

- [ ] **Commit:**

```bash
git add src/main/java/com/b4rrhh/employee/lifecycle/application/usecase/RehireEmployeeService.java \
        src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/RehireEmployeeServiceTest.java
git commit -m "feat(employee-type): wire EmployeeTypeCatalogValidator into RehireEmployeeService"
```

---

## Task 6: Change default `"EMP"` → `"INTERNAL"` and update `HireEmployeeDefaultingTest`

**Files:**
- Modify: `src/main/java/com/b4rrhh/employee/lifecycle/application/model/HireEmployeeDefaultValues.java`
- Modify: `src/test/java/com/b4rrhh/employee/lifecycle/infrastructure/rest/HireEmployeeDefaultingTest.java`

- [ ] **Step 1: Update `HireEmployeeDefaultValues`**

Change:
```java
    public static final String DEFAULT_EMPLOYEE_TYPE_CODE = "EMP";
```
To:
```java
    public static final String DEFAULT_EMPLOYEE_TYPE_CODE = "INTERNAL";
```

- [ ] **Step 2: Rewrite `HireEmployeeDefaultingTest`**

Replace the entire file content with:

```java
package com.b4rrhh.employee.lifecycle.infrastructure.rest;

import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireEmployeeResult;
import com.b4rrhh.employee.lifecycle.application.usecase.HireEmployeeUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HireEmployeeDefaultingTest {

    private static final String REQUIRED_HIRE_BLOCKS = """
                            "hireDate": "2026-03-23",
                            "companyCode": "COMP",
                            "entryReasonCode": "HIRE",
                            "workCenterCode": "WC1",
                            "laborClassification": {
                                    "agreementCode": "AGR",
                                    "agreementCategoryCode": "CAT"
                            },
                            "contract": {
                                    "contractTypeCode": "CON",
                                    "contractSubtypeCode": "SUB"
                            },
                            "workingTime": {
                                    "workingTimePercentage": 75
                            }
                        """;

    @Mock
    private HireEmployeeUseCase hireEmployeeUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HireEmployeeController controller = new HireEmployeeController(hireEmployeeUseCase, new HireEmployeeWebMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private HireEmployeeResult createMockResult(String rs, String type, String num) {
        LocalDate hireDate = LocalDate.now();
        return new HireEmployeeResult(
                new HireEmployeeResult.EmployeeSummary(rs, type, num, "Ana", "Lopez", null, null, "Ana Lopez", "ACTIVE", hireDate),
                new HireEmployeeResult.PresenceSummary(1, hireDate, "COMP", "HIRE"),
                new HireEmployeeResult.WorkCenterSummary(hireDate, "WC1", "WC1"),
                null,
                new HireEmployeeResult.ContractSummary(hireDate, "CON", "SUB"),
                new HireEmployeeResult.LaborClassificationSummary(hireDate, "AGR", "CAT"),
                new HireEmployeeResult.WorkingTimeSummary(
                        1,
                        new BigDecimal("75"),
                        new BigDecimal("30.00"),
                        new BigDecimal("6.00"),
                        new BigDecimal("125.00"),
                        hireDate,
                        null
                )
        );
    }

    @Test
    void hireWithExplicitInternalUsesProvidedValue() throws Exception {
        when(hireEmployeeUseCase.hire(any(HireEmployeeCommand.class)))
                .thenReturn(createMockResult("ESP", "INTERNAL", "E001"));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "employeeNumber": "E001",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                """ + REQUIRED_HIRE_BLOCKS + """
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<HireEmployeeCommand> captor = ArgumentCaptor.forClass(HireEmployeeCommand.class);
        verify(hireEmployeeUseCase).hire(captor.capture());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
    }

    @Test
    void hireWithoutEmployeeTypeCodeDefaultsToInternal() throws Exception {
        when(hireEmployeeUseCase.hire(any(HireEmployeeCommand.class)))
                .thenReturn(createMockResult("ESP", "INTERNAL", "E002"));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeNumber": "E002",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                """ + REQUIRED_HIRE_BLOCKS + """
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<HireEmployeeCommand> captor = ArgumentCaptor.forClass(HireEmployeeCommand.class);
        verify(hireEmployeeUseCase).hire(captor.capture());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
    }

    @Test
    void hireWithBlankEmployeeTypeCodeDefaultsToInternal() throws Exception {
        when(hireEmployeeUseCase.hire(any(HireEmployeeCommand.class)))
                .thenReturn(createMockResult("ESP", "INTERNAL", "E003"));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "   ",
                                  "employeeNumber": "E003",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                """ + REQUIRED_HIRE_BLOCKS + """
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<HireEmployeeCommand> captor = ArgumentCaptor.forClass(HireEmployeeCommand.class);
        verify(hireEmployeeUseCase).hire(captor.capture());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
    }

    @Test
    void hireWithExplicitExternalValuePreservesIt() throws Exception {
        when(hireEmployeeUseCase.hire(any(HireEmployeeCommand.class)))
                .thenReturn(createMockResult("ESP", "EXTERNAL", "E004"));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "EXTERNAL",
                                  "employeeNumber": "E004",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                """ + REQUIRED_HIRE_BLOCKS + """
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<HireEmployeeCommand> captor = ArgumentCaptor.forClass(HireEmployeeCommand.class);
        verify(hireEmployeeUseCase).hire(captor.capture());
        assertEquals("EXTERNAL", captor.getValue().employeeTypeCode());
    }

    @Test
    void hireWithLowercaseValueNormalizesToUppercase() throws Exception {
        when(hireEmployeeUseCase.hire(any(HireEmployeeCommand.class)))
                .thenReturn(createMockResult("ESP", "INTERNAL", "E005"));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "esp",
                                  "employeeTypeCode": "internal",
                                  "employeeNumber": "E005",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                """ + REQUIRED_HIRE_BLOCKS + """
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<HireEmployeeCommand> captor = ArgumentCaptor.forClass(HireEmployeeCommand.class);
        verify(hireEmployeeUseCase).hire(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
    }
}
```

- [ ] **Step 3: Run the updated tests**

Run: `mvn test -Dtest=HireEmployeeDefaultingTest -q`
Expected: BUILD SUCCESS, 5 tests passing

- [ ] **Step 4: Run the full test suite to check for regressions**

Run: `mvn test -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/lifecycle/application/model/HireEmployeeDefaultValues.java \
        src/test/java/com/b4rrhh/employee/lifecycle/infrastructure/rest/HireEmployeeDefaultingTest.java
git commit -m "feat(employee-type): change default employeeTypeCode from EMP to INTERNAL"
```

---

## Task 7: V93 migration — register `employeeTypeCode` in catalog binding

**Files:**
- Create: `src/main/resources/db/migration/V93__seed_employee_type_catalog_binding.sql`

- [ ] **Step 1: Create the migration**

```sql
-- =========================================================
-- V93__seed_employee_type_catalog_binding.sql
-- Registers employeeTypeCode as a catalog-backed field in the
-- resource_field_catalog_binding metamodel (ADR-015).
-- EMPLOYEE_TYPE rule_entity_type is seeded at V49.
-- =========================================================

insert into rulesystem.resource_field_catalog_binding (
    resource_code,
    field_code,
    rule_entity_type_code,
    catalog_kind,
    depends_on_field_code,
    custom_resolver_code,
    active
)
values ('employee', 'employeeTypeCode', 'EMPLOYEE_TYPE', 'DIRECT', null, null, true)
on conflict (resource_code, field_code) do nothing;
```

- [ ] **Step 2: Run all tests (Flyway runs V93 on H2)**

Run: `mvn test -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V93__seed_employee_type_catalog_binding.sql
git commit -m "feat(employee-type): V93 — seed resource_field_catalog_binding for employeeTypeCode"
```

---

## Task 8: Frontend — change default and update test specs

**Files:**
- Modify: `b4rrhh_frontend/src/app/features/employee/models/hire-employee.defaults.ts`
- Modify: `b4rrhh_frontend/src/app/features/employee/data-access/employee-hiring.mapper.spec.ts`
- Modify: `b4rrhh_frontend/src/app/features/employee/data-access/employee-rehire.mapper.spec.ts`
- Modify: `b4rrhh_frontend/src/app/features/employee/data-access/employee-rehire.store.spec.ts`
- Modify: `b4rrhh_frontend/src/app/features/employee/lifecycle/hire/pages/hire-employee-page.component.spec.ts`
- Modify: `b4rrhh_frontend/src/app/features/employee/lifecycle/rehire/pages/rehire-employee-page.component.spec.ts`
- Plus any other `*.spec.ts` referencing `employeeTypeCode: 'EMP'` (run the grep below to confirm the full list)

> Note: Nomina/payroll specs that use `'EMP'` as a generic fixture unrelated to hire flow do NOT need to change.

- [ ] **Step 1: Update the frontend default**

In `b4rrhh_frontend/src/app/features/employee/models/hire-employee.defaults.ts`, change:
```typescript
export const HIRE_EMPLOYEE_DEFAULTS = {
  employeeTypeCode: 'EMP',
} as const;
```
To:
```typescript
export const HIRE_EMPLOYEE_DEFAULTS = {
  employeeTypeCode: 'INTERNAL',
} as const;
```

- [ ] **Step 2: Find all frontend test specs using `'EMP'` as employee type**

Run from `b4rrhh_frontend/`:
```bash
grep -rn "employeeTypeCode.*'EMP'\|'EMP'.*employeeTypeCode" src/ --include="*.spec.ts"
```
Expected: several hits across the lifecycle/mapper/section specs listed above. Nomina specs (recibos.*) may also appear — skip those.

- [ ] **Step 3: Replace `'EMP'` with `'INTERNAL'` in all affected employee lifecycle specs**

Run from `b4rrhh_frontend/`:
```bash
# Replace employeeTypeCode: 'EMP' with employeeTypeCode: 'INTERNAL' in affected specs
# (run this for each file found in step 2 that is NOT a nomina/payroll spec)
sed -i "s/employeeTypeCode: 'EMP'/employeeTypeCode: 'INTERNAL'/g" \
  src/app/features/employee/data-access/employee-hiring.mapper.spec.ts \
  src/app/features/employee/data-access/employee-rehire.mapper.spec.ts \
  src/app/features/employee/data-access/employee-rehire.store.spec.ts \
  src/app/features/employee/lifecycle/hire/pages/hire-employee-page.component.spec.ts \
  src/app/features/employee/lifecycle/rehire/pages/rehire-employee-page.component.spec.ts
```

Then run the grep again to confirm no remaining hits in those files (and to discover any files missed):
```bash
grep -rn "employeeTypeCode.*'EMP'" src/ --include="*.spec.ts"
```

For any remaining hit files that are employee-related (not payroll), apply the same sed replacement.

- [ ] **Step 4: Run frontend tests**

Run from `b4rrhh_frontend/`:
```bash
npm run test
```
Expected: all tests passing

- [ ] **Step 5: Commit**

```bash
git add src/app/features/employee/models/hire-employee.defaults.ts \
        src/app/features/employee/data-access/employee-hiring.mapper.spec.ts \
        src/app/features/employee/data-access/employee-rehire.mapper.spec.ts \
        src/app/features/employee/data-access/employee-rehire.store.spec.ts \
        src/app/features/employee/lifecycle/hire/pages/hire-employee-page.component.spec.ts \
        src/app/features/employee/lifecycle/rehire/pages/rehire-employee-page.component.spec.ts
git commit -m "feat(employee-type): change frontend default employeeTypeCode from EMP to INTERNAL"
```

---

## Final verification

- [ ] **Run full backend test suite**

Run: `mvn test -q`
Expected: BUILD SUCCESS

- [ ] **Run full frontend test suite**

Run from `b4rrhh_frontend/`: `npm run test`
Expected: all tests passing

---

## Error propagation summary

| Exception | Catch block wraps it in | HTTP response |
|-----------|-------------------------|---------------|
| `EmployeeTypeInvalidException` (hire) | `HireEmployeeCatalogValueInvalidException` | 422 `INVALID_CATALOG_VALUE` |
| `EmployeeTypeInvalidException` (rehire) | `RehireEmployeeCatalogValueInvalidException` | 422 `INVALID_CATALOG_VALUE` |
