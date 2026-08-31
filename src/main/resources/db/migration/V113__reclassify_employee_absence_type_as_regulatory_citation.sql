-- backend#37 (ADR-054 corregido en ebc0332): EMPLOYEE_ABSENCE_TYPE no es vocabulario del
-- dominio, es cita reglamentaria. La V102 lo dice por todas partes: sus literales son
-- terminología de la Seguridad Social («IT Contingencia Común», «IT Accidente de Trabajo
-- / Enfermedad Profesional») y del artículo 37.3 del Estatuto («Permiso por fuerza
-- mayor»); el propio fichero se llama _esp; no lleva el cross join sobre rule_system que
-- sí llevan la V4 y la V16; y el nombre va en castellano con la descripción en inglés,
-- al revés que en todos los tipos de vocabulario. No estaba mal sembrado: estaba mal
-- clasificado, y sembrarlo en FRA y PRT habría sido inventar contenido normativo.

update rulesystem.rule_entity_type
set
    literal_class = 'REGULATORY_CITATION',
    updated_at = now()
where code = 'EMPLOYEE_ABSENCE_TYPE';
