# ADR-052 — El idioma de los literales del metamodelo

## Estado
Aceptado

## Contexto

En la ficha del empleado, la primera tabla de la pantalla principal muestra «Hiring» y
«Retirement» bajo cabeceras en español. Es el borde áspero más visible de la aplicación, porque está
donde más se mira.

El literal vive en la propia fila de `rule_entity`:

```sql
create table rulesystem.rule_entity (
    rule_system_code      varchar(5)   not null,
    rule_entity_type_code varchar(30)  not null,
    code                  varchar(30)  not null,
    name                  varchar(100) not null,
    description           varchar(500),
    ...
);
alter table rulesystem.rule_entity
    add constraint uk_rule_entity_business unique (rule_system_code, rule_entity_type_code, code);
```

No hay ningún sitio donde poner un segundo idioma.

### Lo que la tabla mezcla

Al mirar los datos sembrados aparecen tres cosas distintas con las mismas columnas:

```sql
('ESP', 'EMPLOYEE_PRESENCE_ENTRY_REASON', 'HIRING',      'Hiring',
        'Initial hiring into the company')

('ESP', 'CONTRACT',                       '100',         'Indefinido ordinario (jornada completa)',
        'Contrato indefinido ordinario a tiempo completo (art. 12 ET)')

('ESP', 'AGREEMENT_CATEGORY',             '99002405-G1', 'Grupo I - Tecnico y Titulado',
        'Grupo profesional I: personal tecnico y titulado. Convenio 99002405.')
```

La primera es **vocabulario nuestro**: la inventamos, el inglés es su forma neutra y traducirla es
correcto.

Las otras dos **ya están en español y están bien así**. No son traducciones pendientes: son el nombre
oficial de una figura contractual citando el artículo 12 del Estatuto, y un grupo profesional del
convenio 99002405. Nadie las escribió en el idioma equivocado.

Y hay una tercera clase, que no está en esa lista pero comparte tabla: los **nombres propios** —
«Spain Company 01», «Branch North»—. No tienen idioma; tienen nombre. No por casualidad son
justamente los tipos que tienen tabla `*_profile`, y las únicas filas de `rule_entity` a las que
alguien apunta con clave ajena.

### Por qué esto importa antes de escribir una sola línea

Si se declara «los literales del backend van en inglés y esperan traducción», las citas
reglamentarias quedan marcadas como deuda. Alguien vendrá a completarla, y la traducción inglesa de
`CONTRACT/100` acabará siendo *Permanent ordinary contract*. Eso no es traducir: es **inventarse el
nombre de una figura contractual española**, y queda registrado como dato bueno.

### La causa de fondo: el alcance, no el idioma

Hay códigos que **trascienden** la localización. «Contratación» es lo mismo en España, Francia y
Portugal; por eso su código es único y por convención va en inglés. Y hay códigos que **son**
localizados: un contrato fijo es `100` en ESP y otra cosa en PRT, porque cada regulación tiene su
catálogo.

El modelo hoy sólo tiene un eje —`rule_system_code`— y **obliga a todo a colgar de él**. Por eso
`HIRING` está tres veces, una por reglamentación, con el mismo texto. Y como la unicidad es
`(rule_system_code, type, code)`, esas tres filas son **independientes**: nada impide corregir el
literal de una y dejar las otras atrás. El modelo afirma que son tres cosas distintas cuando son una,
y sólo la convención las mantiene iguales.

Las tres clases de arriba no son un eje nuevo: **son una consecuencia del alcance**. Un código que
trasciende no tiene regulación detrás, luego es vocabulario nuestro, luego se traduce. Un código
nacional es una cita de su norma, luego no.

## Decisión

### 1. El idioma vive en una tabla aparte, colgando de `rule_entity`

```sql
create table rulesystem.rule_entity_translation (
    rule_entity_id bigint       not null references rulesystem.rule_entity(id),
    language_code  varchar(5)   not null,
    name           varchar(100) not null,
    description    varchar(500),
    primary key (rule_entity_id, language_code)
);
```

`rule_entity.name` y `rule_entity.description` **se quedan donde están** como literal base. El cambio
es puramente aditivo: ni una columna que cambie, ni un dato que mover.

**Se indexa por `rule_entity_id`, no por `(tipo, código)`.** Eso duplica la traducción de `HIRING`
una vez por reglamentación, y se asume: la clave única de la tabla ya es por reglamentación, así que
indexar por `(tipo, código)` afirmaría que `HIRING` se llama igual en ESP y en FRA — y eso no es una
afirmación sobre el idioma, sino sobre la regulación. Tres filas de texto son baratas; deshacer una
clave equivocada, no.

### 2. No todas las filas llevan traducción

La traducibilidad es un atributo del **`rule_entity_type`**, con tres valores:

| Clase | Ejemplo | Literal base | Traducciones |
|---|---|---|---|
| Vocabulario del dominio | `HIRING`, `HOME`, `FISCAL` | inglés | sí |
| Cita reglamentaria | `100 · Indefinido ordinario`, `Grupo I` | el idioma de la norma | **no** |
| Nombre propio | `Spain Company 01`, `Branch North` | el nombre | **no** |

