-- =========================================================
-- V147__the_overtime_reaches_the_net_pay_only_through_the_total_earnings.sql
-- El liquido suma las horas extra una vez, no dos (backend#120)
-- =========================================================
--
-- La V133 declaro dos alimentaciones del 102:
--
--     ('CONCEPT', '102', '970'),
--     ('CONCEPT', '102', '990'),
--
-- y el 970 ya alimentaba al 990. Asi que el liquido salia
--
--     990 = 101 + 2 x 102 - 980
--
-- y las horas extra se pagaban dos veces. En la semilla de la demo son 245 de
-- los 863 recibos; en los otros 618 el termino que sobra vale cero y por eso no
-- se veia.
--
-- La segunda sobra, y sobra desde el dia que se escribio: la V77 habia retirado
-- a proposito el 101 -> 990 al introducir el 970 -> 990 —«REMOVE: 101 -> 990
-- (direct earning feed, replaced by 970 -> 990)»— y el 102 entro despues
-- repitiendo el patron que aquella migracion acababa de quitar.
--
-- La regla que queda escrita: UN DEVENGO LLEGA AL LIQUIDO POR EL 970 Y POR
-- NINGUN OTRO SITIO. Nada se enchufa al 990 directamente (ADR-070).
--
-- Esta migracion no toca nada mas. El 102 -> 970 se queda —las horas extra son
-- devengo— y el 102 -> B01 tambien —cotizan—. Lo unico que se retira es el
-- atajo al liquido.
delete from payroll_engine.payroll_concept_feed_relation
where source_object_id = (
    select id from payroll_engine.payroll_object
     where rule_system_code = 'ESP' and object_type_code = 'CONCEPT' and object_code = '102'
)
and target_object_id = (
    select id from payroll_engine.payroll_object
     where rule_system_code = 'ESP' and object_type_code = 'CONCEPT' and object_code = '990'
);
