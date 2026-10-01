-- backend#156, paso 1 del camino 5 (workspace#20, ADR-077): las capas existen y todo esta en el
-- nivel 3. Al acabar, nada se comporta distinto.
--
-- Una reglamentacion deja de ser el sitio donde se definen las cosas y pasa a ser un puzle de
-- cinco capas, una por nivel. Las cosas se definen en una capa. Hoy todo lo que hay es nacional,
-- asi que todo va a la capa nacional, que tiene el mismo codigo que su reglamentacion: ESP en
-- ESP, FRA en FRA, PRT en PRT. Por eso rule_entity.rule_system_code pasa a layer_code sin mover
-- un dato: es un rename con FK, no un traslado. Reclasificar tipos a COM e INT es el paso 3
-- (backend#158); las tablas del motor a la capa 4, el paso 5 (backend#159).
--
-- Cuatro invariantes, y donde vive cada una:
--   1. una capa por nivel en cada reglamentacion      -> PK (rule_system_code, level)
--   2. el nivel de la capa es el de la fila            -> FK compuesta (layer_code, level)
--   3. las cinco, obligatorias                         -> trigger de restriccion, diferido
--   4. el nivel de la capa de una entidad es el de su tipo -> trigger de restriccion, diferido
-- Las dos ultimas no caben en un CHECK porque miran otra tabla. Son diferidas para que una misma
-- transaccion pueda crear la reglamentacion y despues sus capas (CreateRuleSystemService), o
-- mover un tipo de nivel y sus entidades de capa (backend#158), y la comprobacion se haga al
-- confirmar, con todo en su sitio.

-- ---------------------------------------------------------------------------------------------
-- Los cinco niveles. Son fijos: no hay API que los cree, y el codigo los nombra por numero.
-- ---------------------------------------------------------------------------------------------
create table rulesystem.level (
    level smallint     primary key,
    name  varchar(100) not null unique,
    constraint chk_level_range check (level between 1 and 5)
);

insert into rulesystem.level (level, name) values
    (1, 'Común'),
    (2, 'Internacional'),
    (3, 'Nacional'),
    (4, 'Nómina nacional'),
    (5, 'Nómina de empresa');

-- ---------------------------------------------------------------------------------------------
-- La capa. 20 caracteres y no 5 como rule_system.code: NOM_<codigo>_EMP son trece con un codigo
-- de reglamentacion de cinco.
-- ---------------------------------------------------------------------------------------------
create table rulesystem.layer (
    id         bigint       generated always as identity primary key,
    code       varchar(20)  not null,
    name       varchar(100) not null,
    level      smallint     not null references rulesystem.level(level),
    active     boolean      not null default true,
    created_at timestamp    not null default now(),
    updated_at timestamp    not null default now(),
    constraint uk_layer_code unique (code),
    -- La diana de la FK compuesta de rule_system_layer: es lo que permite decir en el esquema, y
    -- no en un trigger, que la capa montada en el nivel N es de nivel N.
    constraint uk_layer_code_level unique (code, level),
    constraint chk_layer_code check (code ~ '^[A-Z0-9_]+$')
);

insert into rulesystem.layer (code, name, level) values
    ('COM', 'Común', 1),
    ('INT', 'Internacional', 2);

insert into rulesystem.layer (code, name, level)
select rs.code, rs.name, 3 from rulesystem.rule_system rs
union all
select 'NOM_' || rs.code, 'Nómina nacional · ' || rs.name, 4 from rulesystem.rule_system rs
union all
select 'NOM_' || rs.code || '_EMP', 'Nómina de empresa · ' || rs.name, 5 from rulesystem.rule_system rs;

-- ---------------------------------------------------------------------------------------------
-- El puzle: que capa monta cada reglamentacion en cada nivel.
-- ---------------------------------------------------------------------------------------------
create table rulesystem.rule_system_layer (
    rule_system_code varchar(5)  not null references rulesystem.rule_system(code),
    level            smallint    not null references rulesystem.level(level),
    layer_code       varchar(20) not null,
    created_at       timestamp   not null default now(),
    primary key (rule_system_code, level),
    constraint fk_rule_system_layer_layer_level
        foreign key (layer_code, level) references rulesystem.layer(code, level)
);

insert into rulesystem.rule_system_layer (rule_system_code, level, layer_code)
select rs.code, l.level,
       case l.level
           when 1 then 'COM'
           when 2 then 'INT'
           when 3 then rs.code
           when 4 then 'NOM_' || rs.code
           when 5 then 'NOM_' || rs.code || '_EMP'
       end
  from rulesystem.rule_system rs
 cross join rulesystem.level l;

create function rulesystem.assert_rule_system_has_one_layer_per_level() returns trigger
language plpgsql as $$
declare
    affected varchar(5);
    mounted  int;
begin
    -- Por to_jsonb y no por new.campo: la misma funcion sirve a dos tablas con columnas
    -- distintas, y plpgsql resuelve new.campo aunque la rama del case no se tome.
    affected := case when tg_op = 'DELETE' then to_jsonb(old) ->> 'rule_system_code'
                     when tg_table_name = 'rule_system' then to_jsonb(new) ->> 'code'
                     else to_jsonb(new) ->> 'rule_system_code' end;
    -- Si la reglamentacion ya no existe (se borro en la misma transaccion), no hay puzle que
    -- comprobar.
    if not exists (select 1 from rulesystem.rule_system where code = affected) then
        return null;
    end if;
    select count(*) into mounted from rulesystem.rule_system_layer where rule_system_code = affected;
    if mounted <> (select count(*) from rulesystem.level) then
        raise exception 'La reglamentacion % monta % capas y tiene que montar una por nivel (ADR-077)',
            affected, mounted
            using errcode = '23514';
    end if;
    return null;
end;
$$;

create constraint trigger trg_rule_system_has_one_layer_per_level
    after insert on rulesystem.rule_system
    deferrable initially deferred
    for each row execute function rulesystem.assert_rule_system_has_one_layer_per_level();

create constraint trigger trg_rule_system_layer_keeps_one_per_level
    after delete or update on rulesystem.rule_system_layer
    deferrable initially deferred
    for each row execute function rulesystem.assert_rule_system_has_one_layer_per_level();

-- ---------------------------------------------------------------------------------------------
-- El tipo declara su nivel. Todos a 3 en esta migracion: reclasificar es el paso 3.
-- Sin default, como literal_class y maintenance_mode: un tipo nuevo lo declara.
-- ---------------------------------------------------------------------------------------------
alter table rulesystem.rule_entity_type add column level smallint references rulesystem.level(level);
update rulesystem.rule_entity_type set level = 3;
alter table rulesystem.rule_entity_type alter column level set not null;

-- ---------------------------------------------------------------------------------------------
-- La entidad vive en una capa. El codigo de la capa nacional es el de su reglamentacion, asi que
-- ningun valor cambia: sale la FK a rule_system, entra la FK a layer.
-- ---------------------------------------------------------------------------------------------
alter table rulesystem.rule_entity drop constraint fk_rule_entity_rule_system_code;
alter table rulesystem.rule_entity rename column rule_system_code to layer_code;
alter table rulesystem.rule_entity alter column layer_code type varchar(20);
alter table rulesystem.rule_entity
    add constraint fk_rule_entity_layer_code foreign key (layer_code) references rulesystem.layer(code);

alter table rulesystem.rule_entity drop constraint uk_rule_entity_business;
alter table rulesystem.rule_entity
    add constraint uk_rule_entity_business unique (layer_code, rule_entity_type_code, code);

create function rulesystem.assert_rule_entity_lives_at_the_level_of_its_type() returns trigger
language plpgsql as $$
declare
    misplaced record;
    changed   jsonb := to_jsonb(new);
begin
    select re.layer_code, re.rule_entity_type_code, re.code, l.level as layer_level, t.level as type_level
      into misplaced
      from rulesystem.rule_entity re
      join rulesystem.layer l            on l.code = re.layer_code
      join rulesystem.rule_entity_type t on t.code = re.rule_entity_type_code
     where l.level <> t.level
       and case tg_table_name
               when 'rule_entity'      then re.id = (changed ->> 'id')::bigint
               when 'rule_entity_type' then t.code = changed ->> 'code'
               when 'layer'            then l.code = changed ->> 'code'
           end
     limit 1;
    if found then
        raise exception 'La entidad %/%/% esta en una capa de nivel % y su tipo es de nivel % (ADR-077)',
            misplaced.layer_code, misplaced.rule_entity_type_code, misplaced.code,
            misplaced.layer_level, misplaced.type_level
            using errcode = '23514';
    end if;
    return null;
end;
$$;

create constraint trigger trg_rule_entity_lives_at_the_level_of_its_type
    after insert or update of layer_code, rule_entity_type_code on rulesystem.rule_entity
    deferrable initially deferred
    for each row execute function rulesystem.assert_rule_entity_lives_at_the_level_of_its_type();

create constraint trigger trg_rule_entity_type_level_keeps_its_entities
    after update of level on rulesystem.rule_entity_type
    deferrable initially deferred
    for each row execute function rulesystem.assert_rule_entity_lives_at_the_level_of_its_type();

create constraint trigger trg_layer_level_keeps_its_entities
    after update of level on rulesystem.layer
    deferrable initially deferred
    for each row execute function rulesystem.assert_rule_entity_lives_at_the_level_of_its_type();
