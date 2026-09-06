# B4RRHH – Strong Timeline Replace Pattern

## 1. Context

En múltiples verticales del dominio employee (por ejemplo:

* labor_classification
* contract)

existe una operación común:

**replaceFromDate(effectiveDate)**

Esta operación:

* sustituye el valor activo a partir de una fecha
* respeta la continuidad temporal
* puede implicar división de periodos existentes

Estas verticales pertenecen al tipo:

**STRONG_TIMELINE**

---

## 2. Problema

La lógica de replaceFromDate incluye una parte repetida en varias verticales:

* detección de tramo que cubre la fecha
* distinción entre:

  * exact match (startDate == effectiveDate)
  * split (fecha dentro del tramo)
  * no covering period
* cálculo de nuevos límites temporales

Antes, esta lógica estaba duplicada en cada vertical.

---

## 3. Decisión

Se introduce un helper técnico reutilizable:

### StrongTimelineReplacePlanner

Este componente:

* recibe una lista ordenada de DateRange
* recibe una effectiveDate
* devuelve un plan de reemplazo (StrongTimelineReplacePlan)

---

## 4. Modelo

### ReplaceMode

* NO_COVERING
* EXACT_START
* SPLIT

### StrongTimelineReplacePlan

Describe el resultado del análisis temporal:

* tipo de operación
* tramo afectado
* posibles nuevos rangos temporales

---

## 5. Responsabilidades

### Planner (helper técnico)

Responsable de:

* analizar geometría temporal
* decidir tipo de operación
* calcular fechas derivadas

NO es responsable de:

* validaciones de negocio
* catálogo
* relaciones
* persistencia
* excepciones de dominio

---

### Servicios de vertical

Siguen siendo responsables de:

* construir agregados
* validar reglas de negocio
* validar no solape
* validar coverage completo
* persistir cambios

---

## 6. Patrón de uso

Para cualquier vertical STRONG_TIMELINE:

1. Cargar histórico ordenado
2. Convertir a DateRange
3. Invocar planner:
   → StrongTimelineReplacePlan
4. Aplicar lógica de dominio según el plan:

   * EXACT_START → update
   * SPLIT → close + create
   * NO_COVERING → decidir comportamiento
5. Construir projected history
6. Validar timeline
7. Persistir

---

## 7. Cuándo usar este patrón

Aplicar StrongTimelineReplacePlanner SOLO cuando:

* la vertical es STRONG_TIMELINE
* existe operación replaceFromDate
* hay garantía de:

  * no solape
  * un único activo por fecha

Ejemplos:

* contract ✅
* labor_classification ✅

No aplicar directamente a:

* cost_center (DISTRIBUTED_TIMELINE)
* verticales sin cobertura completa

---

## 8. Beneficios

* elimina duplicación de lógica temporal crítica
* mejora legibilidad de servicios
* introduce lenguaje común
* reduce errores en operaciones de split

---

## 9. Regla de evolución

Este helper:

* puede evolucionar si aparece en ≥ 3 verticales
* NO debe convertirse en:

  * engine genérico
  * framework configurable
  * capa de negocio

---

## 10. Decisión futura

Si nuevas verticales STRONG_TIMELINE aparecen:

→ deben reutilizar este planner

Si aparecen variaciones significativas:

→ evaluar extensión del planner, no duplicación

---

## 11. Estado

**Sustituido por el ADR-057.** El `replaceFromDate` deja de ser el modelo de escritura de
una serie temporal: se escribe añadiendo una ocurrencia y el componente temporal
(`TimelinePlanner`) juzga el resultado contra los invariantes. Con `contract`,
`labor_classification` y `workcenter` reconducidas al componente, el planner de este ADR
(`StrongTimelineReplacePlanner`, `StrongTimelineReplacePlan` y `ReplaceMode`) se quedó sin
consumidores y se retiró del árbol en el `backend#57`. Lo que aquí era `SPLIT` es hoy la única
consecuencia automática de un alta; lo que era `EXACT_START` es un plan rechazado que nombra
la ocurrencia a corregir. Se conserva como registro de por qué existió.
