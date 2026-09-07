-- backend#76: IND y TMP en CONTRACT, y FT1, PT1 e INT en CONTRACT_SUBTYPE, son la misma
-- figura que los convenios inventados que retiraron la V121 y la V122. Nacieron en la V29,
-- cuando el catalogo no tenia ningun tipo de contrato real y hacia falta algo con lo que
-- contratar, y la V50/V52 los arrastraron al baseline de ESP con sus cinco relaciones.
--
-- Desde la V82 existen los diez tipos legales espanoles —100, 108, 109, 110, 401, 402, 410,
-- 420, 421 y 422, del Real Decreto-ley 32/2021— con su subtipo 01. Los genericos no anaden
-- ninguno: 'Indefinite Contract' es lo mismo que el 100 dicho en ingles y sin norma detras.
--
-- Y se ven. Un tipo de contrato sale en un desplegable, no se elige una vez: ahi convivian
-- 'Indefinite Contract', 'Full Time' e 'Internship' con '110 - Fijo discontinuo (tiempo
-- parcial)'. Esa mezcla es lo que hace que la aplicacion no parezca real.
--
-- El loader ya los dejaba fuera desde workforce-loader#5 (numeric-contract-types-only), y
-- su propio comentario decia donde estaba el arreglo: en el catalogo, no en el loader.

-- employee.contract.contract_code y contract_subtype_code son texto, no claves ajenas:
-- retirar un tipo bajo el que alguien este contratado dejaria el dato huerfano en silencio.
-- Eso no lo decide una migracion (como la V109 con EXTERNAL y la V121 con los convenios).
--
-- payroll_engine.concept_assignment no se mira: sus ambitos son compania, convenio y tipo
-- de empleado (V58), no hay ninguno por contrato. payroll.payroll_object_binding si, porque
-- owner_type_code es texto libre y admite cualquier tipo de entidad.
do $$
declare
    contratos bigint;
    subtipos  bigint;
    vinculos  bigint;
begin
    select count(*) into contratos
    from employee.contract
    where contract_code in ('IND', 'TMP');

    select count(*) into subtipos
    from employee.contract
    where contract_subtype_code in ('FT1', 'PT1', 'INT');

    select count(*) into vinculos
    from payroll.payroll_object_binding
    where (owner_type_code = 'CONTRACT' and owner_code in ('IND', 'TMP'))
       or (owner_type_code = 'CONTRACT_SUBTYPE' and owner_code in ('FT1', 'PT1', 'INT'));

    if contratos + subtipos + vinculos > 0 then
        raise exception using message =
            'IND, TMP, FT1, PT1 o INT siguen en uso: '
            || contratos || ' contratos por su tipo, '
            || subtipos || ' por su subtipo y '
            || vinculos || ' vinculos de objeto de nomina. '
            || 'No se pueden retirar sin decidir que pasa con ellos (backend#76).';
    end if;
end $$;

-- Las cinco relaciones IND->FT1, IND->PT1, TMP->FT1, TMP->PT1 y TMP->INT caen en cascada
-- por sus dos extremos (V107), y no hay nada mas colgando: CONTRACT y CONTRACT_SUBTYPE no
-- declaran extension de perfil en rule_entity_extension (V106), asi que la guardia 3 del
-- ADR-053 no les sembro nada en la V108, y no tienen traducciones (V104/V105).
--
-- Sin filtro por sistema de reglas a proposito: la V29 los sembro con un cross join contra
-- rule_system, asi que estan tambien en FRA y PRT, y son igual de inventados alli. Los dos
-- se quedan sin ningun tipo de contrato, que es el mismo estado en el que la V122 los dejo
-- sin convenio: un sistema de reglas declarado y sin poblar es un estado verdadero.
delete from rulesystem.rule_entity
where (rule_entity_type_code = 'CONTRACT' and code in ('IND', 'TMP'))
   or (rule_entity_type_code = 'CONTRACT_SUBTYPE' and code in ('FT1', 'PT1', 'INT'));
