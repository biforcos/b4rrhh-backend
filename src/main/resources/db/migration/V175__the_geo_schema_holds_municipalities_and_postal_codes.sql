-- =========================================================
-- V175__the_geo_schema_holds_municipalities_and_postal_codes.sql
-- backend#154, paso 1 del camino 4 (ADR-078)
-- =========================================================
--
-- Lo que del territorio no es catalogo: los municipios y los codigos postales. Son miles,
-- se regeneran de su fuente y no se editan a mano, asi que no son entidades de rule_entity.
-- Las comunidades, las provincias y los tipos de via si lo son, y viven en la capa ESP
-- (V174). Aqui la provincia se nombra por su codigo INE.
--
-- Las tablas estan vacias al salir de esta migracion: los datos los carga la V176 desde
-- sus ficheros (src/main/resources/db/geo), que es donde dicen de donde salen y como se
-- regeneran.
--
-- El pais va en la clave porque el modelo es generico (ADR-078 §7): la semilla de FRA y PRT
-- llegara con INSEE y CTT. Hoy solo hay ESP, y las comprobaciones de formato son las de ESP.
-- =========================================================

create schema geo;

-- La forma en que se busca un nombre: minusculas, sin tildes, y la barra de los nombres
-- bilingues y los apostrofos como espacios. «Valencia» encuentra «Valencia»; «castellon»,
-- «Castello de la Plana/Castellon de la Plana». La usa la columna calculada y la usa la
-- consulta, para que las dos digan lo mismo.
create function geo.search_form(texto text) returns text
    language sql immutable parallel safe
    return translate(lower(texto),
                     'áàâäãéèêëíìîïóòôöõúùûüçñ·''/-',
                     'aaaaaeeeeiiiiooooouuuucn.   ');

-- ---------------------------------------------------------
-- Municipios
-- ---------------------------------------------------------
-- Vigencia «desde al menos»: start_date no es la fecha en que se creo el municipio, sino la
-- de la primera relacion del INE cargada en la que aparece (ADR-078, «Al hacerlo»). Un
-- renombre no es un municipio nuevo: mismo codigo, nombre de la relacion mas reciente.
create table geo.municipality (
    country_code  varchar(3)   not null,
    code          varchar(10)  not null,
    name          varchar(100) not null,
    province_code varchar(10)  not null,
    start_date    date         not null,
    end_date      date,
    search_name   text generated always as (geo.search_form(name)) stored,
    constraint pk_municipality primary key (country_code, code),
    constraint chk_municipality_dates check (end_date is null or end_date >= start_date),
    constraint chk_municipality_esp_code
        check (country_code <> 'ESP' or (code ~ '^[0-9]{5}$' and province_code = left(code, 2)))
);

comment on column geo.municipality.start_date is
    'Vigente desde al menos esta fecha: la de la primera relacion del INE cargada que lo trae. '
    'No es la fecha de creacion del municipio.';
comment on column geo.municipality.province_code is
    'Codigo INE de la provincia, una entidad PROVINCE de la capa nacional (V174).';

-- ---------------------------------------------------------
-- Codigos postales
-- ---------------------------------------------------------
-- La provincia de un codigo postal espanol no se guarda: son sus dos primeras cifras,
-- siempre. Lo que se guarda es el municipio que sugiere la fuente, y es una sugerencia:
-- la columna se llama asi.
create table geo.postal_code (
    country_code                varchar(3)  not null,
    code                        varchar(10) not null,
    suggested_municipality_code varchar(10),
    constraint pk_postal_code primary key (country_code, code),
    constraint fk_postal_code_suggested_municipality
        foreign key (country_code, suggested_municipality_code)
        references geo.municipality (country_code, code),
    constraint chk_postal_code_esp
        check (country_code <> 'ESP'
               or (code ~ '^[0-9]{5}$'
                   and (suggested_municipality_code is null
                        or left(suggested_municipality_code, 2) = left(code, 2))))
);
