# ADR-076 — El recibo es un documento y el cálculo vigente es estado

## Estado
Aceptado

## Contexto

El paso 6 de `workspace#9` son los atrasos, y un atraso empieza con una pregunta que hasta ahora este
motor no sabía contestar: **¿cuánto valdría agosto si lo calculáramos hoy?**

Agosto está cerrado. Su recibo es `DEFINITIVE`, tiene PDF, y el empleado tiene una copia impresa. Y a
la vez hay unas horas que nadie metió a tiempo, así que **el número que hay en ese recibo ya no es el
número correcto**. Las dos cosas son verdad al mismo tiempo, y ahí está el problema entero.

Las dos salidas obvias son las dos maneras de estropear un motor de nómina:

| lo fácil | qué se pierde |
|---|---|
| **Recalcular el recibo de agosto.** «Hay que corregirlo, pues se corrige.» | La única copia de lo que se le entregó al empleado. A partir de ahí ya no se puede explicar una nómina: el número que la persona tiene impreso **deja de existir en el sistema**, y si pregunta, nadie puede decirle de dónde salía. |
| **Negarse a recalcular agosto.** «Un mes cerrado es intocable.» | La capacidad de pagar un atraso. Lo que se sabe hoy de agosto no cabe en ninguna parte, así que el delta no se puede calcular y las horas no se pagan nunca. |

Hasta el `backend#128` el motor no había mirado nunca fuera de su período, y la regla que se puso allí
—**sólo se lee lo cerrado**— se escribió sabiendo que la heredarían los atrasos. Ésta es la otra mitad:
la que dice **dónde se escribe** lo que un mes cerrado vale hoy.

Y hay una restricción que ya existía y que no se toca: `canBeRecalculated()` sólo permite recalcular
desde `NOT_VALID`. Está bien y sigue igual. Lo que este ADR decide es que el modo retro **no pasa por
ahí**, porque no escribe recibos.

## Decisión

**Dos tablas con la misma forma y distinta naturaleza, y no se tocan.**

> El **recibo** es un documento: inmutable, se entrega, tiene PDF.
> El **cálculo vigente** es estado: mutable, se pisa, no lo ve nadie.
> **Confundirlas es como se pudren los motores de nómina.**

### 1. El cálculo vigente: conceptos, no totales, y sin pasos

`payroll.current_calculation` + `payroll.current_calculation_concept` (V160). Por empleado, presencia y
período: **las líneas completas** de lo que ese mes vale hoy, con el instante y el run que lo calculó.

**Conceptos y no totales**, porque lo que viaja al atraso son los deltas *por concepto*: diez euros de
salario base de agosto no son lo mismo que diez de horas extra, ni cotizan igual, ni se imprimen en el
mismo bloque. Guardar el líquido de agosto no serviría para construir ni una línea.

**Sin los pasos de cálculo**, y eso sí es una decisión con coste: el vigente no se explica, se compara.
Guardarlos multiplicaría por treinta el tamaño de la tabla —la semilla tiene 30.575 pasos frente a
12.000 líneas— para responder una pregunta que nadie hace de un número que se va a pisar. La
explicación de un atraso (`backend#134`) se da con tres importes —vigente, pagado, diferencia— y los
tres están en las líneas.

**Se pisa**, y no se pierde nada al pisarlo: la historia vive en los recibos, cada línea de atraso
guardada en su mes con su período de origen (`backend#133`). Dos respuestas a «cuánto vale agosto hoy»
serían dos números sin forma de saber cuál manda.

### 2. El tramo va hacia delante, y entero

Al recalcular, se recorren los meses **desde el más antiguo tocado hasta P−1, en orden y hacia
delante**. Las dos mitades hacen falta:

- **Entero y no sólo el mes tocado**, porque los meses se leen unos a otros: unas horas en julio mueven
  la base de cotización de julio, y la base reguladora de agosto lee julio (`backend#128`). Recalcular
  sólo julio dejaría agosto diciendo un número que ya no sale de ninguna parte.
- **En orden**, porque si agosto se calculara antes que julio leería el julio viejo. No es una
  preferencia: es la condición que hace que el resultado sea el mismo que si nunca hubiera habido un
  error, y eso es lo único que un atraso puede prometer.

Cada mes se escribe en su propia transacción y **un mes que falla no aborta el tramo**: el vigente de
julio es cierto aunque agosto no se haya podido calcular, y una transacción sobre el tramo entero
obligaría a repetirlo todo por un mes.

### 3. En modo retro, lo que lee otro mes lee el vigente

El puerto del `backend#128` gana un modo (`PreviousPeriodSource`) y **sigue siendo el único camino** por
el que un cálculo mira fuera de su período: mismo puerto, mismo candado
(`OnlyOnePortReadsAPayrollOfAnotherPeriodTest`). Lo que cambia es la fuente:

| modo | de dónde lee lo de otro mes |
|---|---|
| `RECEIPT` (el normal) | **sólo del recibo cerrado**, con el filtro escrito en la consulta (ADR-069 §2, ADR-074) |
| `CURRENT_CALCULATION` (retro) | **del vigente si existe, y si no del recibo cerrado** |

La segunda mitad de esa fila no es un descuido: si julio no tiene vigente es porque julio no entraba en
el tramo, y entonces **su recibo cerrado es lo que julio vale**.

Y en modo retro no hay tercera respuesta. Un vigente no tiene estados: existe o no existe, así que no
puede salir `NOT_DEFINITIVE`. Las tres respuestas del ADR-074 siguen intactas donde siguen aplicando,
que es cuando se cae al recibo.

