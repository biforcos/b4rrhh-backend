# TerminateEmployeeService Participant Pattern Refactor (ADR-047 Phase 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `TerminateEmployeeService` (13 dependencies, ~600 lines) with a 3-dependency orchestrator that delegates each vertical's closure logic to a `TerminationParticipant`, mirroring Phase 1's `HireParticipant` pattern.

**Architecture:** `TerminationPreConditionValidator` handles input validation, employee lookup, and idempotency detection. `TerminationContext` carries normalized inputs (final) and participant results (mutable). Six participants (WorkingTime→WorkCenter→CostCenter→Contract→LaborClassification→Presence) each close their vertical's active record within the existing single `@Transactional` boundary. The refactored service holds only 3 dependencies: `validator`, `participants`, `employeeRepository`.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Mockito, H2 (rollback integration test)

---

## File Map

**New — production:**
- `src/main/java/com/b4rrhh/employee/lifecycle/application/port/TerminationParticipant.java`
- `src/main/java/com/b4rrhh/employee/lifecycle/application/model/TerminationContext.java`
- `src/main/java/com/b4rrhh/employee/lifecycle/application/service/TerminationPreConditionValidator.java`
- `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/WorkingTimeTerminationParticipant.java`
- `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/WorkCenterTerminationParticipant.java`
- `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/CostCenterTerminationParticipant.java`
- `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/ContractTerminationParticipant.java`
- `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/LaborClassificationTerminationParticipant.java`
- `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/PresenceTerminationParticipant.java`

**New — test:**
- `src/test/java/com/b4rrhh/employee/lifecycle/application/service/TerminationPreConditionValidatorTest.java`
- `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/WorkingTimeTerminationParticipantTest.java`
- `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/WorkCenterTerminationParticipantTest.java`
- `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/CostCenterTerminationParticipantTest.java`
- `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/ContractTerminationParticipantTest.java`
- `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/LaborClassificationTerminationParticipantTest.java`
- `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/PresenceTerminationParticipantTest.java`

**Modified:**
- `src/main/java/com/b4rrhh/employee/lifecycle/application/usecase/TerminateEmployeeService.java` — full rewrite
- `src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/TerminateEmployeeServiceTest.java` — full rewrite
- `src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/TerminateEmployeeServiceRollbackIntegrationTest.java` — `@TestConfiguration` rewrite

---

## Task 1: TerminationParticipant interface + TerminationContext model

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/port/TerminationParticipant.java`
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/model/TerminationContext.java`

- [ ] **Step 1: Create TerminationParticipant interface**

```java
package com.b4rrhh.employee.lifecycle.application.port;

import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;

public interface TerminationParticipant {
    int order();
    void participate(TerminationContext ctx);
}
```

- [ ] **Step 2: Create TerminationContext**

```java
package com.b4rrhh.employee.lifecycle.application.model;

import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class TerminationContext {

    private final String ruleSystemCode;
    private final String employeeTypeCode;
    private final String employeeNumber;
    private final LocalDate terminationDate;
    private final String exitReasonCode;
    private final Employee employee;
    private final boolean alreadyTerminated;
    private final TerminateEmployeeResult idempotentResult;

    private Presence closedPresence;
    private WorkCenter closedWorkCenter;
    private Contract closedContract;
    private LaborClassification closedLaborClassification;
    private WorkingTime closedWorkingTime;

    public TerminationContext(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate terminationDate,
            String exitReasonCode,
            Employee employee) {
        this.ruleSystemCode = ruleSystemCode;
        this.employeeTypeCode = employeeTypeCode;
        this.employeeNumber = employeeNumber;
        this.terminationDate = terminationDate;
        this.exitReasonCode = exitReasonCode;
        this.employee = employee;
        this.alreadyTerminated = false;
        this.idempotentResult = null;
    }

    public TerminationContext(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate terminationDate,
            String exitReasonCode,
            Employee employee,
            TerminateEmployeeResult idempotentResult) {
        this.ruleSystemCode = ruleSystemCode;
        this.employeeTypeCode = employeeTypeCode;
        this.employeeNumber = employeeNumber;
        this.terminationDate = terminationDate;
        this.exitReasonCode = exitReasonCode;
        this.employee = employee;
        this.alreadyTerminated = true;
        this.idempotentResult = idempotentResult;
    }

    public String ruleSystemCode() { return ruleSystemCode; }
    public String employeeTypeCode() { return employeeTypeCode; }
    public String employeeNumber() { return employeeNumber; }
    public LocalDate terminationDate() { return terminationDate; }
    public String exitReasonCode() { return exitReasonCode; }
    public Employee employee() { return employee; }
    public boolean isAlreadyTerminated() { return alreadyTerminated; }

    public void setClosedPresence(Presence p) { this.closedPresence = p; }
    public void setClosedWorkCenter(WorkCenter wc) { this.closedWorkCenter = wc; }
    public void setClosedContract(Contract c) { this.closedContract = c; }
    public void setClosedLaborClassification(LaborClassification lc) { this.closedLaborClassification = lc; }
    public void setClosedWorkingTime(WorkingTime wt) { this.closedWorkingTime = wt; }

    public Presence closedPresence() { return closedPresence; }
    public WorkCenter closedWorkCenter() { return closedWorkCenter; }
    public Contract closedContract() { return closedContract; }
    public LaborClassification closedLaborClassification() { return closedLaborClassification; }
    public WorkingTime closedWorkingTime() { return closedWorkingTime; }

    public TerminateEmployeeResult reconstructIdempotentResult() {
        if (!alreadyTerminated) {
            throw new IllegalStateException("Context is not in idempotent state");
        }
        return idempotentResult;
    }

    public void assertNoActivePresence() {
        if (closedPresence == null) {
            throw new TerminateEmployeeConflictException("No active presence found for employee");
        }
    }

    public Employee terminatedEmployee() {
        return new Employee(
                employee.getId(),
                employee.getRuleSystemCode(),
                employee.getEmployeeTypeCode(),
                employee.getEmployeeNumber(),
                employee.getFirstName(),
                employee.getLastName1(),
                employee.getLastName2(),
                employee.getPreferredName(),
                "TERMINATED",
                employee.getCreatedAt(),
                LocalDateTime.now(),
                employee.getPhotoUrl()
        );
    }

    public TerminateEmployeeResult toResult() {
        Objects.requireNonNull(closedPresence, "closedPresence must be set before calling toResult()");
        return new TerminateEmployeeResult(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                terminationDate,
                exitReasonCode,
                "TERMINATED",
                closedPresence.getPresenceNumber(),
                closedPresence.getCompanyCode(),
                closedPresence.getEntryReasonCode(),
                closedPresence.getExitReasonCode(),
                closedPresence.getStartDate(),
                closedPresence.getEndDate(),
                closedContract != null ? closedContract.getContractCode() : null,
                closedContract != null ? closedContract.getContractSubtypeCode() : null,
                closedContract != null ? closedContract.getStartDate() : null,
                closedContract != null ? closedContract.getEndDate() : null,
                closedLaborClassification != null ? closedLaborClassification.getAgreementCode() : null,
                closedLaborClassification != null ? closedLaborClassification.getAgreementCategoryCode() : null,
                closedLaborClassification != null ? closedLaborClassification.getStartDate() : null,
                closedLaborClassification != null ? closedLaborClassification.getEndDate() : null,
                closedWorkCenter != null ? closedWorkCenter.getWorkCenterAssignmentNumber() : null,
                closedWorkCenter != null ? closedWorkCenter.getWorkCenterCode() : null,
                closedWorkCenter != null ? closedWorkCenter.getStartDate() : null,
                closedWorkCenter != null ? closedWorkCenter.getEndDate() : null,
                closedWorkingTime != null ? closedWorkingTime.getWorkingTimeNumber() : null,
                closedWorkingTime != null ? closedWorkingTime.getWorkingTimePercentage() : null,
                closedWorkingTime != null ? closedWorkingTime.getWeeklyHours() : null,
                closedWorkingTime != null ? closedWorkingTime.getDailyHours() : null,
                closedWorkingTime != null ? closedWorkingTime.getMonthlyHours() : null,
                closedWorkingTime != null ? closedWorkingTime.getStartDate() : null,
                closedWorkingTime != null ? closedWorkingTime.getEndDate() : null
        );
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -pl . -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```
git add src/main/java/com/b4rrhh/employee/lifecycle/application/port/TerminationParticipant.java
git add src/main/java/com/b4rrhh/employee/lifecycle/application/model/TerminationContext.java
git commit -m "feat(terminate): add TerminationParticipant interface and TerminationContext model"
```

---

## Task 2: TerminationPreConditionValidator + test

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/service/TerminationPreConditionValidator.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/service/TerminationPreConditionValidatorTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.b4rrhh.employee.lifecycle.application.service;

import com.b4rrhh.employee.contract.application.command.ListEmployeeContractsCommand;
import com.b4rrhh.employee.contract.application.usecase.ListEmployeeContractsUseCase;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.labor_classification.application.command.ListEmployeeLaborClassificationsCommand;
import com.b4rrhh.employee.labor_classification.application.usecase.ListEmployeeLaborClassificationsUseCase;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.TerminateEmployeeResult;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeEmployeeNotFoundException;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeRequestInvalidException;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.workcenter.application.usecase.ListEmployeeWorkCentersUseCase;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesCommand;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesUseCase;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeDerivedHours;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerminationPreConditionValidatorTest {

    @Mock private GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKey;
    @Mock private ListEmployeePresencesUseCase listPresences;
    @Mock private ListEmployeeContractsUseCase listContracts;
    @Mock private ListEmployeeLaborClassificationsUseCase listLaborClassifications;
    @Mock private ListEmployeeWorkCentersUseCase listWorkCenters;
    @Mock private ListEmployeeWorkingTimesUseCase listWorkingTimes;

    private TerminationPreConditionValidator validator;

    private static final LocalDate TERMINATION_DATE = LocalDate.of(2026, 3, 31);
    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        validator = new TerminationPreConditionValidator(
                getEmployeeByBusinessKey, listPresences, listContracts,
                listLaborClassifications, listWorkCenters, listWorkingTimes);
    }

    @Test
    void throwsWhenRuleSystemCodeMissing() {
        assertThrows(TerminateEmployeeRequestInvalidException.class,
                () -> validator.validateAndLookup(cmd(null, "INTERNAL", "EMP001")));
    }

    @Test
    void throwsWhenEmployeeTypeCodeMissing() {
        assertThrows(TerminateEmployeeRequestInvalidException.class,
                () -> validator.validateAndLookup(cmd("ESP", "", "EMP001")));
    }

    @Test
    void throwsWhenEmployeeNumberMissing() {
        assertThrows(TerminateEmployeeRequestInvalidException.class,
                () -> validator.validateAndLookup(cmd("ESP", "INTERNAL", "  ")));
    }

    @Test
    void throwsWhenTerminationDateNull() {
        TerminateEmployeeCommand command = new TerminateEmployeeCommand(
                "ESP", "INTERNAL", "EMP001", null, "VOL");
        assertThrows(TerminateEmployeeRequestInvalidException.class,
                () -> validator.validateAndLookup(command));
    }

    @Test
    void throwsWhenExitReasonCodeMissing() {
        TerminateEmployeeCommand command = new TerminateEmployeeCommand(
                "ESP", "INTERNAL", "EMP001", TERMINATION_DATE, null);
        assertThrows(TerminateEmployeeRequestInvalidException.class,
                () -> validator.validateAndLookup(command));
    }

    @Test
    void throwsWhenEmployeeNotFound() {
        when(getEmployeeByBusinessKey.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.empty());
        assertThrows(TerminateEmployeeEmployeeNotFoundException.class,
                () -> validator.validateAndLookup(cmd("ESP", "INTERNAL", "EMP001")));
    }

    @Test
    void returnsActiveContextWhenEmployeeIsActive() {
        Employee active = activeEmployee();
        when(getEmployeeByBusinessKey.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(active));

        TerminationContext ctx = validator.validateAndLookup(cmd("ESP", "INTERNAL", "EMP001"));

        assertFalse(ctx.isAlreadyTerminated());
        assertSame(active, ctx.employee());
        assertEquals("ESP", ctx.ruleSystemCode());
        assertEquals(TERMINATION_DATE, ctx.terminationDate());
        assertEquals("VOL", ctx.exitReasonCode());
    }

    @Test
    void normalizesInputsToUpperCase() {
        Employee active = activeEmployee();
        when(getEmployeeByBusinessKey.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(active));

        TerminationContext ctx = validator.validateAndLookup(
                new TerminateEmployeeCommand(" esp ", " internal ", " emp001 ", TERMINATION_DATE, " vol "));

        assertEquals("ESP", ctx.ruleSystemCode());
        assertEquals("INTERNAL", ctx.employeeTypeCode());
        assertEquals("EMP001", ctx.employeeNumber());
        assertEquals("VOL", ctx.exitReasonCode());
    }

    @Test
    void returnsIdempotentContextWhenEmployeeAlreadyTerminated() {
        Employee terminated = terminatedEmployee();
        when(getEmployeeByBusinessKey.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(terminated));

        Presence presence = closedPresence(1, TERMINATION_DATE, "VOL");
        Contract contract = new Contract(100L, "IND", "FT1", START_DATE, TERMINATION_DATE);
        LaborClassification lc = new LaborClassification(100L, "AGR", "CAT", START_DATE, TERMINATION_DATE);
        WorkCenter wc = new WorkCenter(20L, 100L, 1, "WC1", START_DATE, TERMINATION_DATE, NOW, NOW);
        WorkingTime wt = WorkingTime.rehydrate(30L, 100L, 1, START_DATE, TERMINATION_DATE,
                new BigDecimal("75"),
                new WorkingTimeDerivedHours(new BigDecimal("30"), new BigDecimal("6"), new BigDecimal("130")),
                NOW, NOW);

        when(listPresences.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(presence));
        when(listContracts.listByEmployeeBusinessKey(any())).thenReturn(List.of(contract));
        when(listLaborClassifications.listByEmployeeBusinessKey(any())).thenReturn(List.of(lc));
        when(listWorkCenters.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(wc));
        when(listWorkingTimes.listByEmployeeBusinessKey(any())).thenReturn(List.of(wt));

        TerminationContext ctx = validator.validateAndLookup(cmd("ESP", "INTERNAL", "EMP001"));

        assertTrue(ctx.isAlreadyTerminated());
        TerminateEmployeeResult result = ctx.reconstructIdempotentResult();
        assertEquals("ESP", result.ruleSystemCode());
        assertEquals("TERMINATED", result.status());
        assertEquals(1, result.closedPresenceNumber());
        assertEquals("IND", result.closedContractTypeCode());
        assertEquals("AGR", result.closedAgreementCode());
        assertEquals(1, result.closedWorkCenterAssignmentNumber());
        assertEquals(1, result.closedWorkingTimeNumber());
    }

    @Test
    void idempotentContextFiltersRecordsByTerminationDate() {
        Employee terminated = terminatedEmployee();
        when(getEmployeeByBusinessKey.getByBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(terminated));

        // Two presences — only one matches terminationDate + exitReasonCode
        Presence wrong = closedPresence(1, LocalDate.of(2025, 1, 1), "VOL");
        Presence right = closedPresence(2, TERMINATION_DATE, "VOL");
        when(listPresences.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(wrong, right));
        when(listContracts.listByEmployeeBusinessKey(any())).thenReturn(List.of());
        when(listLaborClassifications.listByEmployeeBusinessKey(any())).thenReturn(List.of());
        when(listWorkCenters.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of());
        when(listWorkingTimes.listByEmployeeBusinessKey(any())).thenReturn(List.of());

        TerminationContext ctx = validator.validateAndLookup(cmd("ESP", "INTERNAL", "EMP001"));

        assertEquals(2, ctx.reconstructIdempotentResult().closedPresenceNumber());
    }

    // --- helpers ---

    private TerminateEmployeeCommand cmd(String rs, String et, String en) {
        return new TerminateEmployeeCommand(rs, et, en, TERMINATION_DATE, "VOL");
    }

    private Employee activeEmployee() {
        return new Employee(1L, "ESP", "INTERNAL", "EMP001",
                "Ana", "Lopez", null, null,
                "ACTIVE", NOW, NOW, null);
    }

    private Employee terminatedEmployee() {
        return new Employee(1L, "ESP", "INTERNAL", "EMP001",
                "Ana", "Lopez", null, null,
                "TERMINATED", NOW, NOW, null);
    }

    private Presence closedPresence(int number, LocalDate endDate, String exitReason) {
        return new Presence(10L, 100L, number, "COMP", "HIRE", exitReason,
                START_DATE, endDate, NOW, NOW);
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL (class not found)**

Run: `mvn test -Dtest=TerminationPreConditionValidatorTest -pl . -q`
Expected: compilation error — `TerminationPreConditionValidator` does not exist

- [ ] **Step 3: Create TerminationPreConditionValidator**

```java
package com.b4rrhh.employee.lifecycle.application.service;

