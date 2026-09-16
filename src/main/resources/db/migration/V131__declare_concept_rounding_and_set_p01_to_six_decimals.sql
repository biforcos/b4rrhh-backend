-- =========================================================
-- V131__declare_concept_rounding_and_set_p01_to_six_decimals.sql
-- Los decimales y el modo de redondeo son del concepto (backend#61, ADR-066)
-- =========================================================
--
-- Hasta aqui el redondeo eran tres constantes repartidas por el motor, las tres
-- a 2 y HALF_UP: en RateByQuantityOperandResolver, en PercentageConceptResolver
-- y en el caso AGGREGATE de DefaultSegmentExecutionEngine. No habia forma de
-- decir que el precio por dia necesita seis decimales y los dias ninguno.
--
-- Una nomina paga DIAS. El decimal aparece donde se aplica una tasa —el precio
-- derivado P01 = P02 x J01, y los porcentajes— y solo ahi. El concepto ES el
-- sitio donde se aplica una tasa, asi que es el sitio donde se declara.
--
-- ---------------------------------------------------------
-- Los valores por omision, y por que no cambian nada
-- ---------------------------------------------------------
-- 2 y HALF_UP es exactamente lo que el motor hacia. Un concepto que no declare
-- otra cosa se queda como estaba, asi que esta migracion no mueve ni un importe
-- salvo donde se declara algo distinto a proposito — que es un sitio: P01.
--
-- ---------------------------------------------------------
-- P01 a seis decimales: la decision, y lo que costo medirla
-- ---------------------------------------------------------
-- Medido sobre los 873 recibos de la semilla antes de tocar nada:
--
--     tramos P01 medidos            878
--     tramos P01 que cambian          1
--     recibos que cambian             1      (EMP000003)
--     diferencia                  -0,07 EUR de devengos, -0,01 de IRPF
--
-- Con media jornada y precio de categoria 61,67, P01 sale 30,835 y se guardaba
-- 30,84: quince dias a cinco milesimas son siete centimos. Los otros cuatro
-- meses partidos de la semilla no cambian porque 0,5 x 47,50 y 0,5 x 40,00 son
-- exactos a dos decimales.
--
-- Siete centimos es lo que da ESTA semilla, no lo que da la regla: solo hay
-- cinco meses partidos, de un solo corte, con la jornada al 50 %, que es el
-- divisor mas amable que existe. Con un tercio de jornada son diez centimos al
-- mes por empleado, y el backend#47 va a multiplicar los tramos. Por eso se
-- elige ahora, que es cuando cuesta un recibo.
--
-- ---------------------------------------------------------
-- 1. Las dos columnas
-- ---------------------------------------------------------
alter table payroll_engine.payroll_concept
    -- integer y no smallint: la entidad JPA lo mapea a Integer y la validacion de
    -- esquema de Hibernate rechaza int2 contra Integer al arrancar. Lo caza el
    -- primer test que levanta el contexto, pero el aviso vale mas aqui.
    add column if not exists rounding_scale integer      not null default 2,
    add column if not exists rounding_mode  varchar(20)  not null default 'HALF_UP';

-- Los valores del enum java.math.RoundingMode que tienen sentido en nomina. No
-- se admite UNNECESSARY: un concepto que declare "no hace falta redondear" y se
-- encuentre un decimal revienta la corrida entera, y eso no es una decision de
-- parametrizacion sino una forma de romper la nomina desde el catalogo.
alter table payroll_engine.payroll_concept
    add constraint chk_payroll_concept_rounding_mode
        check (rounding_mode in ('HALF_UP', 'HALF_DOWN', 'HALF_EVEN', 'UP', 'DOWN', 'CEILING', 'FLOOR'));

alter table payroll_engine.payroll_concept
    add constraint chk_payroll_concept_rounding_scale
        check (rounding_scale between 0 and 6);

-- ---------------------------------------------------------
-- 2. Lo declarado, concepto a concepto
-- ---------------------------------------------------------
-- Cada uno con su motivo. Lo que no aparece aqui se queda en el defecto, que es
-- el mismo 2 / HALF_UP de siempre.
update payroll_engine.payroll_concept c
   set rounding_scale = v.scale
  from (values
        -- Los dias son enteros. Un dia y medio no existe en la nomina.
        ('D01', 0), ('D02', 0), ('D03', 0),
        -- La jornada es una proporcion: un tercio es 0,333333 y no 0,33.
        ('J01', 6),
        -- EL QUE IMPORTA. El precio por dia sale de multiplicar por la jornada,
        -- y lo que se guarde aqui multiplica despues por los dias del tramo.
        ('P01', 6)
       ) as v(concept_code, scale),
       payroll_engine.payroll_object o
 where o.id = c.object_id
   and o.object_type_code = 'CONCEPT'
   and o.object_code = v.concept_code
   and o.rule_system_code = 'ESP';

-- ---------------------------------------------------------
-- 3. Lo que esta migracion NO hace
-- ---------------------------------------------------------
-- No toca los importes ya calculados. Los 873 recibos de la semilla siguen con
-- los numeros que tienen hasta que se relancen; el que cambia lo hara cuando se
-- recalcule, y por eso la semilla hay que recapturarla (backend#61, criterio 5).
