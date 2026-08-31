-- ADR-054 §5 (backend#37): la agrupación del menú vive en su propia tabla, no en un
-- check sobre una columna de texto. El menú necesita el nombre del grupo y su orden,
-- que en un check no caben; y la clave ajena da gratis la mitad de la guardia de
-- clausura (§7.1).

create table rulesystem.rule_entity_type_group (
    code varchar(30) primary key,
    name varchar(100) not null,
    display_order integer not null,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

-- Dos grupos con el mismo orden dejarían el menú al azar del plan de la consulta.
alter table rulesystem.rule_entity_type_group
    add constraint uk_rule_entity_type_group_display_order
    unique (display_order);

insert into rulesystem.rule_entity_type_group (code, name, display_order)
values
    ('ORGANIZATION', 'Organización', 1),
    ('SOCIETY',      'Sociedad',     2);
