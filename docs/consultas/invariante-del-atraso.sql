-- =========================================================
-- La invariante del atraso (backend#133, paso 6 de workspace#9)
-- =========================================================
--
--   Lo cobrado por un periodo es igual a su ultimo calculo.
--   recibo(M) + suma de atrasos(M) == vigente(M), por concepto.
--
-- Cero filas es que cuadra. Cada fila que salga es un concepto de un mes por el
-- que se ha pagado algo distinto de lo que ese mes vale hoy, y eso es un atraso
-- mal calculado o uno que falta.
--
-- ---------------------------------------------------------
-- Como se usa
-- ---------------------------------------------------------
--   psql -d b4rrhh_semilla -v emp="'%'" -f invariante-del-atraso.sql
--
-- El parametro es el numero de empleado; con '%' se comprueba la base entera. La
-- version del test lo pasa por posicion (?) porque JdbcTemplate no usa :nombre.
--
-- ---------------------------------------------------------
-- Que queda fuera, y por que son ocho
-- ---------------------------------------------------------
-- Los conceptos del mes que PAGA, que no se atribuyen a un mes:
--
--   800                     la retencion de IRPF: es sobre lo que se paga cuando
--                           se paga (ADR-070 §4)
--   970, 980, 990, 725      los totales: son sumas del mes que paga. El vigente
--                           de agosto dice que agosto vale 1.543,80 de bruto, y
--                           por agosto se han pagado 1.543,80 -pero repartidos
--                           entre el 970 de agosto y el de septiembre-. El dinero
--                           cuadra; el concepto total no puede cuadrar
--   A_DEV, A_DED, A_EMP     los tres tecnicos que meten los atrasos en esos
--                           totales: un atraso de un atraso no existe
--
-- Es la MISMA lista que RetroDeltaCalculator.NO_VIAJAN, y que sea la misma es la
-- propiedad que hace segura la invariante: lo que no viaja es exactamente lo que
-- la invariante no puede comparar. Hay un candado que lo cruza
-- (TheInvariantAndTheDeltaAgreeOnWhatDoesNotTravelTest).
--
-- Y se compara POR EMPLEADO y por mes, sumando las presencias de los dos lados:
-- «lo cobrado por agosto» es dinero del empleado, y que la linea de atraso acabe
-- en el recibo de una presencia o de otra es un detalle documental (ADR-074 §3).
--
-- Solo de los meses que TIENEN vigente: un mes que nunca se recalculo no tiene
-- con que compararse, y su recibo es la verdad.

with vigente as (
    select v.employee_number, v.payroll_period_code, c.concept_code,
           sum(c.amount) as importe
      from payroll.current_calculation v
      join payroll.current_calculation_concept c
        on c.current_calculation_id = v.id
     group by 1, 2, 3
),
pagado as (
    select p.employee_number, c.origin_period_code as payroll_period_code, c.concept_code,
           sum(c.amount) as importe
      from payroll.payroll p
      join payroll.payroll_concept c on c.payroll_id = p.id
     where p.status = 'DEFINITIVE'
     group by 1, 2, 3
),
meses_con_vigente as (
    select distinct employee_number, payroll_period_code from vigente
)
select m.employee_number, m.payroll_period_code,
       coalesce(v.concept_code, g.concept_code) as concept_code,
       coalesce(v.importe, 0) as vale_hoy,
       coalesce(g.importe, 0) as se_ha_pagado
  from meses_con_vigente m
  left join vigente v
    on v.employee_number = m.employee_number
   and v.payroll_period_code = m.payroll_period_code
  full outer join pagado g
    on g.employee_number = m.employee_number
   and g.payroll_period_code = m.payroll_period_code
   and g.concept_code = v.concept_code
 where m.employee_number like :emp
   -- Los conceptos del mes que PAGA, que no se atribuyen a un mes y por eso no viajan:
   -- el IRPF, los cuatro totales y los tres tecnicos de los atrasos. Es la misma lista
   -- que RetroDeltaCalculator.NO_VIAJAN, y que sea la misma es la propiedad.
   and coalesce(v.concept_code, g.concept_code) not in
       ('800', '970', '980', '990', '725', 'A_DEV', 'A_DED', 'A_EMP')
   and coalesce(v.importe, 0) <> coalesce(g.importe, 0)
 order by 2, 3
