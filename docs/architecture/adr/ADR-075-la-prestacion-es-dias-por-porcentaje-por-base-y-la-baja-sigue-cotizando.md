# ADR-075 — La prestación es días × porcentaje × base reguladora, y la baja sigue cotizando

## Estado
Aceptado

## Contexto

El `backend#127` dejó la ausencia partiendo el período y quitando días, y dejó dicho que eso está
**mal a medias**: la base de cotización del mes bajaba con los días de baja, y durante la baja se
sigue cotizando. El `backend#128` puso la base reguladora. Éste es el tercio que paga.

Lo que hay que modelar no es un concepto más: es una forma que el modelo no sabía. La prestación por
incapacidad temporal es **días × porcentaje × base reguladora**, con **tres** porcentajes que cambian
por número de día de baja, dos pagadores distintos, un complemento que pone el convenio encima, y una
base de cotización que aparece de la nada para que el mes no se hunda.

La norma, verificada artículo por artículo contra el BOE:

| días de baja | % | quién paga | norma |
|---|---|---|---|
| 1 – 3 | — | nadie | art. 173.1 LGSS: «el subsidio se abonará **a partir del cuarto día** de baja» |
| 4 – 15 | 60 | la empresa | art. 173.1 LGSS: «desde el día cuarto al decimoquinto de baja, ambos inclusive, el subsidio estará **a cargo del empresario**» |
| 16 – 20 | 60 | pago delegado | art. único RD 53/1980 (`BOE-A-1980-1003`): el 60 % «entre el cuarto día […] y hasta el veinteavo día, inclusive» |
| 21 → | 75 | pago delegado | art. 2.1 Decreto 3158/1966 (`BOE-A-1966-21116`): «un subsidio equivalente al setenta y cinco por ciento» |

Y el convenio de la demo **sí complementa**, que era la pregunta abierta del issue: **grandes
almacenes, `BOE-A-2023-13740`, art. 50**:

> «En los supuestos de baja por I.T., las personas trabajadoras […] percibirán un complemento sobre la
> prestación de la Seguridad Social **hasta alcanzar el 100 por 100 del Salario Base de Grupo**.»
>
> «[…] desde el primer proceso que se inicie dentro del año natural […] **no percibirán retribución ni
> complemento alguno durante los tres primeros días** […]»

## Decisión

### 1. Los tramos son una tabla, no un `if`

`payroll_engine.it_prestacion_tramo`: tipo de ausencia, código de tramo, día desde, día hasta,
porcentaje, quién paga, vigencia, **y la cita de la norma**. Es la misma decisión que el tipo de
desempleo (ADR-072) y los topes de cada ejercicio (`V153`), y por la misma razón: son cifras de una
norma, cambian sin que cambie el programa, y cada una tiene que poder llevar su cita al lado.

**Los tres primeros días no tienen fila.** Es la forma de decir que no se pagan: un tramo al 0 %
sería una línea de cero euros, y la regla del cero (`backend#104`) la quitaría igual. Sin fila, no hay
nada que quitar.

El día que entre el accidente de trabajo será **una fila más y no una clase más**: sus tramos son
otros —desde el día siguiente a la baja, al 75 %— y su base reguladora otra.

### 2. Tres tramos, dos líneas

| concepto | qué es |
|---|---|
| `D_IT_*` | cuántos días del tramo del período caen en ese tramo de la baja |
| `P_IT_*` | a qué porcentaje se paga ese tramo |
| `T_IT_*` | el importe **diario**: `PERCENTAGE(BR_CC, P_IT_*)` |
| `110` | días × importe diario del tramo de la empresa. **Se imprime** |
| `111_D60`, `111_D75` | lo mismo para los dos tramos de pago delegado |
| `111` | su suma. **Se imprime** |

Son **dos líneas y tres tramos**, y es deliberado: en el recibo, «prestación a cargo de la empresa» y
«prestación en pago delegado» son dos conceptos que un técnico de nóminas reconoce, y que el segundo
lleve dentro dos porcentajes es un detalle del cálculo — que es donde se ve, paso a paso.

Hay **un porcentaje por tramo** y no uno por valor, aunque hoy dos tramos valgan los dos el 60 %.
Compartirlo ataría el importe del segundo a la fila del primero, y el día que una norma cambie uno y
no el otro cambiaría el que no toca.

### 3. Los días se cuentan desde el inicio de la ausencia, y eso no es leer otro mes

Una baja que empezó el 25 de agosto llega al 1 de septiembre **con siete días detrás**, así que los
días de septiembre son del 8 al 37 de la baja: ocho en el tramo de la empresa, cinco en el primero de
pago delegado y diecisiete en el segundo. Tres tramos de prestación en un recibo.

Eso lo resolvió el `backend#127` llevando en el tramo los días ya transcurridos. **No es leer otro
mes** —eso es el `backend#128`, y es otra cosa—: es una fecha.

### 4. No cotizan, y eso ya estaba en el modelo

Las tres líneas —las dos de prestación y el complemento— **tributan y no cotizan**. Lo dice el art.
147.2.d) de la LGSS: «no se computarán en la base de cotización […] las prestaciones de la Seguridad
Social, **las mejoras de las prestaciones por incapacidad temporal concedidas por las empresas**».

Y **no hacía falta ninguna marca nueva**, que era la duda que el issue dejaba abierta: lo que decide
que un devengo cotice es que alimente a `B01`. Estas tres alimentan al `970` y a nadie más. Que
tributen lo decide que el `800` lea el `970` desde la `V146`. El modelo ya sabía distinguir un
devengo que no cotiza; lo que no tenía era uno.

### 5. La base durante la baja, y la invariante que deja de ser una comprobación

