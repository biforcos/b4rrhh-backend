# Level 2 — Wiring del Segment Engine

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminar el switch inline de 100 líneas del bucle por segmento en `CalculatePayrollUnitService` delegando toda la ejecución por segmento a `DefaultSegmentExecutionEngine`, el único lugar donde esa lógica debe vivir.

**Architecture:** Se añaden tres campos a `SegmentCalculationContext` (`grupoCotizacionCode`, `tipoNomina`, `precomputedDirectAmounts`) que permiten al engine resolver tanto conceptos técnicos ENGINE_PROVIDED como conceptos DIRECT_AMOUNT de origen rule-system. `CalculatePayrollUnitService` pre-calcula los conceptos DIRECT_AMOUNT una sola vez antes del bucle de segmentos (son period-level, invariables entre segmentos), inyecta el engine y lo llama por segmento, y luego itera el plan para fusionar resultados en `composedState` y construir las filas del recibo. El bucle de conceptos aggregate (post-segmento) queda intacto.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Mockito. Maven (`mvn test --no-transfer-progress`).

---

## Mapa de ficheros

### Modificar (Task 1)
- `src/main/java/com/b4rrhh/payroll_engine/segment/domain/model/SegmentCalculationContext.java` — añadir 3 campos + getters + validación
- `src/test/java/com/b4rrhh/payroll_engine/segment/domain/model/SegmentCalculationContextTest.java` — actualizar todos los constructores + tests de validación nuevos
- `src/main/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngine.java` — ENGINE_PROVIDED pasa los 2 nuevos campos; DIRECT_AMOUNT usa precomputedDirectAmounts antes de SegmentTechnicalValueResolver
- `src/test/java/com/b4rrhh/payroll_engine/execution/application/service/SegmentExecutionEngineTest.java` — actualizar helpers de contexto + nuevo test para DIRECT_AMOUNT precomputado
- `src/test/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngineEmployeeInputTest.java` — actualizar helper de contexto

### Modificar (Task 2)
- `src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java` — añadir `segmentEngine`; pre-calcular DIRECT_AMOUNT; reemplazar switch per-segmento con engine.execute(); bucle de merge
- `src/test/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitServiceTest.java` — añadir mock `segmentEngine`; actualizar constructores; actualizar aserciones

---

## Task 1: Extender SegmentCalculationContext y actualizar el engine

**Files:**
- Modify: `src/main/java/com/b4rrhh/payroll_engine/segment/domain/model/SegmentCalculationContext.java`
- Modify: `src/test/java/com/b4rrhh/payroll_engine/segment/domain/model/SegmentCalculationContextTest.java`
- Modify: `src/main/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngine.java`
- Modify: `src/test/java/com/b4rrhh/payroll_engine/execution/application/service/SegmentExecutionEngineTest.java`
- Modify: `src/test/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngineEmployeeInputTest.java`

- [ ] **Step 1: Añadir 3 campos a `SegmentCalculationContext`**

Añadir al bloque de campos (después de `employeeInputs`):
```java
private final String grupoCotizacionCode;
private final String tipoNomina;
private final Map<String, BigDecimal> precomputedDirectAmounts;
```

Ampliar el constructor añadiendo 3 parámetros al final (mantener los 14 existentes, añadir 3 al final):
```java
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
        String tipoNomina,
        Map<String, BigDecimal> precomputedDirectAmounts
) {
```

En el bloque de validación, después de `requireNonNull(employeeInputs, "employeeInputs");`, añadir:
```java
requireNonBlank(grupoCotizacionCode, "grupoCotizacionCode");
requireNonBlank(tipoNomina, "tipoNomina");
requireNonNull(precomputedDirectAmounts, "precomputedDirectAmounts");
```

En el bloque de asignaciones, después de `this.employeeInputs = employeeInputs;`, añadir:
```java
this.grupoCotizacionCode = grupoCotizacionCode;
this.tipoNomina = tipoNomina;
this.precomputedDirectAmounts = precomputedDirectAmounts;
```

