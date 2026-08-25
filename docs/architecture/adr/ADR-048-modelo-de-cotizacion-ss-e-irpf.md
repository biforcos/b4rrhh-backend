# ADR-048 — Modelo de cotización de Seguridad Social e IRPF

## Estado

Aceptado. **Documenta una implementación ya existente**, no propone una nueva.

El grafo de cotización se construyó entre V77 y V91 sin ADR que lo respaldara. Este
documento reconstruye la decisión tal como está en el código a 25 de agosto de 2026 y separa
explícitamente lo que es decisión firme de lo que es provisional. Escribirlo tarde tiene un
coste: partes de este ADR describen elecciones que se tomaron sobre la marcha y que quizá no
se habrían tomado igual con la discusión delante. Se señalan como tales.

---

## Contexto

Los ADR-033 a ADR-046 fijan el metamodelo del motor: `PayrollObject` como raíz,
`PayrollConcept` con tipología de cálculo, operandos resueltos por source, agregación por
feed, grafo acíclico, plan topológico y segmentación temporal.

Sobre esa base había que calcular lo que convierte un devengo en una nómina de verdad: la
cotización a la Seguridad Social y la retención de IRPF. Eso obligó a resolver cuatro cosas
que el metamodelo no cubría:

1. **Topes.** La base de cotización no es el devengo: es el devengo recortado entre un tope
   máximo y un tope mínimo que dependen del grupo de cotización del empleado.
2. **Coste de empresa.** Hay conceptos que se calculan y se muestran pero no descuentan del
   líquido.
3. **Valores que no salen de otro concepto.** Un tipo de cotización o un tope no se calcula:
   se consulta.
4. **Prorrateo de los topes cuando el mes está partido en segmentos.**

---

## Decisión

**La cotización se modela dentro del mismo grafo de conceptos que el resto de la nómina.**
No hay motor paralelo, ni servicio de cotización, ni lógica de negocio fuera del grafo. Un
tope es un nodo; un tipo es un nodo; la base de cotización es un concepto con su código.

De ahí se derivan las cuatro decisiones concretas de este ADR.

### 1. Los topes se aplican con dos tipologías nuevas: `LEAST` y `GREATEST`

ADR-036 declaró cuatro tipologías canónicas y avisó de que no se crearían más por cada caso
de negocio. Aquí se añaden dos, y la justificación es la que el propio ADR-036 exige: no
describen un caso de negocio, describen **operadores** que faltaban.

```
B_CC_MAX = LEAST(B01, P_TOPE_MAX)          ← el devengo, recortado por arriba
B_CC     = GREATEST(B_CC_MAX, P_TOPE_MIN)  ← y después levantado por abajo
```

Cada una toma dos operandos con roles nuevos, `LEFT` y `RIGHT`, añadidos a `OperandRole`.

**El orden importa y es deliberado**: primero el techo, después el suelo. La consecuencia
funcional es que un empleado cuya base queda por debajo del mínimo de su grupo cotiza por el
mínimo aunque haya devengado menos.

### 2. La base de cotización es un concepto, no un campo calculado

```
B01   BASE_COTIZABLE   AGGREGATE   alimentado hoy únicamente por 101
B_CC  BASE_COTIZACION_COTIZ        la base ya recortada, que usan todos los tipos
```

Todos los conceptos de cotización —de trabajador y de empresa— cuelgan de `B_CC`, nunca de
`B01`. V88 reescribió el operando `BASE` del concepto 700 justamente para eso.

Que la base sea un nodo del grafo y no un valor interno es lo que permite explicar una
nómina: se puede preguntar cuánto valía la base y por qué.

### 3. El coste de empresa se calcula, se muestra y no descuenta

Los conceptos 720–724 tienen `functionalNature = INFORMATIONAL` y **no tienen relación de
feed hacia 980**. Se calculan sobre la misma `B_CC`, aparecen en el recibo con su
`payslipOrderCode`, y no tocan el líquido.

Es la aplicación directa de la regla de ADR-036: la naturaleza funcional es presentación y
semántica; quien decide si un importe resta es la relación de feed, no el tipo de cálculo.

### 4. Los valores que se consultan entran como nodos `ENGINE_PROVIDED`

`ENGINE_PROVIDED` (renombrado desde `JAVA_PROVIDED` en V89) identifica conceptos cuyo valor
lo produce una clase Java que implementa `TechnicalConceptCalculator`, registrada por Spring
en `TechnicalConceptCalculatorRegistry` bajo su código de concepto.

Se mantiene la regla de ADR-046: **estas clases no pueden calcular conceptos económicos.**
Solo resuelven valores que se consultan o se derivan del contexto de ejecución — tipos,
topes, días, coeficientes. La frontera es la que separa «consultar un dato» de «calcular una
nómina».

---

## El grafo implementado

