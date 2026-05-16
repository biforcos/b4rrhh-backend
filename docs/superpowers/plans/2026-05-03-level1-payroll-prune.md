# Level 1 — Poda del Payroll Engine

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminar todo el código muerto del motor de nómina — el ejecutor POC, el ejecutor eligible huérfano, el modo FAKE — dejando un único camino de ejecución: `calculateEligibleReal()` en `CalculatePayrollUnitService`.

**Architecture:** Eliminación pura de código sin funcionalidad nueva. Las tres agrupaciones son independientes entre sí y se pueden commitear por separado. Ninguna toca el camino ELIGIBLE_REAL que ya está en producción. El test suite que queda es más pequeño pero igualmente correcto.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Mockito.

---

## Mapa de ficheros

### Eliminar por completo (13 ficheros)

**Cluster POC:**
- `src/main/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultPayrollEnginePocExecutor.java`
- `src/main/java/com/b4rrhh/payroll_engine/execution/application/service/PayrollEnginePocExecutor.java`
- `src/main/java/com/b4rrhh/payroll_engine/execution/application/service/PocExecutionProjectionHelper.java`
- `src/main/java/com/b4rrhh/payroll_engine/execution/domain/exception/MissingPocConceptException.java`
- `src/main/java/com/b4rrhh/payroll_engine/execution/domain/model/PayrollEnginePocRequest.java`
- `src/main/java/com/b4rrhh/payroll_engine/execution/domain/model/PayrollEnginePocResult.java`
- `src/test/java/com/b4rrhh/payroll_engine/execution/application/service/PayrollEnginePocExecutorTest.java`

**Cluster eligible-executor muerto:**
- `src/main/java/com/b4rrhh/payroll_engine/planning/application/service/DefaultEligiblePayrollExecutor.java`
- `src/main/java/com/b4rrhh/payroll_engine/planning/application/service/ExecuteEligiblePayrollUseCase.java`
- `src/main/java/com/b4rrhh/payroll_engine/planning/application/service/EligiblePayrollExecutionRequest.java`
- `src/main/java/com/b4rrhh/payroll_engine/planning/application/service/EligiblePayrollExecutionResult.java`
- `src/test/java/com/b4rrhh/payroll_engine/planning/application/service/DefaultEligiblePayrollExecutorTest.java`

**Cluster FAKE (test E2E):**
- `src/test/java/com/b4rrhh/payroll/application/usecase/LaunchPayrollCalculationEndToEndIntegrationTest.java`

### Modificar (4 ficheros)

- `src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java` — eliminar switch, FAKE methods y referencias al enum
- `src/main/java/com/b4rrhh/payroll/application/usecase/PayrollExecutionMode.java` — eliminar fichero entero
- `src/main/java/com/b4rrhh/payroll/infrastructure/config/PayrollLaunchExecutionProperties.java` — eliminar campo `mode`
- `src/main/resources/application.yml` — eliminar línea `mode: ELIGIBLE_REAL`
- `src/test/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitServiceTest.java` — eliminar método `generatesDeterministicFakeConceptsAndSnapshotForInternalLaunchCalculation`

---

### Task 1: Eliminar el cluster POC

**Files:**
- Delete: los 7 ficheros listados arriba bajo "Cluster POC"

- [ ] **Step 1: Borrar los 7 ficheros del cluster POC**

```
git rm src/main/java/com/b4rrhh/payroll_engine/execution/application/service/DefaultPayrollEnginePocExecutor.java
git rm src/main/java/com/b4rrhh/payroll_engine/execution/application/service/PayrollEnginePocExecutor.java
git rm src/main/java/com/b4rrhh/payroll_engine/execution/application/service/PocExecutionProjectionHelper.java
git rm src/main/java/com/b4rrhh/payroll_engine/execution/domain/exception/MissingPocConceptException.java
git rm src/main/java/com/b4rrhh/payroll_engine/execution/domain/model/PayrollEnginePocRequest.java
git rm src/main/java/com/b4rrhh/payroll_engine/execution/domain/model/PayrollEnginePocResult.java
git rm src/test/java/com/b4rrhh/payroll_engine/execution/application/service/PayrollEnginePocExecutorTest.java
```

- [ ] **Step 2: Compilar y pasar tests**

```
mvn test --no-transfer-progress
```

Salida esperada: `BUILD SUCCESS`. Ningún fichero de producción importaba estas clases, por lo que no debe haber errores de compilación.

