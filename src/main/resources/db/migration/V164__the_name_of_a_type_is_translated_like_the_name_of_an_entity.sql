-- backend#152: GET /rule-entity-types devolvia los nombres de tipo en ingles («Contact Type»,
-- «Employee Presence Exit Reason»), y no habia donde poner otro idioma.
--
-- Decidido en el issue: el nombre de un tipo lleva traduccion como el de una entidad, y no se
-- siembra en castellano en su propia columna. GRUPO_COTIZACION se sembro asi y fue la
-- excepcion, no el modelo. rule_entity_type.name sigue siendo el nombre almacenado, el que se
-- edita; esto es aditivo, como la V104.
--
-- Es la gemela de rule_entity_translation y no la misma tabla: un tipo no es una rule_entity,
-- y aquella cuelga de rule_entity.id. Esta cuelga del codigo del tipo, que es global —no hay
-- reglamentacion que separar, al contrario que en ADR-052 §1— y es su clave de negocio. Sin
-- descripcion, porque el tipo no la tiene.
--
-- El borrado va en cascada: retirar un tipo es un delete en una migracion (ADR-054 §1, V35,
-- V37), y sus traducciones no deben impedirlo.

create table rulesystem.rule_entity_type_translation (
    rule_entity_type_code varchar(30)  not null
        references rulesystem.rule_entity_type(code) on delete cascade,
    language_code         varchar(5)   not null,
    name                  varchar(100) not null,
    created_at            timestamp    not null default now(),
    updated_at            timestamp    not null default now(),
    primary key (rule_entity_type_code, language_code)
);

alter table rulesystem.rule_entity_type_translation
    add constraint chk_rule_entity_type_translation_language_code
    check (language_code ~ '^[a-z]{2}(-[A-Z]{2})?$');

-- El castellano de los diecisiete tipos que hay. Los nombres son los que ya usa la ficha
-- («Motivo de baja», «Tipo de contacto»). GRUPO_COTIZACION lleva el suyo, que ya estaba en
-- castellano: asi la cobertura no tiene huecos que haya que explicar.
insert into rulesystem.rule_entity_type_translation (rule_entity_type_code, language_code, name)
select t.code, 'es-ES', v.name
  from (values
        ('COMPANY',                        'Empresa'),
        ('CONTACT_TYPE',                   'Tipo de contacto'),
        ('COST_CENTER',                    'Centro de coste'),
        ('COUNTRY',                        'País'),
        ('EMPLOYEE_ABSENCE_TYPE',          'Tipo de ausencia'),
        ('EMPLOYEE_ADDRESS_TYPE',          'Tipo de dirección'),
        ('EMPLOYEE_IDENTIFIER_TYPE',       'Tipo de identificador'),
        ('EMPLOYEE_PRESENCE_ENTRY_REASON', 'Motivo de alta'),
        ('EMPLOYEE_PRESENCE_EXIT_REASON',  'Motivo de baja'),
        ('EMPLOYEE_TYPE',                  'Tipo de empleado'),
        ('WORK_CENTER',                    'Centro de trabajo'),
        ('PAYROLL_RUN_MESSAGE',            'Mensaje del cálculo de nómina'),
        ('AGREEMENT',                      'Convenio'),
        ('AGREEMENT_CATEGORY',             'Categoría de convenio'),
        ('CONTRACT',                       'Contrato'),
        ('CONTRACT_SUBTYPE',               'Subtipo de contrato'),
        ('GRUPO_COTIZACION',               'Grupo de cotización SS')
       ) as v(code, name)
  join rulesystem.rule_entity_type t on t.code = v.code;

-- Si un tipo nuevo aparecio entre la lista de arriba y esta migracion, no se queda sin
-- traducir en silencio: la migracion falla y dice cual.
do $$
declare
    untranslated text;
begin
    select string_agg(t.code, ', ' order by t.code)
      into untranslated
      from rulesystem.rule_entity_type t
     where not exists (select 1
                         from rulesystem.rule_entity_type_translation tr
                        where tr.rule_entity_type_code = t.code
                          and tr.language_code = 'es-ES');
    if untranslated is not null then
        raise exception 'Tipos sin nombre en castellano: %', untranslated;
    end if;
end $$;
