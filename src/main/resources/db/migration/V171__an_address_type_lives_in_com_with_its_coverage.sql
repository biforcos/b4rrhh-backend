-- backend#158, paso 3 del camino 5 (workspace#20, ADR-077): EMPLOYEE_ADDRESS_TYPE sube a COM.
--
-- Domicilio, fiscal, postal y temporal son lo mismo en cualquier pais. La V11 los sembraba una vez
-- por reglamentacion; aqui quedan una vez, en la capa comun.
--
-- Este es el tipo que tiene algo colgando de su id: employee_address_type_profile, la cobertura
-- de cada tipo (obligatorio u opcional, ADR-053 §1). Una fila por copia, doce, con
-- unique (address_type_rule_entity_id) y la FK «on delete cascade». La cobertura cuelga de su raiz
-- —es del tipo de direccion, no de la reglamentacion—, asi que sube con el, y por eso:
--   1. se comprueba que la cobertura es la misma en todas las copias de cada tipo. Si no lo es, la
--      migracion falla y dice en que: que pais exige que direccion no lo decide una migracion;
--   2. se queda una fila por tipo, la de la copia que la funcion de la V167 toma como representante
--      (la de menor id), y se borran las demas a mano, no por la cascada;
--   3. rulesystem.raise_rule_entity_type re-apunta esa fila al id nuevo antes de borrar las copias.
-- Decidido con Juan el 01/10, en el inventario de backend#158.

do $$
declare
    differing text;
begin
    select string_agg(format('%s: %s', d.code, d.coverages), '; ' order by d.code)
      into differing
      from (select re.code,
                   string_agg(format('%s=%s', re.layer_code, coalesce(p.coverage, '(sin cobertura)')), ', '
                              order by re.layer_code) as coverages,
                   count(distinct coalesce(p.coverage, '(sin cobertura)')) as distinct_coverages
              from rulesystem.rule_entity re
              left join rulesystem.employee_address_type_profile p on p.address_type_rule_entity_id = re.id
             where re.rule_entity_type_code = 'EMPLOYEE_ADDRESS_TYPE'
             group by re.code) d
     where d.distinct_coverages > 1;
    if differing is not null then
        raise exception 'No se sube EMPLOYEE_ADDRESS_TYPE a COM: la cobertura no es la misma en todos los paises. %',
            differing using errcode = '23514';
    end if;
end;
$$;

delete from rulesystem.employee_address_type_profile p
 using rulesystem.rule_entity re
 where re.id = p.address_type_rule_entity_id
   and re.rule_entity_type_code = 'EMPLOYEE_ADDRESS_TYPE'
   and re.id <> (select min(other.id) from rulesystem.rule_entity other
                  where other.rule_entity_type_code = 'EMPLOYEE_ADDRESS_TYPE' and other.code = re.code);

select rulesystem.raise_rule_entity_type('EMPLOYEE_ADDRESS_TYPE', 'COM');
