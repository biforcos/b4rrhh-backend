-- =========================================================
-- V148__the_official_model_has_three_bases_and_each_quota_reads_its_own.sql
-- Las tres bases de cotizacion del modelo oficial (backend#121)
-- =========================================================
--
-- El motor tenia UNA base de cotizacion, montada en la V88 para poder jugar con
-- los topes, y las nueve cuotas leian la misma. El modelo oficial tiene TRES, y
-- la simplificacion se veia en la demo: los 138,78 EUR de horas extra de
-- EMP001000 cotizaban por contingencias comunes, donde no van.
--
--   1. Contingencias comunes .................. remuneracion mensual + prorrata
--   2. Contingencias profesionales y
--      recaudacion conjunta ................... la de comunes + horas extra
--   3. Horas extraordinarias .................. el importe de las horas extra
--
-- y una cuarta magnitud que el recuadro imprime y que NO es una base de
-- cotizacion: la base sujeta a retencion del IRPF, que es el 970 (ADR-070 §4).
--
-- ---------------------------------------------------------
-- Quien lee cual
-- ---------------------------------------------------------
--   B_CC  (comunes)      700 CC trabajador, 702 MEI trabajador,
--                        720 CC empresario, 724 MEI empresario
--   B_CP  (profesionales) 701 FP trabajador, 703 desempleo trabajador,
--                        721 desempleo empresario, 722 FP empresario,
--                        723 FOGASA empresario  (y el AT/EP del backend#122)
--   B08   (horas extra)   704 cotizacion adicional del trabajador (4,70 %)
--                        726 cotizacion adicional de la empresa (23,60 %)
--
-- El MEI cotiza sobre la base de contingencias comunes y por eso el 702 y el
-- 724 se quedan donde estan. Los otros cinco cambian de base, y en un recibo
-- SIN horas extra no se mueven ni un centimo: B_CP = B_CC + 0.
--
-- ---------------------------------------------------------
-- La remuneracion mensual se define por exclusion, no por lista
-- ---------------------------------------------------------
-- «Remuneracion mensual» son los devengos cotizables sin horas extra y sin la
-- prorrata. Escribirlo como una lista de alimentaciones —hoy, solo el 101—
-- dejaria el concepto viejo el dia que alguien anada un plus: habria que
-- acordarse de alimentar los dos sitios, y olvidarlo no rompe nada, solo
-- descuadra el recuadro.
--
-- Asi que se escribe al reves, que es como lo dice el modelo:
--
--     B03 = B01 - B04
--
-- B01 sigue siendo la puerta por la que un devengo cotizable entra en la base,
-- igual que hasta hoy, y B03 es lo que queda al quitarle la prorrata. Un
-- devengo nuevo aparece en la remuneracion mensual sin tocar esta migracion.
--
-- Lo que se lee de arriba abajo en el papel —remuneracion + prorrata = base— y
-- lo que el motor calcula —base - prorrata = remuneracion— son la misma
-- identidad escrita en las dos direcciones. La del motor es la que no envejece.
--
-- Y las horas extra salen de B01 del todo: ya no lo alimentan. Ahi es donde
-- estaba el defecto.
--
-- ---------------------------------------------------------
-- Los topes, y una cita que descubre otra cosa
-- ---------------------------------------------------------
-- La base de contingencias profesionales tiene los suyos: el maximo es el
-- mismo, y el minimo es el TOPE MINIMO DE COTIZACION, que no es la base minima
-- del grupo. `ss_cotizacion_topes` no sabia decirlo —una fila por grupo y nada
-- mas— asi que se le anade la contingencia.
--
--   Tope minimo de cotizacion 2024: el salario minimo interprofesional
--   incrementado en un sexto (Orden PJC/51/2024, de 29 de enero, art. 2; BOE de
--   30 de enero de 2024). Con el SMI de 2024 (1.134,00 EUR/mes, RD 145/2024)
--   son 1.323,00 EUR/mes y 44,10 EUR/dia.
--
--   Tope maximo: el que la tabla ya trae, 4.909,50 EUR/mes y 163,65 EUR/dia.
--
-- DEFECTO HEREDADO QUE ESTA CITA DEJA A LA VISTA, y que no se arregla aqui:
-- las filas de la V88 mezclan dos ejercicios. Las bases minimas por grupo
-- (1.847,40 / 1.532,10 / 1.332,90 / 1.323,00 y 44,10) son las de 2024 —Orden
-- PJC/51/2024 en la redaccion de la Orden PJC/281/2024, art. 3— y el tope
-- maximo (4.909,50 / 163,65) es el de 2025 —Orden PJC/178/2025, de 25 de
-- febrero, arts. 2 y 3; BOE de 26 de febrero de 2025—, todas con
-- valid_from 2025-01-01.
--
-- Por eso el minimo de profesionales se siembra con la cifra de 2024 y no con
-- la de 2025 (1.381,20 EUR/mes y 46,04 EUR/dia): con la tabla mezclada, el
-- minimo de profesionales de 2025 quedaria POR ENCIMA de la base minima de los
-- grupos 04 a 11, y la demo ensenaria una base profesional mayor que la comun
-- para un salario bajo. Eso no pasa bajo ninguna orden: en la de 2025 las dos
-- cifras coinciden. El artefacto vendria de mezclar ejercicios, no del modelo.
--
-- Poner la tabla entera al ejercicio corriente mueve todos los recibos de base
-- baja y es otra decision y otro issue. Aqui se deja escrito, que es lo que no
-- estaba.
--
-- ---------------------------------------------------------
-- 1. La contingencia entra en los topes
-- ---------------------------------------------------------
alter table payroll_engine.ss_cotizacion_topes
    add column contingency_code varchar(30) not null default 'COMUNES';

comment on column payroll_engine.ss_cotizacion_topes.contingency_code is
    'Sobre que base actuan estos topes: COMUNES o PROFESIONALES. Existe porque el tope minimo de las contingencias profesionales lo fija la Orden de cotizacion y no es la base minima del grupo (backend#121).';

alter table payroll_engine.ss_cotizacion_topes
    drop constraint uq_ss_cotizacion_topes;

alter table payroll_engine.ss_cotizacion_topes
    add constraint uq_ss_cotizacion_topes
        unique (rule_system_code, grupo_code, period_type, contingency_code, valid_from);

-- El mismo maximo para todos, y el tope minimo de cotizacion —igual para los
-- once grupos, que es justo lo que lo distingue de la base minima del grupo.
insert into payroll_engine.ss_cotizacion_topes
    (rule_system_code, grupo_code, period_type, contingency_code, base_min, base_max, valid_from)
select v.rs, v.grupo, v.periodo, 'PROFESIONALES', v.minimo, v.maximo, DATE '2025-01-01'
from (values
        ('ESP', '01', 'MENSUAL', 1323.00, 4909.50),
        ('ESP', '02', 'MENSUAL', 1323.00, 4909.50),
        ('ESP', '03', 'MENSUAL', 1323.00, 4909.50),
        ('ESP', '04', 'MENSUAL', 1323.00, 4909.50),
        ('ESP', '05', 'MENSUAL', 1323.00, 4909.50),
        ('ESP', '06', 'MENSUAL', 1323.00, 4909.50),
        ('ESP', '07', 'MENSUAL', 1323.00, 4909.50),
        ('ESP', '08', 'DIARIO',    44.10,  163.65),
        ('ESP', '09', 'DIARIO',    44.10,  163.65),
        ('ESP', '10', 'DIARIO',    44.10,  163.65),
        ('ESP', '11', 'DIARIO',    44.10,  163.65)
     ) as v(rs, grupo, periodo, minimo, maximo)
where not exists (
    select 1 from payroll_engine.ss_cotizacion_topes e
     where e.rule_system_code = v.rs and e.grupo_code = v.grupo
       and e.period_type = v.periodo and e.contingency_code = 'PROFESIONALES'
       and e.valid_from = DATE '2025-01-01'
);

-- ---------------------------------------------------------
-- 2. Los dos tipos de la cotizacion adicional por horas extraordinarias
-- ---------------------------------------------------------
-- 28,30 % en total: 23,60 % a cargo de la empresa y 4,70 % a cargo de la
-- persona trabajadora (Orden PJC/178/2025, de 25 de febrero, art. 5; BOE de 26
-- de febrero de 2025). Son las horas extraordinarias que no son de fuerza
-- mayor; las de fuerza mayor cotizan al 14,00 % (12,00 + 2,00) y el motor no
-- distingue todavia unas de otras: el H01 es una sola cantidad.
--
-- Van a la tabla y con su vigencia, como los otros nueve (backend#105): un
-- tipo que cambie por ley se declara cerrando la fila y anadiendo otra.
insert into payroll_engine.ss_cotizacion_tipos
    (rule_system_code, contingency_code, rate, valid_from)
select v.rs, v.contingencia, v.tipo, DATE '2025-01-01'
from (values
        ('ESP', 'HORAS_EXTRA_TRAB',  4.70),
        ('ESP', 'HORAS_EXTRA_EMP',  23.60)
     ) as v(rs, contingencia, tipo)
where not exists (
    select 1 from payroll_engine.ss_cotizacion_tipos e
     where e.rule_system_code = v.rs and e.contingency_code = v.contingencia
       and e.valid_from = DATE '2025-01-01'
);

-- ---------------------------------------------------------
-- 3. Los objetos
-- ---------------------------------------------------------
insert into payroll_engine.payroll_object (rule_system_code, object_type_code, object_code)
select 'ESP', 'CONCEPT', v.codigo
from (values ('B03'), ('B04'), ('B05'), ('B06'), ('B07'), ('B08'), ('B09'),
             ('B_CP_MAX'), ('B_CP'),
             ('P_TOPE_MAX_CP'), ('P_TOPE_MIN_CP'),
             ('P_HE_TRAB'), ('P_HE_EMP'),
             ('704'), ('726')) as v(codigo)
where not exists (
    select 1 from payroll_engine.payroll_object o
     where o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
);

-- ---------------------------------------------------------
-- 4. Los conceptos
-- ---------------------------------------------------------
-- Todos PERIOD. Las bases de cotizacion se calculan una vez sobre el periodo
-- entero desde la V88, y los topes son lo unico que se prorratea por tramo y se
-- acumula (ADR-048). Meter una base de tramo aqui descuadraria el recorte.
--
-- B_CP_MAX no se imprime, por la misma razon que B_CC_MAX (V139): entre la base
-- y la base topada no hay nada que un tecnico de nominas tenga que ver.
insert into payroll_engine.payroll_concept (
    object_id, concept_mnemonic, calculation_type, functional_nature,
    payslip_order_code, execution_scope, rounding_scale, rounding_mode
)
select o.id, v.mnemonico, v.tipo, v.naturaleza, v.orden, 'PERIOD', 2, 'HALF_UP'
from (values
        -- 1. Contingencias comunes
        ('B03',           'REMUNERACION_MENSUAL',              'AGGREGATE',       'BASE',          '401'),
        ('B04',           'PRORRATA_EN_LA_BASE',               'AGGREGATE',       'BASE',          '402'),
        -- 2. Contingencias profesionales y recaudacion conjunta
        ('B05',           'BASE_COMUNES_EN_PROFESIONALES',     'AGGREGATE',       'BASE',          '411'),
        ('B06',           'HORAS_EXTRA_EN_PROFESIONALES',      'AGGREGATE',       'BASE',          '412'),
        ('B07',           'BASE_CONTINGENCIAS_PROFESIONALES',  'AGGREGATE',       'BASE',          '413'),
        ('B_CP_MAX',      'BASE_PROFESIONALES_MAX',            'LEAST',           'BASE',          cast(null as varchar)),
        ('B_CP',          'BASE_PROFESIONALES_COTIZ',          'GREATEST',        'BASE',          '414'),
        -- 3. Horas extraordinarias
        ('B08',           'BASE_HORAS_EXTRAORDINARIAS',        'AGGREGATE',       'BASE',          '421'),
        -- 4. Base sujeta a retencion del IRPF
        ('B09',           'BASE_SUJETA_A_RETENCION',           'AGGREGATE',       'BASE',          '431'),
        -- Los topes de la base profesional
        ('P_TOPE_MAX_CP', 'TOPE_MAX_PROFESIONALES',            'ENGINE_PROVIDED', 'TECHNICAL',     cast(null as varchar)),
        ('P_TOPE_MIN_CP', 'TOPE_MIN_PROFESIONALES',            'ENGINE_PROVIDED', 'TECHNICAL',     cast(null as varchar)),
        -- Los dos tipos y las dos cuotas de la cotizacion adicional
        ('P_HE_TRAB',     'TIPO_HORAS_EXTRA_TRABAJADOR',       'ENGINE_PROVIDED', 'TECHNICAL',     cast(null as varchar)),
        ('P_HE_EMP',      'TIPO_HORAS_EXTRA_EMPRESARIO',       'ENGINE_PROVIDED', 'TECHNICAL',     cast(null as varchar)),
        ('704',           'HORAS_EXTRA_TRABAJADOR',            'PERCENTAGE',      'DEDUCTION',     '704'),
        ('726',           'HORAS_EXTRA_EMPRESARIO',            'PERCENTAGE',      'INFORMATIONAL', '726')
     ) as v(codigo, mnemonico, tipo, naturaleza, orden)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
where not exists (
    select 1 from payroll_engine.payroll_concept c where c.object_id = o.id
);

-- ---------------------------------------------------------
-- 5. Operandos
-- ---------------------------------------------------------
insert into payroll_engine.payroll_concept_operand (target_object_id, operand_role, source_object_id)
select destino.id, v.rol, origen.id
from (values
        ('B_CP_MAX', 'LEFT',       'B07'),
        ('B_CP_MAX', 'RIGHT',      'P_TOPE_MAX_CP'),
        ('B_CP',     'LEFT',       'B_CP_MAX'),
        ('B_CP',     'RIGHT',      'P_TOPE_MIN_CP'),
        ('704',      'BASE',       'B08'),
        ('704',      'PERCENTAGE', 'P_HE_TRAB'),
        ('726',      'BASE',       'B08'),
        ('726',      'PERCENTAGE', 'P_HE_EMP')
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
-- 6. Las cinco cuotas que cambian de base
-- ---------------------------------------------------------
-- 701 FP trabajador, 703 desempleo trabajador, 721 desempleo empresario,
-- 722 FP empresario y 723 FOGASA empresario pasan de B_CC a B_CP. Son la
-- «recaudacion conjunta», que cotiza sobre la base de contingencias
-- profesionales.
--
-- El 700 y el 720 (contingencias comunes) y el 702 y el 724 (MEI) se quedan en
-- B_CC: el MEI cotiza sobre la base de comunes.
update payroll_engine.payroll_concept_operand op
   set source_object_id = nueva.id,
       updated_at       = current_timestamp
  from payroll_engine.payroll_object destino,
       payroll_engine.payroll_object nueva
 where op.target_object_id = destino.id
   and op.operand_role     = 'BASE'
   and destino.rule_system_code = 'ESP'
   and destino.object_type_code = 'CONCEPT'
   and destino.object_code in ('701', '703', '721', '722', '723')
   and nueva.rule_system_code   = 'ESP'
   and nueva.object_type_code   = 'CONCEPT'
   and nueva.object_code        = 'B_CP';

-- ---------------------------------------------------------
-- 7. Las horas extra salen de la base de contingencias comunes
-- ---------------------------------------------------------
-- Esta es la linea por la que existe el issue. La V133 declaro 102 -> B01
-- —«las horas extra cotizan»— y es cierto, pero no ahi: cotizan en la base de
-- profesionales y en la suya propia, no en la de comunes.
delete from payroll_engine.payroll_concept_feed_relation
 where source_object_id = (
        select id from payroll_engine.payroll_object
         where rule_system_code = 'ESP' and object_type_code = 'CONCEPT' and object_code = '102')
   and target_object_id = (
        select id from payroll_engine.payroll_object
         where rule_system_code = 'ESP' and object_type_code = 'CONCEPT' and object_code = 'B01');

-- ---------------------------------------------------------
-- 8. Alimentaciones
-- ---------------------------------------------------------
-- B04  = 103 + B02      la prorrata que se paga mas la que solo cotiza. Los dos
--                       coeficientes suman uno (ADR-070), asi que esto es la
--                       prorrata del mes, una vez y sea cual sea el regimen.
-- B05  = B_CC           la base de comunes, ya topada, tal y como entra en la
--                       de profesionales.
-- B06  = 102            las horas extra, tal y como entran en la de
--                       profesionales.
-- B07  = B05 + B06      y de ahi salen los dos topes hasta B_CP.
-- B08  = 102            la base de la cotizacion adicional. Sin topes: el
--                       modelo oficial no le pone ninguno.
-- B09  = 970            la base sujeta a retencion, que es el total devengado
--                       (ADR-070 §4). El recuadro la imprime y hasta hoy no.
-- 704 -> 980            la cotizacion adicional del trabajador descuenta.
-- 726 -> 725            la de la empresa suma en su total y no descuenta.
insert into payroll_engine.payroll_concept_feed_relation (
    source_object_id, target_object_id, feed_mode, feed_value, invert_sign, effective_from, effective_to
)
select origen.id, destino.id, 'FEED_BY_SOURCE', cast(null as numeric), false, DATE '2025-01-01', cast(null as date)
from (values
        ('103',  'B04'),
        ('B02',  'B04'),
        ('B_CC', 'B05'),
        ('102',  'B06'),
        ('B05',  'B07'),
        ('B06',  'B07'),
        ('102',  'B08'),
        ('970',  'B09'),
        ('704',  '980'),
        ('726',  '725')
     ) as v(origen, destino)
join payroll_engine.payroll_object origen
  on origen.rule_system_code = 'ESP' and origen.object_type_code = 'CONCEPT' and origen.object_code = v.origen
join payroll_engine.payroll_object destino
  on destino.rule_system_code = 'ESP' and destino.object_type_code = 'CONCEPT' and destino.object_code = v.destino
where not exists (
    select 1 from payroll_engine.payroll_concept_feed_relation x
     where x.source_object_id = origen.id and x.target_object_id = destino.id
);

-- La remuneracion mensual, por exclusion: B03 = B01 - B04.
insert into payroll_engine.payroll_concept_feed_relation (
    source_object_id, target_object_id, feed_mode, feed_value, invert_sign, effective_from, effective_to
)
select origen.id, destino.id, 'FEED_BY_SOURCE', cast(null as numeric), v.invertir, DATE '2025-01-01', cast(null as date)
from (values
        ('B01', 'B03', false),
        ('B04', 'B03', true)
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
-- 9. El orden del recuadro y el del bloque de empresa
-- ---------------------------------------------------------
-- El recuadro de bases pasa de tres lineas sueltas a los cuatro bloques del
-- modelo oficial, y cada bloque se lee de arriba abajo:
--
--   401 B03  Remuneracion mensual          411 B05  Base de contingencias comunes
--   402 B04  Prorrata de pagas extras      412 B06  Horas extraordinarias
--   403 B01  Base de cotizacion            413 B07  Base de cotizacion
--   404 B_CC Base tras topes               414 B_CP Base tras topes
--   421 B08  Base                          431 B09  Base
--
-- B02 deja de imprimirse por su cuenta: la prorrata que solo cotiza ya sale,
-- sumada con la que se paga, en el B04. Con esto el recuadro ensena la misma
-- linea en los dos regimenes, que es lo que hace el modelo oficial; antes, a
-- quien prorrateaba no le salia ninguna (su B02 vale cero y un cero no se
-- imprime).
update payroll_engine.payroll_concept c
   set payslip_order_code = v.orden,
       updated_at         = current_timestamp
  from (values ('B01', '403'), ('B_CC', '404'), ('B02', cast(null as varchar))) as v(codigo, orden)
  join payroll_engine.payroll_object o
    on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
 where c.object_id = o.id;

-- El total de la aportacion empresarial cierra su bloque, asi que su orden de
-- folio deja de ser su codigo: con el 726 —y con el AT/EP del backend#122— hay
-- sumandos por detras del 725.
update payroll_engine.payroll_concept c
   set payslip_order_code = '790',
       updated_at         = current_timestamp
  from payroll_engine.payroll_object o
 where c.object_id = o.id
   and o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = '725';

-- ---------------------------------------------------------
-- 10. Los nombres
-- ---------------------------------------------------------
-- Los del modelo oficial. Dos bloques repiten rotulo —«Base de cotizacion» y
-- «Base tras topes»— y eso no es un descuido: son la misma pregunta hecha sobre
-- dos bases distintas, y el rotulo del bloque es el que dice cual.
--
-- «Base tras topes» y no «Base de cotizacion» a secas porque el nombre tiene
-- que explicar por que dos lineas seguidas pueden ser distintas. Cuando ningun
-- tope muerde valen lo mismo, y verlo es parte de la lectura.
insert into payroll_engine.payroll_concept_label (object_id, language_code, label)
select o.id, 'es', v.label
from (values
        ('B03',  'Remuneracion mensual'),
        ('B04',  'Prorrata de pagas extraordinarias'),
        ('B05',  'Base de contingencias comunes'),
        ('B06',  'Horas extraordinarias'),
        ('B07',  'Base de cotizacion'),
        ('B_CP', 'Base tras topes'),
        ('B08',  'Base'),
        ('B09',  'Base'),
        ('704',  'Horas extraordinarias'),
        ('726',  'Horas extraordinarias'),
        -- Los que no llegan al papel tambien se nombran: EveryEngineConceptHasASpanishNameTest
        -- no admite conceptos sin nombre, y con razon —un nodo sin nombre en el grafo del
        -- disenador se ensena con su codigo y nadie sabe que es (V136).
        ('B_CP_MAX',      'Base de contingencias profesionales tras el tope maximo'),
        ('P_TOPE_MAX_CP', 'Tope maximo de contingencias profesionales'),
        ('P_TOPE_MIN_CP', 'Tope minimo de contingencias profesionales'),
        ('P_HE_TRAB',     'Tipo de cotizacion adicional por horas extraordinarias (trabajador)'),
        ('P_HE_EMP',      'Tipo de cotizacion adicional por horas extraordinarias (empresa)')
     ) as v(codigo, label)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
join payroll_engine.payroll_concept c on c.object_id = o.id
where not exists (
    select 1 from payroll_engine.payroll_concept_label l
     where l.object_id = o.id and l.language_code = 'es'
);

-- Los dos que ya existian y cambian de rotulo dentro del recuadro.
update payroll_engine.payroll_concept_label l
   set label      = v.label,
       updated_at = current_timestamp
  from (values ('B01', 'Base de cotizacion'), ('B_CC', 'Base tras topes')) as v(codigo, label)
  join payroll_engine.payroll_object o
    on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
 where l.object_id = o.id and l.language_code = 'es';

-- ---------------------------------------------------------
-- 11. Elegibilidad
-- ---------------------------------------------------------
-- Las dos cuotas nuevas, y ademas TODAS las bases que solo existen como fuente
-- de un agregado. DefaultEligibleConceptExpansionService no expande las fuentes
-- de un AGGREGATE —a proposito, o cualquier concepto con feed a un agregado
-- elegible burlaria la elegibilidad—, asi que un concepto que solo llega por
-- ahi NO SE EJECUTA y la corrida sale igual que antes, sin avisar (V141).
--
-- Entran por operando y no hacen falta aqui: B_CP y B_CP_MAX (operandos del
-- 701), B07 (operando de B_CP_MAX), B08 (operando del 704), los topes y los
-- tipos.
--
-- Hacen falta aqui: B03 y B04 (solo les alimenta B01), B05 y B06 (solo
-- alimentan a B07) y B09 (solo le alimenta el 970).
insert into payroll_engine.concept_assignment
    (rule_system_code, concept_code, company_code, agreement_code, employee_type_code,
     valid_from, valid_to, priority)
select 'ESP', v.codigo, null, '99002405011982', null, DATE '2025-01-01', null, v.prioridad
from (values ('704', 704), ('726', 726),
             ('B03', 401), ('B04', 402), ('B05', 411), ('B06', 412), ('B09', 431)) as v(codigo, prioridad)
where not exists (
    select 1 from payroll_engine.concept_assignment a
     where a.rule_system_code = 'ESP' and a.concept_code = v.codigo
);