```
                      101 SALARIO_BASE
                        │        │
                        │        └──────────────► 970 TOTAL_DEVENGOS ──┐
                        ▼                                              │
                  B01 BASE_COTIZABLE                                   │
                        │                                              │
        P_TOPE_MAX ──►  LEAST  ──► B_CC_MAX                            │
                                      │                                │
        P_TOPE_MIN ──► GREATEST ──►  B_CC                              │
                                      │                                ▼
      ┌───────────────────────────────┼────────────────────┐      990 LIQUIDO
      │            trabajador         │      empresa       │           ▲
      ▼                               ▼                    ▼           │
  700 CC 4,70 %                720 CC 23,60 %                          │
  703 DESEMPLEO 1,55 %         721 DESEMPLEO 7,05 %                    │
  701 FP 0,10 %                722 FP 0,60 %          (INFORMATIONAL,  │
  702 MEI 0,11 %               723 FOGASA 0,20 %       sin feed a 980) │
  800 IRPF 15,00 %             724 MEI 0,58 %                          │
      │                                                                │
      └──────────────► 980 TOTAL_DEDUCCIONES ──────────(invert_sign)───┘
```

### Conceptos

| Código | Mnemónico | Tipo | Naturaleza | Fórmula | Recibo |
|---|---|---|---|---|---|
| `B01` | BASE_COTIZABLE | AGGREGATE | BASE | ← 101 | no |
| `B_CC_MAX` | BASE_COTIZACION_MAX | LEAST | BASE | min(B01, P_TOPE_MAX) | no |
| `B_CC` | BASE_COTIZACION_COTIZ | GREATEST | BASE | max(B_CC_MAX, P_TOPE_MIN) | no |
| `700` | CC_TRABAJADOR | PERCENTAGE | DEDUCTION | B_CC × 4,70 % | 700 |
| `703` | DESEMPLEO_TRABAJADOR | PERCENTAGE | DEDUCTION | B_CC × 1,55 % | 703 |
| `701` | FP_TRABAJADOR | PERCENTAGE | DEDUCTION | B_CC × 0,10 % | 701 |
| `702` | MEI_TRABAJADOR | PERCENTAGE | DEDUCTION | B_CC × 0,11 % | 702 |
| `800` | RETENCION_IRPF | PERCENTAGE | DEDUCTION | B01 × 15,00 % | 800 |
| `720` | SS_CC_EMPRESARIO | PERCENTAGE | INFORMATIONAL | B_CC × 23,60 % | 720 |
| `721` | SS_DESEMPLEO_EMPRESARIO | PERCENTAGE | INFORMATIONAL | B_CC × 7,05 % | 721 |
| `722` | SS_FP_EMPRESARIO | PERCENTAGE | INFORMATIONAL | B_CC × 0,60 % | 722 |
| `723` | SS_FOGASA_EMPRESARIO | PERCENTAGE | INFORMATIONAL | B_CC × 0,20 % | 723 |
| `724` | SS_MEI_EMPRESARIO | PERCENTAGE | INFORMATIONAL | B_CC × 0,58 % | 724 |

Los nodos `P_*` (`P_TOPE_MAX`, `P_TOPE_MIN`, `P_SS_CC`, `P_SS_DESEMPLEO`, `P_FP_TRAB`,
`P_MEI_TRAB`, `P_IRPF`, `P_SS_CC_EMP`, `P_SS_DESEMPLEO_EMP`, `P_SS_FP_EMP`,
`P_SS_FOGASA_EMP`, `P_SS_MEI_EMP`) son todos `ENGINE_PROVIDED` con naturaleza `TECHNICAL` y
sin línea de recibo.

> **Nota**: el concepto 800 (IRPF) toma como base `B01`, no `B_CC`. Es correcto —la base de
> retención no es la de cotización— pero conviene tenerlo presente: hoy coinciden porque
> ambas se alimentan solo del salario base, y dejarán de coincidir en cuanto B01 crezca.

---

## Topes: origen, vigencia y prorrateo

Los topes viven en `payroll_engine.ss_cotizacion_topes`, con vigencia por fecha:

```
(rule_system_code, grupo_code, period_type, base_min, base_max, valid_from, valid_to)
```

Sembrada con los valores TGSS de 2025: grupos 01–07 en `MENSUAL`, grupos 08–11 en `DIARIO`,
tope máximo único de 4.909,50 €/mes y 163,65 €/día.

**El grupo de cotización y el tipo de nómina del empleado salen de
`rulesystem.agreement_category_profile`** (V85, sembrada en V87), que asocia cada categoría
del convenio a su grupo y a `MENSUAL` o `DIARIO`. La cadena completa es:

```
empleado → labor_classification → categoría de convenio → agreement_category_profile
        → (grupo de cotización, tipo de nómina) → ss_cotizacion_topes
```

### Prorrateo por segmento