Añadir 3 getters al final del fichero (antes del cierre `}`):
```java
public String getGrupoCotizacionCode() { return grupoCotizacionCode; }
public String getTipoNomina() { return tipoNomina; }
public Map<String, BigDecimal> getPrecomputedDirectAmounts() { return precomputedDirectAmounts; }
```

- [ ] **Step 2: Actualizar `SegmentCalculationContextTest` — helper `valid()`**

El helper `valid()` pasa de 14 a 17 parámetros. Reemplazar:
```java
private SegmentCalculationContext valid() {
    return new SegmentCalculationContext(
            "ESP", "EMP", "EMP0001",
            APR_01, APR_30,
            APR_01, APR_14,
            true, false,
            30L, 14L,
            new BigDecimal("100"),
            new BigDecimal("2000.00"),
            Map.of()
    );
}
```
Por:
```java
private SegmentCalculationContext valid() {
    return new SegmentCalculationContext(
            "ESP", "EMP", "EMP0001",
            APR_01, APR_30,
            APR_01, APR_14,
            true, false,
            30L, 14L,
            new BigDecimal("100"),
            new BigDecimal("2000.00"),
            Map.of(),
            "G02",
            "MENSUAL",
            Map.of()
    );
}
```

Actualizar también cada una de las 14 invocaciones inline de `new SegmentCalculationContext(...)` que aparecen en los tests que lanzan `IllegalArgumentException`. Cada uno tiene 14 parámetros — añadir `"G02", "MENSUAL", Map.of()` al final. Ejemplo (el test `nullRuleSystemCodeIsRejected`):
```java
assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
        null, "EMP", "EMP0001",
        APR_01, APR_30, APR_01, APR_14,
        true, false, 30L, 14L,
        new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
        "G02", "MENSUAL", Map.of()));
```
Hacer lo mismo para los 13 tests restantes del fichero.

- [ ] **Step 3: Añadir tests de validación nuevos en `SegmentCalculationContextTest`**

Al final del fichero (antes del cierre `}`), añadir:
```java
@Test
void nullGrupoCotizacionCodeIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
            "ESP", "EMP", "EMP0001",
            APR_01, APR_30, APR_01, APR_14,
            true, false, 30L, 14L,
            new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
            null, "MENSUAL", Map.of()));
}

@Test
void blankGrupoCotizacionCodeIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
            "ESP", "EMP", "EMP0001",
            APR_01, APR_30, APR_01, APR_14,
            true, false, 30L, 14L,
            new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
            "  ", "MENSUAL", Map.of()));
}

@Test
void nullTipoNominaIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
            "ESP", "EMP", "EMP0001",
            APR_01, APR_30, APR_01, APR_14,
            true, false, 30L, 14L,
            new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
            "G02", null, Map.of()));
}

@Test
void blankTipoNominaIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
            "ESP", "EMP", "EMP0001",
            APR_01, APR_30, APR_01, APR_14,
            true, false, 30L, 14L,
            new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
            "G02", "  ", Map.of()));
}

@Test
void nullPrecomputedDirectAmountsIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new SegmentCalculationContext(
            "ESP", "EMP", "EMP0001",
            APR_01, APR_30, APR_01, APR_14,
            true, false, 30L, 14L,
            new BigDecimal("100"), new BigDecimal("2000.00"), Map.of(),
            "G02", "MENSUAL", null));
}
```

- [ ] **Step 4: Actualizar `DefaultSegmentExecutionEngine` — ENGINE_PROVIDED y DIRECT_AMOUNT**

En el método `execute()`, localizar el case `ENGINE_PROVIDED`. Reemplazar las dos ocurrencias de `null` al construir `TechnicalConceptSegmentData`:

```java
case ENGINE_PROVIDED -> {
    String conceptCode = entry.identity().getConceptCode();
    TechnicalConceptCalculator calculator = technicalCalculators.get(conceptCode);
    if (calculator == null) {
        throw new UnsupportedTechnicalConceptException(conceptCode);
    }
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
}
```

Localizar el case `DIRECT_AMOUNT`. Reemplazar:
```java
case DIRECT_AMOUNT ->
        technicalValueResolver.resolve(entry.identity().getConceptCode(), context);
```
Por:
```java
case DIRECT_AMOUNT -> {
    String conceptCode = entry.identity().getConceptCode();
    BigDecimal precomputed = context.getPrecomputedDirectAmounts().get(conceptCode);
    if (precomputed != null) {
        yield precomputed;
    }
    yield technicalValueResolver.resolve(conceptCode, context);
}
```

