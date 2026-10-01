-- backend#158, paso 3 del camino 5 (workspace#20, ADR-077): CONTACT_TYPE sube a COM.
--
-- Telefono, movil, movil de empresa, extension y correo son lo mismo en cualquier pais. La V7 los
-- sembraba una vez por reglamentacion con un cross join; aqui quedan una vez, en la capa comun.
--
-- Ninguna tabla apunta a estas entidades por id: employee.contact y work_center_contact guardan
-- el codigo.

select rulesystem.raise_rule_entity_type('CONTACT_TYPE', 'COM');
