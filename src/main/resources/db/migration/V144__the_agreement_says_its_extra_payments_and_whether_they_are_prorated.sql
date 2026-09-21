-- =========================================================
-- V144__the_agreement_says_its_extra_payments_and_whether_they_are_prorated.sql
-- Lo que el convenio sabe de las pagas extras (backend#117, paso 4 de workspace#9)
-- =========================================================
--
-- Esta migracion no calcula nada. Pone en el sistema de reglas dos cosas que el
-- convenio sabe y que hoy no estan en ningun sitio:
--
--   1. Si por defecto las pagas se prorratean.
--   2. Cuantas pagas extras hay y de que se componen.
--
-- Ninguna de las dos mueve un recibo: la primera es el valor que la contratacion
-- copiara al empleado (backend#118), y las segundas no alimentan a nadie hasta
-- el backend#119. Un concepto que no alimenta a ningun otro no puede cambiar un
-- importe, y eso es lo que hace comprobable el «cero recibos se mueven» de este
-- issue en vez de dejarlo en una esperanza.
--
-- ---------------------------------------------------------
-- 1. El testigo de prorrateo por defecto
-- ---------------------------------------------------------
-- Va en el perfil del convenio porque es el convenio quien lo dice. NO es lo que
-- decide el recibo: eso lo decide la vertical del empleado (backend#118), que se
-- rellena COPIANDO este testigo al contratar. Es una copia y no un enlace — si
-- el convenio cambia el ano que viene, los que ya estan no cambian solos.
--
-- No admite nulos, y eso es una decision y no una comodidad: un convenio sin
-- testigo obligaria a la contratacion a decidir que hacer cuando falta, y esa
-- seria una segunda regla, escondida en el codigo, sobre algo que se declara
-- aqui.
alter table rulesystem.agreement_profile
    add column if not exists extra_payments_prorated boolean not null default false;

comment on column rulesystem.agreement_profile.extra_payments_prorated is
    'Si el convenio prorratea las pagas extras por defecto. Es el valor que la contratacion copia a la vertical del empleado, no lo que decide el recibo (backend#117).';

-- El de la demo: grandes almacenes, BOE-A-2023-13740, art. 23. El prorrateo esta
-- permitido «por acuerdo», o sea que el valor por omision del convenio es que NO
-- se prorratea: las extras se pagan en su mes. Quien las quiera prorrateadas lo
-- pide, y eso es una fila mas en su vertical.
update rulesystem.agreement_profile p
set extra_payments_prorated = false,
    updated_at              = now()
from rulesystem.rule_entity e
where e.id                   = p.agreement_rule_entity_id
  and e.rule_system_code     = 'ESP'
  and e.rule_entity_type_code = 'AGREEMENT'
  and e.code                 = '99002405011982';

-- ---------------------------------------------------------
-- 2. Las cuatro pagas del convenio, como conceptos del motor
-- ---------------------------------------------------------
-- El art. 23 distribuye las retribuciones en DIECISEIS pagas, cuatro de ellas
-- extraordinarias, compuestas de salario base de grupo y complementos personales,
-- de puesto y de calidad.
--
-- Que sean agregados del motor y no filas de una tabla nueva es deliberado: el
-- designer ya sabe ensenarlos y editarlos (ADR-067) —un agregado con sus
-- alimentadores es exactamente lo que el grafo pinta—, y una «paga de beneficios
-- II» es un agregado mas y no una columna mas. El numero de pagas no es un
-- parametro del motor: es cuantas define el convenio.
insert into payroll_engine.payroll_object (rule_system_code, object_type_code, object_code)
select 'ESP', 'CONCEPT', v.codigo
from (values ('PE_1'), ('PE_2'), ('PE_3'), ('PE_4')) as v(codigo)
where not exists (
    select 1 from payroll_engine.payroll_object o
     where o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
);

-- AGGREGATE porque cada paga es la suma de los conceptos que la componen.
--
-- Naturaleza BASE: son bases intermedias, como P01. No son un devengo —lo que se
-- devenga es la prorrata del backend#119, no la paga— ni una deduccion.
--
-- Sin payslip_order_code: no se imprimen. La linea que el recibo ensena es la
-- prorrata, y esa es del backend#119.
--
-- Ambito SEGMENT, como el 101 del que se componen. Al sumar lineas que ya vienen
-- prorrateadas por dias, cada paga hereda tramos, presencia y ausencias sin
-- disenar nada; un mes partido deja dos valores de paga que se componen por suma,
-- igual que los dos del salario base.
insert into payroll_engine.payroll_concept (
    object_id, concept_mnemonic, calculation_type, functional_nature,
    payslip_order_code, execution_scope, rounding_scale, rounding_mode
)
select o.id, v.mnemonico, 'AGGREGATE', 'BASE', cast(null as varchar), 'SEGMENT', 2, 'HALF_UP'
from (values
        ('PE_1', 'PAGA_EXTRA_1'),
        ('PE_2', 'PAGA_EXTRA_2'),
        ('PE_3', 'PAGA_EXTRA_3'),
        ('PE_4', 'PAGA_EXTRA_4')
     ) as v(codigo, mnemonico)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
where not exists (
    select 1 from payroll_engine.payroll_concept c where c.object_id = o.id
);

-- ---------------------------------------------------------
-- 3. De que se compone cada paga
-- ---------------------------------------------------------
-- Con lo que el modelo tiene hoy, de un SALARIO_BASE mensual: el 101 es el unico
-- de los conceptos que el art. 23 enumera que existe. El dia que haya complemento
-- de puesto o de calidad se le anade una alimentacion mas a cada paga y no se
-- toca ni una linea de Java — que es justo lo que se compra declarandolas asi.
insert into payroll_engine.payroll_concept_feed_relation (
    source_object_id, target_object_id, feed_mode, feed_value, invert_sign, effective_from, effective_to
)
select origen.id, destino.id, 'FEED_BY_SOURCE', cast(null as numeric), false, DATE '2025-01-01', cast(null as date)
from (values ('PE_1'), ('PE_2'), ('PE_3'), ('PE_4')) as v(paga)
join payroll_engine.payroll_object origen
  on origen.rule_system_code = 'ESP' and origen.object_type_code = 'CONCEPT' and origen.object_code = '101'
join payroll_engine.payroll_object destino
  on destino.rule_system_code = 'ESP' and destino.object_type_code = 'CONCEPT' and destino.object_code = v.paga
where not exists (
    select 1 from payroll_engine.payroll_concept_feed_relation x
     where x.source_object_id = origen.id and x.target_object_id = destino.id
);

-- ---------------------------------------------------------
-- 4. Los nombres
-- ---------------------------------------------------------
-- El convenio no les pone nombre: el art. 23 dice cuantas son y de que se
-- componen, y deja las fechas de pago «segun costumbre de cada empresa». Asi que
-- se numeran, que es lo que no inventa nada. El dia que una empresa las llame
-- «de verano» y «de Navidad», se cambia el literal y no el concepto — que es para
-- lo que existe la tabla de literales (backend#109, V136).
insert into payroll_engine.payroll_concept_label (object_id, language_code, label)
select o.id, 'es', v.label
from (values
        ('PE_1', 'Paga extraordinaria 1'),
        ('PE_2', 'Paga extraordinaria 2'),
        ('PE_3', 'Paga extraordinaria 3'),
        ('PE_4', 'Paga extraordinaria 4')
     ) as v(codigo, label)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
join payroll_engine.payroll_concept c on c.object_id = o.id
where not exists (
    select 1 from payroll_engine.payroll_concept_label l
     where l.object_id = o.id and l.language_code = 'es'
);

-- ---------------------------------------------------------
-- 5. Elegibilidad
-- ---------------------------------------------------------
-- Las cuatro, al convenio entero, como el 102: son SUS pagas extras, asi que el
-- convenio es exactamente la dimension por la que se asignan.
--
-- Y se asignan AHORA y no en el backend#119, aunque todavia no alimenten nada,
-- porque las fuentes de un agregado no se expanden (backend#110): un concepto
-- declarado y sin asignacion propia no se ejecuta nunca, y el aviso
-- UNREACHABLE_CONCEPTS saldria en todas las corridas de la demo hasta el
-- backend#119. Un aviso que sale siempre no avisa de nada, que es justo lo que
-- aquel issue puso en pie.
--
-- Ejecutarlas no mueve ningun recibo: no tienen orden de folio, asi que no son
-- linea, y no alimentan a nadie, asi que no cambian un importe. Lo unico que
-- crece es el rastro de calculo, que es donde se ven — cuatro pasos por tramo.
insert into payroll_engine.concept_assignment
    (rule_system_code, concept_code, company_code, agreement_code, employee_type_code,
     valid_from, valid_to, priority)
select 'ESP', v.codigo, null, '99002405011982', null, DATE '2025-01-01', null, 117
from (values ('PE_1'), ('PE_2'), ('PE_3'), ('PE_4')) as v(codigo)
where not exists (
    select 1 from payroll_engine.concept_assignment a
     where a.rule_system_code = 'ESP' and a.concept_code = v.codigo
);

-- ---------------------------------------------------------
-- 6. Lo que esta migracion NO hace
-- ---------------------------------------------------------
-- No conecta las pagas a B01 ni declara la prorrata. Eso es el backend#119, y es
-- el que mueve los 863 recibos a proposito.
--
-- FRA y PRT no reciben nada: no tienen convenio, y una paga extra que ningun
-- convenio define no es una paga, es una fila.
