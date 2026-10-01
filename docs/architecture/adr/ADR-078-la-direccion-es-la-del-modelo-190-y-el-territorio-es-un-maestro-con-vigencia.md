# ADR-078 — La dirección es la del modelo 190 y el territorio es un maestro con vigencia

## Estado
Propuesto (30/09/2026), detrás del ADR-077.

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
