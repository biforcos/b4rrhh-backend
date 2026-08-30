-- ADR-053 §2 (backend#33): las extensiones de cada tipo de entidad se declaran, no se
-- llevan en la cabeza. Una fila por (tipo, extensión); la ausencia significa «sólo raíz»
-- y es el caso común: los tipos que son sólo código y literal NO aparecen aquí, a propósito.
--
-- El metamodelo describe; no ejecuta (ADR-053 §4). Lo leen las guardias, la navegación y
-- el aviso de borrado. No hay columna de política de borrado porque no hay política que
-- elegir: una extensión siempre cae en cascada (ADR-053 §3).

create table rulesystem.rule_entity_extension (
    rule_entity_type_code varchar(30) not null references rulesystem.rule_entity_type(code),
    extension_code        varchar(30) not null,   -- PROFILE, CONTACTS…
    table_name            varchar(63) not null,   -- rulesystem.company_profile
    cardinality           varchar(3)  not null,   -- '1:1' | '1:N'
    required              boolean     not null,
    created_at            timestamp   not null default now(),
    updated_at            timestamp   not null default now(),
    primary key (rule_entity_type_code, extension_code)
);

alter table rulesystem.rule_entity_extension
    add constraint chk_rule_entity_extension_cardinality
    check (cardinality in ('1:1', '1:N'));

-- Los perfiles y los contactos.
insert into rulesystem.rule_entity_extension
    (rule_entity_type_code, extension_code, table_name, cardinality, required)
values
    ('COMPANY',            'PROFILE',  'rulesystem.company_profile',            '1:1', true),
    ('WORK_CENTER',        'PROFILE',  'rulesystem.work_center_profile',        '1:1', true),
    ('WORK_CENTER',        'CONTACTS', 'rulesystem.work_center_contact',        '1:N', false),
    ('AGREEMENT',          'PROFILE',  'rulesystem.agreement_profile',          '1:1', true),
    ('AGREEMENT_CATEGORY', 'PROFILE',  'rulesystem.agreement_category_profile', '1:1', true);

-- Las relaciones cuelgan de sus DOS extremos: borrar el convenio se lleva sus relaciones
-- con categorías, y borrar la categoría también. Se declaran por las dos puntas para que
-- el aviso de borrado derivado (ADR-053 §7) las cuente desde cualquiera de los lados.
insert into rulesystem.rule_entity_extension
    (rule_entity_type_code, extension_code, table_name, cardinality, required)
values
    ('AGREEMENT',          'CATEGORY_RELATIONS', 'rulesystem.agreement_category_relation', '1:N', false),
    ('AGREEMENT_CATEGORY', 'AGREEMENT_RELATIONS', 'rulesystem.agreement_category_relation', '1:N', false),
    ('CONTRACT',           'SUBTYPE_RELATIONS',  'rulesystem.contract_subtype_relation',   '1:N', false),
    ('CONTRACT_SUBTYPE',   'CONTRACT_RELATIONS', 'rulesystem.contract_subtype_relation',   '1:N', false);

-- rulesystem.rule_entity_translation NO se declara: cuelga de cualquier raíz, sea del tipo
-- que sea (ADR-052 §1), y declararla exigiría una fila por tipo —también por cada tipo
-- futuro—, con lo que la ausencia dejaría de significar «sólo raíz» (ADR-053 §2). Es un
-- satélite universal: la guardia del inverso lo exime con motivo y su cascada se afirma
-- igual que la de las extensiones declaradas.