- [ ] **Step 3: Commit**

```
git commit -m "refactor(payroll-engine): delete POC executor cluster — 7 dead files"
```

---

### Task 2: Eliminar el cluster eligible-executor muerto

**Files:**
- Delete: los 5 ficheros listados arriba bajo "Cluster eligible-executor muerto"

- [ ] **Step 1: Borrar los 5 ficheros**

```
git rm src/main/java/com/b4rrhh/payroll_engine/planning/application/service/DefaultEligiblePayrollExecutor.java
git rm src/main/java/com/b4rrhh/payroll_engine/planning/application/service/ExecuteEligiblePayrollUseCase.java
git rm src/main/java/com/b4rrhh/payroll_engine/planning/application/service/EligiblePayrollExecutionRequest.java
git rm src/main/java/com/b4rrhh/payroll_engine/planning/application/service/EligiblePayrollExecutionResult.java
git rm src/test/java/com/b4rrhh/payroll_engine/planning/application/service/DefaultEligiblePayrollExecutorTest.java
```

- [ ] **Step 2: Compilar y pasar tests**

```
mvn test --no-transfer-progress
```

Salida esperada: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```
git commit -m "refactor(payroll-engine): delete dead eligible-executor cluster — 5 files"
```

---

### Task 3: Eliminar el modo FAKE

**Files:**
- Modify: `src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java`
- Delete: `src/main/java/com/b4rrhh/payroll/application/usecase/PayrollExecutionMode.java`
- Modify: `src/main/java/com/b4rrhh/payroll/infrastructure/config/PayrollLaunchExecutionProperties.java`
- Modify: `src/main/resources/application.yml`
- Delete: `src/test/java/com/b4rrhh/payroll/application/usecase/LaunchPayrollCalculationEndToEndIntegrationTest.java`
- Modify: `src/test/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitServiceTest.java`

- [ ] **Step 1: En `CalculatePayrollUnitService.java` — eliminar el import de `PayrollExecutionMode`**

Localizar y eliminar esta línea de imports (cerca de la línea 28, junto al resto de imports del paquete `payroll.application.usecase`):

```java
// ELIMINAR esta línea:
import com.b4rrhh.payroll.application.usecase.PayrollExecutionMode;
```

- [ ] **Step 2: En `CalculatePayrollUnitService.java` — reemplazar el switch por una llamada directa**

Localizar el cuerpo del método `calculate()`. Actualmente contiene:

```java
        return switch (payrollLaunchExecutionProperties.getMode()) {
            case ELIGIBLE_REAL -> calculateEligibleReal(command);
            case FAKE -> calculateFake(command);
        };
```

Reemplazar por:

```java
        return calculateEligibleReal(command);
```

- [ ] **Step 3: En `CalculatePayrollUnitService.java` — reemplazar las 5 referencias a `PayrollExecutionMode.ELIGIBLE_REAL.name()`**

Hacer un reemplazo global en el fichero: `PayrollExecutionMode.ELIGIBLE_REAL.name()` → `"ELIGIBLE_REAL"`.

Hay 5 ocurrencias, todas dentro de `calculateEligibleReal()` y sus métodos auxiliares. Después del reemplazo, el enum ya no se usa en ningún sitio del fichero.

- [ ] **Step 4: En `CalculatePayrollUnitService.java` — eliminar los 4 métodos privados del modo FAKE**

Localizar y eliminar los siguientes métodos privados (están al final del fichero, antes del método `toJson()`):

```java
    private Payroll calculateFake(CalculatePayrollUnitCommand command) {
        // ... 
    }

    private List<PayrollConcept> fakeConcepts(CalculatePayrollUnitCommand command) {
        // ...
    }

    private List<PayrollWarning> fakeWarnings(CalculatePayrollUnitCommand command) {
        // ...
    }

    private PayrollContextSnapshot fakeSnapshot(CalculatePayrollUnitCommand command) {
        // ...
    }
```

El método `toJson()` que viene después **NO se elimina** — lo usa el camino real.

- [ ] **Step 5: Borrar `PayrollExecutionMode.java`**

```
git rm src/main/java/com/b4rrhh/payroll/application/usecase/PayrollExecutionMode.java
```

- [ ] **Step 6: En `PayrollLaunchExecutionProperties.java` — eliminar el campo `mode`**

