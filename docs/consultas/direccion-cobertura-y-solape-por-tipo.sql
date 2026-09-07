-- ===========================================================================
-- DIRECCION (address) -- invariantes de la serie temporal, POR TIPO
-- ===========================================================================
-- Serie de: el empleado Y EL TIPO DE DIRECCION (ADR-057, decision 0). Son
-- series independientes que conviven: que el domicilio y la fiscal esten
-- vigentes el mismo dia no es un solape, es lo normal.
--
-- Cobertura: la declara el catalogo, no el codigo. Se lee de
--            rulesystem.employee_address_type_profile.coverage por sistema de
--            reglas (ADR-054, backend#53): MANDATORY para el domicilio,
--            OPTIONAL para el resto. Un hueco en una opcional es legal.
--
-- Comprueba tres cosas:
--   SOLAPE       dos direcciones DEL MISMO TIPO vigentes el mismo dia.
--   HUECO        un tramo de presencia sin direccion de un tipo cuya
--                cobertura el catalogo declara MANDATORY.
--   SIN_PERFIL   un tipo de direccion en uso que el catalogo no clasifica.
--                Sin el, el HUECO se calcularia sobre una lista de tipos
--                obligatorios incompleta y su cero no valdria nada.
--
-- RESULTADO CORRECTO: CERO FILAS.
--
-- Esta consulta sustituye a la que agregaba TODOS los tipos con range_agg y
-- solo contaba huecos. Aquella devolvio cero y ese cero entro en el ADR-057
-- como hecho: era ciega a los solapes, y la agregacion tapaba que la serie no
-- es por empleado sino por tipo. Bajo su premisa equivocada hay 314 parejas de
-- direcciones de TIPOS DISTINTOS que se pisan y que son correctas (backend#68).
-- ===========================================================================

WITH perfil AS (
    SELECT re.rule_system_code,
           re.code AS address_type_code,
           p.coverage
    FROM rulesystem.employee_address_type_profile p
    JOIN rulesystem.rule_entity re ON re.id = p.address_type_rule_entity_id
    WHERE re.rule_entity_type_code = 'EMPLOYEE_ADDRESS_TYPE'
),
ocurrencia AS (
    SELECT a.employee_id,
           a.id,
           a.address_type_code,
           a.start_date,
           a.end_date,
           daterange(a.start_date, COALESCE(a.end_date, DATE '9999-12-31') + 1, '[)') AS vigencia
    FROM employee.address a
),
exigido AS (
    SELECT p.employee_id,
           p.presence_number,
           p.start_date,
           p.end_date,
           perfil.address_type_code,
           datemultirange(daterange(p.start_date, COALESCE(p.end_date, DATE '9999-12-31') + 1, '[)')) AS vigencia
    FROM employee.presence p
    JOIN employee.employee e ON e.id = p.employee_id
    JOIN perfil ON perfil.rule_system_code = e.rule_system_code
               AND perfil.coverage = 'MANDATORY'
),
cubierto AS (
    SELECT employee_id,
           address_type_code,
           range_agg(vigencia) AS vigencia
    FROM ocurrencia
    GROUP BY employee_id, address_type_code
)
SELECT 'SOLAPE' AS problema,
       e.rule_system_code,
       e.employee_type_code,
       e.employee_number,
       a.address_type_code,
       format('%s..%s se solapa con %s..%s',
              a.start_date, COALESCE(a.end_date::text, 'abierto'),
              b.start_date, COALESCE(b.end_date::text, 'abierto')) AS detalle
FROM ocurrencia a
JOIN ocurrencia b
  ON b.employee_id = a.employee_id
 AND b.address_type_code = a.address_type_code
 AND b.id > a.id
 AND a.vigencia && b.vigencia
JOIN employee.employee e ON e.id = a.employee_id

UNION ALL

SELECT 'HUECO',
       e.rule_system_code,
       e.employee_type_code,
       e.employee_number,
       x.address_type_code,
       format('presencia %s (%s..%s) sin direccion de este tipo en %s',
              x.presence_number,
              x.start_date, COALESCE(x.end_date::text, 'abierta'),
              x.vigencia - COALESCE(c.vigencia, '{}'::datemultirange))
FROM exigido x
JOIN employee.employee e ON e.id = x.employee_id
LEFT JOIN cubierto c ON c.employee_id = x.employee_id
                    AND c.address_type_code = x.address_type_code
WHERE x.vigencia - COALESCE(c.vigencia, '{}'::datemultirange) <> '{}'::datemultirange

UNION ALL

SELECT DISTINCT 'SIN_PERFIL',
       e.rule_system_code,
       e.employee_type_code,
       e.employee_number,
       a.address_type_code,
       'el catalogo no declara si la cobertura de este tipo es obligatoria'
FROM ocurrencia a
JOIN employee.employee e ON e.id = a.employee_id
LEFT JOIN perfil ON perfil.rule_system_code = e.rule_system_code
                AND perfil.address_type_code = a.address_type_code
WHERE perfil.address_type_code IS NULL

ORDER BY 1, 2, 3, 4, 5, 6;
