-- =========================================================
-- V151__the_professional_base_starts_from_the_cotizable_base_not_from_the_capped_one.sql
-- La base de profesionales no se apoya en la base minima del grupo (backend#121)
-- =========================================================
--
-- La V148 monto el apartado 2 del recuadro asi:
--
--     B05 = B_CC            la base de comunes YA TOPADA
--     B07 = B05 + 102
--     B_CP = topes(B07)
--
-- y eso es lo que la Orden de cotizacion no dice. La base de contingencias
-- profesionales se determina con las mismas normas que la de comunes
-- —remuneracion mensual mas prorrata mas horas extraordinarias— y se limita por
-- el TOPE MINIMO y el TOPE MAXIMO. Las bases minimas POR GRUPO son solo de
-- contingencias comunes (Orden PJC/297/2026, arts. 1.2 y 2.2: el tope minimo es
-- el salario minimo interprofesional incrementado en un sexto, sin que pueda ser
-- inferior a 1.424,40 EUR).
--
-- Apoyar la base profesional en la de comunes ya topada le mete por la puerta de
-- atras un minimo que no le corresponde. Se ve en dos sitios:
--
--   EMP000008 de la semilla: remuneracion 1.200,00 + 100,00 de horas = 1.300,00.
--   Hoy sale B_CP = 1.323,00 + 100,00 = 1.423,00; con la Orden es
--   max(1.300,00, tope minimo), que con el tope sembrado hoy son 1.323,00.
--
--   Un grupo 1 con 1.500,00 de salario: B_CC sube a 1.847,40 por el minimo de su
--   grupo y B_CP se queda en 1.500,00. Ahi las dos bases no coinciden ni
--   sumandole las horas extra, que es lo que el cableado de la V148 daba por
--   hecho.
--
-- ---------------------------------------------------------
-- 1. El apartado 2 arranca de la base cotizable
-- ---------------------------------------------------------
-- La cadena de recorte no se toca y ya era la correcta:
--
--     B_CP_MAX = LEAST(B07, P_TOPE_MAX_CP)
--     B_CP     = GREATEST(B_CP_MAX, P_TOPE_MIN_CP)
--
-- Lo unico que cambia es de donde sale el primer sumando: del B01 —remuneracion
-- mensual mas prorrata, sin topes— y no del B_CC.
delete from payroll_engine.payroll_concept_feed_relation
 where source_object_id = (
        select id from payroll_engine.payroll_object
         where rule_system_code = 'ESP' and object_type_code = 'CONCEPT' and object_code = 'B_CC')
   and target_object_id = (
        select id from payroll_engine.payroll_object
         where rule_system_code = 'ESP' and object_type_code = 'CONCEPT' and object_code = 'B05');

insert into payroll_engine.payroll_concept_feed_relation (
    source_object_id, target_object_id, feed_mode, feed_value, invert_sign, effective_from, effective_to
)
select origen.id, destino.id, 'FEED_BY_SOURCE', cast(null as numeric), false, DATE '2025-01-01', cast(null as date)
from payroll_engine.payroll_object origen, payroll_engine.payroll_object destino
where origen.rule_system_code = 'ESP' and origen.object_type_code = 'CONCEPT' and origen.object_code = 'B01'
  and destino.rule_system_code = 'ESP' and destino.object_type_code = 'CONCEPT' and destino.object_code = 'B05'
  and not exists (
    select 1 from payroll_engine.payroll_concept_feed_relation x
     where x.source_object_id = origen.id and x.target_object_id = destino.id);

-- ---------------------------------------------------------
-- 2. Y el literal lo dice
-- ---------------------------------------------------------
-- «Base de contingencias comunes» a secas era cierto mientras la linea valia la
-- base topada, y desde este arreglo seria mentira justo en los recibos donde
-- importa: los que tienen el minimo de su grupo por encima de su remuneracion.
-- El nombre tiene que decir cual de las dos magnitudes es, porque en el bloque
-- de arriba estan las dos y se parecen.
update payroll_engine.payroll_concept_label l
   set label      = 'Base de contingencias comunes antes de topes',
       updated_at = current_timestamp
  from payroll_engine.payroll_object o
 where l.object_id = o.id and l.language_code = 'es'
   and o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = 'B05';
