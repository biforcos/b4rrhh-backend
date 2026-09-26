-- =========================================================
-- V159__the_past_that_changed_leaves_a_row_and_nobody_edits_it_in_place.sql
-- Las marcas de retroactividad (backend#130, paso 6 de workspace#9)
-- =========================================================
--
-- Primera pieza del paso 6, y la que no calcula nada: **registra que el pasado
-- ha cambiado**. Cuando alguien mete unas horas a agosto y agosto ya tiene un
-- recibo entregado, lo que queda escrito aqui es que agosto hay que volver a
-- calcularlo. Quien lo calcula es el #131 y quien lo paga el #133.
--
-- ---------------------------------------------------------
-- Registros, no una marca
-- ---------------------------------------------------------
-- La forma facil seria una columna en el empleado: «recalcular desde». Se
-- descarto en el diseno del 05/10 y por una razon que se ve en cuanto pasa dos
-- veces: una ausencia olvidada al mes anterior y unas horas a dos meses son
-- **dos hechos distintos**, y una columna que se sobreescribe deja de saber
-- cuantos habia. Peor: cuando alguien decide que una correccion no se paga, la
-- columna se pone a nulo y **el recibo ya no puede contar que la habia**.
--
-- Asi que son filas, varias por empleado y mes, y **no se editan en sitio ni se
-- borran**. Descartar es un cambio de estado con quien y por que; irse a otro
-- mes es una fila nueva. El motor consume el minimo de las activas.
--
-- ---------------------------------------------------------
-- Por que en el esquema payroll
-- ---------------------------------------------------------
-- La marca la escriben las verticales de `employee` y la consume el calculo. Su
-- unidad no es una fecha: es un **periodo de nomina**, la vive un `calculation_run`
-- y la cierra un recibo definitivo. Es de payroll; lo que pasa es que la
-- escriben desde fuera, por un unico puerto.

create table payroll.retro_mark (
    id                   bigint generated always as identity primary key,

    -- La clave de negocio del empleado, como en payroll.payroll: identidad por
    -- codigos y no por id surrogado.
    rule_system_code     varchar(10)  not null,
    employee_type_code   varchar(30)  not null,
    employee_number      varchar(20)  not null,

    -- La presencia a la que pertenece la fila que se toco. Un recibo es de una
    -- presencia (payroll.payroll la lleva en su clave), asi que la marca tambien:
    -- unas horas metidas a la presencia 1 de un readmitido no obligan a
    -- recalcular la 2.
    presence_number      integer      not null,

    -- El periodo al que la escritura fue, y desde el que hay que recalcular
    -- HACIA DELANTE hasta el mes abierto. Leido del otro lado es «hasta que mes
    -- alcanza esta marca», que es como lo dice la ficha del empleado.
    from_period_code     varchar(30)  not null,

    status               varchar(20)  not null,

    created_at           timestamp    not null default now(),

    -- Que la genero. No es traza decorativa: la ficha del empleado tiene que
    -- poder decir «una ausencia» o «unas horas» y ensenar de que fila sale, y la
    -- checklist del ciclo tiene que poder agrupar por vertical.
    source_vertical_code varchar(40)  not null,
    source_table         varchar(80)  not null,
    -- Nulo cuando la escritura fue un borrado: la fila que la genero ya no esta,
    -- y la marca es justo lo que queda de ella.
    source_row_id        bigint,
    -- Y la clave de negocio de esa fila, en texto, para las verticales cuya
    -- identidad NO es un id surrogado: una entrada de nomina se identifica por
    -- concepto y periodo (employee.employee_payroll_input), y su modelo de
    -- dominio no tiene id a proposito. Sin esta columna la marca de unas horas
    -- extra no podria decir de QUE concepto eran.
    source_row_key       varchar(200),

    -- El descarte: quien y por que, obligatorios los tres juntos o ninguno.
    discarded_at         timestamp,
    discarded_by         varchar(120),
    discard_reason       varchar(500),

    -- El consumo: que run de que periodo la pago.
    consumed_at          timestamp,
    consumed_period_code varchar(30),
    consumed_run_id      bigint,

    constraint ck_retro_mark_status
        check (status in ('ACTIVE', 'DISCARDED', 'CONSUMED')),

    -- Los `is not null` de estos dos checks NO son redundantes. Una comparacion
    -- con una columna nula sale desconocida, y un CHECK solo rechaza cuando su
    -- expresion es FALSA: sin ellos la restriccion deja pasar exactamente la fila
    -- que existe para impedir. Eso ya paso una vez aqui (V154).
    constraint ck_retro_mark_discarded
        check (
            (status <> 'DISCARDED'
                and discarded_at is null and discarded_by is null and discard_reason is null)
            or (status = 'DISCARDED'
                and discarded_at is not null
                and discarded_by is not null
                and discard_reason is not null
                and length(trim(discard_reason)) > 0)
        ),

    constraint ck_retro_mark_consumed
        check (
            (status <> 'CONSUMED'
                and consumed_at is null and consumed_period_code is null and consumed_run_id is null)
            or (status = 'CONSUMED'
                and consumed_at is not null
                and consumed_period_code is not null
                and consumed_run_id is not null)
        ),

    constraint fk_retro_mark_run
        foreign key (consumed_run_id) references payroll.calculation_run (id)
);

comment on table payroll.retro_mark is
    'Que el pasado ha cambiado: una fila por movimiento a un periodo que ya tenia recibo definitivo. No se editan en sitio ni se borran (backend#130).';

comment on column payroll.retro_mark.from_period_code is
    'El periodo al que la escritura fue, y desde el que se recalcula hacia delante. El motor consume el minimo de las activas del empleado.';

comment on column payroll.retro_mark.source_row_id is
    'La fila que la genero. Nulo si la escritura fue un borrado: entonces la marca es lo unico que queda de ella.';

comment on column payroll.retro_mark.source_row_key is
    'La clave de negocio de la fila, en texto, para las verticales que no tienen id surrogado (las entradas de nomina). Nulo cuando el id ya la identifica.';

comment on constraint ck_retro_mark_discarded on payroll.retro_mark is
    'Un descarte sin quien y sin por que no se puede contar en el recibo. Los is not null no son redundantes: sin ellos el CHECK deja pasar la fila incompleta (V154).';

-- ---------------------------------------------------------
-- Los indices, por como se pregunta
-- ---------------------------------------------------------
-- La pregunta caliente es la del lanzamiento: «de este empleado, que marcas
-- activas hay y cual es la mas antigua». Se hace una vez por empleado del run,
-- asi que ochocientas sesenta veces por corrida.
create index ix_retro_mark_active_by_employee
    on payroll.retro_mark (rule_system_code, employee_type_code, employee_number, presence_number, from_period_code)
    where status = 'ACTIVE';

-- Y la de la ficha, que las quiere todas y en su estado.
create index ix_retro_mark_by_employee
    on payroll.retro_mark (rule_system_code, employee_type_code, employee_number, created_at);
