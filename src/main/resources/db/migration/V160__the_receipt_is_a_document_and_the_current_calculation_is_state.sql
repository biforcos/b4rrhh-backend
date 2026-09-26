-- =========================================================
-- V160__the_receipt_is_a_document_and_the_current_calculation_is_state.sql
-- El calculo vigente de un mes cerrado (backend#131, paso 6 de workspace#9,
-- ADR-076)
-- =========================================================
--
-- Dos cosas con la misma forma y distinta naturaleza:
--
--   El RECIBO es un documento: inmutable, se entrega, tiene PDF.
--   El CALCULO VIGENTE es estado: mutable, se pisa, no lo ve nadie.
--
-- Confundirlas es como se pudren los motores de nomina. Un motor que deja
-- recalcular el recibo de agosto «porque hay que corregirlo» pierde la unica
-- copia de lo que se le entrego al empleado, y a partir de ahi ya no hay forma
-- de explicar una nomina: el numero que el empleado tiene impreso deja de
-- existir en el sistema. Un motor que en cambio se niega a recalcular agosto no
-- puede pagar un atraso.
--
-- La salida es tener las dos tablas. **No se pierde nada al pisar el vigente**:
-- la historia vive en los recibos, cada linea de atraso guardada en su mes con
-- su periodo de origen (el backend#133).
--
-- ---------------------------------------------------------
-- Conceptos y no totales
-- ---------------------------------------------------------
-- Lo que viaja al atraso son los deltas POR CONCEPTO -diez euros de salario
-- base de agosto no son lo mismo que diez de horas extra, ni cotizan igual, ni
-- se imprimen en el mismo bloque-, asi que guardar el liquido de agosto no
-- serviria para construir ni una linea. Por eso la tabla hija.
--
-- Lo que NO se guarda son los pasos de calculo. El vigente no se explica: se
-- compara. Guardarlos multiplicaria por treinta el tamano de esto -la semilla
-- tiene 30.575 pasos y 12.000 lineas- para responder una pregunta que nadie
-- hace de un numero que se va a pisar. La explicacion de un atraso (el #134) se
-- da con tres numeros -vigente, pagado, diferencia- y los tres son importes.

create table payroll.current_calculation (
    id                 bigint generated always as identity primary key,

    -- La misma clave de negocio que payroll.payroll, y por la misma razon: el
    -- vigente es «lo que vale este mes de esta presencia de este empleado».
    rule_system_code   varchar(10) not null,
    employee_type_code varchar(30) not null,
    employee_number    varchar(20) not null,
    payroll_period_code varchar(30) not null,
    payroll_type_code  varchar(30) not null,
    presence_number    integer     not null,

    -- Cuando se calculo y quien lo pidio. El run es el del PERIODO ABIERTO que
    -- disparo la retro, no el del mes que se recalculo: un lanzamiento de
    -- septiembre escribe vigentes de junio, julio y agosto, y los tres llevan el
    -- run de septiembre. Por eso el periodo y el run son columnas distintas y
    -- ninguna se deduce de la otra.
    calculated_at      timestamp   not null,
    run_id             bigint,

    created_at         timestamp   not null default now(),
    updated_at         timestamp   not null default now(),

    constraint uk_current_calculation_business
        unique (rule_system_code, employee_type_code, employee_number,
                payroll_period_code, payroll_type_code, presence_number),

    constraint fk_current_calculation_run
        foreign key (run_id) references payroll.calculation_run (id)
);

comment on table payroll.current_calculation is
    'Lo que un mes cerrado vale HOY. Estado, no documento: se pisa en cada retro y no lo ve nadie. La historia vive en los recibos (backend#131, ADR-076).';

comment on column payroll.current_calculation.run_id is
    'El run del periodo ABIERTO que disparo la retro, no el del mes recalculado. Por eso periodo y run son columnas distintas.';

-- ---------------------------------------------------------
-- Las lineas del vigente
-- ---------------------------------------------------------
-- Mismas columnas que payroll.payroll_concept en lo que hace falta para
-- construir una linea de atraso, y ni una mas. No lleva `origin_period_code`:
-- el vigente es de UN mes y todo lo suyo es de ese mes. Un atraso dentro de un
-- vigente seria el atraso de un atraso, y eso no existe: lo que se compara es
-- «lo que agosto vale» contra «lo que por agosto se ha pagado», y la segunda
-- mitad la suman los recibos.
create table payroll.current_calculation_concept (
    id                      bigint generated always as identity primary key,
    current_calculation_id  bigint       not null,
    line_number             integer      not null,
    concept_code            varchar(30)  not null,
    concept_mnemonic        varchar(50),
    concept_label           varchar(200) not null,
    amount                  numeric(19, 6) not null,
    quantity                numeric(19, 6),
    rate                    numeric(19, 6),
    concept_nature_code     varchar(30)  not null,
    display_order           integer      not null,
    payslip_section_code    varchar(30),
    payslip_subsection_code varchar(30),

    constraint fk_current_calculation_concept_root
        foreign key (current_calculation_id)
        references payroll.current_calculation (id)
        on delete cascade,

    constraint uk_current_calculation_concept_line
        unique (current_calculation_id, line_number)
);

comment on table payroll.current_calculation_concept is
    'Las lineas de un calculo vigente. Conceptos y no totales, porque lo que viaja al atraso son los deltas por concepto (backend#131).';

-- La pregunta del #133: «dame el vigente de este mes de este empleado, entero».
create index ix_current_calculation_concept_by_root
    on payroll.current_calculation_concept (current_calculation_id, concept_code);
