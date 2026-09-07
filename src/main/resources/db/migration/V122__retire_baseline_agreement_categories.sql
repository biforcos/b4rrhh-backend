-- backend#6, segunda parte: CAT_ADMIN, CAT_TECH_1 y CAT_TECH_2 eran las categorias de los
-- convenios inventados que retiro la V121. Sin ellos no cuelgan de nada: la V121 se llevo
-- por cascada las relaciones, y lo que queda son nueve raices sueltas —tres codigos por
-- cada uno de los tres sistemas de reglas, porque la V25 los sembro con un cross join— mas
-- los nueve perfiles de grupo de cotizacion que la V108 les puso para cumplir la guardia 3.
--
-- Un convenio se elige poco; una categoria sale en un desplegable. Y estas salen en ingles:
-- sus literales son 'Administrative', 'Technical Level 1' y 'Technical Level 2', sin
-- traduccion en rule_entity_translation, conviviendo con 'Grupo III - Auxiliar y
-- Administrativo'. Esa mezcla es justo lo que hace que la aplicacion no parezca real.

-- Mismo cuidado que en la V121: labor_classification.agreement_category_code es texto y no
-- clave ajena, y search_code es la dimension por la que una tabla salarial busca su fila.
-- Retirar una categoria que alguien use dejaria el dato huerfano en silencio, y eso no lo
-- decide una migracion.
do $$
declare
    clasificaciones bigint;
    filas_salariales bigint;
begin
    select count(*) into clasificaciones
    from employee.labor_classification
    where agreement_category_code in ('CAT_ADMIN', 'CAT_TECH_1', 'CAT_TECH_2');

    select count(*) into filas_salariales
    from payroll.payroll_table_row
    where search_code in ('CAT_ADMIN', 'CAT_TECH_1', 'CAT_TECH_2');

    if clasificaciones + filas_salariales > 0 then
        raise exception using message =
            'CAT_ADMIN, CAT_TECH_1 o CAT_TECH_2 siguen en uso: '
            || clasificaciones || ' clasificaciones laborales y '
            || filas_salariales || ' filas de tabla salarial. '
            || 'No se pueden retirar sin decidir que pasa con ellas (backend#6).';
    end if;
end $$;

-- Los perfiles de categoria (V108) y las traducciones caen en cascada (V107, V105).
--
-- FRA y PRT se quedan sin convenio y sin categoria, y es a proposito: son sistemas de reglas
-- declarados y sin poblar, que es un estado verdadero. Rellenarlos con convenios inventados
-- seria volver a crear lo que este issue quita. Esta escrito en CONTEXTO.md.
delete from rulesystem.rule_entity
where rule_entity_type_code = 'AGREEMENT_CATEGORY'
  and code in ('CAT_ADMIN', 'CAT_TECH_1', 'CAT_TECH_2');
