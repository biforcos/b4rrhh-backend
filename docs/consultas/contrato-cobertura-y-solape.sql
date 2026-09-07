-- ===========================================================================
-- CONTRATO -- invariantes de la serie temporal
-- ===========================================================================
-- Serie de: el empleado. Una ocurrencia vigente cada vez (ADR-057, decision 0).
-- Cobertura: OBLIGATORIA -- un empleado presente sin contrato no es un estado
--            valido (ADR-057, decision 1).
--
-- Comprueba dos invariantes, y los dos por separado:
--   SOLAPE  dos contratos del mismo empleado vigentes el mismo dia.
--   HUECO   un tramo de presencia sin ningun contrato vigente.
--
-- RESULTADO CORRECTO: CERO FILAS.
-- Cada fila es un defecto de los datos, no un aviso: el detalle dice que
-- fechas lo provocan.
--
-- El solape se busca por parejas y NO agregando la serie: una union de rangos
-- se traga el solape y lo esconde, que es como se dio por buena una cobertura
-- que no lo era (backend#68).
-- ===========================================================================

WITH ocurrencia AS (
    SELECT c.employee_id,
           c.id,
           c.start_date,
           c.end_date,
           daterange(c.start_date, COALESCE(c.end_date, DATE '9999-12-31') + 1, '[)') AS vigencia
    FROM employee.contract c
),
presencia AS (
    SELECT p.employee_id,
           p.presence_number,
           p.start_date,
           p.end_date,
           datemultirange(daterange(p.start_date, COALESCE(p.end_date, DATE '9999-12-31') + 1, '[)')) AS vigencia
    FROM employee.presence p
),
cubierto AS (
    SELECT employee_id,
           range_agg(vigencia) AS vigencia
    FROM ocurrencia
    GROUP BY employee_id
)
SELECT 'SOLAPE' AS problema,
       e.rule_system_code,
       e.employee_type_code,
       e.employee_number,
       format('%s..%s se solapa con %s..%s',
              a.start_date, COALESCE(a.end_date::text, 'abierto'),
              b.start_date, COALESCE(b.end_date::text, 'abierto')) AS detalle
FROM ocurrencia a
JOIN ocurrencia b
  ON b.employee_id = a.employee_id
 AND b.id > a.id
 AND a.vigencia && b.vigencia
JOIN employee.employee e ON e.id = a.employee_id

UNION ALL

SELECT 'HUECO',
       e.rule_system_code,
       e.employee_type_code,
       e.employee_number,
       format('presencia %s (%s..%s) sin contrato en %s',
              p.presence_number,
              p.start_date, COALESCE(p.end_date::text, 'abierta'),
              p.vigencia - COALESCE(c.vigencia, '{}'::datemultirange))
FROM presencia p
JOIN employee.employee e ON e.id = p.employee_id
LEFT JOIN cubierto c ON c.employee_id = p.employee_id
WHERE p.vigencia - COALESCE(c.vigencia, '{}'::datemultirange) <> '{}'::datemultirange

ORDER BY 1, 2, 3, 4, 5;
