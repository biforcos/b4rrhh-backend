# ADR-074 — Un cálculo sólo lee de otro período lo que está cerrado

## Estado
Aceptado

## Contexto

Hasta el `backend#128`, **ningún cálculo de este motor había mirado fuera de su período**. Una unidad
de nómina se resolvía entera con lo que tenía delante: la presencia, las vigencias de sus verticales,
las entradas del mes y el catálogo de reglas. Eso se acabó con la incapacidad temporal: la base
reguladora de una baja por enfermedad común es **la base de cotización del mes anterior**, y eso es un
número que vive en otro recibo.

La norma, verificada artículo por artículo contra el BOE:

> **Art. 13.1 del Decreto 1646/1972, de 23 de junio** (`BOE-A-1972-944`): «La base reguladora para el
> cálculo de la cuantía del subsidio de incapacidad laboral transitoria será el resultado de dividir el
> importe de la base de cotización del trabajador […] **en el mes anterior** al de la fecha de
> iniciación de la situación de incapacidad […] por el número de días a que dicha cotización se
> refiera.»
>
> **Art. 13.2**: «cuando el trabajador perciba retribución mensual y haya permanecido en alta en la
> Empresa todo el mes natural […] se dividirá **por treinta**.»
>
> **Art. 13.3**: «Para el trabajador que haya **ingresado en la Empresa en el mismo mes** en el que se
> inicie la situación […] se aplicará lo dispuesto en los números anteriores, referido al indicado mes.»
>
> **Art. 13.4**: las pagas extraordinarias entran por el promedio anual de sus bases de cotización.

El ADR-069 ya había puesto una regla parecida **hacia fuera**: ninguna salida lee nada que no sea
`DEFINITIVE`. Ésta es la de dentro, y la diferencia entre las dos es a quién se le miente. Una remesa
de transferencias hecha con un número provisional se reconcilia mal; una **prestación calculada sobre
una base que mañana es otra ya está entregada**, en un recibo, con su PDF.

Y la regla que se escriba aquí no se queda aquí: **la heredan los atrasos del paso 6** de
`workspace#9`, que es el issue difícil de este camino y el que de verdad lee otros meses.

## Decisión

**Un cálculo sólo puede leer de otro período lo que está `DEFINITIVE`, y lo lee por un solo camino.**

### 1. Las tres respuestas, y la del medio es la decisión

Al mirar el mes anterior hay tres respuestas, no dos:

| lo que hay | qué se hace |
|---|---|
| recibo **`DEFINITIVE`** | se lee su `B_CC` y se divide entre 30 (o entre los días del mes si la nómina es diaria) |
| recibo que **existe y no es definitivo** | **el recibo de este mes no se calcula**: queda `NOT_VALID` con su motivo |
| **no hay** recibo | la base teórica de este mes, con aviso salvo que el empleado entrara este mes |

La del medio es lo que este ADR decide de verdad. Un `Optional` sabe decir dos respuestas, y la que
se perdería es justo la que no puede pasar desapercibida: **«hay uno y todavía puede cambiar» no es
«no hay»**. Por eso el puerto devuelve un resultado con tres estados y no un `Optional`.

Lo que **no** se hace, y hay que escribirlo porque es la tentación: leer el `B_CC` de un
`CALCULATED` «porque seguramente no cambia». Si cambia, el recibo de este mes ya está fuera. Y no se
nota: cuando el sabotaje se probó a mano, el recibo salió `CALCULATED`, con una base reguladora de
cero, sin un error y sin un aviso.

Tampoco se hace una tabla de bases históricas importadas. Cuando haga falta —para dar de alta una
empresa con historia— será otro paso, y será un dato de entrada, no una lectura.

### 2. Se pregunta sólo si hace falta

**Si el empleado no tiene ninguna baja por enfermedad común en el período, no se lee nada.** No hay
prestación que calcular ni base durante la baja, así que no hay ninguna razón para exigir que su mes
anterior esté cerrado.

Esto no es una optimización: es la diferencia entre bloquear a un empleado y bloquear una empresa.
Sin esta condición, un recibo de agosto que alguien dejó a medio revisar dejaría sin calcular los 863
recibos de septiembre.

### 3. Un solo camino, y el filtro escrito en la consulta

La lectura pasa por `PreviousPeriodContributionBaseLookupPort` y por nadie más. El filtro
`PayrollStatus.DEFINITIVE` va **dentro de la consulta** y escrito, no pasado como parámetro: un
parámetro se puede pasar mal desde otro sitio, y esta lectura no admite otro estado.

Se lee la **línea del recibo** y no los pasos de cálculo. El recibo es el documento (ADR-062), y «la
base de cotización de agosto» es lo que el recibo de agosto dice que fue.

Si el empleado tiene dos recibos de aquel mes —cese y readmisión— se suman, porque la base de
cotización de un mes es la del mes y no la de una presencia. Y si **alguno** de los dos no está
cerrado, la suma tampoco lo está.

