# ADR-059 — El motor decide si un recibo es válido; las personas deciden si está cerrado

## Estado
Aceptado

## Contexto

`PayrollStatus` tiene cuatro valores desde la V53 —`NOT_VALID`, `CALCULATED`, `EXPLICIT_VALIDATED`,
`DEFINITIVE`—, con sus transiciones escritas en `Payroll`, sus servicios (`ValidatePayrollService`,
`FinalizePayrollService`, `BulkInvalidatePayrollService`) y sus endpoints en el contrato. La regla
de protección está incluso comentada en el código:

```java
// BulkInvalidatePayrollService:85
// EXPLICIT_VALIDATED or DEFINITIVE — protected, must not be bulk-invalidated
```

**Y nada de eso está escrito en ninguna parte fuera del código.** Ninguna pantalla alcanza
`validateExplicitly()` ni `finalizePayroll()`; el motor no produce nunca `NOT_VALID`; y `DEFINITIVE`
no lo pone nadie. Para cualquier barrida honesta de código muerto —el `backend#79` es exactamente
eso— esos cuatro estados se parecen demasiado a lo que hemos retirado tres veces este mes: el stack
duplicado de `workcenter`, los tres campos que nadie envía, los predicados de solape sin llamador.

La diferencia es que **éstos no sobran**. Son un pasillo construido y sin cartel, y un pasillo sin
cartel lo tapia el siguiente que pase con la brocha.

Este ADR es el cartel.

## Decisión

### 1. El motor produce exactamente dos estados

Al calcular, un recibo sale **`CALCULATED`** o **`NOT_VALID`**, y nada más. Los otros dos no los
produce un cálculo: los pone una persona.

`NOT_VALID` **no significa «el motor falló»**. Significa «el motor no da esto por bueno», y eso
incluye dos cosas distintas:

- **No se pudo calcular** — falta un dato sin el que no hay resultado.
- **Se pudo calcular y el resultado no es aceptable** — hay un resultado técnicamente correcto que
  infringe una regla de negocio. Por ejemplo: una categoría de convenio que no admite horas extras
  y una entrada de variables que las trae. El importe sale; el recibo no vale.

Esa segunda es la que justifica el estado. Sin ella, `NOT_VALID` sería un sinónimo de excepción y no
haría falta.

### 2. Un cálculo correcto sobre un mes incompleto **no** es inválido

Un empleado que cesa el día 15 tiene convenio hasta el día 15. Eso es el dato correcto, no un error.
Su recibo sale `CALCULATED`.

Se dice aquí porque la tentación es la contraria: cuando hoy falla el cálculo de quien cesa a mitad
de mes (`backend#73`), marcarlo `NOT_VALID` parece resolverlo. **Sería consagrar un defecto como
estado del dominio.** Ese fallo es que el lanzador resuelve el convenio a fin de período —pregunta
mal—, y se arregla preguntando bien.

**Y la rendición de cuentas ya existe, que es lo que importa de verdad aquí.** La tentación
contraria es escribir un invariante del tipo «todo empleado con presencia en el mes tiene fila de
recibo». **No hay que escribirlo**: eso no es una regla independiente, es el criterio de selección
del lanzamiento (`ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD`) dicho una segunda vez. Una segunda lista
que repite la primera se queda mintiendo el día que la primera cambie.

La regla que sí es del dominio, y que no duplica nada porque **se deriva de lo que la propia
ejecución seleccionó**:

> Una ejecución termina con **cada unidad que seleccionó rendida**: o con un recibo, o con un mensaje
> que la nombra y dice por qué no.

Y se cumple. Medido sobre la corrida completa del `deploy#3`:

```
calculation_run #1 · 202609 · ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD
  total_candidates 873 = total_calculated 871 + total_skipped_not_eligible 2
  calculation_run_message: 871 × UNIT_ELIGIBLE_REAL_EXECUTED
                            2 × UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT
```

`payroll.calculation_run_message` identifica la unidad exacta —hasta el `presence_number`— y lleva
código, severidad, texto y `details_json`. Los dos empleados que hoy «no cobran» (`backend#73`)
**están escritos, con su nombre y su motivo**, y hay `GET /payroll/calculation-runs/{runId}/messages`
para leerlos.

Así que el defecto de aquel issue es más pequeño y más preciso de lo que parecía: **el lanzador
pregunta mal** —resuelve el convenio a fin de período—, y **nadie enseña los mensajes de la
ejecución**. No falta el dato; falta la pantalla.

### 3. La unidad del recibo es la presencia, no el empleado

Un recibo por cada período de presencia del mes. Un empleado presente del 1 al 10 y del 20 al 30
tiene **dos** recibos, porque son dos relaciones con la empresa.

Ya es así: la clave de negocio de `payroll.payroll` incluye `presence_number`, y el lanzamiento
resuelve *presence-based calculation units*, no empleados. Queda escrito para que no se «simplifique»
más adelante.

### 4. Desde `CALCULATED` sólo se sale por un acto de una persona

| A | Qué significa | Quién puede volver |
|---|---|---|
| **`NOT_VALID`** | «hay que rehacerlo» — invalidación manual o masiva | sí, recalculando |
| **`EXPLICIT_VALIDATED`** | «esto no lo toques, está bien así» | **sí**, se puede invalidar |
| **`DEFINITIVE`** | «esto está cerrado» | **no** |

La distinción entre los dos últimos es deliberada y es la razón de que sean dos y no uno:

