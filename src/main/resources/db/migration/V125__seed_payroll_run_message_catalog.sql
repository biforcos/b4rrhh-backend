-- =========================================================
-- V125__seed_payroll_run_message_catalog.sql
-- Los codigos de mensaje de ejecucion, en el catalogo (backend#81)
-- =========================================================
--
-- La pantalla de una ejecucion (frontend#61) pinta el codigo desnudo porque no
-- hay catalogo detras. El worker hizo lo correcto al no inventarse los literales
-- en el cliente —un diccionario en el front se desincroniza el dia uno—, asi que
-- lo que falta es la semilla.
--
-- La lista sale de leer los servicios, no de una lista escrita antes: son los
-- ocho que emite LaunchPayrollCalculationService mas el que emite
-- RecoverAbandonedPayrollCalculationRunsService al cerrar una ejecucion que un
-- reinicio dejo a medias. Ese noveno no lo escribe el lanzador, pero cae en la
-- misma tabla y lo pinta la misma pantalla: dejarlo fuera seria dejar un codigo
-- desnudo justo en el caso en que alguien esta mirando por que no cuadra algo.

-- ---------------------------------------------------------
-- 1. El codigo mas largo no cabia
-- ---------------------------------------------------------
-- UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT son 40 caracteres y rule_entity.code
-- es varchar(30) desde la V1. Se ensancha a 50, que es lo que ya mide
-- payroll.calculation_run_message.message_code: si la columna que los guarda
-- admite 50, la que los nombra no puede admitir menos. Ensanchar un varchar no
-- reescribe la tabla ni toca los indices.
alter table rulesystem.rule_entity
    alter column code type varchar(50);

-- ---------------------------------------------------------
-- 2. Un grupo nuevo: estos codigos no son Organizacion ni Sociedad
-- ---------------------------------------------------------
-- El ADR-054 §5 dejo dos grupos porque eran los que habia. Un codigo que emite
-- el motor al calcular no es ninguno de los dos, y meterlo en Organizacion
-- seria clasificarlo mal a sabiendas.
--
-- No aparece ninguna cabecera nueva en el menu: la derivacion del frontend#33
-- descarta los grupos que se quedan sin entradas, y un tipo sin extensiones
-- vive en Catalogos (ADR-053 §2), que es donde se consulta que significa cada
-- codigo.
insert into rulesystem.rule_entity_type_group (code, name, display_order)
values ('PAYROLL', 'Nómina', 3)
on conflict (code) do nothing;

-- ---------------------------------------------------------
-- 3. El tipo, con las tres decisiones del ADR-054 §6
-- ---------------------------------------------------------
-- Vocabulario del dominio: es nuestro vocabulario, no una figura de la norma ni
-- el nombre propio de nada, asi que la forma base es inglesa y se traduce
-- (ADR-052).
--
-- Cerrado, y no referencia: una fila nueva aqui no es un dato mas. Un codigo de
-- mensaje solo significa algo si alguien lo emite desde el motor, asi que darlo
-- de alta sin implementarlo deja una pregunta abierta dentro del motor —que es
-- exactamente el criterio con el que el ADR-054 §4 justifica «cerrado» para
-- EMPLOYEE_TYPE—. Al reves tambien vale, y es la regla que hereda el motivo de
-- invalidez del ADR-059 §7: cada comprobacion nueva se implementa en el codigo
-- y se da de alta aqui, en el mismo commit.
insert into rulesystem.rule_entity_type (code, name, active, literal_class, maintenance_mode, group_code)
values ('PAYROLL_RUN_MESSAGE', 'Payroll Run Message', true, 'DOMAIN_VOCABULARY', 'CLOSED', 'PAYROLL')
on conflict (code) do nothing;

-- ---------------------------------------------------------
-- 4. Los codigos, solo en ESP
-- ---------------------------------------------------------
-- Un tipo cerrado se declara donde hace falta y no en todas las
-- reglamentaciones: es lo que dice la guardia de universalidad para
-- EMPLOYEE_TYPE (backend#25), y estos codigos estan en el mismo caso.
--
-- Los literales en ingles como forma base (ADR-052): el castellano va en
-- rule_entity_translation, mas abajo, y no en esta columna.
--
-- UNIT_NOT_ELIGIBLE y UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT no son
-- intercambiables y sus literales tienen que dejarlo obvio (backend#85): el
-- primero es lo esperable en un relanzamiento y no pide nada de nadie; el
-- segundo siempre pide que alguien mire. Uno se lee «ya tenia recibo» y el otro
-- «faltaban datos», sin que haga falta saberse el codigo.
insert into rulesystem.rule_entity (
    rule_system_code, rule_entity_type_code, code, name, description, active, start_date, end_date
)
select 'ESP', 'PAYROLL_RUN_MESSAGE', v.code, v.name, v.description, true, DATE '1900-01-01', cast(null as date)
from (
    values
        ('UNIT_ELIGIBLE_REAL_EXECUTED',
         'Calculated',
         'The unit was eligible and its payroll was calculated'),
        ('UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT',
         'Not calculated: data was missing',
         'The unit was eligible but something the calculation needs was not there. Someone has to look at it'),
        ('UNIT_NOT_ELIGIBLE',
         'Already had a payroll',
         'An immutable payroll already existed for the unit, so the run left it alone. Expected when relaunching'),
        ('UNIT_ALREADY_CLAIMED',
         'Taken by another run',
         'Another run had already claimed the unit, so this one did not touch it'),
        ('UNIT_CALCULATION_ERROR',
         'Calculation failed',
         'The calculation of the unit ended in an error'),
        ('NO_RELEVANT_PRESENCE',
         'No presence in the period',
         'The employee had no presence overlapping the payroll period, so there was nothing to calculate'),
        ('LAUNCH_REJECTED',
         'Launch rejected: the queue was full',
         'The launch did not fit in the queue and the run was closed without starting'),
        ('LAUNCH_ABORTED',
         'Run stopped by an unexpected error',
         'The run itself broke, not one of its units, and it was closed without finishing'),
        ('RUN_ABANDONED_ON_RESTART',
         'Run cut short by a restart',
         'The backend was restarted while the run was in flight, so the run never finished')
) as v(code, name, description)
where not exists (
    select 1
    from rulesystem.rule_entity e
    where e.rule_system_code = 'ESP'
      and e.rule_entity_type_code = 'PAYROLL_RUN_MESSAGE'
      and e.code = v.code
);

-- ---------------------------------------------------------
-- 5. El castellano, donde va el castellano
-- ---------------------------------------------------------
-- Misma forma que la V114: se cruza con rule_entity_type y solo entra lo que sea
-- vocabulario del dominio, para que esto no pueda usarse nunca para traducir una
-- cita reglamentaria o un nombre propio.
insert into rulesystem.rule_entity_translation (rule_entity_id, language_code, name, description)
select e.id, 'es-ES', v.name, v.description
from rulesystem.rule_entity e
join rulesystem.rule_entity_type t
  on t.code = e.rule_entity_type_code
 and t.literal_class = 'DOMAIN_VOCABULARY'
join (
    values
        ('UNIT_ELIGIBLE_REAL_EXECUTED',
         'Calculada',
         'La unidad era elegible y su nómina se calculó'),
        ('UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT',
         'Sin calcular: faltaban datos',
         'La unidad era elegible y no se pudo calcular porque faltaba algo que el cálculo necesita. Alguien tiene que mirarlo'),
        ('UNIT_NOT_ELIGIBLE',
         'Ya tenía recibo',
         'Ya existía un recibo inmutable para la unidad, así que la ejecución no la tocó. Es lo esperable al relanzar'),
        ('UNIT_ALREADY_CLAIMED',
         'La cogió otra ejecución',
         'Otra ejecución había reservado la unidad, así que ésta no la tocó'),
        ('UNIT_CALCULATION_ERROR',
         'El cálculo falló',
         'El cálculo de la unidad terminó en error'),
        ('NO_RELEVANT_PRESENCE',
         'Sin presencia en el periodo',
         'El empleado no tenía ninguna presencia que solapara el periodo de nómina, así que no había nada que calcular'),
        ('LAUNCH_REJECTED',
         'Lanzamiento rechazado: la cola estaba llena',
         'El lanzamiento no cupo en la cola y la ejecución se cerró sin llegar a empezar'),
        ('LAUNCH_ABORTED',
         'Ejecución detenida por un error inesperado',
         'Lo que falló fue la ejecución, no una de sus unidades, y se cerró sin terminar'),
        ('RUN_ABANDONED_ON_RESTART',
         'Ejecución cortada por un reinicio',
         'El backend se reinició con la ejecución en marcha, así que nunca terminó')
) as v(code, name, description)
  on v.code = e.code
where e.rule_entity_type_code = 'PAYROLL_RUN_MESSAGE'
  and not exists (
    select 1
    from rulesystem.rule_entity_translation tr
    where tr.rule_entity_id = e.id
      and tr.language_code = 'es-ES'
);