El candado es `OnlyOnePortReadsAPayrollOfAnotherPeriodTest`: enumera quién consulta la tabla de
recibos y falla si aparece un camino nuevo. Hacía falta porque el test de comportamiento no puede ver
una segunda lectura: el día que los atrasos necesiten saber cuánto valía agosto, la forma natural de
escribirlo es una consulta nueva, y una consulta nueva no pone en rojo el test de la base reguladora.

### 4. La base teórica se calcula en el grafo, no en Java

Cuando no hay recibo anterior hace falta un número, y no hay de dónde leerlo. Las dos salidas fáciles
se descartaron a mano:

- **`B01` del mes entre los días devengados.** Divide por cero justo en el caso que más importa: una
  baja que viene de agosto y ocupa septiembre entero no tiene ni un día devengado.
- **`precio del día × (1 + pagas/12)` en Java.** Mete en código una regla que el convenio declara. En
  este motor **lo que interviene en un cálculo se ve en el grafo** (V146), y el número de pagas es
  «cuántas define el convenio» (V144), no un parámetro.

Así que la base diaria teórica es una cadena de conceptos, espejo de la del salario pero sobre **un**
día: `PE_n_DIA` (lo que de cada paga corresponde a un día) → `PE_TOTAL_DIA` → `P_PRORRATA_DIA`
(entre doce) → `BR_TEO` = precio del día + su prorrata diaria. Eso es exactamente lo que dice el art.
13.4: las pagas entran por su promedio.

**El coste está dicho:** hay cuatro conceptos espejo porque el convenio de la demo tiene cuatro pagas,
y una paga nueva son dos filas y no una. Lo que se compra es que el número de pagas siga estando
declarado y no escondido. El día que eso moleste, la respuesta es un ámbito nuevo en el metamodelo
—«por día»— y no una constante en Java.

### 5. `BR_CC` es `SEGMENT`, y el issue decía `PERIOD`

Es una corrección del diseño, y la razón es del metamodelo: `BR_TEO` sale de `P01`, que es `SEGMENT`
desde la `V135` porque el precio del día se busca por categoría. **Un concepto `PERIOD` no puede leer
uno `SEGMENT`** (ADR-058), así que una base reguladora `PERIOD` no podría calcular su teórica.

No se pierde nada: quien la lee —la prestación y la base durante la baja del `backend#129`— es de
tramo también. El valor compuesto del período es una suma que nadie lee, exactamente como le pasa a
`P01`.

### 6. Vale cero donde no hay baja, y eso es a propósito

`BR_CC` vale cero en un tramo sin baja por enfermedad común. No se pierde información —lo que la lee
vale cero ahí también— y se gana una consulta: **«quién tiene base reguladora» se contesta mirando el
número**, sin cruzar nada con las ausencias.

Es la respuesta al «los que no tienen baja no calculan `BR_CC`» del issue, y es lo más cerca que el
modelo llega: `payroll_engine.concept_assignment` acota por sociedad, convenio y tipo de empleado y
**no por empleado**, así que el paso existe para todos y no hay forma de que no exista. Lo que sí hay
es que valga cero y que se pueda contar.

## Consecuencias

- **Un recibo `NOT_VALID` nuevo, y con motivo.** Hasta ahora `NOT_VALID` sólo salía de invalidar a
  mano; ahora el motor lo produce. La corrida lo cuenta en `totalNotValid` y no como fallo, que es lo
  correcto: no falló nada, faltaba algo.
- **Ese recibo se guarda, sin líneas y sin pasos.** Es lo que hace visible que el empleado falta, y es
  lo único desde donde se puede recalcular cuando el mes anterior se cierre (`NOT_VALID` es el único
  estado desde el que se recalcula).
- **La semilla toma el camino de la base teórica, con aviso.** No hay ningún recibo de agosto en el
  sistema, así que los recibos con baja de quien no entró en septiembre llevan
  `REGULATORY_BASE_FROM_CURRENT_PERIOD`. Es la séptima regla del `workspace#3`: donde la salida dice
  «no se sabe del todo», nada se disfraza.
- **Un mes al día es un mes que se calcula.** La regla obliga a cerrar antes de calcular el siguiente
  **sólo para quien tiene baja**, y eso es lo que hace que la regla sea vivible. La suposición de fondo
  —un único período abierto a la vez— está escrita y no implementada: nada impide hoy calcular octubre
  con septiembre abierto, y para quien no tenga baja, sale.
- **Esta migración no mueve ningún recibo.** `BR_CC` no alimenta a nadie todavía. Quien lo lee es el
  `backend#129`, y los dos van en la misma resiembra.
- **Queda abierto el accidente de trabajo.** Su base reguladora es la de contingencias profesionales
  y lleva las horas extra del año anterior, que es un promedio de doce meses y no la base de uno.
  Cuando entre, esta regla de lectura sirve igual; lo que hay que diseñar es de dónde sale el promedio.
