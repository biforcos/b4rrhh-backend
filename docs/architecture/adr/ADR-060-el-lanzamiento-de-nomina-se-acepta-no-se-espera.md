# ADR-060 — El lanzamiento de nómina se acepta, no se espera

## Estado
Aceptado

Sustituye la «primera iteración» del ADR-032 en lo único que este ADR toca: **la espera**. Todo lo
demás de aquel ADR sigue vigente palabra por palabra —launch coordina, calculate materializa, claim
excluye, run resume; la unidad mínima de cálculo; los contadores; los mensajes—. Aquel ADR rechazaba
explícitamente «introducir ya paralelización real o asincronía obligatoria», y lo rechazaba con razón:
no se sabía todavía si la semántica era la correcta. Ya se sabe.

## Contexto

La resiembra del `deploy#3` dejó el número: **871 recibos en 5 minutos y 19 segundos, cero errores**.
El motor funciona. Los **319 segundos** son el problema.

Cloudflare corta muy por debajo de eso, así que no es que el lanzamiento *pueda* agotar el tiempo:
**se agota siempre** para cualquiera que pulse el botón desde fuera de la red local. Y subir el límite
—aunque se pudiera— no arregla nada:

- Cinco minutos de rueda girando son indistinguibles de una aplicación colgada, y a los dos minutos
  alguien recarga y vuelve a lanzar.
- No se puede cancelar ni saber por dónde va.
- Cualquier corte —wifi, suspensión, despliegue— lo pierde entero.
- 5m19s son mil empleados. Un cliente real tiene diez mil.

Optimizar el motor tampoco: 0,37 s por nómina no es escandaloso, y al doble de velocidad seguirían
siendo dos minutos y medio.

Lo que hace que esto sea pequeño es que **la ejecución ya tiene identidad, estado e historia**:
`payroll.calculation_run` con sus ocho contadores (V55), `GET /payroll/calculation-runs/{runId}` y su
endpoint de mensajes, `calculation_claim` por unidad, el `run_id` en el recibo (`backend#62`), y cinco
estados declarados de los que `REQUESTED` no lo usaba nadie. Y `LaunchPayrollCalculationService` no
tiene ni un `@Transactional`: guarda los contadores **unidad por unidad**. El avance ya estaba
persistido y era observable. Lo único que impedía verlo era que la llamada HTTP tenía al cliente
esperando.

## Decisión

### 1. La API acepta el lanzamiento y devuelve la identidad de la ejecución

`POST /payroll/calculation-runs/launch` crea el `calculation_run` en **`REQUESTED`**, lo encola y
contesta **202** con el `runId`. El cálculo ocurre fuera del hilo de la petición.

`REQUESTED` significa exactamente lo que ya decía su nombre: «he recogido tu petición, aquí tienes el
asa». El estado ya estaba declarado; lo único nuevo es que ahora se ve.

**La validación del encargo sigue siendo síncrona.** Un período que no es `YYYYMM`, un
`targetSelection` incoherente o un código que no cabe se contestan con **400** y **no dejan ejecución
ninguna**. Un 202 es una promesa, y no se promete lo que ya se sabe que no se puede cumplir.

**No se añade `@Transactional` al servicio.** Parece lo correcto y rompe lo único que nos sirve: en
una sola transacción los contadores no se verían hasta el final, y el avance es el motivo de todo
esto.

### 2. Las ejecuciones se sirven de una en una

El trabajo lo hace **un pool de un solo hilo con cola acotada**.

**Un hilo**, porque dos corridas simultáneas se pelean por la misma base sin que nadie gane tiempo. El
`calculation_claim` protege los datos unidad por unidad, pero no dice nada de la cola; serializar las
ejecuciones es lo que hace que no tenga que decirlo. La regla operativa del ADR-032 —«dos launches
pueden coexistir, no pueden procesar la misma unidad»— sigue siendo cierta y sigue siendo la defensa
de integridad; esta decisión es más estricta que ella, no la contradice.

**Qué pasa, entonces, si llegan dos lanzamientos a la vez:** el segundo se queda en `REQUESTED`,
encolado y consultable, hasta que el primero termina. Una ejecución en `REQUESTED` con `startedAt`
vacío es, literalmente, «estás en la cola».

**Cola acotada** (`payroll.launch.execution.queue-capacity`, 16 por defecto), porque un botón pulsado
veinte veces no puede dejar veinte ejecuciones vivas. Lo que no cabe se rechaza, y **la ejecución
rechazada se cierra como `FAILED` con un mensaje `LAUNCH_REJECTED`**: aceptada y perdida es el peor
final posible, y la respuesta lleva siempre la identidad de la ejecución y su estado de verdad.

**El apagado no espera al trabajo en curso.** Un despliegue no puede colgarse cinco minutos esperando
a una nómina. La corrida a medias se queda en `RUNNING` y la cierra el arranque siguiente, que es el
punto 3.

### 3. Al arrancar, lo que esté «en marcha» está muerto

Al arrancar, el backend cierra como **`FAILED`** toda ejecución en `REQUESTED` o `RUNNING`, con un
mensaje `RUN_ABANDONED_ON_RESTART`, y **borra todas las reservas**.

**Por qué al arrancar y no con un reloj:** lo que distingue una ejecución muerta de una viva es el
proceso que la estaba calculando. Al arrancar no hay ninguno, así que cualquier cosa en `REQUESTED` o
`RUNNING` es de un proceso que ya no existe. No hay que adivinar cuántos minutos sin latir son
demasiados, y no hay ventana en la que una ejecución viva se confunda con una muerta.

