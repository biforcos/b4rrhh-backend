-- ADR-054 §§3, 4 y 6 (backend#37): lo que un tipo de entidad sabe de sí mismo — la clase
-- de su literal, su modo de mantenimiento y su grupo del menú. Ninguna columna admite
-- null y ninguna tiene default: un tipo sin clasificar es indistinguible de uno escondido
-- a propósito, y un default es una ausencia disfrazada. Añadir un tipo obliga a tomar las
-- tres decisiones, y esta migración no pasa si no se toman.

alter table rulesystem.rule_entity_type add column literal_class varchar(30);
alter table rulesystem.rule_entity_type add column maintenance_mode varchar(30);
alter table rulesystem.rule_entity_type add column group_code varchar(30);

alter table rulesystem.rule_entity_type
    add constraint chk_rule_entity_type_literal_class
    check (literal_class in ('DOMAIN_VOCABULARY', 'REGULATORY_CITATION', 'PROPER_NOUN'));

alter table rulesystem.rule_entity_type
    add constraint chk_rule_entity_type_maintenance_mode
    check (maintenance_mode in ('MAINTAINED', 'REFERENCE', 'CLOSED'));

alter table rulesystem.rule_entity_type
    add constraint fk_rule_entity_type_group_code
    foreign key (group_code)
    references rulesystem.rule_entity_type_group(code);

-- La clasificación de los dieciséis, tal cual la tabla del ADR-054. Es el momento en que
-- las decisiones se toman en vez de heredarse; no se reinterpreta aquí.
update rulesystem.rule_entity_type t
set
    literal_class = c.literal_class,
    maintenance_mode = c.maintenance_mode,
    group_code = c.group_code,
    updated_at = now()
from (
    values
        ('COMPANY',                        'PROPER_NOUN',         'MAINTAINED', 'ORGANIZATION'),
        ('WORK_CENTER',                    'PROPER_NOUN',         'MAINTAINED', 'ORGANIZATION'),
        ('COST_CENTER',                    'PROPER_NOUN',         'MAINTAINED', 'ORGANIZATION'),
        ('AGREEMENT',                      'REGULATORY_CITATION', 'MAINTAINED', 'SOCIETY'),
        ('AGREEMENT_CATEGORY',             'REGULATORY_CITATION', 'MAINTAINED', 'SOCIETY'),
        ('CONTRACT',                       'REGULATORY_CITATION', 'REFERENCE',  'SOCIETY'),
        ('CONTRACT_SUBTYPE',               'REGULATORY_CITATION', 'REFERENCE',  'SOCIETY'),
        ('GRUPO_COTIZACION',               'REGULATORY_CITATION', 'REFERENCE',  'SOCIETY'),
        ('COUNTRY',                        'DOMAIN_VOCABULARY',   'REFERENCE',  'ORGANIZATION'),
        ('EMPLOYEE_PRESENCE_ENTRY_REASON', 'DOMAIN_VOCABULARY',   'MAINTAINED', 'ORGANIZATION'),
        ('EMPLOYEE_PRESENCE_EXIT_REASON',  'DOMAIN_VOCABULARY',   'MAINTAINED', 'ORGANIZATION'),
        ('EMPLOYEE_ADDRESS_TYPE',          'DOMAIN_VOCABULARY',   'MAINTAINED', 'ORGANIZATION'),
        ('CONTACT_TYPE',                   'DOMAIN_VOCABULARY',   'MAINTAINED', 'ORGANIZATION'),
        ('EMPLOYEE_IDENTIFIER_TYPE',       'DOMAIN_VOCABULARY',   'MAINTAINED', 'ORGANIZATION'),
        ('EMPLOYEE_ABSENCE_TYPE',          'DOMAIN_VOCABULARY',   'MAINTAINED', 'ORGANIZATION'),
        ('EMPLOYEE_TYPE',                  'DOMAIN_VOCABULARY',   'CLOSED',     'ORGANIZATION')
) as c(code, literal_class, maintenance_mode, group_code)
where t.code = c.code;

-- El set not null es la primera guardia y corre antes de que exista ningún test: si un
-- tipo se quedó sin clasificar, la migración revienta aquí (ADR-054 §6).
alter table rulesystem.rule_entity_type alter column literal_class set not null;
alter table rulesystem.rule_entity_type alter column maintenance_mode set not null;
alter table rulesystem.rule_entity_type alter column group_code set not null;