- [ ] **Step 5: Actualizar helpers de contexto en los tests del engine**

En `SegmentExecutionEngineTest`, el helper `context100pct` tiene el constructor con 14 parámetros. Ampliar a 17:
```java
private static SegmentCalculationContext context100pct(int daysInPeriod, int daysInSegment, BigDecimal salary) {
    LocalDate periodStart = LocalDate.of(2026, 4, 1);
    LocalDate periodEnd   = LocalDate.of(2026, 4, 30);
    LocalDate segStart    = LocalDate.of(2026, 4, 1);
    LocalDate segEnd      = segStart.plusDays(daysInSegment - 1);
    return new SegmentCalculationContext(
            RULE_SYSTEM, "EMP", "EMP0001",
            periodStart, periodEnd, segStart, segEnd,
            true, true,
            daysInPeriod, daysInSegment,
            new BigDecimal("100"),
            salary,
            Map.of(),
            "G02",
            "MENSUAL",
            Map.of()
    );
}
```

Localizar también los dos tests que crean `SegmentCalculationContext` inline (los que usan `50%` de jornada — buscar `new BigDecimal("50")` en el fichero). Añadir `"G02", "MENSUAL", Map.of()` al final de esos constructores también.

En `DefaultSegmentExecutionEngineEmployeeInputTest`, el helper `contextWithInputs` tiene el constructor con 14 parámetros. Ampliar a 17:
```java
private static SegmentCalculationContext contextWithInputs(Map<String, BigDecimal> employeeInputs) {
    LocalDate periodStart = LocalDate.of(2026, 4, 1);
    LocalDate periodEnd   = LocalDate.of(2026, 4, 30);
    return new SegmentCalculationContext(
            RULE_SYSTEM, "EMP", "EMP0001",
            periodStart, periodEnd,
            periodStart, periodEnd,
            true, true,
            30, 30,
            new BigDecimal("100"),
            new BigDecimal("2000.00"),
            employeeInputs,
            "G02",
            "MENSUAL",
            Map.of()
    );
}
```

- [ ] **Step 6: Añadir test de DIRECT_AMOUNT con precomputedDirectAmounts en `SegmentExecutionEngineTest`**

Al final del fichero (antes del cierre `}`), añadir un nuevo test:

```java
@Test
void directAmountUsesPrecomputedValueWhenConceptCodeIsNotTechnical() {
    BigDecimal precioDiaPleno = new BigDecimal("56.32");
    ConceptNodeIdentity priceId = new ConceptNodeIdentity(RULE_SYSTEM, "PRECIO_DIA_PLENO");

    SegmentCalculationContext ctx = new SegmentCalculationContext(
            RULE_SYSTEM, "EMP", "EMP0001",
            LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30),
            LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 14),
            true, false,
            30L, 14L,
            new BigDecimal("100"),
            new BigDecimal("2000.00"),
            Map.of(),
            "G02",
            "MENSUAL",
            Map.of("PRECIO_DIA_PLENO", precioDiaPleno)
    );

    List<ConceptExecutionPlanEntry> plan = List.of(
            new ConceptExecutionPlanEntry(priceId, CalculationType.DIRECT_AMOUNT)
    );

    SegmentExecutionState state = engine.execute(plan, ctx);

    assertEquals(0, precioDiaPleno.compareTo(state.getRequiredAmount(priceId)),
            "DIRECT_AMOUNT must return precomputed value when conceptCode is in precomputedDirectAmounts");
}
```

Añadir el import necesario en el fichero si no está ya:
```java
import java.time.LocalDate;
```
(Verificar que ya existe antes de añadirlo.)

- [ ] **Step 7: Compilar y pasar tests**

```
mvn test --no-transfer-progress
```

