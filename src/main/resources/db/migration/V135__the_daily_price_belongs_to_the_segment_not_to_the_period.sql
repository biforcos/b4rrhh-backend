-- =========================================================
-- V135__the_daily_price_belongs_to_the_segment_not_to_the_period.sql
-- El precio del dia es del tramo, no del periodo (backend#47)
-- =========================================================
--
-- La V119 repaso el ambito de los 36 conceptos y dejo escrito esto:
--
--     P02 PRECIO_DIA_PLENO     un unico precio del rule system para el periodo.
--
-- Era cierto mientras el periodo solo se partiera por la jornada. Desde el
-- backend#47 tambien lo parte un cambio de categoria de convenio, y entonces
-- aquella frase deja de ser verdad: el precio del dia sale de una fila de tabla
-- que se busca POR CATEGORIA, asi que un empleado que asciende el dia 16 tiene
-- dos precios en el mismo mes.
--
-- ---------------------------------------------------------
-- Por que esto es parametrizacion y no codigo
-- ---------------------------------------------------------
-- El motor ya sabe evaluar un concepto una vez por tramo y resolver cada uno
-- contra su propio contexto de convenio. Lo unico que faltaba es que este
-- concepto DIJERA que su valor es del tramo. Sin esta linea, la particion del
-- backend#47 funciona y no cambia ni un numero: P02 se seguiria resolviendo una
-- vez, con la categoria del ultimo tramo, y los quince primeros dias se pagarian
-- al precio de los ultimos.
--
-- ---------------------------------------------------------
-- No cambia ningun recibo que ya saliera bien
-- ---------------------------------------------------------
-- Con una sola categoria en el mes los dos tramos leen la misma fila y dan el
-- mismo numero, y el folio los vuelve a fundir: el colapso agrupa por
-- concepto|tarifa. Lo unico que se mueve es el recuento de PASOS de los
-- empleados con el mes partido, que suman uno: P02 se evalua una vez por tramo.
--
-- ---------------------------------------------------------
-- P03 se queda en PERIOD, y no por simetria
-- ---------------------------------------------------------
-- El precio de la hora extra sale de una tabla por categoria igual que este, asi
-- que el mismo argumento le valdria. Lo que lo impide es quien lo lee: el 102
-- (IMPORTE_HORAS_EXTRA) es PERIOD y ningun operando puede cruzar de segmento a
-- periodo (ADR-058), asi que mover P03 obligaria a mover el 102 — y el 102
-- multiplica las horas que el empleado declaro PARA EL MES, que en dos tramos se
-- contarian dos veces. Eso no es un ambito mal puesto: es que las horas extra
-- por tramo no estan modeladas. Se queda como esta hasta que lo esten.

update payroll_engine.payroll_concept concept
set execution_scope = 'SEGMENT',
    updated_at      = current_timestamp
from payroll_engine.payroll_object object
where object.id               = concept.object_id
  and object.rule_system_code = 'ESP'
  and object.object_type_code = 'CONCEPT'
  and object.object_code      = 'P02'
  and concept.execution_scope <> 'SEGMENT';
