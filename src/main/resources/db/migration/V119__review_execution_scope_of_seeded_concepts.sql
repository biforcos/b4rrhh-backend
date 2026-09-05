-- =========================================================
-- V119__review_execution_scope_of_seeded_concepts.sql
-- Repasar el ambito de ejecucion de los 36 conceptos sembrados (ADR-058, backend#64)
-- =========================================================
--
-- Desde backend#64 el motor lee execution_scope: un concepto SEGMENT se evalua
-- en cada tramo de jornada y su valor de periodo es la suma; uno PERIOD se
-- evalua una vez sobre el periodo entero, con los feeds ya compuestos. Hasta
-- ahora nadie lo leia y los 36 conceptos estaban en PERIOD sin que nadie lo
-- hubiera decidido (salvo el 101, que la V118 dejo en SEGMENT como piloto).
--
-- Repasados uno a uno, pasan a SEGMENT los que dependen del tramo:
--
--   D01 DIAS_DEVENGO         min(dias del tramo, 30): es un valor por tramo por
--                            definicion (V74), y 15 + 15 = 30 solo si se suma.
--   J01 COEFICIENTE_JORNADA  la jornada es justo lo que cambia entre tramos.
--   P01 PRECIO_DIA           J01 x P02: lee J01 como operando, y un PERIOD no
--                            puede leer un operando SEGMENT (backend#63).
--   101 SALARIO_BASE         D01 x P01, ya en SEGMENT desde la V118: con esto la
--                            V118 deja de ser un piloto y pasa a ser la
--                            declaracion correcta.
--
-- Se quedan en PERIOD los otros 32:
--
--   P02 PRECIO_DIA_PLENO     un unico precio del rule system para el periodo.
--   D02, D03                 constantes del mes (30 y dias naturales).
--   P_* (tipos SS e IRPF)    tasas del periodo, iguales en todos los tramos.
--   P_TOPE_MAX, P_TOPE_MIN   el tope y el suelo del mes, prorrateados una vez
--                            por los dias cubiertos; los leen como operando
--                            B_CC_MAX y B_CC, que son PERIOD, asi que no pueden
--                            ser SEGMENT. Su result_composition_mode ACCUMULATE
--                            (V90) queda inerte: lo retira backend#60.
--   B01, B_CC_MAX, B_CC      la base y el tope/suelo aplicados una vez sobre
--                            el mes, que es el caso que justifica el campo.
--   700-703, 720-724, 800    porcentajes sobre bases de periodo.
--   970, 980, 990            agregados del recibo.

update payroll_engine.payroll_concept concept
set execution_scope = 'SEGMENT',
    updated_at      = current_timestamp
from payroll_engine.payroll_object object
where object.id               = concept.object_id
  and object.rule_system_code = 'ESP'
  and object.object_type_code = 'CONCEPT'
  and object.object_code      in ('D01', 'J01', 'P01')
  and concept.execution_scope <> 'SEGMENT';
