-- =========================================================
-- V173__the_contribution_groups_are_spanish_only.sql
-- backend#163 (camino 5, ADR-077)
-- =========================================================
--
-- Los grupos de cotizacion son de la Seguridad Social espanola. La V84 los sembro
-- en todas las reglamentaciones que habia, y por eso FRA y PRT tienen once grupos
-- espanoles cada una. Se quitan de las dos; el tipo se queda en el nivel 3, y
-- cuando FRA o PRT tengan nomina sembraran el suyo.
--
-- Antes de borrar se comprueba que nada los usa, por las tres vias que hay:
--   1. una FK a rule_entity (las once que hay, menos las traducciones, que se van
--      con la entidad: on delete cascade);
--   2. el perfil de una categoria de convenio de FRA o PRT que nombre un grupo
--      por codigo (agreement_category_profile.grupo_cotizacion_code);
--   3. los topes de cotizacion de una capa de nomina de FRA o PRT, que tambien
--      nombran el grupo por codigo (ss_cotizacion_topes.grupo_code).
-- Si algo los usa, la migracion falla y dice que.
-- =========================================================

do $$
declare
    fk record;
    referencias bigint;
begin
    for fk in
        select c.conrelid::regclass as tabla, a.attname as columna
          from pg_constraint c
          join pg_attribute a on a.attrelid = c.conrelid and a.attnum = c.conkey[1]
         where c.contype = 'f'
           and c.confrelid = 'rulesystem.rule_entity'::regclass
           and c.conrelid <> 'rulesystem.rule_entity_translation'::regclass
    loop
        execute format(
            'select count(*) from %s x join rulesystem.rule_entity e on e.id = x.%I
              where e.rule_entity_type_code = ''GRUPO_COTIZACION'' and e.layer_code in (''FRA'', ''PRT'')',
            fk.tabla, fk.columna)
          into referencias;
        if referencias > 0 then
            raise exception 'backend#163: % fila(s) de % apuntan a un grupo de cotizacion de FRA o PRT por %',
                referencias, fk.tabla, fk.columna;
        end if;
    end loop;

    select count(*) into referencias
      from rulesystem.agreement_category_profile p
      join rulesystem.rule_entity c on c.id = p.agreement_category_rule_entity_id
     where c.layer_code in ('FRA', 'PRT')
       and p.grupo_cotizacion_code is not null;
    if referencias > 0 then
        raise exception 'backend#163: % categoria(s) de convenio de FRA o PRT nombran un grupo de cotizacion',
            referencias;
    end if;

    select count(*) into referencias
      from payroll_engine.ss_cotizacion_topes
     where layer_code in ('NOM_FRA', 'NOM_PRT');
    if referencias > 0 then
        raise exception 'backend#163: % tope(s) de cotizacion de NOM_FRA o NOM_PRT nombran un grupo de cotizacion',
            referencias;
    end if;
end $$;

delete from rulesystem.rule_entity
 where rule_entity_type_code = 'GRUPO_COTIZACION'
   and layer_code in ('FRA', 'PRT');

do $$
declare
    restantes bigint;
    fuera bigint;
begin
    select count(*) filter (where layer_code = 'ESP'), count(*) filter (where layer_code <> 'ESP')
      into restantes, fuera
      from rulesystem.rule_entity
     where rule_entity_type_code = 'GRUPO_COTIZACION';
    if restantes <> 11 or fuera <> 0 then
        raise exception 'backend#163: quedan % grupos en ESP y % fuera; se esperaban 11 y 0', restantes, fuera;
    end if;
end $$;
