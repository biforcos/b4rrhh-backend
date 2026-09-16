# ADR-065 — Primero el que llegue, y el que pierde lo dice

## Estado
Aceptado

## Contexto

Dos caminos calculan el mismo recibo: el lanzamiento masivo —`POST /payroll/calculation-runs/launch`,
mil unidades— y el recálculo puntual —`POST /payrolls/…/recalculate`, una—. Desde el `frontend#70`
recalcular es **la acción principal del recibo**, un clic; desde el `backend#99` abre su propia
ejecución; y el paso 7 del recorrido de la demo es tocar una regla y recalcular. Con eso, «alguien
recalcula un recibo mientras corre la nómina de la plantilla» dejó de ser una rareza.

`payroll.calculation_claim` existía desde el `backend#75` para impedir exactamente eso, con una
clave única sobre la clave de negocio de seis columnas (`uk_calculation_claim_business`). **La tomaba
el lanzamiento y no la tomaba el recálculo**, así que la reserva se tomaba y no servía de nada: el
otro camino no la consultaba.

Lo que sí estaba medido antes de decidir nada (`backend#101`, seis rondas con dos hilos y una
barrera, fuera de transacción y sobre el esquema real):

- **Coinciden siempre.** Las seis rondas con solape de reloj.
- **No pueden mezclar.** Un cálculo **reemplaza** el recibo entero en vez de editarlo:
  `CalculatePayrollService` borra la fila anterior e inserta una nueva con id nuevo, los pasos
  cuelgan de ese id con `on delete cascade`, y `uk_payroll_business` admite una fila y sólo una. Las
  líneas y los pasos de un recibo nacen las dos contra el id que ese cálculo acaba de crear. No hay
  ningún camino que edite un recibo en su sitio.
- **El que perdía, perdía por donde no era.** La corrida masiva apuntaba la unidad como
  `UNIT_CALCULATION_ERROR` y terminaba `COMPLETED_WITH_ERRORS`. Ahí no falló ningún cálculo: la
  unidad estaba cogida.

## Decisión

**Primero el que llegue, y el que pierde lo dice.**

1. **La reserva de `calculation_claim` la toman los dos caminos**, y es lo único que decide quién
   llegó antes. No hay otro árbitro.
2. **El que no la consigue no espera: falla rápido y con nombre.** El recálculo contesta `409` con
   `UNIT_ALREADY_CLAIMED`, el mismo código que la corrida masiva escribe en el mensaje de la unidad.
3. **Perder la reserva no es un error de cálculo.** La corrida que encuentra la unidad cogida la
   cuenta en `totalSkippedAlreadyClaimed` y no en `totalErrors`, así que una corrida que sólo pierde
   unidades por reserva **no termina en rojo**.

### Por qué no hay prioridades

La alternativa evidente —que el recálculo puntual mande sobre la corrida masiva, por ser interactivo—
se descarta, y no por gusto:

- **Obliga a inventar un concepto que el modelo no tiene.** No existe «prioridad» en ninguna parte:
  ni en `calculation_run`, ni en `calculation_claim`, ni en el metamodelo del motor. Habría que
  crearlo, persistirlo y decidir quién lo asigna, para gobernar un caso que dura segundos.
- **Y obliga a contestar una pregunta que no tiene respuesta barata: «¿y si el prioritario se
  cuelga?»**. Una reserva que alguien puede arrebatar necesita saber cuándo arrebatarla —un tiempo de
  gracia, un latido, una recuperación—, y eso es una máquina nueva con sus propios modos de fallo.
  Hoy la única recuperación que hay es la del arranque (`deleteAll`, `backend#75`), que se apoya en
  que al arrancar no hay ninguna ejecución viva; con expropiación, esa premisa se cae.

«Primero el que llegue» no necesita nada nuevo, y **las dos direcciones quedan explicables**, que era
la condición. Tampoco se eligió hacer esperar al recálculo: un botón principal que se queda pensando
cinco minutos porque alguien lanzó la plantilla es peor que un «ahora no, inténtalo en un momento».

### La asimetría, que es lo que no se deduce leyendo la regla

**Leída la regla, cualquiera espera que los dos caminos fallen igual de rápido. No es así, y la
diferencia está medida.**

