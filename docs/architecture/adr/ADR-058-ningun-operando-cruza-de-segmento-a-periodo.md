# ADR-058 — Ningún operando cruza de segmento a período

## Estado
Aceptado

## Contexto

El piloto del backend#46 puso un concepto en `SEGMENT` sobre un empleado con la jornada partida al
50 % a mitad de mes y miró qué salía. Salió lo que se esperaba —dos líneas de `SALARIO_BASE`, 15 ×
61,67 y 15 × 30,84— y, de paso, algo que no:

> Con el concepto de vuelta en `PERIOD` y la misma jornada partida, **el recibo sale idéntico**.

`execution_scope` no lo lee nadie. Y no está solo:

```
persistToConcepts       referencias en com.b4rrhh.payroll: 0
resultCompositionMode   referencias en com.b4rrhh.payroll: 0
executionScope          referencias en com.b4rrhh.payroll: 0
```

Tres propiedades del metamodelo de nómina, guardadas, expuestas en la API, dos de ellas pintadas en
el panel de detalle del designer, y ninguna gobierna nada. El motor deduce el comportamiento de
`functionalNature` —vía `isAccumulable = EARNING || DEDUCTION`— y de `payslipOrderCode != null`. Lo
que parte el recibo son **exclusivamente las ventanas de jornada**.

Debajo hay algo peor que tres campos muertos: **dos defectos que se estaban tapando el uno al otro**.
`DIAS_DEVENGO` está documentado en la V74 como *«accrual days in segment, min(daysInSegment, 30)»* —
es un valor por tramo— y es `TECHNICAL`, así que `isAccumulable` da falso y el motor lo compone con
`put`: gana el último. El valor mensual compuesto de `DIAS_DEVENGO` en un mes partido es **15, no
30**. Hoy nadie se lleva ese 15 porque, al no leerse el ámbito, todo se evalúa dentro de su propio
segmento y nadie lo lee a nivel de período. Cablear el ámbito sin arreglar la composición
**convertiría un campo decorativo en una nómina mal calculada**.

## Decisión

### 1. `execution_scope` se queda, y significa esto

**`PERIOD` quiere decir que la regla del concepto está definida sobre el período entero y no se
reparte en subperíodos.** No es «evalúalo una vez porque es más barato».

La diferencia importa, y se ve con dos ejemplos de la propia semilla. Un subsidio de alimentación a
6 €/día da el mismo número por tramos que de una vez —6×15 + 6×15 = 6×30—: eso es una optimización, y
una optimización no merece un campo en el modelo. Pero:

```
'ESP', 'B_CC_MAX', 'BASE_COTIZACION_MAX',   'LEAST'
'ESP', 'B_CC',     'BASE_COTIZACION_COTIZ', 'GREATEST'
```

Un tope y un suelo **no se distribuyen**: topar dos quincenas por separado y topar el mes una vez dan
números distintos, y sólo uno es el que exige la normativa. Ahí el ámbito es semántica, no
rendimiento, y por eso el campo existe.

### 2. Ningún operando cruza de segmento a período. Los feeds, sí

El modelo ya separa las dos clases de arista en dos tablas, y no son lo mismo:

- **`payroll_concept_feed_relation`** — la bolsa. Un feed de un concepto `SEGMENT` a uno `PERIOD`
  **suma**, y sumar está definido: 15 días + 15 días = 30; 925,05 + 462,60 = 1387,65. Una base
  `PERIOD` alimentada por devengos `SEGMENT` es correcta y es el caso normal.
- **`payroll_concept_operand`** — `qty`, `rate`, `base`, `pct`, `left`, `right`. Un operando
  `SEGMENT` leído desde un concepto `PERIOD` **no tiene respuesta**: «el precio del día» en un mes
  con dos precios no es un número. **Se prohíbe.**

La restricción es de una sola dirección: un concepto `SEGMENT` que lee un valor `PERIOD` no tiene
problema, porque es un número solo, igual en todos los tramos.

Se planteó primero como «un concepto `PERIOD` no puede usar conceptos `SEGMENT`, salvo las bases».
Se descartó porque la excepción hacía todo el trabajo: una base `PERIOD` alimentada por devengos
`SEGMENT` **es** un `PERIOD` leyendo `SEGMENT`. Puesta sobre la arista, la regla no necesita
excepciones y además explica por qué la base es distinta en vez de listarla como caso especial.

