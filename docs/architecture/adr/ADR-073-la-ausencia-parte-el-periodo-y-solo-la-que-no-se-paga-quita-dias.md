# ADR-073 — La ausencia parte el período, y sólo la que no se paga quita días

## Estado
Aceptado

## Contexto

`employee.employee_absence` existe desde la `V100`, con tipo y fechas, y **no llegaba al motor**. La
semilla de la demo tiene 6.685 ausencias; 85 bajas por enfermedad común y 14 excedencias sin sueldo
tocan `202609`, y **ninguna movía un céntimo**: `EMP000009`, de vacaciones del 15 al 25 de
septiembre, cobraba 30 días, y `EMP000008`, con un día de baja, también.

El ADR-068 dejó dicho que el período lo parte **la unión de los puntos de cambio de las verticales
que afectan al cálculo**, y dejó la lista en su mínimo: jornada, clasificación laboral y contrato. El
`backend#118` añadió el régimen de pagas extras. La ausencia es la quinta causa, y es la primera que
**no es una vigencia sino un hueco**: lo que cambia dentro de su tramo no es a qué precio se paga el
día, es que el día no se paga.

Y hay una tentación evidente que hay que descartar por escrito: partir por **cualquier** ausencia.
Es más simple de escribir —una lista menos que mantener— y es peor. Unas vacaciones no cambian nada
de lo que se calcula, así que su tramo sería un paso de cálculo más por concepto de ámbito `SEGMENT`,
con el mismo importe que el tramo de al lado, que el folio vuelve a fundir. Pagaríamos rastro de
cálculo a cambio de nada, y el rastro es justo lo que este producto tiene que mantener legible
(`workspace#10`: el límite legible ya se cruzó).

## Decisión

**Parte el período la ausencia que cambia lo que se paga, y en su tramo no se devengan días.**

Hoy son dos: `IT_COMMON` —la baja por enfermedad común— y `UNPAID_LEAVE` —el permiso no retribuido—.

### 1. Qué hace cada tipo de ausencia, y por qué

| tipo | ¿parte? | ¿quita días? | por qué |
|---|---|---|---|
| `IT_COMMON` | sí | sí | el salario deja de devengarse y aparece una prestación (`backend#129`) |
| `UNPAID_LEAVE` | sí | sí | el caso más simple del mismo mecanismo: días que no se pagan y punto |
| `VACATION` | no | no | se cobran; un tramo para ellas no cambiaría ningún número |
| `PAID_PERSONAL_LEAVE` | no | no | igual: es retribuido, es lo que dice su nombre |
| `FORCE_MAJEURE` | no | no | igual |
| `IT_WORK_ACCIDENT` | no | no | **alcance de la v1**, ver §4 |
| `PARENTAL_LEAVE` | no | no | **alcance de la v1**, ver §4 |

### 2. El filtro vive en un sitio, y no es la consulta

El lanzador trae **todas** las ausencias que solapan el período, del tipo que sean. Quién decide
cuáles parten es `CalculatePayrollUnitService`, y ahí está la lista.

No se filtra en la consulta, aunque sería una línea menos de datos: repartir la decisión entre el
adaptador y el cálculo la dejaría escrita en dos sitios, y el día que entre el accidente de trabajo
habría que acordarse de los dos. `SegmentAbsence` no lleva un testigo «¿se paga?» por lo mismo: un
tramo **tiene** ausencia si y sólo si es de las que no se pagan, así que quien calcula no tiene que
volver a preguntarse por el tipo para saber si devenga.

La lista es una constante de Java y no una columna del catálogo. Es una decisión de alcance del
producto —la misma para todos los sistemas de reglas—, y una tabla con un solo valor posible
esconde la decisión donde nadie la busca en vez de declararla. El día que un sistema de reglas
necesite otra lista, esto se convierte en una propiedad del tipo de ausencia; hasta entonces, no.

### 3. Cero días basta: lo que cuelga de los días cae solo

