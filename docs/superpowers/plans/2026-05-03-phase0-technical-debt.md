# Phase 0 — Cierre de Deuda Técnica del Payroll Engine

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Commitear y testar los 9 nuevos calculadores de cotización SS, y propagar `grupoCotizacionCode`/`tipoNomina` desde `SegmentCalculationContext` hasta `DefaultSegmentExecutionEngine` para que los calculadores de tope funcionen correctamente.

**Architecture:** Los 7 calculadores simples de tipo `ENGINE_PROVIDED` implementan una tasa fija sin estado. Los 2 calculadores de tope (`TopeMax`/`TopeMin`) delegan en `SsCotizacionTopesRepository` y prorratean la base mensual por días de segmento. El camino de ejecución `DefaultSegmentExecutionEngine` → `TechnicalConceptSegmentData` pasa actualmente `null, null` para `grupoCotizacionCode` y `tipoNomina`, bloqueando a los calculadores de tope; la solución es añadir esos campos a `SegmentCalculationContext` y propagarlos.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, AssertJ. Sin Mockito — los tests usan implementaciones anónimas inline.

---

### Task 1: Tests para los 7 calculadores de tasa fija

**Files:**
- Create: `src/test/java/com/b4rrhh/payroll_engine/execution/application/service/SimpleRateCalculatorTest.java`

- [ ] **Step 1: Escribir el test**

```java
package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleRateCalculatorTest {

    private static final TechnicalConceptSegmentData DUMMY = new TechnicalConceptSegmentData(
            LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
            LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
            30, BigDecimal.ONE, "ESP", "GR01", "MENSUAL"
    );

    record Case(TechnicalConceptCalculator calc, String code, String rate) {}

    static Stream<Case> cases() {
        return Stream.of(
                new Case(new FpTrabajadorRateCalculator(),          "P_FP_TRAB",          "0.10"),
                new Case(new MeiTrabajadorRateCalculator(),         "P_MEI_TRAB",          "0.11"),
                new Case(new SsCcEmpresarioRateCalculator(),        "P_SS_CC_EMP",        "23.60"),
                new Case(new SsDesempleoEmpresarioRateCalculator(), "P_SS_DESEMPLEO_EMP",  "7.05"),
                new Case(new SsFogasaEmpresarioRateCalculator(),    "P_SS_FOGASA_EMP",     "0.20"),
                new Case(new SsFpEmpresarioRateCalculator(),        "P_SS_FP_EMP",         "0.60"),
                new Case(new SsMeiEmpresarioRateCalculator(),       "P_SS_MEI_EMP",        "0.58")
        );
    }

    @ParameterizedTest
    @MethodSource("cases")
    void conceptCodeMatches(Case c) {
        assertThat(c.calc().conceptCode()).isEqualTo(c.code());
    }

    @ParameterizedTest
    @MethodSource("cases")
    void resolveReturnsFixedRate(Case c) {
        assertThat(c.calc().resolve(DUMMY))
                .isEqualByComparingTo(new BigDecimal(c.rate()));
    }
}
```

- [ ] **Step 2: Ejecutar el test — debe PASAR (los calculadores ya existen)**

```
mvn test -Dtest=SimpleRateCalculatorTest -pl . --no-transfer-progress
```

Salida esperada: `BUILD SUCCESS`, 14 tests passed.

- [ ] **Step 3: Commit**

```
git add src/main/java/com/b4rrhh/payroll_engine/execution/application/service/FpTrabajadorRateCalculator.java
git add src/main/java/com/b4rrhh/payroll_engine/execution/application/service/MeiTrabajadorRateCalculator.java
git add src/main/java/com/b4rrhh/payroll_engine/execution/application/service/SsCcEmpresarioRateCalculator.java
git add src/main/java/com/b4rrhh/payroll_engine/execution/application/service/SsDesempleoEmpresarioRateCalculator.java
git add src/main/java/com/b4rrhh/payroll_engine/execution/application/service/SsFogasaEmpresarioRateCalculator.java
git add src/main/java/com/b4rrhh/payroll_engine/execution/application/service/SsFpEmpresarioRateCalculator.java
git add src/main/java/com/b4rrhh/payroll_engine/execution/application/service/SsMeiEmpresarioRateCalculator.java
git add src/test/java/com/b4rrhh/payroll_engine/execution/application/service/SimpleRateCalculatorTest.java
git commit -m "feat(payroll-engine): add 7 fixed SS contribution rate calculators with tests"
```

