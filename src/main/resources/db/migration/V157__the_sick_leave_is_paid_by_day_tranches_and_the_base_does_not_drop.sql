-- =========================================================
-- V157__the_sick_leave_is_paid_by_day_tranches_and_the_base_does_not_drop.sql
-- La prestacion por incapacidad temporal (backend#129, paso 5 de workspace#9,
-- ADR-075)
-- =========================================================
--
-- El tercio que mueve importes. Tres cosas a la vez:
--
--   1. La PRESTACION, por tramos de dias de baja, contados desde el inicio de la
--      ausencia -que puede estar en otro mes-. No es salarial: tributa y no cotiza.
--   2. El COMPLEMENTO del convenio, que rellena hasta el 100 % del salario base de
--      grupo.
--   3. La BASE DURANTE LA BAJA, para que la base de cotizacion no baje con los
--      dias que el backend#127 quito.
--
-- Todas las cifras, verificadas articulo por articulo contra el BOE por xml.php.

-- ---------------------------------------------------------
-- 1. El testigo de derecho a prestacion
-- ---------------------------------------------------------
-- La carencia -180 dias cotizados en los cinco anos inmediatamente anteriores,
-- art. 172.a) de la LGSS (BOE-A-2015-11724)- es la vida del empleado FUERA de
-- esta empresa, y la decide el INSS. La nomina no la puede calcular y no lo
-- intenta: esto es un DATO que pone quien registra la baja con la resolucion
-- delante, no una regla.
--
-- Con derecho por omision, y el defecto no es comodidad: casi todo el mundo
-- cumple la carencia, asi que el valor por omision es el caso normal. Sin
-- derecho, la baja quita dias (backend#127) y no paga nada ni cotiza.
--
-- Va en la ausencia y no en el empleado porque es de la baja: el mismo empleado
-- puede tener una baja con derecho y otra sin el.
alter table employee.employee_absence
    add column if not exists benefit_entitled boolean not null default true;

comment on column employee.employee_absence.benefit_entitled is
    'Si la baja lleva derecho a prestacion economica. Lo decide el INSS con la carencia del art. 172.a) LGSS; la nomina no lo calcula, lo lee (backend#129).';

-- ---------------------------------------------------------
-- 2. Los tramos, en una tabla y con su cita
-- ---------------------------------------------------------
-- Los limites y los porcentajes son cifras de una norma: cambian sin que cambie
-- el programa, y cada una tiene que poder llevar su cita al lado. Es la misma
-- decision que el tipo de desempleo (ADR-072) y los topes de cada ejercicio
-- (V153), y la contraria de un if en Java.
create table payroll_engine.it_prestacion_tramo (
    rule_system_code   varchar(10)  not null,
    absence_type_code  varchar(50)  not null,
    tramo_code         varchar(30)  not null,
    day_from           integer      not null,
    day_to             integer,
    percentage         numeric(5,2) not null,
    payer_code         varchar(20)  not null,
    effective_from     date         not null,
    effective_to       date,
    legal_reference    varchar(400) not null,
    created_at         timestamp    not null default now(),
    -- Sin zona horaria, y a proposito: la zona se le pone a las columnas que ALGUIEN
    -- COMPARA con otra (V143), y esta tabla no entra en la comparacion de la
    -- reglamentacion. Un candado lo vigila.
    updated_at         timestamp    not null default now(),
    constraint pk_it_prestacion_tramo
        primary key (rule_system_code, absence_type_code, tramo_code, effective_from),
    -- El dia 1 es el primero de la baja, y un tramo sin fin lleva day_to nulo. El
    -- check admite el nulo a proposito: un check que deja pasar un nulo no
    -- comprueba (V154), y aqui el nulo SIGNIFICA algo, asi que se nombra.
    constraint chk_it_tramo_dias
        check (day_from >= 1 and (day_to is null or day_to >= day_from)),
    constraint chk_it_tramo_porcentaje
        check (percentage >= 0 and percentage <= 100),
    constraint chk_it_tramo_pagador
        check (payer_code in ('EMPLOYER', 'DELEGATED'))
);

comment on table payroll_engine.it_prestacion_tramo is
    'Tramos de dias de la prestacion por incapacidad temporal, con su porcentaje, quien paga y la norma de la que sale (backend#129).';

comment on column payroll_engine.it_prestacion_tramo.day_from is
    'Primer dia del tramo, contado desde el inicio de la ausencia. El dia 1 es el primer dia de baja.';

comment on column payroll_engine.it_prestacion_tramo.day_to is
    'Ultimo dia del tramo, incluido. Nulo es un tramo que no termina.';

comment on column payroll_engine.it_prestacion_tramo.payer_code is
    'EMPLOYER: a cargo de la empresa. DELEGATED: de la Seguridad Social, en pago delegado.';

-- Enfermedad comun. Los tres tramos y lo que NO es un tramo -los tres primeros
-- dias, que no se pagan y por eso no tienen fila-:
--
--   1-3    nada.       Art. 173.1 LGSS: «el subsidio se abonara a partir del
--                      cuarto dia de baja en el trabajo».
--   4-15   60 %, empresa. El mismo art. 173.1: «desde el dia cuarto al
--                      decimoquinto de baja, ambos inclusive, el subsidio estara
--                      a cargo del empresario».
--   16-20  60 %, pago delegado. Articulo unico del RD 53/1980: el subsidio es
--                      «del sesenta por ciento de la base reguladora» «entre el
--                      cuarto dia [...] y hasta el veinteavo dia, inclusive».
--   21+    75 %, pago delegado. Art. 2.1 del Decreto 3158/1966: «un subsidio
--                      equivalente al setenta y cinco por ciento».
--
-- Que los tres primeros dias no tengan fila es la forma de decir que no se pagan:
-- un tramo al 0 % seria una linea de cero euros en el recibo, y la regla del cero
-- del backend#104 la quitaria igual. Sin fila, no hay nada que quitar.
--
-- Vigencia desde el 01/02/1980, que es cuando entro en vigor el RD 53/1980 -«el
-- primer dia del mes siguiente al de su publicacion»-, y es la fecha desde la que
-- el reparto 60/75 es el que esta escrito arriba.
insert into payroll_engine.it_prestacion_tramo (
    rule_system_code, absence_type_code, tramo_code,
    day_from, day_to, percentage, payer_code,
    effective_from, effective_to, legal_reference
)
values
    ('ESP', 'IT_COMMON', 'EMPRESA_60',  4, 15,   60.00, 'EMPLOYER',
     DATE '1980-02-01', null,
     'Art. 173.1 LGSS (RDL 8/2015, BOE-A-2015-11724): subsidio a partir del cuarto dia y a cargo del empresario del cuarto al decimoquinto, ambos inclusive. Porcentaje: art. unico RD 53/1980 (BOE-A-1980-1003).'),
    ('ESP', 'IT_COMMON', 'DELEGADO_60', 16, 20,  60.00, 'DELEGATED',
     DATE '1980-02-01', null,
     'Art. unico RD 53/1980 (BOE-A-1980-1003): 60 % de la base reguladora entre el cuarto dia y el veinte, inclusive. A partir del decimosexto ya no es a cargo del empresario (art. 173.1 LGSS), se abona en pago delegado.'),
    ('ESP', 'IT_COMMON', 'DELEGADO_75', 21, null, 75.00, 'DELEGATED',
     DATE '1980-02-01', null,
     'Art. 2.1 del Decreto 3158/1966 (BOE-A-1966-21116): subsidio equivalente al 75 % de la base reguladora. El RD 53/1980 rebajo al 60 % solo hasta el dia 20, asi que desde el 21 vuelve a regir el 75 %.')
on conflict (rule_system_code, absence_type_code, tramo_code, effective_from) do nothing;

-- ---------------------------------------------------------
-- 3. Los objetos
-- ---------------------------------------------------------
insert into payroll_engine.payroll_object (rule_system_code, object_type_code, object_code)
select 'ESP', 'CONCEPT', v.codigo
from (values ('D_IT_0'),
             ('D_IT_E60'), ('D_IT_D60'), ('D_IT_D75'),
             ('P_IT_E60'), ('P_IT_D60'), ('P_IT_D75'),
             ('T_IT_E60'), ('T_IT_D60'), ('T_IT_D75'),
             ('110'), ('111_D60'), ('111_D75'), ('111'),
             ('D_IT_C'), ('IT_100'), ('P_CERO'), ('IT_DIF'), ('112'),
             ('B10')) as v(codigo)
where not exists (
    select 1 from payroll_engine.payroll_object o
     where o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
);

-- ---------------------------------------------------------
-- 4. Los conceptos
-- ---------------------------------------------------------
-- La prestacion es DIAS x PORCENTAJE x BASE REGULADORA, y se escribe asi tres
-- veces, una por tramo, porque los tres tramos son tres cosas distintas y no un
-- porcentaje variable:
--
--   D_IT_*   cuantos dias del tramo del periodo caen en ese tramo de la baja.
--   P_IT_*   a que porcentaje se paga ese tramo.
--   T_IT_*   el importe DIARIO: PERCENTAGE(BR_CC, P_IT_*).
--   110      dias x importe diario, el tramo de la empresa. SE IMPRIME.
--   111_*    lo mismo para los dos tramos de pago delegado. No se imprimen.
--   111      su suma. SE IMPRIME.
--
-- Son DOS lineas y tres tramos a proposito: en el recibo, «prestacion a cargo de
-- la empresa» y «prestacion en pago delegado» son dos conceptos, y que el segundo
-- tenga dentro dos porcentajes es un detalle del calculo, que es donde se ve.
--
-- Y el complemento del convenio:
--
--   D_IT_C   los dias con prestacion: la suma de los tres tramos, o sea los dias
--            4 en adelante. Los tres primeros no llevan complemento.
--   IT_100   el 100 % del salario base de grupo de esos dias: D_IT_C x P01.
--   IT_DIF   IT_100 menos lo que paga la Seguridad Social.
--   112      GREATEST(IT_DIF, P_CERO). SE IMPRIME.
--
-- Y la base durante la baja:
--
--   B10      dias de baja x base reguladora. SE IMPRIME, en el recuadro de bases.
--
-- Todos SEGMENT menos los tres porcentajes: un porcentaje legal no depende de como
-- se parta el mes, y los dias si. Un concepto de tramo puede leer uno de periodo,
-- que es la direccion permitida (ADR-058).
insert into payroll_engine.payroll_concept (
    object_id, concept_mnemonic, calculation_type, functional_nature,
    payslip_order_code, execution_scope, rounding_scale, rounding_mode,
    payslip_subsection_code
)
select o.id, v.mnemonico, v.tipo, v.naturaleza, v.orden, v.ambito, v.decimales, 'HALF_UP',
       v.subseccion
from (values
        ('D_IT_0',   'DIAS_DE_BAJA',                'ENGINE_PROVIDED',  'TECHNICAL', cast(null as varchar), 'SEGMENT', 0, cast(null as varchar)),
        ('D_IT_E60', 'DIAS_BAJA_EMPRESA',           'ENGINE_PROVIDED',  'TECHNICAL', cast(null as varchar), 'SEGMENT', 0, cast(null as varchar)),
        ('D_IT_D60', 'DIAS_BAJA_DELEGADO_60',       'ENGINE_PROVIDED',  'TECHNICAL', cast(null as varchar), 'SEGMENT', 0, cast(null as varchar)),
        ('D_IT_D75', 'DIAS_BAJA_DELEGADO_75',       'ENGINE_PROVIDED',  'TECHNICAL', cast(null as varchar), 'SEGMENT', 0, cast(null as varchar)),
        ('P_IT_E60', 'TIPO_IT_EMPRESA',             'ENGINE_PROVIDED',  'TECHNICAL', cast(null as varchar), 'PERIOD',  2, cast(null as varchar)),
        ('P_IT_D60', 'TIPO_IT_DELEGADO_60',         'ENGINE_PROVIDED',  'TECHNICAL', cast(null as varchar), 'PERIOD',  2, cast(null as varchar)),
        ('P_IT_D75', 'TIPO_IT_DELEGADO_75',         'ENGINE_PROVIDED',  'TECHNICAL', cast(null as varchar), 'PERIOD',  2, cast(null as varchar)),
        ('T_IT_E60', 'PRESTACION_DIARIA_EMPRESA',   'PERCENTAGE',       'BASE',      cast(null as varchar), 'SEGMENT', 2, cast(null as varchar)),
        ('T_IT_D60', 'PRESTACION_DIARIA_DEL_60',    'PERCENTAGE',       'BASE',      cast(null as varchar), 'SEGMENT', 2, cast(null as varchar)),
        ('T_IT_D75', 'PRESTACION_DIARIA_DEL_75',    'PERCENTAGE',       'BASE',      cast(null as varchar), 'SEGMENT', 2, cast(null as varchar)),
        ('110',      'PRESTACION_IT_EMPRESA',       'RATE_BY_QUANTITY', 'EARNING',   '110',                 'SEGMENT', 2, cast(null as varchar)),
        ('111_D60',  'PRESTACION_IT_DELEGADO_60',   'RATE_BY_QUANTITY', 'BASE',      cast(null as varchar), 'SEGMENT', 2, cast(null as varchar)),
        ('111_D75',  'PRESTACION_IT_DELEGADO_75',   'RATE_BY_QUANTITY', 'BASE',      cast(null as varchar), 'SEGMENT', 2, cast(null as varchar)),
        ('111',      'PRESTACION_IT_PAGO_DELEGADO', 'AGGREGATE',        'EARNING',   '111',                 'SEGMENT', 2, cast(null as varchar)),
        ('D_IT_C',   'DIAS_BAJA_CON_PRESTACION',    'AGGREGATE',        'TECHNICAL', cast(null as varchar), 'SEGMENT', 0, cast(null as varchar)),
        ('IT_100',   'SALARIO_BASE_DE_LA_BAJA',     'RATE_BY_QUANTITY', 'BASE',      cast(null as varchar), 'SEGMENT', 2, cast(null as varchar)),
        ('P_CERO',   'CERO',                        'ENGINE_PROVIDED',  'TECHNICAL', cast(null as varchar), 'PERIOD',  0, cast(null as varchar)),
        ('IT_DIF',   'COMPLEMENTO_IT_BRUTO',        'AGGREGATE',        'BASE',      cast(null as varchar), 'SEGMENT', 2, cast(null as varchar)),
        ('112',      'COMPLEMENTO_IT_CONVENIO',     'GREATEST',         'EARNING',   '112',                 'SEGMENT', 2, cast(null as varchar)),
        ('B10',      'BASE_DURANTE_LA_BAJA',        'RATE_BY_QUANTITY', 'BASE',      '403',                 'SEGMENT', 2, 'BASE_CC')
     ) as v(codigo, mnemonico, tipo, naturaleza, orden, ambito, decimales, subseccion)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
where not exists (
    select 1 from payroll_engine.payroll_concept c where c.object_id = o.id
);

-- ---------------------------------------------------------
-- 5. El recuadro de bases gana una linea, y las de detras se corren
-- ---------------------------------------------------------
-- El primer bloque del recuadro pasa de cuatro lineas a cinco:
--
--   401  Remuneracion mensual                (B03)
--   402  Prorrata de pagas extraordinarias   (B04)
--   403  Base de cotizacion durante la baja  (B10)  <- nueva
--   404  Base de cotizacion                  (B01)  <- era 403
--   405  Base tras topes                     (B_CC) <- era 404
--
-- Se renumeran dos conceptos, y es presentacion y no importe: el orden de folio
-- decide donde se pinta la linea, no lo que vale. Las lineas ya congeladas llevan
-- su propio display_order y no se mueven (ADR-062): un recibo entregado no cambia
-- de forma porque el catalogo se reordene.
update payroll_engine.payroll_concept c
set payslip_order_code = '405',
    updated_at         = current_timestamp
from payroll_engine.payroll_object o
where o.id = c.object_id
  and o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = 'B_CC';

update payroll_engine.payroll_concept c
set payslip_order_code = '404',
    updated_at         = current_timestamp
from payroll_engine.payroll_object o
where o.id = c.object_id
  and o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = 'B01';

-- ---------------------------------------------------------
-- 6. Operandos
-- ---------------------------------------------------------
insert into payroll_engine.payroll_concept_operand (target_object_id, operand_role, source_object_id)
select destino.id, v.rol, origen.id
from (values
        ('T_IT_E60', 'BASE',       'BR_CC'),
        ('T_IT_E60', 'PERCENTAGE', 'P_IT_E60'),
        ('T_IT_D60', 'BASE',       'BR_CC'),
        ('T_IT_D60', 'PERCENTAGE', 'P_IT_D60'),
        ('T_IT_D75', 'BASE',       'BR_CC'),
        ('T_IT_D75', 'PERCENTAGE', 'P_IT_D75'),
        ('110',      'QUANTITY',   'D_IT_E60'),
        ('110',      'RATE',       'T_IT_E60'),
        ('111_D60',  'QUANTITY',   'D_IT_D60'),
        ('111_D60',  'RATE',       'T_IT_D60'),
        ('111_D75',  'QUANTITY',   'D_IT_D75'),
        ('111_D75',  'RATE',       'T_IT_D75'),
        ('IT_100',   'QUANTITY',   'D_IT_C'),
        ('IT_100',   'RATE',       'P01'),
        ('112',      'LEFT',       'IT_DIF'),
        ('112',      'RIGHT',      'P_CERO'),
        ('B10',      'QUANTITY',   'D_IT_0'),
        ('B10',      'RATE',       'BR_CC')
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
-- 7. Alimentaciones
-- ---------------------------------------------------------
-- Lo que decide que la prestacion NO cotice es que no alimente a B01, y eso ya
-- estaba en el modelo: no hacia falta ninguna marca nueva. Lo dice el art.
-- 147.2.d) de la LGSS: «no se computaran en la base de cotizacion [...] las
-- prestaciones de la Seguridad Social, las mejoras de las prestaciones por
-- incapacidad temporal concedidas por las empresas». Las tres lineas -las dos de
-- prestacion y el complemento del convenio- alimentan al 970 y a nadie mas.
--
-- Y lo que decide que TRIBUTEN es que el 970 es la base de la retencion desde la
-- V146: el 800 lee el total devengado y no la base de cotizacion.
--
-- La base durante la baja alimenta a B01, y el recuadro de bases se reconecta en el
-- apartado siguiente para que eso no cuente B10 dos veces.
insert into payroll_engine.payroll_concept_feed_relation (
    source_object_id, target_object_id, feed_mode, feed_value, invert_sign, effective_from, effective_to
)
select origen.id, destino.id, 'FEED_BY_SOURCE', cast(null as numeric), v.invertir,
       DATE '2025-01-01', cast(null as date)
from (values
        -- Las tres lineas al total devengado. Ninguna a B01: no cotizan.
        ('110',      '970',    false),
        ('111',      '970',    false),
        ('112',      '970',    false),
        -- Los dos tramos de pago delegado suman en su linea.
        ('111_D60',  '111',    false),
        ('111_D75',  '111',    false),
        -- Los dias con prestacion son los tres tramos juntos.
        ('D_IT_E60', 'D_IT_C', false),
        ('D_IT_D60', 'D_IT_C', false),
        ('D_IT_D75', 'D_IT_C', false),
        -- El complemento bruto: el 100 % del salario base menos lo que paga la SS.
        ('IT_100',   'IT_DIF', false),
        ('110',      'IT_DIF', true),
        ('111',      'IT_DIF', true),
        -- Y la base durante la baja, que es lo que impide que la base se hunda.
        ('B10',      'B01',    false)
     ) as v(origen, destino, invertir)
join payroll_engine.payroll_object origen
  on origen.rule_system_code = 'ESP' and origen.object_type_code = 'CONCEPT' and origen.object_code = v.origen
join payroll_engine.payroll_object destino
  on destino.rule_system_code = 'ESP' and destino.object_type_code = 'CONCEPT' and destino.object_code = v.destino
where not exists (
    select 1 from payroll_engine.payroll_concept_feed_relation x
     where x.source_object_id = origen.id and x.target_object_id = destino.id
);

-- ---------------------------------------------------------
-- 7 bis. El primer bloque del recuadro se reconecta al reves
-- ---------------------------------------------------------
-- Hasta aqui el bloque estaba escrito por resta:
--
--   B01 = 101 + 103 + B02          (los tres alimentaban a B01)
--   B03 = B01 - B04                (y la remuneracion mensual salia restando)
--
-- Anadir B10 a eso obliga a alimentar B03 con -B10 para que siga siendo el 101, y
-- entonces B10 le llega a B03 POR DOS CAMINOS -directo en negativo y dentro de B01
-- en positivo-. Eso es exactamente lo que el backend#120 puso un candado para
-- impedir, y el candado salto: «B03 suma B10 dos veces».
--
-- No se le pone una excepcion al candado. Se da la vuelta a la conexion, que
-- ademas es como habria que haberla escrito desde el principio:
--
--   B03 = 101                      (la remuneracion mensual ES el salario)
--   B04 = 103 + B02                (la prorrata, por la puerta que sea)
--   B01 = B03 + B04 + B10          (la base de cotizacion es la suma de los tres)
--
-- La invariante del paso 5 deja de ser algo que hay que comprobar y pasa a ser la
-- forma del grafo: B01 = B03 + B04 + B10 porque eso es literalmente lo que le
-- alimenta, y nadie mas.
--
-- Ni un importe se mueve. B03 valia B01 - B04 = 101 y ahora vale 101; B01 valia
-- 101 + 103 + B02 y ahora vale lo mismo mas B10, que es cero para quien no tiene
-- baja. Lo que cambia es que se puede leer.
delete from payroll_engine.payroll_concept_feed_relation f
using payroll_engine.payroll_object origen, payroll_engine.payroll_object destino
where f.source_object_id = origen.id
  and f.target_object_id = destino.id
  and origen.rule_system_code = 'ESP' and destino.rule_system_code = 'ESP'
  and (origen.object_code, destino.object_code) in
      (('101', 'B01'), ('103', 'B01'), ('B02', 'B01'), ('B01', 'B03'), ('B04', 'B03'));

insert into payroll_engine.payroll_concept_feed_relation (
    source_object_id, target_object_id, feed_mode, feed_value, invert_sign, effective_from, effective_to
)
select origen.id, destino.id, 'FEED_BY_SOURCE', cast(null as numeric), false,
       DATE '2025-01-01', cast(null as date)
from (values
        ('101', 'B03'),
        ('B03', 'B01'),
        ('B04', 'B01')
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
-- 8. Los nombres
-- ---------------------------------------------------------
-- Los de las tres lineas son los que un tecnico de nominas reconoce, y el del
-- complemento nombra el convenio, que es de donde sale.
insert into payroll_engine.payroll_concept_label (object_id, language_code, label)
select o.id, 'es', v.label
from (values
        ('D_IT_0',   'Dias de baja'),
        ('D_IT_E60', 'Dias de baja a cargo de la empresa'),
        ('D_IT_D60', 'Dias de baja en pago delegado al 60 %'),
        ('D_IT_D75', 'Dias de baja en pago delegado al 75 %'),
        ('P_IT_E60', 'Porcentaje de IT a cargo de la empresa'),
        ('P_IT_D60', 'Porcentaje de IT en pago delegado, dias 16 a 20'),
        ('P_IT_D75', 'Porcentaje de IT en pago delegado, dia 21 y siguientes'),
        ('T_IT_E60', 'Prestacion diaria a cargo de la empresa'),
        ('T_IT_D60', 'Prestacion diaria en pago delegado al 60 %'),
        ('T_IT_D75', 'Prestacion diaria en pago delegado al 75 %'),
        ('110',      'Prestacion por IT a cargo de la empresa'),
        ('111_D60',  'Prestacion por IT en pago delegado, dias 16 a 20'),
        ('111_D75',  'Prestacion por IT en pago delegado, dia 21 y siguientes'),
        ('111',      'Prestacion por IT en pago delegado'),
        ('D_IT_C',   'Dias de baja con prestacion'),
        ('IT_100',   'Salario base de grupo de los dias de baja'),
        ('P_CERO',   'Suelo de cero de un importe que no puede ser negativo'),
        ('IT_DIF',   'Complemento de IT antes del suelo'),
        ('112',      'Complemento de IT del convenio'),
        ('B10',      'Base de cotizacion durante la incapacidad temporal')
     ) as v(codigo, label)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
join payroll_engine.payroll_concept c on c.object_id = o.id
where not exists (
    select 1 from payroll_engine.payroll_concept_label l
     where l.object_id = o.id and l.language_code = 'es'
);

-- ---------------------------------------------------------
-- 9. Elegibilidad
-- ---------------------------------------------------------
-- Al convenio entero y a todo el mundo, como las dos puertas de la prorrata
-- (V146): quien no tiene baja obtiene cero en todo y un cero no se imprime desde
-- el backend#104. Acotar la asignacion no vale, porque la asignacion no conoce al
-- empleado — solo a su sociedad, su convenio y su tipo.
--
-- Se asignan los que son FUENTE DE UN AGREGADO, que no se expanden
-- (backend#110), y los que no son operando de nadie. Los T_IT_* y los P_IT_*
-- entran solos, como operandos.
insert into payroll_engine.concept_assignment
    (rule_system_code, concept_code, company_code, agreement_code, employee_type_code,
     valid_from, valid_to, priority)
select 'ESP', v.codigo, null, '99002405011982', null, DATE '2025-01-01', null, 129
from (values ('D_IT_0'), ('D_IT_E60'), ('D_IT_D60'), ('D_IT_D75'),
             ('110'), ('111_D60'), ('111_D75'), ('111'),
             ('D_IT_C'), ('IT_100'), ('IT_DIF'), ('112'),
             ('B10')) as v(codigo)
where not exists (
    select 1 from payroll_engine.concept_assignment a
     where a.rule_system_code = 'ESP' and a.concept_code = v.codigo
);
