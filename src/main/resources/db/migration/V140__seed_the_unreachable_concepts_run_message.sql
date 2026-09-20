-- =========================================================
-- V140__seed_the_unreachable_concepts_run_message.sql
-- El aviso de los conceptos que no alcanza ninguna asignacion (backend#110)
-- =========================================================
--
-- La V125 §3 deja escrita la regla, heredada del ADR-059 §7: cada comprobacion
-- nueva se implementa en el codigo Y se da de alta en el catalogo, en el mismo
-- commit. Este es ese commit, y este es ese alta.
--
-- ---------------------------------------------------------
-- De que avisa
-- ---------------------------------------------------------
-- Un concepto puede estar declarado, con sus operandos, y no ejecutarse nunca:
-- pasa cuando solo existe como fuente de un AGGREGATE y no esta asignado a
-- nadie por su cuenta. Las fuentes de un agregado no se expanden, a proposito
-- -un agregado suma lo que se le ha asignado, y quien se calcula lo decide la
-- asignacion y no el catalogo-, asi que a ese concepto no llega nadie.
--
-- Lo caro no es eso: es el silencio. La corrida contesta 202, termina
-- COMPLETED, salen los 873 recibos y ni uno cambia, que es indistinguible de
-- "este cambio no tenia que mover nada" -un caso legitimo y frecuente-. Le
-- costo medio dia a quien lo encontro, sabiendo lo que buscaba y con un
-- catalogo de mentira que el mismo habia montado.
--
-- Es el hermano del backend#96 por el otro extremo: alli se retiro el P_SS, un
-- concepto que no alimentaba a nadie; este es uno al que no llega nadie.
--
-- ---------------------------------------------------------
-- WARNING y no ERROR
-- ---------------------------------------------------------
-- La ejecucion sigue. Un concepto inalcanzable puede ser transitorio -se
-- declara hoy y se asigna manana- y parar la nomina de 873 personas por eso
-- seria cambiar un silencio por un portazo.
insert into rulesystem.rule_entity (
    rule_system_code, rule_entity_type_code, code, name, description, active, start_date, end_date
)
select 'ESP', 'PAYROLL_RUN_MESSAGE', v.code, v.name, v.description, true, DATE '1900-01-01', cast(null as date)
from (
    values
        ('UNREACHABLE_CONCEPTS',
         'Concepts no assignment reaches',
         'The rule system declares concepts that no assignment brings into any plan, so they were not executed. The run went ahead')
) as v(code, name, description)
where not exists (
    select 1
    from rulesystem.rule_entity e
    where e.rule_system_code = 'ESP'
      and e.rule_entity_type_code = 'PAYROLL_RUN_MESSAGE'
      and e.code = v.code
);

-- El castellano donde va el castellano, cruzando con rule_entity_type para que
-- esto no pueda usarse nunca para traducir una cita reglamentaria (ADR-052).
insert into rulesystem.rule_entity_translation (rule_entity_id, language_code, name, description)
select e.id, 'es-ES', v.name, v.description
from rulesystem.rule_entity e
join rulesystem.rule_entity_type t
  on t.code = e.rule_entity_type_code
 and t.literal_class = 'DOMAIN_VOCABULARY'
join (
    values
        ('UNREACHABLE_CONCEPTS',
         'Conceptos a los que no llega ninguna asignación',
         'El sistema de reglas declara conceptos que ninguna asignación mete en ningún plan, así que no se han ejecutado. La ejecución siguió adelante')
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
