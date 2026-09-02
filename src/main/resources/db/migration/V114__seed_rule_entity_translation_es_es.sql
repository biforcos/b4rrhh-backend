-- backend#40 (ADR-052 §1, §2 y §5): la tabla de traducciones existía desde la V104 y no
-- tenía una sola fila, así que Accept-Language cruzaba hasta Postgres para volver siempre
-- con el literal base. Ésta es la primera semilla: el castellano de los tipos de
-- vocabulario del dominio.
--
-- Qué se traduce lo dice el metamodelo, no esta lista: la inserción cruza con
-- rule_entity_type y sólo entra lo que tenga literal_class = 'DOMAIN_VOCABULARY'
-- (ADR-054). Una cita reglamentaria o un nombre propio que alguien añadiera abajo por
-- error no pasaría del join: traducir «IT Contingencia Común» o «Branch North» sería
-- falsificarlos. Y como se cruza con rule_entity por (tipo, código), la traducción cae en
-- la fila de cada reglamentación —ESP, FRA y PRT— sin enumerarlas: indexar por
-- rule_entity_id es lo que decidió el ADR-052 §1.
--
-- El literal base no se toca: rule_entity.name sigue siendo el inglés neutro. Otro idioma
-- mañana es otra migración como ésta, con otro language_code.
--
-- Idempotente: si la fila ya existe, se respeta lo que haya.

insert into rulesystem.rule_entity_translation (rule_entity_id, language_code, name, description)
select
    e.id,
    'es-ES',
    v.name,
    v.description
from rulesystem.rule_entity e
join rulesystem.rule_entity_type t
  on t.code = e.rule_entity_type_code
 and t.literal_class = 'DOMAIN_VOCABULARY'
join (
    values
        -- CONTACT_TYPE
        ('CONTACT_TYPE', 'COMPANY_MOBILE', 'Móvil de empresa', 'Móvil de empresa asignado al empleado'),
        ('CONTACT_TYPE', 'EMAIL',          'Correo electrónico', 'Correo electrónico del empleado'),
        ('CONTACT_TYPE', 'EXTENSION',      'Extensión', 'Extensión telefónica del empleado'),
        ('CONTACT_TYPE', 'MOBILE',         'Móvil', 'Teléfono móvil del empleado'),
        ('CONTACT_TYPE', 'PHONE',          'Teléfono', 'Teléfono del empleado'),

        -- COUNTRY
        ('COUNTRY', 'ARG', 'Argentina',      'Catálogo de países según ISO 3166-1 alfa-3'),
        ('COUNTRY', 'BRA', 'Brasil',         'Catálogo de países según ISO 3166-1 alfa-3'),
        ('COUNTRY', 'DEU', 'Alemania',       'Catálogo de países según ISO 3166-1 alfa-3'),
        ('COUNTRY', 'ESP', 'España',         'Catálogo de países según ISO 3166-1 alfa-3'),
        ('COUNTRY', 'FRA', 'Francia',        'Catálogo de países según ISO 3166-1 alfa-3'),
        ('COUNTRY', 'GBR', 'Reino Unido',    'Catálogo de países según ISO 3166-1 alfa-3'),
        ('COUNTRY', 'ITA', 'Italia',         'Catálogo de países según ISO 3166-1 alfa-3'),
        ('COUNTRY', 'MEX', 'México',         'Catálogo de países según ISO 3166-1 alfa-3'),
        ('COUNTRY', 'PRT', 'Portugal',       'Catálogo de países según ISO 3166-1 alfa-3'),
        ('COUNTRY', 'USA', 'Estados Unidos', 'Catálogo de países según ISO 3166-1 alfa-3'),

        -- EMPLOYEE_ADDRESS_TYPE
        ('EMPLOYEE_ADDRESS_TYPE', 'FISCAL',    'Fiscal',    'Domicilio fiscal'),
        ('EMPLOYEE_ADDRESS_TYPE', 'HOME',      'Domicilio', 'Residencia habitual'),
        ('EMPLOYEE_ADDRESS_TYPE', 'MAILING',   'Postal',    'Dirección de correspondencia'),
        ('EMPLOYEE_ADDRESS_TYPE', 'TEMPORARY', 'Temporal',  'Dirección de estancia temporal'),

        -- EMPLOYEE_IDENTIFIER_TYPE
        ('EMPLOYEE_IDENTIFIER_TYPE', 'NATIONAL_ID',     'Documento nacional de identidad', 'Documento nacional de identificación'),
        ('EMPLOYEE_IDENTIFIER_TYPE', 'PASSPORT',        'Pasaporte',                       'Pasaporte'),
        ('EMPLOYEE_IDENTIFIER_TYPE', 'SOCIAL_SECURITY', 'Seguridad Social',                'Número de afiliación a la Seguridad Social'),
        ('EMPLOYEE_IDENTIFIER_TYPE', 'TAX_ID',          'Identificación fiscal',           'Número de identificación fiscal'),

        -- EMPLOYEE_PRESENCE_ENTRY_REASON
        ('EMPLOYEE_PRESENCE_ENTRY_REASON', 'HIRING',      'Contratación',       'Contratación inicial en la empresa'),
        ('EMPLOYEE_PRESENCE_ENTRY_REASON', 'REHIRE',      'Readmisión',         'Readmisión tras un cese anterior'),
        ('EMPLOYEE_PRESENCE_ENTRY_REASON', 'TRANSFER_IN', 'Traslado de entrada', 'Empleado trasladado a esta empresa o contexto'),

        -- EMPLOYEE_PRESENCE_EXIT_REASON
        ('EMPLOYEE_PRESENCE_EXIT_REASON', 'RETIREMENT',   'Jubilación',        'Jubilación del empleado'),
        ('EMPLOYEE_PRESENCE_EXIT_REASON', 'TERMINATION',  'Cese',              'Fin de la relación laboral'),
        ('EMPLOYEE_PRESENCE_EXIT_REASON', 'TRANSFER_OUT', 'Traslado de salida', 'Empleado trasladado a otra empresa o contexto'),

        -- EMPLOYEE_TYPE
        ('EMPLOYEE_TYPE', 'EXTERNAL', 'Empleado externo', 'Tipo de empleado secundario, disponible por coherencia de los datos de referencia'),
        ('EMPLOYEE_TYPE', 'INTERNAL', 'Empleado interno', 'Tipo de empleado por defecto para los flujos de administración de personal')
) as v(rule_entity_type_code, code, name, description)
  on v.rule_entity_type_code = e.rule_entity_type_code
 and v.code = e.code
on conflict (rule_entity_id, language_code) do nothing;