### 3. `result_composition_mode` se retira, por consecuencia

Con la regla anterior, una **tasa** `SEGMENT` ya no puede ser operando de nadie a nivel de período,
así que **nunca hace falta componerla**. Y una **magnitud** `SEGMENT` sólo se compone de dos maneras
—para salir en el recibo, o para alimentar un feed— y las dos son suma.

La composición deja de ser una elección del concepto y pasa a ser **siempre suma**. El único caso en
que no lo era queda prohibido por el invariante del grafo, no resuelto por un campo.

Se retira porque sobra, no porque no importara: el eje que intentaba expresar —magnitudes contra
tasas, que `functionalNature` no puede distinguir porque `DIAS_DEVENGO` y `TIPO_IRPF` son las dos
`TECHNICAL`— sigue siendo real. Lo que cambia es que la regla del grafo lo hace inobservable.

### 4. `persistToConcepts` se retira

Su razón era no llenar la tabla de resultados. Esa razón no sobrevive a la aritmética: son 36 filas
por nómina, unas 31.000 para los mil empleados sembrados, tres veces una tabla diminuta. Y con la
separación entre el recibo y la traza de cálculo hay que persistirlo todo, precisamente para poder
explicar un importe.

### 5. Esto es una guarda, no una convención

El invariante de la decisión 2 se comprueba **al guardar el grafo** y en **un test que recorra los
conceptos sembrados**. Una regla que sólo vive en este documento protege a quien se acuerda de ella,
que es exactamente cómo llegamos a tener tres campos muertos.

## Consecuencias

**El designer deja de mentir.** Hoy su panel de detalle ofrece cambiar «Composición» y «Persiste
resultado», y ninguna de las dos cosas hace nada: se puede cambiar delante de quien sea, guardar,
recalcular, y el recibo sale igual. Al retirarlas, lo que queda en pantalla gobierna de verdad, que
es lo que hace demostrable la frase «edito el grafo y la nómina cambia».

**Hay que repasar los 36 conceptos sembrados** y decir de cada uno si su ámbito es `SEGMENT` o
`PERIOD`. Ese valor hoy no lo ha mirado nadie al sembrarlo —los 36 están en `PERIOD` y nadie lo
notó, porque no se lee— y en cuanto empiece a mandar, decidirá el resultado.

**El orden del trabajo no es libre.** Primero el invariante de aristas —que además dirá si la semilla
actual ya lo cumple—, después cablear el ámbito, y sólo entonces retirar los dos campos. Al revés,
cablear el ámbito con la composición sin arreglar convierte `DIAS_DEVENGO` en 15 días.

**Lo que este ADR no decide:**

1. **El valor mensual compuesto que se guarda para la traza.** La regla del grafo arregla el cálculo;
   el registro es otra cosa. Cuando exista la tabla de valores de cálculo, el compuesto de
   `DIAS_DEVENGO` en un mes partido tiene que ser 30, y eso hay que escribirlo aparte.
2. **El redondeo por tramo** (backend#61), que es independiente y sigue abierto — y que al cablear el
   ámbito ganó un segundo sitio donde aparece: el precio ponderado del camino `PERIOD`.
3. **Qué lee un concepto `PERIOD` de las propiedades del propio segmento.** El invariante de la
   decisión 2 gobierna aristas **entre conceptos**; no dice nada de la jornada ni de los días, que no
   son nodos del grafo sino contexto que aporta el motor. Al cablear el ámbito hubo que responderlo
   igualmente, y la respuesta fue **la media ponderada por días**: es la única coherente con la suma
   de los tramos y coincide con el valor del tramo cuando todos son iguales.

   Queda abierta la pregunta de si esa media es alguna vez **correcta**, o si «un concepto `PERIOD`
   que lee la jornada» es en sí mismo un error de modelado que el guardián debería cazar, igual que
   caza el operando. Es la misma frase del ADR —«la jornada de un mes con dos jornadas no es un
   número»— un nivel más abajo, y ahí se le dio un número. No hay trabajo pendiente hasta que alguien
   se tropiece con un caso real; hay que reconocerlo cuando pase.
3. **Qué pasa con la V118** del piloto, que puso `SALARIO_BASE` en `SEGMENT`: con este ADR pasa a
   ser una declaración correcta en cuanto el ámbito se cable, pero hay que confirmarla al repasar los
   36.