---

### Task 2: Propagar `grupoCotizacionCode` y `tipoNomina` por el camino de ejecución

**Files:**
- Modify: `src/main/java/com/b4rrhh/payroll_engine/segment/domain/model/SegmentCalculationContext.java`
- Modify: `src/main/java/com/b4rrhh/payroll_engine/execution/domain/model/PayrollEnginePocRequest.java`
- Modify: `src/main/java/com/b4rrhh/payroll_engine/planning/application/service/EligiblePayrollExecutionRequest.java`
- Modify: `src/main/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngine.java`
- Modify: `src/main/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultPayrollEnginePocExecutor.java`
- Modify: `src/main/java/com/b4rrhh/payroll_engine/planning/application/service/DefaultEligiblePayrollExecutor.java`
- Modify: `src/test/java/com/b4rrhh/payroll_engine/segment/domain/model/SegmentCalculationContextTest.java`
- Modify: `src/test/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngineEmployeeInputTest.java`
- Modify: `src/test/java/com/b4rrhh/payroll_engine/execution/application/service/SegmentExecutionEngineTest.java`
- Modify: `src/test/java/com/b4rrhh/payroll_engine/execution/application/service/PayrollEnginePocExecutorTest.java`
- Modify: `src/test/java/com/b4rrhh/payroll_engine/planning/application/service/DefaultEligiblePayrollExecutorTest.java`

- [ ] **Step 1: Escribir tests de validación para los nuevos campos (deben FALLAR hasta que se añadan)**

En `SegmentCalculationContextTest.java`, añadir al final de la clase (antes del cierre `}`):

```java
@Test
void nullGrupoCotizacionCodeIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
            "ESP", "EMP", "EMP0001",
            APR_01, APR_30, APR_01, APR_14,
            true, false, 30L, 14L,
            new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
            null, "MENSUAL"));
}

@Test
void blankGrupoCotizacionCodeIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
            "ESP", "EMP", "EMP0001",
            APR_01, APR_30, APR_01, APR_14,
            true, false, 30L, 14L,
            new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
            "  ", "MENSUAL"));
}

@Test
void nullTipoNominaIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
            "ESP", "EMP", "EMP0001",
            APR_01, APR_30, APR_01, APR_14,
            true, false, 30L, 14L,
            new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
            "GR01", null));
}

@Test
void blankTipoNominaIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
            "ESP", "EMP", "EMP0001",
            APR_01, APR_30, APR_01, APR_14,
            true, false, 30L, 14L,
            new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
            "GR01", "  "));
}
```

- [ ] **Step 2: Ejecutar el test para confirmar FALLO (14 params no compila todavía con 16)**

```
mvn test -Dtest=SegmentCalculationContextTest --no-transfer-progress
```

Salida esperada: `BUILD FAILURE` — error de compilación porque el constructor tiene 14 parámetros.

- [ ] **Step 3: Actualizar `SegmentCalculationContext.java`**

Sustituir el bloque completo del constructor y añadir campos y getters. El fichero queda:

