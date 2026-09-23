# ADR-072 — El tipo de desempleo lo dice una tabla, y no el nombre del contrato

## Estado
Aceptado

## Contexto

El `backend#123` dejó `DESEMPLEO_EMP` en 5,50 y `DESEMPLEO_TRAB` en 1,55 para todo el mundo. Son
correctos, y son los de **una** de las dos modalidades: la Orden de cotización distingue la
contratación indefinida (7,05 % = 5,50 empresa + 1,55 persona trabajadora) de la de duración
determinada (8,30 % = 6,70 + 1,60), art. 33.2.a).1.º y 2.º de la Orden PJC/297/2026 y sus
equivalentes de 2025 y 2024.

En la semilla de la demo hay **182 empleados con contrato 401 o 402** cotizando por la modalidad que
no les toca. No es una hipótesis: son los contratos que el loader siembra.

Y hay una trampa dentro que decide casi todo lo demás. El art. 33.2.a).1.º no dice «indefinido»:
mete en el tipo bajo, además de los indefinidos y los fijos discontinuos, **los contratos
formativos, los de relevo y los de sustitución e interinidad**, y cualquier modalidad celebrada con
una persona con discapacidad reconocida del 33 % o más. En el catálogo español que la demo usa, de
los ocho contratos que no son indefinidos, **seis cotizan como si lo fueran**:

| contrato | | modalidad de desempleo |
|---|---|---|
| 100, 108 | indefinido ordinario, completo y parcial | INDEFINIDA |
| 109, 110 | fijo discontinuo, completo y parcial | INDEFINIDA |
| 410, 420 | sustitución con reserva de puesto, y en proceso de selección | INDEFINIDA |
| 421, 422 | formación en alternancia, práctica profesional | INDEFINIDA |
| **401, 402** | temporal por circunstancias de producción, y ocasional | **DETERMINADA** |

## Decisión 1 — La modalidad es un dato con vigencia, no una regla sobre el código

`payroll_engine.ss_desempleo_modalidad_contrato` dice, por sistema de reglas y por contrato, en qué
modalidad cotiza, con `valid_from` / `valid_to` como el resto del catálogo de cotización.

**Por qué una tabla y no una regla.** Cualquier regla escrita sobre el código («si empieza por 1 es
indefinido», «si es temporal, el tipo alto») acierta en cuatro de los diez y falla en seis. Y
fallaría en silencio: un formativo cotizando al 6,70 sale caro, no da error, y nadie lo mira.

**Por qué en `payroll_engine` y no en el catálogo de `rulesystem`.** La modalidad no es una
propiedad del contrato: es lo que la Orden de cotización dice de él. El mismo contrato podría
cambiar de modalidad con la Orden del año que viene sin dejar de ser el mismo contrato, y lo que se
declararía entonces es una fila nueva con su vigencia, no una migración del catálogo. Es la frontera
del ADR-042: lo que define **cómo** se calcula una nómina vive en `payroll_engine`.

## Decisión 2 — Cuatro códigos de contingencia, no dos más un supuesto

`DESEMPLEO_EMP` y `DESEMPLEO_TRAB` desaparecen de `ss_cotizacion_tipos`. En su lugar:

```
DESEMPLEO_EMP_INDEFINIDA     5,50      DESEMPLEO_EMP_DETERMINADA     6,70
DESEMPLEO_TRAB_INDEFINIDA    1,55      DESEMPLEO_TRAB_DETERMINADA    1,60
```

La alternativa era dejar los dos de antes como «la modalidad indefinida» y añadir sólo los dos
nuevos. Se descartó: el catálogo tendría dos filas que se llaman por lo que cotizan y dos que se
llaman por lo que cotizan **más un supuesto tácito**, y quien las lea dentro de un año no sabría
cuál es cuál.

El motor **compone** el código de contingencia —`DESEMPLEO_EMP_` más la modalidad del tramo— y lo
busca como cualquier otro tipo. Así el *cuánto* sigue viviendo en `ss_cotizacion_tipos` con su
vigencia y el *cuál de los dos* en la tabla nueva, también con la suya. `DesempleoRateCalculator` es
el mismo `SsCotizacionRateCalculator` con un paso delante.

## Decisión 3 — Sin contrato o sin modalidad, la corrida se para

No hay modalidad por omisión. Un tramo sin contrato, o un contrato sin modalidad declarada, para el
cálculo con el código dentro del mensaje.

Es la misma regla que el CNAE del ADR-071 y por el mismo motivo, que aquí es más agudo: **el error
barato es el que no se ve**. Suponer `INDEFINIDA` haría cotizar de menos a los temporales, que es
exactamente el defecto que este ADR viene a cerrar, y saldría verde.

La consecuencia, que es real: un empleado sin contrato ya no tiene nómina. En producción eso es
correcto —el alta crea el contrato— pero dejó en rojo treinta escenarios de prueba que no hablan de
contratos. `PayrollScenarioFixtures.insertPresence` pone ahora un 100 por omisión, y quien quiera
otro lo declara.

## Lo que este ADR **no** resuelve, y hay que saberlo

**La base no se parte por tramo.** La modalidad viaja por el tramo, pero la base de cotización y las
cuotas son conceptos de ámbito `PERIOD` —la base es mensual, art. 1.1 de la Orden— y el motor las
resuelve una vez, con el contexto del último tramo. Un contrato que cambia el día 16 deja **el mes
entero** al tipo del contrato nuevo, cuando le tocaría medio mes a cada uno.

Partir la base por tramo no es un ajuste de este issue: es un cambio del modelo de cotización, con
dos bases prorrateadas por días y dos juegos de cuotas en el mismo recibo. Queda con su test escrito
—`aMidMonthChangeOfContractIsNotSplitYetAndTakesTheLastContractRate`— que dice lo que pasa y **no**
que esté bien, para que el día que se parta la base esté escrito qué tiene que cambiar.

**La discapacidad no entra.** El art. 33.2.a).1.º manda al tipo bajo cualquier contrato celebrado
con una persona con discapacidad reconocida del 33 % o más. Es una condición de la persona y no del
contrato, y el modelo no guarda hoy el grado de discapacidad fuera de `employee_tax_information`,
donde está para el IRPF y no para esto. Un empleado con un 401 y un 33 % reconocido cotiza por el
2.º cuando le toca el 1.º: caro y no barato, que es el lado bueno de equivocarse, pero sigue siendo
un error y necesita el dato.

## Consecuencias

- Un contrato nuevo en el catálogo necesita su fila de modalidad, o ningún empleado suyo cotiza.
  Eso es a propósito: es la pregunta que hay que hacerse al darlo de alta.
- La demo mueve 182 recibos en `703`, `721`, `725`, `980` y `990`.
- El `ss_cotizacion_tipos` pasa de 33 a 37 filas y sigue sin huecos ni solapes, que es lo que
  comprueba `EachExerciseHasItsOwnRatesAndLimitsTest`.
