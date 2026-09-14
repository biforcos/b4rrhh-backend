-- =========================================================
-- V130__retire_orphan_p_ss_concept.sql
-- backend#96
-- =========================================================
--
-- RETIRA P_SS (TIPO_SS), QUE LLEVA HUERFANO DESDE LA V91.
--
-- La V77 lo sembro como el PERCENTAGE del concepto 700: la cuota obrera era un 6,35 % todo
-- en uno. La V91 partio la cotizacion del trabajador en cuatro conceptos —P_SS_CC, P_SS_FP,
-- P_SS_MEI y P_SS_DESEMPLEO— y reapunto ese operando a P_SS_CC. Movio la arista y dejo el
-- nodo. Mover la arista era lo que habia que hacer; retirar el nodo no formaba parte de aquel
-- cambio y nadie volvio.
--
-- DE LAS DOS SALIDAS, ESTA ES LA PRIMERA
--
-- El backend#96 dejaba dos: retirarlo, o atarlo como el AGREGADO de los cuatro —«cotizacion
-- del trabajador, total»—. Se retira. Atarlo seria un concepto nuevo y no limpieza: haria
-- falta decidir de que naturaleza es, si alimenta al 980 o solo informa, y que sitio ocupa en
-- el folio. Ninguna de esas tres cosas esta decidida, y sembrar un AGGREGATE para que el
-- catalogo deje de tener un huerfano es inventarle un uso a una pieza para no tener que
-- borrarla.
--
-- Si algun dia hace falta ese total, se abre con su decision delante y se llama como toque.
-- Lo que no puede quedarse es un concepto que nadie ejecuta con un calculador Java detras que
-- parece en uso.
--
-- POR QUE ESTO NO CAMBIA NINGUN NUMERO
--
-- Porque nadie lo ejecutaba. No esta asignado, no alimenta a nadie y no es operando de nadie,
-- asi que ningun plan lo incluye y ningun recibo lo ha tenido nunca como paso. Lo que cambia
-- es que «los 36 conceptos del motor» deja de significar dos cosas: el catalogo ESP pasa a
-- tener 35 y los 35 entran en un plan. Los pasos de un recibo siguen siendo 35 en un mes
-- entero y 39 en uno del mes partido — retirar un concepto que nadie ejecuta no puede anadir
-- un paso, y de ahi que el numero no se mueva (medido en el backend#97).
--
-- Los comentarios de la V129 y de la V77 hablan de P_SS en presente. No se tocan: una
-- migracion aplicada no se reescribe, y lo que decian era verdad cuando se escribio. Esta es
-- la que dice que dejo de serlo.
--
-- LA GUARDA ES EL CUERPO DE LA MIGRACION, NO UN ADORNO
--
-- Borrar esta bien SI Y SOLO SI sigue siendo inalcanzable, y entre que se escribio el
-- backend#96 y que esto corra en una base de verdad puede haber pasado justo lo contrario:
-- que alguien lo haya atado. Un delete que se lleve por delante una asignacion o un operando
-- recien puestos seria la salida 2 deshecha en silencio. Asi que primero se comprueban las
-- tres cosas que lo harian alcanzable —las mismas tres que mira conceptsInNoPlan() en el
-- test del backend#93— y si alguna se cumple, esto para y lo dice.

do $$
declare
    objeto_id bigint;
begin
    select id into objeto_id
    from payroll_engine.payroll_object
    where rule_system_code = 'ESP'
      and object_type_code = 'CONCEPT'
      and object_code      = 'P_SS';

    if objeto_id is null then
        -- Ya no esta. Nada que retirar y nada que avisar.
        return;
    end if;

    if exists (select 1 from payroll_engine.concept_assignment
                where rule_system_code = 'ESP' and concept_code = 'P_SS') then
        raise exception
            'P_SS ya no esta huerfano: tiene asignacion en payroll_engine.concept_assignment. '
            'Eso es la salida 2 del backend#96 —atarlo— y esta migracion es la 1 —retirarlo—. '
            'Si se ha atado a proposito, esta migracion sobra y hay que quitarla; si se ato sin '
            'querer, lo que hay que borrar es la asignacion.';
    end if;

    if exists (select 1 from payroll_engine.payroll_concept_operand
                where source_object_id = objeto_id or target_object_id = objeto_id) then
        raise exception
            'P_SS ya no esta huerfano: es operando de algun concepto, o alguno es operando suyo. '
            'Retirarlo ahora dejaria ese concepto sin su operando. Mirad quien lo usa antes de '
            'volver a correr esto (backend#96).';
    end if;

    if exists (select 1 from payroll_engine.payroll_concept_feed_relation
                where source_object_id = objeto_id or target_object_id = objeto_id) then
        raise exception
            'P_SS ya no esta huerfano: alimenta a alguien, o alguien le alimenta a el. '
            'Retirarlo ahora romperia esa arista del grafo (backend#96).';
    end if;

    -- El concepto se va con el objeto: la clave ajena de payroll_concept lleva
    -- "on delete cascade" desde la V78.
    delete from payroll_engine.payroll_object where id = objeto_id;
end $$;
