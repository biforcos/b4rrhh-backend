# ADR-056 — La unicidad de un código de catálogo no incluye la vigencia

## Estado
Aceptado

## Contexto

El backend#5 apareció al intentar sembrar los tipos de contrato españoles anteriores a la reforma
del RDL 32/2021. La reforma **reutilizó números con significados nuevos**: el `401` era «obra o
servicio determinado» y desde 2022-03-30 es «temporal por circunstancias de la producción»; el `410`
era «interinidad» y ahora es «sustitución con reserva de puesto».

Sembrar los antiguos exige que el mismo código exista dos veces con vigencias disjuntas. Y eso hoy
lo impide una restricción, no una regla de negocio:

```sql
-- V1__initial_personnel_model.sql
alter table rulesystem.rule_entity
    add constraint uk_rule_entity_business
    unique (rule_system_code, rule_entity_type_code, code);
```

`rule_entity` tiene `start_date` y `end_date`, pero su unicidad los ignora. Dicho de otra forma: **el
modelo afirma que un código tiene un único significado para siempre**, y el dominio dice lo
contrario cada vez que una norma renumera algo. No es una peculiaridad de los contratos: los grupos
de cotización, los tipos de ausencia y las categorías de convenio están expuestos a lo mismo.

### Lo que se midió antes de decidir

El issue daba por hecho que levantar la restricción arrastraría al catálogo entero. Al mirarlo, el
terreno resultó **menos hostil de lo que parecía, y no por casualidad**:

- `RuleEntityPersistenceAdapter.findApplicableByBusinessKey(sistema, tipo, código, fechaDeReferencia)`
  ya existe: resuelve por fecha y **lanza `IllegalStateException` si encuentra más de una fila
  aplicable**. Es decir, alguien ya previó que pudiera haber varias y dejó puesto el detector de
  ambigüedad.
- `findByBusinessKeyAndStartDate(...)` también existe: la fecha de inicio ya se usa como parte de la
  identidad al actualizar.
- Los adaptadores de catálogo que leen subtipos de contrato y categorías de convenio **ya filtran por
  fecha de referencia** sobre las tres tablas implicadas. Dos filas del `401` con vigencias disjuntas
  no los confundirían: elegirían la correcta.

Así que el bloqueo no es conceptual. Es una restricción, y unos cuantos sitios que preguntan por un
código **sin decir a qué fecha**.

### Lo que sí costaría

`RuleEntityRepository.findByBusinessKey(sistema, tipo, código)` —la versión sin fecha— se usa en
**una veintena de puntos**, casi todos validadores de catálogo: los que comprueban que el código
existe al escribir una dirección, un contacto, un contrato, una presencia, una clasificación. Cada
uno tendría que decidir *a qué fecha* valida. La respuesta suele ser evidente —la fecha de inicio de
la fila que se está escribiendo, que ya tienen a mano—, pero son veinte decisiones, no una.

Y queda una que no es mecánica: **la resolución de etiquetas**. `RuleEntityLabelResolver` busca por
clave de negocio sin fecha. Si el `401` tuviera dos significados, un contrato de 2020 se mostraría
con la etiqueta de 2022. Hoy eso no puede pasar porque sólo hay un `401`; el día que hubiera dos,
el histórico empezaría a leerse mal en pantalla sin que nada fallara.

Hay además un síntoma ya presente de esta misma familia: tres puntos de `rulesystem` llaman a
`findApplicableByBusinessKey(..., LocalDate.now())`. Resuelven «lo que significa hoy» aunque el dato
sea de antes. Con un solo significado por código da igual; con dos, deja de dar igual.

## Decisión

**La unicidad de `rule_entity` sigue siendo `(rule_system_code, rule_entity_type_code, code)`, sin
componente temporal. Se descarta la `exclude` con solapamiento de rangos.** Un código de catálogo
tiene, en este sistema, un único significado a lo largo del tiempo.

Es una decisión de alcance, no de arquitectura: lo que compra hoy hacerlo es poder mostrar en una
demo contratos derogados hace más de cuatro años. Lo que cuesta es llevar una fecha de referencia
por una veintena de validadores más una semántica nueva para las etiquetas. La relación no sale.

Los códigos anteriores a una reforma que renumeró significados **no se siembran**. Si alguna vez
hicieran falta para una demo, la salida barata es de datos y no de esquema: darlos de alta con un
código distinto que diga lo que son (`401_PRE2022`) y un nombre que lo explique. Es feo, y es
deliberadamente feo: que se note que es un apaño evita que alguien lo tome por el modelo.

## Consecuencias

- **Está escrito que es una simplificación, no un descuido.** Éste es el motivo por el que este ADR
  existe: la alternativa era cerrar el issue en silencio y que dentro de un año alguien —incluidos
  nosotros— construyera encima sin saber que había una asunción debajo.
- El histórico se lee con el significado actual del código. Mientras un código signifique una sola
  cosa, eso es correcto; en cuanto deje de serlo, será incorrecto **sin que ninguna guardia se queje**.
  No hay forma barata de vigilarlo: la restricción de unicidad es precisamente lo que impide que el
  caso llegue a existir, así que el sistema no puede detectar lo que no permite.
- El detector de ambigüedad de `findApplicableByBusinessKey` se queda donde está. Hoy no puede
  dispararse, y ésa es la mejor razón para no tocarlo: el día que la restricción cambie, es la red
  que avisa.
- `LocalDate.now()` en los tres puntos citados sigue siendo aceptable por la misma razón, y deja de
  serlo el mismo día.

## Cuándo reabrir

Cuando haga falta un código con dos significados **en producción, no en la demo**. Señales concretas:
una reforma que renumere mientras el sistema tenga nóminas históricas que la crucen; o un segundo
sistema de reglas cuyo catálogo ya venga con códigos reutilizados de origen.

Si llega ese día, el orden es: primero la fecha de referencia en los validadores y en la resolución
de etiquetas, y sólo después la restricción. Al revés —levantar la unicidad primero— deja la puerta
abierta con el histórico leyéndose mal y sin nada que lo diga.

## Fuera de alcance

Sembrar catálogo antiguo por otras vías. La vigencia de las **extensiones** (ADR-053), que sí cuelgan
de la fila y heredan su periodo. Y las siete divergencias del backend#2, que son otra familia.
