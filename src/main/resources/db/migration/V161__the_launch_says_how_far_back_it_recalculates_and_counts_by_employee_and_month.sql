-- =========================================================
-- V161__the_launch_says_how_far_back_it_recalculates_and_counts_by_employee_and_month.sql
-- El suelo, el limite y los contadores de la retro (backend#132, paso 6 de
-- workspace#9)
-- =========================================================
--
-- El #130 registra que el pasado cambio y el #131 sabe recalcular un mes cerrado.
-- Lo que falta es quien decide DESDE CUANDO, y esa decision es de gestion y no
-- del motor: la toma quien lanza la nomina y se guarda con el run, porque el
-- recibo y la checklist tienen que poder decir con que se calculo.

-- ---------------------------------------------------------
-- 1. Los dos parametros, en el run
-- ---------------------------------------------------------
-- Van aqui y no en la empresa, y eso es lo que se decidio el 05/10. Una revision
-- de convenio -«todos desde enero»- no es una propiedad de la empresa: es una
-- cosa que pasa una vez, en una corrida concreta, y la corrida siguiente no
-- tiene por que arrastrarla. Ponerlo en la empresa habria convertido un suceso
-- en una configuracion, que es la forma de que alguien lo deje puesto.
alter table payroll.calculation_run
    add column retro_floor_period_code varchar(30),
    add column retro_limit_period_code varchar(30);

comment on column payroll.calculation_run.retro_floor_period_code is
    'Suelo obligatorio para todos: todo empleado de la corrida recalcula desde aqui aunque no tenga marca. Nulo es lo normal (backend#132).';

comment on column payroll.calculation_run.retro_limit_period_code is
    'Limite duro: nada mas atras de este periodo. Nulo significa que esta corrida NO hace retro, y entonces lo dice en sus mensajes si habia marcas que podria haber pagado.';

-- ---------------------------------------------------------
-- 2. Los contadores de la retro son SUYOS, y no los de siempre
-- ---------------------------------------------------------
-- La tentacion era hacer que `total_candidates` contara empleado x mes, porque
-- es lo que la pantalla tiene que ensenar. Se descarto: los nueve contadores de
-- arriba tienen una particion escrita que cuadra
-- (`total_candidates = skipped + calculated + not_valid + errors`, ver
-- CalculationRun), y una unidad de retro NO acaba en ninguno de esos cajones
-- -no escribe recibo, escribe vigente-, asi que meterla en el universo habria
-- roto la suma sin que nada avisara.
--
-- Asi que la retro trae su propia terna con su propia particion:
--
--   total_retro_units = total_retro_recalculated + total_retro_not_recalculated
--
-- y la pantalla ensena empleado x mes como `total_candidates + total_retro_units`,
-- que es el trabajo de verdad de la corrida. Sin eso, un lanzamiento con suelo
-- para todos parece colgado: dice «873 candidatos» y esta calculando siete mil
-- meses.
alter table payroll.calculation_run
    add column total_retro_units integer not null default 0,
    add column total_retro_recalculated integer not null default 0,
    add column total_retro_not_recalculated integer not null default 0;

comment on column payroll.calculation_run.total_retro_units is
    'El universo de la retro: unidades empleado x mes que hay que recalcular. Se cuenta una vez, al principio, como total_candidates (backend#132).';

alter table payroll.calculation_run
    add constraint chk_calculation_run_retro_counters_non_negative
        check (
            total_retro_units >= 0
            and total_retro_recalculated >= 0
            and total_retro_not_recalculated >= 0
        );

-- ---------------------------------------------------------
-- 3. Y las dos restricciones que hacen que los parametros signifiquen algo
-- ---------------------------------------------------------
-- Un suelo mas antiguo que el limite no se puede introducir. Se rechaza en la
-- peticion, con mensaje, y aqui queda la red: pedir recalcular desde enero con
-- el limite en junio es pedir dos cosas contrarias, y la respuesta correcta es
-- que no se pueda guardar, no que gane una de las dos en silencio.
--
-- Los codigos de periodo son `yyyyMM` en texto, asi que se comparan como texto y
-- el orden coincide con el cronologico. Eso es cierto mientras el formato no
-- cambie, y si cambia esta restriccion hay que reescribirla, no quitarla.
--
-- El `is not null` de los dos no es redundante: sin el, la comparacion con un
-- nulo sale desconocida y el CHECK la deja pasar (V154).
alter table payroll.calculation_run
    add constraint chk_calculation_run_retro_floor_not_before_limit
        check (
            retro_floor_period_code is null
            or retro_limit_period_code is null
            or retro_floor_period_code >= retro_limit_period_code
        );

-- Y si la corrida hizo retro, tenia limite. Al reves de lo que parece: no es que
-- el limite sea obligatorio siempre -las corridas de antes del #132 no lo
-- tienen y no hacian retro-, es que **no se puede recalcular el pasado sin haber
-- dicho hasta donde**.
alter table payroll.calculation_run
    add constraint chk_calculation_run_retro_needs_a_limit
        check (
            total_retro_units = 0
            or retro_limit_period_code is not null
        );

comment on constraint chk_calculation_run_retro_needs_a_limit on payroll.calculation_run is
    'No se recalcula el pasado sin haber dicho hasta donde. Las corridas de antes del backend#132 no tienen limite y no hacian retro: por eso la condicion es sobre el contador y no sobre la columna.';

-- ---------------------------------------------------------
-- 4. Los dos mensajes nuevos, en el catalogo
-- ---------------------------------------------------------
-- La V125 §3 deja escrita la regla, heredada del ADR-059 §7: cada comprobacion
-- nueva se implementa en el codigo Y se da de alta en el catalogo, en el mismo
-- commit. Hay un test que lo cruza (EveryRunMessageCodeIsInTheCatalogTest).
--
-- El segundo es el que importa contar aqui. Una corrida SIN limite no hace
-- retro, y eso es correcto -no puede inventarse hasta donde llega-, pero
-- callarselo cuando habia marcas activas seria dejar sin pagar un atraso sin que
-- nadie lo sepa. Asi que lo dice: cuantos empleados tenian marca y no se les
-- pago nada.
insert into rulesystem.rule_entity (
    rule_system_code, rule_entity_type_code, code, name, description, active, start_date, end_date
)
select 'ESP', 'PAYROLL_RUN_MESSAGE', v.code, v.name, v.description, true, DATE '1900-01-01', cast(null as date)
from (
    values
        ('RETRO_MONTH_NOT_RECALCULATED',
         'A closed month could not be recalculated',
         'A month in the retroactivity range could not be recalculated. Its receipt was not touched and the rest of the range went ahead'),
        ('RETRO_SKIPPED_NO_LIMIT',
         'Retroactivity skipped: the launch carried no limit',
         'Employees of this run had active retroactivity marks, and the launch did not say how far back it allows recalculating, so nothing was paid for them')
) as v(code, name, description)
where not exists (
    select 1
    from rulesystem.rule_entity e
    where e.rule_system_code = 'ESP'
      and e.rule_entity_type_code = 'PAYROLL_RUN_MESSAGE'
      and e.code = v.code
);

-- El castellano donde va el castellano, cruzando con rule_entity_type para que
-- esto no pueda usarse nunca para traducir una cita reglamentaria (ADR-052).
insert into rulesystem.rule_entity_translation (rule_entity_id, language_code, name, description)
select e.id, 'es-ES', v.name, v.description
from rulesystem.rule_entity e
join rulesystem.rule_entity_type t
  on t.code = e.rule_entity_type_code
 and t.literal_class = 'DOMAIN_VOCABULARY'
join (
    values
        ('RETRO_MONTH_NOT_RECALCULATED',
         'Un mes cerrado no se ha podido recalcular',
         'Un mes del tramo de retroactividad no se ha podido recalcular. Su recibo no se ha tocado y el resto del tramo ha seguido adelante'),
        ('RETRO_SKIPPED_NO_LIMIT',
         'Retroactividad no aplicada: el lanzamiento no llevaba límite',
         'Había empleados de esta corrida con marcas de retroactividad activas, y el lanzamiento no dijo hasta dónde permite recalcular, así que no se les ha pagado nada')
) as v(code, name, description)
  on v.code = e.code
where e.rule_system_code = 'ESP'
  and e.rule_entity_type_code = 'PAYROLL_RUN_MESSAGE'
  and not exists (
      select 1
      from rulesystem.rule_entity_translation tr
      where tr.rule_entity_id = e.id
        and tr.language_code = 'es-ES'
  );
