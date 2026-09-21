-- =========================================================
-- V146__the_proration_enters_the_payslip_by_one_of_two_doors.sql
-- La prorrata de pagas extras (backend#119, paso 4 de workspace#9, ADR-070)
-- =========================================================
--
-- La primera forma que el modelo no sabia: un valor que viene de un IMPORTE
-- ANUAL REPARTIDO, y no de cantidad x tarifa. Y cotiza, asi que mueve la base y
-- con ella media nomina.
--
-- ---------------------------------------------------------
-- 1. Dividir entre doce, que es exacto
-- ---------------------------------------------------------
-- El motor no sabia dividir. Se le ensena con un tipo de calculo que lo dice,
-- QUOTIENT, con sus dos operandos: BASE entre DIVISOR.
--
-- No es un PERCENTAGE al 8,33 %. Escribirlo asi seria redondear dos veces —una
-- al escribir el tipo y otra al aplicarlo— y el backend#61 dejo dicho que nada
-- se redondea dos veces. Entre doce hay un solo redondeo: el que el motor aplica
-- al salir, con los decimales que el concepto declara.
--
-- El divisor es un concepto y no una constante escondida en Java porque en este
-- motor lo que interviene en un calculo SE VE EN EL GRAFO. La pregunta «de donde
-- sale este numero» no puede tener una respuesta que solo este en el codigo.
insert into payroll_engine.payroll_object (rule_system_code, object_type_code, object_code)
select 'ESP', 'CONCEPT', v.codigo
from (values ('PE_TOTAL'), ('P_MESES_ANO'), ('P_PRORRATA'),
             ('J_PRORRATEADAS'), ('J_NO_PRORRATEADAS'),
             ('103'), ('B02')) as v(codigo)
where not exists (
    select 1 from payroll_engine.payroll_object o
     where o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
);

-- ---------------------------------------------------------
-- 2. Los siete conceptos
-- ---------------------------------------------------------
-- PE_TOTAL          la suma de las cuatro pagas del convenio (backend#117).
-- P_MESES_ANO       doce. PERIOD: doce son doce en un mes partido tambien.
-- P_PRORRATA        PE_TOTAL / P_MESES_ANO. La cantidad, sin decidir por donde
--                   entra.
-- J_PRORRATEADAS    1 si el tramo va prorrateado, 0 si no.
-- J_NO_PRORRATEADAS su complemento.
-- 103               la prorrata QUE SE PAGA = J_PRORRATEADAS x P_PRORRATA.
-- B02               la prorrata QUE COTIZA  = J_NO_PRORRATEADAS x P_PRORRATA.
--
-- Todos SEGMENT menos P_MESES_ANO: el regimen puede cambiar a mitad de mes y
-- cada tramo entra por la puerta que le toca. Un concepto de tramo puede leer
-- uno de periodo, que es la direccion permitida (ADR-058).
--
-- Los dos coeficientes suman UNO siempre, y los dos conceptos alimentan B01: por
-- eso la base recibe la prorrata exactamente una vez sea cual sea el regimen.
-- La invariante del paso 4 —la base no sabe si se pago— es cierta POR
-- CONSTRUCCION y no por cuidado.
insert into payroll_engine.payroll_concept (
    object_id, concept_mnemonic, calculation_type, functional_nature,
    payslip_order_code, execution_scope, rounding_scale, rounding_mode
)
select o.id, v.mnemonico, v.tipo, v.naturaleza, v.orden, v.ambito, v.decimales, 'HALF_UP'
from (values
        ('PE_TOTAL',          'TOTAL_PAGAS_EXTRAS',      'AGGREGATE',       'BASE',          cast(null as varchar), 'SEGMENT', 2),
        ('P_MESES_ANO',       'MESES_DEL_ANO',           'ENGINE_PROVIDED', 'TECHNICAL',     cast(null as varchar), 'PERIOD',  0),
        ('P_PRORRATA',        'PRORRATA_MENSUAL',        'QUOTIENT',        'BASE',          cast(null as varchar), 'SEGMENT', 2),
        ('J_PRORRATEADAS',    'REGIMEN_PRORRATEADO',     'ENGINE_PROVIDED', 'TECHNICAL',     cast(null as varchar), 'SEGMENT', 0),
        ('J_NO_PRORRATEADAS', 'REGIMEN_NO_PRORRATEADO',  'ENGINE_PROVIDED', 'TECHNICAL',     cast(null as varchar), 'SEGMENT', 0),
        ('103',               'PRORRATA_PAGAS_EXTRAS',   'RATE_BY_QUANTITY','EARNING',       '103',                 'SEGMENT', 2),
        ('B02',               'PRORRATA_QUE_COTIZA',     'RATE_BY_QUANTITY','BASE',          '405',                 'SEGMENT', 2)
     ) as v(codigo, mnemonico, tipo, naturaleza, orden, ambito, decimales)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
where not exists (
    select 1 from payroll_engine.payroll_concept c where c.object_id = o.id
);

-- ---------------------------------------------------------
-- 3. Operandos
-- ---------------------------------------------------------
insert into payroll_engine.payroll_concept_operand (target_object_id, operand_role, source_object_id)
select destino.id, v.rol, origen.id
from (values
        ('P_PRORRATA', 'BASE',     'PE_TOTAL'),
        ('P_PRORRATA', 'DIVISOR',  'P_MESES_ANO'),
        ('103',        'QUANTITY', 'J_PRORRATEADAS'),
        ('103',        'RATE',     'P_PRORRATA'),
        ('B02',        'QUANTITY', 'J_NO_PRORRATEADAS'),
        ('B02',        'RATE',     'P_PRORRATA')
     ) as v(destino, rol, origen)
join payroll_engine.payroll_object destino
  on destino.rule_system_code = 'ESP' and destino.object_type_code = 'CONCEPT' and destino.object_code = v.destino
join payroll_engine.payroll_object origen
  on origen.rule_system_code = 'ESP' and origen.object_type_code = 'CONCEPT' and origen.object_code = v.origen
where not exists (
    select 1 from payroll_engine.payroll_concept_operand x
     where x.target_object_id = destino.id and x.operand_role = v.rol
);

-- ---------------------------------------------------------
-- 4. Alimentaciones
-- ---------------------------------------------------------
-- Las cuatro pagas suman en PE_TOTAL, y las dos puertas alimentan B01. La que se
-- paga alimenta ademas el total de devengos.
--
-- La que se paga NO alimenta al 990 directamente: llega al liquido por el 970,
-- que es el camino que dejo puesto la V77 al retirar el 101 -> 990. Sumar a las
-- dos seria pagarla dos veces, que es lo que le pasa hoy al 102 (backend#120).
insert into payroll_engine.payroll_concept_feed_relation (
    source_object_id, target_object_id, feed_mode, feed_value, invert_sign, effective_from, effective_to
)
select origen.id, destino.id, 'FEED_BY_SOURCE', cast(null as numeric), false, DATE '2025-01-01', cast(null as date)
from (values
        ('PE_1', 'PE_TOTAL'),
        ('PE_2', 'PE_TOTAL'),
        ('PE_3', 'PE_TOTAL'),
        ('PE_4', 'PE_TOTAL'),
        ('103',  'B01'),
        ('103',  '970'),
        ('B02',  'B01')
     ) as v(origen, destino)
join payroll_engine.payroll_object origen
  on origen.rule_system_code = 'ESP' and origen.object_type_code = 'CONCEPT' and origen.object_code = v.origen
join payroll_engine.payroll_object destino
  on destino.rule_system_code = 'ESP' and destino.object_type_code = 'CONCEPT' and destino.object_code = v.destino
where not exists (
    select 1 from payroll_engine.payroll_concept_feed_relation x
     where x.source_object_id = origen.id and x.target_object_id = destino.id
);

-- ---------------------------------------------------------
-- 5. La base del IRPF deja de ser la base de cotizacion
-- ---------------------------------------------------------
-- El 800 leia B01. Hasta hoy daba igual —B01 y 970 valen lo mismo: los dos se
-- alimentan del 101 y del 102— pero con la prorrata dejan de valerlo, y es justo
-- ahi donde se ve cual de los dos es la base de la retencion.
--
-- El IRPF se retiene sobre lo que se PAGA. Quien tiene las extras prorrateadas
-- cobra la prorrata todos los meses y tributa por ella todos los meses; quien no,
-- tributara por la paga entera el mes que la cobre. Que la prorrata que NO se
-- paga entrara en la base de la retencion seria retener por dinero que no se ha
-- entregado.
--
-- Por eso el 800 pasa a leer el 970, el total devengado. Se cambia AHORA, que es
-- cuando no cuesta ni un centimo en ningun recibo existente, y no el dia que
-- alguien lo note: es la misma decision que tomo la V131 con los decimales del
-- precio del dia.
--
-- Lo que B01 sigue siendo es la base de COTIZACION, que es lo que dice su nombre
-- (V136) y lo que leen B_CC_MAX y B_CC.
update payroll_engine.payroll_concept_operand op
set source_object_id = nuevo.id,
    updated_at       = current_timestamp
from payroll_engine.payroll_object destino,
     payroll_engine.payroll_object nuevo
where op.target_object_id = destino.id
  and op.operand_role     = 'BASE'
  and destino.rule_system_code = 'ESP' and destino.object_type_code = 'CONCEPT' and destino.object_code = '800'
  and nuevo.rule_system_code   = 'ESP' and nuevo.object_type_code   = 'CONCEPT' and nuevo.object_code   = '970';

-- ---------------------------------------------------------
-- 6. Los nombres
-- ---------------------------------------------------------
-- El de B02 es el del modelo oficial: «Prorrata de pagas extraordinarias» es
-- como se llama la linea del recuadro de bases que se suma a la remuneracion
-- mensual para dar la base de contingencias comunes.
insert into payroll_engine.payroll_concept_label (object_id, language_code, label)
select o.id, 'es', v.label
from (values
        ('PE_TOTAL',          'Total de pagas extraordinarias'),
        ('P_MESES_ANO',       'Meses del ano'),
        ('P_PRORRATA',        'Prorrata mensual de pagas extraordinarias'),
        ('J_PRORRATEADAS',    'Regimen de pagas extras prorrateado'),
        ('J_NO_PRORRATEADAS', 'Regimen de pagas extras no prorrateado'),
        ('103',               'Prorrata de pagas extraordinarias'),
        ('B02',               'Prorrata de pagas extraordinarias')
     ) as v(codigo, label)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
join payroll_engine.payroll_concept c on c.object_id = o.id
where not exists (
    select 1 from payroll_engine.payroll_concept_label l
     where l.object_id = o.id and l.language_code = 'es'
);

-- ---------------------------------------------------------
-- 7. Elegibilidad
-- ---------------------------------------------------------
-- Las dos puertas, al convenio entero y a todo el mundo. La que sobra da CERO
-- —su coeficiente vale cero— y un cero no se imprime desde el backend#104, asi
-- que el recibo de cada uno ensena exactamente una de las dos.
--
-- Eso es lo contrario de acotar la asignacion por regimen, y es a proposito: la
-- asignacion se resuelve una vez por periodo y el regimen puede cambiar a mitad
-- de mes (ADR-070). Los demas conceptos entran solos, como operandos.
insert into payroll_engine.concept_assignment
    (rule_system_code, concept_code, company_code, agreement_code, employee_type_code,
     valid_from, valid_to, priority)
select 'ESP', v.codigo, null, '99002405011982', null, DATE '2025-01-01', null, 119
from (values ('103'), ('B02')) as v(codigo)
where not exists (
    select 1 from payroll_engine.concept_assignment a
     where a.rule_system_code = 'ESP' and a.concept_code = v.codigo
);