```java
package com.b4rrhh.payroll_engine.segment.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public final class SegmentCalculationContext {

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be null or blank");
        }
    }

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    private final String ruleSystemCode;
    private final String employeeTypeCode;
    private final String employeeNumber;

    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final LocalDate segmentStart;
    private final LocalDate segmentEnd;

    private final boolean firstSegment;
    private final boolean lastSegment;

    private final long daysInPeriod;
    private final long daysInSegment;

    private final BigDecimal workingTimePercentage;
    private final BigDecimal monthlySalaryAmount;
    private final Map<String, BigDecimal> employeeInputs;

    private final String grupoCotizacionCode;
    private final String tipoNomina;

    public SegmentCalculationContext(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate segmentStart,
            LocalDate segmentEnd,
            boolean firstSegment,
            boolean lastSegment,
            long daysInPeriod,
            long daysInSegment,
            BigDecimal workingTimePercentage,
            BigDecimal monthlySalaryAmount,
            Map<String, BigDecimal> employeeInputs,
            String grupoCotizacionCode,
            String tipoNomina
    ) {
        requireNonBlank(ruleSystemCode, "ruleSystemCode");
        requireNonBlank(employeeTypeCode, "employeeTypeCode");
        requireNonBlank(employeeNumber, "employeeNumber");
        requireNonNull(periodStart, "periodStart");
        requireNonNull(periodEnd, "periodEnd");
        requireNonNull(segmentStart, "segmentStart");
        requireNonNull(segmentEnd, "segmentEnd");
        if (periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("periodEnd must not be before periodStart");
        }
        if (segmentEnd.isBefore(segmentStart)) {
            throw new IllegalArgumentException("segmentEnd must not be before segmentStart");
        }
        if (segmentStart.isBefore(periodStart) || segmentEnd.isAfter(periodEnd)) {
            throw new IllegalArgumentException("segment [" + segmentStart + ", " + segmentEnd +
                    "] must be contained within period [" + periodStart + ", " + periodEnd + "]");
        }
        if (daysInPeriod <= 0) {
            throw new IllegalArgumentException("daysInPeriod must be > 0, got: " + daysInPeriod);
        }
        if (daysInSegment <= 0) {
            throw new IllegalArgumentException("daysInSegment must be > 0, got: " + daysInSegment);
        }
        requireNonNull(workingTimePercentage, "workingTimePercentage");
        requireNonNull(monthlySalaryAmount, "monthlySalaryAmount");
        requireNonNull(employeeInputs, "employeeInputs");
        requireNonBlank(grupoCotizacionCode, "grupoCotizacionCode");
        requireNonBlank(tipoNomina, "tipoNomina");
        this.ruleSystemCode = ruleSystemCode;
        this.employeeTypeCode = employeeTypeCode;
        this.employeeNumber = employeeNumber;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.segmentStart = segmentStart;
        this.segmentEnd = segmentEnd;
        this.firstSegment = firstSegment;
        this.lastSegment = lastSegment;
        this.daysInPeriod = daysInPeriod;
        this.daysInSegment = daysInSegment;
        this.workingTimePercentage = workingTimePercentage;
        this.monthlySalaryAmount = monthlySalaryAmount;
        this.employeeInputs = employeeInputs;
        this.grupoCotizacionCode = grupoCotizacionCode;
        this.tipoNomina = tipoNomina;
    }

    public String getRuleSystemCode() { return ruleSystemCode; }
    public String getEmployeeTypeCode() { return employeeTypeCode; }
    public String getEmployeeNumber() { return employeeNumber; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public LocalDate getSegmentStart() { return segmentStart; }
    public LocalDate getSegmentEnd() { return segmentEnd; }
    public boolean isFirstSegment() { return firstSegment; }
    public boolean isLastSegment() { return lastSegment; }
    public long getDaysInPeriod() { return daysInPeriod; }
    public long getDaysInSegment() { return daysInSegment; }
    public BigDecimal getWorkingTimePercentage() { return workingTimePercentage; }
    public BigDecimal getMonthlySalaryAmount() { return monthlySalaryAmount; }
    public Map<String, BigDecimal> getEmployeeInputs() { return employeeInputs; }
    public String getGrupoCotizacionCode() { return grupoCotizacionCode; }
    public String getTipoNomina() { return tipoNomina; }
}
```

- [ ] **Step 4: Actualizar `PayrollEnginePocRequest.java`**

Añadir `grupoCotizacionCode` y `tipoNomina` al final del constructor (después de `workingTimeWindows`):

```java
// Nuevos campos (declaración)
private final String grupoCotizacionCode;
private final String tipoNomina;

// Nuevos parámetros en el constructor (añadir al final de la firma)
String grupoCotizacionCode,
String tipoNomina

// Validación en el constructor (añadir antes de las asignaciones)
if (grupoCotizacionCode == null || grupoCotizacionCode.isBlank()) {
    throw new IllegalArgumentException("grupoCotizacionCode is required");
}
if (tipoNomina == null || tipoNomina.isBlank()) {
    throw new IllegalArgumentException("tipoNomina is required");
}

// Asignaciones (añadir al final)
this.grupoCotizacionCode = grupoCotizacionCode;
this.tipoNomina = tipoNomina;

// Getters (añadir al final)
public String getGrupoCotizacionCode() { return grupoCotizacionCode; }
public String getTipoNomina() { return tipoNomina; }
```

