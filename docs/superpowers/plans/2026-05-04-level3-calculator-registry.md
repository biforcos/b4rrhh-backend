# Level 3 — Calculator Registry Extraction

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate the duplicated `TechnicalConceptCalculator` registry that exists in both `DefaultSegmentExecutionEngine` and `CalculatePayrollUnitService`, and align the exception type thrown when a calculator is missing.

**Architecture:** Extract a new `TechnicalConceptCalculatorRegistry` Spring `@Component` that accepts the Spring-injected `List<TechnicalConceptCalculator>` and owns the `Map<String, TechnicalConceptCalculator>`. Both the engine and the service inject the registry instead of building their own maps.

**Tech Stack:** Java 21, Spring Boot (`@Component`, constructor injection), JUnit 5, Mockito

---

## File Map

| Action | File |
|--------|------|
| Create | `src/main/java/com/b4rrhh/payroll_engine/execution/application/service/TechnicalConceptCalculatorRegistry.java` |
| Modify | `src/main/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngine.java` |
| Modify | `src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java` |
| Modify | `src/main/java/com/b4rrhh/payroll_engine/execution/domain/exception/UnsupportedTechnicalConceptException.java` |
| Modify | `src/test/java/com/b4rrhh/payroll_engine/execution/application/service/SegmentExecutionEngineTest.java` |
| Modify | `src/test/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngineEmployeeInputTest.java` |
| Modify | `src/test/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitServiceTest.java` |

---

## Task 1: Create `TechnicalConceptCalculatorRegistry` and wire it into both engine and service

### Files
- Create: `src/main/java/com/b4rrhh/payroll_engine/execution/application/service/TechnicalConceptCalculatorRegistry.java`
- Modify: `src/main/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngine.java`
- Modify: `src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java`
- Modify: `src/test/java/com/b4rrhh/payroll_engine/execution/application/service/SegmentExecutionEngineTest.java`
- Modify: `src/test/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngineEmployeeInputTest.java`
- Modify: `src/test/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitServiceTest.java`

---

- [ ] **Step 1: Write the failing test — SegmentExecutionEngineTest uses registry**

  In `SegmentExecutionEngineTest.java`, the `DefaultSegmentExecutionEngine` constructor currently accepts `List<TechnicalConceptCalculator>`. After this task it will accept `TechnicalConceptCalculatorRegistry`. Update the constructor call in the test to pass a registry instance instead of a list.

  First, confirm the current constructor call in the test (it uses `List.of()` for the calculators param). The failing test should show a compilation error because `TechnicalConceptCalculatorRegistry` doesn't exist yet.

  ```java
  // In SegmentExecutionEngineTest — find the engine instantiation, it currently looks like:
  private final DefaultSegmentExecutionEngine engine = new DefaultSegmentExecutionEngine(
          new SegmentTechnicalValueResolver(),
          new RateByQuantityOperandResolver(),
          new PercentageConceptResolver(),
          new GreatestConceptResolver(),
          new LeastConceptResolver(),
          List.of());  // <-- this becomes: new TechnicalConceptCalculatorRegistry(List.of())
  ```

  Change the last argument to `new TechnicalConceptCalculatorRegistry(List.of())`.

  Same for `DefaultSegmentExecutionEngineEmployeeInputTest.java` — it has an identical engine instantiation.

- [ ] **Step 2: Run test to verify it fails**

  ```
  mvn test -Dtest=SegmentExecutionEngineTest -q
  ```
  Expected: COMPILATION ERROR — `TechnicalConceptCalculatorRegistry` not found.

- [ ] **Step 3: Create `TechnicalConceptCalculatorRegistry`**

  ```java
  package com.b4rrhh.payroll_engine.execution.application.service;

  import org.springframework.stereotype.Component;

  import java.util.List;
  import java.util.Map;
  import java.util.stream.Collectors;

  @Component
  public class TechnicalConceptCalculatorRegistry {

      private final Map<String, TechnicalConceptCalculator> calculators;

      public TechnicalConceptCalculatorRegistry(List<TechnicalConceptCalculator> calculators) {
          this.calculators = calculators.stream()
                  .collect(Collectors.toMap(TechnicalConceptCalculator::conceptCode, c -> c));
      }

      public TechnicalConceptCalculator get(String conceptCode) {
          return calculators.get(conceptCode);
      }
  }
  ```