Salida esperada: `BUILD SUCCESS`. Todos los tests existentes del engine pasan porque:
- T_-prefixed DIRECT_AMOUNT sigue funcionando vía `SegmentTechnicalValueResolver` (fallback cuando `precomputedDirectAmounts` no tiene la clave)
- ENGINE_PROVIDED ahora recibe "G02"/"MENSUAL" en vez de null — ningún test de producción llama a calculators reales, así que no hay cambio de comportamiento
- El nuevo test verifica el path de `precomputedDirectAmounts`

- [ ] **Step 8: Commit**

```
git add src/main/java/com/b4rrhh/payroll_engine/segment/domain/model/SegmentCalculationContext.java
git add src/test/java/com/b4rrhh/payroll_engine/segment/domain/model/SegmentCalculationContextTest.java
git add src/main/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngine.java
git add src/test/java/com/b4rrhh/payroll_engine/execution/application/service/SegmentExecutionEngineTest.java
git add src/test/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultSegmentExecutionEngineEmployeeInputTest.java
git commit -m "feat(payroll-engine): add grupoCotizacion/tipoNomina/precomputedDirectAmounts to SegmentCalculationContext"
```

---

## Task 2: Inyectar engine en CalculatePayrollUnitService y reemplazar el switch per-segmento

**Files:**
- Modify: `src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java`
- Modify: `src/test/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitServiceTest.java`

- [ ] **Step 1: Añadir los imports necesarios a `CalculatePayrollUnitService`**

Localizar el bloque de imports. Añadir (si no están):
```java
import com.b4rrhh.payroll_engine.execution.application.service.SegmentExecutionEngine;
import com.b4rrhh.payroll_engine.execution.domain.model.SegmentExecutionState;
import com.b4rrhh.payroll_engine.segment.domain.model.SegmentCalculationContext;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
```

Eliminar el import de `TechnicalConceptCalculator` solo si ya no se referencia en el fichero (comprobar antes de borrar — todavía se usa en el bucle aggregate). Si se usa en el aggregate loop, dejarlo.

- [ ] **Step 2: Añadir `segmentEngine` como campo y parámetro de constructor**

En el bloque de campos, añadir (después de `getAgreementCategoryProfileUseCase`):
```java
private final SegmentExecutionEngine segmentEngine;
```

En el constructor, añadir el parámetro al final de la lista (después de `GetAgreementCategoryProfileUseCase getAgreementCategoryProfileUseCase`):
```java
SegmentExecutionEngine segmentEngine
```

En el cuerpo del constructor, añadir la asignación al final (después de `this.getAgreementCategoryProfileUseCase = getAgreementCategoryProfileUseCase;`):
```java
this.segmentEngine = segmentEngine;
```

- [ ] **Step 3: Pre-computar los conceptos DIRECT_AMOUNT antes del bucle de segmentos**

En `calculateEligibleReal()`, localizar el bloque donde termina la carga de `employeeInputsForPeriod`. Justo después de esas líneas y **antes** del `for (SegmentSpec seg : segments)`, añadir:

```java
Map<String, BigDecimal> precomputedDirectAmounts = new HashMap<>();
for (ConceptExecutionPlanEntry entry : perSegmentPlan) {
    if (entry.calculationType() == com.b4rrhh.payroll_engine.concept.domain.model.CalculationType.DIRECT_AMOUNT) {
        String conceptCode = entry.identity().getConceptCode();
        PayrollConceptExecutionResult directResult =
                payrollConceptGraphCalculator.calculateConceptResult(conceptCode, calcContext);
        precomputedDirectAmounts.put(conceptCode, directResult.amount());
        log.debug("[NÓMINA] Pre-calculado DIRECT_AMOUNT {} = {}", conceptCode, directResult.amount());
    }
}
long daysInPeriod = ChronoUnit.DAYS.between(command.periodStart(), command.periodEnd()) + 1;
BigDecimal monthlySalary = Objects.requireNonNullElse(
        payrollLaunchExecutionProperties.getEligibleRealMonthlySalaryAmount(), BigDecimal.ZERO);
```

- [ ] **Step 4: Reemplazar el bucle per-segmento con engine.execute()**

Localizar el bloque completo del `for (SegmentSpec seg : segments) { ... }`. Eliminarlo por completo y reemplazarlo con:

