-- =========================================================
-- V156__the_regulatory_base_reads_the_previous_month_or_the_theoretical_one.sql
-- La base reguladora diaria por contingencias comunes (backend#128, paso 5 de
-- workspace#9, ADR-074)
-- =========================================================
--
-- Es la primera vez que un calculo de este motor mira FUERA de su periodo, y la
-- regla que se pone aqui la heredan los atrasos del paso 6.
--
-- La norma, verificada articulo por articulo contra el BOE:
--
--   Art. 13.1 del Decreto 1646/1972, de 23 de junio (BOE-A-1972-944):
--     «La base reguladora para el calculo de la cuantia del subsidio de
--     incapacidad laboral transitoria sera el resultado de dividir el importe de
--     la base de cotizacion del trabajador [...] EN EL MES ANTERIOR al de la
--     fecha de iniciacion de la situacion de incapacidad [...] por el numero de
--     dias a que dicha cotizacion se refiera.»
--
--   Art. 13.2: «cuando el trabajador perciba retribucion mensual y haya
--     permanecido en alta en la Empresa todo el mes natural [...] se dividira por
--     TREINTA.» Por eso el divisor no son los dias del calendario.
--
--   Art. 13.3: «Para el trabajador que haya INGRESADO EN LA EMPRESA EN EL MISMO
--     MES en el que se inicie la situacion [...] se aplicara lo dispuesto en los
--     numeros anteriores, referido al indicado mes.» Es el caso que se resuelve
--     sin aviso: la ley lo dice.
--
--   Art. 13.4: las pagas extraordinarias entran por el promedio anual de sus
--     bases de cotizacion. En este modelo eso ya lo hace la prorrata (ADR-070), y
--     es lo que justifica que la base diaria teorica sea el precio del dia MAS su
--     parte de prorrata y no el precio del dia a secas.
--
-- ---------------------------------------------------------
-- 1. Lo que este issue decide de verdad: que se puede leer
-- ---------------------------------------------------------
-- El numero se resuelve UNA VEZ por unidad, antes de calcular nada, y por un
-- solo puerto que filtra por DEFINITIVE. El motor lo recibe hecho. Las tres
-- respuestas posibles estan en el ADR-074; en el catalogo se ven asi:
--
--   BR_ANT     la base diaria que VIENE del mes anterior cerrado, o cero.
--   J_SIN_ANT  uno cuando NO hay mes anterior cerrado y hace falta base.
--   BR_TEO     la base diaria TEORICA de este mes, que la calcula el grafo.
--   BR_ACT     J_SIN_ANT x BR_TEO: la teorica, cuando toca.
--   BR_CC      BR_ANT + BR_ACT.
--
-- Los dos sumandos nunca valen algo a la vez: donde uno aporta, el otro es cero.
-- Es la forma de las dos puertas de la prorrata (ADR-070) aplicada a otra cosa, y
-- lo que compra es que el motor no tenga que saber que existe un «caso sin recibo
-- anterior»: sabe sumar.
--
-- El tercer caso -hay recibo del mes anterior y todavia puede cambiar- NO esta en
-- el catalogo, y no puede estar: ese recibo no se calcula. Un numero que puede
-- cambiar no se lee.
--
-- ---------------------------------------------------------
-- 2. Por que la base teorica se calcula en el GRAFO
-- ---------------------------------------------------------
-- Porque no hay de donde leerla: en la semilla no existe ningun recibo anterior,
-- y en una baja que ocupa el mes entero no queda ni un dia trabajado del que
-- deducirla. Las dos salidas faciles se descartaron a mano:
--
--   * «B01 del mes entre los dias devengados» -> divide por cero justo en el caso
--     que mas importa, el de la baja que viene de agosto y ocupa septiembre.
--   * «precio del dia x (1 + pagas/12) en Java» -> mete en codigo una regla que
--     el convenio declara. En este motor lo que interviene en un calculo SE VE EN
--     EL GRAFO (V146).
--
-- Asi que la base diaria teorica es una cadena de conceptos, espejo de la del
-- salario pero sobre UN dia:
--
--   PE_n_DIA        la parte de cada paga extra que corresponde a un dia = P01.
--   PE_TOTAL_DIA    su suma.
--   P_PRORRATA_DIA  PE_TOTAL_DIA / 12, la prorrata diaria.
--   BR_TEO          P01 + P_PRORRATA_DIA.
--
-- El coste esta dicho: hay CUATRO conceptos espejo porque el convenio de la demo
-- tiene cuatro pagas (V144), y una paga nueva son dos filas y no una. Lo que se
-- compra es que el numero de pagas siga siendo «cuantas define el convenio» y no
-- un parametro escondido. El dia que eso moleste, la respuesta es un ambito nuevo
-- en el metamodelo -«por dia»-, y no una constante en Java.
--
-- ---------------------------------------------------------
-- 3. Ambito SEGMENT, y no PERIOD
-- ---------------------------------------------------------
-- El issue decia PERIOD. Es SEGMENT, y por una razon del metamodelo y no de
-- gusto: BR_TEO sale de P01, que es SEGMENT desde la V135 porque el precio del
-- dia se busca por categoria. Un concepto PERIOD no puede leer uno SEGMENT
-- (ADR-058), asi que una base reguladora PERIOD no podria calcular su teorica.
--
-- Y no hay nada que se pierda: quien lee la base reguladora -la prestacion y la
-- base durante la baja del backend#129- es de tramo tambien. El valor compuesto
-- del periodo es una suma que nadie lee, exactamente como le pasa a P01.
--
-- ---------------------------------------------------------
-- 4. Cero en el tramo trabajado, y eso es a proposito
-- ---------------------------------------------------------
-- BR_CC vale cero en un tramo sin baja por enfermedad comun. No se pierde nada
-- -lo que la lee vale cero ahi tambien- y se gana una consulta: «quien tiene base
-- reguladora» se contesta mirando el numero, sin cruzar nada con las ausencias. En
-- la semilla, los recibos con BR_CC distinto de cero son exactamente los que
-- tienen baja.
--
-- Esta migracion NO mueve ningun recibo: BR_CC no alimenta a nadie todavia. Quien
-- lo lee es el backend#129, y los dos van en la misma resiembra.

-- ---------------------------------------------------------
-- 5. Los objetos
-- ---------------------------------------------------------
insert into payroll_engine.payroll_object (rule_system_code, object_type_code, object_code)
select 'ESP', 'CONCEPT', v.codigo
from (values ('BR_ANT'), ('J_SIN_ANT'),
             ('PE_1_DIA'), ('PE_2_DIA'), ('PE_3_DIA'), ('PE_4_DIA'),
             ('PE_TOTAL_DIA'), ('P_PRORRATA_DIA'),
             ('BR_TEO'), ('BR_ACT'), ('BR_CC')) as v(codigo)
where not exists (
    select 1 from payroll_engine.payroll_object o
     where o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
);

-- ---------------------------------------------------------
-- 6. Los conceptos
-- ---------------------------------------------------------
-- Ninguno se imprime: no hay orden de folio en ninguno. La base reguladora no es
-- una linea del recibo, es de donde sale la prestacion.
--
-- Decimales: la cadena espejo va a SEIS, los mismos que P01, para no redondear
-- dos veces por el camino; BR_TEO, BR_ACT, BR_ANT y BR_CC a DOS, que es como se
-- escribe una base reguladora diaria y donde se aplica de verdad (ADR-066).
insert into payroll_engine.payroll_concept (
    object_id, concept_mnemonic, calculation_type, functional_nature,
    payslip_order_code, execution_scope, rounding_scale, rounding_mode
)
select o.id, v.mnemonico, v.tipo, v.naturaleza, cast(null as varchar), 'SEGMENT', v.decimales, 'HALF_UP'
from (values
        ('BR_ANT',         'BASE_REGULADORA_MES_ANTERIOR', 'ENGINE_PROVIDED',  'TECHNICAL', 2),
        ('J_SIN_ANT',      'SIN_RECIBO_ANTERIOR',          'ENGINE_PROVIDED',  'TECHNICAL', 0),
        ('PE_1_DIA',       'PAGA_EXTRA_1_DIARIA',          'AGGREGATE',        'BASE',      6),
        ('PE_2_DIA',       'PAGA_EXTRA_2_DIARIA',          'AGGREGATE',        'BASE',      6),
        ('PE_3_DIA',       'PAGA_EXTRA_3_DIARIA',          'AGGREGATE',        'BASE',      6),
        ('PE_4_DIA',       'PAGA_EXTRA_4_DIARIA',          'AGGREGATE',        'BASE',      6),
        ('PE_TOTAL_DIA',   'TOTAL_PAGAS_EXTRAS_DIARIO',    'AGGREGATE',        'BASE',      6),
        ('P_PRORRATA_DIA', 'PRORRATA_DIARIA',              'QUOTIENT',         'BASE',      6),
        ('BR_TEO',         'BASE_REGULADORA_TEORICA',      'AGGREGATE',        'BASE',      2),
        ('BR_ACT',         'BASE_REGULADORA_DEL_MES',      'RATE_BY_QUANTITY', 'BASE',      2),
        ('BR_CC',          'BASE_REGULADORA_DIARIA_CC',    'AGGREGATE',        'TECHNICAL', 2)
     ) as v(codigo, mnemonico, tipo, naturaleza, decimales)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
where not exists (
    select 1 from payroll_engine.payroll_concept c where c.object_id = o.id
);

-- ---------------------------------------------------------
-- 7. Operandos
-- ---------------------------------------------------------
-- Dividir entre doce es exacto y por eso es un QUOTIENT y no un PERCENTAGE al
-- 8,33 %: nada se redondea dos veces (backend#61). El divisor es el mismo
-- P_MESES_ANO que ya usa la prorrata mensual, porque son doce meses los mismos
-- doce meses.
insert into payroll_engine.payroll_concept_operand (target_object_id, operand_role, source_object_id)
select destino.id, v.rol, origen.id
from (values
        ('P_PRORRATA_DIA', 'BASE',     'PE_TOTAL_DIA'),
        ('P_PRORRATA_DIA', 'DIVISOR',  'P_MESES_ANO'),
        ('BR_ACT',         'QUANTITY', 'J_SIN_ANT'),
        ('BR_ACT',         'RATE',     'BR_TEO')
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
-- 8. Alimentaciones
-- ---------------------------------------------------------
-- El espejo de las cuatro pagas sobre UN dia: cada paga vale, por dia, lo que
-- vale el precio del dia, igual que cada paga entera vale lo que vale el 101
-- (V144). El dia que una paga se componga de otro concepto mas, se le anade a las
-- dos: a la paga y a su espejo.
insert into payroll_engine.payroll_concept_feed_relation (
    source_object_id, target_object_id, feed_mode, feed_value, invert_sign, effective_from, effective_to
)
select origen.id, destino.id, 'FEED_BY_SOURCE', cast(null as numeric), false, DATE '2025-01-01', cast(null as date)
from (values
        ('P01',            'PE_1_DIA'),
        ('P01',            'PE_2_DIA'),
        ('P01',            'PE_3_DIA'),
        ('P01',            'PE_4_DIA'),
        ('PE_1_DIA',       'PE_TOTAL_DIA'),
        ('PE_2_DIA',       'PE_TOTAL_DIA'),
        ('PE_3_DIA',       'PE_TOTAL_DIA'),
        ('PE_4_DIA',       'PE_TOTAL_DIA'),
        ('P01',            'BR_TEO'),
        ('P_PRORRATA_DIA', 'BR_TEO'),
        ('BR_ANT',         'BR_CC'),
        ('BR_ACT',         'BR_CC')
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
-- 9. Los nombres
-- ---------------------------------------------------------
insert into payroll_engine.payroll_concept_label (object_id, language_code, label)
select o.id, 'es', v.label
from (values
        ('BR_ANT',         'Base reguladora diaria del mes anterior'),
        ('J_SIN_ANT',      'Sin recibo definitivo del mes anterior'),
        ('PE_1_DIA',       'Paga extraordinaria 1 por dia'),
        ('PE_2_DIA',       'Paga extraordinaria 2 por dia'),
        ('PE_3_DIA',       'Paga extraordinaria 3 por dia'),
        ('PE_4_DIA',       'Paga extraordinaria 4 por dia'),
        ('PE_TOTAL_DIA',   'Total de pagas extraordinarias por dia'),
        ('P_PRORRATA_DIA', 'Prorrata diaria de pagas extraordinarias'),
        ('BR_TEO',         'Base reguladora diaria teorica de este mes'),
        ('BR_ACT',         'Base reguladora diaria tomada de este mes'),
        ('BR_CC',          'Base reguladora diaria por contingencias comunes')
     ) as v(codigo, label)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
join payroll_engine.payroll_concept c on c.object_id = o.id
where not exists (
    select 1 from payroll_engine.payroll_concept_label l
     where l.object_id = o.id and l.language_code = 'es'
);

-- ---------------------------------------------------------
-- 10. Elegibilidad
-- ---------------------------------------------------------
-- Se asignan los que son FUENTE DE UN AGREGADO y el propio BR_CC. Los demas
-- entran solos, como operandos, que si se expanden.
--
-- Esa distincion no es un detalle de implementacion: las fuentes de un agregado
-- NO se expanden (backend#110, workspace#10), asi que un concepto declarado y sin
-- asignacion propia no se ejecuta nunca. La primera forma honda que se midio en el
-- workspace#10 se construyo encadenando AGGREGATE y NO SE EJECUTO -cero conceptos,
-- cero avisos, recibo identico-. Aqui la cadena se encadena por operandos donde
-- puede, y donde es un agregado, se asigna.
--
-- Y BR_CC se asigna porque hoy no lo lee nadie: quien lo lee es el backend#129.
-- Sin asignacion propia no se ejecutaria y esta migracion no se podria comprobar.
insert into payroll_engine.concept_assignment
    (rule_system_code, concept_code, company_code, agreement_code, employee_type_code,
     valid_from, valid_to, priority)
select 'ESP', v.codigo, null, '99002405011982', null, DATE '2025-01-01', null, 128
from (values ('PE_1_DIA'), ('PE_2_DIA'), ('PE_3_DIA'), ('PE_4_DIA'),
             ('P_PRORRATA_DIA'), ('BR_ANT'), ('BR_ACT'), ('BR_CC')) as v(codigo)
where not exists (
    select 1 from payroll_engine.concept_assignment a
     where a.rule_system_code = 'ESP' and a.concept_code = v.codigo
);
