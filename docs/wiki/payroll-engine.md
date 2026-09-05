# Motor de Nómina B4RRHH — Wiki Técnica

> **Estado:** PoC funcional. El motor resuelve el ciclo completo de cálculo por segmentos temporales sobre un conjunto fijo de conceptos. La arquitectura está diseñada para generalizarse progresivamente.

---

## Tabla de contenidos

1. [Visión general](#1-visión-general)
2. [Bounded context y estructura de paquetes](#2-bounded-context-y-estructura-de-paquetes)
3. [Metamodelo de objetos de nómina](#3-metamodelo-de-objetos-de-nómina)
4. [Concepto de nómina (PayrollConcept)](#4-concepto-de-nómina-payrollconcept)
5. [Tipos de cálculo](#5-tipos-de-cálculo)
6. [Grafo de dependencias](#6-grafo-de-dependencias)
7. [Segmentación temporal](#7-segmentación-temporal)
8. [Elegibilidad y asignación de conceptos](#8-elegibilidad-y-asignación-de-conceptos)
9. [Pipeline de planificación](#9-pipeline-de-planificación)
10. [Motor de ejecución](#10-motor-de-ejecución)
11. [Ejecutor PoC end-to-end](#11-ejecutor-poc-end-to-end)
12. [Política de redondeo](#12-política-de-redondeo)
13. [Modelo de lanzamiento y claim](#13-modelo-de-lanzamiento-y-claim)
14. [Esquema de base de datos](#14-esquema-de-base-de-datos)
15. [Mapa de invariantes y contratos de fallo](#15-mapa-de-invariantes-y-contratos-de-fallo)
16. [Limitaciones PoC y deuda conocida](#16-limitaciones-poc-y-deuda-conocida)

---

## 1. Visión general

El motor de nómina de B4RRHH es un motor de cálculo basado en conceptos configurables. Su objetivo es calcular el devengo y la retención de un empleado para un período dado, descomponiéndolo en segmentos temporales homogéneos (franjas donde las condiciones laborales son constantes) y ejecutando un plan ordenado de cálculo sobre cada uno.

### Principios de diseño

| Principio | Implementación |
|---|---|
| Sin acceso a repositorio durante la ejecución | El plan se enriquece en build-time; el engine opera 100 % en memoria |
| Orden garantizado por topología | El grafo de dependencias produce el orden de cálculo mediante DFS |
| Detección de ciclos eager | El grafo lanza excepción en `build()`, nunca en ejecución |
| Elegibilidad desacoplada de dependencias | `eligibility` decide qué conceptos aplican; `planning` expande qué conceptos necesita cada uno |
| Fallo rápido | Cada invariante se valida en el constructor; las excepciones son de dominio, no de runtime genérico |

---

## 2. Bounded context y estructura de paquetes

El motor vive en el bounded context `payroll_engine`, separado del contexto `employee` y `rulesystem`. Su estructura interna replica la arquitectura hexagonal del proyecto:

```
com.b4rrhh.payroll_engine
│
├── object/                        ← Metamodelo base
│   └── domain/model/              PayrollObject, PayrollObjectTypeCode
│
├── concept/                       ← Definición semántica de conceptos
│   ├── domain/model/              PayrollConcept, CalculationType, FunctionalNature,
│   │                              ResultCompositionMode, ExecutionScope, FeedMode,
│   │                              PayrollConceptFeedRelation, PayrollConceptOperand, OperandRole
│   ├── domain/port/               PayrollConceptRepository, PayrollConceptFeedRelationRepository,
│   │                              PayrollConceptOperandRepository
│   └── infrastructure/persistence/
│
├── dependency/                    ← Grafo de dependencias entre conceptos
│   ├── domain/model/              ConceptDependencyGraph, ConceptDependencyGraphBuilder,
│   │                              ConceptDependency, ConceptNodeIdentity, DependencyType
│   └── application/service/      ConceptDependencyGraphService (+ Default impl)
│
├── segment/                       ← Segmentación temporal del período
│   ├── domain/model/              CalculationPeriod, CalculationSegment, WorkingTimeWindow,
│   │                              SegmentCalculationContext, WorkingTimeSegmentBuilder
│   └── application/service/      DefaultWorkingTimeSegmentBuilder
│
├── eligibility/                   ← Resolución de qué conceptos aplican a cada empleado
│   ├── domain/model/              ConceptAssignment, EmployeeAssignmentContext,
│   │                              ResolvedConceptAssignment
│   ├── domain/port/               ConceptAssignmentRepository
│   └── application/service/      ResolveApplicableConceptsUseCase,
│                                  DefaultConceptEligibilityResolver
│
├── planning/                      ← Pipeline completo: elegibilidad → expansión → plan
│   ├── domain/model/              EligibleExecutionPlanResult
│   └── application/service/      BuildEligibleExecutionPlanUseCase,
│                                  DefaultEligibleExecutionPlanBuilder,
│                                  EligibleConceptExpansionService,
│                                  DefaultEligibleConceptExpansionService
│
└── execution/                     ← Motor de cálculo por segmento
    ├── domain/model/              ConceptExecutionPlanEntry, SegmentExecutionState,
    │                              SegmentExecutionResult, PayrollEnginePocRequest,
    │                              PayrollEnginePocResult
    └── application/service/       ExecutionPlanBuilder, DefaultExecutionPlanBuilder,
                                   SegmentExecutionEngine, DefaultSegmentExecutionEngine,
                                   SegmentTechnicalValueResolver,
                                   RateByQuantityOperandResolver, PercentageConceptResolver,
                                   OperandConfigurationValidator,
                                   RateByQuantityConfigurationValidator,
                                   PayrollEnginePocExecutor, DefaultPayrollEnginePocExecutor
```

---

## 3. Metamodelo de objetos de nómina

Todo elemento calculable en el motor es un `PayrollObject`. Es la entidad raíz del metamodelo.

### Clase `PayrollObject`

```
PayrollObject
  id                Long              (surrogate, BD-generated)
  ruleSystemCode    String            (código del sistema de reglas)
  objectTypeCode    PayrollObjectTypeCode
  objectCode        String            (código semántico dentro del tipo)
  createdAt / updatedAt
```

**Business key:** `(ruleSystemCode, objectTypeCode, objectCode)` — la igualdad entre dos `PayrollObject` se determina por estos tres campos, nunca por `id`.

### Enum `PayrollObjectTypeCode`

| Valor | Significado |
|---|---|
| `CONCEPT` | Concepto de nómina (devengos, deducciones, técnicos, bases) |
| `TABLE` | Tabla de valores (p.ej. tablas de IRPF) |
| `CONSTANT` | Constante escalar del sistema de reglas |

En el estado actual del PoC sólo `CONCEPT` está en uso en el motor de ejecución. `TABLE` y `CONSTANT` están definidos como extensión futura.

---

## 4. Concepto de nómina (PayrollConcept)

`PayrollConcept` es un subtipo semántico de `PayrollObject`. No duplica la identidad: la delega completamente al objeto base al que envuelve.

```
PayrollConcept
  object               PayrollObject     (debe tener objectTypeCode = CONCEPT)
  conceptMnemonic      String            (etiqueta técnica legible)
  calculationType      CalculationType
  functionalNature     FunctionalNature
  resultCompositionMode ResultCompositionMode
  payslipOrderCode     String (nullable)
  executionScope       ExecutionScope
```

**`conceptCode`** es un alias de `object.objectCode` en el contexto de conceptos.

### `FunctionalNature`

Clasifica el concepto según su rol económico:

| Valor | Rol |
|---|---|
| `EARNING` | Devengo — suma al bruto |
| `DEDUCTION` | Deducción — resta al líquido |
| `BASE` | Base de cálculo — alimenta a otros conceptos |
| `INFORMATIONAL` | Informativo — aparece en nómina pero no afecta al neto |

### `ExecutionScope`

| Valor | Significado |
|---|---|
| `SEGMENT` | Se evalúa en cada segmento temporal; su valor de período es la **suma** de los segmentos |
| `PERIOD` | La regla está definida sobre el período entero y no se reparte en subperíodos: se evalúa una vez, con los feeds `SEGMENT` ya compuestos |

`PERIOD` no es «evalúalo una vez porque es más barato»: un tope o un suelo aplicados por quincenas dan
otro número que aplicados al mes. Ningún operando (`qty`, `rate`, `base`, `pct`, `left`, `right`) cruza
de un concepto `SEGMENT` a uno `PERIOD`; los feeds sí, porque sumar está definido (ADR-058).

### `ResultCompositionMode`

| Valor | Comportamiento al combinar resultados multi-segmento |
|---|---|
| `REPLACE` | El resultado del último segmento reemplaza al anterior |
| `ACCUMULATE` | Los resultados de todos los segmentos se suman |

> Inerte desde backend#64: la composición es siempre suma por regla (ADR-058). Se retira en backend#60.

### `PayrollConceptFeedRelation`

Representa una relación de alimentación entre dos conceptos: un `sourceObject` alimenta datos a un `targetObject`. En el grafo de dependencias, esta relación se traduce en que el `target` depende del `source` (el source debe calcularse antes).

```
PayrollConceptFeedRelation
  sourceObject   PayrollObject   (CONCEPT)
  targetObject   PayrollObject   (CONCEPT)
  feedMode       FeedMode        (actualmente solo FEED_BY_SOURCE)
  feedValue      BigDecimal (nullable)
  effectiveFrom  LocalDate       (inclusivo)
  effectiveTo    LocalDate (nullable, inclusivo si presente)
```

La relación tiene **vigencia temporal**: `isActiveAt(referenceDate)` determina si aplica en una fecha dada.

### `PayrollConceptOperand`

Declara que un concepto `RATE_BY_QUANTITY` o `PERCENTAGE` usa otro concepto como operando con un rol específico.

```
PayrollConceptOperand
  targetObject   PayrollObject   (el concepto calculado)
  operandRole    OperandRole
  sourceObject   PayrollObject   (el concepto que actúa como operando)
```

`OperandRole` define qué papel juega el operando:

| Rol | Tipo de cálculo que lo usa |
|---|---|
| `QUANTITY` | `RATE_BY_QUANTITY` |
| `RATE` | `RATE_BY_QUANTITY` |
| `BASE` | `PERCENTAGE` |
| `PERCENTAGE` | `PERCENTAGE` |

---

## 5. Tipos de cálculo

`CalculationType` determina cómo se calcula el importe de un concepto en cada segmento.

### `DIRECT_AMOUNT`

El valor lo resuelve directamente `SegmentTechnicalValueResolver` a partir del contexto del segmento. No tiene operandos persistidos. Son los conceptos "técnicos" de entrada del motor.

Conceptos técnicos del PoC y sus fórmulas:

| Código | Valor computado |
|---|---|
| `T_DIAS_PRESENCIA_SEGMENTO` | `daysInSegment` (días inclusivos del segmento) |
| `T_SALARIO_MENSUAL` | `monthlySalaryAmount` del contexto |
| `T_FACTOR_JORNADA` | `workingTimePercentage / 100` |
| `T_PRECIO_DIA` | `monthlySalary / daysInPeriod × (workingTimePct / 100)` |
| `T_PRECIO_TRANSPORTE` | `7.50` (valor fijo PoC) |
| `T_PCT_IRPF` | `15` (porcentaje fijo PoC) |

### `RATE_BY_QUANTITY`

```
resultado = QUANTITY × RATE
```

Los operandos se resuelven en build-time y se embeben en el `ConceptExecutionPlanEntry`. Durante la ejecución no hay acceso al repositorio. Ejemplo: `SALARIO_BASE = T_DIAS_PRESENCIA_SEGMENTO × T_PRECIO_DIA`.

### `PERCENTAGE`

```
resultado = BASE × PERCENTAGE / 100
```

Idem: operandos resueltos en build-time. Ejemplo: `RETENCION_IRPF_TRAMO = TOTAL_DEVENGOS_SEGMENTO × T_PCT_IRPF / 100`.

### `AGGREGATE`

```
resultado = ∑ (importe de cada concepto fuente declarado en el grafo)
```

Las fuentes provienen del grafo de dependencias (no de la tabla de operandos). Ejemplo: `TOTAL_DEVENGOS_SEGMENTO = SALARIO_BASE + PLUS_TRANSPORTE`.

---

## 6. Grafo de dependencias

El grafo es el corazón del orden de ejecución. Garantiza que cada concepto se calcula después de todos sus dependientes.

### Dirección de aristas

Una arista **A → B** significa: _"A depende de B"_, por tanto B debe calcularse antes que A. En el orden topológico, B aparece a un índice menor que A.

### Tipos de dependencia (`DependencyType`)

| Tipo | Origen |
|---|---|
| `OPERAND_DEPENDENCY` | Declarado explícitamente vía `addOperandDependency()` |
| `FEED_DEPENDENCY` | Derivado de `PayrollConceptFeedRelation` (source alimenta a target → target depende de source) |

### Construcción con `ConceptDependencyGraphBuilder`

```java
ConceptDependencyGraph graph = new ConceptDependencyGraphBuilder()
    .addNodes(concepts)
    .addFeedRelation(relation)    // FEED_DEPENDENCY
    .addOperandDependency(a, b)   // OPERAND_DEPENDENCY
    .build();                     // lanza ConceptDependencyCycleException si hay ciclo
```

La detección de ciclos es **eager**: se ejecuta en `build()` mediante DFS (algoritmo de marcas TEMPORARY/PERMANENT). Si hay ciclo, se reconstruye el camino completo y se incluye en la excepción.

### `ConceptNodeIdentity`

Identidad ligera de un nodo del grafo: `(ruleSystemCode, conceptCode)`. Inmutable, con `equals`/`hashCode` por valor.

### Cross-rule-system

Las relaciones feed entre distintos sistemas de reglas están rechazadas explícitamente tanto en `ConceptDependencyGraphBuilder.addFeedRelation()` como en `DefaultEligibleConceptExpansionService`.

---

## 7. Segmentación temporal

El motor no asume que las condiciones del empleado son homogéneas durante todo el período. Si, por ejemplo, un empleado cambia de jornada a mitad de mes, habrá dos segmentos, cada uno con su propia `workingTimePercentage`.

### `CalculationPeriod`

Define los límites externos del cálculo (típicamente un mes natural):

```
CalculationPeriod
  periodStart   LocalDate (inclusivo)
  periodEnd     LocalDate (inclusivo)
```

### `WorkingTimeWindow`

Cada ventana declara una franja temporal con una jornada fija:

```
WorkingTimeWindow
  startDate               LocalDate
  endDate                 LocalDate (nullable = abierto)
  workingTimePercentage   BigDecimal  (p.ej. 100.00 para jornada completa)
```

### `CalculationSegment`

Resultado de recortar las ventanas al período:

```
CalculationSegment
  segmentStart    LocalDate (inclusivo)
  segmentEnd      LocalDate (inclusivo)
  firstSegment    boolean
  lastSegment     boolean
```

`lengthInDaysInclusive()` = `ChronoUnit.DAYS.between(start, end) + 1`.

### Algoritmo de construcción (`DefaultWorkingTimeSegmentBuilder`)

1. Ordenar ventanas por `startDate`.
2. Recortar cada ventana al período (intersection): `effectiveStart = max(window.start, period.start)`, `effectiveEnd = min(window.end ?? period.end, period.end)`.
3. Descartar ventanas completamente fuera del período.
4. Validar cobertura **completa, contigua y no solapada**:
   - Al menos una ventana efectiva.
   - La primera comienza exactamente en `periodStart`.
   - Cada ventana siguiente comienza el día después de que termina la anterior.
   - La última termina exactamente en `periodEnd`.
5. Producir un `CalculationSegment` por ventana efectiva.

Huecos y solapamientos se rechazan con `InvalidWorkingTimeCoverageException`. El builder **no rellena huecos automáticamente**.

### `SegmentCalculationContext`

Objeto de valor inmutable que encapsula todo lo necesario para calcular un segmento:

```
SegmentCalculationContext
  ruleSystemCode, employeeTypeCode, employeeNumber
  periodStart, periodEnd          ← límites del período completo
  segmentStart, segmentEnd        ← límites de este segmento
  firstSegment, lastSegment
  daysInPeriod                    ← días totales del período (pre-calculado)
  daysInSegment                   ← días de este segmento (pre-calculado)
  workingTimePercentage           ← jornada del segmento
  monthlySalaryAmount             ← salario mensual base
```

La invariante exige que `[segmentStart, segmentEnd] ⊆ [periodStart, periodEnd]`.

---

## 8. Elegibilidad y asignación de conceptos

La elegibilidad responde a la pregunta: _¿qué conceptos de nómina deben calcularse para este empleado en este contexto?_

### `ConceptAssignment`

Regla de elegibilidad persistida. Declara que un concepto (`conceptCode`) es aplicable dentro de un sistema de reglas, opcionalmente acotado por empresa, convenio colectivo y tipo de empleado:

```
ConceptAssignment
  ruleSystemCode      obligatorio
  conceptCode         obligatorio
  companyCode         optional (null = wildcard en asignación)
  agreementCode       optional (null = wildcard en asignación)
  employeeTypeCode    optional (null = wildcard en asignación)
  validFrom           LocalDate (inclusivo)
  validTo             LocalDate (nullable)
  priority            int
```

**Semántica de wildcards:** un campo `null` en la asignación actúa como comodín: aplica a cualquier valor en esa dimensión. Un campo no nulo requiere concordancia exacta.

**Vigencia:** `isValidOn(referenceDate)` comprueba que `validFrom <= date <= validTo` (o validTo nulo).

### `EmployeeAssignmentContext`

Portador de las dimensiones del empleado para la resolución:

```
EmployeeAssignmentContext
  ruleSystemCode     (obligatorio)
  companyCode        (null = desconocido)
  agreementCode      (null = desconocido)
  employeeTypeCode   (null = desconocido)
```

**Asimetría de null:** en el lado del contexto, `null` significa "desconocido". Una asignación con dimensión específica no casará con un contexto que no conoce esa dimensión. Solo casarán las asignaciones cuya dimensión es también `null` (wildcard).

### Algoritmo de resolución (`DefaultConceptEligibilityResolver`)

1. Cargar todos los candidatos del repositorio para el contexto y fecha de referencia (el repositorio aplica matching wildcard y filtro de vigencia en SQL).
2. Agrupar por `conceptCode`.
3. Por cada grupo, encontrar la prioridad máxima. Si más de una asignación comparte esa prioridad máxima → `DuplicateConceptAssignmentException`.
4. El ganador de cada grupo se encapsula en `ResolvedConceptAssignment`.
5. Ordenar: prioridad descendente, luego `conceptCode` ascendente.

### `ResolvedConceptAssignment`

Resultado limpio: un ganador por `conceptCode` con sus dimensiones de contexto:

```
ResolvedConceptAssignment
  conceptCode
  winningPriority
  ruleSystemCode
  companyCode, agreementCode, employeeTypeCode   (los del ganador)
```

---

## 9. Pipeline de planificación

El vertical `planning` orquesta el ciclo completo desde elegibilidad hasta plan de ejecución listo para el motor.

### `EligibleExecutionPlanResult`

El resultado es un record auditable que expone **todos los estadios intermedios**:

```
EligibleExecutionPlanResult
  applicableAssignments   List<ResolvedConceptAssignment>  ← resultado de elegibilidad
  eligibleConcepts        List<PayrollConcept>             ← conceptos directamente elegibles
  expandedConcepts        List<PayrollConcept>             ← elegibles + dependencias transitivas
  dependencyGraph         ConceptDependencyGraph
  executionPlan           List<ConceptExecutionPlanEntry>  ← plan en orden topológico
```

### `BuildEligibleExecutionPlanUseCase` — algoritmo en 5 pasos

```
Paso 1: Resolver asignaciones aplicables
        ResolveApplicableConceptsUseCase.resolve(context, referenceDate)

Paso 2: Cargar definiciones de conceptos elegibles
        PayrollConceptRepository.findAllByCodes(ruleSystemCode, codes)
        → falla rápido si algún código no tiene definición

Paso 3: Expandir dependencias transitivas
        EligibleConceptExpansionService.expand(eligibleConcepts, referenceDate)
        → BFS sobre feed relations activas en la fecha de referencia
        → solo dentro del mismo ruleSystem
        → técnicos (T_PRECIO_DIA, etc.) se incorporan aquí, no por elegibilidad

Paso 4: Construir grafo de dependencias
        ConceptDependencyGraphService.build(expandedConcepts, referenceDate)

Paso 5: Construir plan de ejecución
        ExecutionPlanBuilder.build(graph, expandedConcepts)
```

### `EligibleConceptExpansionService` — BFS de dependencias

Recorre los conceptos elegibles como semilla e itera:
- Para cada concepto, carga sus `PayrollConceptFeedRelation` activas (por `targetObjectId`).
- Si el `sourceObject` pertenece al mismo sistema de reglas y aún no está cargado, lo carga y lo encola.
- Continúa hasta que la cola esté vacía.

Esto garantiza que conceptos técnicos no asignables directamente (como `T_DIAS_PRESENCIA_SEGMENTO`) queden incluidos si son dependencia de cualquier concepto elegible.

---

## 10. Motor de ejecución

### `ExecutionPlanBuilder` — construcción del plan

Convierte `(ConceptDependencyGraph, List<PayrollConcept>)` en una lista ordenada de `ConceptExecutionPlanEntry`.

**Algoritmo:**
1. Indexar conceptos por `ConceptNodeIdentity`. Duplicados → `DuplicateConceptIdentityException`.
2. Obtener orden topológico del grafo.
3. Por cada nodo en orden:
   - Buscar el concepto en el índice. Si no existe → `MissingConceptDefinitionException`.
   - Según el `CalculationType`:
     - `DIRECT_AMOUNT`: entrada simple, sin operandos.
     - `RATE_BY_QUANTITY`: cargar operandos del repositorio, validar coherencia con el grafo, resolver QUANTITY y RATE como `ConceptNodeIdentity`, embeber en la entrada.
     - `PERCENTAGE`: idem con BASE y PERCENTAGE.
     - `AGGREGATE`: obtener dependencias del grafo, verificar que no estén vacías, embeber como lista de fuentes.

### `OperandConfigurationValidator`

Valida la coherencia entre el grafo y la configuración de operandos antes de embeber el cableado:
- Cada operando declarado debe ser una dependencia del grafo para ese concepto (`OperandGraphMismatchException`).
- No puede haber operandos duplicados por rol (`DuplicateOperandDefinitionException`).
- No puede faltar ningún operando requerido (`MissingOperandDefinitionException`).

### `ConceptExecutionPlanEntry`

Record que une identidad, tipo de cálculo y cableado pre-resuelto:

```
ConceptExecutionPlanEntry
  identity           ConceptNodeIdentity
  calculationType    CalculationType
  operands           Map<OperandRole, ConceptNodeIdentity>  ← para RATE_BY_QUANTITY / PERCENTAGE
  aggregateSources   List<ConceptNodeIdentity>              ← para AGGREGATE
```

### `SegmentExecutionState`

Acumulador por segmento. Mapa `ConceptNodeIdentity → BigDecimal` ordenado por inserción:
- `storeResult(concept, amount)` — falla si se intenta almacenar el mismo concepto dos veces.
- `getRequiredAmount(concept)` — falla rápido con `MissingConceptResultException` si el concepto aún no se ha calculado (indicativo de orden no topológico).

### `DefaultSegmentExecutionEngine` — dispatcher por tipo

```java
for (ConceptExecutionPlanEntry entry : plan) {
    BigDecimal amount = switch (entry.calculationType()) {
        case DIRECT_AMOUNT   → technicalValueResolver.resolve(entry.identity().getConceptCode(), context)
        case RATE_BY_QUANTITY → rateByQuantityResolver.resolve(entry, state)
        case PERCENTAGE       → percentageConceptResolver.resolve(entry, state)
        case AGGREGATE        → sum(entry.aggregateSources(), state)  // redondeo final scale=2 HALF_UP
    };
    state.storeResult(entry.identity(), amount);
}
```

No hay acceso al repositorio durante la ejecución. Todo el cableado está pre-resuelto en el plan.

---

## 11. Ejecutor PoC end-to-end

`DefaultPayrollEnginePocExecutor` orquesta el flujo completo para un escenario PoC con 8 conceptos fijos.

### Conceptos del PoC

| Código | Tipo | Naturaleza | Fórmula |
|---|---|---|---|
| `T_DIAS_PRESENCIA_SEGMENTO` | `DIRECT_AMOUNT` | — | Días del segmento |
| `T_PRECIO_DIA` | `DIRECT_AMOUNT` | — | Salario mensual / días período × factor jornada |
| `T_PRECIO_TRANSPORTE` | `DIRECT_AMOUNT` | — | 7.50 (PoC fijo) |
| `T_PCT_IRPF` | `DIRECT_AMOUNT` | — | 15% (PoC fijo) |
| `SALARIO_BASE` | `RATE_BY_QUANTITY` | `EARNING` | `T_DIAS_PRESENCIA_SEGMENTO × T_PRECIO_DIA` |
| `PLUS_TRANSPORTE` | `RATE_BY_QUANTITY` | `EARNING` | `T_DIAS_PRESENCIA_SEGMENTO × T_PRECIO_TRANSPORTE` |
| `TOTAL_DEVENGOS_SEGMENTO` | `AGGREGATE` | `BASE` | `SALARIO_BASE + PLUS_TRANSPORTE` |
| `RETENCION_IRPF_TRAMO` | `PERCENTAGE` | `DEDUCTION` | `TOTAL_DEVENGOS_SEGMENTO × T_PCT_IRPF / 100` |

### Flujo de ejecución

```
1. Cargar los 8 conceptos del repositorio por business key
   └── MissingPocConceptException si alguno falta

2. Construir el grafo de dependencias
   ConceptDependencyGraphService.build(pocConcepts, periodStart)

3. Construir el plan de ejecución
   ExecutionPlanBuilder.build(graph, pocConcepts)
   └── Operand wiring y aggregate sources se resuelven aquí

4. Construir segmentos temporales
   WorkingTimeSegmentBuilder.build(period, workingTimeWindows)

5. Por cada segmento:
   a. Resolver workingTimePercentage del segmento
   b. Construir SegmentCalculationContext
   c. segmentExecutionEngine.execute(plan, context)  ← 100% in-memory
   d. Extraer importes de SegmentExecutionState por conceptCode
   e. Crear SegmentExecutionResult

6. Consolidar totales del período
   (suma de segmentos, scale=2 HALF_UP)

7. Devolver PayrollEnginePocResult
```

### `PayrollEnginePocResult`

```
PayrollEnginePocResult
  segmentResults              List<SegmentExecutionResult>
  totalSalarioBase            BigDecimal
  totalPlusTransporte         BigDecimal
  totalDevengosConsolidated   BigDecimal
  totalRetencionIrpf          BigDecimal
```

### `SegmentExecutionResult`

Captura por segmento:

```
SegmentExecutionResult
  segmentStart / segmentEnd
  firstSegment / lastSegment
  daysInPeriod / daysInSegment
  workingTimePercentage
  dailyRate
  salarioBaseAmount
  plusTransporteAmount
  totalDevengosSegmentoAmount
  retencionIrpfTramoAmount
```

---

## 12. Política de redondeo

Todas las operaciones de redondeo son **HALF_UP** y se aplican en capas:

| Capa | Escala | Componente |
|---|---|---|
| Intermedias (divisiones, multiplicaciones parciales) | 8 | `SegmentTechnicalValueResolver` |
| `RATE_BY_QUANTITY` resultado final | 2 | `RateByQuantityOperandResolver` |
| `PERCENTAGE` división intermedia (/100) | 8 | `PercentageConceptResolver` |
| `PERCENTAGE` resultado final | 2 | `PercentageConceptResolver` |
| `AGGREGATE` resultado final | 2 | `DefaultSegmentExecutionEngine` |
| Totales de período | 2 | `DefaultPayrollEnginePocExecutor` |

> **Regla general:** los conceptos técnicos (`DIRECT_AMOUNT`) se mantienen a escala 8 para no acumular error en las multiplicaciones posteriores. El redondeo a escala 2 se aplica sólo en el resultado final de cada tipo de cálculo.

---

## 13. Modelo de lanzamiento y claim

Independientemente del motor de cálculo, el sistema tiene un modelo de lanzamiento y trazabilidad en el schema `payroll`:

### `calculation_run`

Representa una ejecución de nómina completa para un período y tipo determinados:

```sql
calculation_run (
  rule_system_code, payroll_period_code, payroll_type_code,
  calculation_engine_code, calculation_engine_version,
  status,                        -- estado del lanzamiento
  target_selection_json,         -- JSON con los empleados objetivo
  total_candidates / eligible / claimed / calculated / errors / ...,
  requested_at, started_at, finished_at
)
```

### `calculation_claim`

Claim atómico por empleado+período+tipo+número de presencia. La constraint única previene doble cálculo:

```sql
UNIQUE (rule_system_code, employee_type_code, employee_number,
        payroll_period_code, payroll_type_code, presence_number)
```

### Mensajería de ejecución

`calculation_run_message` — mensajes de traza, avisos y errores a nivel de run con referencia opcional al empleado afectado.

`payroll_warning` — avisos a nivel de nómina individual.

---

## 14. Esquema de base de datos

El motor de nómina usa el schema `payroll_engine`. Las tablas del modelo de lanzamiento están en el schema `payroll`.

### Schema `payroll_engine`

```
payroll_engine.payroll_object
  id                  BIGINT IDENTITY PK
  rule_system_code    VARCHAR(10)
  object_type_code    VARCHAR(30)
  object_code         VARCHAR(50)
  UNIQUE (rule_system_code, object_type_code, object_code)

payroll_engine.payroll_concept
  object_id               BIGINT PK FK → payroll_object.id
  concept_mnemonic        VARCHAR(50)
  calculation_type        VARCHAR(30)
  functional_nature       VARCHAR(30)
  result_composition_mode VARCHAR(30)
  payslip_order_code      VARCHAR(30) nullable
  execution_scope         VARCHAR(30)

payroll_engine.payroll_concept_feed_relation
  id               BIGINT IDENTITY PK
  source_object_id BIGINT FK → payroll_object.id
  target_object_id BIGINT FK → payroll_object.id
  feed_mode        VARCHAR(30)
  feed_value       NUMERIC(19,6) nullable
  effective_from   DATE
  effective_to     DATE nullable

payroll_engine.payroll_concept_operand
  id               BIGINT IDENTITY PK
  target_object_id BIGINT FK → payroll_object.id
  operand_role     VARCHAR(30)
  source_object_id BIGINT FK → payroll_object.id
  UNIQUE (target_object_id, operand_role)
```

> **Nota de diseño:** `payroll_concept` tiene `object_id` como PK (tabla "joined"), no un id propio. Un concepto sin objeto base no puede existir. La herencia se modela en BD mediante la relación 1-1 obligatoria.

### Schema `payroll_engine` — elegibilidad

```
payroll_engine.concept_assignment
  id                  BIGINT IDENTITY PK
  rule_system_code    VARCHAR
  concept_code        VARCHAR
  company_code        VARCHAR nullable
  agreement_code      VARCHAR nullable
  employee_type_code  VARCHAR nullable
  valid_from          DATE
  valid_to            DATE nullable
  priority            INTEGER
```

---

## 15. Mapa de invariantes y contratos de fallo

| Situación | Excepción |
|---|---|
| Ciclo en el grafo de dependencias | `ConceptDependencyCycleException` |
| Relación feed entre distintos rule systems | `IllegalArgumentException` (en build) |
| Concepto duplicado en el índice del plan | `DuplicateConceptIdentityException` |
| Concepto en el grafo sin definición | `MissingConceptDefinitionException` |
| Concepto AGGREGATE sin fuentes en el grafo | `MissingAggregateSourcesException` |
| Fuentes AGGREGATE duplicadas | `DuplicateAggregateSourceException` |
| Operando del grafo no coincide con operand config | `OperandGraphMismatchException` |
| Rol de operando ausente en tabla | `MissingOperandDefinitionException` |
| Rol de operando duplicado | `DuplicateOperandDefinitionException` |
| Cableado de operando ausente en plan entry | `MissingPlannedOperandException` |
| Resultado de concepto requerido no calculado aún | `MissingConceptResultException` |
| Tipo de cálculo no soportado por el engine | `UnsupportedCalculationTypeException` |
| Concepto técnico no reconocido | `UnsupportedTechnicalConceptException` |
| Concepto PoC ausente del repositorio | `MissingPocConceptException` |
| Cobertura temporal de jornada incompleta | `InvalidWorkingTimeCoverageException` |
| Dos asignaciones con la misma prioridad máxima | `DuplicateConceptAssignmentException` |
| Concepto elegible sin definición en repo | `MissingEligibleConceptDefinitionException` |
| Dependencia transitiva sin definición en repo | `MissingDependencyConceptDefinitionException` |

---

## 16. Limitaciones PoC y deuda conocida

| Limitación | Descripción | Dirección futura |
|---|---|---|
| Conceptos PoC hardcodeados | `DefaultPayrollEnginePocExecutor` carga 8 conceptos por código fijo | Parametrizar desde el plan de ejecución elegible |
| Extracción de resultados no genérica | Los importes se extraen por `conceptCode` literal al final del executor | Extraer todos los resultados del `SegmentExecutionState` de forma genérica |
| `SegmentTechnicalValueResolver` hardcodeado | `T_PRECIO_TRANSPORTE` y `T_PCT_IRPF` son constantes PoC | Resolver desde tablas o convenio colectivo |
| `FeedMode` solo `FEED_BY_SOURCE` | El enum existe pero solo hay un valor implementado | Preparado para extensión |
| `PayrollObjectTypeCode` TABLE/CONSTANT sin usar en ejecución | El metamodelo los prevé pero el motor no los consume aún | Integrar tablas de valores como operandos |
| Integración eligibility → executor | El `DefaultPayrollEnginePocExecutor` bypasea el pipeline de planificación | Conectar `BuildEligibleExecutionPlanUseCase` al executor genérico |
| Sin persistencia de resultados | Los resultados de la ejecución se devuelven en memoria | Persistir en tablas de resultado de nómina |
| `PERIOD` scope no implementado | `ExecutionScope.PERIOD` está definido pero el executor sólo ejecuta por segmento | Implementar paso de consolidación de período |
