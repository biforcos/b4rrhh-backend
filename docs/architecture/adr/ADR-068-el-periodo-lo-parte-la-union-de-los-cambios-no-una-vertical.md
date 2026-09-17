# ADR-068 — El período lo parte la unión de los cambios, y ninguna vertical tiene privilegio

## Estado
Aceptado

## Contexto

`CalculatePayrollUnitService.buildSegments` rompía el período **sólo** por las ventanas de jornada:

```java
if (input.workingTimeWindows() == null || input.workingTimeWindows().isEmpty()) {
    return List.of(new SegmentSpec(presenceStart, presenceEnd, days, BigDecimal.valueOf(100)));
}
for (var window : input.workingTimeWindows()) { ... }
```

Un cambio de **categoría de convenio** o de **contrato** a mitad de mes no creaba segmento. Y no
había interruptor que lo arreglara: **no es que estuviera desactivado, es que no existía el
mecanismo**. La partición vivía dentro del bucle de una vertical, así que romper el período era un
privilegio suyo.

El coste no es teórico. El precio del día sale de una fila de tabla que se busca **por categoría**,
así que resolverlo una vez para todo el mes ponía el precio del último tramo en los días del
primero: un empleado que asciende el día 16 cobraba los 31 días al precio nuevo. El recibo salía con
un número redondo que parecía correcto, y la única forma de ver que mentía era rehacer los dos
tramos a mano — justo lo que el colapso por `conceptCode|rate` está diseñado para impedir.

## Decisión

**Las fechas de corte del período salen de la unión de los puntos de cambio de las verticales que
afectan al cálculo. La partición sólo sabe de fechas.**

`PayrollPeriodSegmentation` recibe una colección de fechas y devuelve intervalos. No conoce la
jornada, ni el convenio, ni el contrato: cada vertical aporta las suyas, se unen, se ordenan, y los
segmentos son los intervalos entre cortes consecutivos recortados contra la presencia.

### 1. Qué es un corte

El **primer día de un tramo nuevo**. De una ventana que empieza el 16 sale un corte el 16; de una
que termina el 15, un corte el 16 —lo que cambia es el día siguiente—. Esa segunda mitad es la que
se olvida: una ventana que se cierra y **no tiene relevo** también parte, porque a partir de ahí lo
que valiera en ella deja de valer.

Dos verticales que cambian el mismo día son **un** corte, y ése es el caso normal: un alta cambia el
contrato y la clasificación a la vez.

### 2. Qué verticales rompen

Jornada, clasificación laboral y contrato. Es una decisión de negocio y éste es su mínimo.

El **centro de trabajo** y la **distribución de coste** se quedan fuera *hasta que alguien decida
que entran*, no porque no quepan: caben con una línea en `buildSegments` y otra en `vigenciasEn`.

El **contrato rompe aunque hoy no lo lea ningún concepto**, y eso es deliberado: lo que cuesta es un
paso repetido en la pestaña de cálculo —el folio los vuelve a fundir, porque valen lo mismo—, y lo
que compra es que el día que un concepto lea el contrato ya esté partido.

### 3. El segmento lleva sus vigencias, y la jornada es una más

`SegmentSpec` deja de llevar `workingTimePercentage` como campo propio —que era decir que la jornada
es la única causa— y pasa a llevar un registro de vigencias con lo que cada vertical tenía en pie.
Se resuelven **el primer día del segmento**, y con eso basta: dentro de un segmento no hay cambios
**por construcción**, porque si los hubiera ese día sería un corte.

### 4. Lo que se calcula por contexto y no por período

Los conceptos `DIRECT_AMOUNT` se precalculan **una vez por contexto de convenio**. Dos segmentos con
la misma categoría comparten el precálculo —la búsqueda es la misma y la respuesta también—; dos con
categorías distintas tienen precios distintos, que es el punto entero.

Lo que es del período —los conceptos de ámbito `PERIOD`, cuya regla está definida sobre el mes
entero (ADR-058)— se resuelve con el contexto del **último** segmento, que es el último día que el
empleado estuvo presente. Es lo que se hacía antes para todo, y para ellos sigue siendo lo correcto:
no hay un tramo suyo al que preguntarle.

### 5. Partir no basta: el concepto tiene que decir que su valor es del tramo

Un concepto de ámbito `PERIOD` se evalúa una vez aunque haya diez segmentos. Así que la partición no
cambia nada por sí sola para un valor declarado `PERIOD` — y el precio del día lo estaba, por
decisión explícita de la `V119`: *«P02 PRECIO_DIA_PLENO: un único precio del rule system para el
período»*.

Esa frase era cierta mientras el período sólo lo partiera la jornada. **La `V135` la sustituye** y
pasa `P02` a `SEGMENT`. Es parametrización y no código: el motor ya sabía evaluar un concepto una
vez por tramo contra su propio contexto.

`P03` —el precio de la hora extra— se queda en `PERIOD` y no por descuido: lo lee el `102`, que es
`PERIOD`, y ningún operando cruza de segmento a período (ADR-058). Moverlo arrastraría al `102`, que
multiplica las horas declaradas **para el mes** y en dos tramos las contaría dos veces. Eso no es un
ámbito mal puesto: es que las horas extra por tramo no están modeladas.

## Consecuencias

- **Ningún recibo que ya salía bien se mueve.** Un empleado sin cambios tiene un segmento; uno sin
  ventanas de jornada se calcula al 100 %, como antes.
- **Se mueven recuentos de pasos, no importes.** Los conceptos de ámbito `SEGMENT` pasan de 4 a 5, y
  un empleado del mes partido de 42 pasos a 43. Con una sola categoría los dos tramos leen la misma
  fila y el folio los vuelve a fundir: lo que se añade es el paso que lo dice.
- **`collapsePayslipRows` no se toca**, y ahora hace más trabajo del que hacía: más causas de
  ruptura son más segmentos, y el colapso por precio sigue siendo lo que evita duplicar líneas
  idénticas. Dos tramos al mismo precio son una línea; dos a precios distintos son dos, que es la
  verdad que antes no se podía contar.
- **No se añaden columnas por vertical a `payroll.payroll_segment`.** Eso estaba descartado y sigue
  estándolo: privilegiar una vertical con una columna no escala, y para eso está el snapshot de
  contexto.
- **El apaño del `backend#73` deja de decidir el precio.** La fecha de referencia del lanzador —el
  último día dentro de la presencia— sigue resolviendo lo que es del período: con qué convenio se
  arma el plan de conceptos y qué foto se guarda. Ya no decide a qué precio se paga cada día.
- **Queda abierto qué hacer con las horas extra por tramo.** Hoy son un dato del mes, y hasta que no
  lo sean del tramo, `P03` y el `102` no pueden bajar a `SEGMENT`.
