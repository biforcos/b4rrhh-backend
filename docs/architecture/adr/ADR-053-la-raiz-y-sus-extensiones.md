# ADR-053 — La raíz y sus extensiones

## Estado
Aceptado

## Contexto

Una entidad de reglamentación es la tripla **(tipo de entidad, sistema de reglas, código)**, y vive
en `rule_entity` con su literal. La mayoría no necesita nada más: un motivo de baja, un tipo de
dirección, un tipo de contacto son código y literal, y con eso se validan contra el metamodelo.

Pero algunos tipos tienen más que decir. Una empresa tiene nombre legal; un centro de trabajo tiene
contactos; un convenio tiene su perfil. La salida fácil sería sacarlos de `rule_entity` y darles
tabla propia — una para empresas, otra para centros, otra para centros de coste — y esa es
exactamente la trampa: cada maestro nuevo pasaría a ser un esquema nuevo.

La aplicación ya eligió lo contrario, y funciona: **la raíz se queda y la información extendida
cuelga de ella**.

```
rule_entity  ←──  company_profile              (1:1)
             ←──  work_center_profile          (1:1)
             ←──  agreement_profile            (1:1)
             ←──  agreement_category_profile   (1:1)
             ←──  work_center_contact          (1:N)
             ←──  agreement_category_relation
             ←──  contract_subtype_relation
             ←──  rule_entity_translation      (1:N por idioma)
```

**Y esa decisión ya se ha pagado sola.** Todo el ADR-052 —la tabla de traducciones, el resolutor
único de literales, los participantes de uso— funciona porque la raíz es uniforme: cuelgan de
`rule_entity(id)` sin preguntar si eso es un motivo de baja o una empresa. Con tabla por maestro
harían falta tantas tablas de traducciones como maestros, y un resolutor que primero averiguara qué
está resolviendo.

### Lo que falta

El patrón está en el esquema pero **no está declarado en ninguna parte**, y se nota en tres sitios:

**Nada obliga a que una extensión obligatoria exista.** La clave ajena va del perfil a la raíz, no al
revés, así que se puede insertar una `COMPANY` sin `company_profile`. El código ya lo da por hecho:

```java
// ListCompaniesService
String legalName = profile.map(CompanyProfile::getLegalName).orElse(companyEntity.getName());
```

Ese `orElse` es la confesión: *el nombre legal es el del perfil, o el de la raíz si no hay perfil*.
«Una empresa tiene nombre legal» es una regla que vive en la intención, y cuando se incumple la
aplicación no se rompe: **enseña otra cosa parecida**.

