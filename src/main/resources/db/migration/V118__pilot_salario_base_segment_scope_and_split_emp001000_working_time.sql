-- =========================================================
-- V118__pilot_salario_base_segment_scope_and_split_emp001000_working_time.sql
-- Piloto: poner SALARIO_BASE en SEGMENT y observar que sale (backend#46)
-- =========================================================
--
-- Los 36 conceptos de la semilla tienen execution_scope = 'PERIOD'. Toda la
-- maquinaria de segmentos existe y no ha ejecutado nunca en serio. Este piloto
-- cambia un unico concepto a SEGMENT y parte la jornada de un empleado de prueba
-- a mitad de mes, para que el calculo produzca dos tramos con precio distinto.
--
-- Esto cambia el comportamiento del calculo de un concepto existente y NO se
-- deja puesto sin decidirlo: si la observacion termina y no se adopta, hace
-- falta la migracion que lo devuelve (SALARIO_BASE a PERIOD y la ventana 2 de
-- jornada fuera, con la ventana 1 abierta otra vez).

-- 1. SALARIO_BASE (objeto 101 del rule system ESP) pasa a SEGMENT.

update payroll_engine.payroll_concept concept
set execution_scope = 'SEGMENT',
    updated_at      = current_timestamp
from payroll_engine.payroll_object object
where object.id               = concept.object_id
  and object.rule_system_code = 'ESP'
  and object.object_type_code = 'CONCEPT'
  and object.object_code      = '101';

-- 2. Jornada partida del empleado de prueba ESP/INTERNAL/EMP001000:
--    100% hasta el 2026-09-15 (su ventana 1, abierta desde 2023-02-06) y
--    50% desde el 2026-09-16, abierta. Al empleado lo crea el loader, no una
--    migracion: en una base sin el (los tests, por ejemplo) esto no hace nada.

update employee.working_time working_time
set end_date   = date '2026-09-15',
    updated_at = current_timestamp
from employee.employee employee
where employee.id                 = working_time.employee_id
  and employee.rule_system_code   = 'ESP'
  and employee.employee_type_code = 'INTERNAL'
  and employee.employee_number    = 'EMP001000'
  and working_time.working_time_number = 1
  and working_time.end_date is null
  and working_time.start_date <= date '2026-09-15';

insert into employee.working_time (
    employee_id,
    working_time_number,
    start_date,
    end_date,
    working_time_percentage,
    weekly_hours,
    daily_hours,
    monthly_hours
)
select
    employee.id,
    2,
    date '2026-09-16',
    null,
    50.00,
    16.69,
    3.34,
    72.34
from employee.employee employee
where employee.rule_system_code   = 'ESP'
  and employee.employee_type_code = 'INTERNAL'
  and employee.employee_number    = 'EMP001000'
  and exists (
      select 1
      from employee.working_time closed
      where closed.employee_id         = employee.id
        and closed.working_time_number = 1
        and closed.end_date            = date '2026-09-15'
  )
  and not exists (
      select 1
      from employee.working_time existing
      where existing.employee_id = employee.id
        and existing.working_time_number = 2
  );
