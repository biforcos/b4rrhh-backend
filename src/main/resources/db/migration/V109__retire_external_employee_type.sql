-- ADR-054 §4 (backend#37): EMPLOYEE_TYPE es un tipo cerrado — cada código sembrado lleva
-- lógica aparejada en Java. INTERNAL la tiene (HireEmployeeDefaultValues); EXTERNAL nunca
-- la tuvo: la V49 lo sembró «kept available for reference data coherence», es decir, para
-- que el catálogo no tuviera una sola fila. Un código sin comportamiento en un tipo
-- cerrado es deuda, no un dato, y se retira antes de escribir la guardia que lo vigila.
--
-- Los tipos de empleado de verdad (asalariado, externo, becario) llegarán con su lógica
-- en la migración de renombrado que el ADR-054 deja fuera de alcance.

-- Si algún empleado usara EXTERNAL, retirar el código lo dejaría huérfano en silencio.
-- Eso no es una decisión de migración: se para aquí y se decide con el dato delante.
do $$
declare
    empleados_externos bigint;
begin
    select count(*) into empleados_externos
    from employee.employee
    where employee_type_code = 'EXTERNAL';

    if empleados_externos > 0 then
        raise exception using message =
            'Hay ' || empleados_externos || ' empleados con employee_type_code = EXTERNAL. '
            || 'EXTERNAL no se puede retirar sin decidir qué pasa con ellos (backend#37).';
    end if;
end $$;

-- Las traducciones, si las hubiera, caen en cascada (V105).
delete from rulesystem.rule_entity
where rule_entity_type_code = 'EMPLOYEE_TYPE'
  and code = 'EXTERNAL';
