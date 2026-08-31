# ADR-054 — Lo que un tipo de entidad sabe de sí mismo

## Estado
Aceptado

## Contexto

`rule_entity_type` sigue exactamente como la dejó la V1: `id`, `code`, `name`, `active`,
`created_at`, `updated_at`. Ni un `alter` en más de cien migraciones. Hay dieciséis tipos vivos —los
dos que estaban mal nombrados, `EMPLOYEE_PRESENCE_COMPANY` y `EMPLOYEE_CONTACT_TYPE`, los borraron la
V35 y la V37 al renombrarlos.

Tres cosas distintas han acabado pidiendo el mismo sitio:

- **El menú.** La barra lateral es HTML escrito a mano. Si dar de alta un maestro obliga a editarla,
  se pierde justo la propiedad que hacía valioso el metamodelo (b4rrhh/frontend#33).
- **La clase del literal.** El ADR-052 §2 la dejó anotada como atributo del tipo y siguió sin ella.
- **La lista de tipos universales**, que hoy vive en un `List.of` dentro de
  `UniversalCatalogLiteralsGuardTest`, con un comentario que dice que es provisional (backend#25).

El ADR-053 §5 fijó el criterio de admisión: sólo entra lo que una guardia pueda comprobar. Este ADR
se somete a él, y en un punto lo obliga a afinarse.

## Decisión

### 1. No entra un nombre para la interfaz

El issue original lo pedía, apoyándose en la deuda de nombres del ADR-012. Esa deuda ya se pagó, y se
pagó de otra manera: la V35 migró las filas de `EMPLOYEE_PRESENCE_COMPANY` a `COMPANY`, reapuntó la
configuración y terminó con `delete from rulesystem.rule_entity_type where code = ...`. La V37 hizo lo
mismo con `EMPLOYEE_CONTACT_TYPE`.

El precedente de esta casa es que **un tipo mal nombrado se renombra con una migración**, no se
disfraza con una etiqueta. Dos nombres para la misma cosa divergen el primer día y, peor, quitan el
dolor: el código se queda mal para siempre porque ya no molesta a nadie.

Si algún día hay que traducir el nombre de un tipo, va donde van los literales de las entidades
—`rule_entity_translation`, ADR-052— y no a una segunda columna en el mismo idioma.

### 2. Tres ejes, y ninguno se deriva de los otros

La tentación era una sola columna con dos valores, «catálogo» y «entidad». No sale, y hay dos pruebas
en los datos de hoy:

- **Tener extensión no es tener nombre propio.** La V106 declara cuatro extensiones `required`:
  `company_profile`, `work_center_profile`, `agreement_profile` y `agreement_category_profile`. Los
  literales de las dos últimas son citas de la norma —«Grupo I - Técnico y Titulado», del convenio
  99002405—, no nombres propios como «Branch North». Tener extensión correlaciona con «tiene más
  atributos», que es lo que dice el ADR-053, y no con la clase del literal. Coinciden en `COMPANY` y
  `WORK_CENTER`, y por eso parecía un patrón.
- **`COUNTRY` es vocabulario traducible y a la vez nadie lo mantiene a mano.** La V16 lo siembra
  desde ISO 3166-1 alpha-3, cruzado contra todas las reglamentaciones. Se traduce —«España», «Spain»,
  «Espagne»— y jamás se da de alta un país desde una pantalla.

Son dos ejes independientes que se solapan a medias. Se pagan por separado.

### 3. La clase del literal

Tres valores:

- **Vocabulario del dominio.** Es nuestro vocabulario, o uno neutro compartido. Inglés como forma
  base, traducible. Los motivos de entrada y salida, los tipos de dirección, de contacto, de
  identificador, de ausencia.
- **Cita reglamentaria.** El literal es el nombre oficial de una figura de la norma y ya viene en su
  idioma. Traducirlo sería falsificarlo. Los contratos, sus subtipos, los convenios, sus categorías,
  los grupos de cotización.
- **Nombre propio.** El nombre de una cosa concreta. No tiene idioma. Empresas, centros de trabajo,
  centros de coste.

`COUNTRY` queda como **vocabulario del dominio** aunque el vocabulario lo decida ISO y no nosotros. La
razón es que los tres consumidores del atributo —la traducibilidad del ADR-052 §1, la guardia de
universalidad del backend#25 y la forma del formulario de alta de frontend#31— lo tratan exactamente
igual que a los nuestros. Si algún día uno de los tres necesita distinguirlo, ése será el momento de
partir el valor. Hoy sería una distinción sin consecuencia.

### 4. El modo de mantenimiento, no un booleano de visibilidad

Un `visible boolean` confunde dos cosas: **se ve** y **se edita**. `COUNTRY` y `GRUPO_COTIZACION`
—«01 → Ingenieros y Licenciados», que viene del BOE— son cosas que hay que ver y que nunca se tocan.
Con un booleano hay que elegir entre esconderlas, y entonces no hay dónde consultarlas, o mostrarlas
como si fueran editables.

Tres valores:

- **Se mantiene.** El usuario da de alta y edita.
- **Referencia.** Sólo lectura para el usuario, pero una fila nueva es sólo un dato más. Si mañana
  aparece Andorra en el catálogo de países, no se rompe nada.
- **Cerrado.** Sólo lectura **y** una fila nueva exige que alguien le dé significado, porque cada
  valor lleva comportamiento aparejado. `EMPLOYEE_TYPE` es el caso: `employee_type_code` es una de
  las cuatro dimensiones de ámbito de `concept_assignment` (V58), de modo que un tipo de empleado
  nuevo puede cambiar qué conceptos de nómina se le aplican a alguien. Sembrar uno sin decidir eso no
  es añadir un dato: es dejar una pregunta abierta dentro del motor.

La diferencia entre los dos últimos es la que un booleano borra, y es la que tiene consecuencias.

**No hay un cuarto valor para «oculto».** El issue lo pedía, con el argumento de que algunos tipos son
fontanería. Al listar los dieciséis, ninguno lo es: todos son cosas que un usuario de nóminas
reconoce y quiere poder consultar. Añadir hoy esa columna sería añadir un valor que nadie usa.
**Disparador:** el día que exista un tipo que de verdad no deba salir en ninguna pantalla, se añade
entonces, con ese tipo delante como caso concreto.

### 5. La agrupación vive en su propia tabla

Nace `rule_entity_type_group` con `code`, `name` y `display_order`, y `rule_entity_type` la referencia
por clave ajena. Dos grupos: **Organización** y **Sociedad**.

Podría haber sido un `check` sobre una columna de texto. La tabla gana dos cosas: el menú necesita el
nombre del grupo y su orden, que en un `check` no caben; y la clave ajena da gratis la mitad de la
guardia.

### 6. Ninguna de las tres es nullable, y ninguna tiene valor por defecto

Aquí se invierte a propósito el ADR-053. Allí la ausencia significaba algo —«sólo raíz»— y era
segura, porque era el caso común e inofensivo.

Aquí la ausencia es peligrosa: **un tipo sin clasificar es indistinguible de un tipo escondido a
propósito**, y ese es exactamente el fallo que este ADR existe para impedir — aparece un maestro
nuevo, nadie lo clasifica, y no llega nunca al menú sin que salte nada.

Un valor por defecto es una ausencia disfrazada, así que tampoco. Añadir un tipo obliga a tomar las
tres decisiones, y la migración no pasa si no se toman.

### 7. Las guardias

El ADR-053 §5 dice que sólo entra lo que una guardia pueda comprobar, y estos metadatos son
**decisiones editoriales**: ninguna guardia puede saber que los centros de coste van en Organización y
no en Sociedad. La regla necesita el matiz que este caso obliga a escribir.

**No se puede garantizar que la clasificación sea correcta. Se puede hacer imposible que falte.** Y
eso cubre el fallo que de verdad ocurre, que no es clasificar mal —eso se ve al mirar el menú— sino no
clasificar en absoluto.

Con lo cual la mayor parte del trabajo no la hace ningún test:

1. **Clausura del grupo** — la clave ajena. La impone Postgres.
2. **Completitud** — los tres `not null` sin defecto. La impone Postgres.
3. **La sonda** — añadir un tipo sin clasificar en la transacción del test y ver que falla, con el
   código del tipo en el mensaje.

**Y ninguna guardia de comportamiento para el modo `cerrado`.** La primera redacción de este ADR decía
que los códigos de un tipo cerrado tenían que existir como constante en Java, y que eso era lo que
justificaba el valor. Es falso, y se vio al ir a implementarlo: el comportamiento que cuelga de un
tipo de empleado no vive en constantes, vive en **datos** —las filas de ámbito de
`concept_assignment`—. La guardia estaba mirando el artefacto equivocado.

Se retira sin sustituto. Los tres modos son igual de editoriales y el criterio de arriba los cubre a
los tres por igual. El valor `cerrado` se justifica porque es la distinción que tiene consecuencias
—una fila nueva abre una pregunta en el motor de nómina—, no porque fuera comprobable.

### 8. Lo que se deriva

- **El menú** (frontend#33): pertenencia, grupo y orden salen del modelo. Se acaba la lista a mano.
- **La acción de la pantalla**: del modo de mantenimiento. Un tipo `referencia` o `cerrado` no ofrece
  «Nuevo». Es el mismo movimiento que el ADR-051 hizo con el contenedor.
- **Qué filas admiten traducción** (ADR-052 §1): las de clase vocabulario del dominio.
- **La guardia de universalidad** (backend#25): la lista de tipos universales pasa a ser una consulta
  por clase de literal. Desaparece el `List.of` y su comentario de provisionalidad.
- **La forma del formulario de alta** (frontend#31), y si hay formulario siquiera.

**Relación con el ADR-053 §7.** Aquél deriva de las extensiones la **forma** de la pantalla: un tipo
con extensiones tiene pantalla propia, uno sin ellas vive en Catálogos. Éste deriva **dónde vive y qué
se puede hacer con él**. Son preguntas distintas, y `COST_CENTER` lo demuestra: hoy tiene entrada
propia en el menú y no tiene ninguna extensión declarada. Si sólo existiera el ADR-053, se iría a
Catálogos. La pregunta que frontend#33 dejó abierta —si a los centros de coste les falta un perfil o
están en el sitio equivocado— sigue abierta, pero ya no bloquea el menú.

## La clasificación de los dieciséis

| Tipo | Clase de literal | Mantenimiento | Grupo |
|---|---|---|---|
| `COMPANY` | nombre propio | se mantiene | Organización |
| `WORK_CENTER` | nombre propio | se mantiene | Organización |
| `COST_CENTER` | nombre propio | se mantiene | Organización |
| `AGREEMENT` | cita reglamentaria | se mantiene | Sociedad |
| `AGREEMENT_CATEGORY` | cita reglamentaria | se mantiene | Sociedad |
| `CONTRACT` | cita reglamentaria | referencia | Sociedad |
| `CONTRACT_SUBTYPE` | cita reglamentaria | referencia | Sociedad |
| `GRUPO_COTIZACION` | cita reglamentaria | referencia | Sociedad |
| `COUNTRY` | vocabulario del dominio | referencia | Organización |
| `EMPLOYEE_PRESENCE_ENTRY_REASON` | vocabulario del dominio | se mantiene | Organización |
| `EMPLOYEE_PRESENCE_EXIT_REASON` | vocabulario del dominio | se mantiene | Organización |
| `EMPLOYEE_ADDRESS_TYPE` | vocabulario del dominio | se mantiene | Organización |
| `CONTACT_TYPE` | vocabulario del dominio | se mantiene | Organización |
| `EMPLOYEE_IDENTIFIER_TYPE` | vocabulario del dominio | se mantiene | Organización |
| `EMPLOYEE_ABSENCE_TYPE` | cita reglamentaria | se mantiene | Organización |
| `EMPLOYEE_TYPE` | vocabulario del dominio | **cerrado** | Organización |

### Corrección (backend#37): `EMPLOYEE_ABSENCE_TYPE` es cita, no vocabulario

La primera redacción de la tabla lo clasificaba como vocabulario del dominio. Está corregido arriba,
y conviene dejar escrito por qué, porque lo destapó una guardia y no una revisión.

Al reconvertir la guardia de universalidad (backend#25) para que leyera la clase de la columna en vez
de una lista a mano, saltó en rojo: los siete códigos de la V102 existen sólo en ESP. La primera
lectura fue que faltaba sembrarlos en FRA y PRT. Es la lectura equivocada: lo que falta no son filas,
es que la clasificación era mía y estaba mal.

Los literales lo dicen solos —«IT Contingencia Común», «IT Accidente de Trabajo / Enfermedad
Profesional», «Permiso por fuerza mayor»—: terminología de la Seguridad Social y el artículo 37.3 del
Estatuto. Es Derecho laboral español, como `CONTRACT` y `CONTRACT_SUBTYPE`, que ya estaban bien
clasificados y sembrados sólo en ESP sin que nadie lo tomara por un agujero. Tres señales más
apuntaban a lo mismo y ninguna se miró al escribir la tabla: el fichero se llama
`V102__seed_employee_absence_type_esp.sql`; no lleva el `cross join` sobre `rule_system` que sí llevan
la V4 y la V16; y el nombre está en castellano con la descripción en inglés, al revés que en todos los
tipos de vocabulario.

Sembrar los siete en FRA y PRT habría exigido **inventarse los tipos de ausencia franceses y
portugueses**, es decir, meter contenido normativo falso en la base para que un test se pusiera verde.

### Límite conocido: la clase va en el tipo, no en la fila

El mismo caso destapa un límite de este ADR. De los siete códigos de ausencia, `IT_COMMON` e
`IT_WORK_ACCIDENT` son citas sin discusión, pero `VACATION` y `UNPAID_LEAVE` son bastante genéricos.
El tipo es **mixto**, y aquí la clase se declara una vez por tipo.

Se deja así a propósito: una clase por fila multiplica por el número de filas un trabajo que hoy se
hace dieciséis veces, y todavía no se conoce ningún consumidor al que la mezcla le cambie la
respuesta. **Disparador:** el día que un tipo mixto tenga una consecuencia real —una fila que debería
traducirse dentro de un tipo que no se traduce, o al revés—, la clase baja a la fila, con ese caso
delante.

## Consecuencias

- La migración tiene que clasificar los dieciséis a mano. Eso es la mayor parte del trabajo y es
  deliberado: es el momento en que las decisiones se toman en vez de heredarse.
- Añadir un tipo nuevo cuesta tres decisiones. Es el precio de que el menú no pueda contradecir al
  modelo, y es barato comparado con editar HTML.
- **El hueco de `EMPLOYEE_TYPE` en FRA y PRT deja de ser una pregunta.** El backend#25 lo dejó
  anotado como posible agujero. Siendo un tipo cerrado, sólo se declara donde hace falta: no falta
  nada.
- La guardia del backend#25 pierde su lista a mano, que era la deuda que ella misma documentaba.
- El ADR-012 puede cerrarse.

## Fuera de alcance

- **El nombre para la interfaz** (§1), con su razón escrita.
- **El valor «oculto»** del modo de mantenimiento (§4), con su disparador.
- **Renombrar `EMPLOYEE_TYPE` y sus códigos.** `INTERNAL`/`EXTERNAL` describen mal lo que son —los
  tipos de empleado que una empresa puede tener: asalariado, externo, becario—. Es una migración de
  renombrado del estilo de la V35 y la V37, y va aparte.
- **El nombre de la pantalla**, «Maestros» en el menú y «Catálogos» en el título. El ADR-053 ya lo
  dejó fuera y sigue fuera: es vocabulario, no modelo.
- **Si a los centros de coste les falta un perfil.** Sale de §8 y se decide con el ADR-053 delante,
  no aquí.
