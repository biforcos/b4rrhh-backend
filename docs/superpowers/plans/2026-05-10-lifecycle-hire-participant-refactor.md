# Hire Lifecycle — Participant Pattern Refactor (ADR-047 Phase 1)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor `HireEmployeeService` so that adding a new vertical to the hire workflow is a local operation — create one `@Component` file, not modify the central service.

**Architecture:** `HireParticipant` is a secondary port; each vertical provides one implementation. `HireEmployeePreConditionValidator` absorbs all normalization/validation. `HireContext` is a mutable bag that flows through all participants. Single `@Transactional` is preserved; exception mapping is preserved.

**Tech Stack:** Java 21, Spring Boot, Mockito/JUnit 5. No new libraries. Tests run with `mvn test`.

---

## File Structure

### New files
```
src/main/java/com/b4rrhh/employee/lifecycle/application/port/HireParticipant.java
src/main/java/com/b4rrhh/employee/lifecycle/application/model/HireContext.java
src/main/java/com/b4rrhh/employee/lifecycle/application/service/HireEmployeePreConditionValidator.java
src/main/java/com/b4rrhh/employee/lifecycle/application/participant/EmployeeCoreParticipant.java
src/main/java/com/b4rrhh/employee/lifecycle/application/participant/PresenceParticipant.java
src/main/java/com/b4rrhh/employee/lifecycle/application/participant/WorkCenterParticipant.java
src/main/java/com/b4rrhh/employee/lifecycle/application/participant/CostCenterParticipant.java
src/main/java/com/b4rrhh/employee/lifecycle/application/participant/ContractParticipant.java
src/main/java/com/b4rrhh/employee/lifecycle/application/participant/LaborClassificationParticipant.java
src/main/java/com/b4rrhh/employee/lifecycle/application/participant/WorkingTimeParticipant.java

src/test/java/com/b4rrhh/employee/lifecycle/application/service/HireEmployeePreConditionValidatorTest.java
src/test/java/com/b4rrhh/employee/lifecycle/application/participant/EmployeeCoreParticipantTest.java
src/test/java/com/b4rrhh/employee/lifecycle/application/participant/PresenceParticipantTest.java
src/test/java/com/b4rrhh/employee/lifecycle/application/participant/WorkCenterParticipantTest.java
src/test/java/com/b4rrhh/employee/lifecycle/application/participant/CostCenterParticipantTest.java
src/test/java/com/b4rrhh/employee/lifecycle/application/participant/ContractParticipantTest.java
src/test/java/com/b4rrhh/employee/lifecycle/application/participant/LaborClassificationParticipantTest.java
src/test/java/com/b4rrhh/employee/lifecycle/application/participant/WorkingTimeParticipantTest.java
```

### Modified files
```
src/main/java/com/b4rrhh/employee/lifecycle/application/usecase/HireEmployeeService.java  (full rewrite)
src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/HireEmployeeServiceTest.java  (full rewrite)
```

---

## Reference — context helpers used across multiple tasks

Use these helpers wherever needed. They are defined in `HireEmployeePreConditionValidator` and are used in the test tasks for `HireContext` construction:

```java
// Normalized ruleSystemCode
"ESP"

// Standard valid HireContext fields (copy this into tests where a HireContext is needed)
new HireContext(
    "ESP", "INTERNAL", "Ana", "Lopez", null, "Ani",
    LocalDate.of(2026, 3, 23),
    "COMP", "HIRE", "WC1",
    new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
    new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
    null,
    new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
)
// then ctx.setEmployeeNumber("EMP000001")
```

---

## Task 1: HireParticipant port + HireContext model

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/port/HireParticipant.java`
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/model/HireContext.java`

These are data-only structures. No test class needed — they compile or they don't. Confirm compilation via `mvn compile`.

- [ ] **Step 1: Create HireParticipant**

```java
package com.b4rrhh.employee.lifecycle.application.port;

import com.b4rrhh.employee.lifecycle.application.model.HireContext;

public interface HireParticipant {
    // Execution order. 10=EmployeeCore, 20=Presence, 30=WorkCenter, 40=CostCenter,
    // 50=Contract, 60=LaborClassification, 70=WorkingTime
    int order();
    void participate(HireContext ctx);
}
```

- [ ] **Step 2: Create HireContext**