Eliminar:
1. El import de `PayrollExecutionMode`
2. El campo `mode` y su Javadoc
3. El getter `getMode()`
4. El setter `setMode()`

El fichero resultante contiene sólo:

```java
package com.b4rrhh.payroll.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "payroll.launch.execution")
public class PayrollLaunchExecutionProperties {

    private BigDecimal eligibleRealMonthlySalaryAmount;

    private boolean collapseSegmentRows = true;

    public BigDecimal getEligibleRealMonthlySalaryAmount() {
        return eligibleRealMonthlySalaryAmount;
    }

    public void setEligibleRealMonthlySalaryAmount(BigDecimal eligibleRealMonthlySalaryAmount) {
        this.eligibleRealMonthlySalaryAmount = eligibleRealMonthlySalaryAmount;
    }

    public boolean isCollapseSegmentRows() {
        return collapseSegmentRows;
    }

    public void setCollapseSegmentRows(boolean collapseSegmentRows) {
        this.collapseSegmentRows = collapseSegmentRows;
    }
}
```

- [ ] **Step 7: En `application.yml` — eliminar la línea `mode: ELIGIBLE_REAL`**

El bloque `payroll.launch.execution` pasa de:

```yaml
payroll:
  launch:
    execution:
      mode: ELIGIBLE_REAL
      collapse-segment-rows: true
```

A:

```yaml
payroll:
  launch:
    execution:
      collapse-segment-rows: true
```

- [ ] **Step 8: Borrar `LaunchPayrollCalculationEndToEndIntegrationTest.java`**

```
git rm src/test/java/com/b4rrhh/payroll/application/usecase/LaunchPayrollCalculationEndToEndIntegrationTest.java
```

- [ ] **Step 9: En `CalculatePayrollUnitServiceTest.java` — eliminar el test del modo FAKE**

Localizar y eliminar el método completo `generatesDeterministicFakeConceptsAndSnapshotForInternalLaunchCalculation` (el primero de los 3 tests del fichero). Ocupa desde `@Test` hasta el cierre `}` del método, incluyendo la configuración de `properties.setMode(PayrollExecutionMode.FAKE)`.

Eliminar también el import de `PayrollExecutionMode` del fichero (ya no se usará en ningún test).

Tras la eliminación el fichero debe tener exactamente 2 tests:
- `eligibleRealMode_persistsSingleConcept101FromMinimalExecutor`
- `eligibleRealMode_missingAgreementCategory_throwsExplicitException`

En **ambos** tests restantes, eliminar también la línea:

```java
properties.setMode(PayrollExecutionMode.ELIGIBLE_REAL);
```

Esa llamada ya no compilará porque `setMode()` se elimina en Step 6. La línea `PayrollLaunchExecutionProperties properties = new PayrollLaunchExecutionProperties();` se mantiene — el objeto properties aún se pasa al constructor del servicio.

- [ ] **Step 10: Compilar y pasar la suite completa**

```
mvn test --no-transfer-progress
```

Salida esperada: `BUILD SUCCESS`. Los tests que quedan son:
- `CalculatePayrollUnitServiceTest` (2 tests)
- `LaunchPayrollCalculationEligibleRealEndToEndIntegrationTest`
- Todos los tests del motor de ejecución (`SegmentExecutionEngineTest`, `DefaultSegmentExecutionEngineEmployeeInputTest`, etc.)
- Todos los tests de planificación (`DefaultEligibleExecutionPlanBuilderTest`, etc.)

- [ ] **Step 11: Commit final**

```
git add src/main/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitService.java
git add src/main/java/com/b4rrhh/payroll/infrastructure/config/PayrollLaunchExecutionProperties.java
git add src/main/resources/application.yml
git add src/test/java/com/b4rrhh/payroll/application/usecase/CalculatePayrollUnitServiceTest.java
git commit -m "refactor(payroll): remove FAKE mode — single execution path is ELIGIBLE_REAL"
```

---

## Verificación post-poda

Tras los 3 commits, ejecutar:

```
mvn test --no-transfer-progress
```

Y comprobar que el número de tests que pasan es coherente con lo esperado (los tests eliminados ya no aparecen). No debe haber ningún `FAKE`, `POC` ni `PocExecutor` referenciado en ningún fichero de producción.

```
# Verificación rápida — debe devolver vacío:
grep -r "FAKE\|PocExecutor\|EligiblePayrollExecution\|PayrollExecutionMode" src/main --include="*.java"
```
