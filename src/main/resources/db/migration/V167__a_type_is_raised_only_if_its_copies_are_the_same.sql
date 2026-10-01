-- backend#158, paso 3 del camino 5 (workspace#20, ADR-077): subir un tipo de nivel.
--
-- Hasta la V166 todo vivia en la capa nacional, y lo que es igual en cualquier pais estaba
-- sembrado una vez por reglamentacion. Subir un tipo es dejar una sola fila por codigo en la capa
-- de destino (COM o INT) y quitar las copias. Eso solo es legitimo si las copias son la misma cosa
-- escrita N veces: codigo, nombre, descripcion, si esta activa, vigencia y traducciones. Si
-- divergen, la subida falla y dice en que, codigo a codigo y capa a capa: eso es informacion, no un
-- obstaculo, y decidir cual de las versiones manda no le toca a una migracion.
--
-- La funcion la llaman las migraciones siguientes, una por tipo, y se queda en el esquema porque
-- el camino 4 (provincias, tipos de via) la volvera a necesitar. No es API: nada de Java la llama.
--
-- Lo que hace, en este orden y en la transaccion de quien la llame:
--   1. comprueba que el tipo baja de nivel (3 -> 2 o 1, nunca al reves) y que las copias son
--      identicas y estan en todas las capas de su nivel;
--   2. inserta una fila por codigo en la capa de destino;
--   3. mueve a ella las traducciones de una de las copias (las de las otras son las mismas);
--   4. re-apunta al id nuevo cada columna que referencia rule_entity(id). Las FK son
--      «on delete cascade»: borrar una copia a la que algo apunta se llevaria ese algo sin avisar.
--      Si una tabla tiene una clave unica que no admite que N filas pasen a apuntar a la misma, el
--      update falla: la migracion del tipo tiene que resolverlo antes, a mano y comprobado;
--   5. borra las copias y cambia rule_entity_type.level.
-- El trigger diferido de la V166 comprueba al confirmar que cada entidad vive en el nivel de su
-- tipo, asi que el orden de 2 y 5 no importa.

create function rulesystem.raise_rule_entity_type(p_type varchar, p_layer varchar) returns void
language plpgsql as $$
declare
    from_level smallint;
    to_level   smallint;
    problems   text;
    reference  record;
    repointed  int;
    report     text := '';
begin
    select level into from_level from rulesystem.rule_entity_type where code = p_type;
    if not found then
        raise exception 'No se sube %: el tipo no existe', p_type using errcode = '23503';
    end if;
    select level into to_level from rulesystem.layer where code = p_layer;
    if not found then
        raise exception 'No se sube % a %: la capa no existe', p_type, p_layer using errcode = '23503';
    end if;
    if to_level >= from_level then
        raise exception 'No se sube % a %: el tipo es de nivel % y la capa de nivel %; subir es ir a un nivel menor',
            p_type, p_layer, from_level, to_level
            using errcode = '23514';
    end if;

    drop table if exists pg_temp.raise_copy;
    create temp table raise_copy on commit drop as
    select re.id, re.layer_code, re.code, re.name, re.description, re.active, re.start_date, re.end_date,
           coalesce((select string_agg(tr.language_code || '=' || tr.name || coalesce(' (' || tr.description || ')', ''),
                                       '; ' order by tr.language_code)
                       from rulesystem.rule_entity_translation tr
                      where tr.rule_entity_id = re.id), '') as translations
      from rulesystem.rule_entity re
     where re.rule_entity_type_code = p_type;

    -- Una copia por capa de su nivel: si en un pais falta, subir la haria aparecer alli.
    select string_agg(format('%s falta en %s', c.code, array_to_string(c.missing, ', ')), '; ' order by c.code)
      into problems
      from (select codes.code,
                   array(select rsl.layer_code from rulesystem.rule_system_layer rsl where rsl.level = from_level
                         except
                         select rc.layer_code from raise_copy rc where rc.code = codes.code
                         order by 1) as missing
              from (select distinct code from raise_copy) codes) c
     where cardinality(c.missing) > 0;

    -- Y las copias, campo a campo, iguales.
    select concat_ws('; ', problems,
                     string_agg(format('%s.%s: %s', d.code, d.field, d.versions), '; ' order by d.code, d.field))
      into problems
      from (select rc.code, f.key as field,
                   string_agg(format('%s=%s', rc.layer_code, coalesce(f.value, '(nulo)')), ', ' order by rc.layer_code) as versions,
                   count(distinct coalesce(f.value, '(nulo)')) as distinct_values
              from raise_copy rc,
                   jsonb_each_text(jsonb_build_object(
                       'name', rc.name, 'description', rc.description, 'active', rc.active,
                       'start_date', rc.start_date, 'end_date', rc.end_date,
                       'translations', rc.translations)) f
             group by rc.code, f.key) d
     where d.distinct_values > 1;

    if problems <> '' then
        raise exception 'No se sube % a %: las copias no son identicas. %', p_type, p_layer, problems
            using errcode = '23514';
    end if;

    drop table if exists pg_temp.raise_map;
    create temp table raise_map (copy_id bigint primary key, raised_id bigint not null) on commit drop;

    with representative as (
        select distinct on (code) * from raise_copy order by code, id
    ), raised as (
        insert into rulesystem.rule_entity
            (layer_code, rule_entity_type_code, code, name, description, active, start_date, end_date)
        select p_layer, p_type, code, name, description, active, start_date, end_date from representative
        returning id, code
    )
    insert into raise_map (copy_id, raised_id)
    select rc.id, raised.id from raise_copy rc join raised on raised.code = rc.code;

    update rulesystem.rule_entity_translation tr
       set rule_entity_id = m.raised_id
      from (select min(copy_id) as copy_id, raised_id from raise_map group by raised_id) m
     where tr.rule_entity_id = m.copy_id;

    for reference in
        select c.conrelid::regclass as tbl, a.attname as col
          from pg_constraint c
          join pg_attribute a on a.attrelid = c.conrelid and a.attnum = c.conkey[1]
         where c.contype = 'f'
           and c.confrelid = 'rulesystem.rule_entity'::regclass
           and c.conrelid <> 'rulesystem.rule_entity_translation'::regclass
         order by 1, 2
    loop
        execute format('update %s t set %I = m.raised_id from raise_map m where t.%I = m.copy_id',
                       reference.tbl, reference.col, reference.col);
        get diagnostics repointed = row_count;
        if repointed > 0 then
            report := report || format(' %s.%s: %s re-apuntadas.', reference.tbl, reference.col, repointed);
        end if;
    end loop;

    delete from rulesystem.rule_entity where id in (select copy_id from raise_map);
    update rulesystem.rule_entity_type set level = to_level, updated_at = now() where code = p_type;

    raise notice '% sube a %: % copias de % codigos quedan en una fila por codigo.%',
        p_type, p_layer, (select count(*) from raise_map), (select count(distinct raised_id) from raise_map), report;
end;
$$;