- [ ] **Step 5: Actualizar `EligiblePayrollExecutionRequest.java`**

Mismo patrón que el paso anterior. El constructor actual tiene 9 parámetros. Añadir `grupoCotizacionCode` y `tipoNomina` al final:

```java
// Nuevos campos
private final String grupoCotizacionCode;
private final String tipoNomina;

// En la firma del constructor (añadir al final)
String grupoCotizacionCode,
String tipoNomina

// Validación (añadir antes de las asignaciones, después de la validación de workingTimeWindows)
if (grupoCotizacionCode == null || grupoCotizacionCode.isBlank()) {
    throw new IllegalArgumentException("grupoCotizacionCode is required");
}
if (tipoNomina == null || tipoNomina.isBlank()) {
    throw new IllegalArgumentException("tipoNomina is required");
}

// Asignaciones
this.grupoCotizacionCode = grupoCotizacionCode;
this.tipoNomina = tipoNomina;

// Getters
public String getGrupoCotizacionCode() { return grupoCotizacionCode; }
public String getTipoNomina() { return tipoNomina; }
```

- [ ] **Step 6: Actualizar `DefaultSegmentExecutionEngine.java` — pasar campos en lugar de null**

En el `case ENGINE_PROVIDED`, reemplazar:

```java
// ANTES:
yield calculator.resolve(new TechnicalConceptSegmentData(
        context.getPeriodStart(),
        context.getPeriodEnd(),
        context.getSegmentStart(),
        context.getSegmentEnd(),
        context.getDaysInSegment(),
        context.getWorkingTimePercentage(),
        context.getRuleSystemCode(),
        null,
        null
));
```

Por:

```java
// DESPUÉS:
yield calculator.resolve(new TechnicalConceptSegmentData(
        context.getPeriodStart(),
        context.getPeriodEnd(),
        context.getSegmentStart(),
        context.getSegmentEnd(),
        context.getDaysInSegment(),
        context.getWorkingTimePercentage(),
        context.getRuleSystemCode(),
        context.getGrupoCotizacionCode(),
        context.getTipoNomina()
));
```

- [ ] **Step 7: Actualizar `DefaultPayrollEnginePocExecutor.java` — pasar grupo/tipo en la construcción del contexto**

En el bucle de segmentos (línea ~163), añadir los 2 nuevos argumentos al final de la construcción de `SegmentCalculationContext`:

```java
// ANTES (los últimos argumentos):
                request.getMonthlySalaryAmount(),
                Map.of()

// DESPUÉS:
                request.getMonthlySalaryAmount(),
                Map.of(),
                request.getGrupoCotizacionCode(),
                request.getTipoNomina()
```

- [ ] **Step 8: Actualizar `DefaultEligiblePayrollExecutor.java` — pasar grupo/tipo en la construcción del contexto**

Mismo patrón que el paso anterior (línea ~127):

```java
// ANTES (los últimos argumentos):
                request.getMonthlySalaryAmount(),
                Map.of()

// DESPUÉS:
                request.getMonthlySalaryAmount(),
                Map.of(),
                request.getGrupoCotizacionCode(),
                request.getTipoNomina()
```

- [ ] **Step 9: Actualizar `SegmentCalculationContextTest.java` — añadir `"GR01", "MENSUAL"` a todos los constructores existentes**

Cada `new SegmentCalculationContext(...)` existente en este fichero termina con `Map.of())` o `Map.of()))`. Hay 19 sitios en total (1 en el helper `valid()` + 18 en assertThrows). El patrón de sustitución es:

```
// ANTES:
new BigDecimal("100"), new BigDecimal("2000.00"), Map.of())

// DESPUÉS:
new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(), "GR01", "MENSUAL")
```

El helper `valid()` también termina con `Map.of()` — mismo cambio:
```java
// ANTES:
Map.of()
// DESPUÉS:
Map.of(), "GR01", "MENSUAL"
```