```java
package com.b4rrhh.employee.lifecycle.application.model;

import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;

public class HireContext {

    // Normalized inputs — set once by HireEmployeePreConditionValidator
    private final String ruleSystemCode;
    private final String employeeTypeCode;
    private final String firstName;
    private final String lastName1;
    private final String lastName2;
    private final String preferredName;
    private final LocalDate hireDate;
    private final String companyCode;
    private final String entryReasonCode;
    private final String workCenterCode;
    private final HireEmployeeCommand.HireEmployeeContractCommand contract;
    private final HireEmployeeCommand.HireEmployeeLaborClassificationCommand laborClassification;
    private final HireEmployeeCommand.HireEmployeeCostCenterDistributionCommand costCenterDistribution;
    private final HireEmployeeCommand.HireEmployeeWorkingTimeCommand workingTime;

    // Set after nextEmployeeNumberPort.consumeNext()
    private String employeeNumber;

    // Participant results — written by each participant in order
    private Employee employee;
    private Presence presence;
    private WorkCenter workCenter;
    private CostCenterDistributionWindow costCenter;
    private Contract contractResult;
    private LaborClassification laborClassificationResult;
    private WorkingTime workingTimeResult;

    public HireContext(
            String ruleSystemCode, String employeeTypeCode,
            String firstName, String lastName1, String lastName2, String preferredName,
            LocalDate hireDate, String companyCode, String entryReasonCode, String workCenterCode,
            HireEmployeeCommand.HireEmployeeContractCommand contract,
            HireEmployeeCommand.HireEmployeeLaborClassificationCommand laborClassification,
            HireEmployeeCommand.HireEmployeeCostCenterDistributionCommand costCenterDistribution,
            HireEmployeeCommand.HireEmployeeWorkingTimeCommand workingTime) {
        this.ruleSystemCode = ruleSystemCode;
        this.employeeTypeCode = employeeTypeCode;
        this.firstName = firstName;
        this.lastName1 = lastName1;
        this.lastName2 = lastName2;
        this.preferredName = preferredName;
        this.hireDate = hireDate;
        this.companyCode = companyCode;
        this.entryReasonCode = entryReasonCode;
        this.workCenterCode = workCenterCode;
        this.contract = contract;
        this.laborClassification = laborClassification;
        this.costCenterDistribution = costCenterDistribution;
        this.workingTime = workingTime;
    }

    // Accessors for normalized inputs
    public String ruleSystemCode() { return ruleSystemCode; }
    public String employeeTypeCode() { return employeeTypeCode; }
    public String firstName() { return firstName; }
    public String lastName1() { return lastName1; }
    public String lastName2() { return lastName2; }
    public String preferredName() { return preferredName; }
    public LocalDate hireDate() { return hireDate; }
    public String companyCode() { return companyCode; }
    public String entryReasonCode() { return entryReasonCode; }
    public String workCenterCode() { return workCenterCode; }
    public HireEmployeeCommand.HireEmployeeContractCommand contract() { return contract; }
    public HireEmployeeCommand.HireEmployeeLaborClassificationCommand laborClassification() { return laborClassification; }
    public HireEmployeeCommand.HireEmployeeCostCenterDistributionCommand costCenterDistribution() { return costCenterDistribution; }
    public HireEmployeeCommand.HireEmployeeWorkingTimeCommand workingTime() { return workingTime; }

    public String employeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }

    public Employee employee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public Presence presence() { return presence; }
    public void setPresence(Presence presence) { this.presence = presence; }
    public WorkCenter workCenter() { return workCenter; }
    public void setWorkCenter(WorkCenter workCenter) { this.workCenter = workCenter; }
    public CostCenterDistributionWindow costCenter() { return costCenter; }
    public void setCostCenter(CostCenterDistributionWindow costCenter) { this.costCenter = costCenter; }
    public Contract contractResult() { return contractResult; }
    public void setContractResult(Contract contractResult) { this.contractResult = contractResult; }
    public LaborClassification laborClassificationResult() { return laborClassificationResult; }
    public void setLaborClassificationResult(LaborClassification laborClassificationResult) { this.laborClassificationResult = laborClassificationResult; }
    public WorkingTime workingTimeResult() { return workingTimeResult; }
    public void setWorkingTimeResult(WorkingTime workingTimeResult) { this.workingTimeResult = workingTimeResult; }

    public HireEmployeeResult toResult() {
        return new HireEmployeeResult(
                new HireEmployeeResult.EmployeeSummary(
                        employee.getRuleSystemCode(),
                        employee.getEmployeeTypeCode(),
                        employee.getEmployeeNumber(),
                        employee.getFirstName(),
                        employee.getLastName1(),
                        employee.getLastName2(),
                        employee.getPreferredName(),
                        formatDisplayName(employee),
                        employee.getStatus(),
                        hireDate
                ),
                new HireEmployeeResult.PresenceSummary(
                        presence.getPresenceNumber(),
                        presence.getStartDate(),
                        presence.getCompanyCode(),
                        presence.getEntryReasonCode()
                ),
                new HireEmployeeResult.WorkCenterSummary(
                        workCenter.getStartDate(),
                        workCenter.getWorkCenterCode(),
                        workCenter.getWorkCenterCode()
                ),
                costCenter != null ? new HireEmployeeResult.CostCenterSummary(
                        costCenter.getStartDate(),
                        costCenter.getTotalAllocationPercentage().doubleValue(),
                        costCenter.getItems().stream()
                                .map(item -> new HireEmployeeResult.CostCenterItemSummary(
                                        item.getCostCenterCode(),
                                        item.getCostCenterCode(),
                                        item.getAllocationPercentage().doubleValue()
                                ))
                                .collect(Collectors.toList())
                ) : null,
                new HireEmployeeResult.ContractSummary(
                        contractResult.getStartDate(),
                        contractResult.getContractCode(),
                        contractResult.getContractSubtypeCode()
                ),
                new HireEmployeeResult.LaborClassificationSummary(
                        laborClassificationResult.getStartDate(),
                        laborClassificationResult.getAgreementCode(),
                        laborClassificationResult.getAgreementCategoryCode()
                ),
                new HireEmployeeResult.WorkingTimeSummary(
                        workingTimeResult.getWorkingTimeNumber(),
                        workingTimeResult.getWorkingTimePercentage(),
                        workingTimeResult.getWeeklyHours(),
                        workingTimeResult.getDailyHours(),
                        workingTimeResult.getMonthlyHours(),
                        workingTimeResult.getStartDate(),
                        workingTimeResult.getEndDate()
                )
        );
    }

    private static String formatDisplayName(Employee employee) {
        StringBuilder sb = new StringBuilder()
                .append(employee.getFirstName())
                .append(" ")
                .append(employee.getLastName1());
        if (employee.getLastName2() != null && !employee.getLastName2().isEmpty()) {
            sb.append(" ").append(employee.getLastName2());
        }
        return sb.toString();
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -pl . -q`
Expected: BUILD SUCCESS (no errors)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/lifecycle/application/port/HireParticipant.java
git add src/main/java/com/b4rrhh/employee/lifecycle/application/model/HireContext.java
git commit -m "feat(lifecycle): add HireParticipant port and HireContext model (ADR-047)"
```

---

## Task 2: HireEmployeePreConditionValidator

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/service/HireEmployeePreConditionValidator.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/service/HireEmployeePreConditionValidatorTest.java`

This extracts all the normalization, required-field validation, and business-rule validation from `HireEmployeeService`. It returns a populated `HireContext` (without `employeeNumber`).

- [ ] **Step 1: Write the failing tests**

```java
package com.b4rrhh.employee.lifecycle.application.service;

import com.b4rrhh.employee.employee.application.service.EmployeeTypeCatalogValidator;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeRequestInvalidException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCompanyMismatchException;
import com.b4rrhh.employee.workcenter.domain.service.WorkCenterCompanyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HireEmployeePreConditionValidatorTest {

    @Mock
    private WorkCenterCompanyValidator workCenterCompanyValidator;
    @Mock
    private EmployeeTypeCatalogValidator employeeTypeCatalogValidator;

    private HireEmployeePreConditionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new HireEmployeePreConditionValidator(workCenterCompanyValidator, employeeTypeCatalogValidator);
    }

    @Test
    void returnsNormalizedContextForValidCommand() {
        HireContext ctx = validator.validateAndNormalize(validCommand());

        assertThat(ctx.ruleSystemCode()).isEqualTo("ESP");
        assertThat(ctx.employeeTypeCode()).isEqualTo("INTERNAL");
        assertThat(ctx.firstName()).isEqualTo("Ana");
        assertThat(ctx.lastName1()).isEqualTo("Lopez");
        assertThat(ctx.lastName2()).isNull();
        assertThat(ctx.preferredName()).isEqualTo("Ani");
        assertThat(ctx.companyCode()).isEqualTo("COMP");
        assertThat(ctx.entryReasonCode()).isEqualTo("HIRE");
        assertThat(ctx.workCenterCode()).isEqualTo("WC1");
        assertThat(ctx.hireDate()).isEqualTo(LocalDate.of(2026, 3, 23));
        assertThat(ctx.contract().contractTypeCode()).isEqualTo("CON");
        assertThat(ctx.laborClassification().agreementCode()).isEqualTo("AGR");
        assertThat(ctx.workingTime().workingTimePercentage()).isEqualByComparingTo("75");
        assertThat(ctx.employeeNumber()).isNull();
    }

    @Test
    void normalizesCodeFieldsToUpperCase() {
        HireEmployeeCommand command = new HireEmployeeCommand(
                "esp", "internal", "Ana", "Lopez", null, null,
                LocalDate.of(2026, 3, 23), "hire", "comp", "wc1",
                new HireEmployeeCommand.HireEmployeeContractCommand("con", "sub"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("agr", "cat"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );

        HireContext ctx = validator.validateAndNormalize(command);

        assertThat(ctx.ruleSystemCode()).isEqualTo("ESP");
        assertThat(ctx.employeeTypeCode()).isEqualTo("INTERNAL");
        assertThat(ctx.companyCode()).isEqualTo("COMP");
        assertThat(ctx.workCenterCode()).isEqualTo("WC1");
        assertThat(ctx.contract().contractTypeCode()).isEqualTo("CON");
        assertThat(ctx.laborClassification().agreementCode()).isEqualTo("AGR");
    }

    @Test
    void defaultsEmployeeTypeCodeWhenBlank() {
        HireEmployeeCommand command = new HireEmployeeCommand(
                "ESP", null, "Ana", "Lopez", null, null,
                LocalDate.of(2026, 3, 23), "HIRE", "COMP", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );

        HireContext ctx = validator.validateAndNormalize(command);

        assertThat(ctx.employeeTypeCode()).isEqualTo("INTERNAL"); // HireEmployeeDefaultValues.DEFAULT_EMPLOYEE_TYPE_CODE
    }

    @Test
    void throwsWhenCommandIsNull() {
        assertThatThrownBy(() -> validator.validateAndNormalize(null))
                .isInstanceOf(HireEmployeeRequestInvalidException.class);
    }

    @Test
    void throwsWhenRuleSystemCodeIsBlank() {
        HireEmployeeCommand command = new HireEmployeeCommand(
                "  ", "INTERNAL", "Ana", "Lopez", null, null,
                LocalDate.of(2026, 3, 23), "HIRE", "COMP", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );

        assertThatThrownBy(() -> validator.validateAndNormalize(command))
                .isInstanceOf(HireEmployeeRequestInvalidException.class)
                .hasMessageContaining("ruleSystemCode");
    }

    @Test
    void throwsWhenContractIsNull() {
        HireEmployeeCommand command = new HireEmployeeCommand(
                "ESP", "INTERNAL", "Ana", "Lopez", null, null,
                LocalDate.of(2026, 3, 23), "HIRE", "COMP", "WC1",
                null,
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );

        assertThatThrownBy(() -> validator.validateAndNormalize(command))
                .isInstanceOf(HireEmployeeRequestInvalidException.class)
                .hasMessageContaining("contract");
    }

    @Test
    void throwsWhenWorkCenterDoesNotBelongToCompany() {
        doThrow(new WorkCenterCompanyMismatchException("WC1", "COMP"))
                .when(workCenterCompanyValidator)
                .validateBelongsToCompany(eq("ESP"), eq("WC1"), eq("COMP"), any(LocalDate.class));

        assertThatThrownBy(() -> validator.validateAndNormalize(validCommand()))
                .isInstanceOf(WorkCenterCompanyMismatchException.class);
    }

    @Test
    void throwsWhenEmployeeTypeIsInvalid() {
        doThrow(new com.b4rrhh.employee.employee.domain.exception.EmployeeTypeInvalidException("INTERNAL", "ESP"))
                .when(employeeTypeCatalogValidator)
                .validateEmployeeTypeCode(eq("ESP"), eq("INTERNAL"), any(LocalDate.class));

        assertThatThrownBy(() -> validator.validateAndNormalize(validCommand()))
                .isInstanceOf(HireEmployeeCatalogValueInvalidException.class);
    }

    @Test
    void callsValidatorsWithNormalizedValues() {
        validator.validateAndNormalize(validCommand());

        verify(workCenterCompanyValidator)
                .validateBelongsToCompany("ESP", "WC1", "COMP", LocalDate.of(2026, 3, 23));
        verify(employeeTypeCatalogValidator)
                .validateEmployeeTypeCode("ESP", "INTERNAL", LocalDate.of(2026, 3, 23));
    }

    private HireEmployeeCommand validCommand() {
        return new HireEmployeeCommand(
                "ESP", "INTERNAL", "Ana", "Lopez", null, "Ani",
                LocalDate.of(2026, 3, 23), "HIRE", "COMP", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

Run: `mvn test -Dtest=HireEmployeePreConditionValidatorTest -q`
Expected: FAIL — `HireEmployeePreConditionValidator` does not exist yet

- [ ] **Step 3: Implement HireEmployeePreConditionValidator**

```java
package com.b4rrhh.employee.lifecycle.application.service;

