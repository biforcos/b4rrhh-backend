-- =========================================================
-- V132__link_payslip_lines_to_the_steps_they_merge.sql
-- La relacion entre una linea del folio y sus pasos, explicita (backend#103)
-- =========================================================
--
-- Una linea del recibo puede ser la SUMA DE VARIOS PASOS y nada lo decia.
-- CalculatePayrollUnitService.collapsePayslipRows agrupa por concepto|tarifa y
-- suma, y esta encendido por omision. El caso que nadie adivina son dos tramos
-- NO CONTIGUOS al mismo precio: el folio ensena una linea donde el calculo dio
-- dos, la pestana «Calculo» ensena las dos, y las dos pantallas discrepan en el
-- numero de filas sin que nada explique por que.
--
-- ---------------------------------------------------------
-- Por que se persiste en vez de derivarse
-- ---------------------------------------------------------
-- El issue pide que se decida aqui y con su motivo, porque la relacion ES
-- derivable: las lineas salen de filtrar los pasos por payslip_order_code y
-- agruparlos por concepto y tarifa.
--
-- El motivo es el ADR-062 §1. Aquel ADR existe porque tener DOS construcciones
-- en paralelo —una para los pasos y otra para las lineas— es lo que permite que
-- diverjan, y su decision fue que hubiera un solo recorrido y una proyeccion.
-- Derivar la relacion despues seria volver a poner la regla de agrupacion en un
-- segundo sitio, y entonces la pregunta «¿y si manana alguien toca una y no la
-- otra?» vuelve a tener sentido.
--
-- La proyeccion conoce la relacion en el instante en que fusiona. Esto es
-- escribir lo que ya sabia, no calcularlo otra vez.

-- ---------------------------------------------------------
-- 1. Del paso a su linea
-- ---------------------------------------------------------
-- Nulo cuando el paso no llego al folio, que son la mayoria: de los 35 pasos de
-- un recibo solo 14 se imprimen. Varios pasos pueden compartir numero, y eso es
-- precisamente la fusion.
alter table payroll.payroll_calculation_step
    add column if not exists payslip_line_number integer;

comment on column payroll.payroll_calculation_step.payslip_line_number is
    'Linea del folio en la que quedo este paso, o null si no llego al folio. Varios pasos con el '
    'mismo numero son los que esa linea funde (backend#103).';

-- SIN restriccion de coherencia, y no por descuido. El invariante que uno querria
-- —un paso se imprime si y solo si tiene linea— no lo cumplen las filas que ya
-- existen: los 12.227 pasos impresos de la semilla tienen payslip_order_code y
-- se quedan sin numero de linea hasta que se recalculen. Anadir la restriccion
-- reventaria esta migracion, y anadirla NOT VALID seria una guarda que no mira
-- justo las filas por las que se puso.
--
-- Rellenar las viejas tampoco vale: habria que reconstruir la agrupacion en SQL,
-- que es la segunda copia de la regla que este fichero acaba de explicar que no
-- queremos. Se llenan al recalcular.

create index if not exists idx_payroll_step_payslip_line
    on payroll.payroll_calculation_step (payroll_id, payslip_line_number);

-- ---------------------------------------------------------
-- 2. De la linea a cuantos pasos funde
-- ---------------------------------------------------------
-- El recuento va en la linea y no se cuenta al vuelo porque la pantalla del
-- recibo no pide los pasos: los sirve otro endpoint. Una marca que obligara a
-- pedir 35 filas para saber si una linea lleva una estrella no se pondria.
alter table payroll.payroll_concept
    add column if not exists merged_step_count integer not null default 1;

alter table payroll.payroll_concept
    add constraint chk_payroll_concept_merged_step_count
        check (merged_step_count >= 1);

-- ---------------------------------------------------------
-- 3. Lo que esta migracion NO hace
-- ---------------------------------------------------------
-- No rellena los recibos ya calculados: sus pasos se quedan sin linea y sus
-- lineas con el 1 del defecto. Rellenarlos exigiria reconstruir la agrupacion
-- con SQL, que es justo la segunda copia de la regla que este fichero explica
-- que no queremos. Se llenan al recalcular.
