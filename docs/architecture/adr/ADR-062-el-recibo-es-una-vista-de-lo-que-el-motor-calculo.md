# ADR-062 — El recibo es una vista de lo que el motor calculó, no lo que el motor calculó

## Estado
Aceptado

## Contexto

El motor calcula 36 conceptos y sólo 14 llegaban a la base. `CalculatePayrollUnitService` descartaba
todo el que no tuviera `payslip_order_code`:

```java
if (engineConcept.getPayslipOrderCode() == null) {
    return;
}
```

Los 22 que se tiraban son **5 `BASE` y 17 `TECHNICAL`**: la base de cotización, el tope y el suelo,
el precio por día, los días devengados, el coeficiente de jornada, los tipos de cotización y de
IRPF. O sea, **exactamente los que explican de dónde sale el número**. Una pantalla que tuviera que
enseñar por qué el líquido es el que es pondría 14 valores sobre 36 nodos, con los huecos justo
donde está la explicación.

El problema no era el filtro, que para el folio es correcto. El problema es que **el filtro decidía
lo que se persiste**, y eso confunde dos cosas distintas: lo que el motor hizo, y lo que el folio
enseña.

## Decisión

**Lo que el motor calcula se guarda entero, en `payroll.payroll_calculation_step`. Las líneas del
recibo se derivan de ahí.**

`payroll.payroll_concept` no cambia: sigue significando *línea de recibo*, con su `line_number`, su
`display_order` y su `quantity` de presentación.

### 1. Una travesía y una proyección

Ésta es la parte que hay que respetar, y el motivo por el que este ADR existe:

```java
List<ConceptRow> payslipRows = calculationSteps.stream()
        .filter(PayrollCalculationStep::isPayslipLine)
        .map(this::toPayslipRow)
        .collect(...);
```

El recorrido del plan arma **el conjunto completo**. El recibo sale después, filtrando. No hay dos
construcciones en paralelo, así que **no hay nada que pueda divergir**: la pregunta «¿y si mañana
alguien toca un camino de escritura y no el otro?» deja de tener sentido porque no hay dos caminos.

Y las dos tablas **no son copias, y nunca hay que probar que lo sean**. El `quantity` de una línea de
recibo es una decisión de presentación —la CANTIDAD de un `RATE_BY_QUANTITY`, la BASE de un
`PERCENTAGE`—, no el bruto del paso. Un test de igualdad entre las dos habría fallado con razón, y
el arreglo habría sido relajarlo hasta que no comprobara nada. Con la derivación, esa regla de
presentación vive en `toPayslipRow`, en un sitio y a la vista.

### 2. La identidad es `(payroll_id, execution_order)`, y no el concepto

**El caso que decide la clave existe en la semilla:** un empleado con el mes partido tiene
`SALARIO_BASE` dos veces, con dos precios, en dos segmentos. Cualquier identidad que no distinga los
dos tramos se come una fila **en silencio** — que es exactamente el fallo por el que no se ensanchó
`payroll_concept`: `PayrollConceptEntity` compara por `(payroll, line_number)` y el agregado los
guarda en un `LinkedHashSet`, así que con `line_number` nulo los 22 son iguales entre sí y se queda
uno.

El orden de ejecución resuelve dos cosas con una columna: es una clave que nunca se repite y es, a la
vez, **en qué orden hizo el motor lo que hizo**, que es la diferencia entre «aquí tienes 36 valores»
y «esto es lo que hizo, en este orden».

### 3. `execution_scope` explícito, y el bicondicional en el esquema

El ámbito es una columna, no algo que se deduzca de que las fechas de segmento vengan nulas: no se
codifica un hecho en la ausencia de otro. Y como las dos cosas tienen que decir lo mismo, **las dos
fechas son nulas si y sólo si el ámbito es `PERIOD`** es un invariante de la fila y lleva su `check`,
además del mismo invariante en el constructor compacto de `PayrollCalculationStep`.

### 4. El puerto de escritura no tiene lectura

`PayrollCalculationStepWritePort` sólo escribe. Los pasos todavía no los sirve nadie, y un
`findByPayrollId` devolvería lista vacía cuando nadie lo hubiera llamado nunca: «vacío» no se
distingue de «este recibo no tiene pasos». Es la forma que este proyecto lleva quitando —
`WorkCenterProfile.empty()`, el `company_profile`, el `ofDefault()` fiscal (`backend#92`)—, y aquí se
evita **por construcción**: que el código diga «esto no se lee todavía» no teniendo por dónde leerlo
vale más que un comentario. El puerto de lectura se añade el día que haya un consumidor delante.

### 5. La tabla cuelga de un recibo, no de una ejecución

`payroll.calculation_run` y `calculation_run_message` ya usan el prefijo `calculation_*` para cosas
de **la ejecución**. Ésta no. Una fila de aquí es un paso que el motor dio calculando **un recibo**, y
muere con él por `on delete cascade`. Lo dice la cabecera de la `V129` y un `comment on table`, para
quien haga `\d` en vez de abrir el repositorio.

Lo que va **por ejecución** —el grafo, qué alimenta a qué— no cabe aquí: eso es la regla y no el
resultado (ADR-061), y su forma está sin decidir.

## La trampa del `insert` diferido, que es lo que hay que leer antes de añadir otra tabla así

`payroll_calculation_step` es **la única entidad de `payroll` con clave asignada**. Todas las demás
—`payroll`, `payroll_concept`, `payroll_warning`, `payroll_context_snapshot`, `payroll_segment`,
`calculation_run`, `calculation_claim`, `calculation_run_message`— llevan
`@GeneratedValue(IDENTITY)`, y eso tiene una consecuencia que no es evidente:

> **Con `IDENTITY`, Hibernate ejecuta el `insert` al persistir**, porque necesita el id que genera la
> base. **Con clave asignada no lo necesita, así que encola la inserción hasta que se vacíe la
> sesión.**

Durante años todas las escrituras de este contexto han sido inmediatas. La primera que no lo es
cambia lo que se puede observar, y costó un diagnóstico equivocado:

- El test de integración de recálculo **leía cero pasos** en el recibo nuevo y el diagnóstico
  publicado fue «en el recálculo puntual los pasos no llegan a la base». **Era falso.**
- `@TestWebSobreEsquemaReal` es `@Transactional` y **deshace la transacción al terminar**. Con
  instrumentación —`TransactionSynchronizationManager`— se ve que nunca hubo `beforeCommit` y que
  `afterCompletion` llega con `status=1`, `STATUS_ROLLED_BACK`. **No es que el commit no vaciara la
  sesión: es que no había commit.**
- Dentro de esa transacción, un `JdbcTemplate` no ve lo que sigue encolado en la sesión de Hibernate.
  En el camino del lanzamiento algo posterior vaciaba la sesión por su cuenta y las filas aparecían;
  en el del recálculo no quedaba nada detrás que lo hiciera, y no aparecían. **La diferencia entre
  los dos tests era casualidad, no comportamiento.**
- Comprobado contra **la aplicación arrancada** y contando por `psql` desde fuera, sin `flush` en el
  adaptador: **35 pasos por los dos caminos**, lanzamiento y recálculo puntual, y cero huérfanos.

De ahí **tres reglas**:

1. **Un `flush()` en el adaptador no es lo que hace duradera la escritura.** El commit la hace. Un
   `flush` sólo adelanta el momento, y puesto «para que funcione» es un arreglo que tapa el
   diagnóstico en vez de darlo. No está.
2. **El vaciado va en el test, donde está el motivo**, y dicho: el test vive en una transacción que no
   hace commit, así que si va a leer por JDBC lo que acaba de escribir por JPA, vacía la sesión él.
   Sin eso, el test depende de un vaciado incidental, que es una forma de test verde que no prueba lo
   que cree.
3. **Un test que hace rollback prueba lo que la base hizo, no lo que quedó en ella.** La distinción
   importa y no es la misma para todo: el borrado en cascada **sí** se observa ahí, porque el `delete`
   se ejecuta de verdad dentro de la transacción y Postgres aplica la cascada; lo que no se observa es
   la durabilidad, ni nada que dependa del commit. Para eso hace falta un commit de verdad, y
   `@TestWebSobreEsquemaReal` dice explícitamente que ése no es su caso. Aquí la durabilidad la cubre
   la corrida completa contra la aplicación arrancada: 30575 pasos comprometidos y cero huérfanos.

## Lo que se rechaza explícitamente

- **Ensanchar `payroll_concept` con los 22.** Está roto por construcción: el `LinkedHashSet` del
  agregado se queda con una de las 22 filas y las otras 21 desaparecen antes de llegar a la base, sin
  excepción, sin registro, con el índice único sin verlas y el test verde cubriendo nada.
- **Guardar sólo los 22 que faltaban.** Obligaría a unir dos poblaciones cada vez que alguien quiera
  saber qué calculó el motor, y una unión mal hecha es un número mal en una pantalla. Con el conjunto
  completo, «qué hizo el motor con esta persona este mes» es una consulta a una tabla. El precio —los
  14 están en los dos sitios— lo paga la derivación.
- **Un test que compare las dos tablas campo a campo.** No dicen lo mismo y no deben.
- **Un `flush()` en el adaptador.** Ver arriba.
- **Guardar aquí el grafo.** Va por ejecución (ADR-061), no por recibo.
- **Migrar los recibos viejos.** Esto vale hacia adelante; la semilla se vuelve a capturar.

## Consecuencias

- **El recibo de hoy sale idéntico.** Comprobado contra la base con la semilla de la demo entera:
  873 recibos de 202609 recalculados con el código nuevo y comparados por clave de negocio con los
  que trae la semilla, calculados con el anterior. **12227 líneas y diff vacío.** Al derivar el recibo
  del conjunto completo, ese diff es lo que prueba que la proyección es exactamente la construcción
  que había.
- **`CalculatePayrollUnitService` pasa a ser `@Transactional`.** El recibo y sus pasos son dos
  escrituras y tienen que ir o no ir juntas; el lanzador no abre transacción a propósito, así que sin
  esto la segunda caería en una transacción distinta.
- **Un recálculo no puede dejar un recibo nuevo con pasos viejos**, y está comprobado contando filas
  en las dos tablas, no supuesto: el recálculo borra el recibo anterior y crea uno nuevo, y la
  cascada se lleva sus pasos. Son dos mecanismos distintos para el mismo ciclo de vida —`orphanRemoval`
  desde el agregado para las líneas, la clave ajena para los pasos— y por eso hay que comprobar los
  dos.
- **Los conceptos del catálogo no son los conceptos de un plan.** Contarlos reveló que de los 36 de
  ESP sólo 35 entran en algún plan: `P_SS` (`TIPO_SS`) quedó huérfano en la `V91`, cuando el
  `PERCENTAGE` del 700 pasó de leerlo a él a leer `P_SS_CC`. No está asignado, no alimenta y no es operando de
  nadie. El test lo fija para que el recuento no siga diciendo algo que dejó de ser verdad.
- **Queda abierto —y no se hace aquí— servir los pasos y pintarlos.** El contrato no cambia en esta
  decisión. Cuando haya consumidor, el puerto de lectura y el endpoint son otro issue y probablemente
  otro ADR.