import com.b4rrhh.employee.employee.application.service.EmployeeTypeCatalogValidator;
import com.b4rrhh.employee.employee.domain.exception.EmployeeTypeInvalidException;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.application.model.HireEmployeeDefaultValues;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeRequestInvalidException;
import com.b4rrhh.employee.workcenter.domain.service.WorkCenterCompanyValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class HireEmployeePreConditionValidator {

    private final WorkCenterCompanyValidator workCenterCompanyValidator;
    private final EmployeeTypeCatalogValidator employeeTypeCatalogValidator;

    public HireEmployeePreConditionValidator(
            WorkCenterCompanyValidator workCenterCompanyValidator,
            EmployeeTypeCatalogValidator employeeTypeCatalogValidator) {
        this.workCenterCompanyValidator = workCenterCompanyValidator;
        this.employeeTypeCatalogValidator = employeeTypeCatalogValidator;
    }

    public HireContext validateAndNormalize(HireEmployeeCommand command) {
        if (command == null) {
            throw new HireEmployeeRequestInvalidException("request body is required");
        }

        String ruleSystemCode = requireCode("ruleSystemCode", command.ruleSystemCode());
        String employeeTypeCode = resolveEmployeeTypeCode(command.employeeTypeCode());
        String firstName = requireText("firstName", command.firstName());
        String lastName1 = requireText("lastName1", command.lastName1());
        String lastName2 = normalizeOptionalText(command.lastName2());
        String preferredName = normalizeOptionalText(command.preferredName());
        LocalDate hireDate = requireDate(command.hireDate());

        String companyCode = requireCode("companyCode", command.companyCode());
        String entryReasonCode = requireCode("entryReasonCode", command.entryReasonCode());
        String workCenterCode = requireCode("workCenterCode", command.workCenterCode());

        HireEmployeeCommand.HireEmployeeContractCommand contract = requireContract(command.contract());
        HireEmployeeCommand.HireEmployeeLaborClassificationCommand laborClassification =
                requireLaborClassification(command.laborClassification());
        HireEmployeeCommand.HireEmployeeWorkingTimeCommand workingTime = requireWorkingTime(command.workingTime());

        workCenterCompanyValidator.validateBelongsToCompany(ruleSystemCode, workCenterCode, companyCode, hireDate);

        try {
            employeeTypeCatalogValidator.validateEmployeeTypeCode(ruleSystemCode, employeeTypeCode, hireDate);
        } catch (EmployeeTypeInvalidException ex) {
            throw new HireEmployeeCatalogValueInvalidException(ex.getMessage(), ex);
        }

        return new HireContext(ruleSystemCode, employeeTypeCode, firstName, lastName1, lastName2, preferredName,
                hireDate, companyCode, entryReasonCode, workCenterCode,
                contract, laborClassification, command.costCenterDistribution(), workingTime);
    }

    private String requireCode(String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new HireEmployeeRequestInvalidException(field + " is required");
        }
        return value.trim().toUpperCase();
    }

    private String requireText(String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new HireEmployeeRequestInvalidException(field + " is required");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    private LocalDate requireDate(LocalDate value) {
        if (value == null) {
            throw new HireEmployeeRequestInvalidException("hireDate is required");
        }
        return value;
    }

    private String resolveEmployeeTypeCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return HireEmployeeDefaultValues.DEFAULT_EMPLOYEE_TYPE_CODE;
        }
        return value.trim().toUpperCase();
    }

    private HireEmployeeCommand.HireEmployeeContractCommand requireContract(
            HireEmployeeCommand.HireEmployeeContractCommand contract) {
        if (contract == null) {
            throw new HireEmployeeRequestInvalidException("contract is required");
        }
        String subtypeCode = (contract.contractSubtypeCode() == null || contract.contractSubtypeCode().trim().isEmpty())
                ? null : contract.contractSubtypeCode().trim().toUpperCase();
        return new HireEmployeeCommand.HireEmployeeContractCommand(
                requireCode("contract.contractTypeCode", contract.contractTypeCode()),
                subtypeCode
        );
    }

    private HireEmployeeCommand.HireEmployeeLaborClassificationCommand requireLaborClassification(
            HireEmployeeCommand.HireEmployeeLaborClassificationCommand laborClassification) {
        if (laborClassification == null) {
            throw new HireEmployeeRequestInvalidException("laborClassification is required");
        }
        return new HireEmployeeCommand.HireEmployeeLaborClassificationCommand(
                requireCode("laborClassification.agreementCode", laborClassification.agreementCode()),
                requireCode("laborClassification.agreementCategoryCode", laborClassification.agreementCategoryCode())
        );
    }

    private HireEmployeeCommand.HireEmployeeWorkingTimeCommand requireWorkingTime(
            HireEmployeeCommand.HireEmployeeWorkingTimeCommand workingTime) {
        if (workingTime == null) {
            throw new HireEmployeeRequestInvalidException("workingTime is required");
        }
        if (workingTime.workingTimePercentage() == null) {
            throw new HireEmployeeRequestInvalidException("workingTime.workingTimePercentage is required");
        }
        return new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(
                workingTime.workingTimePercentage().stripTrailingZeros());
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

Run: `mvn test -Dtest=HireEmployeePreConditionValidatorTest -q`
Expected: PASS — all 8 tests green

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/lifecycle/application/service/HireEmployeePreConditionValidator.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/service/HireEmployeePreConditionValidatorTest.java
git commit -m "feat(lifecycle): add HireEmployeePreConditionValidator (ADR-047)"
```

---

## Task 3: EmployeeCoreParticipant (order = 10)

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/EmployeeCoreParticipant.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/EmployeeCoreParticipantTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.employee.application.usecase.CreateEmployeeCommand;
import com.b4rrhh.employee.employee.application.usecase.CreateEmployeeUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeCoreParticipantTest {

    @Mock
    private CreateEmployeeUseCase createEmployeeUseCase;

    @InjectMocks
    private EmployeeCoreParticipant participant;

    @Test
    void orderIs10() {
        assertThat(participant.order()).isEqualTo(10);
    }

    @Test
    void createsEmployeeFromContextAndStoresResult() {
        HireContext ctx = validContext();
        Employee employee = new Employee(
                100L, "ESP", "INTERNAL", "EMP000001", "Ana", "Lopez", null, "Ani", "ACTIVE",
                LocalDateTime.now(), LocalDateTime.now(), null
        );
        when(createEmployeeUseCase.create(any(CreateEmployeeCommand.class))).thenReturn(employee);

        participant.participate(ctx);

        assertThat(ctx.employee()).isSameAs(employee);

        ArgumentCaptor<CreateEmployeeCommand> captor = ArgumentCaptor.forClass(CreateEmployeeCommand.class);
        verify(createEmployeeUseCase).create(captor.capture());
        CreateEmployeeCommand cmd = captor.getValue();
        assertThat(cmd.ruleSystemCode()).isEqualTo("ESP");
        assertThat(cmd.employeeTypeCode()).isEqualTo("INTERNAL");
        assertThat(cmd.employeeNumber()).isEqualTo("EMP000001");
        assertThat(cmd.firstName()).isEqualTo("Ana");
        assertThat(cmd.lastName1()).isEqualTo("Lopez");
        assertThat(cmd.lastName2()).isNull();
        assertThat(cmd.preferredName()).isEqualTo("Ani");
    }

    private HireContext validContext() {
        HireContext ctx = new HireContext(
                "ESP", "INTERNAL", "Ana", "Lopez", null, "Ani",
                LocalDate.of(2026, 3, 23),
                "COMP", "HIRE", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );
        ctx.setEmployeeNumber("EMP000001");
        return ctx;
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

Run: `mvn test -Dtest=EmployeeCoreParticipantTest -q`
Expected: FAIL — `EmployeeCoreParticipant` does not exist

- [ ] **Step 3: Implement EmployeeCoreParticipant**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.employee.application.usecase.CreateEmployeeCommand;
import com.b4rrhh.employee.employee.application.usecase.CreateEmployeeUseCase;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.application.port.HireParticipant;
import org.springframework.stereotype.Component;

@Component
public class EmployeeCoreParticipant implements HireParticipant {

    private final CreateEmployeeUseCase createEmployeeUseCase;

    public EmployeeCoreParticipant(CreateEmployeeUseCase createEmployeeUseCase) {
        this.createEmployeeUseCase = createEmployeeUseCase;
    }

    @Override
    public int order() { return 10; }

    @Override
    public void participate(HireContext ctx) {
        ctx.setEmployee(createEmployeeUseCase.create(new CreateEmployeeCommand(
                ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                ctx.firstName(), ctx.lastName1(), ctx.lastName2(), ctx.preferredName()
        )));
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

Run: `mvn test -Dtest=EmployeeCoreParticipantTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/lifecycle/application/participant/EmployeeCoreParticipant.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/participant/EmployeeCoreParticipantTest.java
git commit -m "feat(lifecycle): add EmployeeCoreParticipant order=10 (ADR-047)"
```

---

## Task 4: PresenceParticipant (order = 20)

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/PresenceParticipant.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/PresenceParticipantTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.presence.application.usecase.CreatePresenceCommand;
import com.b4rrhh.employee.presence.application.usecase.CreatePresenceUseCase;
import com.b4rrhh.employee.presence.domain.exception.PresenceCatalogValueInvalidException;
import com.b4rrhh.employee.presence.domain.model.Presence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceParticipantTest {

    @Mock
    private CreatePresenceUseCase createPresenceUseCase;

    @InjectMocks
    private PresenceParticipant participant;

    @Test
    void orderIs20() {
        assertThat(participant.order()).isEqualTo(20);
    }

    @Test
    void createsPresenceFromContextAndStoresResult() {
        HireContext ctx = validContext();
        LocalDate hireDate = ctx.hireDate();
        Presence presence = new Presence(10L, 100L, 1, "COMP", "HIRE", null, hireDate, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(createPresenceUseCase.create(any(CreatePresenceCommand.class))).thenReturn(presence);

        participant.participate(ctx);

        assertThat(ctx.presence()).isSameAs(presence);

        ArgumentCaptor<CreatePresenceCommand> captor = ArgumentCaptor.forClass(CreatePresenceCommand.class);
        org.mockito.Mockito.verify(createPresenceUseCase).create(captor.capture());
        CreatePresenceCommand cmd = captor.getValue();
        assertThat(cmd.ruleSystemCode()).isEqualTo("ESP");
        assertThat(cmd.employeeTypeCode()).isEqualTo("INTERNAL");
        assertThat(cmd.employeeNumber()).isEqualTo("EMP000001");
        assertThat(cmd.companyCode()).isEqualTo("COMP");
        assertThat(cmd.entryReasonCode()).isEqualTo("HIRE");
        assertThat(cmd.exitReasonCode()).isNull();
        assertThat(cmd.startDate()).isEqualTo(hireDate);
        assertThat(cmd.endDate()).isNull();
    }

    @Test
    void wrapsPresenceCatalogValueInvalidExceptionToLifecycleException() {
        HireContext ctx = validContext();
        when(createPresenceUseCase.create(any(CreatePresenceCommand.class)))
                .thenThrow(new PresenceCatalogValueInvalidException("companyCode", "BAD"));

        assertThatThrownBy(() -> participant.participate(ctx))
                .isInstanceOf(HireEmployeeCatalogValueInvalidException.class);
    }

    private HireContext validContext() {
        HireContext ctx = new HireContext(
                "ESP", "INTERNAL", "Ana", "Lopez", null, "Ani",
                LocalDate.of(2026, 3, 23),
                "COMP", "HIRE", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );
        ctx.setEmployeeNumber("EMP000001");
        return ctx;
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

Run: `mvn test -Dtest=PresenceParticipantTest -q`
Expected: FAIL

- [ ] **Step 3: Implement PresenceParticipant**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.application.port.HireParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.presence.application.usecase.CreatePresenceCommand;
import com.b4rrhh.employee.presence.application.usecase.CreatePresenceUseCase;
import com.b4rrhh.employee.presence.domain.exception.PresenceCatalogValueInvalidException;
import org.springframework.stereotype.Component;

@Component
public class PresenceParticipant implements HireParticipant {

    private final CreatePresenceUseCase createPresenceUseCase;

    public PresenceParticipant(CreatePresenceUseCase createPresenceUseCase) {
        this.createPresenceUseCase = createPresenceUseCase;
    }

    @Override
    public int order() { return 20; }

    @Override
    public void participate(HireContext ctx) {
        try {
            ctx.setPresence(createPresenceUseCase.create(new CreatePresenceCommand(
                    ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                    ctx.companyCode(), ctx.entryReasonCode(), null, ctx.hireDate(), null
            )));
        } catch (PresenceCatalogValueInvalidException ex) {
            throw new HireEmployeeCatalogValueInvalidException(ex.getMessage(), ex);
        }
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

Run: `mvn test -Dtest=PresenceParticipantTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/lifecycle/application/participant/PresenceParticipant.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/participant/PresenceParticipantTest.java
git commit -m "feat(lifecycle): add PresenceParticipant order=20 (ADR-047)"
```

---

## Task 5: WorkCenterParticipant (order = 30)

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/WorkCenterParticipant.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/WorkCenterParticipantTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.workcenter.application.usecase.CreateWorkCenterCommand;
import com.b4rrhh.employee.workcenter.application.usecase.CreateWorkCenterUseCase;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCatalogValueInvalidException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkCenterParticipantTest {

    @Mock
    private CreateWorkCenterUseCase createWorkCenterUseCase;

    @InjectMocks
    private WorkCenterParticipant participant;

    @Test
    void orderIs30() {
        assertThat(participant.order()).isEqualTo(30);
    }

    @Test
    void createsWorkCenterFromContextAndStoresResult() {
        HireContext ctx = validContext();
        LocalDate hireDate = ctx.hireDate();
        WorkCenter workCenter = new WorkCenter(20L, 100L, 1, "WC1", hireDate, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class))).thenReturn(workCenter);

        participant.participate(ctx);

        assertThat(ctx.workCenter()).isSameAs(workCenter);

        ArgumentCaptor<CreateWorkCenterCommand> captor = ArgumentCaptor.forClass(CreateWorkCenterCommand.class);
        verify(createWorkCenterUseCase).create(captor.capture());
        assertThat(captor.getValue().workCenterCode()).isEqualTo("WC1");
        assertThat(captor.getValue().startDate()).isEqualTo(hireDate);
        assertThat(captor.getValue().endDate()).isNull();
    }

    @Test
    void wrapsWorkCenterCatalogValueInvalidExceptionToLifecycleException() {
        HireContext ctx = validContext();
        when(createWorkCenterUseCase.create(any(CreateWorkCenterCommand.class)))
                .thenThrow(new WorkCenterCatalogValueInvalidException("workCenterCode", "BAD"));

        assertThatThrownBy(() -> participant.participate(ctx))
                .isInstanceOf(HireEmployeeCatalogValueInvalidException.class);
    }

    private HireContext validContext() {
        HireContext ctx = new HireContext(
                "ESP", "INTERNAL", "Ana", "Lopez", null, "Ani",
                LocalDate.of(2026, 3, 23),
                "COMP", "HIRE", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );
        ctx.setEmployeeNumber("EMP000001");
        return ctx;
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

Run: `mvn test -Dtest=WorkCenterParticipantTest -q`
Expected: FAIL

- [ ] **Step 3: Implement WorkCenterParticipant**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.application.port.HireParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.workcenter.application.usecase.CreateWorkCenterCommand;
import com.b4rrhh.employee.workcenter.application.usecase.CreateWorkCenterUseCase;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCatalogValueInvalidException;
import org.springframework.stereotype.Component;

@Component
public class WorkCenterParticipant implements HireParticipant {

    private final CreateWorkCenterUseCase createWorkCenterUseCase;

    public WorkCenterParticipant(CreateWorkCenterUseCase createWorkCenterUseCase) {
        this.createWorkCenterUseCase = createWorkCenterUseCase;
    }

    @Override
    public int order() { return 30; }

    @Override
    public void participate(HireContext ctx) {
        try {
            ctx.setWorkCenter(createWorkCenterUseCase.create(new CreateWorkCenterCommand(
                    ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                    ctx.workCenterCode(), ctx.hireDate(), null
            )));
        } catch (WorkCenterCatalogValueInvalidException ex) {
            throw new HireEmployeeCatalogValueInvalidException(ex.getMessage(), ex);
        }
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

Run: `mvn test -Dtest=WorkCenterParticipantTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/lifecycle/application/participant/WorkCenterParticipant.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/participant/WorkCenterParticipantTest.java
git commit -m "feat(lifecycle): add WorkCenterParticipant order=30 (ADR-047)"
```

---

## Task 6: CostCenterParticipant (order = 40)

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/CostCenterParticipant.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/CostCenterParticipantTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.cost_center.application.usecase.CostCenterDistributionItem;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionCommand;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionUseCase;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterCatalogValueInvalidException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostCenterParticipantTest {

    @Mock
    private CreateCostCenterDistributionUseCase createCostCenterDistributionUseCase;

    @InjectMocks
    private CostCenterParticipant participant;

    @Test
    void orderIs40() {
        assertThat(participant.order()).isEqualTo(40);
    }

    @Test
    void skipsCreationWhenCostCenterDistributionIsNull() {
        HireContext ctx = contextWithoutCostCenter();

        participant.participate(ctx);

        verify(createCostCenterDistributionUseCase, never()).create(any());
        assertThat(ctx.costCenter()).isNull();
    }

    @Test
    void createsCostCenterDistributionWhenPresent() {
        HireContext ctx = contextWithCostCenter();
        CostCenterDistributionWindow window = org.mockito.Mockito.mock(CostCenterDistributionWindow.class);
        when(createCostCenterDistributionUseCase.create(any(CreateCostCenterDistributionCommand.class)))
                .thenReturn(window);

        participant.participate(ctx);

        assertThat(ctx.costCenter()).isSameAs(window);
    }

    @Test
    void wrapsCostCenterCatalogValueInvalidExceptionToLifecycleException() {
        HireContext ctx = contextWithCostCenter();
        when(createCostCenterDistributionUseCase.create(any(CreateCostCenterDistributionCommand.class)))
                .thenThrow(new CostCenterCatalogValueInvalidException("costCenterCode", "BAD"));

        assertThatThrownBy(() -> participant.participate(ctx))
                .isInstanceOf(HireEmployeeCatalogValueInvalidException.class);
    }

    private HireContext contextWithoutCostCenter() {
        HireContext ctx = new HireContext(
                "ESP", "INTERNAL", "Ana", "Lopez", null, "Ani",
                LocalDate.of(2026, 3, 23),
                "COMP", "HIRE", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );
        ctx.setEmployeeNumber("EMP000001");
        return ctx;
    }

    private HireContext contextWithCostCenter() {
        HireContext ctx = new HireContext(
                "ESP", "INTERNAL", "Ana", "Lopez", null, "Ani",
                LocalDate.of(2026, 3, 23),
                "COMP", "HIRE", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                new HireEmployeeCommand.HireEmployeeCostCenterDistributionCommand(
                        List.of(new HireEmployeeCommand.HireEmployeeCostCenterItemCommand("CC1", 100.0))
                ),
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );
        ctx.setEmployeeNumber("EMP000001");
        return ctx;
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

Run: `mvn test -Dtest=CostCenterParticipantTest -q`
Expected: FAIL

- [ ] **Step 3: Implement CostCenterParticipant**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.cost_center.application.usecase.CostCenterDistributionItem;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionCommand;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionUseCase;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterCatalogValueInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionInvalidException;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.application.port.HireParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Component
public class CostCenterParticipant implements HireParticipant {

    private final CreateCostCenterDistributionUseCase createCostCenterDistributionUseCase;

    public CostCenterParticipant(CreateCostCenterDistributionUseCase createCostCenterDistributionUseCase) {
        this.createCostCenterDistributionUseCase = createCostCenterDistributionUseCase;
    }

    @Override
    public int order() { return 40; }

    @Override
    public void participate(HireContext ctx) {
        HireEmployeeCommand.HireEmployeeCostCenterDistributionCommand costCenterDistribution =
                ctx.costCenterDistribution();
        if (costCenterDistribution == null) {
            return;
        }
        try {
            ctx.setCostCenter(createCostCenterDistributionUseCase.create(
                    new CreateCostCenterDistributionCommand(
                            ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                            ctx.hireDate(),
                            costCenterDistribution.items().stream()
                                    .map(item -> new CostCenterDistributionItem(
                                            item.costCenterCode(),
                                            BigDecimal.valueOf(item.allocationPercentage())))
                                    .collect(Collectors.toList())
                    )));
        } catch (CostCenterCatalogValueInvalidException | CostCenterDistributionInvalidException ex) {
            throw new HireEmployeeCatalogValueInvalidException(ex.getMessage(), ex);
        }
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

Run: `mvn test -Dtest=CostCenterParticipantTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/lifecycle/application/participant/CostCenterParticipant.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/participant/CostCenterParticipantTest.java
git commit -m "feat(lifecycle): add CostCenterParticipant order=40 (ADR-047)"
```

---

## Task 7: ContractParticipant (order = 50)

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/ContractParticipant.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/ContractParticipantTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.contract.application.command.CreateContractCommand;
import com.b4rrhh.employee.contract.application.usecase.CreateContractUseCase;
import com.b4rrhh.employee.contract.domain.exception.ContractInvalidException;
import com.b4rrhh.employee.contract.domain.exception.ContractSubtypeRelationInvalidException;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeDependentRelationInvalidException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractParticipantTest {

    @Mock
    private CreateContractUseCase createContractUseCase;

    @InjectMocks
    private ContractParticipant participant;

    @Test
    void orderIs50() {
        assertThat(participant.order()).isEqualTo(50);
    }

    @Test
    void createsContractFromContextAndStoresResult() {
        HireContext ctx = validContext();
        LocalDate hireDate = ctx.hireDate();
        Contract contract = new Contract(100L, "CON", "SUB", hireDate, null);
        when(createContractUseCase.create(any(CreateContractCommand.class))).thenReturn(contract);

        participant.participate(ctx);

        assertThat(ctx.contractResult()).isSameAs(contract);

        ArgumentCaptor<CreateContractCommand> captor = ArgumentCaptor.forClass(CreateContractCommand.class);
        verify(createContractUseCase).create(captor.capture());
        assertThat(captor.getValue().contractTypeCode()).isEqualTo("CON");
        assertThat(captor.getValue().contractSubtypeCode()).isEqualTo("SUB");
        assertThat(captor.getValue().startDate()).isEqualTo(hireDate);
    }

    @Test
    void wrapsContractInvalidExceptionToLifecycleException() {
        HireContext ctx = validContext();
        when(createContractUseCase.create(any(CreateContractCommand.class)))
                .thenThrow(new ContractInvalidException("contractTypeCode", "BAD"));

        assertThatThrownBy(() -> participant.participate(ctx))
                .isInstanceOf(HireEmployeeCatalogValueInvalidException.class);
    }

    @Test
    void wrapsContractSubtypeRelationInvalidExceptionToLifecycleException() {
        HireContext ctx = validContext();
        when(createContractUseCase.create(any(CreateContractCommand.class)))
                .thenThrow(new ContractSubtypeRelationInvalidException("CON", "SUB"));

        assertThatThrownBy(() -> participant.participate(ctx))
                .isInstanceOf(HireEmployeeDependentRelationInvalidException.class);
    }

    private HireContext validContext() {
        HireContext ctx = new HireContext(
                "ESP", "INTERNAL", "Ana", "Lopez", null, "Ani",
                LocalDate.of(2026, 3, 23),
                "COMP", "HIRE", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );
        ctx.setEmployeeNumber("EMP000001");
        return ctx;
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

Run: `mvn test -Dtest=ContractParticipantTest -q`
Expected: FAIL

- [ ] **Step 3: Implement ContractParticipant**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.contract.application.command.CreateContractCommand;
import com.b4rrhh.employee.contract.application.usecase.CreateContractUseCase;
import com.b4rrhh.employee.contract.domain.exception.ContractInvalidException;
import com.b4rrhh.employee.contract.domain.exception.ContractSubtypeInvalidException;
import com.b4rrhh.employee.contract.domain.exception.ContractSubtypeRelationInvalidException;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.application.port.HireParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeDependentRelationInvalidException;
import org.springframework.stereotype.Component;

@Component
public class ContractParticipant implements HireParticipant {

    private final CreateContractUseCase createContractUseCase;

    public ContractParticipant(CreateContractUseCase createContractUseCase) {
        this.createContractUseCase = createContractUseCase;
    }

    @Override
    public int order() { return 50; }

    @Override
    public void participate(HireContext ctx) {
        try {
            ctx.setContractResult(createContractUseCase.create(new CreateContractCommand(
                    ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                    ctx.contract().contractTypeCode(),
                    ctx.contract().contractSubtypeCode(),
                    ctx.hireDate(), null
            )));
        } catch (ContractInvalidException | ContractSubtypeInvalidException ex) {
            throw new HireEmployeeCatalogValueInvalidException(ex.getMessage(), ex);
        } catch (ContractSubtypeRelationInvalidException ex) {
            throw new HireEmployeeDependentRelationInvalidException(ex.getMessage(), ex);
        }
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

Run: `mvn test -Dtest=ContractParticipantTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/lifecycle/application/participant/ContractParticipant.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/participant/ContractParticipantTest.java
git commit -m "feat(lifecycle): add ContractParticipant order=50 (ADR-047)"
```

---

## Task 8: LaborClassificationParticipant (order = 60)

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/LaborClassificationParticipant.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/LaborClassificationParticipantTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.labor_classification.application.command.CreateLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.usecase.CreateLaborClassificationUseCase;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAgreementCategoryRelationInvalidException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAgreementInvalidException;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeDependentRelationInvalidException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaborClassificationParticipantTest {

    @Mock
    private CreateLaborClassificationUseCase createLaborClassificationUseCase;

    @InjectMocks
    private LaborClassificationParticipant participant;

    @Test
    void orderIs60() {
        assertThat(participant.order()).isEqualTo(60);
    }

    @Test
    void createsLaborClassificationFromContextAndStoresResult() {
        HireContext ctx = validContext();
        LocalDate hireDate = ctx.hireDate();
        LaborClassification laborClassification = new LaborClassification(100L, "AGR", "CAT", hireDate, null);
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class)))
                .thenReturn(laborClassification);

        participant.participate(ctx);

        assertThat(ctx.laborClassificationResult()).isSameAs(laborClassification);

        ArgumentCaptor<CreateLaborClassificationCommand> captor =
                ArgumentCaptor.forClass(CreateLaborClassificationCommand.class);
        verify(createLaborClassificationUseCase).create(captor.capture());
        assertThat(captor.getValue().agreementCode()).isEqualTo("AGR");
        assertThat(captor.getValue().agreementCategoryCode()).isEqualTo("CAT");
        assertThat(captor.getValue().startDate()).isEqualTo(hireDate);
        assertThat(captor.getValue().endDate()).isNull();
    }

    @Test
    void wrapsAgreementInvalidExceptionToLifecycleException() {
        HireContext ctx = validContext();
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class)))
                .thenThrow(new LaborClassificationAgreementInvalidException("ESP", "BAD_AGR", LocalDate.of(2026, 3, 23)));

        assertThatThrownBy(() -> participant.participate(ctx))
                .isInstanceOf(HireEmployeeCatalogValueInvalidException.class);
    }

    @Test
    void wrapsAgreementCategoryRelationInvalidExceptionToLifecycleException() {
        HireContext ctx = validContext();
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class)))
                .thenThrow(new LaborClassificationAgreementCategoryRelationInvalidException(
                        "ESP", "AGR", "BAD_CAT", LocalDate.of(2026, 3, 23)));

        assertThatThrownBy(() -> participant.participate(ctx))
                .isInstanceOf(HireEmployeeDependentRelationInvalidException.class);
    }

    private HireContext validContext() {
        HireContext ctx = new HireContext(
                "ESP", "INTERNAL", "Ana", "Lopez", null, "Ani",
                LocalDate.of(2026, 3, 23),
                "COMP", "HIRE", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );
        ctx.setEmployeeNumber("EMP000001");
        return ctx;
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

Run: `mvn test -Dtest=LaborClassificationParticipantTest -q`
Expected: FAIL

- [ ] **Step 3: Implement LaborClassificationParticipant**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.labor_classification.application.command.CreateLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.usecase.CreateLaborClassificationUseCase;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAgreementCategoryRelationInvalidException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAgreementInvalidException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationCategoryInvalidException;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.application.port.HireParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeDependentRelationInvalidException;
import org.springframework.stereotype.Component;

@Component
public class LaborClassificationParticipant implements HireParticipant {

    private final CreateLaborClassificationUseCase createLaborClassificationUseCase;

    public LaborClassificationParticipant(CreateLaborClassificationUseCase createLaborClassificationUseCase) {
        this.createLaborClassificationUseCase = createLaborClassificationUseCase;
    }

    @Override
    public int order() { return 60; }

    @Override
    public void participate(HireContext ctx) {
        try {
            ctx.setLaborClassificationResult(createLaborClassificationUseCase.create(
                    new CreateLaborClassificationCommand(
                            ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                            ctx.laborClassification().agreementCode(),
                            ctx.laborClassification().agreementCategoryCode(),
                            ctx.hireDate(), null
                    )));
        } catch (LaborClassificationAgreementInvalidException | LaborClassificationCategoryInvalidException ex) {
            throw new HireEmployeeCatalogValueInvalidException(ex.getMessage(), ex);
        } catch (LaborClassificationAgreementCategoryRelationInvalidException ex) {
            throw new HireEmployeeDependentRelationInvalidException(ex.getMessage(), ex);
        }
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

Run: `mvn test -Dtest=LaborClassificationParticipantTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/lifecycle/application/participant/LaborClassificationParticipant.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/participant/LaborClassificationParticipantTest.java
git commit -m "feat(lifecycle): add LaborClassificationParticipant order=60 (ADR-047)"
```

---

## Task 9: WorkingTimeParticipant (order = 70)

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/WorkingTimeParticipant.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/WorkingTimeParticipantTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeBusinessValidationException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeConflictException;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.domain.exception.InvalidWorkingTimePercentageException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNumberConflictException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeDerivedHours;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkingTimeParticipantTest {

    @Mock
    private CreateWorkingTimeUseCase createWorkingTimeUseCase;

    @InjectMocks
    private WorkingTimeParticipant participant;

    @Test
    void orderIs70() {
        assertThat(participant.order()).isEqualTo(70);
    }

    @Test
    void createsWorkingTimeFromContextAndStoresResult() {
        HireContext ctx = validContext();
        LocalDate hireDate = ctx.hireDate();
        WorkingTime workingTime = WorkingTime.rehydrate(
                30L, 100L, 1, hireDate, null, new BigDecimal("75"),
                new WorkingTimeDerivedHours(new BigDecimal("30.00"), new BigDecimal("6.00"), new BigDecimal("125.00")),
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class))).thenReturn(workingTime);

        participant.participate(ctx);

        assertThat(ctx.workingTimeResult()).isSameAs(workingTime);

        ArgumentCaptor<CreateWorkingTimeCommand> captor = ArgumentCaptor.forClass(CreateWorkingTimeCommand.class);
        verify(createWorkingTimeUseCase).create(captor.capture());
        assertThat(captor.getValue().ruleSystemCode()).isEqualTo("ESP");
        assertThat(captor.getValue().employeeTypeCode()).isEqualTo("INTERNAL");
        assertThat(captor.getValue().employeeNumber()).isEqualTo("EMP000001");
        assertThat(captor.getValue().startDate()).isEqualTo(hireDate);
        assertThat(captor.getValue().workingTimePercentage()).isEqualByComparingTo("75");
    }

    @Test
    void wrapsInvalidWorkingTimePercentageExceptionToLifecycleException() {
        HireContext ctx = validContext();
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class)))
                .thenThrow(new InvalidWorkingTimePercentageException("workingTimePercentage must be > 0 and <= 100"));

        assertThatThrownBy(() -> participant.participate(ctx))
                .isInstanceOf(HireEmployeeBusinessValidationException.class);
    }

    @Test
    void wrapsWorkingTimeNumberConflictExceptionToLifecycleConflictException() {
        HireContext ctx = validContext();
        when(createWorkingTimeUseCase.create(any(CreateWorkingTimeCommand.class)))
                .thenThrow(new WorkingTimeNumberConflictException(
                        "ESP", "INTERNAL", "EMP000001", 1, new RuntimeException("dup")));

        assertThatThrownBy(() -> participant.participate(ctx))
                .isInstanceOf(HireEmployeeConflictException.class);
    }

    private HireContext validContext() {
        HireContext ctx = new HireContext(
                "ESP", "INTERNAL", "Ana", "Lopez", null, "Ani",
                LocalDate.of(2026, 3, 23),
                "COMP", "HIRE", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );
        ctx.setEmployeeNumber("EMP000001");
        return ctx;
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

Run: `mvn test -Dtest=WorkingTimeParticipantTest -q`
Expected: FAIL

- [ ] **Step 3: Implement WorkingTimeParticipant**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.application.port.HireParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeBusinessValidationException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeConflictException;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.domain.exception.InvalidWorkingTimePercentageException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeEmployeeNotFoundException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNumberConflictException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOutsidePresencePeriodException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOverlapException;
import org.springframework.stereotype.Component;

@Component
public class WorkingTimeParticipant implements HireParticipant {

    private final CreateWorkingTimeUseCase createWorkingTimeUseCase;

    public WorkingTimeParticipant(CreateWorkingTimeUseCase createWorkingTimeUseCase) {
        this.createWorkingTimeUseCase = createWorkingTimeUseCase;
    }

    @Override
    public int order() { return 70; }

    @Override
    public void participate(HireContext ctx) {
        try {
            ctx.setWorkingTimeResult(createWorkingTimeUseCase.create(new CreateWorkingTimeCommand(
                    ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                    ctx.hireDate(), ctx.workingTime().workingTimePercentage()
            )));
        } catch (InvalidWorkingTimePercentageException
                 | WorkingTimeOutsidePresencePeriodException
                 | WorkingTimeOverlapException ex) {
            throw new HireEmployeeBusinessValidationException(ex.getMessage(), ex);
        } catch (WorkingTimeNumberConflictException ex) {
            throw new HireEmployeeConflictException(ex.getMessage());
        } catch (WorkingTimeEmployeeNotFoundException ex) {
            throw new HireEmployeeConflictException("Created employee is not available for initial workingTime creation");
        }
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

Run: `mvn test -Dtest=WorkingTimeParticipantTest -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/lifecycle/application/participant/WorkingTimeParticipant.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/participant/WorkingTimeParticipantTest.java
git commit -m "feat(lifecycle): add WorkingTimeParticipant order=70 (ADR-047)"
```

---

## Task 10: Refactor HireEmployeeService + rewrite HireEmployeeServiceTest

**Files:**
- Modify: `src/main/java/com/b4rrhh/employee/lifecycle/application/usecase/HireEmployeeService.java` (full rewrite)
- Modify: `src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/HireEmployeeServiceTest.java` (full rewrite)

**Precondition:** Tasks 1–9 are complete. All participant and validator tests pass. Verify with `mvn test -Dtest="HireEmployeePreConditionValidatorTest,EmployeeCoreParticipantTest,PresenceParticipantTest,WorkCenterParticipantTest,CostCenterParticipantTest,ContractParticipantTest,LaborClassificationParticipantTest,WorkingTimeParticipantTest" -q`

- [ ] **Step 1: Rewrite HireEmployeeService**

Replace the entire file content with:

```java
package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.application.model.HireEmployeeResult;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.port.HireParticipant;
import com.b4rrhh.employee.lifecycle.application.port.NextEmployeeNumberPort;
import com.b4rrhh.employee.lifecycle.application.service.HireEmployeePreConditionValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class HireEmployeeService implements HireEmployeeUseCase {

    private final HireEmployeePreConditionValidator validator;
    private final NextEmployeeNumberPort nextEmployeeNumberPort;
    private final List<HireParticipant> participants;

    public HireEmployeeService(
            HireEmployeePreConditionValidator validator,
            NextEmployeeNumberPort nextEmployeeNumberPort,
            List<HireParticipant> participants) {
        this.validator = validator;
        this.nextEmployeeNumberPort = nextEmployeeNumberPort;
        this.participants = participants.stream()
                .sorted(Comparator.comparingInt(HireParticipant::order))
                .toList();
    }

    @Override
    @Transactional
    public HireEmployeeResult hire(HireEmployeeCommand command) {
        HireContext ctx = validator.validateAndNormalize(command);
        ctx.setEmployeeNumber(nextEmployeeNumberPort.consumeNext(ctx.ruleSystemCode()));
        participants.forEach(p -> p.participate(ctx));
        return ctx.toResult();
    }
}
```

- [ ] **Step 2: Write the new HireEmployeeServiceTest**

Replace the entire file content with:

```java
package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.application.model.HireEmployeeResult;
import com.b4rrhh.employee.lifecycle.application.port.HireParticipant;
import com.b4rrhh.employee.lifecycle.application.port.NextEmployeeNumberPort;
import com.b4rrhh.employee.lifecycle.application.service.HireEmployeePreConditionValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HireEmployeeServiceTest {

    @Mock
    private HireEmployeePreConditionValidator validator;
    @Mock
    private NextEmployeeNumberPort nextEmployeeNumberPort;

    // Two mock participants to verify ordering behaviour
    private final HireParticipant firstParticipant = mock(HireParticipant.class);
    private final HireParticipant secondParticipant = mock(HireParticipant.class);

    @Test
    void callsValidatorThenConsumesNumberThenRunsParticipantsInOrder() {
        HireContext ctx = validContext();
        when(validator.validateAndNormalize(any(HireEmployeeCommand.class))).thenReturn(ctx);
        when(nextEmployeeNumberPort.consumeNext(anyString())).thenReturn("EMP000001");
        when(firstParticipant.order()).thenReturn(10);
        when(secondParticipant.order()).thenReturn(20);

        HireEmployeeService service = new HireEmployeeService(
                validator, nextEmployeeNumberPort, List.of(secondParticipant, firstParticipant));
        service.hire(validCommand());

        InOrder order = inOrder(validator, nextEmployeeNumberPort, firstParticipant, secondParticipant);
        order.verify(validator).validateAndNormalize(any(HireEmployeeCommand.class));
        order.verify(nextEmployeeNumberPort).consumeNext("ESP");
        order.verify(firstParticipant).participate(ctx);
        order.verify(secondParticipant).participate(ctx);
    }

    @Test
    void setsEmployeeNumberOnContextBeforeRunningParticipants() {
        HireContext ctx = validContext();
        when(validator.validateAndNormalize(any(HireEmployeeCommand.class))).thenReturn(ctx);
        when(nextEmployeeNumberPort.consumeNext(anyString())).thenReturn("EMP999999");
        when(firstParticipant.order()).thenReturn(10);

        HireEmployeeService service = new HireEmployeeService(
                validator, nextEmployeeNumberPort, List.of(firstParticipant));
        service.hire(validCommand());

        assertThat(ctx.employeeNumber()).isEqualTo("EMP999999");
    }

    @Test
    void returnsResultFromContext() {
        HireContext ctx = mock(HireContext.class);
        HireEmployeeResult expectedResult = mock(HireEmployeeResult.class);
        when(validator.validateAndNormalize(any(HireEmployeeCommand.class))).thenReturn(ctx);
        when(nextEmployeeNumberPort.consumeNext(any())).thenReturn("EMP000001");
        when(ctx.ruleSystemCode()).thenReturn("ESP");
        when(ctx.toResult()).thenReturn(expectedResult);

        HireEmployeeService service = new HireEmployeeService(
                validator, nextEmployeeNumberPort, List.of());
        HireEmployeeResult result = service.hire(validCommand());

        assertThat(result).isSameAs(expectedResult);
    }

    @Test
    void sortedByOrderRegardlessOfInjectionOrder() {
        HireContext ctx = validContext();
        when(validator.validateAndNormalize(any(HireEmployeeCommand.class))).thenReturn(ctx);
        when(nextEmployeeNumberPort.consumeNext(anyString())).thenReturn("EMP000001");
        when(firstParticipant.order()).thenReturn(10);
        when(secondParticipant.order()).thenReturn(20);

        // Deliberately pass second before first — service must sort them
        HireEmployeeService service = new HireEmployeeService(
                validator, nextEmployeeNumberPort, List.of(secondParticipant, firstParticipant));
        service.hire(validCommand());

        InOrder order = inOrder(firstParticipant, secondParticipant);
        order.verify(firstParticipant).participate(ctx);
        order.verify(secondParticipant).participate(ctx);
    }

    private HireContext validContext() {
        return new HireContext(
                "ESP", "INTERNAL", "Ana", "Lopez", null, "Ani",
                LocalDate.of(2026, 3, 23),
                "COMP", "HIRE", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );
    }

    private HireEmployeeCommand validCommand() {
        return new HireEmployeeCommand(
                "ESP", "INTERNAL", "Ana", "Lopez", null, "Ani",
                LocalDate.of(2026, 3, 23), "HIRE", "COMP", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );
    }
}
```

- [ ] **Step 3: Run the new service tests**

Run: `mvn test -Dtest=HireEmployeeServiceTest -q`
Expected: PASS — 4 tests green

- [ ] **Step 4: Run the full test suite**

Run: `mvn test -q`
Expected: All tests pass (including HireEmployeeBaselineFlywayIntegrationTest and HireEmployeeServiceRollbackIntegrationTest)

If `HireEmployeeServiceRollbackIntegrationTest` fails, it's because it constructs `HireEmployeeService` directly with the old 10-arg constructor. Update its `@TestConfiguration` bean to match the new 3-arg constructor, providing a list of the 7 participants and a real `HireEmployeePreConditionValidator`. See the existing test config for the mocked use case beans — they map 1:1 to the participant implementations.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/b4rrhh/employee/lifecycle/application/usecase/HireEmployeeService.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/HireEmployeeServiceTest.java
git commit -m "refactor(lifecycle): replace HireEmployeeService orchestration with HireParticipant pattern (ADR-047)"
```

---

## Post-refactor verification

After Task 10, run the integration tests to confirm end-to-end behaviour is unchanged:

```bash
mvn test -Dtest="HireEmployeeBaselineFlywayIntegrationTest,HireEmployeeServiceRollbackIntegrationTest,NextEmployeeNumberAdapterIntegrationTest" -q
```

All three must pass.

---

## Notes

- **Phase 2 (TERMINATION)** is a separate plan. `TerminateEmployeeService` has 13 deps and an idempotency path that requires its own analysis. Do not touch it here.
- **Exception handler cleanup** (`HireEmployeeExceptionHandler`) can be done as a follow-up: after the participants own their exception translation, the vertical-specific imports in the handler can be removed. Not required for Phase 1 to be correct.
- **`HireEmployeeServiceRollbackIntegrationTest`** needs its `@TestConfiguration` wired to the new constructor. It already has beans for all 7 use cases — just wrap each in the corresponding participant class and provide a real `HireEmployeePreConditionValidator` bean.
