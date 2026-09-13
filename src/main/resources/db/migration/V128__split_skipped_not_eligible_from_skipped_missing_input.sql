-- backend#85: total_skipped_not_eligible sumaba dos eventos de severidad opuesta, y uno de
-- ellos contradecia su propio nombre. LaunchPayrollCalculationService lo incrementaba en dos
-- sitios: en la linea 292, cuando ya existia un recibo inmutable —«ya estaba hecho», no pide
-- nada de nadie— y en la 354, cuando la unidad era elegible y faltaban datos —«que alguien
-- mire»—, con el literal UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT sumando a NOT_ELIGIBLE.
--
-- Se parte en dos. El nombre viejo se queda donde estaba: nunca describio mal lo suyo,
-- describia mal lo que no era suyo, asi que quitandole el segundo camino pasa a ser verdad sin
-- migrar nada de lo que ya significaba.
--
-- total_eligible NO se toca. Se incrementa despues del filtro de no-elegible y antes de
-- intentar el calculo, asi que las unidades que luego se saltan por falta de datos SI eran
-- elegibles. Lo que queda escrito es que las incluye: esta en el javadoc de CalculationRun,
-- que es donde se lee antes de usarlas.
alter table payroll.calculation_run
    add column total_skipped_missing_input integer not null default 0;

alter table payroll.calculation_run
    drop constraint chk_calculation_run_counters_non_negative;

alter table payroll.calculation_run
    add constraint chk_calculation_run_counters_non_negative
    check (
        total_candidates >= 0
        and total_eligible >= 0
        and total_claimed >= 0
        and total_skipped_not_eligible >= 0
        and total_skipped_already_claimed >= 0
        and total_skipped_missing_input >= 0
        and total_calculated >= 0
        and total_not_valid >= 0
        and total_errors >= 0
    );

comment on column payroll.calculation_run.total_skipped_not_eligible is
    'Saltadas porque ya existia un recibo inmutable (UNIT_NOT_ELIGIBLE). Esperable en un '
    'relanzamiento; no pide nada de nadie. Ver el javadoc de CalculationRun (backend#85).';

comment on column payroll.calculation_run.total_skipped_missing_input is
    'Eran elegibles y faltaban datos para calcularlas '
    '(UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT). Siempre pide que alguien mire (backend#85).';

comment on column payroll.calculation_run.total_eligible is
    'Acumulador, no un total: sube por cada unidad que pasa el filtro de elegibilidad, e '
    'INCLUYE a las que despues se saltan por falta de datos o fallan. El denominador de '
    'cualquier porcentaje es total_candidates (backend#85).';

-- Los historicos se reparten desde los mensajes, que es donde esta la informacion que el
-- contador perdio: calculation_run_message distingue los dos codigos por unidad. No se
-- inventa nada — lo que no tiene mensaje se queda donde estaba.
with reparto as (
    select run_id, count(*) as faltaban_datos
      from payroll.calculation_run_message
     where message_code = 'UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT'
     group by run_id
)
update payroll.calculation_run r
   set total_skipped_missing_input = least(reparto.faltaban_datos, r.total_skipped_not_eligible),
       total_skipped_not_eligible  = r.total_skipped_not_eligible
                                     - least(reparto.faltaban_datos, r.total_skipped_not_eligible)
  from reparto
 where reparto.run_id = r.id;