Adicionalmente hay un caso especial `null` al final en `nullEmployeeInputsIsRejected`:
```java
// ANTES:
new BigDecimal("100"), new BigDecimal("2000.00"), null)
// DESPUÉS:
new BigDecimal("100"), new BigDecimal("2000.00"), null, "GR01", "MENSUAL")
```

Los 4 tests nuevos añadidos en Step 1 ya usan la firma de 16 parámetros — no necesitan cambio.

- [ ] **Step 10: Actualizar `DefaultSegmentExecutionEngineEmployeeInputTest.java`**

La llamada en `contextWithInputs()` (línea ~32) termina con `employeeInputs`. Añadir `"GR01", "MENSUAL"` al final:

```java
// ANTES:
        return new SegmentCalculationContext(
                RULE_SYSTEM, "EMP", "EMP0001",
                periodStart, periodEnd,
                periodStart, periodEnd,
                true, true,
                30, 30,
                new BigDecimal("100"),
                new BigDecimal("2000.00"),
                employeeInputs
        );

// DESPUÉS:
        return new SegmentCalculationContext(
                RULE_SYSTEM, "EMP", "EMP0001",
                periodStart, periodEnd,
                periodStart, periodEnd,
                true, true,
                30, 30,
                new BigDecimal("100"),
                new BigDecimal("2000.00"),
                employeeInputs,
                "GR01",
                "MENSUAL"
        );
```

- [ ] **Step 11: Actualizar `SegmentExecutionEngineTest.java` — 4 sitios**

**Sitio 1** — `context100pct()` helper (línea ~92), termina con `Map.of()`:

```java
// ANTES (últimas 2 líneas del constructor):
                salary,
                Map.of()

// DESPUÉS:
                salary,
                Map.of(),
                "GR01",
                "MENSUAL"
```

**Sitios 2 y 3** — `directAmountPrecioDiaAt50PctIsHalfOfFull` y `salarioBaseAt50PctIs533_33` (líneas ~139 y ~180), mismo patrón:

```java
// ANTES:
                new BigDecimal("2000.00"),
                Map.of()

// DESPUÉS:
                new BigDecimal("2000.00"),
                Map.of(),
                "GR01",
                "MENSUAL"
```

(Hay exactamente 3 sitios en este fichero según el grep — los 3 usan `context100pct()` o inline, ambos terminan con `Map.of()`.)

- [ ] **Step 12: Actualizar `PayrollEnginePocExecutorTest.java` — 4 sitios**

Todos los `new PayrollEnginePocRequest(...)` en este fichero. Actualmente hay 4 sitios. El constructor tiene 7 params terminando con la lista de ventanas. Añadir `"GR01", "MENSUAL"` al final de cada uno:

**Sitio 1** — `referenceRequest()` (línea ~65):

```java
// ANTES (últimas 3 líneas del constructor):
                List.of(
                        new WorkingTimeWindow(APR_01, APR_14, new BigDecimal("100")),
                        new WorkingTimeWindow(APR_15, null,   new BigDecimal("50"))
                )

// DESPUÉS:
                List.of(
                        new WorkingTimeWindow(APR_01, APR_14, new BigDecimal("100")),
                        new WorkingTimeWindow(APR_15, null,   new BigDecimal("50"))
                ),
                "GR01",
                "MENSUAL"
```

**Sitios 2 y 3** (líneas ~146 y ~157) — misma transformación. Buscar cada `new PayrollEnginePocRequest(` y añadir `"GR01", "MENSUAL"` antes del cierre de paréntesis.

**Sitio 4** — `zeroRequest` (línea ~188):

```java
// ANTES:
        PayrollEnginePocRequest zeroRequest = new PayrollEnginePocRequest(
                "ESP", "EMP", "EMP0001", APR_01, APR_30,
                BigDecimal.ZERO,
                List.of(new WorkingTimeWindow(APR_01, null, new BigDecimal("100")))
        );

// DESPUÉS:
        PayrollEnginePocRequest zeroRequest = new PayrollEnginePocRequest(
                "ESP", "EMP", "EMP0001", APR_01, APR_30,
                BigDecimal.ZERO,
                List.of(new WorkingTimeWindow(APR_01, null, new BigDecimal("100"))),
                "GR01",
                "MENSUAL"
        );
```

