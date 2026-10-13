-- =========================================================
-- V177__the_three_addresses_share_one_structure_and_say_if_they_are_normalized.sql
-- backend#155, paso 2 del camino 4 (ADR-078)
-- =========================================================
--
-- Tres tablas guardaban direcciones con tres definiciones de texto libre: employee.address
-- (calle, ciudad, pais, CP y un region_code libre), rulesystem.company_profile y
-- rulesystem.work_center_profile (calle, ciudad, CP, region_code, pais). Desde aqui las tres
-- guardan la misma estructura, la del domicilio de la AEAT y la TGSS: los mismos campos,
-- con los mismos nombres y los mismos tipos. Misma estructura, no misma tabla: el empleado
-- sigue en su esquema con su vigencia, y empresa y centro en la reglamentacion.
-- AddressStructureIsTheSameInTheThreeTablesTest lo comprueba sobre el esquema real.
--
--   street_type_code   tipo de via, una entidad STREET_TYPE de la capa ESP (V174)
--   street_name        nombre de la via
--   street_number      numero
--   number_qualifier   calificador del numero: bis, km...
--   block, stair, floor, door
--   locality           texto libre: la pedania, o la poblacion de una direccion de fuera
--   municipality_code  municipio, codigo INE de geo.municipality (V175)
--   subdivision_code   la provincia, codigo INE de una entidad PROVINCE de la capa ESP
--   postal_code
--   country_code       una entidad COUNTRY de la capa INT (V168)
--   normalized         si cumple la validacion; «sin normalizar» cuando no
--
-- Las referencias se validan en la aplicacion y no son claves ajenas: una direccion que no
-- cumple se guarda igual, con normalized = false, y la ficha lo enseña. Un alta no se bloquea
-- por un pueblo que no se encuentra (ADR-078 §5). La provincia se guarda aunque se deduzca
-- del municipio, porque una direccion de fuera de España no tiene municipio INE.
--
-- La validacion, la misma en los tres sitios (geo.address, PostalAddressValidator):
--   en España: municipio que existe y no ha terminado en la fecha; provincia la del
--   municipio; CP de cinco cifras de esa provincia; tipo de via del catalogo. Fuera de
--   España, el pais y nada mas. Se rechaza solo la direccion sin pais y el CP que contradice
--   a la provincia.
--
-- ---------------------------------------------------------
-- Lo que habia: como se pasa, y cuanto cuadra
-- ---------------------------------------------------------
-- La calle se parte en tipo, nombre, numero, planta y puerta cuando tiene la forma fija de
-- las que siembra el loader («Calle Real, 84 - 7C»). Si no, entera a street_name.
-- La ciudad se busca entre los municipios de la provincia del CP por su nombre, sin tildes
-- ni mayusculas, por cada lado de los nombres bilingues («Alacant/Alicante») y con el
-- articulo delante («Coruña, A» es «A Coruña»). Si no aparece exactamente uno, la ciudad se
-- queda en locality, que no se tira nada. region_code deja de existir: en la semilla era la
-- comunidad (ES-VC) y no la provincia. La provincia sale del municipio, o del CP si no hay.
--
-- Sobre la semilla (b4rrhh_semilla, 1.000 empleados, medida al hacerlo):
--   employee.address:            1377 direcciones, 1377 normalizadas y 0 sin normalizar
--   rulesystem.company_profile:  4 perfiles,     4 normalizados y 0 sin normalizar
--   rulesystem.work_center_profile: 15 perfiles,  14 normalizados y 1 sin normalizar
-- El que queda sin normalizar es el centro REMOTE de ESP, que no tiene direccion ninguna.
-- Lo que la validacion no mira, porque el municipio que da el CP es una sugerencia (ADR-078 §4):
-- de las 1.341 direcciones españolas del loader, 990 llevan un CP que no existe y 308 uno de
-- otro municipio de la provincia; solo 43 un CP de su municipio. El loader pone la provincia y
-- tres cifras al azar (workforce-loader#17).
-- TheAddressMigrationSaysWhatItLeftOnTheSeedTest lo comprueba contra una copia migrada de la
-- semilla cuando se le da (no hay semilla en la integracion continua).
-- =========================================================

-- ---------------------------------------------------------
-- Las columnas, las mismas en las tres tablas
-- ---------------------------------------------------------
alter table employee.address
    add column street_type_code  varchar(30),
    add column street_name       varchar(200),
    add column street_number     varchar(10),
    add column number_qualifier  varchar(10),
    add column block             varchar(10),
    add column stair             varchar(10),
    add column floor             varchar(10),
    add column door              varchar(10),
    add column locality          varchar(120),
    add column municipality_code varchar(10),
    add column subdivision_code  varchar(10),
    add column normalized        boolean;

alter table rulesystem.company_profile
    add column street_type_code  varchar(30),
    add column street_name       varchar(200),
    add column street_number     varchar(10),
    add column number_qualifier  varchar(10),
    add column block             varchar(10),
    add column stair             varchar(10),
    add column floor             varchar(10),
    add column door              varchar(10),
    add column locality          varchar(120),
    add column municipality_code varchar(10),
    add column subdivision_code  varchar(10),
    add column normalized        boolean;

alter table rulesystem.work_center_profile
    add column street_type_code  varchar(30),
    add column street_name       varchar(200),
    add column street_number     varchar(10),
    add column number_qualifier  varchar(10),
    add column block             varchar(10),
    add column stair             varchar(10),
    add column floor             varchar(10),
    add column door              varchar(10),
    add column locality          varchar(120),
    add column municipality_code varchar(10),
    add column subdivision_code  varchar(10),
    add column normalized        boolean;

-- El pais y el CP ya estaban, con tipos distintos: char(3) en los perfiles, varchar(3) en el
-- empleado. Uno solo.
alter table rulesystem.company_profile alter column country_code type varchar(3);
alter table rulesystem.work_center_profile alter column country_code type varchar(3);

-- ---------------------------------------------------------
-- Las piezas de la migracion: viven lo que dura la sesion
-- ---------------------------------------------------------

-- El tipo de via de una palabra («Travesia»): la sigla cuyo nombre del Catastro la trae, si
-- es una sola. «COLEGIO» esta en dos siglas y no da ninguna.
create function pg_temp.street_type_of(word text) returns varchar
    language sql stable
    return (select min(re.code)
              from rulesystem.rule_entity re
             where re.layer_code = 'ESP'
               and re.rule_entity_type_code = 'STREET_TYPE'
               and geo.search_form(word) = any (string_to_array(geo.search_form(re.name), ', '))
            having count(*) = 1);

-- El municipio de una ciudad dentro de una provincia, si es exactamente uno.
create function pg_temp.municipality_of(city text, province varchar, at date) returns varchar
    language sql stable
    return (select min(m.code)
              from geo.municipality m
             where m.country_code = 'ESP'
               and m.province_code = province
               and (m.end_date is null or m.end_date >= at)
               and exists (
                     select 1
                       from unnest(string_to_array(m.name, '/')) as part(name)
                      where geo.search_form(city) in (
                                geo.search_form(part.name),
                                geo.search_form(regexp_replace(part.name, '^(.*), (\S+)$', '\2 \1'))))
            having count(*) = 1);

-- La provincia de un CP español: sus dos primeras cifras, si es una provincia del catalogo.
create function pg_temp.province_of_postal_code(postal_code varchar) returns varchar
    language sql stable
    return (select re.code
              from rulesystem.rule_entity re
             where re.layer_code = 'ESP'
               and re.rule_entity_type_code = 'PROVINCE'
               and postal_code ~ '^[0-9]{5}$'
               and re.code = left(postal_code, 2));

-- La validacion de ADR-078 §5, la misma que PostalAddressValidator.
create function pg_temp.is_normalized(
        country varchar, municipality varchar, subdivision varchar, postal_code varchar,
        street_type varchar, at date) returns boolean
    language sql stable
    return case
        when country is null then false
        when country <> 'ESP' then municipality is null and subdivision is null and street_type is null
        else coalesce((
              select subdivision = m.province_code
                     and postal_code ~ '^[0-9]{5}$'
                     and left(postal_code, 2) = m.province_code
                     and (m.end_date is null or m.end_date >= at)
                     and exists (select 1 from rulesystem.rule_entity re
                                  where re.layer_code = 'ESP' and re.rule_entity_type_code = 'PROVINCE'
                                    and re.code = subdivision)
                     and exists (select 1 from rulesystem.rule_entity re
                                  where re.layer_code = 'ESP' and re.rule_entity_type_code = 'STREET_TYPE'
                                    and re.code = street_type)
                from geo.municipality m
               where m.country_code = 'ESP' and m.code = municipality), false)
    end;

-- ---------------------------------------------------------
-- La calle partida, la ciudad buscada, la marca puesta: en las tres
-- ---------------------------------------------------------
-- Dos formas: la del loader, «<Tipo> <nombre>, <numero>[ - <planta><puerta>]», y la de las
-- empresas y centros sembrados por migracion (V51), «<Tipo> <nombre> <numero>».
create temporary table legacy_address (
    source           text    not null,
    id               bigint  not null,
    country_code     varchar(3),
    postal_code      varchar(20),
    at               date    not null,
    street           text,
    city             text,
    parts            text[],
    street_type_code varchar(30),
    municipality_code varchar(10),
    subdivision_code varchar(10),
    primary key (source, id)
) on commit drop;

insert into legacy_address (source, id, country_code, postal_code, at, street, city)
select 'employee.address', id, country_code, postal_code, start_date, street, city
  from employee.address
union all
select 'rulesystem.company_profile', id, country_code, postal_code, current_date, street, city
  from rulesystem.company_profile
union all
select 'rulesystem.work_center_profile', id, country_code, postal_code, current_date, street, city
  from rulesystem.work_center_profile;

update legacy_address
   set parts = coalesce(
           regexp_match(street, '^(\S+)\s+([^,]+),\s*([0-9]+[A-Za-z]?)(?:\s*-\s*([0-9]+)([A-Za-z]))?$'),
           regexp_match(street, '^(\S+)\s+([^,]+)\s+([0-9]+[A-Za-z]?)$'));

update legacy_address
   set street_type_code = case when country_code = 'ESP' then pg_temp.street_type_of(parts[1]) end,
       municipality_code = case when country_code = 'ESP'
                                then pg_temp.municipality_of(city, pg_temp.province_of_postal_code(postal_code), at) end;

update legacy_address la
   set subdivision_code = case when la.country_code <> 'ESP' then null
                               when la.municipality_code is not null
                               then (select m.province_code from geo.municipality m
                                      where m.country_code = 'ESP' and m.code = la.municipality_code)
                               else pg_temp.province_of_postal_code(la.postal_code) end;

-- Cuando el tipo de via se reconoce, el nombre es lo que va detras; si no, la calle entera
-- es el nombre (fuera de España no hay tipo: «Rua Direita» se queda junta).
create temporary view legacy_address_parsed as
select la.*,
       case when la.parts is null then la.street
            when la.street_type_code is not null then la.parts[2]
            else la.parts[1] || ' ' || la.parts[2] end      as street_name,
       la.parts[3]                                          as street_number,
       la.parts[4]                                          as floor,
       upper(la.parts[5])                                   as door,
       case when la.municipality_code is null then la.city end as locality
  from legacy_address la;

update employee.address x
   set street_type_code = p.street_type_code, street_name = p.street_name,
       street_number = p.street_number, floor = p.floor, door = p.door,
       locality = p.locality, municipality_code = p.municipality_code,
       subdivision_code = p.subdivision_code,
       normalized = pg_temp.is_normalized(p.country_code, p.municipality_code, p.subdivision_code,
                                          p.postal_code, p.street_type_code, p.at)
  from legacy_address_parsed p
 where p.source = 'employee.address' and p.id = x.id;

update rulesystem.company_profile x
   set street_type_code = p.street_type_code, street_name = p.street_name,
       street_number = p.street_number, floor = p.floor, door = p.door,
       locality = p.locality, municipality_code = p.municipality_code,
       subdivision_code = p.subdivision_code,
       normalized = pg_temp.is_normalized(p.country_code, p.municipality_code, p.subdivision_code,
                                          p.postal_code, p.street_type_code, p.at)
  from legacy_address_parsed p
 where p.source = 'rulesystem.company_profile' and p.id = x.id;

update rulesystem.work_center_profile x
   set street_type_code = p.street_type_code, street_name = p.street_name,
       street_number = p.street_number, floor = p.floor, door = p.door,
       locality = p.locality, municipality_code = p.municipality_code,
       subdivision_code = p.subdivision_code,
       normalized = pg_temp.is_normalized(p.country_code, p.municipality_code, p.subdivision_code,
                                          p.postal_code, p.street_type_code, p.at)
  from legacy_address_parsed p
 where p.source = 'rulesystem.work_center_profile' and p.id = x.id;

drop view legacy_address_parsed;

-- ---------------------------------------------------------
-- Lo que deja de existir, y la marca, obligatoria
-- ---------------------------------------------------------
alter table employee.address
    drop column street,
    drop column city,
    drop column region_code,
    alter column normalized set not null;

alter table rulesystem.company_profile
    drop column street,
    drop column city,
    drop column region_code,
    alter column normalized set not null,
    alter column normalized set default false;

alter table rulesystem.work_center_profile
    drop column street,
    drop column city,
    drop column region_code,
    alter column normalized set not null,
    alter column normalized set default false;

alter table employee.address alter column normalized set default false;

comment on column employee.address.normalized is
    'Si la direccion cumple la validacion de ADR-078 §5. false es «sin normalizar»: se guardo igual y la ficha lo enseña.';
comment on column rulesystem.company_profile.normalized is
    'Si la direccion cumple la validacion de ADR-078 §5. false es «sin normalizar»: se guardo igual y la ficha lo enseña.';
comment on column rulesystem.work_center_profile.normalized is
    'Si la direccion cumple la validacion de ADR-078 §5. false es «sin normalizar»: se guardo igual y la ficha lo enseña.';
