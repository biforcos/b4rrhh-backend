-- =========================================================
-- V141__total_the_employer_contribution_in_the_engine.sql
-- El total de la aportacion empresarial lo calcula el motor (backend#114)
-- =========================================================
--
-- El recuadro de aportacion empresarial (720-724) era el unico bloque con total
-- en el modelo oficial que NO tenia total en el motor. Los otros tres lo tienen
-- como concepto —970, 980, 990—; este lo sumaba la plantilla del PDF, y el
-- frontend#79 ya se lo habia quitado al folio. Resultado: el papel ensenaba una
-- cifra que la pantalla no, y esa cifra se calculaba fuera del motor.
--
-- ---------------------------------------------------------
-- Por que esta suma si se declara y la de las bases no
-- ---------------------------------------------------------
-- Porque significa algo: es el coste total de Seguridad Social a cargo de la
-- empresa, y el modelo oficial lo imprime. La suma del recuadro de bases no
-- significa nada —B_CC y B01 son dos magnitudes distintas del mismo mes— y por
-- eso el frontend#79 la retiro en vez de declararla.
--
-- La regla que queda es la misma para las dos salidas: ninguna inventa cifras.
-- El motor calcula; el folio y el papel leen.
--
-- ---------------------------------------------------------
-- 1. El objeto
-- ---------------------------------------------------------
-- 725, detras de los cinco sumandos, que es donde lo pone el orden de folio.
insert into payroll_engine.payroll_object (rule_system_code, object_type_code, object_code)
select 'ESP', 'CONCEPT', '725'
where not exists (
    select 1 from payroll_engine.payroll_object o
     where o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = '725'
);

-- ---------------------------------------------------------
-- 2. El concepto, con naturaleza NUEVA
-- ---------------------------------------------------------
-- TOTAL_EMPLOYER_CONTRIBUTION y no INFORMATIONAL, que es la naturaleza de sus
-- cinco sumandos. Reutilizarla dejaria el total indistinguible de una linea mas
-- del recuadro, y las dos salidas necesitan distinguirlo: es por la naturaleza
-- —no por el codigo— como se sabe si una linea ES un concepto o el total que
-- cierra su bloque.
--
-- Sigue el patron de TOTAL_EARNING y TOTAL_DEDUCTION: un total tiene su propia
-- naturaleza y vive dentro del bloque que cierra.
--
-- AGGREGATE y PERIOD, como el 970: se compone de lo que le alimentan y se
-- evalua una vez por periodo, igual que los cinco conceptos que lo alimentan.
insert into payroll_engine.payroll_concept (
    object_id, concept_mnemonic, calculation_type, functional_nature,
    payslip_order_code, execution_scope
)
select o.id, 'TOTAL_APORTACION_EMPRESARIAL', 'AGGREGATE', 'TOTAL_EMPLOYER_CONTRIBUTION', '725', 'PERIOD'
from payroll_engine.payroll_object o
where o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = '725'
  and not exists (select 1 from payroll_engine.payroll_concept c where c.object_id = o.id);

-- ---------------------------------------------------------
-- 3. Sus cinco sumandos
-- ---------------------------------------------------------
-- Sin invertir el signo: el coste de la empresa se suma, no se resta. Y no
-- alimenta a nadie: no es una deduccion del trabajador ni entra en el liquido.
insert into payroll_engine.payroll_concept_feed_relation (
    source_object_id, target_object_id, feed_mode, feed_value, invert_sign, effective_from, effective_to
)
select origen.id, destino.id, 'FEED_BY_SOURCE', cast(null as numeric), false, DATE '2025-01-01', cast(null as date)
from (values ('720'), ('721'), ('722'), ('723'), ('724')) as v(origen)
join payroll_engine.payroll_object origen
  on origen.rule_system_code = 'ESP' and origen.object_type_code = 'CONCEPT' and origen.object_code = v.origen
join payroll_engine.payroll_object destino
  on destino.rule_system_code = 'ESP' and destino.object_type_code = 'CONCEPT' and destino.object_code = '725'
where not exists (
    select 1 from payroll_engine.payroll_concept_feed_relation x
     where x.source_object_id = origen.id and x.target_object_id = destino.id
);

-- ---------------------------------------------------------
-- 4. Su bloque
-- ---------------------------------------------------------
-- El total de un bloque vive en su bloque (V138): la ultima linea del recuadro
-- de aportacion empresarial, no un recuadro aparte.
insert into payroll_engine.payslip_section_nature (rule_system_code, functional_nature, section_code)
select 'ESP', 'TOTAL_EMPLOYER_CONTRIBUTION', 'APORTACION_EMPRESARIAL'
where not exists (
    select 1 from payroll_engine.payslip_section_nature n
     where n.rule_system_code = 'ESP' and n.functional_nature = 'TOTAL_EMPLOYER_CONTRIBUTION'
);

-- ---------------------------------------------------------
-- 5. Su nombre
-- ---------------------------------------------------------
-- El del modelo oficial, como los otros tres totales (V136).
insert into payroll_engine.payroll_concept_label (object_id, language_code, label)
select o.id, 'es', 'Total aportacion empresarial'
from payroll_engine.payroll_object o
where o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = '725'
  and not exists (
    select 1 from payroll_engine.payroll_concept_label l
     where l.object_id = o.id and l.language_code = 'es'
);

-- ---------------------------------------------------------
-- 6. Elegibilidad
-- ---------------------------------------------------------
-- Explicita, y no por arrastre de sus fuentes: DefaultEligibleConceptExpansionService
-- NO expande las fuentes de un AGGREGATE —y es a proposito, porque si no, cualquier
-- concepto con feed a un agregado elegible burlaria la elegibilidad—. Un agregado que
-- no se asigna no se ejecuta, y no avisa: la corrida sale igual que antes.
--
-- Al convenio entero, como los otros tres totales.
insert into payroll_engine.concept_assignment
    (rule_system_code, concept_code, company_code, agreement_code, employee_type_code,
     valid_from, valid_to, priority)
select 'ESP', '725', null, '99002405011982', null, DATE '2025-01-01', null, 725
where not exists (
    select 1 from payroll_engine.concept_assignment a
     where a.rule_system_code = 'ESP' and a.concept_code = '725'
);
