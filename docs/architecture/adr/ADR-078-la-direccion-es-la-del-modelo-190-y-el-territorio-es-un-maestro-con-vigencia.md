# ADR-078 — La dirección es la del modelo 190 y el territorio es un maestro con vigencia

## Estado
Aceptado (02/10/2026, `backend#154`). Propuesto el 30/09/2026, detrás del ADR-077. Lo que cambió al hacerlo, al final.

## Contexto

Tres tablas guardan direcciones con tres definiciones de texto libre: `employee.address` (calle, ciudad, país, CP, `region_code`), `rulesystem.company_profile` y `rulesystem.work_center_profile` (calle, ciudad, CP, país). En la semilla, «Región: ES-VC» es una comunidad y no una provincia, y «Valencia» con CP 46363 no cuadra; nadie lo sabe porque **no hay nada que pueda estar mal**. Lo dijeron los compañeros de Juan: *«no tenemos algo estándar; las provincias, las localidades, todo eso debería estar amaestrado»*.

La dirección calcula: residencia fiscal (tramo autonómico de IRPF, forales, Ceuta y Melilla), provincia del centro (CCC, afiliación), y el **modelo 190** exige la dirección del perceptor estructurada.

## Decisión

1. **La estructura de una dirección es la del modelo 190**: tipo de vía (catálogo AEAT), nombre, número, calificador, bloque, portal, escalera, planta, puerta, localidad (texto: la pedanía), municipio (código INE), provincia (código), CP, país. No se inventa una propia.
2. **La misma estructura, no la misma tabla.** El empleado vive en su esquema con vigencia; empresa y centro en la reglamentación. Las tres tablas tienen las mismas columnas, los mismos nombres, las mismas referencias y la misma validación; un candado compara las tres.
3. **Dónde vive el territorio** (sobre las capas del ADR-077): países = entidades `COUNTRY` de la capa `INT` (249, castellano); comunidades, provincias y tipos de vía = entidades de la capa nacional (`REGION`, `PROVINCE`, `STREET_TYPE`, con código INE e ISO 3166-2); **municipios y CP en `geo`**, cargados del fichero del INE, con vigencia (el INE fusiona y segrega), referenciando la provincia por código. Un municipio no es una entidad de catálogo: son 8.100 que se regeneran de la fuente, no se editan.
4. **CP**: validación dura (los dos primeros dígitos son la provincia) y municipio **sugerido** desde una fuente abierta, dicho como sugerencia.
5. **La marca «sin normalizar»**: una dirección que no cumple se guarda igual con la marca y la ficha la enseña. Un alta no se bloquea por un pueblo que no se encuentra. Se rechaza sólo lo contradictorio (CP contra provincia) o lo vacío (sin país).
6. **Fuentes, con licencia en la migración**: INE (municipios y códigos, anual), `iso-codes` (ISO 3166-1/2, castellano), AEAT (tipos de vía del diseño de registro del 190), GeoNames ES (CC-BY, sólo para sugerir).
7. Fuera de ESP: país, CP y texto; el modelo de niveles es genérico y la semilla de FRA/PRT llega con INSEE y CTT cuando toque.

## Alternativas descartadas

- **Una estructura propia.** El 190 ya dice cómo; hacer otra es tener que mapear después.
- **Google Places o la base de Correos.** El producto vive sin nube y sin licencias; INE + fuentes abiertas basta para lo que la nómina necesita.
- **Callejero (CartoCiudad).** Pesa gigas y el 190 no lo pide: la vía es texto.
- **Todo en `geo`** (también países y provincias): saca del catálogo lo que sí es catálogo (códigos con nombre, pocos, que alguien corrige a mano), y duplica lo que las capas ya resuelven.
- **Todo en `rule_entity`** (también municipios): 8.100 filas con carga anual del INE y jerarquía no son una entidad de catálogo.
- **Bloquear altas hasta normalizar.** Es lo que hace que la gente meta «Valencia» a todo.

## Consecuencias

- Camino 4 (`workspace#19`): `backend#154` (maestro), `#155` (las tres direcciones, migración con la marca y la cifra de lo que cuadra), `workforce-loader#17` (direcciones de verdad), `frontend#126` (un solo componente). Rompe la semilla: resiembra al final.
- Deja el dato para el IRPF autonómico y forales (paso 7 del camino 2), sin hacerlo.

