-- V180__a_regulatory_citation_carries_a_common_label.sql
--
-- backend#173. En la sesion de workspace#18: «Hostia... vale que el nombre legal de la enfermedad
-- es IT comun... pero que poco intuitivo». Quien declara una baja busca «enfermedad».
--
-- EMPLOYEE_ABSENCE_TYPE es cita reglamentaria (V113, ADR-054 §3): su literal es el nombre oficial
-- y traducirlo seria falsificarlo, asi que no va a rule_entity_translation, y el candado que lo
-- vigila sigue igual. Lo que se anade es otra cosa: una etiqueta comun, en el mismo idioma que
-- la cita, que la acompana sin sustituirla. La pantalla ensena la etiqueta delante y la cita
-- detras. Ver la nota del backend#173 en el ADR-054.
--
-- Solo para entidades de tipos cita reglamentaria (lo vigila
-- RuleEntityTranslationFlywayIntegrationTest), y solo donde la cita no se entiende: de los siete
-- tipos de ausencia, las dos IT. Vacaciones, los permisos y la excedencia ya estan en palabras.

create table rulesystem.rule_entity_common_label (
    rule_entity_id bigint       not null references rulesystem.rule_entity (id) on delete cascade,
    language_code  varchar(5)   not null,
    label          varchar(100) not null,
    created_at     timestamp    not null default now(),
    updated_at     timestamp    not null default now(),
    constraint pk_rule_entity_common_label primary key (rule_entity_id, language_code),
    constraint chk_rule_entity_common_label_language_code check (language_code ~ '^[a-z]{2}(-[A-Z]{2})?$')
);

comment on table rulesystem.rule_entity_common_label is
    'La etiqueta en palabras de una cita reglamentaria (backend#173): acompana al nombre oficial, no lo traduce ni lo sustituye.';

insert into rulesystem.rule_entity_common_label (rule_entity_id, language_code, label)
select re.id, 'es-ES', l.label
  from (values ('IT_COMMON', 'Baja por enfermedad común'),
               ('IT_WORK_ACCIDENT', 'Baja por accidente de trabajo o enfermedad profesional')) as l (code, label)
  join rulesystem.rule_entity re on re.rule_entity_type_code = 'EMPLOYEE_ABSENCE_TYPE' and re.code = l.code;

do $$
begin
    if (select count(*) from rulesystem.rule_entity_common_label) <> 2 then
        raise exception 'Las dos etiquetas comunes de las IT no se han sembrado' using errcode = '23514';
    end if;
end;
$$;