- **`EXPLICIT_VALIDATED`** protege de un recálculo masivo algo que se quiere conservar tal cual —el
  caso típico es un finiquito ya pagado— **sin renunciar a poder corregirlo** si más tarde se
  descubre que estaba mal. Es una protección, no un cierre.
- **`DEFINITIVE`** es el cierre. Un recibo definitivo no vuelve. Si estaba mal, lo que procede es
  una regularización en un período posterior, no reescribir el pasado.

### 5. Recalcular sustituye, y no se guarda historia de cálculos inválidos

Sólo se recalcula lo que está `NOT_VALID` (`PayrollStatus.canBeRecalculated()`), y el cálculo nuevo
**sustituye** al anterior. No se conservan versiones.

Esta regla es segura **por la decisión 4**: como `DEFINITIVE` no se puede invalidar, nunca se
destruye algo que llegó a darse por cerrado. Si algún día `DEFINITIVE` dejara de ser un callejón sin
salida, esta decisión habría que revisarla con ella.

Lo que sí falta es **procedencia**: el recibo debe decir qué ejecución lo produjo (`backend#62`). Eso
no es historia de cálculos; es saber de dónde sale el que está vigente.

### 6. Los consumidores preguntan por el estado, no inventan el suyo

El valor del ciclo no está en los estados: está en que cualquier trabajo posterior pueda acotarse con
una pregunta sobre el estado resultante. Un generador de transferencias SEPA pide «los definitivos» o
«todo lo que no esté inválido», y **no decide por su cuenta qué es una nómina pagable**.

Es el mismo principio del ADR-057 una capa más arriba: la integridad se juzga sobre el estado
resultante, no dentro de cada operación. Aquí, la aptitud se pregunta al estado, no se deduce en cada
consumidor.

### 7. El motivo de invalidez será una vertical, y hasta entonces esta frontera

Cuando el motor empiece a producir `NOT_VALID` por reglas de negocio, un recibo podrá infringir
**varias comprobaciones a la vez**, y un `varchar(50)` no lo soporta. El motivo pasará a ser una
**vertical propia de `payroll`**: una colección de hallazgos colgando del recibo, cada uno con

- **el código de la comprobación**,
- **el detalle libre** que explique el caso concreto.

**No va en `payroll_context_snapshot`.** El snapshot guarda con qué datos se calculó; un hallazgo de
validación no es un dato de entrada. Convertir el snapshot en el sitio donde cabe todo es cómo se
obtiene una tabla que nadie sabe describir.

**El literal no se guarda con el hallazgo: sale del catálogo.** El código de comprobación es un tipo
de entidad de `rule_entity` como todo lo demás, con su literal y su capa de traducciones (ADR-052).
Guardar el texto junto a cada hallazgo daría el mismo código con literales distintos según cuándo se
escribió, y sin camino a la traducción. No hace falta congelarlo: los hallazgos viven sobre recibos
`NOT_VALID`, que por definición se recalculan y desaparecen.

**El detalle libre es para leerlo, nunca para decidir.** El día que alguien necesite filtrar o
ramificar por lo que dice ese texto, lo que falta es un código nuevo, no un análisis del texto.

**La lista no puede crecer sin control**, y ésa es su mejor propiedad: cada comprobación nueva exige
implementarla en el motor **y** darla de alta en el catálogo. El coste de añadir una es el coste de
desarrollarla, así que el catálogo no se llena solo.

**Mientras tanto, la frontera:** `payroll.status_reason_code` se queda **para el acto humano** —quien
invalida a mano declara por qué—, y **el motor no lo escribe**. En cuanto el motor tenga algo que
decir, hace falta la vertical. Media implementación dentro del `varchar` es lo que impediría hacer la
buena después.

## Consecuencias

- **Los cuatro estados, sus servicios y sus endpoints se quedan**, aunque hoy ninguna pantalla los
  alcance. Cualquier barrida de código sin llamador —`backend#79` y las que vengan— tiene que
  exceptuarlos citando este ADR.
- **El motor tiene que aprender a producir `NOT_VALID`.** Hoy sólo sabe producir `CALCULATED` y, si
  algo falla, no producir nada. Eso es lo que hace invisible el `backend#73`.
- **`backend#62` se reduce**: no es «guardar historia», es que el recibo lleve su `run_id`.
- **El `frontend#42` —tocar el grafo y ver cambiar el recibo— sólo funciona sobre `CALCULATED`.** La
  semilla de la demo no puede marcar nada `DEFINITIVE` ni `EXPLICIT_VALIDATED`, o el golpe 4 deja de
  poder enseñarse. Quien añada «cerrar la nómina» a la demo tiene que saberlo.
- **Falta pantalla, dos veces.** Cuatro estados, dos actos humanos y ningún sitio donde ejercerlos;
  y `calculation_run_message`, que rinde cuentas de cada unidad, tampoco se enseña en ninguna parte.
  Es lo que hace que hoy esto parezca código muerto, y lo que dejará de hacerlo cuando exista.
- **`COMPLETED` no significa «han cobrado todos».** La corrida del `deploy#3` terminó `COMPLETED` con
  dos unidades saltadas. Quien pinte el estado de una ejecución tiene que leer los contadores, no
  sólo el `status`.
- **`requested_by` está vacío.** El campo existe para saber quién lanzó y nadie lo rellena. Con el
  lanzamiento asíncrono (`backend#75`) deja de ser un detalle: una ejecución que dura cinco minutos y
  no dice quién la pidió es un problema el día que dos personas lancen a la vez.
