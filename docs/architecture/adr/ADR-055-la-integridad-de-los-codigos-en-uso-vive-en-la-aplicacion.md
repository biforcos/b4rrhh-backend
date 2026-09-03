# ADR-055 — La integridad de los códigos en uso vive en la aplicación

## Estado
Aceptado

## Contexto

Las tablas del esquema `employee` referencian el metamodelo **por código, en texto**:
`presence.entry_reason_code` guarda `HIRING`, `contract.contract_code` guarda `100`,
`address.country_code` guarda `ESP`. Son diecisiete columnas en once tablas, y el código sabe
cuáles son: cada vertical las declara en su `*RuleEntityUsageParticipant` (ADR-052, backend#28), y
una guardia comprueba que ninguna columna `*_code` del esquema se quede sin declarar (backend#29).

Lo que destapó el backend#39 es que en la base de datos **no hay nada** que las defienda: ni clave
ajena, ni `check`, ni disparador. Lo sacó a la luz un fixture que llevaba `HIRE` como motivo de
entrada, un código que no existe en ningún catálogo. Contra H2 pasaba; contra el esquema real
también habría pasado, porque la base no lo impide. Lo cazó la casualidad, no una restricción.

Debajo hay una asimetría entre las dos mitades del metamodelo. Hacia arriba —las extensiones que
cuelgan de `rule_entity`— el ADR-053 las declara, las nueve claves ajenas caen en cascada y cuatro
guardias lo vigilan. Hacia abajo —los datos de negocio que *usan* un código— la única defensa era la
validación en Java al escribir. Una fila metida por una migración, por un script o por cualquier
camino que se saltara el caso de uso entraba sin que nada la parase.

Antes de decidir se midió. Sobre la base local de mil empleados, que es también la que tiene la demo:
**cero filas huérfanas en 23.623 comprobadas**, en las diecisiete columnas. Así que esto es
prevención, no reparación: no hay nada que limpiar. Pero la misma medición dejó dos cosas a la vista
que este ADR no puede callar, y están en la sección de consecuencias.

## Decisión

### 1. No hay clave ajena, y no es un olvido

Se descartó, por dos motivos y en este orden de peso:

**Una clave ajena no sabe decir lo que aquí importa.** Un motivo de entrada puede dejar de estar
vigente sin que las presencias que lo usaron dejen de ser válidas: el histórico se leería mal para
siempre si el código no estuviera, y por eso `countReferences` cuenta todas las filas, vigentes o
no. Una clave ajena no distingue «este código no existe» de «este código ya no está vigente», y esa
diferencia no es un detalle del modelo: **es el dominio**.

**Y la única clave ajena posible sería cara.** Estas columnas guardan el código, no el `id`, y el
código sólo es único dentro de `(rule_system_code, rule_entity_type_code, code)`. Una clave ajena
compuesta arrastraría dos columnas más a once tablas, para luego no distinguir lo que hay que
distinguir.

### 2. La integridad de estas columnas vive en la capa de aplicación, a propósito

Los casos de uso validan el código contra el metamodelo al escribir, y ésa es la defensa primaria.
Esta decisión lo deja escrito para que nadie lo vuelva a descubrir dentro de seis meses y lo tome
por un hueco: **la base no defiende estas columnas porque se decidió que no lo hiciera.**

### 3. Una guardia afirma que el dato cuadra con lo declarado

Lo que la base no impide, un test lo detecta. `EveryCatalogColumnPointsToAnExistingCodeTest` es un
test sobre el esquema real que recorre las columnas declaradas y falla si alguna fila apunta a un
código que no existe en `rulesystem.rule_entity`. Es el hermano directo de la guardia del backend#29:
aquélla comprueba que toda columna esté **declarada**; ésta, que lo declarado **cuadre con el dato**.
Juntas cierran lo que el ADR-053 §5 exige —sólo entra en el metamodelo lo que una guardia pueda
comprobar— por el lado que faltaba.

Tres reglas que la guardia cumple y que no son detalle:

- **La lista de columnas no se escribe en el test.** Sale de `declaredUsages()` del participante, el
  mismo `Map` que alimenta `countReferences`. Para eso se amplió el contrato de
  `EmployeeOwnedRuleEntityUsageParticipant`: antes exponía sólo «tabla.columna», y ahora expone
  también el tipo de catálogo de cada columna y de dónde sale el `rule_system_code`. Un vertical
  nuevo entra en la guardia con declararse. Una segunda lista que alguien tuviera que mantener
  sincronizada sería exactamente el registro central que el patrón del ADR-047 existe para evitar.
- **El ámbito es la reglamentación.** Un código sólo existe dentro de su `rule_system_code`: un
  `FIJO` español no vale para un empleado de Reino Unido. La reglamentación está en la propia tabla
  en `employee` y `employee_payroll_input`, y se hereda del empleado en las otras nueve; lo dice el
  participante, no el test. Una comprobación que mirase sólo el código daría verde con datos rotos, y
  hay un caso que lo demuestra.
- **Todas las filas, vigentes o no.** Igual que `countReferences`, y por el mismo motivo. La
  vigencia no se juzga aquí.

Y, como toda guardia de esta casa, **se demuestra**: un segundo test inserta dentro de la
transacción una fila con un código inexistente y comprueba que la guardia la caza señalando la
columna, el tipo, cuántas filas y qué códigos. Sin eso no se sabría si el test comprueba algo o
simplemente pasa.

### 4. El fallo es una decisión, no un aviso

Cuando la guardia falla dice columna, tipo de catálogo, cuántas filas y **cuáles** son los códigos,
con su reglamentación y su recuento. El «cuáles» es lo que convierte el fallo en una decisión: no es
lo mismo un residuo de fixture que doscientos códigos de convenio. Lo primero se corrige en la fila;
lo segundo es un código que falta en `rule_entity`, y se da de alta con la vigencia que le toque.

## Consecuencias

**El valor de esta guardia crece con la cobertura de la semilla.** Ésta es la frase que no se
puede omitir. La guardia afirma algo sobre las filas que tiene delante, y no sobre las que no
tiene:

- Sobre la base de mil empleados del loader, afirma algo sobre **catorce columnas de diecisiete** y
  nada sobre tres: `cost_center.cost_center_code`, `employee_absence.absence_type_code` y
  `employee_payroll_input.employee_type_code` no tienen ni una fila, porque el loader no las genera
  (workforce-loader#5). Su cero es un no-dato. Y en tres de las catorce —convenio, subtipo de
  contrato y tipo de empleado— hay **un solo código** en juego entre casi dos mil filas: la guardia
  pasa en verde sin haber probado gran cosa.
- **En el pipeline, menos aún.** Los tests sobre el esquema real corren sobre una base recién
  migrada, y las migraciones no siembran ni una fila en `employee`. Allí la guardia comprueba lo que
  siembran los fixtures de la propia suite, que se deshacen con cada transacción. Cazaría un `HIRE`
  en un fixture —que es justo cómo empezó esto— pero no un dato roto en la demo. Para afirmar algo
  sobre una base con datos hay que ejecutarla contra esa base, y hoy no hay camino escrito para eso.

Un ADR que dijera «la integridad de estos códigos está comprobada» sin estos dos párrafos sería
precisamente el tipo de verdad a medias que este proyecto lleva meses cazando.

Lo demás:

- Añadir un vertical que guarde códigos sigue costando lo mismo: un participante. La guardia lo
  recoge sola.
- La guardia del backend#29 no cambia de forma: `declaredColumns()` se deriva ahora de
  `declaredUsages()` y sigue siendo la misma lista.
- Las columnas que no son códigos de `rule_entity` —`concept_code`, `postal_code`, `region_code` y
  los dos `rule_system_code`— siguen exentas en la guardia del backend#29 con su motivo, y esta
  guardia no las mira: no las declara ningún participante.

## Fuera de alcance

Sembrar las tres verticales vacías (workforce-loader#5). Las otras siete divergencias que destapó el
backend#2 al sacar los tests de H2. Y las claves ajenas ausentes entre `payroll`/`payroll_engine` y
`rulesystem`: misma familia, otro tamaño, se miran aparte.