`TopeMaxCotizacionCalculator` y `TopeMinCotizacionCalculator` no devuelven el tope del mes:
devuelven la parte que corresponde al segmento.

```
MENSUAL:  tope × díasDelSegmento / díasDelPeríodo
DIARIO:   tope × díasDelSegmento
```

Y por eso V90 les puso `result_composition_mode = ACCUMULATE`: los trozos de todos los
segmentos se suman y el resultado consolidado es el que recortan `B_CC_MAX` y `B_CC`.

Es la decisión menos evidente de todo el modelo y merece quedar escrita: **los topes son lo
único que se calcula por segmento y se acumula, mientras que todos los conceptos de
cotización tienen `executionScope = PERIOD`.** Si alguien cambia el ámbito de un concepto de
cotización a `SEGMENT` sin entender esto, el recorte deja de cuadrar.

---

## Lo que es firme y lo que es provisional

### Firme

- La cotización vive en el grafo. No hay motor paralelo.
- Los topes se aplican con operadores del grafo (`LEAST`/`GREATEST`), no con condicionales en
  Java.
- El coste de empresa es informativo y no alimenta 980.
- La base de cotización es un concepto con identidad propia.
- El grupo de cotización se deriva de la categoría del convenio, no se informa a mano.

### Provisional — y conscientemente

1. **Los tipos están escritos en Java.** `payroll_engine.ss_cotizacion_tipos` existe, está
   creada y sembrada con los tipos de 2025 por contingencia… y **no la lee nadie**. Los diez
   tipos son constantes en sus respectivas clases `*RateCalculator`. Es la deuda más grave de
   este modelo: cuando la TGSS cambie los tipos habrá que tocar código y desplegar, teniendo
   la tabla al lado.
2. **El IRPF es un 15 % fijo.** `IrpfWithholdingRateCalculator` lo dice en su javadoc: es un
   marcador de posición. El tipo real depende de los datos fiscales del empleado y se
   regulariza. La vertical `employee.tax_information` ya existe (V95); no está conectada.
3. **`P_SS` (6,35 % todo en uno) quedó muerto** al partirse el 700 en V91. El concepto y su
   `SsContributionRateCalculator` siguen en el árbol sin que nadie los referencie.
4. **No hay accidentes de trabajo (725).** Requiere la tarifa por CNAE de cada empresa; queda
   aplazado explícitamente desde V88.
5. **`B01` solo se alimenta del salario base.** Falta la prorrata de pagas extra y cualquier
   otro devengo cotizable. Mientras eso siga así, la base de cotización de esta nómina no es
   la real.
6. **Todo está sembrado para un único convenio** (99002405011982) y un único rule system.

---

## Invariantes

- Ningún concepto de cotización toma su base de `B01`: todos cuelgan de `B_CC`. La excepción
  documentada es 800 (IRPF), cuya base es otra por definición.
- Los conceptos de empresa nunca alimentan 980.
- `P_TOPE_MAX` y `P_TOPE_MIN` deben mantener `ACCUMULATE`; el resto de la cotización, `PERIOD`.
- Un tipo nuevo de cotización se añade como concepto `ENGINE_PROVIDED` + concepto
  `PERCENTAGE` + relación de feed, nunca como rama dentro de un calculador existente.
- Las clases `TechnicalConceptCalculator` no calculan conceptos económicos.

---

## Deuda que este ADR deja abierta

| # | Deuda | Dónde |
|---|---|---|
| 1 | `ss_cotizacion_tipos` no la lee nadie; los tipos son constantes Java | los 10 `*RateCalculator` |
| 2 | IRPF fijo al 15 %, con `employee.tax_information` ya disponible | `IrpfWithholdingRateCalculator` |
| 3 | `P_SS` y `SsContributionRateCalculator` muertos desde V91 | `payroll_engine` |
| 4 | Sin AT/EP (725): falta tarifa CNAE por empresa | V88, aplazado |
| 5 | `B01` incompleta: sin prorrata de extras ni otros devengos cotizables | grafo de feeds |

---

## Relación con ADR anteriores

| ADR | Relación |
|---|---|
| ADR-036 | Amplía las tipologías canónicas de cuatro a seis con `LEAST` y `GREATEST`, por el criterio que el propio ADR-036 fija: son operadores, no casos de negocio |
| ADR-038 | La separación trabajador/empresa se resuelve con `FEED_BY_SOURCE`: quien alimenta 980 descuenta, quien no, informa |
| ADR-041 | El prorrateo de topes por segmento con consolidación `ACCUMULATE` es la aplicación concreta de la segmentación temporal |
| ADR-045 | La elegibilidad de todos estos conceptos se resuelve por `concept_assignment` |
| ADR-046 | `ENGINE_PROVIDED` es el `JAVA_PROVIDED` de ese ADR, renombrado en V89; la regla de que estas clases no calculan conceptos económicos se mantiene intacta |
