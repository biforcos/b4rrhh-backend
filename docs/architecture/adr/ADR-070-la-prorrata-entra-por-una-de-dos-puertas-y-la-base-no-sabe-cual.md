# ADR-070 — La prorrata entra por una de dos puertas, y la base no sabe por cuál

## Estado
Aceptado

## Contexto

La prorrata de pagas extraordinarias es la primera forma que el modelo no sabía expresar: **un
valor que viene de un importe anual repartido**, no de cantidad × tarifa. Y cotiza, así que mueve
la base de cotización y con ella media nómina.

Un empleado cobra las pagas prorrateadas o no. **La cotización no distingue**: la prorrata entra en
la base de contingencias comunes en los dos casos. Lo que cambia es si además se paga.

Eso son dos líneas del recibo y una sola cantidad:

| régimen | qué es | alimenta | dónde se imprime |
|---|---|---|---|
| prorrateado | devengo salarial | `B01`, `970` | entre los devengos |
| no prorrateado | línea del recuadro de bases | `B01` | antes de `B_CC` |

Y una invariante que es la razón de ser del paso: **la base no sabe si se pagó**. Para el mismo
salario, `B_CC` es idéntica en los dos regímenes.

Tres cosas no tenían respuesta en el motor de antes.

## Decisión 1 — Dividir es un tipo de cálculo, no un porcentaje

**Se añade `CalculationType.QUOTIENT`, con operandos `BASE` y `DIVISOR`.**

La prorrata es `Σ pagas / 12`. Escribirla como un `PERCENTAGE` al 8,33 % sería redondear dos veces
—una al escribir el tipo y otra al aplicarlo— y el `backend#61` (ADR-066) dejó dicho que **nada se
redondea dos veces**. Dividir entre doce tiene un solo redondeo: el que el motor aplica al salir,
con los decimales que el concepto declara.

Un tercio no tiene escritura decimal exacta, así que «el valor sin redondear» no existe y hay que
elegir una precisión intermedia: `MathContext.DECIMAL128`, 34 cifras significativas. Es la misma
idea que la escala 8 de `J01`, con más holgura porque aquí el resultado es dinero.

**El divisor es un concepto del grafo** (`P_MESES_ANO` = 12) y no una constante dentro de un
resolutor. En este motor lo que interviene en un cálculo se ve en el grafo; escondido en Java, la
pregunta «de dónde sale este número» tendría una respuesta que sólo está en el código, que es
exactamente lo que el primer camino quitó de en medio.

Dividir entre cero revienta la corrida, y eso es lo correcto: un divisor a cero es un catálogo mal
parametrizado, no un caso de negocio.

## Decisión 2 — El régimen llega por el tramo, no por la asignación

**El régimen viaja en `SegmentCalculationContext` y lo leen dos conceptos `ENGINE_PROVIDED`,
`J_PRORRATEADAS` y `J_NO_PRORRATEADAS`, que valen uno o cero.**

La alternativa natural era una condición en `payroll_engine.concept_assignment`: asignar el
concepto de los devengos a quien prorratea y el de bases a quien no. **No vale, y no es una
cuestión de gusto.**

La asignación se resuelve **una vez por período**: `CalculatePayrollUnitService` arma el plan de
conceptos con un único `EmployeeAssignmentContext` y *después* parte el mes en tramos. El régimen,
en cambio, es una vertical con vigencia y puede cambiar a mitad de mes (ADR-068, `backend#118`). Un
recibo partido necesita las dos respuestas —una por tramo— y la asignación sólo sabe dar una.

Así que las dos puertas se asignan **las dos a todo el mundo**, y lo que decide por cuál entra la
cantidad es un coeficiente que sí es del tramo. La puerta que sobra da cero, y un cero no se
imprime desde el `backend#104`: el recibo de cada uno enseña exactamente una.

Esto le da al motor una capacidad que no tenía: **una condición por tramo**. El paso 5 —la
incapacidad temporal, que es un porcentaje por tramos de días de baja— la va a necesitar igual.