**Se pregunta en dos pasos —si hay vigente, y luego cuánto— por la misma razón que en el ADR-074 §1:**
un vigente cuya base valga cero sería indistinguible de no tener vigente, y la retro de agosto se
calcularía con el julio viejo sin que nada lo dijera.

### 4. El modo va en dos métodos y no en un parámetro

`CalculatePayrollUnitUseCase` tiene `calculate(...)` → `Payroll` y `calculateCurrent(...)` →
`CurrentCalculationOutcome`. **Los tipos de retorno son distintos porque las cosas son distintas.** Un
`Payroll` que a veces no es un recibo sería el primer paso para que alguien lo guarde como si lo fuera.

Dentro, el cálculo es **el mismo hasta la última línea** —el mismo grafo, los mismos tramos, las mismas
reglas, la misma proyección al folio— y bifurca en un solo punto. Que sea el mismo punto y las mismas
líneas es lo que hace **comparable** un vigente con un recibo, que es la invariante del `backend#133`;
que el camino se corte ahí es lo que hace que el recibo entregado no se pueda tocar.

Duplicar el cálculo para los dos modos sería la forma de que el vigente y el recibo se separen en
silencio el día que alguien toque uno de los dos.

### 5. Las reglas de cada mes son las vigentes en aquel mes

El metamodelo se carga **una vez por mes** y con la fecha de ese mes, no con la del lanzamiento. Eso ya
lo sabía hacer el motor (`backend#105`); aquí es donde importa hacia atrás: lo que se corrige es lo que
aquel mes **debió ser**.

El coste está dicho: una consulta del metamodelo por mes y por empleado. La alternativa —cargarlo una
vez— calcularía diciembre de 2024 con las reglas de 2025. Probado a mano rompiendo la regla: no da un
número distinto, da un error (`No active table row found for table P02_…, date 2024-12-31`), porque la
tabla de precios de 2024 no está en el metamodelo de 2025. Que falle ruidosamente es suerte; la regla
no depende de eso.

### 6. El aviso del modo retro no va a ningún recibo

En el camino normal, un mes que no se puede calcular **se guarda** como recibo `NOT_VALID` con su
motivo, porque es lo que hace visible el hueco (ADR-074). En modo retro eso no se hace: el recibo de
aquel mes está entregado y **no se toca ni para decir que algo ha ido mal**. El motivo vuelve a quien
lanzó, que lo cuenta en los mensajes de la corrida.

Es la séptima regla del `workspace#3` —donde la salida dice «no se sabe del todo», nada se disfraza—
aplicada al único sitio donde escribirlo sería peor que decirlo.

### 7. Lo que no viaja: la retención, su base y los totales

La diferencia de un mes baja al recibo abierto concepto a concepto, **salvo** cuatro grupos, que son
del mes que paga y no del mes corregido (`RetroDeltaCalculator.NO_VIAJAN`): **la retención de IRPF
(`800`) y su base (`B09`)**, porque se retiene sobre lo que se paga cuando se paga (ADR-070 §4); **los
totales** (`970`, `980`, `990`, `725`), que son sumas de este mes; y **los técnicos del atraso**
(`A_DEV`, `A_DED`, `A_EMP`), porque un atraso de un atraso no existe. La base del IRPF entró tarde
(`backend#140`): viajaba, y cada mes que había pagado un atraso y se recalculaba sacaba una línea de
`B09` por exactamente ese atraso. La invariante del `backend#133` excluye los mismos, y un candado lo
cruza.

## Consecuencias

- **Un candado nuevo: `NoRetroPathWritesTheReceiptOfAClosedMonthTest`.** Ningún fichero de
  `payroll/retro` puede tomar los tres caminos que escriben el documento, y sólo un fichero del árbol
  —la costura— puede nombrar a la vez el modo retro y uno de esos caminos. Hacía falta porque el
  escenario que compara el recibio antes y después no se pone rojo cuando aparece un camino *nuevo*, y
  el camino nuevo es exactamente lo que va a aparecer: la forma natural de escribir algo más durante una
  retro es reusar el servicio que ya sabe escribir recibos.
- **Leer el recibo desde el modo retro sí vale**, y el candado lo distingue por la llamada y no por el
  nombre del repositorio. Tiene que valer: el modo retro necesita saber qué meses están cerrados
  (`backend#130`) y cuánto se ha pagado por ellos (`backend#133`). Un candado que prohibiera nombrar el
  repositorio prohibiría la mitad del paso 6.
- **El `run_id` del vigente es el del período abierto**, no el del mes recalculado. Un lanzamiento de
  septiembre escribe vigentes de junio, julio y agosto y los tres llevan el run de septiembre; por eso
  el período y el run son dos columnas y ninguna se deduce de la otra.
- **Nada de esto se lanza todavía.** Quién recalcula, desde cuándo y con qué límites es el
  `backend#132`; lo que se paga con el delta es el `backend#133`. Esta pieza da el motor y el sitio
  donde escribir, y se prueba llamándola directamente.
- **`canBeRecalculated()` no se ha tocado.** Sigue permitiendo recalcular sólo desde `NOT_VALID`, y el
  modo retro no pasa por ahí porque no escribe recibos. Las dos reglas conviven sin conocerse.
- **Queda abierto el coste en la semilla.** Nueve meses cerrados por ochocientos sesenta empleados con
  retro son muchos vigentes, y cuánto cuesta escribirlos se sabrá en el `deploy#22`. Lo que ya se sabe
  es que no se guardan pasos, que es lo que habría hecho eso inviable.