- [ ] **Step 4: Update `DefaultSegmentExecutionEngine` to inject `TechnicalConceptCalculatorRegistry`**

  Current field:
  ```java
  private final Map<String, TechnicalConceptCalculator> technicalCalculators;
  ```

  New field:
  ```java
  private final TechnicalConceptCalculatorRegistry technicalCalculatorRegistry;
  ```

  Current constructor param: `List<TechnicalConceptCalculator> technicalCalculators`
  
  New constructor param: `TechnicalConceptCalculatorRegistry technicalCalculatorRegistry`

  Constructor body: replace
  ```java
  this.technicalCalculators = technicalCalculators.stream()
          .collect(Collectors.toMap(TechnicalConceptCalculator::conceptCode, c -> c));
  ```
  with:
  ```java
  this.technicalCalculatorRegistry = technicalCalculatorRegistry;
  ```

  In the `execute()` method, ENGINE_PROVIDED case, replace:
  ```java
  TechnicalConceptCalculator calculator = technicalCalculators.get(conceptCode);
  ```
  with:
  ```java
  TechnicalConceptCalculator calculator = technicalCalculatorRegistry.get(conceptCode);
  ```

  Remove the `import java.util.stream.Collectors;` if it is now unused (it was used for the map build). Keep `import java.util.List;` removed too if unused.

- [ ] **Step 5: Update `CalculatePayrollUnitService` to inject `TechnicalConceptCalculatorRegistry`**

  Current field:
  ```java
  private final Map<String, TechnicalConceptCalculator> technicalCalculatorsMap;
  ```

  New field:
  ```java
  private final TechnicalConceptCalculatorRegistry technicalCalculatorRegistry;
  ```

  Current constructor param: `List<TechnicalConceptCalculator> technicalConceptCalculators`

  New constructor param: `TechnicalConceptCalculatorRegistry technicalCalculatorRegistry`

  Constructor body: replace
  ```java
  this.technicalCalculatorsMap = technicalConceptCalculators.stream()
          .collect(Collectors.toMap(TechnicalConceptCalculator::conceptCode, c -> c));
  ```
  with:
  ```java
  this.technicalCalculatorRegistry = technicalCalculatorRegistry;
  ```

  In `calculateEligibleReal()`, wherever `technicalCalculatorsMap.get(conceptCode)` appears (there are two occurrences — in the per-segment ENGINE_PROVIDED case and in the post-segment aggregate ENGINE_PROVIDED case), replace with `technicalCalculatorRegistry.get(conceptCode)`.

  Wait — after Level 2, the per-segment ENGINE_PROVIDED is now handled by the engine, not the service. So in the service there should only be ONE occurrence: in the post-segment aggregate loop. Verify this before editing.

  Remove now-unused imports:
  - `import com.b4rrhh.payroll_engine.execution.application.service.TechnicalConceptCalculator;` if no longer directly referenced
  - `import java.util.stream.Collectors;` if no longer used elsewhere

- [ ] **Step 6: Update `CalculatePayrollUnitServiceTest`**

  The test currently passes `List.of()` as the `TechnicalConceptCalculator` constructor param. After this task, the constructor takes `TechnicalConceptCalculatorRegistry`. Replace:

  ```java
  List.of(),
  segmentExecutionEngine,
  ```
  with:
  ```java
  new TechnicalConceptCalculatorRegistry(List.of()),
  segmentExecutionEngine,
  ```

  Do this for BOTH service constructor calls in the test (there are two).

  Add import if needed:
  ```java
  import com.b4rrhh.payroll_engine.execution.application.service.TechnicalConceptCalculatorRegistry;
  ```

- [ ] **Step 7: Run tests to verify they pass**

  ```
  mvn test -q
  ```
  Expected: 1227+ tests, 0 failures.