- [ ] **Step 13: Actualizar `DefaultEligiblePayrollExecutorTest.java` — 1 sitio**

El helper `referenceRequest()` (línea ~155). El constructor actualmente tiene 9 params. Añadir `"GR01", "MENSUAL"`:

```java
// ANTES:
        return new EligiblePayrollExecutionRequest(
                RS, "EMP", "EMP0001", "EMP1", "METAL",
                APR_01, APR_30,
                new BigDecimal("2000.00"),
                List.of(
                        new WorkingTimeWindow(APR_01, APR_14, new BigDecimal("100")),
                        new WorkingTimeWindow(APR_15, null, new BigDecimal("50"))
                )
        );

// DESPUÉS:
        return new EligiblePayrollExecutionRequest(
                RS, "EMP", "EMP0001", "EMP1", "METAL",
                APR_01, APR_30,
                new BigDecimal("2000.00"),
                List.of(
                        new WorkingTimeWindow(APR_01, APR_14, new BigDecimal("100")),
                        new WorkingTimeWindow(APR_15, null, new BigDecimal("50"))
                ),
                "GR01",
                "MENSUAL"
        );
```

- [ ] **Step 14: Ejecutar la suite completa**

```
mvn test --no-transfer-progress
```

Salida esperada: `BUILD SUCCESS`. Todos los tests previos deben seguir pasando.

- [ ] **Step 15: Commit**

```
git add src/main/java/com/b4rrhh/payroll_engine/segment/domain/model/SegmentCalculationContext.java
git add src/main/java/com/b4rrhh/payroll_engine/execution/domain/model/PayrollEnginePocRequest.java
git add src/main/java/com/b4rrhh/payroll_engine/planning/application/service/EligiblePayrollExecutionRequest.java
git add src/main/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngine.java
git add src/main/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultPayrollEnginePocExecutor.java
git add src/main/java/com/b4rrhh/payroll_engine/planning/application/service/DefaultEligiblePayrollExecutor.java
git add src/test/java/com/b4rrhh/payroll_engine/segment/domain/model/SegmentCalculationContextTest.java
git add src/test/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngineEmployeeInputTest.java
git add src/test/java/com/b4rrhh/payroll_engine/execution/application/service/SegmentExecutionEngineTest.java
git add src/test/java/com/b4rrhh/payroll_engine/execution/application/service/PayrollEnginePocExecutorTest.java
git add src/test/java/com/b4rrhh/payroll_engine/planning/application/service/DefaultEligiblePayrollExecutorTest.java
git commit -m "feat(payroll-engine): propagate grupoCotizacionCode and tipoNomina through SegmentCalculationContext"
```

---

### Task 3: Tests y commit para los calculadores de tope

**Files:**
- Create: `src/test/java/com/b4rrhh/payroll_engine/execution/application/service/TopeMaxCotizacionCalculatorTest.java`
- Create: `src/test/java/com/b4rrhh/payroll_engine/execution/application/service/TopeMinCotizacionCalculatorTest.java`

- [ ] **Step 1: Escribir `TopeMaxCotizacionCalculatorTest`**

