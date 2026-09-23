-- =========================================================
-- V154__a_check_that_lets_a_null_through_does_not_check.sql
-- Las empresas de FRA y PRT se quedaron con el CNAE viejo (backend#122)
-- =========================================================
--
-- La V152 decia que las empresas de Francia y de Portugal se quedan **sin
-- CNAE**, y no las toco. Siguen con el `4719` de la V150 —un codigo CNAE-2009,
-- derogado— y con la clasificacion a nulo.
--
-- Son dos defectos, y el segundo es el que hace falta contar.
--
-- ---------------------------------------------------------
-- 1. El UPDATE que no alcanzaba a dos de las cuatro
-- ---------------------------------------------------------
-- La V152 unia con `rule_entity` pidiendo `rule_system_code = 'ESP'`. `FR01`
-- vive en el sistema de reglas `FRA` y `PT01` en `PRT`: el join no las
-- encontraba, asi que las dos filas que tenian que quedarse a nulo son
-- exactamente las dos que no se actualizaron. El `where` filtraba por la
-- propiedad que el propio cambio queria distinguir.
--
-- ---------------------------------------------------------
-- 2. Y el check no se quejo, porque un check no rechaza lo desconocido
-- ---------------------------------------------------------
-- La V152 anadio esta restriccion:
--
--   check ((cnae_code is null     and cnae_classification is null)
--       or (cnae_code is not null and cnae_classification = 'CNAE-2025'))
--
-- que se escribio para que nadie pudiera guardar un codigo sin decir en que
-- clasificacion esta. Con `cnae_code = '4719'` y `cnae_classification` a nulo,
-- la primera rama es falsa y la segunda es `true and (null = 'CNAE-2025')`, o
-- sea **desconocida**. Falso o desconocido es desconocido, y **una restriccion
-- CHECK solo rechaza la fila cuando su expresion es falsa**: lo desconocido
-- pasa. Las cuatro filas eran asi cuando se creo la restriccion, y ninguna la
-- rompio.
--
-- Es la trampa clasica de SQL de tres valores, y aqui se llevo por delante
-- justo el caso que la restriccion existia para impedir. La regla que queda:
-- **en un CHECK, la comparacion de una columna que admite nulos se acompana de
-- su `is not null`**, o la restriccion no comprueba nada cuando esa columna
-- falta — que es cuando mas falta hace.
alter table rulesystem.company_profile
    drop constraint chk_company_profile_cnae_classification;

update rulesystem.company_profile cp
   set cnae_code           = null,
       cnae_classification = null,
       updated_at          = current_timestamp
  from rulesystem.rule_entity re
 where re.id = cp.company_rule_entity_id
   and re.rule_entity_type_code = 'COMPANY'
   and re.rule_system_code <> 'ESP'
   and cp.cnae_code is not null;

alter table rulesystem.company_profile
    add constraint chk_company_profile_cnae_classification
        check (
            (cnae_code is null and cnae_classification is null)
            or (cnae_code is not null
                and cnae_classification is not null
                and cnae_classification = 'CNAE-2025')
        );

comment on constraint chk_company_profile_cnae_classification on rulesystem.company_profile is
    'Un codigo de actividad sin clasificacion no significa nada. El is not null no es redundante: sin el, la comparacion sale desconocida y el CHECK la deja pasar (backend#122).';
