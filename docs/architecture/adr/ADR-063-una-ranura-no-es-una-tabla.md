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
- `POST /payroll-engine/{ruleSystemCode}/tables` sigue creando un `payroll_object` de tipo `TABLE`,
  que es una ranura y no una tabla. **Ese endpoint y este `GET` no son el par que parecen**, y ahí
  queda un hueco por decidir: si crear una tabla debe ser crear filas, crear la vinculación, o las
  dos cosas. Este ADR no lo cierra; lo deja escrito para que no se descubra otra vez.
- `objects?type=TABLE` no está mal y no se toca. Contesta otra pregunta —qué objetos del grafo son
  del tipo `TABLE`— y la contesta bien.
- No se inventa ninguna vinculación para que una ranura huérfana deje de estarlo, ni se retira
  `P02_DAILY_AMOUNT_TABLE` de `payroll_object`: es un nodo que el motor necesita.
