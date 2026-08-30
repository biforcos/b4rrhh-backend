-- ADR-052 §1 (backend#23): el idioma vive en una tabla aparte colgando de rule_entity.
-- rule_entity.name y rule_entity.description siguen siendo el literal base; esto es aditivo
-- y no mueve ningún dato.
--
-- language_code va en BCP 47 corto: dos letras minúsculas de idioma y, opcionalmente, un
-- guion y dos mayúsculas de región ('es-ES', 'fr-FR', 'en'). El check impide que convivan
-- 'es', 'es_ES' y 'es-ES' como si fueran tres idiomas.

create table rulesystem.rule_entity_translation (
    rule_entity_id bigint       not null references rulesystem.rule_entity(id),
    language_code  varchar(5)   not null,
    name           varchar(100) not null,
    description    varchar(500),
    created_at     timestamp    not null default now(),
    updated_at     timestamp    not null default now(),
    primary key (rule_entity_id, language_code)
);

alter table rulesystem.rule_entity_translation
    add constraint chk_rule_entity_translation_language_code
    check (language_code ~ '^[a-z]{2}(-[A-Z]{2})?$');

create index ix_rule_entity_translation_language_code
    on rulesystem.rule_entity_translation (language_code);