import com.b4rrhh.employee.contract.application.command.ListEmployeeContractsCommand;
import com.b4rrhh.employee.contract.application.usecase.ListEmployeeContractsUseCase;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.labor_classification.application.command.ListEmployeeLaborClassificationsCommand;
import com.b4rrhh.employee.labor_classification.application.usecase.ListEmployeeLaborClassificationsUseCase;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.TerminateEmployeeResult;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeEmployeeNotFoundException;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeRequestInvalidException;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.workcenter.application.usecase.ListEmployeeWorkCentersUseCase;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesCommand;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesUseCase;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class TerminationPreConditionValidator {

    private final GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKey;
    private final ListEmployeePresencesUseCase listPresences;
    private final ListEmployeeContractsUseCase listContracts;
    private final ListEmployeeLaborClassificationsUseCase listLaborClassifications;
    private final ListEmployeeWorkCentersUseCase listWorkCenters;
    private final ListEmployeeWorkingTimesUseCase listWorkingTimes;

    public TerminationPreConditionValidator(
            GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKey,
            ListEmployeePresencesUseCase listPresences,
            ListEmployeeContractsUseCase listContracts,
            ListEmployeeLaborClassificationsUseCase listLaborClassifications,
            ListEmployeeWorkCentersUseCase listWorkCenters,
            ListEmployeeWorkingTimesUseCase listWorkingTimes) {
        this.getEmployeeByBusinessKey = getEmployeeByBusinessKey;
        this.listPresences = listPresences;
        this.listContracts = listContracts;
        this.listLaborClassifications = listLaborClassifications;
        this.listWorkCenters = listWorkCenters;
        this.listWorkingTimes = listWorkingTimes;
    }

    public TerminationContext validateAndLookup(TerminateEmployeeCommand command) {
        String ruleSystemCode = requireNonBlank(command.ruleSystemCode(), "ruleSystemCode");
        String employeeTypeCode = requireNonBlank(command.employeeTypeCode(), "employeeTypeCode");
        String employeeNumber = requireNonBlank(command.employeeNumber(), "employeeNumber");
        if (command.terminationDate() == null) {
            throw new TerminateEmployeeRequestInvalidException("terminationDate is required");
        }
        LocalDate terminationDate = command.terminationDate();
        String exitReasonCode = requireNonBlank(command.exitReasonCode(), "exitReasonCode");

        Employee employee = getEmployeeByBusinessKey
                .getByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .orElseThrow(() -> new TerminateEmployeeEmployeeNotFoundException(
                        ruleSystemCode, employeeTypeCode, employeeNumber));

        if (employee.isTerminated()) {
            return buildIdempotentContext(ruleSystemCode, employeeTypeCode, employeeNumber,
                    terminationDate, exitReasonCode, employee);
        }

        return new TerminationContext(ruleSystemCode, employeeTypeCode, employeeNumber,
                terminationDate, exitReasonCode, employee);
    }

    private TerminationContext buildIdempotentContext(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate terminationDate,
            String exitReasonCode,
            Employee employee) {

        List<Presence> presences = listPresences
                .listByEmployeeBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber);
        Presence closedPresence = presences.stream()
                .filter(p -> terminationDate.equals(p.getEndDate())
                        && exitReasonCode.equals(p.getExitReasonCode()))
                .findFirst().orElse(null);

        List<Contract> contracts = listContracts
                .listByEmployeeBusinessKey(new ListEmployeeContractsCommand(
                        ruleSystemCode, employeeTypeCode, employeeNumber));
        Contract closedContract = contracts.stream()
                .filter(c -> terminationDate.equals(c.getEndDate()))
                .findFirst().orElse(null);

        List<LaborClassification> laborClassifications = listLaborClassifications
                .listByEmployeeBusinessKey(new ListEmployeeLaborClassificationsCommand(
                        ruleSystemCode, employeeTypeCode, employeeNumber));
        LaborClassification closedLaborClassification = laborClassifications.stream()
                .filter(lc -> terminationDate.equals(lc.getEndDate()))
                .findFirst().orElse(null);

        List<WorkCenter> workCenters = listWorkCenters
                .listByEmployeeBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber);
        WorkCenter closedWorkCenter = workCenters.stream()
                .filter(wc -> terminationDate.equals(wc.getEndDate()))
                .findFirst().orElse(null);

        List<WorkingTime> workingTimes = listWorkingTimes
                .listByEmployeeBusinessKey(new ListEmployeeWorkingTimesCommand(
                        ruleSystemCode, employeeTypeCode, employeeNumber));
        WorkingTime closedWorkingTime = workingTimes.stream()
                .filter(wt -> terminationDate.equals(wt.getEndDate()))
                .findFirst().orElse(null);

        TerminateEmployeeResult idempotentResult = new TerminateEmployeeResult(
                ruleSystemCode, employeeTypeCode, employeeNumber,
                terminationDate, exitReasonCode, "TERMINATED",
                closedPresence != null ? closedPresence.getPresenceNumber() : null,
                closedPresence != null ? closedPresence.getCompanyCode() : null,
                closedPresence != null ? closedPresence.getEntryReasonCode() : null,
                closedPresence != null ? closedPresence.getExitReasonCode() : null,
                closedPresence != null ? closedPresence.getStartDate() : null,
                closedPresence != null ? closedPresence.getEndDate() : null,
                closedContract != null ? closedContract.getContractCode() : null,
                closedContract != null ? closedContract.getContractSubtypeCode() : null,
                closedContract != null ? closedContract.getStartDate() : null,
                closedContract != null ? closedContract.getEndDate() : null,
                closedLaborClassification != null ? closedLaborClassification.getAgreementCode() : null,
                closedLaborClassification != null ? closedLaborClassification.getAgreementCategoryCode() : null,
                closedLaborClassification != null ? closedLaborClassification.getStartDate() : null,
                closedLaborClassification != null ? closedLaborClassification.getEndDate() : null,
                closedWorkCenter != null ? closedWorkCenter.getWorkCenterAssignmentNumber() : null,
                closedWorkCenter != null ? closedWorkCenter.getWorkCenterCode() : null,
                closedWorkCenter != null ? closedWorkCenter.getStartDate() : null,
                closedWorkCenter != null ? closedWorkCenter.getEndDate() : null,
                closedWorkingTime != null ? closedWorkingTime.getWorkingTimeNumber() : null,
                closedWorkingTime != null ? closedWorkingTime.getWorkingTimePercentage() : null,
                closedWorkingTime != null ? closedWorkingTime.getWeeklyHours() : null,
                closedWorkingTime != null ? closedWorkingTime.getDailyHours() : null,
                closedWorkingTime != null ? closedWorkingTime.getMonthlyHours() : null,
                closedWorkingTime != null ? closedWorkingTime.getStartDate() : null,
                closedWorkingTime != null ? closedWorkingTime.getEndDate() : null
        );

        return new TerminationContext(ruleSystemCode, employeeTypeCode, employeeNumber,
                terminationDate, exitReasonCode, employee, idempotentResult);
    }

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new TerminateEmployeeRequestInvalidException(fieldName + " is required");
        }
        return value.trim().toUpperCase();
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

