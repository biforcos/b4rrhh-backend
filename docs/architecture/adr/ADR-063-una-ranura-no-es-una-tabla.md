# ADR-063 — Una ranura no es una tabla, y la lista de tablas no sale del catálogo de objetos

## Estado
Aceptado

## Contexto

El designer pintaba `Objetos → Tablas` con
`GET /payroll-engine/{ruleSystemCode}/objects?type=TABLE`. En ESP eso devuelve **una fila**:

```
payroll_engine.payroll_object where object_type_code = 'TABLE'
 id | object_code            | rule_system_code
  4 | P02_DAILY_AMOUNT_TABLE | ESP
```

Y esa fila no es una tabla. Es un **rol de vinculación** —una ranura—:
`PayrollConceptGraphCalculatorService` coge su `objectCode` y lo usa como `bindingRoleCode` para
resolver, contra `payroll.payroll_object_binding`, cuál es la tabla que tiene de verdad los valores.

Las tablas de verdad de ESP son otras tres, y ninguna de ellas es un `payroll_object`:

| rol de vinculación | tabla atada | filas |
|---|---|---:|
| `BASE_SALARY_TABLE` | `SB_99002405011982` | 3 |
| `AGREEMENT_PLUS_TABLE` | `PC_99002405011982` | 3 |
| `P02_DAILY_AMOUNT_TABLE` | `P02_99002405011982` | 3 |

Hay un tercer hecho que es el que obliga a decidir y no sólo a implementar: **de los tres roles
atados, sólo uno existe como `payroll_object`**. Una lista derivada de `payroll_object` enseña uno
de tres; una derivada de `payroll_object_binding` enseña los tres. No son la misma pregunta.

`PayrollTableManagementController` sólo tenía `@PostMapping`: se podían crear tablas y leer sus
filas si ya sabías el código, y no se podían listar (`backend#95`).

## Decisión

**`GET /payroll-engine/{ruleSystemCode}/tables` lista tablas, y las tablas se derivan de dónde
viven los datos: de las filas y de las vinculaciones. Nunca del catálogo de objetos.**

### 1. Una tabla existe si tiene filas, o si algo la ata

La lista es la unión de dos consultas, y ninguna de las dos sirve sola:

- Sólo las filas → una tabla vinculada y vacía no existiría.
- Sólo las vinculaciones → una tabla que ya no lee nadie desaparecería, que es **justo el caso que
  hay que poder ver**.

Por eso el recuento de filas viaja al lado de las vinculaciones y no en otra pantalla: es lo único
que distingue *«se lee y está vacía»* de *«tiene datos y no la lee nadie»*.

Y el recuento son dos, `rowCount` y `activeRowCount`. Una tabla a la que le desactivaron todas las
filas sigue teniendo filas y ya no alimenta nada, y ése es un tercer estado.

La unión se hace en el servicio de aplicación y no en una consulta: *que una tabla existe si tiene
filas o si algo la ata* es una decisión, y una decisión no vive en el adaptador.

### 2. La ranura sale como el rol que ata cada tabla, no como una fila propia

El `bindingRoleCode` va **dentro** de la tabla que ata, junto con el tipo y el código del
propietario. Es lo que es: el nombre por el que el motor llega a esa tabla, no una entidad
hermana.

Un rol sin tabla atada **no sale en esta lista**, porque no hay tabla que listar. No desaparece del
sistema: sigue siendo un nodo del grafo, y su sitio es el lienzo. Deja de fingir que es una tabla.

### 3. Las vinculaciones inactivas se listan, con `active: false`

Una vinculación apagada es la explicación de una tabla que dejó de leerse. Esconderla convierte esa
explicación en un misterio, y el misterio se paga en la pantalla siguiente.

## Consecuencias

- El designer puede pintar las tablas de verdad. Cómo las pinte va en su propio issue.
- ~~`POST /payroll-engine/{ruleSystemCode}/tables` sigue creando un `payroll_object` de tipo
  `TABLE`, que es una ranura y no una tabla.~~ **Cerrado en `backend#98`** — ver abajo.
- `objects?type=TABLE` no está mal y no se toca. Contesta otra pregunta —qué objetos del grafo son
  del tipo `TABLE`— y la contesta bien.
- No se inventa ninguna vinculación para que una ranura huérfana deje de estarlo, ni se retira
  `P02_DAILY_AMOUNT_TABLE` de `payroll_object`: es un nodo que el motor necesita.

---

## Addendum (`backend#98`) — el hueco que este ADR dejó abierto, cerrado

Este ADR dejó escrito, a propósito y sin decidir, que `POST .../tables` y `GET .../tables` **no
eran el par que parecían**, y preguntaba qué debería ser «crear una tabla»: crear la ranura, crear
filas, o las dos cosas atadas.

**Es la primera: crear la ranura.** Lo que estaba mal no era la operación, era su nombre y su sitio.

Tres cosas ordenaron la decisión, y las tres salieron de mirar y no de opinar:

1. **La ranura es portante.** No es código muerto que se pueda borrar: un `payroll_object` de tipo
   `TABLE` es el nodo del que cuelga la alimentación de un concepto. `P02_DAILY_AMOUNT_TABLE` es
   justo eso — es el origen de un `payroll_concept_feed_relation` que alimenta a `P02`—, y sin él
   ese camino del grafo no existe. Los otros dos roles atados en ESP no son objeto ninguno porque
   **se alcanzan por otro camino**: un calculador `ENGINE_PROVIDED` del vertical `basesalary` lleva
   el rol escrito en Java y va directo a `payroll_object_binding`. Ésa, y no una incoherencia, es la
   explicación del «tres vinculaciones y una sola ranura» que este ADR anotó.
2. **Atar no se puede ofrecer aquí.** Una ranura es del sistema de reglas; una vinculación es **de
   un convenio** (`owner_type_code = AGREEMENT`). Un «crear tabla» que además atara tendría que
   elegir a qué convenio, y convenios hay muchos: sería inventarse un flujo antes de saber si hace
   falta.
3. **Crear filas ya existe.** `POST .../tables/{tableCode}/rows` está y es correcto. Lo que faltaba
   no era una operación, era una pantalla de tablas de verdad que la use — y eso lo habilita el
   `GET` que este ADR decidió.

### Lo que se hizo

`POST /payroll-engine/{ruleSystemCode}/binding-roles`, con `bindingRoleCode` en el cuerpo. Se
renombró en vez de documentarse, porque **una nota explicando un nombre equivocado es peor que el
nombre corregido** y porque romperlo costaba un fichero en un repositorio.

Y cambió de sitio: lo que crea es una fila del catálogo de objetos, así que vive con él
(`payroll_engine.object`) y no con las tablas. El vertical de tablas habla de tablas y de sus filas,
y una ranura no es ninguna de las dos. `PayrollTableManagementController` pasó a llamarse
`PayrollTableListingController`, que es lo único que hace ya.

La pantalla llegó antes que el contrato: el `designer#10` ya lo llamaba «Nueva ranura» y ya explicaba
en el propio modal que no es una tabla de importes.
