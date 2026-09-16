-- =========================================================
-- V133__declare_overtime_as_the_first_employee_input_concept.sql
-- Horas extra: el primer concepto cuyo valor entra desde fuera (backend#104)
-- =========================================================
--
-- EMPLOYEE_INPUT lleva desde siempre en CalculationType y el motor lo resuelve
-- —lee context.getEmployeeInputs() por codigo de concepto— pero NINGUNO de los
-- 35 conceptos sembrados lo declaraba. La cadena estaba construida entera y
-- vacia por un extremo, y por eso employee.employee_payroll_input lleva meses a
-- cero filas (workforce-loader#5).
--
-- Lo que faltaba no era disenar un mecanismo: era declarar un concepto. Y antes,
-- la regla del cero (mitad 1 del backend#104), porque sin ella declarar esto
-- estrenaba una linea de 0,00 en los ~873 recibos de quien no tiene horas.
--
-- ---------------------------------------------------------
-- Por que horas extra y no un plus fijo
-- ---------------------------------------------------------
-- Porque ejercita CANTIDAD x PRECIO, que es la forma que el recibo ya sabe
-- pintar y el grafo ya sabe explicar; porque solo le aplica a algunos, que es lo
-- que hace que la plantilla se lea como real; y porque es el primer concepto
-- cuyo valor entra desde fuera en vez de salir del catalogo.
--
-- ---------------------------------------------------------
-- 1. Los objetos
-- ---------------------------------------------------------
insert into payroll_engine.payroll_object (rule_system_code, object_type_code, object_code)
select 'ESP', v.tipo, v.codigo
from (values ('CONCEPT', 'H01'), ('CONCEPT', 'P03'), ('CONCEPT', '102'),
             ('TABLE',   'P03_HOURLY_OVERTIME_TABLE')) as v(tipo, codigo)
where not exists (
    select 1 from payroll_engine.payroll_object o
     where o.rule_system_code = 'ESP' and o.object_type_code = v.tipo and o.object_code = v.codigo
);

-- ---------------------------------------------------------
-- 2. Los tres conceptos
-- ---------------------------------------------------------
-- H01 es la CANTIDAD que declara la persona. No lleva orden de folio porque no
-- es una linea: son horas, no dinero. Su naturaleza es INFORMATIONAL.
--
-- P03 es el precio de la hora. Sale de una tabla por categoria, como P02.
--
-- 102 es lo que se cobra, y es lo unico que llega al folio. Va detras del 101
-- porque es retribucion del mismo mes.
--
-- Los tres PERIOD: una entrada del empleado es del periodo, no del tramo, y un
-- operando no cruza de segmento a periodo (ADR-058).
insert into payroll_engine.payroll_concept (
    object_id, concept_mnemonic, calculation_type, functional_nature,
    payslip_order_code, execution_scope, rounding_scale, rounding_mode
)
select o.id, v.mnemonico, v.tipo_calculo, v.naturaleza, v.orden_folio, 'PERIOD', v.decimales, 'HALF_UP'
from (values
        ('H01', 'HORAS_EXTRA',         'EMPLOYEE_INPUT',   'INFORMATIONAL', cast(null as varchar), 2),
        ('P03', 'PRECIO_HORA_EXTRA',   'DIRECT_AMOUNT',    'BASE',          cast(null as varchar), 6),
        ('102', 'IMPORTE_HORAS_EXTRA', 'RATE_BY_QUANTITY', 'EARNING',       '102',                 2)
     ) as v(codigo, mnemonico, tipo_calculo, naturaleza, orden_folio, decimales)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
where not exists (
    select 1 from payroll_engine.payroll_concept c where c.object_id = o.id
);

-- ---------------------------------------------------------
-- 3. Operandos: 102 = H01 x P03
-- ---------------------------------------------------------
insert into payroll_engine.payroll_concept_operand (target_object_id, operand_role, source_object_id)
select destino.id, v.rol, origen.id
from (values ('102', 'QUANTITY', 'H01'), ('102', 'RATE', 'P03')) as v(destino, rol, origen)
join payroll_engine.payroll_object destino
  on destino.rule_system_code = 'ESP' and destino.object_type_code = 'CONCEPT' and destino.object_code = v.destino
join payroll_engine.payroll_object origen
  on origen.rule_system_code = 'ESP' and origen.object_type_code = 'CONCEPT' and origen.object_code = v.origen
where not exists (
    select 1 from payroll_engine.payroll_concept_operand x
     where x.target_object_id = destino.id and x.operand_role = v.rol
);

-- ---------------------------------------------------------
-- 4. Alimentaciones: la tabla alimenta P03, y 102 suma a los devengos
-- ---------------------------------------------------------
insert into payroll_engine.payroll_concept_feed_relation (
    source_object_id, target_object_id, feed_mode, feed_value, invert_sign, effective_from, effective_to
)
select origen.id, destino.id, 'FEED_BY_SOURCE', cast(null as numeric), false, DATE '2025-01-01', cast(null as date)
from (values
        ('TABLE',   'P03_HOURLY_OVERTIME_TABLE', 'P03'),
        ('CONCEPT', '102',                       '970'),
        ('CONCEPT', '102',                       '990')
     ) as v(tipo_origen, origen, destino)
join payroll_engine.payroll_object origen
  on origen.rule_system_code = 'ESP' and origen.object_type_code = v.tipo_origen and origen.object_code = v.origen
join payroll_engine.payroll_object destino
  on destino.rule_system_code = 'ESP' and destino.object_type_code = 'CONCEPT' and destino.object_code = v.destino
where not exists (
    select 1 from payroll_engine.payroll_concept_feed_relation x
     where x.source_object_id = origen.id and x.target_object_id = destino.id
);

-- Y a la base de cotizacion: las horas extra cotizan.
insert into payroll_engine.payroll_concept_feed_relation (
    source_object_id, target_object_id, feed_mode, feed_value, invert_sign, effective_from, effective_to
)
select origen.id, destino.id, 'FEED_BY_SOURCE', cast(null as numeric), false, DATE '2025-01-01', cast(null as date)
from payroll_engine.payroll_object origen, payroll_engine.payroll_object destino
where origen.rule_system_code = 'ESP' and origen.object_type_code = 'CONCEPT' and origen.object_code = '102'
  and destino.rule_system_code = 'ESP' and destino.object_type_code = 'CONCEPT' and destino.object_code = 'B01'
  and exists (select 1 from payroll_engine.payroll_object b
               where b.rule_system_code = 'ESP' and b.object_type_code = 'CONCEPT' and b.object_code = 'B01')
  and not exists (
    select 1 from payroll_engine.payroll_concept_feed_relation x
     where x.source_object_id = origen.id and x.target_object_id = destino.id
);

-- ---------------------------------------------------------
-- 5. El precio de la hora, por categoria
-- ---------------------------------------------------------
-- Sale del precio por dia de la tabla P02 dividido entre OCHO horas de jornada,
-- que es la que declara el propio convenio en employee.working_time
-- (daily_hours 8,00). No lleva recargo: la hora extra se paga como la ordinaria.
--
-- Eso NO es una afirmacion sobre la norma: es la opcion que no inventa nada. El
-- recargo de una hora extra es una decision de quien gobierna el convenio, y el
-- dia que se decida se cambian estas tres filas sin tocar el grafo.
--
--   G1  61,67 / 8 = 7,708750 -> 7,71
--   G2  47,50 / 8 = 5,937500 -> 5,94
--   G3  40,00 / 8 = 5,000000 -> 5,00
insert into payroll.payroll_object_binding (
    rule_system_code, owner_type_code, owner_code, binding_role_code,
    bound_object_type_code, bound_object_code, active
)
select 'ESP', 'AGREEMENT', '99002405011982', 'P03_HOURLY_OVERTIME_TABLE', 'TABLE', 'P03_99002405011982', true
where not exists (
    select 1 from payroll.payroll_object_binding b
     where b.rule_system_code = 'ESP' and b.owner_type_code = 'AGREEMENT'
       and b.owner_code = '99002405011982' and b.binding_role_code = 'P03_HOURLY_OVERTIME_TABLE'
);

insert into payroll.payroll_table_row (
    rule_system_code, table_code, search_code, start_date, end_date, daily_value, active
)
select 'ESP', 'P03_99002405011982', v.categoria, DATE '2025-01-01', cast(null as date), v.precio, true
from (values ('99002405-G1', 7.71), ('99002405-G2', 5.94), ('99002405-G3', 5.00))
        as v(categoria, precio)
where not exists (
    select 1 from payroll.payroll_table_row r
     where r.rule_system_code = 'ESP' and r.table_code = 'P03_99002405011982'
       and r.search_code = v.categoria and r.start_date = DATE '2025-01-01'
);

-- ---------------------------------------------------------
-- 6. Elegibilidad
-- ---------------------------------------------------------
-- Solo el 102: las dependencias se expanden solas, asi que H01 y P03 entran con
-- el. Y se asigna al convenio entero y no a nadie en particular, que es lo que
-- concept_assignment sabe hacer — quien tenga horas lo cobra y a quien no las
-- tenga le sale cero, y un cero no se imprime desde la mitad 1 de este issue.
insert into payroll_engine.concept_assignment
    (rule_system_code, concept_code, company_code, agreement_code, employee_type_code,
     valid_from, valid_to, priority)
select 'ESP', '102', null, '99002405011982', null, DATE '2025-01-01', null, 102
where not exists (
    select 1 from payroll_engine.concept_assignment a
     where a.rule_system_code = 'ESP' and a.concept_code = '102'
);

-- ---------------------------------------------------------
-- 7. Lo que esta migracion NO hace
-- ---------------------------------------------------------
-- No siembra ni una fila de employee_payroll_input: eso es del loader y va en el
-- workforce-loader#5. Aqui solo se declara el concepto y se comprueba que
-- calcula.