Ese atributo va junto a los metadatos de agrupación y visibilidad del mismo `rule_entity_type`: misma
tabla y misma conversación.

Consecuencia útil: **lo que queda por traducir es pequeño**. Motivos de entrada y salida, tipos de
dirección, de contacto y de identificador. Unas pocas docenas de valores.

### 3. La resolución va en un solo sitio

Los diez adaptadores que hoy convierten un código en literal —presencia, dirección, contacto,
contrato, centro de coste, identificador, clasificación laboral, centro de trabajo, contacto de
centro, y el de opciones de catálogo— desembocan todos en las mismas dos líneas:

```java
ruleEntityRepository.findByBusinessKey(ruleSystemCode, ruleEntityTypeCode, code)
    .map(entity -> entity.getName()...)
```

Cada uno con **su propia copia privada** del mismo método `findCatalogName`. La resolución del idioma
va en un único componente y los nueve adaptadores borran su copia y delegan. El cambio **quita**
duplicación, no la añade.

Los `*CatalogValidator`, que leen `rule_entity` sólo para comprobar que un código existe, **no se
tocan**: no producen literales.

### 4. El idioma no entra en el dominio

Entra por `Accept-Language` y se resuelve en la **capa web**. Hoy la conversión de código a literal ya
ocurre allí —`PresenceResponseAssembler` está en `infrastructure/web/assembler`—, así que el idioma
llega al resolutor sin que ningún caso de uso se entere de que existen los idiomas.

Un parámetro `language` recorriendo las firmas de los puertos sería lo contrario: contratar a alguien
no tiene por qué saber en qué idioma se va a enseñar el resultado.

### 5. Si falta la traducción, se cae al literal base sin ruido

Nada en la respuesta marca campo por campo qué está traducido: eso ensucia todos los DTO para una
necesidad que es de administración.

Pero **el silencio a secas no vale**: sin nada más, no se puede distinguir «este catálogo no está
traducido» de «está traducido y da la casualidad de que suena inglés», y los huecos no se encuentran
nunca. Por eso hay un **informe de cobertura** que dice qué tipos y qué idiomas están sin traducir. El
hueco se ve donde importa —cuando alguien va a traducir— y no en cada respuesta.

### 6. Los niveles de alcance: todavía no, y con guardia mientras tanto

La solución de fondo a la duplicación es que un catálogo pueda **no colgar de ninguna
reglamentación**: `rule_entity.rule_system_code` nullable, donde `null` significa «vale para todas», y
una resolución en dos escalones — busca `(ESP, tipo, código)`, y si no está, busca
`(null, tipo, código)`. La anulación sale gratis: si ESP tiene su propia fila, gana.

**Eso no se hace ahora**, por dos razones. Toca la identidad de la tabla central del metamodelo, y se
está en mitad del rediseño de la interfaz. Y no urge: cuesta tres filas repetidas.

Dos cosas que hay que dejar escritas para cuando se haga:

- **Los niveles no van en `rule_system`.** Esa tabla tiene `country_code char(3) not null` porque un
  sistema de reglas *es* la reglamentación de un país. Meter ahí un «SISTEMA» o un «INTERNACIONAL»
  obliga a hacer esa columna nullable y diluye lo único que define la tabla. El nivel va en la fila
  del catálogo, no en el sistema de reglas.
- **Postgres trata los `NULL` como distintos dentro de un `unique`**, así que
  `unique (rule_system_code, type, code)` permitiría **varios** `HIRING` globales. Hace falta un
  índice único parcial para el caso nulo. Es una línea, de las que se descubren dos años tarde.

Y de paso, la resolución en dos escalones cae **en el mismo `findByBusinessKey`** donde va el
resolutor de traducciones: hacer las dos cosas costará poco más que hacer una.

**Mientras tanto, la guardia:** los tipos marcados como vocabulario del dominio tienen que tener el
mismo literal en todas las reglamentaciones, y **hay un test que falla si no**. Eso convierte el
riesgo real de hoy —que `HIRING/ESP` y `HIRING/FRA` se separen en silencio— en un test rojo, por el
precio de un test.

**Disparador para hacer los niveles de verdad:** cuando FRA o PRT dejen de ser semilla y tengan
regulación propia. Ahí la duplicación deja de ser tres filas y pasa a ser mantenimiento.

## Consecuencias

- El literal base que este ADR necesita **ya existe**: los catálogos de vocabulario están sembrados en
  inglés. No hay conversión previa.
- Idioma y reglamentación quedan desacoplados: se puede trabajar ESP con los literales en inglés sin
  que nada esté en un estado inconsistente. Es el estado normal mientras no haya traducciones.
- Los nombres propios salen de esta conversación: `company_profile.legal_name` ya separa el nombre
  legal del familiar, y el modelo ya los trataba aparte.
- El trabajo grande nunca fue traducir. Es el alcance, y queda aplazado a propósito y con fecha.

## Fuera de alcance

Los literales de la interfaz —títulos, cabeceras de columna, botones—, que son del frontend y ya
funcionan. El formato de fechas y números, que es locale y no idioma, y va aparte. Y la traducción de
los mensajes de error del backend, que merece su propia conversación.