```java
package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.SsCotizacionTope;
import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import com.b4rrhh.payroll_engine.execution.domain.port.SsCotizacionTopesRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TopeMaxCotizacionCalculatorTest {

    private static final LocalDate APR_01 = LocalDate.of(2026, 4, 1);
    private static final LocalDate APR_14 = LocalDate.of(2026, 4, 14);
    private static final LocalDate APR_30 = LocalDate.of(2026, 4, 30);
    private static final SsCotizacionTope TOPE =
            new SsCotizacionTope(new BigDecimal("1260.00"), new BigDecimal("4495.50"));

    private static SsCotizacionTopesRepository repoReturning(SsCotizacionTope tope) {
        return (ruleSystemCode, grupoCode, periodType, referenceDate) -> Optional.of(tope);
    }

    private static SsCotizacionTopesRepository repoEmpty() {
        return (ruleSystemCode, grupoCode, periodType, referenceDate) -> Optional.empty();
    }

    @Test
    void conceptCodeIsP_TOPE_MAX() {
        assertThat(new TopeMaxCotizacionCalculator(repoReturning(TOPE)).conceptCode())
                .isEqualTo("P_TOPE_MAX");
    }

    @Test
    void proratesMonthlyBaseMaxBySegmentDays() {
        // Monthly baseMax = 4495.50, period = April (30 days), segment = 14 days
        // 4495.50 * 14 / 30 = 2097.90
        TechnicalConceptSegmentData ctx = new TechnicalConceptSegmentData(
                APR_01, APR_30, APR_01, APR_14,
                14, BigDecimal.ONE, "ESP", "GR01", "MENSUAL"
        );

        BigDecimal result = new TopeMaxCotizacionCalculator(repoReturning(TOPE)).resolve(ctx);

        assertThat(result).isEqualByComparingTo(new BigDecimal("2097.90"));
    }

    @Test
    void multipliesDailyBaseMaxBySegmentDaysWhenDiario() {
        // Daily baseMax = 149.85, segment = 14 days → 149.85 * 14 = 2097.90
        SsCotizacionTope topeDiario =
                new SsCotizacionTope(new BigDecimal("42.00"), new BigDecimal("149.85"));
        TechnicalConceptSegmentData ctx = new TechnicalConceptSegmentData(
                APR_01, APR_30, APR_01, APR_14,
                14, BigDecimal.ONE, "ESP", "GR01", "DIARIO"
        );

        BigDecimal result = new TopeMaxCotizacionCalculator(repoReturning(topeDiario)).resolve(ctx);

        assertThat(result).isEqualByComparingTo(new BigDecimal("2097.90"));
    }

    @Test
    void missingTopeEntryThrowsIllegalStateException() {
        TechnicalConceptSegmentData ctx = new TechnicalConceptSegmentData(
                APR_01, APR_30, APR_01, APR_14,
                14, BigDecimal.ONE, "ESP", "GR01", "MENSUAL"
        );

        assertThatThrownBy(() -> new TopeMaxCotizacionCalculator(repoEmpty()).resolve(ctx))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("grupo=GR01");
    }
}
```

- [ ] **Step 2: Escribir `TopeMinCotizacionCalculatorTest`**

```java
package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.SsCotizacionTope;
import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import com.b4rrhh.payroll_engine.execution.domain.port.SsCotizacionTopesRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TopeMinCotizacionCalculatorTest {

    private static final LocalDate APR_01 = LocalDate.of(2026, 4, 1);
    private static final LocalDate APR_14 = LocalDate.of(2026, 4, 14);
    private static final LocalDate APR_30 = LocalDate.of(2026, 4, 30);
    private static final SsCotizacionTope TOPE =
            new SsCotizacionTope(new BigDecimal("1260.00"), new BigDecimal("4495.50"));

    private static SsCotizacionTopesRepository repoReturning(SsCotizacionTope tope) {
        return (ruleSystemCode, grupoCode, periodType, referenceDate) -> Optional.of(tope);
    }

    private static SsCotizacionTopesRepository repoEmpty() {
        return (ruleSystemCode, grupoCode, periodType, referenceDate) -> Optional.empty();
    }

    @Test
    void conceptCodeIsP_TOPE_MIN() {
        assertThat(new TopeMinCotizacionCalculator(repoReturning(TOPE)).conceptCode())
                .isEqualTo("P_TOPE_MIN");
    }

    @Test
    void proratesMonthlyBaseMinBySegmentDays() {
        // Monthly baseMin = 1260.00, period = April (30 days), segment = 14 days
        // 1260.00 * 14 / 30 = 588.00
        TechnicalConceptSegmentData ctx = new TechnicalConceptSegmentData(
                APR_01, APR_30, APR_01, APR_14,
                14, BigDecimal.ONE, "ESP", "GR01", "MENSUAL"
        );

        BigDecimal result = new TopeMinCotizacionCalculator(repoReturning(TOPE)).resolve(ctx);

        assertThat(result).isEqualByComparingTo(new BigDecimal("588.00"));
    }

    @Test
    void multipliesDailyBaseMinBySegmentDaysWhenDiario() {
        // Daily baseMin = 42.00, segment = 14 days → 42.00 * 14 = 588.00
        SsCotizacionTope topeDiario =
                new SsCotizacionTope(new BigDecimal("42.00"), new BigDecimal("149.85"));
        TechnicalConceptSegmentData ctx = new TechnicalConceptSegmentData(
                APR_01, APR_30, APR_01, APR_14,
                14, BigDecimal.ONE, "ESP", "GR01", "DIARIO"
        );

        BigDecimal result = new TopeMinCotizacionCalculator(repoReturning(topeDiario)).resolve(ctx);

        assertThat(result).isEqualByComparingTo(new BigDecimal("588.00"));
    }

    @Test
    void missingTopeEntryThrowsIllegalStateException() {
        TechnicalConceptSegmentData ctx = new TechnicalConceptSegmentData(
                APR_01, APR_30, APR_01, APR_14,
                14, BigDecimal.ONE, "ESP", "GR01", "MENSUAL"
        );

        assertThatThrownBy(() -> new TopeMinCotizacionCalculator(repoEmpty()).resolve(ctx))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("grupo=GR01");
    }
}
```