**Por qué todas las reservas y no sólo las de esas ejecuciones:** al arrancar ninguna ejecución
sostiene un `claim`, así que lo que quede es huérfano por definición —incluido lo que dejara sin
limpiar una corrida que sí terminó, porque esa limpieza es *best-effort* y puede perderse.

**Y por qué esto es lo que más importa de todo el cambio.** Una ejecución que muere deja sus `claim`
puestos, y la siguiente **se salta a esos empleados contándolos en `total_skipped_already_claimed`**:
no vuelven a calcularse, nadie lee ese contador como aviso y el informe sale en verde. Es el fallo
silencioso de siempre. Síncronamente casi no importaba y estaba diferido a propósito, con el comentario
puesto en el código; asíncronamente es el modo de fallo principal.

**Los contadores de la ejecución cerrada no se tocan.** Dicen cuánto se había hecho de verdad antes
del corte, y eso es información. Lo que cambia es el estado: `FAILED` se lee como «no te fíes de
esto», y un `RUNNING` eterno no se lee de ninguna manera.

**Esto da por supuesta una sola instancia de API**, que es lo que levanta la pila de la demo (un único
contenedor `b4rrhh-demo-api`, con `container_name` fijado, que no se puede escalar). El día que haya
dos, el barrido de arranque de una mataría las ejecuciones de la otra, y entonces hace falta un *lease*
por instancia. Se dice aquí para que ese día se vea venir.

### 4. Una ejecución que nunca arrancó también se cierra

`chk_calculation_run_finished_requires_started` (V55) prohíbe una fecha de fin sin fecha de inicio. Una
ejecución puede morir **en la cola**: pedida, nunca arrancada. Al cerrarla se le pone como inicio **el
instante en que se pidió**, que es lo más cercano a la verdad que existe en la fila. Que no llegó a
arrancar lo dice su mensaje, no esa fecha. La restricción se queda como está: es sana, y el caso raro
se resuelve en el dominio, no relajando el esquema.

### 5. `requested_by` se rellena

Lo pone la capa web con el sujeto del token. Mientras el lanzamiento era síncrono daba igual —quien
lanzaba era quien esperaba—; con una ejecución que dura cinco minutos y sobrevive a su petición, no
saber quién la pidió es un problema el día que haya dos personas. No se valida contra el modelo de
usuarios: es traza, no identidad.

Los lanzamientos **en proceso** lo dejan vacío a propósito, porque ahí no hay nadie detrás.

### 6. El lanzamiento síncrono no desaparece: se queda dentro

El caso de uso expone dos entradas al **mismo** trabajo:

- `requestLaunch` — crea, encola y vuelve. Es la de la API.
- `launch` — hace el trabajo y devuelve el resultado. Es la de los escenarios y los tests de
  integración, que corren en su propia transacción y necesitan el resultado en la misma llamada.

No son dos implementaciones: es la misma función ejecutada en dos hilos distintos. Esto no es una
concesión a los tests, es lo que mantiene honesta la prueba del motor: un test que tuviera que esperar
a un hilo de fondo para afirmar sobre los contadores sería un test intermitente.

## Lo que se rechaza explícitamente

- **Subir el tiempo de espera.** No hay límite que haga aceptable una rueda girando cinco minutos.
- **Trocearlo en el cliente** (mil peticiones de un empleado): mueve el problema al navegador y deja
  el lanzamiento sin identidad ni resultado consultable.
- **Inventar estados.** Hay cinco declarados; este ADR usa los cinco y no añade ninguno.
- **Equiparar `COMPLETED` con «han cobrado todos».** La corrida del `deploy#3` acabó `COMPLETED` con
  dos unidades saltadas. Los saltados no son errores y no mueven el estado (ADR-059).
- **Un `@Transactional` en el servicio de lanzamiento.** Ver el punto 1.
- **Paralelizar el cálculo.** No es lo que hace falta: lo que hace falta es verlo avanzar y poder
  volver a mirar.

## Consecuencias

- **El frontend tiene que cambiar.** `frontend#61` construyó la pantalla de una ejecución con sus
  contadores y sus mensajes; lo que falta es la parte viva: que el botón de lanzar lleve allí en vez
  de esperar, y que la pantalla se refresque mientras la ejecución corre. Su forma dependía de qué
  devolviera el lanzamiento, y ya se sabe: un 202 con `runId` y estado `REQUESTED`.
- **El cliente generado cambia de código de respuesta.** El contrato que lee el frontend decía 201.
  Un cliente que trate el 202 como error deja la pantalla sin llegar nunca, así que los dos contratos
  lo dicen y el test de contrato lo fija.
- **`COMPLETED` puede tardar en llegar, y eso es normal.** Quien consulte una ejecución tiene que
  contar con `REQUESTED` y `RUNNING` como estados frecuentes, no excepcionales.
- **Una ejecución `FAILED` ya no significa siempre «falló el cálculo»**: puede ser un backend que se
  reinició o una cola llena. El `calculation_run_message` es el que lo distingue, y es otra razón más
  para que la pantalla los enseñe.
- **Cancelar sigue sin existir.** Ahora es posible —hay identidad, hay estado y el trabajo vive
  fuera— pero no está hecho. Hace falta un acto explícito y probablemente un sexto estado; este ADR
  no lo abre.
