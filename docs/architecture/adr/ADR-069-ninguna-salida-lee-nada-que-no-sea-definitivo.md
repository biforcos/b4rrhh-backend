# ADR-069 — Ninguna salida lee nada que no sea definitivo

## Estado
Aceptado

## Contexto

Un mes de nómina es un ciclo con un punto de no retorno: se lanza varias veces, se revisa, se
invalida y se recalcula, y un día se **cierra**. Cerrar es llevar los recibos a `DEFINITIVE`, y eso
es la foto de la que derivan todas las salidas — el recibo al empleado, las transferencias, los
seguros sociales, la tributación, la contabilidad. El ciclo entero está escrito en el `CICLO.md` de
la raíz del workspace; aquí está la regla que lo sostiene.

**El período no tiene estado.** Lo tiene cada recibo, y el mes es lo que sus recibos dicen (ADR-059:
el motor decide si un recibo es válido, las personas deciden si está cerrado). No hay nada que se
abra y se cierre, así que no hay nada que una salida pueda preguntar para saber si puede leer. Lo
único que hay es el estado de cada recibo, uno a uno.

Eso deja una puerta abierta que hoy no se nota porque sólo hay una salida. El día que haya cuatro
más, cada una va a tener que decidir por su cuenta qué recibos lee, y la forma natural de escribir
una remesa de transferencias es «todos los del período» — que incluye los `CALCULATED` de un mes que
aún se está revisando y los `NOT_VALID` que nadie ha arreglado. El resultado no sería un error: sería
una transferencia hecha con un número provisional, indistinguible de una buena hasta que alguien la
concilie.

Y hay un caso que se resuelve solo si la regla existe, y que sin ella hay que resolver a mano en cada
salida: **un recibo `NOT_VALID` el día del cierre no se paga, no se cotiza y no se declara.** Cobrará
el mes siguiente como atraso. Con la regla, eso no es un caso especial de nada; es lo que pasa por no
estar en la foto.

El ADR-062 ya dijo lo suyo para dentro: *el recibo es una vista de lo que el motor calculó*, y quien
quiera saber qué hizo el motor lee los pasos, no el folio. Esto es lo mismo mirando hacia fuera.

## Decisión

**Ninguna salida lee nada que no sea `DEFINITIVE`.**

Ni inválido, ni calculado, ni validado. Recibos, transferencias, seguros, tributación, contabilidad:
leen la foto fija o no leen.

### 1. Qué es una salida, y qué no

Una **salida** es algo que pone un dato de nómina en manos de alguien de fuera: un documento
entregado, un fichero, una remesa, una declaración, un asiento.

**Enseñar no es entregar.** La pantalla puede pintar un recibo `CALCULATED` —lo hace, y lo marca
*borrador*— porque mirarlo no lo entrega. La distinción no es de intención sino mecánica, y está en
el código desde el `backend#112`:

- un `DEFINITIVE` **se sirve desde el almacén**, tal cual se guardó al cerrar, y pedirlo dos veces
  devuelve los mismos bytes porque es el mismo objeto;
- cualquier otro estado **se dibuja al vuelo, va marcado y no se guarda**.

Lo que la regla prohíbe es que un no-definitivo salga. Que se vea, no.

### 2. Se filtra antes de leer, y en un sitio que se pueda encontrar

Una salida nueva filtra por `DEFINITIVE` **antes** de traerse nada, no después de traérselo todo. La
diferencia importa el día que alguien mire la consulta en la base: `where status = 'DEFINITIVE'` dice
la regla; traerse el período entero y descartar en Java la esconde donde nadie la busca.

Y se filtra **donde un test pueda verlo**: en el método que obtiene los recibos, no tres llamadas más
abajo.

### 3. Cada salida es un vertical bajo `com.b4rrhh.payroll`

`payroll/document/` es el primero y hoy el único. Transferencias, seguros, tributación y
contabilidad serán `payroll/<lo que sean>/`, con sus capas, como cualquier otro vertical.

Esto no es orden por el orden: es lo que hace que la regla sea comprobable sin anotar nada. El
candado del punto siguiente descubre las salidas por dónde viven, así que una salida nueva entra en
la vigilancia el día que se crea su carpeta, sin que nadie se acuerde de registrarla.

### 4. El candado

`NoOutputReadsAnythingButDefinitiveTest`, al estilo del
`EveryPathToDefinitiveArchivesItsDocumentTest` y por el mismo motivo: lo que hay que defender no es
que la salida de hoy filtre —eso lo prueban sus propios tests— sino que **la segunda, cuando la haya,
no pueda no filtrar**. Un test de comportamiento no ve un camino que todavía no existe; el escaneo
sí, y falla en el commit que lo añade.

La pregunta que hace es una sola: cada método de una salida que se trae recibos, ¿nombra
`PayrollStatus.DEFINITIVE`? Hoy hay una salida, un método que lee, y pasa.

## Consecuencias

- **Hoy no cambia nada.** No se toca código de producto: la única salida ya cumple la regla, que es
  precisamente por qué se puede escribir ahora y no dentro de seis meses con cuatro salidas que
  arreglar.
- **El test es el que va a molestar, y para eso está.** La primera salida nueva va a salir roja hasta
  que filtre. Eso no es fricción: es el ADR haciendo su trabajo en el único momento en que sirve de
  algo.
- **Un `NOT_VALID` el día del cierre deja de ser un caso.** No sale por ninguna salida porque no está
  en la foto, y lo de ese mes le llega como atraso. Es el caso de atrasos más limpio que va a haber,
  y esta regla es la que lo hace trivial.
- **Las salidas no necesitan ponerse de acuerdo entre ellas.** Cuatro equipos escribiendo cuatro
  salidas leen el mismo conjunto de recibos sin coordinarse, porque el conjunto está definido por el
  estado y no por una fecha, un lote ni un «cierre de período» que alguien tenga que declarar.
- **La pantalla no queda atada.** Puede seguir enseñando borradores, que es su trabajo, y eso está
  dicho aquí para que nadie lo «arregle» leyendo la regla de más.
- **Queda abierto qué pasa cuando haya que rectificar algo entregado.** La respuesta del `CICLO.md`
  es que va hacia adelante, como atraso, y esa mecánica no está diseñada. Lo que este ADR fija es que
  la corrección **no** puede ser volver a abrir la foto.
- **El candado mira nombres, no bytecode**, como los demás tests de arquitectura de este repositorio.
  Si algún día una salida se trae los recibos por un camino que no se llama como los de hoy, el test
  dejará de verla: es la limitación conocida, y el precio de no depender de ArchUnit.
