-- =========================================================
-- V174__the_territory_regions_provinces_and_street_types.sql
-- backend#154, paso 1 del camino 4 (ADR-078, sobre las capas del ADR-077)
-- =========================================================
--
-- Las comunidades autonomas, las provincias y los tipos de via son catalogo: pocos, con
-- codigo y nombre, y de Espana. Viven en la capa nacional ESP (nivel 3) como entidades de
-- tres tipos nuevos. Los municipios y los codigos postales no: son miles, se regeneran de
-- su fuente y van en el esquema geo (V175, V176).
--
-- El codigo de cada entidad es el oficial del INE, que es el que piden la Seguridad Social
-- y el modelo 190 (la provincia del perceptor, posiciones 76-77): la comunidad, CODAUTO
-- (01-19); la provincia, CPRO (01-52). El ISO 3166-2 va en el perfil de cada una, y la
-- provincia dice ademas su comunidad.
--
-- ---------------------------------------------------------
-- Fuentes
-- ---------------------------------------------------------
--   Comunidades y provincias: INE, «Relacion de comunidades autonomas y provincias»
--     (https://www.ine.es/daco/daco42/codmun/cod_ccaa_provincia.htm), consultada el
--     02/10/2026. Los nombres son los del INE, con el articulo pospuesto («Rioja, La») y
--     la forma bilingue donde la tiene («Valencia/València»): son nombres propios y no se
--     traducen (literal_class PROPER_NOUN, ADR-052).
--   ISO 3166-2: iso-codes 4.18.0 (Debian, https://salsa.debian.org/iso-codes-team/iso-codes),
--     data/iso_3166-2.json, la misma version que la V168. LGPL-2.1. Cada provincia se
--     comprobo contra su padre en iso-codes y coincide con la comunidad del INE en las 50
--     que tienen codigo de provincia. Ceuta y Melilla no lo tienen: en ISO son ciudades
--     autonomas (ES-CE, ES-ML), y su provincia lleva ese mismo codigo.
--   Tipos de via: Direccion General del Catastro, «Servicios web libres de la Sede
--     Electronica del Catastro», version 2.6, Anexo II «Tipos de via»
--     (https://www.catastro.hacienda.gob.es/ws/Webservices_Libres.pdf). Las 93 siglas,
--     enteras: es un catalogo y no se recorta a las que usamos. El nombre va tal y como lo
--     escribe el Catastro, en mayusculas, con sus tildes y sin las que le faltan
--     («POLIGONO»), para que se pueda cotejar con la fuente (REGULATORY_CITATION).
--
--   No vienen del modelo 190: el 190 de la AEAT solo pide al perceptor la provincia, y no
--   trae lista de siglas (ADR-078, «Al hacerlo»).
-- =========================================================

insert into rulesystem.rule_entity_type
    (code, name, active, literal_class, maintenance_mode, group_code, level)
values
    ('REGION', 'Region', true, 'PROPER_NOUN', 'REFERENCE', 'ORGANIZATION', 3),
    ('PROVINCE', 'Province', true, 'PROPER_NOUN', 'REFERENCE', 'ORGANIZATION', 3),
    ('STREET_TYPE', 'Street Type', true, 'REGULATORY_CITATION', 'REFERENCE', 'ORGANIZATION', 3);

insert into rulesystem.rule_entity_type_translation (rule_entity_type_code, language_code, name)
values
    ('REGION', 'es-ES', 'Comunidad autónoma'),
    ('PROVINCE', 'es-ES', 'Provincia'),
    ('STREET_TYPE', 'es-ES', 'Tipo de vía');

-- ---------------------------------------------------------
-- Los perfiles: el ISO de la comunidad; la comunidad y el ISO de la provincia
-- ---------------------------------------------------------
-- Extensiones PROFILE obligatorias (ADR-053), como la de EMPLOYEE_ADDRESS_TYPE: una
-- provincia sin comunidad no es opcional, es un dato que falta. Sin coleccion propia en el
-- API (api_collection_path nulo, V127): se siembran por migracion.

create table rulesystem.region_profile (
    id                    bigint      generated always as identity primary key,
    region_rule_entity_id bigint      not null,
    iso_3166_2            varchar(6)  not null,
    created_at            timestamp   not null default now(),
    updated_at            timestamp   not null default now(),
    constraint uk_region_profile unique (region_rule_entity_id),
    constraint fk_region_profile_rule_entity
        foreign key (region_rule_entity_id) references rulesystem.rule_entity(id) on delete cascade
);

create table rulesystem.province_profile (
    id                      bigint      generated always as identity primary key,
    province_rule_entity_id bigint      not null,
    region_code             varchar(30) not null,
    iso_3166_2              varchar(6)  not null,
    created_at              timestamp   not null default now(),
    updated_at              timestamp   not null default now(),
    constraint uk_province_profile unique (province_rule_entity_id),
    constraint fk_province_profile_rule_entity
        foreign key (province_rule_entity_id) references rulesystem.rule_entity(id) on delete cascade
);

comment on column rulesystem.province_profile.region_code is
    'Codigo INE de la comunidad (REGION) de la misma capa que la provincia.';

insert into rulesystem.rule_entity_extension
    (rule_entity_type_code, extension_code, table_name, cardinality, required)
values
    ('REGION', 'PROFILE', 'rulesystem.region_profile', '1:1', true),
    ('PROVINCE', 'PROFILE', 'rulesystem.province_profile', '1:1', true);

-- ---------------------------------------------------------
-- Las 17 comunidades y las 2 ciudades autonomas
-- ---------------------------------------------------------
create temp table v174_region (
    code varchar(2) primary key, name varchar(100) not null, iso varchar(6) not null
) on commit drop;
insert into v174_region (code, name, iso) values
    ('01', 'Andalucía', 'ES-AN'),
    ('02', 'Aragón', 'ES-AR'),
    ('03', 'Asturias, Principado de', 'ES-AS'),
    ('04', 'Balears, Illes', 'ES-IB'),
    ('05', 'Canarias', 'ES-CN'),
    ('06', 'Cantabria', 'ES-CB'),
    ('07', 'Castilla y León', 'ES-CL'),
    ('08', 'Castilla-La Mancha', 'ES-CM'),
    ('09', 'Cataluña', 'ES-CT'),
    ('10', 'Comunitat Valenciana', 'ES-VC'),
    ('11', 'Extremadura', 'ES-EX'),
    ('12', 'Galicia', 'ES-GA'),
    ('13', 'Madrid, Comunidad de', 'ES-MD'),
    ('14', 'Murcia, Región de', 'ES-MC'),
    ('15', 'Navarra, Comunidad Foral de', 'ES-NC'),
    ('16', 'País Vasco', 'ES-PV'),
    ('17', 'Rioja, La', 'ES-RI'),
    ('18', 'Ceuta', 'ES-CE'),
    ('19', 'Melilla', 'ES-ML');

insert into rulesystem.rule_entity (layer_code, rule_entity_type_code, code, name, active, start_date)
select 'ESP', 'REGION', code, name, true, date '1900-01-01' from v174_region;

insert into rulesystem.region_profile (region_rule_entity_id, iso_3166_2)
select e.id, r.iso
  from v174_region r
  join rulesystem.rule_entity e
    on e.layer_code = 'ESP' and e.rule_entity_type_code = 'REGION' and e.code = r.code;

-- ---------------------------------------------------------
-- Las 52 provincias
-- ---------------------------------------------------------
create temp table v174_province (
    code varchar(2) primary key, name varchar(100) not null,
    region varchar(2) not null references v174_region(code), iso varchar(6) not null
) on commit drop;
insert into v174_province (code, name, region, iso) values
    ('01', 'Araba/Álava', '16', 'ES-VI'),
    ('02', 'Albacete', '08', 'ES-AB'),
    ('03', 'Alicante/Alacant', '10', 'ES-A'),
    ('04', 'Almería', '01', 'ES-AL'),
    ('05', 'Ávila', '07', 'ES-AV'),
    ('06', 'Badajoz', '11', 'ES-BA'),
    ('07', 'Balears, Illes', '04', 'ES-PM'),
    ('08', 'Barcelona', '09', 'ES-B'),
    ('09', 'Burgos', '07', 'ES-BU'),
    ('10', 'Cáceres', '11', 'ES-CC'),
    ('11', 'Cádiz', '01', 'ES-CA'),
    ('12', 'Castellón/Castelló', '10', 'ES-CS'),
    ('13', 'Ciudad Real', '08', 'ES-CR'),
    ('14', 'Córdoba', '01', 'ES-CO'),
    ('15', 'Coruña, A', '12', 'ES-C'),
    ('16', 'Cuenca', '08', 'ES-CU'),
    ('17', 'Girona', '09', 'ES-GI'),
    ('18', 'Granada', '01', 'ES-GR'),
    ('19', 'Guadalajara', '08', 'ES-GU'),
    ('20', 'Gipuzkoa', '16', 'ES-SS'),
    ('21', 'Huelva', '01', 'ES-H'),
    ('22', 'Huesca', '02', 'ES-HU'),
    ('23', 'Jaén', '01', 'ES-J'),
    ('24', 'León', '07', 'ES-LE'),
    ('25', 'Lleida', '09', 'ES-L'),
    ('26', 'Rioja, La', '17', 'ES-LO'),
    ('27', 'Lugo', '12', 'ES-LU'),
    ('28', 'Madrid', '13', 'ES-M'),
    ('29', 'Málaga', '01', 'ES-MA'),
    ('30', 'Murcia', '14', 'ES-MU'),
    ('31', 'Navarra', '15', 'ES-NA'),
    ('32', 'Ourense', '12', 'ES-OR'),
    ('33', 'Asturias', '03', 'ES-O'),
    ('34', 'Palencia', '07', 'ES-P'),
    ('35', 'Palmas, Las', '05', 'ES-GC'),
    ('36', 'Pontevedra', '12', 'ES-PO'),
    ('37', 'Salamanca', '07', 'ES-SA'),
    ('38', 'Santa Cruz de Tenerife', '05', 'ES-TF'),
    ('39', 'Cantabria', '06', 'ES-S'),
    ('40', 'Segovia', '07', 'ES-SG'),
    ('41', 'Sevilla', '01', 'ES-SE'),
    ('42', 'Soria', '07', 'ES-SO'),
    ('43', 'Tarragona', '09', 'ES-T'),
    ('44', 'Teruel', '02', 'ES-TE'),
    ('45', 'Toledo', '08', 'ES-TO'),
    ('46', 'Valencia/València', '10', 'ES-V'),
    ('47', 'Valladolid', '07', 'ES-VA'),
    ('48', 'Bizkaia', '16', 'ES-BI'),
    ('49', 'Zamora', '07', 'ES-ZA'),
    ('50', 'Zaragoza', '02', 'ES-Z'),
    ('51', 'Ceuta', '18', 'ES-CE'),
    ('52', 'Melilla', '19', 'ES-ML');

insert into rulesystem.rule_entity (layer_code, rule_entity_type_code, code, name, active, start_date)
select 'ESP', 'PROVINCE', code, name, true, date '1900-01-01' from v174_province;

insert into rulesystem.province_profile (province_rule_entity_id, region_code, iso_3166_2)
select e.id, p.region, p.iso
  from v174_province p
  join rulesystem.rule_entity e
    on e.layer_code = 'ESP' and e.rule_entity_type_code = 'PROVINCE' and e.code = p.code;

-- ---------------------------------------------------------
-- Los 93 tipos de via del Catastro
-- ---------------------------------------------------------
insert into rulesystem.rule_entity (layer_code, rule_entity_type_code, code, name, active, start_date)
select 'ESP', 'STREET_TYPE', v.code, v.name, true, date '1900-01-01'
  from (values
    ('AC', 'ACCESO'),
    ('AG', 'AGREGADO'),
    ('AL', 'ALDEA, ALAMEDA'),
    ('AN', 'ANDADOR'),
    ('AR', 'AREA, ARRABAL'),
    ('AU', 'AUTOPISTA'),
    ('AV', 'AVENIDA'),
    ('AY', 'ARROYO'),
    ('BJ', 'BAJADA'),
    ('BL', 'BLOQUE'),
    ('BO', 'BARRIO'),
    ('BQ', 'BARRANQUIL'),
    ('BR', 'BARRANCO'),
    ('CA', 'CAÑADA'),
    ('CG', 'COLEGIO, CIGARRAL'),
    ('CH', 'CHALET'),
    ('CI', 'CINTURON'),
    ('CJ', 'CALLEJA, CALLEJON'),
    ('CL', 'CALLE'),
    ('CM', 'CAMINO, CARMEN'),
    ('CN', 'COLONIA'),
    ('CO', 'CONCEJO, COLEGIO'),
    ('CP', 'CAMPA, CAMPO'),
    ('CR', 'CARRETERA, CARRERA'),
    ('CS', 'CASERIO'),
    ('CT', 'CUESTA, COSTANILLA'),
    ('CU', 'CONJUNTO'),
    ('CY', 'CALEYA'),
    ('CZ', 'CALLIZO'),
    ('DE', 'DETRÁS'),
    ('DP', 'DIPUTACION'),
    ('DS', 'DISEMINADOS'),
    ('ED', 'EDIFICIOS'),
    ('EM', 'EXTRAMUROS'),
    ('EN', 'ENTRADA, ENSANCHE'),
    ('EP', 'ESPALDA'),
    ('ER', 'EXTRARRADIO'),
    ('ES', 'ESCALINATA'),
    ('EX', 'EXPLANADA'),
    ('FC', 'FERROCARRIL'),
    ('FN', 'FINCA'),
    ('GL', 'GLORIETA'),
    ('GR', 'GRUPO'),
    ('GV', 'GRAN VIA'),
    ('HT', 'HUERTA, HUERTO'),
    ('JR', 'JARDINES'),
    ('LA', 'LAGO'),
    ('LD', 'LADO, LADERA'),
    ('LG', 'LUGAR'),
    ('MA', 'MALECON'),
    ('MC', 'MERCADO'),
    ('ML', 'MUELLE'),
    ('MN', 'MUNICIPIO'),
    ('MS', 'MASIAS'),
    ('MT', 'MONTE'),
    ('MZ', 'MANZANA'),
    ('PB', 'POBLADO'),
    ('PC', 'PLACETA'),
    ('PD', 'PARTIDA'),
    ('PI', 'PARTICULAR'),
    ('PJ', 'PASAJE, PASADIZO'),
    ('PL', 'POLIGONO'),
    ('PM', 'PARAMO'),
    ('PQ', 'PARROQUIA, PARQUE'),
    ('PR', 'PROLONGACION, CONTINUAC.'),
    ('PS', 'PASEO'),
    ('PT', 'PUENTE'),
    ('PU', 'PASADIZO'),
    ('PZ', 'PLAZA'),
    ('QT', 'QUINTA'),
    ('RA', 'RACONADA'),
    ('RB', 'RAMBLA'),
    ('RC', 'RINCON, RINCONA'),
    ('RD', 'RONDA'),
    ('RM', 'RAMAL'),
    ('RP', 'RAMPA'),
    ('RR', 'RIERA'),
    ('RU', 'RUA'),
    ('SA', 'SALIDA'),
    ('SC', 'SECTOR'),
    ('SD', 'SENDA'),
    ('SL', 'SOLAR'),
    ('SN', 'SALON'),
    ('SU', 'SUBIDA'),
    ('TN', 'TERRENOS'),
    ('TO', 'TORRENTE'),
    ('TR', 'TRAVESIA'),
    ('UR', 'URBANIZACION'),
    ('VA', 'VALLE'),
    ('VD', 'VIADUCTO'),
    ('VI', 'VIA'),
    ('VL', 'VIAL'),
    ('VR', 'VEREDA')
       ) as v(code, name);

do $$
declare
    n_region bigint;
    n_province bigint;
    n_street bigint;
    sin_comunidad bigint;
begin
    select count(*) filter (where rule_entity_type_code = 'REGION'),
           count(*) filter (where rule_entity_type_code = 'PROVINCE'),
           count(*) filter (where rule_entity_type_code = 'STREET_TYPE')
      into n_region, n_province, n_street
      from rulesystem.rule_entity
     where layer_code = 'ESP';
    if n_region <> 19 or n_province <> 52 or n_street <> 93 then
        raise exception 'backend#154: % comunidades, % provincias y % tipos de via; se esperaban 19, 52 y 93',
            n_region, n_province, n_street;
    end if;

    select count(*) into sin_comunidad
      from rulesystem.rule_entity e
     where e.rule_entity_type_code = 'PROVINCE'
       and not exists (
           select 1
             from rulesystem.province_profile p
             join rulesystem.rule_entity r
               on r.layer_code = e.layer_code and r.rule_entity_type_code = 'REGION' and r.code = p.region_code
            where p.province_rule_entity_id = e.id);
    if sin_comunidad > 0 then
        raise exception 'backend#154: % provincia(s) sin una comunidad de su misma capa', sin_comunidad;
    end if;
end $$;
