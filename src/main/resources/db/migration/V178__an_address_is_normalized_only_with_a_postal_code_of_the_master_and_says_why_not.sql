-- =========================================================
-- V178__an_address_is_normalized_only_with_a_postal_code_of_the_master_and_says_why_not.sql
-- backend#155, segunda vuelta (comentario de revision del 13/10/2026)
-- =========================================================
--
-- La V177 daba por normalizada una direccion cuyo CP tuviera las dos cifras de su provincia,
-- existiera o no. Con esa regla las 1.377 direcciones de la semilla salian normalizadas, y casi
-- todas llevaban un CP inventado por el loader: era un fallo presentado como un hecho. La regla
-- cambia, y esta migracion la aplica a lo que ya hay:
--
--   1. normalized solo es verdad si el municipio existe y esta vigente, la provincia es la del
--      municipio, el CP EXISTE en el maestro geo.postal_code y es de esa provincia, y el tipo de
--      via esta en el catalogo. Un CP que existe y sugiere otro municipio sigue normalizado: la
--      sugerencia es sugerencia (ADR-078 §4).
--   2. normalization_issue guarda el motivo, el primero que falla en ese orden, para que la ficha
--      diga por que y no solo que no. Los codigos son los de PostalAddressIssue.
--   3. portal entra en las tres tablas: es del domicilio estructurado AEAT/TGSS y faltaba.
--
-- Nada se bloquea: cambia lo que la marca significa, no lo que se guarda. Las tres tablas
-- siguen con las mismas columnas (AddressStructureIsTheSameInTheThreeTablesTest).
--
-- ---------------------------------------------------------
-- Cuanto queda sin normalizar con la regla nueva
-- ---------------------------------------------------------
-- Sobre la semilla (copia de b4rrhh_semilla, 1.000 empleados, migrada hasta aqui):
--   employee.address:            1377 direcciones, 387 normalizadas y 990 sin normalizar
--   rulesystem.company_profile:  4 perfiles,     4 normalizados y 0 sin normalizar
--   rulesystem.work_center_profile: 15 perfiles,  14 normalizados y 1 sin normalizar
-- Por motivo:
--   employee.address CP_DESCONOCIDO: 990
--   rulesystem.work_center_profile MUNICIPIO_DESCONOCIDO: 1
-- Las 990 son direcciones españolas del loader con un CP que no existe: pone las dos cifras de la
-- provincia y tres al azar. Otras 36 con CP inexistente son de Portugal y Francia, y fuera de
-- España solo cuenta el pais. El centro es REMOTE de ESP, que tiene pais y nada mas.
-- Es la verdad de la semilla de hoy, y la arregla el loader (workforce-loader#17).
-- TheAddressMigrationSaysWhatItLeftOnTheSeedTest lo comprueba contra una copia migrada.
-- =========================================================

alter table employee.address
    add column portal              varchar(10),
    add column normalization_issue varchar(40);

alter table rulesystem.company_profile
    add column portal              varchar(10),
    add column normalization_issue varchar(40);

alter table rulesystem.work_center_profile
    add column portal              varchar(10),
    add column normalization_issue varchar(40);

-- La regla de PostalAddressValidator, en el mismo orden. Vive lo que dura la sesion.
create function pg_temp.issue_of(
        country varchar, municipality varchar, subdivision varchar, postal_code varchar,
        street_type varchar, at date) returns varchar
    language sql stable
    return case
        when country <> 'ESP' then
            case when municipality is null and subdivision is null and street_type is null then null
                 else 'DATOS_DE_ESPANA_FUERA_DE_ESPANA' end
        when not exists (select 1 from geo.municipality m
                          where m.country_code = 'ESP' and m.code = municipality)
            then 'MUNICIPIO_DESCONOCIDO'
        when exists (select 1 from geo.municipality m
                      where m.country_code = 'ESP' and m.code = municipality
                        and m.end_date is not null and m.end_date < at)
            then 'MUNICIPIO_NO_VIGENTE'
        when not exists (select 1 from rulesystem.rule_entity re
                          where re.layer_code = 'ESP' and re.rule_entity_type_code = 'PROVINCE'
                            and re.code = subdivision)
            then 'PROVINCIA_DESCONOCIDA'
        when not exists (select 1 from geo.municipality m
                          where m.country_code = 'ESP' and m.code = municipality
                            and m.province_code = subdivision)
            then 'PROVINCIA_DE_OTRO_MUNICIPIO'
        when postal_code is null or postal_code !~ '^[0-9]{5}$'
             or not exists (select 1 from geo.postal_code pc
                             where pc.country_code = 'ESP' and pc.code = postal_code)
            then 'CP_DESCONOCIDO'
        when left(postal_code, 2) <> subdivision
            then 'CP_DE_OTRA_PROVINCIA'
        when street_type is null
             or not exists (select 1 from rulesystem.rule_entity re
                             where re.layer_code = 'ESP' and re.rule_entity_type_code = 'STREET_TYPE'
                               and re.code = street_type)
            then 'TIPO_DE_VIA_DESCONOCIDO'
    end;

-- Una fila sin pais es un perfil sin direccion: la validacion no deja guardar una direccion sin
-- pais, asi que las que hay son empresas o centros que no la tienen. Su motivo es ese.
update employee.address
   set normalization_issue = pg_temp.issue_of(country_code, municipality_code, subdivision_code,
                                              postal_code, street_type_code, start_date);

update rulesystem.company_profile
   set normalization_issue = case when country_code is null then 'SIN_DIRECCION'
                                  else pg_temp.issue_of(country_code, municipality_code, subdivision_code,
                                                        postal_code, street_type_code, current_date) end;

update rulesystem.work_center_profile
   set normalization_issue = case when country_code is null then 'SIN_DIRECCION'
                                  else pg_temp.issue_of(country_code, municipality_code, subdivision_code,
                                                        postal_code, street_type_code, current_date) end;

update employee.address set normalized = normalization_issue is null;
update rulesystem.company_profile set normalized = normalization_issue is null;
update rulesystem.work_center_profile set normalized = normalization_issue is null;

-- Una fila escrita a mano, sin pasar por la validacion, no se sabe si cumple: por omision queda
-- sin normalizar y lo dice. La aplicacion nunca deja este valor, porque siempre valida.
alter table employee.address alter column normalization_issue set default 'SIN_VALIDAR';
alter table rulesystem.company_profile alter column normalization_issue set default 'SIN_VALIDAR';
alter table rulesystem.work_center_profile alter column normalization_issue set default 'SIN_VALIDAR';

-- La marca y su motivo no pueden decir cosas distintas.
alter table employee.address
    add constraint chk_address_normalized_issue check (normalized = (normalization_issue is null));
alter table rulesystem.company_profile
    add constraint chk_company_profile_normalized_issue check (normalized = (normalization_issue is null));
alter table rulesystem.work_center_profile
    add constraint chk_work_center_profile_normalized_issue check (normalized = (normalization_issue is null));

comment on column employee.address.normalization_issue is
    'Por que no esta normalizada (PostalAddressIssue); null si lo esta.';
comment on column rulesystem.company_profile.normalization_issue is
    'Por que no esta normalizada (PostalAddressIssue); null si lo esta.';
comment on column rulesystem.work_center_profile.normalization_issue is
    'Por que no esta normalizada (PostalAddressIssue); null si lo esta.';
