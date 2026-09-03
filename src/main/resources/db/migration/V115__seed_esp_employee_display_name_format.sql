-- backend#42: la tabla employee_display_name_format existía desde la V97 y ninguna
-- migración la sembraba. El formato que enseña la pantalla de configuración —«Nombre
-- completo (mayúsculas iniciales)»— estaba puesto a mano en la base local y viajaba en
-- el volcado de la demo, pero no en el repositorio: levantar de cero dejaba a ESP sin
-- formato y el nombre para mostrar salía de una concatenación que se parece lo bastante
-- a un nombre como para que nadie lo notara.
--
-- Se siembra sólo ESP, que es la reglamentación con altas, y sólo donde falte: si alguien
-- ya eligió otro formato en esa base, se respeta. FRA y PRT se quedan sin formato a
-- propósito, igual que se quedaron sin numeración (V99): configurar una reglamentación
-- es una decisión de quien la usa, no de una semilla.

insert into rulesystem.employee_display_name_format (
    rule_system_code,
    display_name_format_code,
    created_at,
    updated_at
)
select
    'ESP',
    'FULL_TITLE_CASE',
    now(),
    now()
where not exists (
    select 1
    from rulesystem.employee_display_name_format existing
    where existing.rule_system_code = 'ESP'
);
