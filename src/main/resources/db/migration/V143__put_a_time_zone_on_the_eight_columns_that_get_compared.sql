-- =========================================================
-- V143__put_a_time_zone_on_the_eight_columns_that_get_compared.sql
-- Las ocho columnas que se comparan entre si pasan a llevar zona (backend#116)
-- =========================================================
--
-- RuleSystemLastChangeLookupAdapter decide si un recibo sigue vigente
-- comparando el maximo de siete updated_at con payroll.payroll.calculated_at.
-- Las ocho columnas eran timestamp WITHOUT time zone, o sea: cada quien
-- guardaba SU HORA DE PARED y el numero no decia cual.
--
-- Y aqui hay dos relojes de verdad. La semilla la fabrica el loader en una
-- maquina en Europe/Madrid; la CT de la demo corre en UTC. Medido en el
-- backend#108: durante las dos horas siguientes a cada resiembra, una regla
-- editada en la demo escribia un updated_at ANTERIOR al calculated_at de todos
-- los recibos, y el recibo decia «vigente» con una regla cambiada debajo. Un
-- fallo presentado como un hecho.
--
-- ---------------------------------------------------------
-- Por que 'Europe/Madrid' y no UTC en el USING
-- ---------------------------------------------------------
-- Porque el USING no elige la zona en la que se va a guardar -- una columna
-- timestamptz guarda un instante y no tiene zona-- sino la zona EN LA QUE HAY
-- QUE LEER lo que ya esta escrito. Y lo que ya esta escrito en cualquier base
-- de este proyecto viene de la semilla, que la escribe el loader en Madrid.
--
-- La excepcion son las filas que la CT haya escrito despues de sembrar, que
-- quedan dos horas desplazadas. No se corrigen aqui y no hace falta: el
-- deploy#17 regenera el volcado con este esquema ya puesto, asi que esas filas
-- desaparecen con la siguiente siembra. Dejar el USING en UTC para salvarlas
-- estropearia las 100.000 que vienen de la semilla para salvar unas pocas.
--
-- ---------------------------------------------------------
-- Las otras 96 no se tocan, y por que
-- ---------------------------------------------------------
-- En la base hay 104 columnas timestamp sin zona (y una con zona, la
-- deploy.semilla.capturada_en del volcado). Estas ocho son las unicas que se
-- COMPARAN ENTRE SI. Las otras 96 son sellos: se escriben, se leen y se
-- ensenan, y nadie pregunta cual es anterior a cual. Convertirlas todas es
-- otro tamano de cambio y no arregla nada mas que esto.
--
-- El dia que alguien compare dos de esas 96, lo que hay que hacer es traerlas
-- aqui, no anadir una bandera de zona al arranque. Una bandera es una
-- convencion y se rompe el dia que alguien lanza el loader sin ella; el tipo
-- de la columna no se puede olvidar.
--
-- ---------------------------------------------------------
-- Los siete updated_at de la reglamentacion
-- ---------------------------------------------------------
alter table payroll_engine.payroll_object
    alter column updated_at type timestamptz using updated_at at time zone 'Europe/Madrid';

alter table payroll_engine.payroll_concept
    alter column updated_at type timestamptz using updated_at at time zone 'Europe/Madrid';

alter table payroll_engine.payroll_concept_operand
    alter column updated_at type timestamptz using updated_at at time zone 'Europe/Madrid';

alter table payroll_engine.payroll_concept_feed_relation
    alter column updated_at type timestamptz using updated_at at time zone 'Europe/Madrid';

alter table payroll_engine.concept_assignment
    alter column updated_at type timestamptz using updated_at at time zone 'Europe/Madrid';

alter table payroll.payroll_object_binding
    alter column updated_at type timestamptz using updated_at at time zone 'Europe/Madrid';

alter table payroll.payroll_table_row
    alter column updated_at type timestamptz using updated_at at time zone 'Europe/Madrid';

-- ---------------------------------------------------------
-- Y el calculated_at contra el que se comparan
-- ---------------------------------------------------------
alter table payroll.payroll
    alter column calculated_at type timestamptz using calculated_at at time zone 'Europe/Madrid';

comment on column payroll.payroll.calculated_at is
    'Cuando se calculo este recibo, como INSTANTE. Lleva zona porque se compara con los updated_at de la reglamentacion, y una comparacion entre dos horas de pared de dos maquinas distintas no significa nada (backend#116).';

comment on column payroll_engine.payroll_object.updated_at is
    'Lleva zona porque entra en la comparacion del RuleSystemLastChangeLookupAdapter. Sus hermanas created_at no, porque nadie las compara (backend#116).';
