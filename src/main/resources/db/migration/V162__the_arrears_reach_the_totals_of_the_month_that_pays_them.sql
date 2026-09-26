-- =========================================================
-- V162__the_arrears_reach_the_totals_of_the_month_that_pays_them.sql
-- Los tres conceptos tecnicos de los atrasos (backend#133, paso 6 de
-- workspace#9)
-- =========================================================
--
-- El #131 sabe lo que agosto vale hoy y el #132 dice hasta donde atras se mira.
-- Esta es la pieza que mueve importes: la diferencia entre lo que agosto vale y
-- lo que por agosto se ha pagado baja al recibo del mes abierto como lineas con
-- su periodo de origen.
--
-- Y esas lineas son dinero de ESTE mes, asi que tienen que llegar a los totales.
--
-- ---------------------------------------------------------
-- Por que tres conceptos y no una suma en Java
-- ---------------------------------------------------------
-- La forma facil seria sumar los atrasos al 970 despues de calcular, en Java.
-- Se descarta por la regla de este motor: **lo que interviene en un calculo se
-- ve en el grafo** (V146). Un 970 que valiera una cosa en la tabla y otra en el
-- grafo seria un recibo que no se puede explicar, que es lo unico que este
-- producto tiene.
--
-- Asi que son tres conceptos ENGINE_PROVIDED que alimentan los tres agregados
-- que ya existen, y la excepcion que los permite esta escrita desde el ADR-074
-- §4: un calculador tecnico puede resolver valores que se **buscan o se derivan
-- del contexto de ejecucion**, y nunca calcular un concepto economico. Estos
-- tres no calculan nada: reciben la suma hecha, que la unidad resuelve antes de
-- ejecutar el grafo por el mismo camino que la base reguladora del #128.
--
-- ---------------------------------------------------------
-- Y por que tres y no uno
-- ---------------------------------------------------------
-- Porque van a tres sitios distintos y ninguno es un detalle:
--
--   A_DEV -> 970  los devengos atrasados son mas dinero para el empleado
--   A_DED -> 980  las cuotas atrasadas del trabajador son menos dinero
--   A_EMP -> 725  la aportacion de la empresa no pasa por el liquido
--
-- Un solo concepto con el neto dentro daria el mismo 990 y un recibo que no
-- distingue lo que se cobra de lo que se retiene. Eso es justo lo que el modelo
-- oficial separa.
--
-- Las bases atrasadas NO entran en ningun total de este mes, y eso es la otra
-- mitad de la decision del 05/10: una base se atribuye a SU mes. La linea de
-- base con origen agosto existe en el recibo de septiembre para que se pueda
-- leer y para la liquidacion complementaria, y no suma a las bases de
-- septiembre, que son las de septiembre.
--
-- Y el IRPF no viaja: la retencion es sobre lo que se paga cuando se paga
-- (ADR-070 §4). Los devengos atrasados entran en el 970 de este mes y el 800 de
-- este mes los absorbe, asi que no hay linea 800 con origen.

-- ---------------------------------------------------------
-- 1. Los tres objetos y sus conceptos
-- ---------------------------------------------------------
insert into payroll_engine.payroll_object (rule_system_code, object_type_code, object_code, created_at, updated_at)
select 'ESP', 'CONCEPT', v.codigo, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
from (values ('A_DEV'), ('A_DED'), ('A_EMP')) as v(codigo)
where not exists (
    select 1 from payroll_engine.payroll_object o
    where o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
);

-- PERIOD y no SEGMENT: un atraso es del mes, no de un tramo. No hay ninguna
-- fecha dentro del mes abierto a la que pertenezca.
--
-- Sin `payslip_order_code`: estos tres NO se imprimen. Lo que se imprime son las
-- lineas de atraso, una por concepto y con su origen, que las pone el recibo. Un
-- concepto tecnico que apareciera en el folio seria el total de los atrasos
-- impreso al lado de sus lineas, o sea el mismo dinero dos veces a la vista.
insert into payroll_engine.payroll_concept (
    object_id, concept_mnemonic, calculation_type, functional_nature,
    payslip_order_code, execution_scope, rounding_scale, rounding_mode
)
select o.id, v.mnemonico, 'ENGINE_PROVIDED', v.naturaleza,
       cast(null as varchar), 'PERIOD', 2, 'HALF_UP'
from (values
        ('A_DEV', 'ATRASOS_DEVENGOS',     'TECHNICAL'),
        ('A_DED', 'ATRASOS_DEDUCCIONES',  'TECHNICAL'),
        ('A_EMP', 'ATRASOS_APORTACION_EMPRESA', 'TECHNICAL')
     ) as v(codigo, mnemonico, naturaleza)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
where not exists (
    select 1 from payroll_engine.payroll_concept c where c.object_id = o.id
);

comment on table payroll_engine.payroll_concept is
    'Los conceptos del motor. Los tres A_* son los atrasos y son tecnicos: no se imprimen, alimentan a los totales para que las lineas de atraso sean dinero de este mes (backend#133).';

-- ---------------------------------------------------------
-- 2. Y alimentan a los tres agregados que ya existian
-- ---------------------------------------------------------
-- `invert_sign` en ninguno: A_DED trae las deducciones en positivo y el 980 es
-- un total de deducciones, que ya entra negado en el 990. Negarlo aqui lo
-- sumaria al liquido.
insert into payroll_engine.payroll_concept_feed_relation (
    source_object_id, target_object_id, feed_mode, feed_value, invert_sign, effective_from, effective_to
)
select origen.id, destino.id, 'FEED_BY_SOURCE', cast(null as numeric), false,
       DATE '2025-01-01', cast(null as date)
from (values
        ('A_DEV', '970'),
        ('A_DED', '980'),
        ('A_EMP', '725')
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
-- 3. Sus nombres
-- ---------------------------------------------------------
-- Todo concepto de ESP tiene nombre en castellano y hay un candado que lo cruza
-- (EveryEngineConceptHasASpanishNameTest), y vale igual para los tecnicos: uno
-- sin nombre sale en el grafo con su mnemonico, que es la clave interna donde va
-- un nombre (V136).
insert into payroll_engine.payroll_concept_label (object_id, language_code, label)
select o.id, 'es', v.nombre
from (values
        ('A_DEV', 'Atrasos: total de devengos'),
        ('A_DED', 'Atrasos: total de deducciones del trabajador'),
        ('A_EMP', 'Atrasos: total de aportacion de la empresa')
     ) as v(codigo, nombre)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
where not exists (
    select 1 from payroll_engine.payroll_concept_label l
     where l.object_id = o.id and l.language_code = 'es'
);

-- ---------------------------------------------------------
-- 4. Asignados a todo el convenio, y valen cero donde no hay atraso
-- ---------------------------------------------------------
-- Como el 102 del V133 y por lo mismo: `concept_assignment` acota por sociedad,
-- convenio y tipo de empleado y **no por empleado**, asi que el paso existe para
-- todos. Quien no tiene atrasos saca cero, y un cero no se imprime
-- (backend#104). Lo que se gana es que «quien lleva atrasos» se contesta mirando
-- el numero.
insert into payroll_engine.concept_assignment
    (rule_system_code, concept_code, company_code, agreement_code, employee_type_code,
     valid_from, valid_to, priority)
select 'ESP', v.codigo, null, '99002405011982', null, DATE '2025-01-01', null, v.prioridad
from (values ('A_DEV', 933), ('A_DED', 934), ('A_EMP', 935)) as v(codigo, prioridad)
where not exists (
    select 1 from payroll_engine.concept_assignment a
     where a.rule_system_code = 'ESP' and a.concept_code = v.codigo
);

-- ---------------------------------------------------------
-- 5. El indice que hace barata la pregunta del atraso
-- ---------------------------------------------------------
-- «Cuanto se ha pagado por agosto» se contesta sumando las lineas con origen
-- agosto, esten en el recibo de agosto o en el de cualquier mes posterior. Una
-- sola condicion, porque en este arbol TODA linea lleva su periodo en esa
-- columna: lo que el #133 anade no es llenarla, es que pueda ser distinta de la
-- del recibo. Lo que si es nuevo es preguntar POR ella, que hasta ahora no hacia
-- nadie.
create index if not exists ix_payroll_concept_origin_period
    on payroll.payroll_concept (origin_period_code, concept_code)
    where origin_period_code is not null;

comment on column payroll.payroll_concept.origin_period_code is
    'El periodo al que pertenece la linea: el del recibo en las propias, y el del mes de origen en las de atraso. La columna existe desde la V53 y TODA linea la lleva; lo que el backend#133 anade es que pueda ser distinta de la del recibo.';

-- ---------------------------------------------------------
-- 6. Una linea puede no venir de ningun paso
-- ---------------------------------------------------------
-- La V132 puso `merged_step_count >= 1` con una razon buena: toda linea del
-- folio venia de al menos un paso del motor, y un cero habria sido un hueco.
--
-- Una linea de atraso rompe esa premisa, y no por un descuido del modelo: **no
-- viene de ningun paso de este calculo**. Viene de comparar lo que otro mes vale
-- con lo que por ese mes se ha pagado. Sus pasos, si los hubiera, serian los del
-- vigente de aquel mes, y esos no se guardan a proposito (ADR-076 §1).
--
-- Asi que cero vale, y el cero es la respuesta y no la ausencia de una: es lo que
-- le dice a la pestana «Calculo» que no busque pasos para esta linea. Lo que esa
-- linea tiene que contar son los tres numeros del backend#134 -lo que vale hoy,
-- lo que se ha pagado, la diferencia- y no una travesia del grafo que no existe.
--
-- Negativo sigue sin valer: eso si seria un error.
alter table payroll.payroll_concept
    drop constraint chk_payroll_concept_merged_step_count;

alter table payroll.payroll_concept
    add constraint chk_payroll_concept_merged_step_count
        check (merged_step_count >= 0);

comment on constraint chk_payroll_concept_merged_step_count on payroll.payroll_concept is
    'Cero vale desde el backend#133: una linea de atraso no viene de ningun paso de este calculo, y el cero es lo que le dice a la pantalla que no los busque. Negativo no vale.';

-- ---------------------------------------------------------
-- 7. El mensaje de la marca que se queda sin recibo donde cobrarse
-- ---------------------------------------------------------
-- Sale de un caso que el diseno del 05/10 no nombraba y que aparecio al montar
-- la invariante: una marca de una presencia que **no tiene recibo en el mes
-- abierto** (el empleado ceso y no ha vuelto, o volvio con otra presencia). No
-- hay documento donde poner su linea de atraso, asi que no se paga.
--
-- Y no se calla. La marca sigue activa y la corrida lo dice, porque lo que hay
-- que mirar no es un recibo: es que hay dinero pendiente de alguien a quien este
-- lanzamiento no alcanza.
insert into rulesystem.rule_entity (
    rule_system_code, rule_entity_type_code, code, name, description, active, start_date, end_date
)
select 'ESP', 'PAYROLL_RUN_MESSAGE', v.code, v.name, v.description, true, DATE '1900-01-01', cast(null as date)
from (
    values
        ('RETRO_MARK_WITHOUT_A_RECEIPT_TO_PAY_IT',
         'A retroactivity mark has no receipt to be paid in',
         'The mark belongs to a presence with no receipt in the open period, so there is no document where its arrears line could go. It was not paid and the mark is still active')
) as v(code, name, description)
where not exists (
    select 1
    from rulesystem.rule_entity e
    where e.rule_system_code = 'ESP'
      and e.rule_entity_type_code = 'PAYROLL_RUN_MESSAGE'
      and e.code = v.code
);

insert into rulesystem.rule_entity_translation (rule_entity_id, language_code, name, description)
select e.id, 'es-ES', v.name, v.description
from rulesystem.rule_entity e
join rulesystem.rule_entity_type t
  on t.code = e.rule_entity_type_code
 and t.literal_class = 'DOMAIN_VOCABULARY'
join (
    values
        ('RETRO_MARK_WITHOUT_A_RECEIPT_TO_PAY_IT',
         'Una marca de retroactividad sin recibo donde cobrarse',
         'La marca es de una presencia que no tiene recibo en el período abierto, así que no hay documento donde poner su línea de atraso. No se ha pagado y la marca sigue activa')
) as v(code, name, description)
  on v.code = e.code
where e.rule_system_code = 'ESP'
  and e.rule_entity_type_code = 'PAYROLL_RUN_MESSAGE'
  and not exists (
      select 1
      from rulesystem.rule_entity_translation tr
      where tr.rule_entity_id = e.id
        and tr.language_code = 'es-ES'
  );
