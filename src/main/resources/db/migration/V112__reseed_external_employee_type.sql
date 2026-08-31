-- backend#37: la V109 retiró EXTERNAL sobre una premisa que después se retiró a ella.
-- La distinción INTERNAL/EXTERNAL se hizo a propósito y quedó a medio montar cuando la
-- nómina se llevó la atención: no era deuda, era una pieza viva sin terminar. Los tipos
-- de empleado de verdad (asalariado / externo / becario) están en el radar como
-- backend#38.
--
-- La lección que deja esto por escrito: la ausencia de uso no es evidencia de abandono.
-- «Nadie le ha dado significado todavía» y «no merece existir» se ven idénticos en el
-- repositorio; distinguirlos solo puede hacerlo quien puso la pieza.
--
-- La V109 se queda en la historia a propósito: el rastro de que esto pasó vale más que
-- un árbol limpio, y borrar una migración ya aplicada dejaría a Flyway quejándose.

insert into rulesystem.rule_entity (
    rule_system_code,
    rule_entity_type_code,
    code,
    name,
    description,
    active,
    start_date,
    end_date
)
select
    'ESP',
    'EMPLOYEE_TYPE',
    'EXTERNAL',
    'External Employee',
    'Secondary baseline employee type kept available for reference data coherence',
    true,
    DATE '1900-01-01',
    cast(null as date)
where not exists (
    select 1
    from rulesystem.rule_entity
    where rule_system_code = 'ESP'
      and rule_entity_type_code = 'EMPLOYEE_TYPE'
      and code = 'EXTERNAL'
);