Run: `mvn test -Dtest=TerminationPreConditionValidatorTest -pl . -q`
Expected: BUILD SUCCESS, 9 tests passing

- [ ] **Step 5: Commit**

```
git add src/main/java/com/b4rrhh/employee/lifecycle/application/service/TerminationPreConditionValidator.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/service/TerminationPreConditionValidatorTest.java
git commit -m "feat(terminate): add TerminationPreConditionValidator with idempotency detection"
```

---

## Task 3: WorkingTimeTerminationParticipant (order=10)

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/WorkingTimeTerminationParticipant.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/WorkingTimeTerminationParticipantTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import com.b4rrhh.employee.working_time.application.usecase.CloseWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CloseWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesCommand;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesUseCase;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeAlreadyClosedException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.working_time.domain.model.WorkingTimeDerivedHours;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkingTimeTerminationParticipantTest {

    @Mock private ListEmployeeWorkingTimesUseCase listWorkingTimes;
    @Mock private CloseWorkingTimeUseCase closeWorkingTime;

    private static final LocalDate TERMINATION_DATE = LocalDate.of(2026, 3, 31);
    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Test
    void orderIs10() {
        assertEquals(10, participant().order());
    }

    @Test
    void closesActiveWorkingTimeAndStoresInContext() {
        WorkingTime active = workingTime(1, START_DATE, null);
        WorkingTime closed = workingTime(1, START_DATE, TERMINATION_DATE);
        when(listWorkingTimes.listByEmployeeBusinessKey(any())).thenReturn(List.of(active));
        when(closeWorkingTime.close(any())).thenReturn(closed);
        TerminationContext ctx = context();

        participant().participate(ctx);

        ArgumentCaptor<CloseWorkingTimeCommand> captor =
                ArgumentCaptor.forClass(CloseWorkingTimeCommand.class);
        verify(closeWorkingTime).close(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals(1, captor.getValue().workingTimeNumber());
        assertEquals(TERMINATION_DATE, captor.getValue().endDate());
        assertSame(closed, ctx.closedWorkingTime());
    }

    @Test
    void skipsWhenNoActiveWorkingTime() {
        WorkingTime onlyClosed = workingTime(1, START_DATE, LocalDate.of(2025, 12, 31));
        when(listWorkingTimes.listByEmployeeBusinessKey(any())).thenReturn(List.of(onlyClosed));

        participant().participate(context());

        verify(closeWorkingTime, never()).close(any());
        assertNull(context().closedWorkingTime());
    }

    @Test
    void skipsWhenActiveStartDateIsAfterTerminationDate() {
        WorkingTime future = workingTime(1, LocalDate.of(2026, 4, 1), null);
        when(listWorkingTimes.listByEmployeeBusinessKey(any())).thenReturn(List.of(future));

        participant().participate(context());

        verify(closeWorkingTime, never()).close(any());
    }

    @Test
    void throwsWhenMultipleActiveWorkingTimes() {
        WorkingTime wt1 = workingTime(1, START_DATE, null);
        WorkingTime wt2 = workingTime(2, START_DATE, null);
        when(listWorkingTimes.listByEmployeeBusinessKey(any())).thenReturn(List.of(wt1, wt2));

        assertThrows(TerminateEmployeeConflictException.class,
                () -> participant().participate(context()));
    }

    @Test
    void translatesWorkingTimeAlreadyClosedToConflictException() {
        WorkingTime active = workingTime(1, START_DATE, null);
        when(listWorkingTimes.listByEmployeeBusinessKey(any())).thenReturn(List.of(active));
        when(closeWorkingTime.close(any()))
                .thenThrow(new WorkingTimeAlreadyClosedException("already closed"));

        TerminateEmployeeConflictException ex = assertThrows(
                TerminateEmployeeConflictException.class,
                () -> participant().participate(context()));
        assertNotNull(ex.getCause());
    }

    // --- helpers ---

    private WorkingTimeTerminationParticipant participant() {
        return new WorkingTimeTerminationParticipant(listWorkingTimes, closeWorkingTime);
    }

    private TerminationContext context() {
        TerminationContext ctx = mock(TerminationContext.class);
        when(ctx.ruleSystemCode()).thenReturn("ESP");
        when(ctx.employeeTypeCode()).thenReturn("INTERNAL");
        when(ctx.employeeNumber()).thenReturn("EMP001");
        when(ctx.terminationDate()).thenReturn(TERMINATION_DATE);
        return ctx;
    }

    private WorkingTime workingTime(int number, LocalDate startDate, LocalDate endDate) {
        return WorkingTime.rehydrate(
                (long) number, 100L, number, startDate, endDate,
                new BigDecimal("75"),
                new WorkingTimeDerivedHours(
                        new BigDecimal("30"), new BigDecimal("6"), new BigDecimal("130")),
                NOW, NOW);
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL**

Run: `mvn test -Dtest=WorkingTimeTerminationParticipantTest -pl . -q`
Expected: compilation error

- [ ] **Step 3: Create WorkingTimeTerminationParticipant**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import com.b4rrhh.employee.working_time.application.usecase.CloseWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CloseWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesCommand;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesUseCase;
import com.b4rrhh.employee.working_time.domain.exception.InvalidWorkingTimeDateRangeException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeAlreadyClosedException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNotFoundException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOutsidePresencePeriodException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkingTimeTerminationParticipant implements TerminationParticipant {

    private final ListEmployeeWorkingTimesUseCase listWorkingTimes;
    private final CloseWorkingTimeUseCase closeWorkingTime;

    public WorkingTimeTerminationParticipant(
            ListEmployeeWorkingTimesUseCase listWorkingTimes,
            CloseWorkingTimeUseCase closeWorkingTime) {
        this.listWorkingTimes = listWorkingTimes;
        this.closeWorkingTime = closeWorkingTime;
    }

    @Override
    public int order() { return 10; }

    @Override
    public void participate(TerminationContext ctx) {
        List<WorkingTime> all = listWorkingTimes.listByEmployeeBusinessKey(
                new ListEmployeeWorkingTimesCommand(
                        ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber()));

        List<WorkingTime> active = all.stream()
                .filter(wt -> wt.getEndDate() == null)
                .toList();

        if (active.size() > 1) {
            throw new TerminateEmployeeConflictException(
                    "Multiple active working times found for employee " + ctx.employeeNumber());
        }
        if (active.isEmpty()) return;

        WorkingTime activeWt = active.get(0);
        if (activeWt.getStartDate().isAfter(ctx.terminationDate())) return;

        try {
            WorkingTime closed = closeWorkingTime.close(new CloseWorkingTimeCommand(
                    ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                    activeWt.getWorkingTimeNumber(), ctx.terminationDate()));
            ctx.setClosedWorkingTime(closed);
        } catch (WorkingTimeAlreadyClosedException | WorkingTimeNotFoundException |
                 InvalidWorkingTimeDateRangeException | WorkingTimeOutsidePresencePeriodException e) {
            throw new TerminateEmployeeConflictException(e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

Run: `mvn test -Dtest=WorkingTimeTerminationParticipantTest -pl . -q`
Expected: BUILD SUCCESS, 6 tests passing

- [ ] **Step 5: Commit**

```
git add src/main/java/com/b4rrhh/employee/lifecycle/application/participant/WorkingTimeTerminationParticipant.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/participant/WorkingTimeTerminationParticipantTest.java
git commit -m "feat(terminate): add WorkingTimeTerminationParticipant (order=10)"
```

---

## Task 4: WorkCenterTerminationParticipant (order=20)

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/WorkCenterTerminationParticipant.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/WorkCenterTerminationParticipantTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import com.b4rrhh.employee.workcenter.application.usecase.CloseWorkCenterCommand;
import com.b4rrhh.employee.workcenter.application.usecase.CloseWorkCenterUseCase;
import com.b4rrhh.employee.workcenter.application.usecase.ListEmployeeWorkCentersUseCase;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterAlreadyClosedException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkCenterTerminationParticipantTest {

    @Mock private ListEmployeeWorkCentersUseCase listWorkCenters;
    @Mock private CloseWorkCenterUseCase closeWorkCenter;

    private static final LocalDate TERMINATION_DATE = LocalDate.of(2026, 3, 31);
    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Test
    void orderIs20() {
        assertEquals(20, participant().order());
    }

    @Test
    void closesActiveWorkCenterAndStoresInContext() {
        WorkCenter active = workCenter(1, START_DATE, null);
        WorkCenter closed = workCenter(1, START_DATE, TERMINATION_DATE);
        when(listWorkCenters.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(active));
        when(closeWorkCenter.close(any())).thenReturn(closed);
        TerminationContext ctx = context();

        participant().participate(ctx);

        ArgumentCaptor<CloseWorkCenterCommand> captor =
                ArgumentCaptor.forClass(CloseWorkCenterCommand.class);
        verify(closeWorkCenter).close(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals(1, captor.getValue().workCenterAssignmentNumber());
        assertEquals(TERMINATION_DATE, captor.getValue().endDate());
        assertSame(closed, ctx.closedWorkCenter());
    }

    @Test
    void skipsWhenNoActiveWorkCenter() {
        WorkCenter onlyClosed = workCenter(1, START_DATE, LocalDate.of(2025, 12, 31));
        when(listWorkCenters.listByEmployeeBusinessKey(any(), any(), any()))
                .thenReturn(List.of(onlyClosed));

        participant().participate(context());

        verify(closeWorkCenter, never()).close(any());
    }

    @Test
    void skipsWhenActiveStartDateIsAfterTerminationDate() {
        WorkCenter future = workCenter(1, LocalDate.of(2026, 4, 1), null);
        when(listWorkCenters.listByEmployeeBusinessKey(any(), any(), any()))
                .thenReturn(List.of(future));

        participant().participate(context());

        verify(closeWorkCenter, never()).close(any());
    }

    @Test
    void throwsWhenMultipleActiveWorkCenters() {
        WorkCenter wc1 = workCenter(1, START_DATE, null);
        WorkCenter wc2 = workCenter(2, START_DATE, null);
        when(listWorkCenters.listByEmployeeBusinessKey(any(), any(), any()))
                .thenReturn(List.of(wc1, wc2));

        assertThrows(TerminateEmployeeConflictException.class,
                () -> participant().participate(context()));
    }

    @Test
    void translatesWorkCenterAlreadyClosedToConflictException() {
        WorkCenter active = workCenter(1, START_DATE, null);
        when(listWorkCenters.listByEmployeeBusinessKey(any(), any(), any()))
                .thenReturn(List.of(active));
        when(closeWorkCenter.close(any()))
                .thenThrow(new WorkCenterAlreadyClosedException(1));

        TerminateEmployeeConflictException ex = assertThrows(
                TerminateEmployeeConflictException.class,
                () -> participant().participate(context()));
        assertNotNull(ex.getCause());
    }

    // --- helpers ---

    private WorkCenterTerminationParticipant participant() {
        return new WorkCenterTerminationParticipant(listWorkCenters, closeWorkCenter);
    }

    private TerminationContext context() {
        TerminationContext ctx = mock(TerminationContext.class);
        when(ctx.ruleSystemCode()).thenReturn("ESP");
        when(ctx.employeeTypeCode()).thenReturn("INTERNAL");
        when(ctx.employeeNumber()).thenReturn("EMP001");
        when(ctx.terminationDate()).thenReturn(TERMINATION_DATE);
        return ctx;
    }

    private WorkCenter workCenter(int number, LocalDate startDate, LocalDate endDate) {
        return new WorkCenter((long) number, 100L, number, "WC1", startDate, endDate, NOW, NOW);
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL**

Run: `mvn test -Dtest=WorkCenterTerminationParticipantTest -pl . -q`
Expected: compilation error

- [ ] **Step 3: Create WorkCenterTerminationParticipant**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import com.b4rrhh.employee.workcenter.application.usecase.CloseWorkCenterCommand;
import com.b4rrhh.employee.workcenter.application.usecase.CloseWorkCenterUseCase;
import com.b4rrhh.employee.workcenter.application.usecase.ListEmployeeWorkCentersUseCase;
import com.b4rrhh.employee.workcenter.domain.exception.InvalidWorkCenterDateRangeException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterAlreadyClosedException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterNotFoundException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkCenterTerminationParticipant implements TerminationParticipant {

    private final ListEmployeeWorkCentersUseCase listWorkCenters;
    private final CloseWorkCenterUseCase closeWorkCenter;

    public WorkCenterTerminationParticipant(
            ListEmployeeWorkCentersUseCase listWorkCenters,
            CloseWorkCenterUseCase closeWorkCenter) {
        this.listWorkCenters = listWorkCenters;
        this.closeWorkCenter = closeWorkCenter;
    }

    @Override
    public int order() { return 20; }

    @Override
    public void participate(TerminationContext ctx) {
        List<WorkCenter> all = listWorkCenters.listByEmployeeBusinessKey(
                ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber());

        List<WorkCenter> active = all.stream()
                .filter(wc -> wc.getEndDate() == null)
                .toList();

        if (active.size() > 1) {
            throw new TerminateEmployeeConflictException(
                    "Multiple active work centers found for employee " + ctx.employeeNumber());
        }
        if (active.isEmpty()) return;

        WorkCenter activeWc = active.get(0);
        if (activeWc.getStartDate().isAfter(ctx.terminationDate())) return;

        try {
            WorkCenter closed = closeWorkCenter.close(new CloseWorkCenterCommand(
                    ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                    activeWc.getWorkCenterAssignmentNumber(), ctx.terminationDate()));
            ctx.setClosedWorkCenter(closed);
        } catch (WorkCenterCatalogValueInvalidException e) {
            throw new TerminateEmployeeCatalogValueInvalidException(e.getMessage(), e);
        } catch (WorkCenterAlreadyClosedException | WorkCenterNotFoundException |
                 InvalidWorkCenterDateRangeException | WorkCenterOutsidePresencePeriodException e) {
            throw new TerminateEmployeeConflictException(e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

Run: `mvn test -Dtest=WorkCenterTerminationParticipantTest -pl . -q`
Expected: BUILD SUCCESS, 6 tests passing

- [ ] **Step 5: Commit**

```
git add src/main/java/com/b4rrhh/employee/lifecycle/application/participant/WorkCenterTerminationParticipant.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/participant/WorkCenterTerminationParticipantTest.java
git commit -m "feat(terminate): add WorkCenterTerminationParticipant (order=20)"
```

---

## Task 5: CostCenterTerminationParticipant (order=30)

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/CostCenterTerminationParticipant.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/CostCenterTerminationParticipantTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.cost_center.application.usecase.CloseActiveCostCenterDistributionAtTerminationUseCase;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CostCenterTerminationParticipantTest {

    @Mock private CloseActiveCostCenterDistributionAtTerminationUseCase closeIfPresent;

    private static final LocalDate TERMINATION_DATE = LocalDate.of(2026, 3, 31);

    @Test
    void orderIs30() {
        assertEquals(30, new CostCenterTerminationParticipant(closeIfPresent).order());
    }

    @Test
    void delegatesToCloseIfPresentWithCorrectArgs() {
        TerminationContext ctx = mock(TerminationContext.class);
        when(ctx.ruleSystemCode()).thenReturn("ESP");
        when(ctx.employeeTypeCode()).thenReturn("INTERNAL");
        when(ctx.employeeNumber()).thenReturn("EMP001");
        when(ctx.terminationDate()).thenReturn(TERMINATION_DATE);

        new CostCenterTerminationParticipant(closeIfPresent).participate(ctx);

        verify(closeIfPresent).closeIfPresent("ESP", "INTERNAL", "EMP001", TERMINATION_DATE);
    }

    @Test
    void doesNotSetAnyResultOnContext() {
        TerminationContext ctx = mock(TerminationContext.class);
        when(ctx.ruleSystemCode()).thenReturn("ESP");
        when(ctx.employeeTypeCode()).thenReturn("INTERNAL");
        when(ctx.employeeNumber()).thenReturn("EMP001");
        when(ctx.terminationDate()).thenReturn(TERMINATION_DATE);

        new CostCenterTerminationParticipant(closeIfPresent).participate(ctx);

        verify(ctx, never()).setClosedPresence(any());
        verify(ctx, never()).setClosedWorkCenter(any());
        verify(ctx, never()).setClosedContract(any());
        verify(ctx, never()).setClosedLaborClassification(any());
        verify(ctx, never()).setClosedWorkingTime(any());
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL**

Run: `mvn test -Dtest=CostCenterTerminationParticipantTest -pl . -q`
Expected: compilation error

- [ ] **Step 3: Create CostCenterTerminationParticipant**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.cost_center.application.usecase.CloseActiveCostCenterDistributionAtTerminationUseCase;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import org.springframework.stereotype.Component;

@Component
public class CostCenterTerminationParticipant implements TerminationParticipant {

    private final CloseActiveCostCenterDistributionAtTerminationUseCase closeIfPresent;

    public CostCenterTerminationParticipant(
            CloseActiveCostCenterDistributionAtTerminationUseCase closeIfPresent) {
        this.closeIfPresent = closeIfPresent;
    }

    @Override
    public int order() { return 30; }

    @Override
    public void participate(TerminationContext ctx) {
        closeIfPresent.closeIfPresent(
                ctx.ruleSystemCode(), ctx.employeeTypeCode(),
                ctx.employeeNumber(), ctx.terminationDate());
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

Run: `mvn test -Dtest=CostCenterTerminationParticipantTest -pl . -q`
Expected: BUILD SUCCESS, 3 tests passing

- [ ] **Step 5: Commit**

```
git add src/main/java/com/b4rrhh/employee/lifecycle/application/participant/CostCenterTerminationParticipant.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/participant/CostCenterTerminationParticipantTest.java
git commit -m "feat(terminate): add CostCenterTerminationParticipant (order=30)"
```

---

## Task 6: ContractTerminationParticipant (order=40)

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/ContractTerminationParticipant.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/ContractTerminationParticipantTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.contract.application.command.CloseContractCommand;
import com.b4rrhh.employee.contract.application.command.ListEmployeeContractsCommand;
import com.b4rrhh.employee.contract.application.usecase.CloseContractUseCase;
import com.b4rrhh.employee.contract.application.usecase.ListEmployeeContractsUseCase;
import com.b4rrhh.employee.contract.domain.exception.ContractAlreadyClosedException;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractTerminationParticipantTest {

    @Mock private ListEmployeeContractsUseCase listContracts;
    @Mock private CloseContractUseCase closeContract;

    private static final LocalDate TERMINATION_DATE = LocalDate.of(2026, 3, 31);
    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);

    @Test
    void orderIs40() {
        assertEquals(40, participant().order());
    }

    @Test
    void closesActiveContractAndStoresInContext() {
        Contract active = new Contract(100L, "IND", "FT1", START_DATE, null);
        Contract closed = new Contract(100L, "IND", "FT1", START_DATE, TERMINATION_DATE);
        when(listContracts.listByEmployeeBusinessKey(any())).thenReturn(List.of(active));
        when(closeContract.close(any())).thenReturn(closed);
        TerminationContext ctx = context();

        participant().participate(ctx);

        ArgumentCaptor<CloseContractCommand> captor =
                ArgumentCaptor.forClass(CloseContractCommand.class);
        verify(closeContract).close(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals(START_DATE, captor.getValue().startDate());
        assertEquals(TERMINATION_DATE, captor.getValue().endDate());
        assertSame(closed, ctx.closedContract());
    }

    @Test
    void skipsWhenNoActiveContract() {
        Contract onlyClosed = new Contract(100L, "IND", "FT1", START_DATE, LocalDate.of(2025, 12, 31));
        when(listContracts.listByEmployeeBusinessKey(any())).thenReturn(List.of(onlyClosed));

        participant().participate(context());

        verify(closeContract, never()).close(any());
    }

    @Test
    void skipsWhenActiveStartDateIsAfterTerminationDate() {
        Contract future = new Contract(100L, "IND", "FT1", LocalDate.of(2026, 4, 1), null);
        when(listContracts.listByEmployeeBusinessKey(any())).thenReturn(List.of(future));

        participant().participate(context());

        verify(closeContract, never()).close(any());
    }

    @Test
    void throwsWhenMultipleActiveContracts() {
        Contract c1 = new Contract(100L, "IND", "FT1", START_DATE, null);
        Contract c2 = new Contract(100L, "IND", "FT2", LocalDate.of(2026, 2, 1), null);
        when(listContracts.listByEmployeeBusinessKey(any())).thenReturn(List.of(c1, c2));

        assertThrows(TerminateEmployeeConflictException.class,
                () -> participant().participate(context()));
    }

    @Test
    void translatesContractAlreadyClosedToConflictException() {
        Contract active = new Contract(100L, "IND", "FT1", START_DATE, null);
        when(listContracts.listByEmployeeBusinessKey(any())).thenReturn(List.of(active));
        when(closeContract.close(any()))
                .thenThrow(new ContractAlreadyClosedException(START_DATE));

        TerminateEmployeeConflictException ex = assertThrows(
                TerminateEmployeeConflictException.class,
                () -> participant().participate(context()));
        assertNotNull(ex.getCause());
    }

    // --- helpers ---

    private ContractTerminationParticipant participant() {
        return new ContractTerminationParticipant(listContracts, closeContract);
    }

    private TerminationContext context() {
        TerminationContext ctx = mock(TerminationContext.class);
        when(ctx.ruleSystemCode()).thenReturn("ESP");
        when(ctx.employeeTypeCode()).thenReturn("INTERNAL");
        when(ctx.employeeNumber()).thenReturn("EMP001");
        when(ctx.terminationDate()).thenReturn(TERMINATION_DATE);
        return ctx;
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL**

Run: `mvn test -Dtest=ContractTerminationParticipantTest -pl . -q`
Expected: compilation error

- [ ] **Step 3: Create ContractTerminationParticipant**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.contract.application.command.CloseContractCommand;
import com.b4rrhh.employee.contract.application.command.ListEmployeeContractsCommand;
import com.b4rrhh.employee.contract.application.usecase.CloseContractUseCase;
import com.b4rrhh.employee.contract.application.usecase.ListEmployeeContractsUseCase;
import com.b4rrhh.employee.contract.domain.exception.ContractAlreadyClosedException;
import com.b4rrhh.employee.contract.domain.exception.ContractCoverageIncompleteException;
import com.b4rrhh.employee.contract.domain.exception.ContractNotFoundException;
import com.b4rrhh.employee.contract.domain.exception.ContractOutsidePresencePeriodException;
import com.b4rrhh.employee.contract.domain.exception.InvalidContractDateRangeException;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContractTerminationParticipant implements TerminationParticipant {

    private final ListEmployeeContractsUseCase listContracts;
    private final CloseContractUseCase closeContract;

    public ContractTerminationParticipant(
            ListEmployeeContractsUseCase listContracts,
            CloseContractUseCase closeContract) {
        this.listContracts = listContracts;
        this.closeContract = closeContract;
    }

    @Override
    public int order() { return 40; }

    @Override
    public void participate(TerminationContext ctx) {
        List<Contract> all = listContracts.listByEmployeeBusinessKey(
                new ListEmployeeContractsCommand(
                        ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber()));

        List<Contract> active = all.stream()
                .filter(c -> c.getEndDate() == null)
                .toList();

        if (active.size() > 1) {
            throw new TerminateEmployeeConflictException(
                    "Multiple active contracts found for employee " + ctx.employeeNumber());
        }
        if (active.isEmpty()) return;

        Contract activeContract = active.get(0);
        if (activeContract.getStartDate().isAfter(ctx.terminationDate())) return;

        try {
            Contract closed = closeContract.close(new CloseContractCommand(
                    ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                    activeContract.getStartDate(), ctx.terminationDate()));
            ctx.setClosedContract(closed);
        } catch (ContractAlreadyClosedException | ContractNotFoundException |
                 InvalidContractDateRangeException | ContractOutsidePresencePeriodException |
                 ContractCoverageIncompleteException e) {
            throw new TerminateEmployeeConflictException(e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

Run: `mvn test -Dtest=ContractTerminationParticipantTest -pl . -q`
Expected: BUILD SUCCESS, 6 tests passing

- [ ] **Step 5: Commit**

```
git add src/main/java/com/b4rrhh/employee/lifecycle/application/participant/ContractTerminationParticipant.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/participant/ContractTerminationParticipantTest.java
git commit -m "feat(terminate): add ContractTerminationParticipant (order=40)"
```

---

## Task 7: LaborClassificationTerminationParticipant (order=50)

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/LaborClassificationTerminationParticipant.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/LaborClassificationTerminationParticipantTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.labor_classification.application.command.CloseLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.command.ListEmployeeLaborClassificationsCommand;
import com.b4rrhh.employee.labor_classification.application.usecase.CloseLaborClassificationUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.ListEmployeeLaborClassificationsUseCase;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAlreadyClosedException;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LaborClassificationTerminationParticipantTest {

    @Mock private ListEmployeeLaborClassificationsUseCase listLaborClassifications;
    @Mock private CloseLaborClassificationUseCase closeLaborClassification;

    private static final LocalDate TERMINATION_DATE = LocalDate.of(2026, 3, 31);
    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);

    @Test
    void orderIs50() {
        assertEquals(50, participant().order());
    }

    @Test
    void closesActiveLaborClassificationAndStoresInContext() {
        LaborClassification active = new LaborClassification(100L, "AGR", "CAT", START_DATE, null);
        LaborClassification closed = new LaborClassification(100L, "AGR", "CAT", START_DATE, TERMINATION_DATE);
        when(listLaborClassifications.listByEmployeeBusinessKey(any())).thenReturn(List.of(active));
        when(closeLaborClassification.close(any())).thenReturn(closed);
        TerminationContext ctx = context();

        participant().participate(ctx);

        ArgumentCaptor<CloseLaborClassificationCommand> captor =
                ArgumentCaptor.forClass(CloseLaborClassificationCommand.class);
        verify(closeLaborClassification).close(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals(START_DATE, captor.getValue().startDate());
        assertEquals(TERMINATION_DATE, captor.getValue().endDate());
        assertSame(closed, ctx.closedLaborClassification());
    }

    @Test
    void skipsWhenNoActiveLaborClassification() {
        LaborClassification onlyClosed = new LaborClassification(100L, "AGR", "CAT", START_DATE, LocalDate.of(2025, 12, 31));
        when(listLaborClassifications.listByEmployeeBusinessKey(any())).thenReturn(List.of(onlyClosed));

        participant().participate(context());

        verify(closeLaborClassification, never()).close(any());
    }

    @Test
    void skipsWhenActiveStartDateIsAfterTerminationDate() {
        LaborClassification future = new LaborClassification(100L, "AGR", "CAT", LocalDate.of(2026, 4, 1), null);
        when(listLaborClassifications.listByEmployeeBusinessKey(any())).thenReturn(List.of(future));

        participant().participate(context());

        verify(closeLaborClassification, never()).close(any());
    }

    @Test
    void throwsWhenMultipleActiveLaborClassifications() {
        LaborClassification lc1 = new LaborClassification(100L, "AGR", "CAT1", START_DATE, null);
        LaborClassification lc2 = new LaborClassification(100L, "AGR", "CAT2", LocalDate.of(2026, 2, 1), null);
        when(listLaborClassifications.listByEmployeeBusinessKey(any())).thenReturn(List.of(lc1, lc2));

        assertThrows(TerminateEmployeeConflictException.class,
                () -> participant().participate(context()));
    }

    @Test
    void translatesLaborClassificationAlreadyClosedToConflictException() {
        LaborClassification active = new LaborClassification(100L, "AGR", "CAT", START_DATE, null);
        when(listLaborClassifications.listByEmployeeBusinessKey(any())).thenReturn(List.of(active));
        when(closeLaborClassification.close(any()))
                .thenThrow(new LaborClassificationAlreadyClosedException(START_DATE));

        TerminateEmployeeConflictException ex = assertThrows(
                TerminateEmployeeConflictException.class,
                () -> participant().participate(context()));
        assertNotNull(ex.getCause());
    }

    // --- helpers ---

    private LaborClassificationTerminationParticipant participant() {
        return new LaborClassificationTerminationParticipant(listLaborClassifications, closeLaborClassification);
    }

    private TerminationContext context() {
        TerminationContext ctx = mock(TerminationContext.class);
        when(ctx.ruleSystemCode()).thenReturn("ESP");
        when(ctx.employeeTypeCode()).thenReturn("INTERNAL");
        when(ctx.employeeNumber()).thenReturn("EMP001");
        when(ctx.terminationDate()).thenReturn(TERMINATION_DATE);
        return ctx;
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL**

Run: `mvn test -Dtest=LaborClassificationTerminationParticipantTest -pl . -q`
Expected: compilation error

- [ ] **Step 3: Create LaborClassificationTerminationParticipant**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.labor_classification.application.command.CloseLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.command.ListEmployeeLaborClassificationsCommand;
import com.b4rrhh.employee.labor_classification.application.usecase.CloseLaborClassificationUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.ListEmployeeLaborClassificationsUseCase;
import com.b4rrhh.employee.labor_classification.domain.exception.InvalidLaborClassificationDateRangeException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAlreadyClosedException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationCoverageIncompleteException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationNotFoundException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationOutsidePresencePeriodException;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LaborClassificationTerminationParticipant implements TerminationParticipant {

    private final ListEmployeeLaborClassificationsUseCase listLaborClassifications;
    private final CloseLaborClassificationUseCase closeLaborClassification;

    public LaborClassificationTerminationParticipant(
            ListEmployeeLaborClassificationsUseCase listLaborClassifications,
            CloseLaborClassificationUseCase closeLaborClassification) {
        this.listLaborClassifications = listLaborClassifications;
        this.closeLaborClassification = closeLaborClassification;
    }

    @Override
    public int order() { return 50; }

    @Override
    public void participate(TerminationContext ctx) {
        List<LaborClassification> all = listLaborClassifications.listByEmployeeBusinessKey(
                new ListEmployeeLaborClassificationsCommand(
                        ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber()));

        List<LaborClassification> active = all.stream()
                .filter(lc -> lc.getEndDate() == null)
                .toList();

        if (active.size() > 1) {
            throw new TerminateEmployeeConflictException(
                    "Multiple active labor classifications found for employee " + ctx.employeeNumber());
        }
        if (active.isEmpty()) return;

        LaborClassification activeLc = active.get(0);
        if (activeLc.getStartDate().isAfter(ctx.terminationDate())) return;

        try {
            LaborClassification closed = closeLaborClassification.close(new CloseLaborClassificationCommand(
                    ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                    activeLc.getStartDate(), ctx.terminationDate()));
            ctx.setClosedLaborClassification(closed);
        } catch (LaborClassificationAlreadyClosedException | LaborClassificationNotFoundException |
                 InvalidLaborClassificationDateRangeException | LaborClassificationOutsidePresencePeriodException |
                 LaborClassificationCoverageIncompleteException e) {
            throw new TerminateEmployeeConflictException(e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

Run: `mvn test -Dtest=LaborClassificationTerminationParticipantTest -pl . -q`
Expected: BUILD SUCCESS, 6 tests passing

- [ ] **Step 5: Commit**

```
git add src/main/java/com/b4rrhh/employee/lifecycle/application/participant/LaborClassificationTerminationParticipant.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/participant/LaborClassificationTerminationParticipantTest.java
git commit -m "feat(terminate): add LaborClassificationTerminationParticipant (order=50)"
```

---

## Task 8: PresenceTerminationParticipant (order=60)

**Files:**
- Create: `src/main/java/com/b4rrhh/employee/lifecycle/application/participant/PresenceTerminationParticipant.java`
- Create: `src/test/java/com/b4rrhh/employee/lifecycle/application/participant/PresenceTerminationParticipantTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import com.b4rrhh.employee.presence.application.usecase.ClosePresenceCommand;
import com.b4rrhh.employee.presence.application.usecase.ClosePresenceUseCase;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.exception.PresenceCatalogValueInvalidException;
import com.b4rrhh.employee.presence.domain.exception.PresenceNotFoundException;
import com.b4rrhh.employee.presence.domain.model.Presence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceTerminationParticipantTest {

    @Mock private ListEmployeePresencesUseCase listPresences;
    @Mock private ClosePresenceUseCase closePresence;

    private static final LocalDate TERMINATION_DATE = LocalDate.of(2026, 3, 31);
    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Test
    void orderIs60() {
        assertEquals(60, participant().order());
    }

    @Test
    void closesActivePresenceAndStoresInContext() {
        Presence active = presence(1, START_DATE, null, null);
        Presence closed = presence(1, START_DATE, TERMINATION_DATE, "VOL");
        when(listPresences.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(active));
        when(closePresence.close(any())).thenReturn(closed);
        TerminationContext ctx = context();

        participant().participate(ctx);

        ArgumentCaptor<ClosePresenceCommand> captor =
                ArgumentCaptor.forClass(ClosePresenceCommand.class);
        verify(closePresence).close(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals(1, captor.getValue().presenceNumber());
        assertEquals(TERMINATION_DATE, captor.getValue().endDate());
        assertEquals("VOL", captor.getValue().exitReasonCode());
        assertSame(closed, ctx.closedPresence());
    }

    @Test
    void throwsWhenNoActivePresenceFound() {
        Presence onlyClosed = presence(1, START_DATE, LocalDate.of(2025, 12, 31), "FIN");
        when(listPresences.listByEmployeeBusinessKey(any(), any(), any()))
                .thenReturn(List.of(onlyClosed));

        assertThrows(TerminateEmployeeConflictException.class,
                () -> participant().participate(context()));
        verify(closePresence, never()).close(any());
    }

    @Test
    void deduplicatesByPresenceNumberPreferringClosedOverActive() {
        // Two records with presenceNumber=1: one active, one closed → choose closed for dedup
        Presence closed = presence(1, START_DATE, LocalDate.of(2025, 6, 30), "FIN");
        Presence active = presence(1, START_DATE, null, null);
        // presenceNumber=2 is the actual active one
        Presence active2 = presence(2, LocalDate.of(2026, 1, 1), null, null);
        Presence closedActive2 = presence(2, LocalDate.of(2026, 1, 1), TERMINATION_DATE, "VOL");

        when(listPresences.listByEmployeeBusinessKey(any(), any(), any()))
                .thenReturn(List.of(closed, active, active2));
        when(closePresence.close(any())).thenReturn(closedActive2);

        participant().participate(context());

        // Only presenceNumber=2 should be closed (presenceNumber=1 was deduped to closed record which is not active)
        ArgumentCaptor<ClosePresenceCommand> captor =
                ArgumentCaptor.forClass(ClosePresenceCommand.class);
        verify(closePresence).close(captor.capture());
        assertEquals(2, captor.getValue().presenceNumber());
    }

    @Test
    void translatesCatalogExceptionToCatalogInvalidException() {
        Presence active = presence(1, START_DATE, null, null);
        when(listPresences.listByEmployeeBusinessKey(any(), any(), any()))
                .thenReturn(List.of(active));
        when(closePresence.close(any()))
                .thenThrow(new PresenceCatalogValueInvalidException("invalid exit reason"));

        assertThrows(TerminateEmployeeCatalogValueInvalidException.class,
                () -> participant().participate(context()));
    }

    @Test
    void translatesDomainExceptionToConflictException() {
        Presence active = presence(1, START_DATE, null, null);
        when(listPresences.listByEmployeeBusinessKey(any(), any(), any()))
                .thenReturn(List.of(active));
        when(closePresence.close(any()))
                .thenThrow(new PresenceNotFoundException("not found"));

        TerminateEmployeeConflictException ex = assertThrows(
                TerminateEmployeeConflictException.class,
                () -> participant().participate(context()));
        assertNotNull(ex.getCause());
    }

    // --- helpers ---

    private PresenceTerminationParticipant participant() {
        return new PresenceTerminationParticipant(listPresences, closePresence);
    }

    private TerminationContext context() {
        TerminationContext ctx = mock(TerminationContext.class);
        when(ctx.ruleSystemCode()).thenReturn("ESP");
        when(ctx.employeeTypeCode()).thenReturn("INTERNAL");
        when(ctx.employeeNumber()).thenReturn("EMP001");
        when(ctx.terminationDate()).thenReturn(TERMINATION_DATE);
        when(ctx.exitReasonCode()).thenReturn("VOL");
        return ctx;
    }

    private Presence presence(int number, LocalDate startDate, LocalDate endDate, String exitReason) {
        return new Presence(10L, 100L, number, "COMP", "HIRE", exitReason, startDate, endDate, NOW, NOW);
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL**

Run: `mvn test -Dtest=PresenceTerminationParticipantTest -pl . -q`
Expected: compilation error

- [ ] **Step 3: Create PresenceTerminationParticipant**

```java
package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import com.b4rrhh.employee.presence.application.usecase.ClosePresenceCommand;
import com.b4rrhh.employee.presence.application.usecase.ClosePresenceUseCase;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.exception.InvalidPresenceDateRangeException;
import com.b4rrhh.employee.presence.domain.exception.PresenceAlreadyClosedException;
import com.b4rrhh.employee.presence.domain.exception.PresenceCatalogValueInvalidException;
import com.b4rrhh.employee.presence.domain.exception.PresenceNotFoundException;
import com.b4rrhh.employee.presence.domain.model.Presence;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PresenceTerminationParticipant implements TerminationParticipant {

    private final ListEmployeePresencesUseCase listPresences;
    private final ClosePresenceUseCase closePresence;

    public PresenceTerminationParticipant(
            ListEmployeePresencesUseCase listPresences,
            ClosePresenceUseCase closePresence) {
        this.listPresences = listPresences;
        this.closePresence = closePresence;
    }

    @Override
    public int order() { return 60; }

    @Override
    public void participate(TerminationContext ctx) {
        List<Presence> all = listPresences.listByEmployeeBusinessKey(
                ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber());

        List<Presence> deduplicated = deduplicate(all);

        Presence activePresence = deduplicated.stream()
                .filter(p -> p.getEndDate() == null)
                .findFirst()
                .orElseThrow(() -> new TerminateEmployeeConflictException(
                        "No active presence found for employee " + ctx.employeeNumber()));

        try {
            Presence closed = closePresence.close(new ClosePresenceCommand(
                    ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                    activePresence.getPresenceNumber(), ctx.terminationDate(), ctx.exitReasonCode()));
            ctx.setClosedPresence(closed);
        } catch (PresenceCatalogValueInvalidException e) {
            throw new TerminateEmployeeCatalogValueInvalidException(e.getMessage(), e);
        } catch (PresenceAlreadyClosedException | PresenceNotFoundException |
                 InvalidPresenceDateRangeException e) {
            throw new TerminateEmployeeConflictException(e.getMessage(), e);
        }
    }

    private List<Presence> deduplicate(List<Presence> presences) {
        Map<Integer, Presence> byNumber = presences.stream().collect(
                Collectors.toMap(
                        Presence::getPresenceNumber,
                        p -> p,
                        (existing, candidate) -> {
                            // prefer closed over active
                            if (existing.getEndDate() == null && candidate.getEndDate() != null) return candidate;
                            if (existing.getEndDate() != null && candidate.getEndDate() == null) return existing;
                            // both closed — prefer latest endDate
                            if (existing.getEndDate() != null && candidate.getEndDate() != null) {
                                return existing.getEndDate().isAfter(candidate.getEndDate()) ? existing : candidate;
                            }
                            return existing;
                        }));
        return List.copyOf(byNumber.values());
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

Run: `mvn test -Dtest=PresenceTerminationParticipantTest -pl . -q`
Expected: BUILD SUCCESS, 6 tests passing

- [ ] **Step 5: Commit**

```
git add src/main/java/com/b4rrhh/employee/lifecycle/application/participant/PresenceTerminationParticipant.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/participant/PresenceTerminationParticipantTest.java
git commit -m "feat(terminate): add PresenceTerminationParticipant (order=60)"
```

---

## Task 9: TerminateEmployeeService rewrite + TerminateEmployeeServiceTest rewrite

**Files:**
- Modify: `src/main/java/com/b4rrhh/employee/lifecycle/application/usecase/TerminateEmployeeService.java`
- Modify: `src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/TerminateEmployeeServiceTest.java`

- [ ] **Step 1: Write the new TerminateEmployeeServiceTest (full rewrite)**

Replace the entire file with:

```java
package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.employee.domain.port.EmployeeRepository;
import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.TerminateEmployeeResult;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.service.TerminationPreConditionValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerminateEmployeeServiceTest {

    @Mock private TerminationPreConditionValidator validator;
    @Mock private EmployeeRepository employeeRepository;

    private final TerminationParticipant firstParticipant = mock(TerminationParticipant.class);
    private final TerminationParticipant secondParticipant = mock(TerminationParticipant.class);

    @Test
    void callsValidatorThenRunsParticipantsInOrder() {
        TerminationContext ctx = mock(TerminationContext.class);
        when(validator.validateAndLookup(any())).thenReturn(ctx);
        when(ctx.isAlreadyTerminated()).thenReturn(false);
        when(ctx.terminatedEmployee()).thenReturn(mock(Employee.class));
        when(firstParticipant.order()).thenReturn(10);
        when(secondParticipant.order()).thenReturn(20);

        service(List.of(secondParticipant, firstParticipant)).terminate(command());

        InOrder order = inOrder(validator, firstParticipant, secondParticipant);
        order.verify(validator).validateAndLookup(any());
        order.verify(firstParticipant).participate(ctx);
        order.verify(secondParticipant).participate(ctx);
    }

    @Test
    void returnsIdempotentResultWhenAlreadyTerminated() {
        TerminationContext ctx = mock(TerminationContext.class);
        TerminateEmployeeResult idempotentResult = mock(TerminateEmployeeResult.class);
        when(validator.validateAndLookup(any())).thenReturn(ctx);
        when(ctx.isAlreadyTerminated()).thenReturn(true);
        when(ctx.reconstructIdempotentResult()).thenReturn(idempotentResult);

        service(List.of(firstParticipant)).terminate(command());

        verify(firstParticipant, never()).participate(any());
        verify(ctx).reconstructIdempotentResult();
    }

    @Test
    void runsPostConditionCheckAfterParticipants() {
        TerminationContext ctx = mock(TerminationContext.class);
        when(validator.validateAndLookup(any())).thenReturn(ctx);
        when(ctx.isAlreadyTerminated()).thenReturn(false);
        when(ctx.terminatedEmployee()).thenReturn(mock(Employee.class));
        when(firstParticipant.order()).thenReturn(10);

        service(List.of(firstParticipant)).terminate(command());

        InOrder order = inOrder(firstParticipant, ctx);
        order.verify(firstParticipant).participate(ctx);
        order.verify(ctx).assertNoActivePresence();
    }

    @Test
    void savesTerminatedEmployeeAfterParticipants() {
        TerminationContext ctx = mock(TerminationContext.class);
        Employee terminatedEmployee = mock(Employee.class);
        when(validator.validateAndLookup(any())).thenReturn(ctx);
        when(ctx.isAlreadyTerminated()).thenReturn(false);
        when(ctx.terminatedEmployee()).thenReturn(terminatedEmployee);

        service(List.of()).terminate(command());

        InOrder order = inOrder(ctx, employeeRepository);
        order.verify(ctx).assertNoActivePresence();
        order.verify(employeeRepository).save(terminatedEmployee);
    }

    // --- helpers ---

    private TerminateEmployeeService service(List<TerminationParticipant> participants) {
        return new TerminateEmployeeService(validator, participants, employeeRepository);
    }

    private TerminateEmployeeCommand command() {
        return new TerminateEmployeeCommand(
                "ESP", "INTERNAL", "EMP001", LocalDate.of(2026, 3, 31), "VOL");
    }
}
```

- [ ] **Step 2: Run tests — expect FAIL (TerminateEmployeeService has wrong constructor)**

Run: `mvn test -Dtest=TerminateEmployeeServiceTest -pl . -q`
Expected: compilation error or test failure

- [ ] **Step 3: Rewrite TerminateEmployeeService**

Replace the entire file content with:

```java
package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.employee.employee.domain.port.EmployeeRepository;
import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.TerminateEmployeeResult;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.service.TerminationPreConditionValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class TerminateEmployeeService implements TerminateEmployeeUseCase {

    private final TerminationPreConditionValidator validator;
    private final List<TerminationParticipant> participants;
    private final EmployeeRepository employeeRepository;

    public TerminateEmployeeService(
            TerminationPreConditionValidator validator,
            List<TerminationParticipant> participants,
            EmployeeRepository employeeRepository) {
        this.validator = validator;
        this.participants = participants.stream()
                .sorted(Comparator.comparingInt(TerminationParticipant::order))
                .toList();
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public TerminateEmployeeResult terminate(TerminateEmployeeCommand command) {
        TerminationContext ctx = validator.validateAndLookup(command);
        if (ctx.isAlreadyTerminated()) return ctx.reconstructIdempotentResult();
        participants.forEach(p -> p.participate(ctx));
        ctx.assertNoActivePresence();
        employeeRepository.save(ctx.terminatedEmployee());
        return ctx.toResult();
    }
}
```

- [ ] **Step 4: Run both test classes — expect PASS**

Run: `mvn test -Dtest="TerminateEmployeeServiceTest" -pl . -q`
Expected: BUILD SUCCESS, 4 tests passing

- [ ] **Step 5: Run the full test suite**

Run: `mvn test -pl . -q`
Expected: BUILD SUCCESS (the rollback integration test will fail — fix in Task 10)

- [ ] **Step 6: Commit**

```
git add src/main/java/com/b4rrhh/employee/lifecycle/application/usecase/TerminateEmployeeService.java
git add src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/TerminateEmployeeServiceTest.java
git commit -m "feat(terminate): rewrite TerminateEmployeeService to 3-dependency orchestrator (ADR-047 phase 2)"
```

---

## Task 10: TerminateEmployeeServiceRollbackIntegrationTest — @TestConfiguration update

**Files:**
- Modify: `src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/TerminateEmployeeServiceRollbackIntegrationTest.java`

- [ ] **Step 1: Replace the @TestConfiguration with the new participant-based configuration**

Replace the entire file with:

```java
package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.employee.contract.application.usecase.ListEmployeeContractsUseCase;
import com.b4rrhh.employee.cost_center.application.usecase.CloseActiveCostCenterDistributionAtTerminationUseCase;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.port.EmployeeRepository;
import com.b4rrhh.employee.employee.infrastructure.persistence.EmployeePersistenceAdapter;
import com.b4rrhh.employee.labor_classification.application.usecase.ListEmployeeLaborClassificationsUseCase;
import com.b4rrhh.employee.lifecycle.application.command.TerminateEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.participant.CostCenterTerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.ContractTerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.LaborClassificationTerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.PresenceTerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.WorkCenterTerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.participant.WorkingTimeTerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import com.b4rrhh.employee.lifecycle.application.service.TerminationPreConditionValidator;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.workcenter.application.usecase.ListEmployeeWorkCentersUseCase;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        EmployeePersistenceAdapter.class,
        TerminateEmployeeService.class,
        TerminateEmployeeServiceRollbackIntegrationTest.TerminateEmployeeRollbackTestConfig.class
})
class TerminateEmployeeServiceRollbackIntegrationTest {

    @Autowired
    private TerminateEmployeeService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute("create schema if not exists employee");
        jdbcTemplate.execute("drop table if exists employee.employee");

        jdbcTemplate.execute("""
                create table employee.employee (
                    id bigint generated by default as identity primary key,
                    rule_system_code varchar(5) not null,
                    employee_type_code varchar(30) not null,
                    employee_number varchar(15) not null,
                    first_name varchar(100) not null,
                    last_name_1 varchar(100) not null,
                    last_name_2 varchar(100),
                    preferred_name varchar(300),
                    status varchar(30) not null,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    photo_url varchar(512),
                    constraint uk_employee_business_key unique (rule_system_code, employee_type_code, employee_number)
                )
                """);

        jdbcTemplate.update(
                "insert into employee.employee (rule_system_code, employee_type_code, employee_number, first_name, last_name_1, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)",
                "ESP", "INTERNAL", "EMP001", "Ana", "Lopez", "ACTIVE");
    }

    @Test
    void rollsBackWhenParticipantThrowsBeforeEmployeeStatusSave() {
        TerminateEmployeeCommand command = new TerminateEmployeeCommand(
                "ESP", "INTERNAL", "EMP001", LocalDate.of(2026, 3, 31), "VOL");

        assertThrows(TerminateEmployeeConflictException.class, () -> service.terminate(command));

        String persistedStatus = jdbcTemplate.queryForObject(
                "select status from employee.employee where rule_system_code = ? and employee_type_code = ? and employee_number = ?",
                String.class, "ESP", "INTERNAL", "EMP001");

        assertEquals("ACTIVE", persistedStatus);
    }

    @TestConfiguration
    static class TerminateEmployeeRollbackTestConfig {

        @Bean
        GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase(EmployeeRepository employeeRepository) {
            return (ruleSystemCode, employeeTypeCode, employeeNumber) -> employeeRepository
                    .findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumber(
                            ruleSystemCode, employeeTypeCode, employeeNumber);
        }

        @Bean
        ListEmployeePresencesUseCase listEmployeePresencesUseCase() {
            return (rs, et, en) -> List.of(new Presence(
                    10L, 100L, 1, "COMP", "HIRE", null,
                    LocalDate.of(2026, 1, 1), null,
                    LocalDateTime.now(), LocalDateTime.now()));
        }

        @Bean
        ListEmployeeContractsUseCase listEmployeeContractsUseCase() {
            return command -> List.of();
        }

        @Bean
        ListEmployeeLaborClassificationsUseCase listEmployeeLaborClassificationsUseCase() {
            return command -> List.of();
        }

        @Bean
        ListEmployeeWorkCentersUseCase listEmployeeWorkCentersUseCase() {
            return (rs, et, en) -> List.of();
        }

        @Bean
        ListEmployeeWorkingTimesUseCase listEmployeeWorkingTimesUseCase() {
            return command -> List.of();
        }

        @Bean
        TerminationPreConditionValidator terminationPreConditionValidator(
                GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKey,
                ListEmployeePresencesUseCase listPresences,
                ListEmployeeContractsUseCase listContracts,
                ListEmployeeLaborClassificationsUseCase listLaborClassifications,
                ListEmployeeWorkCentersUseCase listWorkCenters,
                ListEmployeeWorkingTimesUseCase listWorkingTimes) {
            return new TerminationPreConditionValidator(
                    getEmployeeByBusinessKey, listPresences, listContracts,
                    listLaborClassifications, listWorkCenters, listWorkingTimes);
        }

        @Bean
        TerminationParticipant workingTimeTerminationParticipant() {
            // Throws to trigger rollback scenario
            return new TerminationParticipant() {
                @Override public int order() { return 10; }
                @Override public void participate(TerminationContext ctx) {
                    throw new TerminateEmployeeConflictException(
                            "Simulated working time conflict for rollback test");
                }
            };
        }

        @Bean
        WorkCenterTerminationParticipant workCenterTerminationParticipant() {
            return new WorkCenterTerminationParticipant(
                    (rs, et, en) -> List.of(),
                    command -> null);
        }

        @Bean
        CostCenterTerminationParticipant costCenterTerminationParticipant() {
            return new CostCenterTerminationParticipant(
                    (rs, et, en, date) -> {});
        }

        @Bean
        ContractTerminationParticipant contractTerminationParticipant() {
            return new ContractTerminationParticipant(
                    command -> List.of(),
                    command -> null);
        }

        @Bean
        LaborClassificationTerminationParticipant laborClassificationTerminationParticipant() {
            return new LaborClassificationTerminationParticipant(
                    command -> List.of(),
                    command -> null);
        }

        @Bean
        PresenceTerminationParticipant presenceTerminationParticipant() {
            return new PresenceTerminationParticipant(
                    (rs, et, en) -> List.of(new Presence(
                            10L, 100L, 1, "COMP", "HIRE", null,
                            LocalDate.of(2026, 1, 1), null,
                            LocalDateTime.now(), LocalDateTime.now())),
                    command -> new Presence(
                            10L, 100L, command.presenceNumber(), "COMP", "HIRE",
                            command.exitReasonCode(),
                            LocalDate.of(2026, 1, 1), command.endDate(),
                            LocalDateTime.now(), LocalDateTime.now()));
        }
    }
}
```

- [ ] **Step 2: Run the rollback integration test — expect PASS**

Run: `mvn test -Dtest=TerminateEmployeeServiceRollbackIntegrationTest -pl . -q`
Expected: BUILD SUCCESS, 1 test passing

- [ ] **Step 3: Run the full test suite**

Run: `mvn test -pl . -q`
Expected: BUILD SUCCESS — all tests passing

- [ ] **Step 4: Commit**

```
git add src/test/java/com/b4rrhh/employee/lifecycle/application/usecase/TerminateEmployeeServiceRollbackIntegrationTest.java
git commit -m "feat(terminate): update rollback integration test to participant-based TestConfiguration"
```

---

## Self-Review

Spec coverage checklist:
- ✅ `TerminationParticipant` interface — Task 1
- ✅ `TerminationContext` (all fields, idempotency constructors, assertNoActivePresence, terminatedEmployee, toResult) — Task 1
- ✅ `TerminationPreConditionValidator` (validation, normalization, employee lookup, idempotency detection) — Task 2
- ✅ WorkingTimeTerminationParticipant order=10 — Task 3
- ✅ WorkCenterTerminationParticipant order=20 — Task 4
- ✅ CostCenterTerminationParticipant order=30 (special: delegates to closeIfPresent) — Task 5
- ✅ ContractTerminationParticipant order=40 — Task 6
- ✅ LaborClassificationTerminationParticipant order=50 — Task 7
- ✅ PresenceTerminationParticipant order=60 (mandatory, deduplication) — Task 8
- ✅ `TerminateEmployeeService` rewrite (3 dependencies, idempotency early-return, assertNoActivePresence, save, toResult) — Task 9
- ✅ `TerminateEmployeeServiceTest` rewrite (4 orchestration-only tests using `mock(TerminationContext.class)`) — Task 9
- ✅ `TerminateEmployeeServiceRollbackIntegrationTest` TestConfiguration rewrite — Task 10
- ✅ `@Transactional` boundary preserved (single transaction in service)
- ✅ All domain exception translations (catalog → CatalogValueInvalid, others → Conflict)
- ✅ Per-participant skip paths (no active, startDate > terminationDate)
- ✅ requireAtMostOneActive pattern in each optional participant
- ✅ Presence deduplication by presenceNumber
