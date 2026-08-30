-- ADR-053 §6 guardia 3 (backend#33): toda extensión required tiene fila para cada raíz de
-- su tipo. La primera ejecución de la guardia sacó 22 raíces sin su perfil obligatorio, y
-- la decisión fue sembrar lo que falta, no aflojar ningún required:
--
--   COMPANY (2):            FRA/FR01 y PRT/PT01 — nacieron en V4 como compañías de
--                           presencia y se racionalizaron a COMPANY en V35 sin perfil.
--   WORK_CENTER (11):       los cinco centros de FRA, los cinco de PRT (mismo esqueleto de
--                           presencia, V19) y ESP/REMOTE, que está inactivo y se quedó
--                           fuera de la V52.
--   AGREEMENT_CATEGORY (9): CAT_ADMIN / CAT_TECH_1 / CAT_TECH_2 de los tres sistemas
--                           (baseline demo de V25/V52); las del convenio real ya los
--                           tenían de V87.
--
-- Perfiles mínimos y de demo: el nombre legal existe para todas las compañías (que es lo
-- que el orElse de ListCompaniesService daba por hecho), y lo que no se sabe queda a null,
-- que para eso las columnas son anulables (company_code no lo es desde V40: REMOTE cuelga
-- de ES01, como el resto de centros primarios). Se inserta solo donde falta, sin pisar
-- ningún perfil que ya exista.

insert into rulesystem.company_profile (
    company_rule_entity_id,
    legal_name,
    tax_identifier,
    country_code
)
select
    company_entity.id,
    seeded.legal_name,
    seeded.tax_identifier,
    seeded.country_code
from (
    values
        ('FRA', 'FR01', 'B4RRHH France Company 01, SARL',   'FRB4R001', 'FRA'),
        ('PRT', 'PT01', 'B4RRHH Portugal Company 01, Lda.', 'PTB4R001', 'PRT')
) as seeded(rule_system_code, company_code, legal_name, tax_identifier, country_code)
join rulesystem.rule_entity company_entity
  on company_entity.rule_system_code = seeded.rule_system_code
 and company_entity.rule_entity_type_code = 'COMPANY'
 and company_entity.code = seeded.company_code
where not exists (
    select 1
    from rulesystem.company_profile existing
    where existing.company_rule_entity_id = company_entity.id
);

insert into rulesystem.work_center_profile (
    work_center_rule_entity_id,
    company_code,
    country_code
)
select
    work_center_entity.id,
    seeded.company_code,
    seeded.country_code
from (
    values
        ('FRA', 'MAIN_OFFICE',  'FR01', 'FRA'),
        ('FRA', 'BRANCH_NORTH', 'FR01', 'FRA'),
        ('FRA', 'BRANCH_SOUTH', 'FR01', 'FRA'),
        ('FRA', 'BRANCH_EAST',  'FR01', 'FRA'),
        ('FRA', 'REMOTE',       'FR01', 'FRA'),
        ('PRT', 'MAIN_OFFICE',  'PT01', 'PRT'),
        ('PRT', 'BRANCH_NORTH', 'PT01', 'PRT'),
        ('PRT', 'BRANCH_SOUTH', 'PT01', 'PRT'),
        ('PRT', 'BRANCH_EAST',  'PT01', 'PRT'),
        ('PRT', 'REMOTE',       'PT01', 'PRT'),
        ('ESP', 'REMOTE',       'ES01', 'ESP')
) as seeded(rule_system_code, work_center_code, company_code, country_code)
join rulesystem.rule_entity work_center_entity
  on work_center_entity.rule_system_code = seeded.rule_system_code
 and work_center_entity.rule_entity_type_code = 'WORK_CENTER'
 and work_center_entity.code = seeded.work_center_code
where not exists (
    select 1
    from rulesystem.work_center_profile existing
    where existing.work_center_rule_entity_id = work_center_entity.id
);

-- Grupos de cotización de demo para las categorías baseline, coherentes con el catálogo
-- GRUPO_COTIZACION que V84 sembró en los tres sistemas: administrativa al 05 (Oficiales
-- Administrativos) y las técnicas al 02 y 03, todas de cotización mensual.
insert into rulesystem.agreement_category_profile (
    agreement_category_rule_entity_id,
    grupo_cotizacion_code,
    tipo_nomina
)
select
    cat.id,
    mapping.grupo_cotizacion_code,
    mapping.tipo_nomina
from rulesystem.rule_entity cat
join (
    values
        ('CAT_ADMIN',  '05', 'MENSUAL'),
        ('CAT_TECH_1', '02', 'MENSUAL'),
        ('CAT_TECH_2', '03', 'MENSUAL')
) as mapping(category_code, grupo_cotizacion_code, tipo_nomina)
    on mapping.category_code = cat.code
where cat.rule_entity_type_code = 'AGREEMENT_CATEGORY'
  and not exists (
    select 1
    from rulesystem.agreement_category_profile existing
    where existing.agreement_category_rule_entity_id = cat.id
);