- [ ] **Step 8: Commit**

  ```
  git add src/main/java/com/b4rrhh/payroll_engine/execution/application/service/TechnicalConceptCalculatorRegistry.java
  git add src/main/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngine.java
  git add src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java
  git add src/test/java/com/b4rrhh/payroll_engine/execution/application/service/SegmentExecutionEngineTest.java
  git add src/test/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngineEmployeeInputTest.java
  git add src/test/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitServiceTest.java
  git commit -m "refactor(payroll-engine): extract TechnicalConceptCalculatorRegistry to eliminate duplicated map"
  ```

---

## Task 2: Fix exception type and message in post-segment ENGINE_PROVIDED

### Files
- Modify: `src/main/java/com/b4rrhh/payroll_engine/execution/domain/exception/UnsupportedTechnicalConceptException.java`
- Modify: `src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java`

---

- [ ] **Step 1: Write failing test**

  In `CalculatePayrollUnitService`, the post-segment aggregate loop's ENGINE_PROVIDED case throws:
  ```java
  throw new UnsupportedOperationException("No TechnicalConceptCalculator registered for concept: " + conceptCode);
  ```

  The engine throws `UnsupportedTechnicalConceptException` for the same logical error. Add a test to `CalculatePayrollUnitServiceTest` that verifies `UnsupportedTechnicalConceptException` is thrown from the service for an unknown ENGINE_PROVIDED concept in the aggregate plan. This test should fail because the service currently throws `UnsupportedOperationException`.

  ```java
  @Test
  void aggregatePlan_unknownEngineProvidedConcept_throwsUnsupportedTechnicalConceptException() {
      PayrollLaunchExecutionProperties properties = new PayrollLaunchExecutionProperties();

      CalculatePayrollUnitService service = new CalculatePayrollUnitService(
          calculatePayrollUseCase,
          payrollLaunchEligibleInputLookupPort,
          properties,
          payrollConceptGraphCalculator,
          buildEligibleExecutionPlanUseCase,
          companyProfileLookupPort,
          employeePersonalDataLookupPort,
          agreementProfileLookupPort,
          workCenterProfileLookupPort,
          new TechnicalConceptCalculatorRegistry(List.of()),  // empty registry
          segmentExecutionEngine,
          employeePayrollInputLookupPort,
          getAgreementCategoryProfileUseCase
      );

      // Set up eligible input (same as the first test)
      when(payrollLaunchEligibleInputLookupPort.findByUnitAndPeriod(
          "ESP", "INTERNAL", "EMP001", 2,
          LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)
      )).thenReturn(Optional.of(new PayrollLaunchEligibleInputContext(
          "ES01", "99002405011982", "99002405-G2",
          List.of(new PayrollLaunchWorkingTimeWindowContext(LocalDate.of(2025, 1, 1), null, new BigDecimal("100"))),
          LocalDate.of(2025, 1, 1), null, null
      )));

      when(getAgreementCategoryProfileUseCase.get(
              new GetAgreementCategoryProfileQuery("ESP", "99002405-G2")))
          .thenReturn(new AgreementCategoryProfile("05", TipoNomina.MENSUAL));

      // Plan has one ENGINE_PROVIDED entry in the aggregate section
      ConceptExecutionPlanEntry engineEntry = new ConceptExecutionPlanEntry(
              new ConceptNodeIdentity("ESP", "P_SS"), CalculationType.ENGINE_PROVIDED);

      com.b4rrhh.payroll_engine.object.domain.model.PayrollObject obj =
              new com.b4rrhh.payroll_engine.object.domain.model.PayrollObject(
                      1L, "ESP",
                      com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode.CONCEPT,
                      "P_SS", null, null);
      com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept concept =
              new com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept(
                      obj, "P_SS", CalculationType.ENGINE_PROVIDED,
                      com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature.INFORMATIONAL,
                      com.b4rrhh.payroll_engine.concept.domain.model.ResultCompositionMode.REPLACE,
                      "P_SS",
                      com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope.PERIOD,
                      false, null, null);

      EligibleExecutionPlanResult planResult = new EligibleExecutionPlanResult(
              List.of(), List.of(), List.of(concept), null, List.of(engineEntry));
      when(buildEligibleExecutionPlanUseCase.build(
              org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
          .thenReturn(planResult);

      when(employeePayrollInputLookupPort.findInputsByPeriod(
              org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
              org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
          .thenReturn(Map.of());

      // Empty segment state for the engine (no per-segment concepts)
      when(segmentExecutionEngine.execute(
              org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
          .thenReturn(new com.b4rrhh.payroll_engine.execution.domain.model.SegmentExecutionState());

      assertThrows(
          com.b4rrhh.payroll_engine.execution.domain.exception.UnsupportedTechnicalConceptException.class,
          () -> service.calculate(new CalculatePayrollUnitCommand(
              "ESP", "INTERNAL", "EMP001", "202501", "ORD", 2,
              LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
              "ENGINE", "1.0"
          ))
      );
  }
  ```

  Note: this test drives a scenario where the ENGINE_PROVIDED entry is in the aggregate plan (i.e., after the first AGGREGATE entry). If `engineEntry` alone is the only plan entry and it is ENGINE_PROVIDED (not AGGREGATE), then `firstAggIdx = plan.size()` and it would land in `perSegmentPlan`, not `aggregatePlan`. To land in the aggregate plan you need at least one AGGREGATE entry before it. Adjust the plan to add a placeholder AGGREGATE entry first if needed, or verify the split logic to ensure ENGINE_PROVIDED lands in `aggregatePlan`. If the plan split is tricky to set up in a unit test, an alternative is to simply verify the fix at the method level (read the code and confirm the exception type change directly in the test by reading the aggregate switch).

  **Simpler alternative test approach:** If setting up the aggregate loop path is complex, write a simpler test that just calls a private helper or directly verifies the aggregate loop by reflection, OR just fix the code and confirm the existing integration test covers the aggregate path. In that case, write the test after fixing the code (step 3 first).

