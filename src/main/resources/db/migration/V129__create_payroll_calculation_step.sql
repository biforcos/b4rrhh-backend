-- =========================================================
-- V129__create_payroll_calculation_step.sql
-- backend#93
-- =========================================================
--
-- ESTA TABLA CUELGA DE UN RECIBO, NO DE UNA EJECUCION.
--
-- El prefijo calculation_* ya lo usan payroll.calculation_run y
-- payroll.calculation_run_message, y esas dos cuelgan de la ejecucion: una corrida y los
-- mensajes de una corrida. Esta no. Una fila de aqui es un paso que el motor dio calculando
-- UN recibo, y muere con el. Lo que va por ejecucion —el grafo, que alimenta a que— no cabe
-- aqui: eso es la regla y no el resultado (ADR-061), y su forma esta sin decidir.
--
-- POR QUE EXISTE
--
-- Del catalogo de 36 conceptos del motor, 14 llevan payslip_order_code y llegan al recibo; los
-- otros 22 no: CalculatePayrollUnitService descartaba todo el que no lo tuviera. Esos 22 —5 BASE
-- y 17 TECHNICAL— se calculaban, alimentaban a los demas y se tiraban, y son exactamente los que
-- explican de donde sale el numero. Sin ellos, una pantalla que tenga que ensenar «de donde sale
-- este importe» pondria 14 valores sobre 36 nodos, con los huecos justo donde esta la
-- explicacion.
--
-- POR QUE NO SE ENSANCHO payroll.payroll_concept
--
-- Porque esa tabla significa «linea de recibo» y asi se queda, y porque meterle los 22 estaba
-- roto por construccion: PayrollConceptEntity compara por (payroll, line_number) y la
-- coleccion del agregado es un LinkedHashSet, asi que con line_number nulo los 22 son iguales
-- entre si y se queda uno. Veintiuna filas desaparecerian antes de llegar a la base, sin
-- excepcion y sin registro.
--
-- LA IDENTIDAD ES (payroll_id, execution_order)
--
-- Nunca nula, nunca repetida, y es a la vez el orden en que el motor ejecuto cada paso, que es
-- la diferencia entre «aqui tienes 36 valores» y «esto es lo que hizo, en este orden». Tenia
-- que soportar el mismo concepto repetido dentro del mismo recibo: un empleado con el mes
-- partido tiene SALARIO_BASE dos veces, con precios distintos, en segmentos distintos.
--
-- Los 36 no son 36 filas, y por dos motivos. El primero: 4 conceptos son de ambito SEGMENT y se
-- evaluan una vez por segmento. El segundo, medido al implementar esto: de los 36 del catalogo
-- ESP solo 35 entran en un plan, porque P_SS (TIPO_SS) se quedo huerfano en la V91 —el porcentaje
-- del 700 paso de leerlo a el a leer P_SS_CC— y no esta asignado, ni alimenta, ni es operando de
-- nadie. Asi que un empleado normal deja 35 filas y uno del mes partido 39, con el 101 dos veces.
-- Comprobado sobre la semilla de la demo: 873 recibos, 868 con 35 pasos y 5 con 39.
--
-- execution_scope VA EXPLICITO
--
-- No se codifica un hecho en la ausencia de otro: el ambito es una columna, no algo que se
-- deduzca de que las fechas vengan nulas. Y como las dos cosas tienen que decir lo mismo, que
-- las fechas sean nulas si y solo si el ambito es PERIOD es un invariante de la fila y lleva
-- su restriccion de esquema.

create table payroll.payroll_calculation_step (
    payroll_id         bigint not null,
    execution_order    integer not null,
    concept_code       varchar(30) not null,
    concept_mnemonic   varchar(50) not null,
    calculation_type   varchar(30) not null,
    functional_nature  varchar(30) not null,
    execution_scope    varchar(30) not null,
    segment_start_date date,
    segment_end_date   date,
    amount             numeric(19,6) not null,
    quantity           numeric(19,6),
    rate               numeric(19,6),
    payslip_order_code varchar(30),
    created_at         timestamp not null default now()
);

alter table payroll.payroll_calculation_step
    add constraint pk_payroll_calculation_step
    primary key (payroll_id, execution_order);

alter table payroll.payroll_calculation_step
    add constraint fk_payroll_calculation_step_payroll
    foreign key (payroll_id)
    references payroll.payroll(id)
    on delete cascade;

alter table payroll.payroll_calculation_step
    add constraint chk_payroll_calculation_step_scope_dates
    check (
        (execution_scope = 'PERIOD'
            and segment_start_date is null
            and segment_end_date is null)
        or (execution_scope <> 'PERIOD'
            and segment_start_date is not null
            and segment_end_date is not null
            and segment_start_date <= segment_end_date)
    );

comment on table payroll.payroll_calculation_step is
    'Los pasos que el motor dio calculando un recibo: todos los conceptos que ejecuto, no solo '
    'los que llegan al folio. Cuelga del recibo (payroll.payroll), no de la ejecucion, pese al '
    'prefijo calculation_* que comparte con calculation_run (backend#93).';
