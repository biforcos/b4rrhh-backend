-- =========================================================
-- V134__record_which_table_row_a_step_read_its_value_from.sql
-- De que fila de tabla leyo un paso su valor (backend#107)
-- =========================================================
--
-- El grafo explica QUE CONCEPTOS alimentaron un numero. No dice QUE FILA DE
-- TABLA puso el P02 = 40,00: el paso guardaba el valor y no su procedencia.
--
-- ---------------------------------------------------------
-- Por que se guarda en vez de resolverse al leer
-- ---------------------------------------------------------
-- Porque la busqueda es por vigencia y por categoria, y las dos cambian. Volver
-- a resolverla al abrir el recibo contestaria DONDE ESTARIA HOY ese valor, no de
-- donde salio: si entre medias alguien movio la vigencia o le cambio la categoria
-- al empleado, el salto llevaria a otra fila y nadie se enteraria. Es el mismo
-- defecto por el que se descarto la puerta del `explain` en el backend#93, y la
-- misma regla que lo resolvio entonces:
--
--     SE GUARDA LO QUE EL MOTOR TENIA DELANTE.
--
-- ---------------------------------------------------------
-- Nulable, y el nulo significa algo
-- ---------------------------------------------------------
-- La mayoria de los pasos no leen ninguna tabla: un AGGREGATE suma conceptos, un
-- PERCENTAGE multiplica una base por un tipo, y un ENGINE_PROVIDED deriva del
-- contexto. En un recibo ESP de 38 pasos leen tabla dos: el precio del dia y el de
-- la hora extra. El nulo no es un hueco por rellenar: es «este valor no vino de una
-- fila».
--
-- ---------------------------------------------------------
-- Por que son dos columnas y no una
-- ---------------------------------------------------------
-- Porque el id de la fila no es una direccion. La pantalla de tablas del designer
-- se abre por (sistema de reglas, codigo de tabla) y la fila se senala dentro:
-- con el id solo habria que preguntar en que tabla vive, que es volver a resolver
-- al leer por la puerta de atras. El sistema de reglas ya lo trae el recibo.
--
-- ---------------------------------------------------------
-- Sin clave ajena, a proposito
-- ---------------------------------------------------------
-- Una fila de tabla se puede borrar, y las dos formas de clave ajena hacen aqui
-- lo contrario de lo que se pide: con `cascade` el paso perderia la procedencia
-- que este fichero existe para conservar, y con `restrict` borrar una fila vieja
-- quedaria bloqueado por los recibos que la leyeron. Un paso apunta a lo que
-- leyo aunque ya no exista, y el cliente tiene que saber tratar una direccion que
-- no lleva a ninguna parte.

alter table payroll.payroll_calculation_step
    add column if not exists source_table_code varchar(100);

alter table payroll.payroll_calculation_step
    add column if not exists source_table_row_id bigint;

comment on column payroll.payroll_calculation_step.source_table_code is
    'Tabla de nomina de la que este paso leyo su valor, o null si el valor no vino de una tabla '
    '(backend#107).';

comment on column payroll.payroll_calculation_step.source_table_row_id is
    'Fila que el motor leyo, tal como la tenia delante al calcular. No se resuelve al leer: la '
    'busqueda es por vigencia y categoria, y volver a hacerla contestaria donde estaria hoy el '
    'valor y no de donde salio (backend#107).';

-- Las dos o ninguna: media direccion no lleva a ningun sitio.
alter table payroll.payroll_calculation_step
    add constraint chk_payroll_calculation_step_source_row
    check (
        (source_table_code is null and source_table_row_id is null)
        or (source_table_code is not null and source_table_row_id is not null)
    );

-- ---------------------------------------------------------
-- Lo que esta migracion NO hace
-- ---------------------------------------------------------
-- No rellena los recibos ya calculados. No se puede: reconstruir la procedencia
-- de un paso viejo es exactamente la busqueda que este fichero acaba de explicar
-- que no contesta la pregunta. Se llenan al recalcular, y hasta entonces el nulo
-- de un recibo viejo se lee igual que el de un AGGREGATE. Es la unica ambiguedad
-- que deja, y se cierra sola.