```java
for (int segIdx = 0; segIdx < segments.size(); segIdx++) {
    SegmentSpec seg = segments.get(segIdx);
    boolean isFirst = (segIdx == 0);
    boolean isLast  = (segIdx == segments.size() - 1);

    log.info("[NÓMINA] ▶ Segmento {} → {} ({} días, jornada={}%)",
            seg.segmentStart(), seg.segmentEnd(), seg.daysInSegment(), seg.workingTimePercentage());

    SegmentCalculationContext segCtx = new SegmentCalculationContext(
            command.ruleSystemCode(),
            command.employeeTypeCode(),
            command.employeeNumber(),
            command.periodStart(),
            command.periodEnd(),
            seg.segmentStart(),
            seg.segmentEnd(),
            isFirst,
            isLast,
            daysInPeriod,
            seg.daysInSegment(),
            seg.workingTimePercentage(),
            monthlySalary,
            employeeInputsForPeriod,
            grupoCotizacionCode,
            tipoNomina,
            precomputedDirectAmounts
    );

    SegmentExecutionState state = segmentEngine.execute(perSegmentPlan, segCtx);

    for (ConceptExecutionPlanEntry entry : perSegmentPlan) {
        String conceptCode = entry.identity().getConceptCode();
        BigDecimal amount = state.getRequiredAmount(entry.identity());
        BigDecimal quantity = null;
        BigDecimal rate = null;

        if (entry.calculationType() == com.b4rrhh.payroll_engine.concept.domain.model.CalculationType.RATE_BY_QUANTITY) {
            quantity = state.getRequiredAmount(entry.operands().get(OperandRole.QUANTITY));
            rate     = state.getRequiredAmount(entry.operands().get(OperandRole.RATE));
        }

        log.info("[NÓMINA] {} {} = {} (q={} r={})", entry.calculationType(), conceptCode, amount, quantity, rate);

        var engineConcept = engineConceptByCode.get(conceptCode);
        FunctionalNature nature = engineConcept.getFunctionalNature();
        if (isAccumulable(nature)) {
            composedState.merge(entry.identity(), amount, BigDecimal::add);
        } else {
            composedState.put(entry.identity(), amount);
        }
        if (engineConcept.getPayslipOrderCode() != null) {
            int displayOrder = Integer.parseInt(engineConcept.getPayslipOrderCode());
            if (!isAccumulable(nature)) {
                payslipRows.removeIf(r -> r.conceptCode().equals(conceptCode));
            }
            payslipRows.add(new ConceptRow(conceptCode, engineConcept.getConceptMnemonic(),
                    amount, quantity, rate, nature.name(), displayOrder));
        }
    }
}
```

- [ ] **Step 5: Actualizar `CalculatePayrollUnitServiceTest` — añadir mock del engine**

Añadir el campo mock al principio de la clase (junto a los otros `@Mock`):
```java
@Mock
private SegmentExecutionEngine segmentExecutionEngine;
```

Añadir el import:
```java
import com.b4rrhh.payroll_engine.execution.application.service.SegmentExecutionEngine;
import com.b4rrhh.payroll_engine.execution.domain.model.SegmentExecutionState;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptNodeIdentity;
```

- [ ] **Step 6: Actualizar el constructor del servicio en ambos tests**

En los dos tests restantes (`eligibleRealMode_persistsSingleConcept101FromMinimalExecutor` y `eligibleRealMode_missingAgreementCategory_throwsExplicitException`), la construcción del servicio pasa de 12 a 13 argumentos. Añadir `segmentExecutionEngine` al final:

```java
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
    List.of(),
    employeePayrollInputLookupPort,
    getAgreementCategoryProfileUseCase,
    segmentExecutionEngine
);
```

- [ ] **Step 7: Actualizar el test `eligibleRealMode_persistsSingleConcept101FromMinimalExecutor`**

El plan entry para concept 101 es `CalculationType.DIRECT_AMOUNT`. Con Level 2:
- El servicio pre-computa concept 101 vía `payrollConceptGraphCalculator` → lo pone en `precomputedDirectAmounts`
- El engine devuelve el valor en `SegmentExecutionState`
- El merge loop extrae `amount` desde el state, `quantity=null`, `rate=null` (DIRECT_AMOUNT no tiene operandos)