**Nada dice qué pasa al borrar la raíz.** `rule_entity_translation` nació sin cascada y hubo que
añadírsela (backend#26) después de descubrir que borrar un código traducido reventaba con una
violación de integridad. `work_center_contact` sigue igual hoy.

**La navegación se escribe a mano.** El menú saca Empresas, Centros de trabajo y Centros de coste
fuera de Catálogos, pero Centros de coste **no tiene perfil**, así que o le falta uno o está en el
sitio equivocado — y sin una regla escrita, esa pregunta no tiene respuesta, sólo opiniones.

## Decisión

### 1. La raíz no se abandona nunca

Todo tipo de entidad vive en `rule_entity`, tenga las propiedades que tenga. **Ninguna extensión
saca a nadie de la raíz**: la añade.

### 2. Las extensiones se declaran

```sql
create table rulesystem.rule_entity_extension (
    rule_entity_type_code varchar(30) not null references rulesystem.rule_entity_type(code),
    extension_code        varchar(30) not null,   -- PROFILE, CONTACTS…
    table_name            varchar(63) not null,   -- rulesystem.company_profile
    cardinality           varchar(3)  not null,   -- '1:1' | '1:N'
    required              boolean     not null,
    primary key (rule_entity_type_code, extension_code)
);
```

```sql
('COMPANY',     'PROFILE',  'rulesystem.company_profile',     '1:1', true),
('WORK_CENTER', 'PROFILE',  'rulesystem.work_center_profile', '1:1', true),
('WORK_CENTER', 'CONTACTS', 'rulesystem.work_center_contact', '1:N', false),
('AGREEMENT',   'PROFILE',  'rulesystem.agreement_profile',   '1:1', true)
```

**La ausencia significa «sólo raíz».** `EMPLOYEE_PRESENCE_ENTRY_REASON`, `CONTACT_TYPE`,
`EMPLOYEE_ADDRESS_TYPE` y la mayoría no aparecen, y eso es correcto: el caso común cuesta cero filas.
Un metamodelo que obligue a declarar lo normal se abandona el segundo mes.

### 3. Una extensión siempre cae en cascada

Si el tipo declara una extensión, su clave ajena a `rule_entity(id)` lleva `on delete cascade`. Sin
excepciones, y **por eso no hay ninguna columna que lo configure**.

El motivo no es que la cascada sea siempre cómoda, es que **una extensión es algo poseído por la
raíz**: no significa nada sin ella y conservarla no reconstruye nada. Si la raíz se borró por error,
la que se cree después tendrá otro identificador, así que los satélites viejos no vuelven a engancharse
ni queriendo. Lo único que se conserva al no borrarlos es basura inalcanzable.

**Y de ahí la regla que separa los dos casos que es fácil confundir:**

| | Qué es | Al borrar la raíz |
|---|---|---|
| **Extensión** | Lo posee la raíz; no existe sin ella | **Siempre cascada.** Se declara aquí |
| **Uso** | Otra cosa que la señala, normalmente por código en texto (`entry_reason_code`) | **Siempre bloquea.** Lo comprueba `RuleEntityUsageCheckPort` (ADR-052, backend#28) |

**Si algo no se puede borrar en cascada, es que no es una extensión: es un uso**, y no pertenece a
esta tabla.

### 4. El metamodelo describe; no ejecuta

Lo leen las guardias, la navegación, las lecturas genéricas y el aviso de borrado. **No genera
escrituras.**

Un tipo con extensiones tiene endpoints propios escritos a mano —`CreateCompanyService` ya existe— y
eso no es una renuncia a la escalabilidad, porque **las dos poblaciones crecen a ritmos distintos**:
los maestros simples crecen rápido y son triviales, y los tipos con extensiones crecen despacio y cada
uno trae reglas de dominio de verdad. Un CIF se valida; una ciudad no. El coste está acotado por la
naturaleza del dominio y el camino genérico absorbe el crecimiento.

Se descarta expresamente el endpoint genérico que acepte las extensiones como carga anidada: la
validación de negocio de cada extensión acabaría en una tabla de reglas —y entonces se ha construido
un intérprete— o en código por tipo, y entonces hay **un endpoint que parece genérico con código
secreto dentro**, que es peor que uno específico y honesto.

### 5. Sólo entra lo que una guardia pueda comprobar

Los metamodelos tienen una forma conocida de morir: crecen hasta ser un segundo lenguaje que nadie
sabe depurar. Para evitarlo, el criterio de admisión de cualquier columna futura no es si es útil,
sino **qué test falla cuando esté mal**. Sin esa respuesta, se queda fuera.

### 6. Las guardias

1. **Cada `table_name` declarada existe** y tiene clave ajena a `rule_entity(id)`. Caza declaraciones
   caducadas.
2. **Cada tabla con clave ajena a `rule_entity(id)` está declarada.** El inverso, para que un satélite
   nuevo no entre sin que nadie lo vea. Es la misma lección que
   `EveryCatalogColumnIsDeclaredOrExemptedTest` (backend#29).
3. **Toda extensión `required` tiene fila para cada raíz de su tipo.** Es la guardia que hoy no existe
   y que ese `orElse` está tapando.
4. **Toda tabla declarada tiene `on delete cascade`.** No compara contra una columna —no hay columna—:
   lo afirma. Habría hecho fallar la V104 en su propio commit, en vez de esperar a que alguien borrara
   un código traducido.

### 7. Lo que se deriva

- **La navegación.** Un tipo que aparece en `rule_entity_extension` tiene pantalla propia; uno que no,
  vive en Catálogos. Se acaba la lista escrita a mano.
- **La lectura genérica.** Un visor puede traer cualquier entidad con sus extensiones sin código por
  tipo, que es lo que la pantalla de Catálogos necesita para su detalle.
- **El aviso de borrado.** Del recuento de las extensiones 1:N sale «esto borrará también 3
  contactos». No hace falta una política: hace falta contar.

## Consecuencias

- Añadir un maestro simple sigue costando cero esquema: una fila en `rule_entity` y nada más.
- Añadir propiedades a un tipo no migra ningún dato: se añade una extensión y se declara.
- La primera ejecución de la guardia 3 dirá si hay raíces sin su extensión obligatoria. **No se
  espera que salga cero**, y lo que salga es deuda real que hoy nadie ve.
- `work_center_contact` necesita su cascada, que hoy no tiene.
- El menú deja de poder contradecir al modelo, porque sale de él.

## Fuera de alcance

La escritura genérica, descartada arriba. La agrupación y visibilidad de los tipos en la pantalla de
Catálogos, y la clase del literal del ADR-052 §2: son metadatos del **tipo**, no de sus extensiones, y
van en `rule_entity_type` (backend#15). Y el nombre de la pantalla —«Maestros» en el menú, «Catálogos»
en el título—, que es una decisión de vocabulario y no de modelo.
