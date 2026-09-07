-- ===========================================================================
-- CENTRO DE COSTE (cost_center) -- invariantes de la serie temporal
-- ===========================================================================
-- Serie de: el empleado, PERO CON LA OCURRENCIA COMPUESTA (ADR-057, decision
-- 0). Una ocurrencia no es una fila: es una ventana de reparto, el conjunto de
-- lineas que comparten tramo de fechas. El invariante se aplica al tramo, no a
-- la linea. Lo que identifica una ventana es el dia en que empieza.
--
-- Cobertura: OPCIONAL -- es imputacion analitica, no un requisito legal ni del
--            calculo (ADR-057, decision 1). AQUI NO SE COMPRUEBAN HUECOS a
--            proposito: un tramo de presencia sin reparto es valido. Se dio
--            por obligatoria una vez sin justificarla y la comprobacion
--            devolvio 1000 de 1003 empleados incumpliendola.
--
-- Comprueba tres cosas:
--   VENTANA_INCOHERENTE  lineas que empiezan el mismo dia y acaban en dias
--                        distintos. Sin esto la ventana no esta bien definida
--                        y el SOLAPE de abajo se calcularia sobre tramos
--                        inventados.
--   SOLAPE               dos ventanas distintas vigentes el mismo dia. Las
--                        lineas de una misma ventana nunca son un solape.
--   REPARTO              una ventana cuyas lineas suman mas de 100.
--
-- RESULTADO CORRECTO: CERO FILAS.
-- ===========================================================================

WITH ventana AS (
    SELECT cc.employee_id,
           cc.start_date,
           min(COALESCE(cc.end_date, DATE '9999-12-31')) AS fin_min,
           max(COALESCE(cc.end_date, DATE '9999-12-31')) AS fin_max,
           sum(cc.allocation_percentage) AS reparto,
           count(*) AS lineas
    FROM employee.cost_center cc
    GROUP BY cc.employee_id, cc.start_date
)
SELECT 'VENTANA_INCOHERENTE' AS problema,
       e.rule_system_code,
       e.employee_type_code,
       e.employee_number,
       format('la ventana que empieza el %s tiene %s lineas que acaban entre %s y %s',
              v.start_date, v.lineas, v.fin_min, v.fin_max) AS detalle
FROM ventana v
JOIN employee.employee e ON e.id = v.employee_id
WHERE v.fin_min <> v.fin_max

UNION ALL

SELECT 'SOLAPE',
       e.rule_system_code,
       e.employee_type_code,
       e.employee_number,
       format('la ventana %s..%s se solapa con la ventana %s..%s',
              a.start_date, a.fin_max, b.start_date, b.fin_max)
FROM ventana a
JOIN ventana b
  ON b.employee_id = a.employee_id
 AND b.start_date > a.start_date
 AND daterange(a.start_date, a.fin_max + 1, '[)') && daterange(b.start_date, b.fin_max + 1, '[)')
JOIN employee.employee e ON e.id = a.employee_id

UNION ALL

SELECT 'REPARTO',
       e.rule_system_code,
       e.employee_type_code,
       e.employee_number,
       format('la ventana que empieza el %s reparte %s por ciento en %s lineas',
              v.start_date, v.reparto, v.lineas)
FROM ventana v
JOIN employee.employee e ON e.id = v.employee_id
WHERE v.reparto > 100

ORDER BY 1, 2, 3, 4, 5;