`B10 = días de baja × BR_CC`, y alimenta `B01`. Con eso la base del mes no baja con los días que el
`backend#127` quitó, y **no hacen falta bases por tramo** (el `backend#126` sigue siendo otro
problema).

Se cotiza **desde el primer día** de la baja, también los tres que no se pagan.

Y al meterlo hubo que **dar la vuelta al primer bloque del recuadro de bases**, porque el candado del
`backend#120` saltó: `B03` estaba escrito como `B01 − B04`, y alimentar además `B03` con `−B10` le
hacía llegar `B10` por dos caminos. No se le pone una excepción al candado. Se reescribe la conexión:

```
B03 = 101                    (la remuneración mensual ES el salario)
B04 = 103 + B02              (la prorrata, por la puerta que sea)
B01 = B03 + B04 + B10        (la base de cotización es la suma de los tres)
```

**La invariante del paso 5 deja de ser algo que hay que comprobar y pasa a ser la forma del grafo.**
`B01 = B03 + B04 + B10` porque eso es literalmente lo que le alimenta, y nadie más. Ni un importe se
mueve por el cambio; lo que cambia es que se puede leer.

### 6. El complemento del convenio, y su suelo

`112 = GREATEST(IT_100 − 110 − 111, 0)`, donde `IT_100` es el 100 % del salario base de grupo de los
días **con** prestación —los tres primeros no llevan complemento, art. 50 del convenio—.

El suelo es un concepto (`P_CERO`) y no un `Math.max` en Java, por lo mismo que `B_CC` se recorta con
`GREATEST(B_CC_MAX, P_TOPE_MIN)` desde la `V88`: **en este motor un límite es un concepto**, y «este
complemento no puede ser negativo» no puede ser una regla que sólo esté en el código.

Y el caso es real. La prestación es un porcentaje de la **base reguladora**, que lleva la prorrata
dentro y por tanto es mayor que el salario del día: con cuatro pagas extras, la base reguladora diaria
es `4/3` del precio del día, así que **el 75 % de la base reguladora es exactamente el 100 % del
salario base** y el complemento vale cero justo en el tramo del 75 %. Con una base reguladora que
venga de un mes con horas extra, sería negativa.

El resultado tiene una lectura que vale la pena escribir: **quien está de baja el mes entero devenga
exactamente lo que devengaría trabajando**, porque el convenio lo completa. El recibo lo demuestra
línea a línea, y eso es lo que este camino tenía que seguir haciendo.

### 7. El testigo de derecho es dato, no regla

La carencia —180 días cotizados en los cinco años anteriores, art. 172.a) LGSS— es la vida del
empleado **fuera de esta empresa**, y la decide el INSS. La nómina no la puede calcular y no lo
intenta: la ausencia `IT_COMMON` lleva un testigo **«con derecho a prestación»**, con derecho por
omisión, que pone quien registra la baja con la resolución delante.

Sin derecho: los días se quitan igual (`backend#127`), **ninguna** de las líneas aparece, **la base
durante la baja tampoco**, y el recibo lo dice. Las dos cosas van juntas y por eso son una sola
pregunta en el código.

Va en la ausencia y no en el empleado porque es de la baja: el mismo empleado puede tener una con
derecho y otra sin él.

## Consecuencias

- **Se mueven exactamente los recibos con baja por enfermedad común, y también los de permiso no
  retribuido** (por el `backend#127`). Los demás, ninguno: todos los conceptos nuevos valen cero sin
  baja, y un cero no se imprime.
- **`BR_CC` vale cero donde no hay baja con derecho**, así que «cuántos recibos tienen prestación» se
  contesta mirando el número. Es lo que hace comprobable el diferencial de la resiembra.
- **El catálogo pasa de 78 a 98 conceptos**, veinte más, dieciséis de tramo y cuatro de período. Son
  veinte y no cinco porque la prestación se escribe **tres veces, una por tramo**, y porque el
  complemento y su suelo también son conceptos. Lo que se compra es que todo lo que interviene en el
  importe se vea en el grafo, que es la tesis entera de este producto. `workspace#10` ya midió que el
  motor aguanta cien conceptos con holgura; lo que hay que seguir vigilando es el folio.
- **El recuadro de bases gana una línea y dos se corren.** `B01` pasa de orden 403 a 404 y `B_CC` de
  404 a 405. Es presentación: las líneas ya congeladas llevan su propio `display_order` y no se
  mueven (ADR-062).
- **La regla del cero hace todo el trabajo de la elegibilidad.** Los conceptos se asignan al convenio
  entero, como las dos puertas de la prorrata (ADR-070), porque `concept_assignment` no conoce al
  empleado. Un recibo sin baja tiene cuatro pasos más a cero y ni una línea más.
- **Queda fuera, y se dice.** El accidente de trabajo y el permiso de nacimiento siguen cobrando
  entero (ADR-073 §4). La paga de los tres primeros días que el convenio devuelve a fin de año si no
  hubo otro proceso necesita estado anual y no está. La excepción del art. 50 para empresas con
  «otro u otros sistemas más beneficiosos» no está: se aplica el sistema del convenio a todo el mundo.
  Y la liquidación con la Seguridad Social donde la empresa recupera el pago delegado es otra salida y
  no existe: hoy el `111` se paga y no se recupera en ningún sitio del sistema.
- **Y queda abierto que la prestación no reconoce el bloque de «percepciones no salariales» del modelo
  oficial.** Las tres líneas se imprimen entre los devengos, seguidas, como decidió el issue. Ese
  bloque es una subsección de `DEVENGOS` y la maquinaria existe (`V149`); ponerlo obliga a declarar
  también la subsección de las salariales, y eso es un issue de folio y no de cálculo.
