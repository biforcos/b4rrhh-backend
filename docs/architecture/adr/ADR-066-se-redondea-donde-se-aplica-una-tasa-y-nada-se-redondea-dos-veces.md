# ADR-066 — Se redondea donde se aplica una tasa, y nada se redondea dos veces

## Estado
Aceptado

## Contexto

El `backend#61` nació como «el importe depende de en cuántos tramos se parte el mes»: con la jornada
partida, el precio del segundo tramo salía `30,835` y se guardaba `30,84`, y el valor del concepto
era la suma de tramos **ya redondeados**. Con el `backend#64` apareció la otra cara: el mismo
concepto por los dos caminos no cuadraba —`SEGMENT` daba `1068,75` y `PERIOD` `1068,90`— y la
diferencia no era el ámbito, era redondear un precio intermedio con dos decimales en una columna que
admite seis.

Debajo había tres constantes repartidas por el motor, las tres a `2` y `HALF_UP`:
`RateByQuantityOperandResolver`, `PercentageConceptResolver` y el caso `AGGREGATE` de
`DefaultSegmentExecutionEngine`. Ninguna se podía cambiar sin cambiarlas todas, y ninguna sabía si
estaba redondeando un precio intermedio o un importe de folio.

## Decisión

### 1. Se redondea donde se aplica una tasa, y el concepto es ese sitio

Una nómina paga **días**. Un segmento es *los días de este tramo, a qué precio*, y el precio sale de
la tabla salarial y de la jornada — que es literalmente el grafo:

```
P02 (precio día pleno) × J01 (jornada) = P01 (precio día)
D01 (días)             × P01           = 101
```

Los días son enteros y no se redondean. El importe es días × precio, exacto una vez fijado el
precio. **El decimal aparece donde se aplica una tasa** —el precio derivado y los porcentajes— y sólo
ahí. Como el concepto es donde se aplica la tasa, el concepto es donde se declara con cuántos
decimales se queda:

| propiedad | qué dice |
|---|---|
| `rounding_scale` | decimales del resultado de este concepto |
| `rounding_mode` | cómo se resuelve el empate |

No hace falta una tercera propiedad de «punto de aplicación», que es lo que se propuso al principio:
**no hay dos puntos**.

### 2. El motor redondea una vez, al final de evaluar el concepto

Los resolutores devuelven el valor **exacto**; `DefaultSegmentExecutionEngine.evaluate` aplica el
redondeo declarado una sola vez, para **todos** los tipos de cálculo. Eso incluye los que antes no se
redondeaban —`DIRECT_AMOUNT`, `ENGINE_PROVIDED`, `GREATEST`, `LEAST`—, y es deliberado: lo que no
puede pasar es que un valor lleve más precisión de la que dice llevar. Un precio técnico que necesita
ocho decimales los declara.

Hay una excepción, y está acotada: la división entre 100 de un porcentaje sigue trabajando con escala
8. Es un paso intermedio **de esa misma operación**, no el resultado del concepto.

### 3. Nada se redondea dos veces

> El valor de periodo de un concepto `SEGMENT` **es la suma de sus valores de tramo ya redondeados**,
> y nadie vuelve a redondear encima.

Con eso la suma de las partes **es** el total por construcción, y no hay céntimos sobrantes. Eso
elimina toda una familia de reglas —residuo a la última línea, concepto de ajuste— que en otros
motores es donde se acumula la complejidad. No la necesitamos si no redondeamos dos veces.

Lo sujeta un test, no sólo este párrafo, y ese test costó un sabotaje: la primera versión no medía
nada porque `collapsePayslipRows` agrupa por `concepto|tarifa`, y con dos tramos a **precios
distintos** la función de fusión no llega a ejecutarse. La comprobación necesita dos tramos **al
mismo precio**, y necesita que la suma lleve más de dos decimales para que un segundo redondeo se
notara.

### 4. Se enseña la precisión que se usó

El paso guardado lleva el valor **con sus decimales declarados**, y la pestaña «Cálculo» y el grafo
lo enseñan así. Un concepto con seis decimales enseña seis, y entonces la multiplicación cuadra con
una calculadora igual de bien.

**Lo prohibido no es la precisión: es la precisión oculta.** Enseñar `33,33` habiendo usado
`33,333333` es lo que impide rehacer el cálculo a mano.

### 5. `P01` lleva seis decimales, y lo decidió una medición

Era la única casilla abierta de la propuesta. Medido sobre los 873 recibos de la semilla antes de
tocar nada:

```
tramos P01 medidos            878
tramos P01 que cambian          1
recibos que cambian             1      (EMP000003)
diferencia                  -0,07 EUR de devengos, -0,01 de IRPF
```

Con dos decimales se lee mejor en el recibo; con seis se paga lo que corresponde. Cuesta un recibo y
siete céntimos migrarlo **hoy**, y ése es el argumento: la semilla es benévola —cinco meses partidos,
de un solo corte, con la jornada al 50 %, que es el divisor más amable que existe— y el `backend#47`
va a multiplicar los tramos. Con un tercio de jornada son diez céntimos al mes por empleado.

## Consecuencias

- **Una propiedad de cálculo en el catálogo de conceptos sólo entra con un test que demuestre que
  cambiar su valor cambia un resultado.** No es una preferencia: ya metimos
  `result_composition_mode` con su `ACCUMULATE` (V90) y hubo que retirarla en la V120 por **inerte**.
  Las dos de este ADR nacen con ese test.
- **El defecto es `2` y `HALF_UP`, que es lo que el motor hacía.** Un concepto que no declare nada se
  queda como estaba, así que declarar las propiedades no movió ni un importe salvo donde se declaró
  otra cosa a propósito.
- **Un concepto técnico que necesite precisión tiene que declararla.** Antes se la quedaba por no
  pasar por ningún redondeo; ahora, si no la declara, la pierde. Es el precio de que no haya
  precisión oculta, y se paga una vez por concepto.
- **La semilla se recaptura** cuando esto entra: hay un recibo que cambia de importe.

## Fuera de alcance

Qué rompe el periodo en segmentos: eso es el `backend#47`, y va **después** de esto a propósito. Los
dos obligan a recomprobar la semilla entera, y hacerlos al revés significa medirla dos veces —primero
con más tramos y la aritmética vieja, después con la nueva sobre más tramos—.

Y la base de cotización de un mes parcial, que es otra pregunta del mismo issue y no es de redondeo:
si el mínimo mensual se prorratea por días o no. Vive con el `backend#47`.
