-- backend#174: los bloques del recibo solo crecen, porque un cerrado se vuelve a dibujar con ellos.
--
-- Un recibo cerrado al que le falta el PDF se regenera de sus filas definitivas, y eso solo es
-- licito si el documento de hoy es EL MISMO que el que se archivo al cerrar. Las lineas llevan
-- congelado todo lo suyo (literal, importe, payslip_section_code y payslip_subsection_code, desde
-- el backend#109 y el #121); lo que no llevan es el NOMBRE y el ORDEN de su bloque y de su
-- apartado, que el documento lee de estas dos tablas tal como estan hoy. No tienen fechas.
--
-- Renombrar «Devengos», reordenar los bloques o borrar un apartado cambiaria por tanto el papel de
-- todos los recibos ya cerrados que se regeneren despues, sin que ninguno de ellos se haya tocado.
-- Anadir no cambia nada: un bloque o un apartado nuevo solo recoge lineas cuyo codigo congelado lo
-- nombre, y las de un recibo cerrado ya estaban congeladas antes.
--
-- Asi que las dos tablas solo crecen. Lo que se cierra es el dato que el papel imprime; las
-- marcas de tiempo de la fila (created_at, updated_at) siguen libres.
--
-- El dia que haga falta renombrar un bloque de verdad, esto no es un estorbo que se quita: es la
-- senal de que el catalogo tiene que pasar a tener vigencia y el documento leerlo a la fecha del
-- periodo. Se quita el trigger en la misma migracion que trae eso.

create function payroll_engine.a_payslip_block_only_grows() returns trigger
language plpgsql as $$
begin
    if tg_op = 'DELETE' then
        raise exception '% no admite borrar: el recibo cerrado que tenga lineas en % se volveria a dibujar sin el (backend#174)',
            tg_table_name, to_jsonb(old) - 'created_at' - 'updated_at';
    end if;

    if (to_jsonb(new) - 'created_at' - 'updated_at') <> (to_jsonb(old) - 'created_at' - 'updated_at') then
        raise exception '% solo crece: % pasaria a ser %, y los recibos cerrados se volverian a dibujar distintos (backend#174)',
            tg_table_name,
            to_jsonb(old) - 'created_at' - 'updated_at',
            to_jsonb(new) - 'created_at' - 'updated_at';
    end if;

    return new;
end;
$$;

create trigger trg_payslip_section_only_grows
    before update or delete on payroll_engine.payslip_section
    for each row execute function payroll_engine.a_payslip_block_only_grows();

create trigger trg_payslip_subsection_only_grows
    before update or delete on payroll_engine.payslip_subsection
    for each row execute function payroll_engine.a_payslip_block_only_grows();

comment on function payroll_engine.a_payslip_block_only_grows() is
    'Los bloques y apartados del recibo solo crecen: un recibo cerrado sin documento se vuelve a dibujar con ellos y tiene que salir igual (backend#174).';