## Al hacerlo (`backend#154`, V174–V176)

- **La estructura no es la del modelo 190, y la premisa estaba mal escrita.** El diseño de registro del 190 de la AEAT (2025) sólo le pide al perceptor el **código de provincia** (posiciones 76-77), y no trae lista de siglas de vía; el 190 foral de Gipuzkoa estructura un domicilio, pero es el de la persona presentadora. La dirección estructurada que se adopta en el §1 —tipo de vía, nombre, número, bloque, escalera, planta, puerta, CP, municipio INE, provincia— es **el domicilio estructurado que comparten la AEAT en sus modelos censales (030/036) y la TGSS en afiliación**. La decisión no cambia; cambia a quién se atribuye, que es el argumento de por qué no se inventa una propia. La provincia sigue siendo obligatoria, por el 190 y por el CCC.
- **Tipos de vía: el Anexo II del Catastro**, no el 190. «Servicios web libres de la Sede Electrónica del Catastro», versión 2.6: las 93 siglas, enteras, como entidades `STREET_TYPE` de la capa `ESP`, con el nombre tal y como lo escribe el Catastro (mayúsculas; `REGULATORY_CITATION`). Las del padrón del INE son de la misma familia.
- **Comunidades y provincias**: entidades `REGION` (19: 17 comunidades y Ceuta y Melilla) y `PROVINCE` (52) de la capa `ESP`, con el **código del INE** como código de la entidad (CODAUTO, CPRO), que es el que piden el 190 y la TGSS. El ISO 3166-2 y, en la provincia, su comunidad van en un perfil obligatorio de cada tipo (`region_profile`, `province_profile`, ADR-053), sembrado por migración y sin colección propia en el API. Ceuta y Melilla no tienen ISO de provincia: llevan el de la ciudad autónoma.
- **Municipios: vigencia «desde al menos».** El INE sirve en fichero las relaciones a 1 de enero de 2021 a 2025 (las anteriores dan 404). Lo que está en la de 2021 entra con `start_date = 2021-01-01`, y esa fecha significa *vigente desde al menos*, no *creado en*; Usansolo, con la de 2024. Entre 2021 y 2025 no hay bajas, así que **ningún municipio real tiene `end_date`** hoy: el criterio «no sale después de su `end_date`» se prueba con uno de prueba dentro de un test transaccional. No se reconstruye desde 1842; si hace falta, la fuente son las «Alteraciones de los municipios en los Censos de Población desde 1842» y la columna ya existe.
- **Un renombre no es un municipio nuevo**: mismo código, el nombre de la relación más reciente, sin historial de nombres (49 entre 2021 y 2025). La identidad es el código.
- **El procedimiento anual está escrito junto al fichero de datos** (`src/main/resources/db/geo/LEEME.md`) y lo hace `tools/geo/datos_geo.py anual <año>`: compara la relación nueva con la anterior y escribe el SQL de la migración siguiente (altas con fila nueva, bajas con `end_date`, renombres con el nombre). Los ficheros de la carga inicial quedan congelados: la V176 es una migración Java que los lee del classpath, y su checksum es el de su contenido.
- **El municipio sugerido de un CP no sale de la columna de municipio de GeoNames**: se equivoca en el 11 % de los CP comprobables (663 de 6.068; el 46250 es L'Alcúdia y dice Sagunto). Sale del nombre del lugar cuando coincide con un municipio de la misma provincia; 6.068 de los 11.150 CP tienen sugerencia, y ninguno la sugiere de otra provincia. La provincia del CP no se guarda: son sus dos primeras cifras.
- **El esquema `geo`** lleva el país en la clave (`country_code`, `code`), porque el modelo es genérico (§7), y una función `geo.search_form` que normaliza igual lo que se guarda y lo que se busca (minúsculas, sin tildes, la barra de los bilingües como espacio).
- **La API** es `GET /territory/{ruleSystemCode}/…`: `countries`, `provinces`, `street-types`, `municipalities` y `postal-codes/{cp}`. Va por reglamentación porque lo que es catálogo se lee desde sus capas (ADR-077); el código es el contexto `com.b4rrhh.geo.territory`.
