-- backend#6: AGR_OFFICE y AGR_TECH son convenios inventados. Nacieron en la V25, cuando el
-- catalogo AGREEMENT no tenia ninguna cita real y hacia falta algo con lo que contratar, y
-- la V50/V52/V60 los arrastraron al baseline de ESP con categorias y perfil.
--
-- Desde la V61 existe el convenio de verdad, el 99002405011982 (BOE-A-2023-13740), con sus
-- tres grupos profesionales, su perfil, sus tablas salariales (V67, V70) y su grafo de
-- conceptos (V65-V73). Los inventados no tienen nada de eso: un empleado bajo ellos se da
-- de alta y luego no tiene nomina calculable, que es justo lo que la demo no puede
-- permitirse. El loader lleva desde workforce-loader#5 filtrando por convenio para no
-- repartir empleados entre los tres.
--
-- Un codigo de convenio sin norma detras ni nomina que calcular es ruido, no un dato.

-- Retirar el convenio bajo el que alguien este clasificado lo dejaria huerfano en silencio:
-- labor_classification.agreement_code es texto, no una clave ajena, y nadie se enteraria.
-- Lo mismo con la parametrizacion de nomina que colgara de ellos. Eso no es una decision de
-- migracion: se para aqui y se decide con el dato delante (como la V109 con EXTERNAL).
do $$
declare
    clasificaciones bigint;
    asignaciones    bigint;
    vinculos        bigint;
begin
    select count(*) into clasificaciones
    from employee.labor_classification
    where agreement_code in ('AGR_OFFICE', 'AGR_TECH');

    select count(*) into asignaciones
    from payroll_engine.concept_assignment
    where agreement_code in ('AGR_OFFICE', 'AGR_TECH');

    -- payroll.payroll_object_activation no se mira: la V103 la dejo caer cuando el
    -- ADR-045 la sustituyo por concept_assignment, que es la linea de arriba.
    select count(*) into vinculos
    from payroll.payroll_object_binding
    where owner_type_code = 'AGREEMENT'
      and owner_code in ('AGR_OFFICE', 'AGR_TECH');

    if clasificaciones + asignaciones + vinculos > 0 then
        raise exception using message =
            'AGR_OFFICE o AGR_TECH siguen en uso: '
            || clasificaciones || ' clasificaciones laborales, '
            || asignaciones || ' asignaciones de concepto y '
            || vinculos || ' vinculos de objeto de nomina. '
            || 'No se pueden retirar sin decidir que pasa con ellos (backend#6).';
    end if;
end $$;

-- El perfil de convenio (V60) y las relaciones con CAT_ADMIN / CAT_TECH_1 / CAT_TECH_2
-- caen en cascada (V107), igual que las traducciones (V105). Las tres categorias se quedan:
-- las usa el resto del baseline y retirarlas es otra conversacion.
--
-- Sin filtro por sistema de reglas a proposito: la V25 los sembro con un cross join contra
-- rule_system, asi que estan tambien en FRA y PRT, y son igual de inventados alli.
delete from rulesystem.rule_entity
where rule_entity_type_code = 'AGREEMENT'
  and code in ('AGR_OFFICE', 'AGR_TECH');
