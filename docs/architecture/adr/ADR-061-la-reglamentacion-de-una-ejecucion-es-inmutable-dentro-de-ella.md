# ADR-061 — La reglamentación de una ejecución es inmutable dentro de ella

## Estado
Aceptado

## Contexto

La medida del `backend#82` dejó un número por unidad de cálculo: **425,7 sentencias**, de las cuales
**349 —el 82 %— eran el metamodelo del motor**: `payroll_object` 194, alimentaciones 71, operandos 63,
conceptos 21. Los 36 conceptos del sistema de reglas con sus operandos y sus alimentaciones se
preguntaban 349 veces por unidad y 873 veces seguidas: unas **304.000 lecturas por corrida para leer
36 filas**. La misma respuesta para la unidad 1 y para la 873.

Lo que hizo que esto pasara de ser una ineficiencia a ser una decisión de modelo no es el número, y
conviene decirlo en el orden correcto para que no se priorice mal:

1. **Una corrida de la plantilla dura minutos. Si alguien tocaba el grafo de conceptos a mitad, las
   primeras unidades y las últimas se calculaban con reglas distintas y nada lo decía.** Dos nóminas
   de la misma ejecución, con los mismos datos de entrada, podían dar resultados distintos. Eso no es
   lentitud: es que **el resultado dependía del reloj**.
2. Deja de leerse 304.000 veces lo que no cambia.
3. ~5,5× de propina.

El segundo y el tercer motivo son reales —la implementación midió **427,1 → 75,6 sentencias por
unidad**, 5,65×, y de 9m03s a 1m33s en local— pero son la consecuencia, no la razón. El primero es la
razón, y es el que este ADR fija.

Hasta aquí el metamodelo no era un concepto: era lo que salía de las consultas que cada trozo del
motor hacía por su cuenta. Nada lo nombraba, así que nada podía garantizar nada sobre él.

## Decisión

**La reglamentación contra la que se calcula una ejecución de nómina se carga una vez, entera, al
empezar la ejecución, y es la misma para todas sus unidades.**

Está reificada en `RuleSystemMetamodel` (`payroll_engine.metamodel.domain.model`): los conceptos de un
sistema de reglas con sus operandos, sus alimentaciones vigentes y sus asignaciones válidas.
`RuleSystemMetamodelRepository.load(ruleSystemCode, referenceDate)` la trae en cuatro consultas, las
cuatro con `join fetch` de `payroll_object`.

### 1. Eager, no perezoso — y esto es lo que no se puede cambiar

Una caché perezosa por concepto daría **casi todo el rendimiento** y **nada de la garantía**. Un
concepto que se usa por primera vez en la unidad 600 leería el grafo en el minuto tres de la corrida,
con el cambio ya dentro; el resultado seguiría dependiendo del instante en que a cada concepto le tocó
aparecer. La divergencia sería incluso más difícil de ver que antes, porque sería intermitente.

La carga completa por adelantado es lo que hace que la reglamentación **entre entera en la ejecución o
no entre**. Quien vea dentro de un año una carga completa de 36 conceptos y piense «esto se puede
hacer bajo demanda» tiene que encontrar aquí por qué no: **no se carga todo porque sea más rápido, se
carga todo porque cargarlo a trozos es cargar trozos de instantes distintos.**

Por la misma razón esto **no es una caché de segundo nivel de Hibernate**, y no debe convertirse en
una: aquello es estado de proceso con invalidación que hay que pensar. Aquí la vida de lo cargado es
**la ejecución**, que es una ventana con principio y final claros. Dos ejecuciones seguidas con un
cambio en el grafo en medio ven cada una la suya, porque cada una carga la suya.

### 2. El borde de «todo»: `ruleSystemCode` + fecha de fin del periodo

«Cargar todo» sin un borde crece hasta que no cabe. El borde es el que **la ejecución ya declara**: el
sistema de reglas de la corrida y la fecha contra la que se resuelven las vigencias, que es la fecha
de fin de su periodo. La reglamentación aplicable a `ESP` en `202609`, no todos los convenios de todos
los años.

Hoy da igual —36 conceptos— pero es lo que hace que la decisión siga siendo correcta cuando haya cinco
convenios con sus tablas por año. Y es un borde que ya existía en el modelo: no se ha inventado uno
para esto.

**No es la base entera** y **no es una caché de proceso**. Son las dos formas de perder el borde.

### 3. Se carga en `execute`, después de pasar a `RUNNING`. No en `requestLaunch`

`LaunchPayrollCalculationService.requestLaunch` valida el encargo, crea la ejecución en `REQUESTED` y
la encola (ADR-060). El trabajo lo hace `execute`, en el hilo del worker, y **la carga es su primera
instrucción después de poner la corrida en `RUNNING`**.

Esto tiene una consecuencia visible que nunca habíamos dicho y que este ADR declara correcta:

> **Dos ejecuciones pedidas a la vez y ejecutadas con diez minutos de diferencia ven reglamentaciones
> distintas.**

Con ADR-060 las ejecuciones se sirven de una en una y una corrida puede esperar en cola un buen rato.
Si en esa espera alguien cambia el grafo, la segunda calcula con el grafo nuevo. **Eso es lo que debe
pasar:** la reglamentación de una ejecución es la del instante en que **empieza a trabajar**, no la
del instante en que se pidió.