El test necesita:
1. Que `payrollConceptGraphCalculator.calculateConceptResult("101", any())` ya está mockeado → devuelve `PayrollConceptExecutionResult("101", 1425.00, null, null)` — cambiar de `new BigDecimal("30")` y `new BigDecimal("47.50")` a `null, null`:

```java
when(payrollConceptGraphCalculator.calculateConceptResult(
        org.mockito.ArgumentMatchers.eq("101"),
        org.mockito.ArgumentMatchers.any()))
        .thenReturn(new PayrollConceptExecutionResult("101", new BigDecimal("1425.00"), null, null));
```

2. Añadir mock para `segmentEngine.execute()`. Justo antes de `service.calculate(...)`:

```java
SegmentExecutionState segmentState = new SegmentExecutionState();
segmentState.storeResult(new ConceptNodeIdentity("ESP", "101"), new BigDecimal("1425.00"));
when(segmentExecutionEngine.execute(
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any()))
        .thenReturn(segmentState);
```

3. Actualizar las aserciones: eliminar las líneas de `quantity` y `rate` (son null para DIRECT_AMOUNT):

```java
CalculatePayrollCommand persisted = captor.getValue();
assertEquals(1, persisted.concepts().size());
assertEquals("101", persisted.concepts().getFirst().getConceptCode());
assertEquals(0, new BigDecimal("1425.00").compareTo(persisted.concepts().getFirst().getAmount()));
// quantity y rate son null para DIRECT_AMOUNT — no verificar
```

Eliminar también el `when(payrollConceptGraphCalculator.calculate...)` original si era para un `CalculatePayrollCommand` (el que llama a `calculatePayrollUseCase`); ese sigue existiendo y ya está mockeado.

4. Añadir import de `SegmentExecutionState` y `ConceptNodeIdentity` si no están:
```java
import com.b4rrhh.payroll_engine.execution.domain.model.SegmentExecutionState;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptNodeIdentity;
```

- [ ] **Step 8: Compilar y pasar la suite completa**

```
mvn test --no-transfer-progress
```

Salida esperada: `BUILD SUCCESS`. Los tests que deben pasar:
- `CalculatePayrollUnitServiceTest` (2 tests)
- `LaunchPayrollCalculationEligibleRealEndToEndIntegrationTest`
- Todos los tests del engine (`SegmentExecutionEngineTest`, `DefaultSegmentExecutionEngineEmployeeInputTest`)
- Todo lo demás (~1220+ tests)

Si falla la compilación, las causas más probables son:
- Falta el nuevo parámetro `segmentEngine` en algún `new CalculatePayrollUnitService(...)` → añadirlo
- Algún constructor de `SegmentCalculationContext` con 14 parámetros que no se actualizó → añadir `"G02", "MENSUAL", Map.of()` al final

- [ ] **Step 9: Verificación post-refactor**

Ejecutar:
```
grep -r "switch.*calculationType\|case DIRECT_AMOUNT\|case RATE_BY_QUANTITY\|case PERCENTAGE\|case ENGINE_PROVIDED" src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java
```

Debe devolver únicamente referencias en el bucle `aggregatePlan` (el per-segmento ya no tiene switch). El bucle per-segmento debe contener sólo la construcción del `SegmentCalculationContext`, la llamada a `segmentEngine.execute()`, y el bucle de merge.

- [ ] **Step 10: Commit**

```
git add src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java
git add src/test/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitServiceTest.java
git commit -m "refactor(payroll): delegate per-segment execution to SegmentExecutionEngine"
```

---

## Verificación final

```
mvn test --no-transfer-progress
```

Y verificar que no queda switch por calculationType en el bucle per-segmento:

```
grep -n "case DIRECT_AMOUNT\|case RATE_BY_QUANTITY\|case PERCENTAGE\|case ENGINE_PROVIDED\|case AGGREGATE" \
  src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java
```

Deben aparecer sólo líneas del bucle `aggregatePlan`. El servicio habrá pasado de ~740 líneas a ~580, eliminando ~160 líneas de lógica de ejecución duplicada.