Sólo hay un cambio de cálculo: `D01` —los días de devengo del tramo— vale cero cuando el tramo tiene
ausencia. Con eso se cae el `101`, y con el `101` las cuatro pagas extras (que son agregados suyos),
`PE_TOTAL`, `P_PRORRATA` y las dos puertas de la prorrata. **No hay que tocar ningún otro
calculador**, y eso no es suerte: es lo que se compró en el `backend#119` construyendo la prorrata
con operandos y agregados en vez de con lógica por concepto.

### 4. Lo que la v1 deja fuera, dicho aquí para que no se lea como un olvido

**`IT_WORK_ACCIDENT` sigue cobrando entero.** No es que se nos haya pasado: es otra base reguladora
—la de contingencias profesionales, con las horas extra del año anterior— y el 75 % desde el día
siguiente a la baja. Es un copia-y-cambia de la enfermedad común cuando ésta esté bien, y hasta
entonces el accidente de trabajo se paga como se pagaba. Hay un test que lo afirma: el día que entre,
ese test cambia y se ve.

**`PARENTAL_LEAVE` igual.** En la semilla hay 14 que ocupan septiembre entero, así que si partiera y
quitara días sin pagar prestación, catorce empleados cobrarían cero. Eso sería peor que no hacer
nada.

### 5. El tramo sabe cuánta ausencia lleva detrás

`SegmentAbsence` lleva el tipo y **los días de esta ausencia transcurridos antes del primer día del
tramo**. Lo segundo no lo necesita este ADR: lo necesita el `backend#129`, que cambia de porcentaje
en el día 4, en el 16 y en el 21 **contados desde el inicio de la baja**, que puede estar en agosto.

Va en el contexto del tramo y no como concepto del motor porque es un **dato del tramo**, como el
contrato o el régimen de pagas: lo resuelve quien arma el tramo. Y contar hacia atrás desde la fecha
de inicio de la ausencia **no es leer otro mes** —eso es el `backend#128`—: es una fecha.

## Consecuencias

- **Se mueven exactamente los recibos con `IT_COMMON` o `UNPAID_LEAVE` en el período.** Los demás,
  ninguno: quien no tiene ausencia de esas dos no tiene un corte más ni un paso más.
- **La base de cotización del mes baja con los días de baja, y eso está mal.** Durante la baja se
  sigue cotizando. Lo arregla el `backend#129` con la base durante la baja, y los tres tercios del
  paso 5 van en la misma resiembra: este estado intermedio no llega a la demo. Está dicho aquí y en
  los tests para que nadie lo lea como bueno.
- **Un mes con baja tiene más tramos, y por tanto más pasos de cálculo.** Una baja que empieza y
  acaba dentro del mes aporta **dos** cortes —el día que empieza y el día siguiente al que acaba, que
  es la mitad que se olvida (ADR-068 §1)— y deja tres tramos donde había uno.
- **Una baja que pasa de largo no parte nada.** Sus dos cortes caen fuera del período y la partición
  los descarta; queda un tramo, el mes entero, con cero días. Es el caso que obliga a que los cortes
  se recorten contra el período y no se apliquen tal cual.
- **La media jornada y la baja se componen sin diseñar nada.** Son dos causas de corte de la misma
  unión: una baja dentro de un mes que además cambia de jornada deja los tramos que dejen, y cada uno
  resuelve sus vigencias el primer día.
- **El candado es el escaneo, no el escenario.** `TheUnionOfCutsCoversEveryVerticalThatBreaksThePeriodTest`
  falla si alguien quita una causa de la unión. Hacía falta porque quitar la del contrato no pondría
  en rojo ningún importe —hoy no lo lee nadie, y el ADR-068 §2 dice que rompe aun así— y quitar la de
  la ausencia sólo se nota si además nadie toca su escenario.
- **Queda abierto el mes de 31 días.** `D01` es `min(díasDelTramo, 30)` y el tope se aplica por
  tramo, así que un mes de 31 días partido en dos puede devengar 31 días. Eso ya pasaba con la
  jornada y el contrato desde el `backend#47`; la ausencia no lo empeora ni lo arregla, y la demo
  calcula septiembre.