## Decisión 3 — La invariante es cierta por construcción

Los dos conceptos son **el mismo importe** multiplicado por dos coeficientes que **suman uno
siempre**, y **los dos alimentan `B01`**.

```
B01 = remuneración cotizable + J_PRORRATEADAS × prorrata + J_NO_PRORRATEADAS × prorrata
    = remuneración cotizable + prorrata
```

La base recibe la prorrata exactamente una vez sea cual sea el régimen. No hay nada que recordar y
no hay caso que se escape: es una identidad algebraica, no una regla.

Lo que se descartó fue lo contrario —calcular la base sumando «la prorrata si toca»— que es la
forma en la que esto se escribe cuando se escribe deprisa, y la que deja el error donde no se ve:
en el régimen que menos gente tiene.

**Se comprueba igual.** Que algo sea cierto por construcción no quita el test, porque la
construcción se puede deshacer sin querer: basta con que alguien le quite un feed a `B01` o toque
un coeficiente.

### Los topes salen gratis

`B_CC_MAX = LEAST(B01, P_TOPE_MAX)` se aplica **después** de `B01`, así que la prorrata entra en la
base y el tope la recorta sin que nadie lo haya programado. No hubo que hacer nada; lo que hubo que
hacer es **demostrarlo**, con un salario que sólo con la prorrata pasa del tope.

## Decisión 4 — La base del IRPF deja de ser la base de cotización

**El `800` pasa de leer `B01` a leer `970`, el total devengado.**

Esto no lo pedía el issue, y es la decisión de este ADR que conviene mirar dos veces.

Hasta hoy daba igual: `B01` y `970` valen lo mismo, porque los dos se alimentan del `101` y del
`102`. Con la prorrata dejan de valerlo, y es justo ahí donde se ve cuál de los dos es la base de
la retención.

**El IRPF se retiene sobre lo que se paga.** Quien tiene las extras prorrateadas cobra la prorrata
todos los meses y tributa por ella todos los meses; quien no, tributará por la paga entera el mes
que la cobre. Que la prorrata que **no se paga** entrara en la base de la retención sería retener
por dinero que no se ha entregado.

Sin este cambio, la invariante gemela del paso 4 —«mismo salario y distinto régimen: la misma
`B_CC` y las mismas cuotas, distinto líquido **e IRPF**»— sería falsa en su última palabra.

Se hace **ahora, que no cuesta un céntimo en ningún recibo existente**, y no el día que alguien lo
note. Es la misma decisión que tomó la `V131` con los decimales del precio del día: se elige cuando
cuesta un recibo, no cuando cuesta una nómina.

Lo que `B01` sigue siendo es la **base de cotización**, que es lo que dice su nombre y lo que leen
`B_CC_MAX` y `B_CC`.

## Consecuencias

- El motor sabe dividir, y el paso 6 —los atrasos, que son deltas— tiene una forma menos que
  inventar.
- El motor sabe condicionar **por tramo**, que es lo que el paso 5 necesita.
- Todos los recibos se mueven: sube `B01` en todos, y con ella las cuotas, el `725` y el líquido.
  La retención sube sólo en los prorrateados.
- El recuadro de bases del recibo pasa de dos líneas a tres.
- `B01` y `970` dejan de ser intercambiables. Quien escriba un concepto nuevo tiene que elegir cuál
  lee, y ya no es una pregunta sin consecuencias.

## Lo que sigue fuera

- **Pagar la extra en su mes** y las partes proporcionales del finiquito: necesitan la maquinaria
  del cálculo vigente del paso 6. `payroll_type_code` ya tiene el `EXTRA` reservado.
- **La bolsita de lo devengado**: no se construye. Lo que iría a ella es exactamente la línea de
  prorrata de cada recibo definitivo, con su período. Un tercer sitio con el mismo número sería uno
  más sin mandar.
- **Cuántas pagas y de qué se componen** es del convenio (`backend#117`), no del motor. Una «paga
  de beneficios II» es un agregado más.