- [ ] **Step 3: Ejecutar los dos tests — deben PASAR**

```
mvn test -Dtest="TopeMaxCotizacionCalculatorTest,TopeMinCotizacionCalculatorTest" --no-transfer-progress
```

Salida esperada: `BUILD SUCCESS`, 8 tests passed.

- [ ] **Step 4: Commit**

```
git add src/main/java/com/b4rrhh/payroll_engine/execution/application/service/TopeMaxCotizacionCalculator.java
git add src/main/java/com/b4rrhh/payroll_engine/execution/application/service/TopeMinCotizacionCalculator.java
git add src/main/java/com/b4rrhh/payroll_engine/execution/domain/model/SsCotizacionTope.java
git add src/main/java/com/b4rrhh/payroll_engine/execution/domain/port/SsCotizacionTopesRepository.java
git add src/main/java/com/b4rrhh/payroll_engine/execution/infrastructure/persistence/SsCotizacionTopesJdbcAdapter.java
git add src/test/java/com/b4rrhh/payroll_engine/execution/application/service/TopeMaxCotizacionCalculatorTest.java
git add src/test/java/com/b4rrhh/payroll_engine/execution/application/service/TopeMinCotizacionCalculatorTest.java
git commit -m "feat(payroll-engine): add TopeMax/TopeMinCotizacionCalculator with DB-backed prorated caps"
```

---

### Task 4: Verificación final de la suite completa

**Files:** ninguno nuevo.

- [ ] **Step 1: Ejecutar la suite completa**

```
mvn test --no-transfer-progress
```

Salida esperada: `BUILD SUCCESS`. No deben existir tests marcados como `FAILED` o `ERROR`.

- [ ] **Step 2: Verificar migraciones Flyway**

Arrancar el backend (requiere PostgreSQL en `localhost:5432/b4rrhh`):

```
cd docker/postgres && docker compose up -d
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=default"
```

Verificar en los logs que Flyway aplica las migraciones sin errores:

```
Flyway Community Edition ... by Redgate
Successfully validated ... migrations
Successfully applied ... migrations to schema "public"
```

Si hay error de Flyway, NO editar migraciones existentes — crear una nueva migración `VXX__fix_...sql`.

- [ ] **Step 3: Confirmar estado limpio de git**

```
git status
```

Salida esperada: `nothing to commit, working tree clean`.

---

## Math verification

Proration for TopeMax/TopeMin tests:

| tipoNomina | base     | daysInSegment | daysInPeriod | formula                   | result  |
|------------|----------|---------------|--------------|---------------------------|---------|
| MENSUAL    | 4495.50  | 14            | 30           | 4495.50 × 14 / 30         | 2097.90 |
| DIARIO     | 149.85   | 14            | —            | 149.85 × 14               | 2097.90 |
| MENSUAL    | 1260.00  | 14            | 30           | 1260.00 × 14 / 30         | 588.00  |
| DIARIO     | 42.00    | 14            | —            | 42.00 × 14                | 588.00  |