La reserva del recálculo se toma **dentro de su transacción** (`RecalculatePayrollService` es
`@Transactional`), así que esa fila **no se ve desde fuera hasta que confirma**. La del lanzamiento no:
el bucle de unidades es deliberadamente no transaccional —los contadores se guardan unidad por unidad
para que la ejecución se vea avanzar—, así que cada reserva suya queda confirmada en el acto.

De ahí salen dos comportamientos distintos, y ninguno es un defecto:

| quién llega segundo | qué le pasa |
|---|---|
| **el recálculo** | La reserva del lanzamiento ya está confirmada: choca con la clave única **en el acto** y sale con su `409`. |
| **la corrida masiva** | La reserva del recálculo está sin confirmar: PostgreSQL **la hace esperar en su propio `insert`** hasta que aquella transacción termina. |

Y lo que pasa cuando se suelta no es lo que uno supondría: el recálculo **borra su reserva antes de
confirmar**, así que la corrida masiva **consigue la reserva** —y se encuentra el recibo ya
`CALCULATED`, porque quien la tenía acaba de calcularlo—. Por eso las seis rondas medidas salieron
con `reservados=1` **y** `yaReservados=1` a la vez: la misma unidad pasó por las dos cosas.

Tres consecuencias que hay que tener presentes y que no se ven en el código de ninguno de los dos:

1. **La corrida masiva espera lo que tarda una unidad, no una nómina.** Un recálculo es un cálculo.
   La política se cumple igual —nadie se queda detrás de mil empleados— pero se cumple por el lado
   que no se había mirado.
2. **`UNIT_ALREADY_CLAIMED` se emite por dos caminos distintos**: cuando la reserva no se consigue, y
   cuando se consigue y el recibo aparece ya calculado. El suceso es el mismo —la unidad estaba
   cogida— y por eso el código es el mismo. Quitar el segundo camino devuelve el defecto original:
   la unidad vuelve a contarse como error de cálculo.
3. **El cerrojo `for update` de la presencia no es lo que impide la mezcla.** Se midió quitándolo:
   sin él tampoco hubo recibos mezclados —eso lo sostienen el reemplazo entero, el `cascade` y
   `uk_payroll_business`—. Lo que el cerrojo sostiene es que el fallo sea **legible**: con él, una
   excepción de negocio; sin él, un `ObjectOptimisticLockingFailureException` con un nombre de
   entidad dentro y disfrazado de «el cálculo falló».

## Consecuencias

- **Un tercer camino que calcule debe tomar la reserva.** No hay defensa pasiva: quien no la
  consulta, no la ve. Fue el defecto original y sería el mismo otra vez.
- **La reserva del recálculo no sobrevive a nadie**, porque vive dentro de su transacción: gane o
  pierda, desaparece con ella. No necesita la recuperación de arranque que sí necesitan las del
  lanzamiento.
- **La invalidación no toma reserva y no hace falta que la tome.** Invalidar exige `CALCULATED` y
  recalcular exige `NOT_VALID`: los dos escritores necesitan estados opuestos, así que en cualquier
  instante sólo uno es admisible. Está medido saliendo de los dos estados. Si algún día un camino
  admitiera los dos estados, este argumento se cae y ese camino necesita reserva.
- **El argumento de «no se puede mezclar» es estructural, no estadístico.** Seis rondas no demuestran
  nada; lo que lo sostiene es el reemplazo entero más `cascade` más la clave única. El día que un
  camino escriba un recibo **modificándolo** en vez de reemplazándolo, esta decisión deja de cubrirlo
  y hay que volver a mirarla entera.

## Fuera de alcance

Dos corridas masivas a la vez: eso lo gobierna la cola del lanzamiento —`PayrollLaunchWorkerPort`, un
hilo y de una en una—, que es anterior a esta decisión y no la toca.

Y la espera del `insert` como mecanismo de coordinación: aquí se documenta porque es lo que ocurre,
no porque se haya elegido. Si algún día molesta —una unidad que tarde de verdad, un lote que no pueda
detenerse— la salida no es reordenar transacciones a ojo, sino que el lanzamiento pida la reserva sin
bloquearse y trate «ocupada» como lo que ya sabe tratar: una unidad cogida.
