-- =========================================================
-- V117__create_employee_address_type_profile.sql
-- La cobertura de cada tipo de direccion vive en el catalogo (backend#53)
-- =========================================================
--
-- La serie de direcciones de un empleado va por tipo (ADR-057, decision 0): domicilio,
-- fiscal y postal conviven, cada una con su propia cobertura. Cual de ellas es el domicilio
-- —la que el empleado esta obligado a tener mientras esta presente— no se escribe en Java:
-- es una propiedad del tipo de direccion dentro de cada sistema de reglas, y otro sistema
-- puede marcar un tipo distinto. Es una extension PROFILE de EMPLOYEE_ADDRESS_TYPE
-- (ADR-053), declarada como required para que la guardia 3 exija que todo tipo diga su
-- cobertura: un tipo sin cobertura declarada no es opcional, es una pregunta sin responder.

create table rulesystem.employee_address_type_profile (
    id                          bigint      generated always as identity primary key,
    address_type_rule_entity_id bigint      not null,
    coverage                    varchar(10) not null,
    created_at                  timestamp   not null default now(),
    updated_at                  timestamp   not null default now(),
    constraint uk_employee_address_type_profile
        unique (address_type_rule_entity_id),
    constraint chk_employee_address_type_profile_coverage
        check (coverage in ('MANDATORY', 'OPTIONAL')),
    constraint fk_employee_address_type_profile_rule_entity
        foreign key (address_type_rule_entity_id)
        references rulesystem.rule_entity(id)
        on delete cascade
);

insert into rulesystem.rule_entity_extension
    (rule_entity_type_code, extension_code, table_name, cardinality, required)
values
    ('EMPLOYEE_ADDRESS_TYPE', 'PROFILE', 'rulesystem.employee_address_type_profile', '1:1', true);

-- La V11 sembro los mismos cuatro tipos para todos los sistemas de reglas, asi que la
-- cobertura se siembra igual: HOME es el domicilio en cada uno y el resto son opcionales.
-- Exactamente un tipo MANDATORY por sistema de reglas; la guardia lo comprueba.
insert into rulesystem.employee_address_type_profile (address_type_rule_entity_id, coverage)
select re.id,
       case when re.code = 'HOME' then 'MANDATORY' else 'OPTIONAL' end
from rulesystem.rule_entity re
where re.rule_entity_type_code = 'EMPLOYEE_ADDRESS_TYPE';
