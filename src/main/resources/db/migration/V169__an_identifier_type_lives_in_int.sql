-- backend#158, paso 3 del camino 5 (workspace#20, ADR-077): EMPLOYEE_IDENTIFIER_TYPE sube a INT.
--
-- El tipo de documento es del pais que lo emite, no de la reglamentacion que lo pide
-- (workspace#22, decidido el 30/09): un pasaporte italiano es el mismo en ESP que en FRA. Lo
-- nacional es la politica —que tipos habilitan para trabajar, cual es obligatorio para calcular—,
-- y eso es un tipo aparte de nivel 3 (backend#162). Aqui sube el tipo con sus cuatro entidades tal
-- cual, sembradas tres veces por la V14 con un cross join; lo que cada tipo sabe de si mismo (pais
-- emisor, clase, validador) es del backend#161.
--
-- Ninguna tabla apunta a estas entidades por id: employee.identifier guarda el codigo.

select rulesystem.raise_rule_entity_type('EMPLOYEE_IDENTIFIER_TYPE', 'INT');