- [ ] **Step 2: Run test to verify it fails**

  ```
  mvn test -Dtest=CalculatePayrollUnitServiceTest -q
  ```
  Expected: FAIL — `UnsupportedOperationException` thrown, not `UnsupportedTechnicalConceptException`.

- [ ] **Step 3: Update `UnsupportedTechnicalConceptException` message to be generic**

  The current message references "PoC resolver" and lists PoC-specific concept codes:
  ```java
  super("Technical concept not supported in PoC resolver: '" + conceptCode +
        "'. Supported codes: T_DIAS_PRESENCIA_SEGMENTO, T_SALARIO_MENSUAL, T_FACTOR_JORNADA, T_PRECIO_DIA.");
  ```

  Replace with a generic production-appropriate message:
  ```java
  super("No TechnicalConceptCalculator registered for concept: '" + conceptCode + "'.");
  ```

  This makes the exception usable from both the engine and the service without lying about "PoC".

- [ ] **Step 4: Fix the post-segment ENGINE_PROVIDED throw in `CalculatePayrollUnitService`**

  In the aggregate loop's `ENGINE_PROVIDED` case, replace:
  ```java
  throw new UnsupportedOperationException(
          "No TechnicalConceptCalculator registered for concept: " + conceptCode);
  ```
  with:
  ```java
  throw new UnsupportedTechnicalConceptException(conceptCode);
  ```

  Add import if needed:
  ```java
  import com.b4rrhh.payroll_engine.execution.domain.exception.UnsupportedTechnicalConceptException;
  ```

- [ ] **Step 5: Run tests to verify they pass**

  ```
  mvn test -q
  ```
  Expected: 1227+ tests, 0 failures.

- [ ] **Step 6: Commit**

  ```
  git add src/main/java/com/b4rrhh/payroll_engine/execution/domain/exception/UnsupportedTechnicalConceptException.java
  git add src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java
  git add src/test/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitServiceTest.java
  git commit -m "fix(payroll): use UnsupportedTechnicalConceptException in aggregate ENGINE_PROVIDED path"
  ```
