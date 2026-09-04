# ADR-057 — Una serie temporal se gobierna por sus invariantes, no por sus operaciones

## Estado
Aceptado

## Contexto

Un intento de cambiar la jornada de un empleado a mitad de mes devolvió un `409
WORKING_TIME_OVERLAP`. Al tirar del hilo apareció un segundo síntoma: una ocurrencia metida por
error no se puede deshacer en casi ninguna vertical. Se hizo el inventario de las quince verticales
de `employee`, controlador a controlador y *gateway* a *gateway*, y los dos síntomas resultaron ser
el mismo: **no hay una forma acordada de «una cosa del empleado que cambia con el tiempo»**. Cada
vertical se inventó su propio subconjunto de operaciones.

Agrupadas por comportamiento salen cinco tipos:

- **A — serie temporal con cierre explícito**: `contract`, `labor_classification`, `workcenter`,
  `cost_center`, `working_time`, `address`. Una vigente cada vez, con fecha de fin propia.
- **B — serie temporal con fin implícito**: `tax_information`. La vigencia acaba donde empieza la
  siguiente; sin cierre, sustituir *es* insertar.
- **C — colección atemporal**: `contact`, `identifier`, `payroll_input`. CRUD completo, sin deuda.
- **D — valor singular**: `employee`, `photo`.
- **E — derivada**: `journey`. Sólo lectura por construcción.

`presence` quedó fuera del tipo A: la escriben los flujos `HIRE`, `TERMINATE` y `REHIRE`, no el
usuario, y que su pantalla sólo lea es la forma correcta, no una carencia. El eje que la separa —
quién escribe: usuario o flujo de negocio — afecta también a la *creación* de `contract` y
`labor_classification`, que nacen al contratar.

Los tipos C y D están completos. **Toda la deuda vive en el tipo A**, y no está repartida al azar:

| | crear | sustituir desde fecha | corregir | cerrar | borrar |
|---|---|---|---|---|---|
| `contract` | sí | **sí** | sí | sí | no |
| `labor_classification` | sí | **sí** | sí | sí | no |
| `workcenter` | sí | **sí** (el front no lo llama) | sí | sí | sí |
| `cost_center` | sí | **sí** | no | sí | no |
| `working_time` | sí | **no** | sí | sí | no |
| `address` | sí | **no** | sí | sí | no |

La causa está en la columna del medio. `Replace…FromDate` se resolvió como **una operación**, y una
operación sólo obliga a quien pasa por ella: la vertical que no la implementó no se quedó sin un
método, se quedó **sin la regla**. `working_time` y `address` saben cerrar y saben crear, pero no
ofrecen hacerlo a la vez, así que el usuario tiene que ser la transacción, en el orden correcto —o
se come el solapamiento. `workcenter` tiene el remedio escrito y la pantalla no lo usa.

Y lo que el inventario no vio a la primera: **la abstracción ya existe a medias**. En
`employee/temporal/support` están `DateRange`, `TemporalDates`, `TimelineCoverageValidator` — con
`isContained` («ninguna ocurrencia fuera de la presencia») e `isFullyCovered` («sin huecos dentro de
ella») — y `StrongTimelineReplacePlanner`, cuyo `ReplaceMode {NO_COVERING, EXACT_START, SPLIT}` ya
contempla la inserción en medio. La usan `contract` y `labor_classification`. Nadie más.

## Decisión

### 1. El tipo A se define por sus invariantes, no por su juego de operaciones

Dos, y son separables:

- **Sin solapes.** Siempre, en las seis.
- **Sin huecos dentro del período de presencia.** Sólo en las de **cobertura obligatoria**: si el
  empleado está presente tiene que haber un contrato, una jornada y una clasificación. Una dirección
  se puede no tener, y un hueco ahí es legal.

Cada vertical declara si su cobertura es obligatoria. El planificador ya se llama `Strong`: la
variante débil estaba prevista y aquí se nombra.