Cargarla en `requestLaunch` sería la otra respuesta —congelar las reglas en el momento de pulsar el
botón— y se rechaza por dos motivos. El primero es que `REQUESTED` no promete cálculo todavía: el
ADR-060 dice que una ejecución encolada puede morir sin arrancar, y una reglamentación cargada para
algo que no llegó a ocurrir es trabajo tirado. El segundo es que la garantía que hace falta es **una
ejecución, una reglamentación**, y esa se cumple igual cargando al arrancar; lo que se ganaría
congelando antes es la respuesta a una pregunta distinta —«¿con qué reglas se pidió?»— que hoy no se
hace nadie.

El recálculo puntual (`RecalculatePayrollService`) es también una ejecución, de una unidad: carga su
reglamentación y calcula contra ella. Por eso ve los cambios del grafo posteriores a la corrida que
produjo el recibo anterior, que es lo que se espera de un recálculo.

### 4. La regla vive en las firmas, no en un comentario

El motor **se queda sin repositorios**. `BuildEligibleExecutionPlanUseCase.build`, la expansión de
dependencias, el grafo y el plan ya no reciben un `LocalDate`: reciben el metamodelo, que lleva su
`referenceDate` dentro. `DefaultConceptEligibilityResolver` y `DefaultConceptDependencyGraphService`
se quedan sin estado. La semántica de comodín de `concept_assignment` —que vivía en el SQL— se muda al
dominio, incluida la parte sutil: una dimensión nula **en el contexto** significa desconocida y solo
casa con el comodín.

**Dos unidades de la misma ejecución no pueden planificarse contra fechas distintas porque el
parámetro con el que decirlo ya no existe.** Un javadoc que dice «no pases fechas distintas»
envejece; una firma que no acepta fechas, no. Es la misma forma que el supertipo sellado del
`backend#59`: la diferencia entre una regla que alguien tiene que respetar y una que no se puede
incumplir.

El metamodelo viaja a cada unidad dentro de `CalculatePayrollUnitCommand`, al lado de las fechas del
periodo. La unidad no lo busca: se lo dan.

## La excepción conocida: las tablas salariales quedan fuera

`payroll.payroll_table_row` y `payroll.payroll_object_binding` **no entran en el metamodelo**. Se leen
una vez por unidad —2 sentencias de las 75,6 que quedan— y se han dejado fuera a propósito.

**El motivo:** la **fila** que se lee depende de la categoría del empleado, o sea del caso, no de la
regla. Meter en la reglamentación de la ejecución algo que se elige por empleado sería confundir las
dos cosas que este ADR separa.

**Lo que eso deja sin cubrir, dicho con exactitud:** la fila es el caso, pero **la tabla es la regla**.
Así que la garantía que instala este ADR —«todas las unidades de una ejecución ven la misma
reglamentación»— **cubre hoy el grafo de conceptos y no cubre las tablas salariales**. Si alguien sube
las tablas del convenio a mitad de una corrida, dos unidades pueden cobrar con tablas distintas y nada
lo dice. Es el mismo defecto que este ADR vino a cerrar, en la mitad que no se ha cerrado.

Se registra aquí y no en un issue porque una decisión con una excepción conocida que vive en un
comentario es exactamente cómo se olvida la excepción.

**Por qué se acepta por ahora:** la ventana es estrecha —una corrida dura noventa segundos— y el
arreglo cae de suyo en el paseo a la parametrización de conceptos (`backend#61`, `backend#47`), donde
las tablas dejan de ser un caso aparte. Cuando eso ocurra, entran en el metamodelo por la puerta
grande y esta sección se borra con un ADR sucesor. Hasta entonces: **está escrito que no están
cubiertas.**

## Lo que se rechaza explícitamente

- **Una caché perezosa por concepto.** Punto 1: da el rendimiento y no da la garantía.
- **Una caché de segundo nivel de Hibernate**, o cualquier cosa cuya vida sea el proceso. Lo cargado
  muere con la ejecución.
- **Cargar la base entera.** El borde es sistema de reglas + fecha.
- **Cargar en `requestLaunch`.** Punto 3.
- **Paralelizar el cálculo.** `backend#84` quedó cerrado sin hacerse: con 349 sentencias por unidad
  menos, los hilos dejaron de hacer falta. Y seguirían chocando con los contadores de
  `CalculationRun`, que se incrementan leyendo-modificando-escribiendo (`backend#83`).
- **Tocar `GenerationType.IDENTITY` o `batch_size`.** El `backend#82` midió que el techo por ahí es el
  6 %.

## Consecuencias

- **El motor no lee de la base.** Elegibilidad, expansión, grafo, plan y el cálculo de los
  `DIRECT_AMOUNT` resuelven todo contra el metamodelo que reciben. Añadir una consulta a un componente
  del motor es, a partir de aquí, romper esta decisión: lo que haga falta se carga en la carga.
- **El `join fetch` de `payroll_object` es parte de la decisión, no un detalle.** Sin él, el mapeo a
  dominio volvería a pedir un select por objeto y la carga única no ahorraría nada: de ahí salían 194
  de las 349 lecturas por unidad.
- **Cambiar el grafo mientras corre una nómina ya no es peligroso, es inocuo.** Se aplica a la
  siguiente. Esto quita una regla operativa no escrita que nadie estaba respetando porque nadie sabía
  que existía.
- **Queda abierto —y no se hace aquí— que una ejecución diga cuál cargó.** Si la reglamentación es un
  objeto identificable, se puede registrar; y entonces «recalcular con las mismas reglas» y
  «recalcular con las de hoy» pasan a ser dos cosas distinguibles, que hoy no lo son. Eso toca el
  ADR-059 —el recálculo sustituye sin histórico— y va en la misma dirección que `deploy#4`. La forma
  elegida aquí lo deja posible; hacerlo es otro ADR.
- **Las tablas salariales siguen sin la garantía.** Ver la excepción.