Esto es el ADR-055 un paso más allá. Allí se decidió que la integridad de los códigos vive en la
aplicación y no en una clave ajena; aquí se decide que la integridad de una serie vive en **el
estado resultante**, no en la precondición de una operación. Un invariante sobre la serie es
comprobable en cualquier momento y por cualquier camino —una migración, un script, una importación—;
una regla escondida en una operación sólo protege a quien pasa por ella, que es exactamente cómo
llegamos aquí.

### 2. Una sola forma de escribir: añadir una ocurrencia

El usuario mete una ocurrencia con inicio y fin. **Una única consecuencia automática**, y acotada: si
la nueva empieza después de la última y esa última está abierta, se cierra el día anterior al inicio
de la nueva. Nada más se mueve solo.

Desaparecen `replace-from-date` y `close` como operaciones del API. Lo que hacían pasa a ser efecto
del invariante.

### 3. Estirar o encoger una ocurrencia lo hace el usuario

Nunca el sistema. De ahí sale la regla del borrado, que es donde estaba la pregunta difícil —poner
controles porque el dato es el núcleo, contra «me equivoqué, era día 4 y no día 3»:

- **Borrar la última** reabre la anterior. Es el «ups» y es seguro.
- **Borrar una de en medio** lo rechaza el invariante de huecos. Si de verdad hay que tapar el
  agujero, se estira una vecina, explícitamente.

La línea no está en prohibir el cambio: está en que **reescribir un histórico sea un acto
deliberado y no el efecto colateral de un borrado**.

### 4. El orden se deriva de la fecha de inicio y no se persiste

No hay número de ocurrencia que haga de orden. El `workingTimeNumber` y sus hermanos siguen siendo
identificadores. Un orden almacenado sería una segunda fuente de verdad capaz de contradecir a las
fechas, y entonces habría que decidir quién gana.

### 5. `Replace…FromDate` deja de ser el modelo

**No se escribe ninguno nuevo.** En particular, no se escribe `ReplaceWorkingTimeFromDate`: sería la
quinta copia de lo que este ADR sustituye. Las verticales sin el patrón se enganchan al componente
temporal; las cuatro que lo tienen se reconducen a él.

### 6. El cambio se planifica antes de aplicarse

`StrongTimelineReplacePlan` ya es un plan y no una ejecución. Se generaliza: el usuario mete lo que
quiera y ve qué le va a pasar a la línea temporal —qué se cierra, qué hueco aparece— antes de
confirmar. Es lo que permite que la escritura sea libre sin que la consecuencia sea una sorpresa.

## Consecuencias

**Seis verticales pasan a tener la misma forma**, y las pantallas dejan de necesitar saberse el orden
de dos llamadas. El `409` de la jornada desaparece por construcción, no por un arreglo.

**Cuesta reconducir cuatro verticales que ya están en el patrón viejo**, y sus *gateways* en el
front: `employee-contract-read`, `employee-labor-classification-read`, `employee-work-center` y
`employee-cost-center` llaman hoy a `replace…FromDate` o a `close`. Se hace ahora, antes de que haya
datos reales de clientes.

**Este ADR no decide tres cosas, y conviene que no se lea como si lo hiciera:**

1. **Qué pasa con las nóminas ya calculadas cuando se cambia un dato hacia atrás.** El
   `payroll_context_snapshot` deja saber qué jornada usó cada nómina, así que la trazabilidad está
   cubierta; lo que falta es que el cambio *avise* de qué nóminas quedan desactualizadas. Es el ciclo
   de nómina y tendrá su propia serie de ADRs.
2. **Si `address` es de cobertura obligatoria u opcional.** Aquí se asume opcional; es una decisión
   de negocio y hay que confirmarla.
3. **Cómo se crean las verticales que nacen de un flujo.** `contract` y `labor_classification` se
   crean al contratar, y este ADR no toca los flujos de ciclo de vida.

**Y deja a la vista tres cosas que no se callan aunque queden fuera:** `photo` está entera en el
backend sin un solo *gateway* en el front; `journey` tiene dos endpoints vivos a la vez (`/journey` y
`/journey-v2`); y `cost_center` deja sustituir una distribución pero no corregir una errata dentro,
lo que obliga a inventar un cambio histórico que nunca ocurrió.
