-- =========================================================
-- V155__the_unemployment_rate_depends_on_the_kind_of_contract.sql
-- Dos modalidades de desempleo, y quien decide cual (backend#124)
-- =========================================================
--
-- La V153 dejo el desempleo en 5,50 + 1,55 para todo el mundo. Es el tipo de la
-- **contratacion indefinida**; la de duracion determinada cotiza al 8,30, del
-- que 6,70 son de la empresa y 1,60 de la persona trabajadora.
--
--   Orden PJC/297/2026, art. 33.2.a).1.o y 2.o
--   Orden PJC/178/2025, art. 33.2.a).1.o y 2.o
--   Orden PJC/51/2024,  art. 31.2.a).1.o y 2.o
--
-- Y UNA CORRECCION A LO QUE ESCRIBI EN LA V153: alli deje dicho que «la demo
-- sale bien porque todos sus contratos son indefinidos». **No es verdad y no lo
-- habia mirado.** La semilla tiene los diez tipos de contrato del catalogo, y
-- 182 empleados con el 401 o el 402. Esos 182 llevan cotizando de menos.
--
-- ---------------------------------------------------------
-- 1. Que contrato cotiza por cual, y por que va aqui
-- ---------------------------------------------------------
-- La tabla nueva dice, por sistema de reglas y por contrato, en que modalidad
-- de desempleo cotiza. No es una propiedad del contrato: **es lo que la Orden
-- de cotizacion dice de el**, y por eso vive en `payroll_engine` y no en el
-- catalogo de `rulesystem` (ADR-042). El mismo contrato podria cambiar de
-- modalidad con la Orden del ano que viene sin dejar de ser el mismo contrato,
-- y entonces lo que se declara es una fila nueva con su vigencia.
--
-- LO QUE HACE FALTA LEER DE LA ORDEN, porque no es «indefinido al 5,50 y
-- temporal al 6,70». El art. 33.2.a).1.o mete en el tipo de la indefinida:
--
--   - la contratacion indefinida, incluidos el tiempo parcial y los fijos
--     discontinuos                                        -> 100, 108, 109, 110
--   - los contratos formativos: formacion en alternancia y practica
--     profesional                                                -> 421, 422
--   - los de relevo, sustitucion e interinidad                   -> 410, 420
--   - y cualquier modalidad con una persona con discapacidad reconocida del
--     33 por ciento o mas
--
-- y solo deja en el 2.o la «contratacion de duracion determinada a tiempo
-- completo o a tiempo parcial»                                   -> 401, 402.
--
-- O sea que de los ocho contratos que no son indefinidos, seis cotizan como si
-- lo fueran. Deducirlo del nombre del contrato habria dado seis veces mal.
--
-- LO QUE SIGUE SIN PARTIRSE ES LA BASE. La modalidad llega por el tramo, pero
-- la base de cotizacion y las cuotas son conceptos de ambito PERIOD -la base es
-- mensual, art. 1.1 de la Orden- y el motor las resuelve una vez, con el
-- contexto del ultimo tramo. Un contrato que cambia el dia 16 deja el mes
-- entero al tipo del contrato nuevo, cuando le tocaria medio mes a cada uno.
-- Partir la base por tramo es un cambio del modelo de cotizacion, no de aqui, y
-- tiene su test escrito diciendo lo que pasa y no que este bien.
--
-- LA DISCAPACIDAD NO ENTRA, y se dice aqui: es una condicion de la persona y no
-- del contrato, y el modelo no guarda hoy el grado de discapacidad. Un empleado
-- con un 401 y un 33 por ciento reconocido cotizaria por el 2.o cuando le toca
-- el 1.o. Sale caro y no barato, que es el lado bueno de equivocarse, pero
-- sigue siendo un error y necesita el dato.
--
-- LAS VIGENCIAS: una sola fila abierta por contrato, desde el 1 de enero de
-- 2024. Las tres Ordenes dicen lo mismo palabra por palabra, asi que sembrar
-- tres bloques identicos solo afirmaria que cambio algo que no cambio. Si una
-- Orden mueve un contrato de modalidad, eso se declara cerrando la fila y
-- abriendo otra, que es para lo que estan las fechas.
create table payroll_engine.ss_desempleo_modalidad_contrato (
    id               bigserial    primary key,
    rule_system_code varchar(10)  not null,
    contract_code    varchar(30)  not null,
    modality         varchar(20)  not null,
    valid_from       date         not null,
    valid_to         date,
    created_at       timestamp    not null default now(),
    constraint uq_ss_desempleo_modalidad
        unique (rule_system_code, contract_code, valid_from),
    constraint chk_ss_desempleo_modalidad
        check (modality in ('INDEFINIDA', 'DETERMINADA')),
    constraint chk_ss_desempleo_modalidad_fechas
        check (valid_to is null or valid_from <= valid_to)
);

comment on table payroll_engine.ss_desempleo_modalidad_contrato is
    'En que modalidad de desempleo cotiza cada contrato, segun la Orden de cotizacion. No es una propiedad del contrato sino lo que la Orden dice de el, y por eso vive aqui y no en el catalogo (backend#124, ADR-072).';

comment on column payroll_engine.ss_desempleo_modalidad_contrato.modality is
    'INDEFINIDA (art. 33.2.a).1.o) o DETERMINADA (art. 33.2.a).2.o). El sufijo del codigo de contingencia que se busca en ss_cotizacion_tipos.';

insert into payroll_engine.ss_desempleo_modalidad_contrato
    (rule_system_code, contract_code, modality, valid_from, valid_to)
select 'ESP', v.contrato, v.modalidad, DATE '2024-01-01', cast(null as date)
from (values
    -- Indefinidos, incluidos el tiempo parcial y los fijos discontinuos
    ('100', 'INDEFINIDA'),   -- Indefinido ordinario (jornada completa)
    ('108', 'INDEFINIDA'),   -- Indefinido ordinario (tiempo parcial)
    ('109', 'INDEFINIDA'),   -- Fijo discontinuo (jornada completa)
    ('110', 'INDEFINIDA'),   -- Fijo discontinuo (tiempo parcial)
    -- Sustitucion e interinidad: temporales, pero el art. 33.2.a).1.o los nombra
    ('410', 'INDEFINIDA'),   -- Sustitucion con reserva de puesto
    ('420', 'INDEFINIDA'),   -- Sustitucion en proceso de seleccion
    -- Formativos: igual, nombrados en el 1.o
    ('421', 'INDEFINIDA'),   -- Formacion en alternancia
    ('422', 'INDEFINIDA'),   -- Practica profesional
    -- Y los unicos que van al 2.o
    ('401', 'DETERMINADA'),  -- Temporal por circunstancias de produccion
    ('402', 'DETERMINADA')   -- Temporal ocasional o imprevisible
) as v(contrato, modalidad);

-- ---------------------------------------------------------
-- 2. Los tipos, ahora uno por modalidad
-- ---------------------------------------------------------
-- `DESEMPLEO_EMP` y `DESEMPLEO_TRAB` desaparecen. Podrian quedarse como «la
-- modalidad indefinida» y anadirse solo las dos nuevas, pero entonces el
-- catalogo tendria dos filas que se llaman por lo que cotizan y dos que se
-- llaman por lo que cotizan **mas un supuesto tacito**, y el que las lea dentro
-- de un ano no sabria cual es cual. Los cuatro codigos dicen su modalidad.
--
-- El motor compone el codigo -`DESEMPLEO_EMP_` mas la modalidad del tramo-, asi
-- que un contrato sin modalidad declarada no cotiza de menos: la corrida se para
-- diciendo que falta.
delete from payroll_engine.ss_cotizacion_tipos
 where rule_system_code = 'ESP'
   and contingency_code in ('DESEMPLEO_EMP', 'DESEMPLEO_TRAB');

insert into payroll_engine.ss_cotizacion_tipos
    (rule_system_code, contingency_code, rate, valid_from, valid_to)
select 'ESP', v.contingencia, v.tipo, v.desde, v.hasta
from (values
    -- ── 2024 · Orden PJC/51/2024, art. 31.2.a) ──────────────────────────────
    ('DESEMPLEO_EMP_INDEFINIDA',    5.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('DESEMPLEO_TRAB_INDEFINIDA',   1.55, DATE '2024-01-01', DATE '2024-12-31'),
    ('DESEMPLEO_EMP_DETERMINADA',   6.70, DATE '2024-01-01', DATE '2024-12-31'),
    ('DESEMPLEO_TRAB_DETERMINADA',  1.60, DATE '2024-01-01', DATE '2024-12-31'),

    -- ── 2025 · Orden PJC/178/2025, art. 33.2.a) ─────────────────────────────
    ('DESEMPLEO_EMP_INDEFINIDA',    5.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('DESEMPLEO_TRAB_INDEFINIDA',   1.55, DATE '2025-01-01', DATE '2025-12-31'),
    ('DESEMPLEO_EMP_DETERMINADA',   6.70, DATE '2025-01-01', DATE '2025-12-31'),
    ('DESEMPLEO_TRAB_DETERMINADA',  1.60, DATE '2025-01-01', DATE '2025-12-31'),

    -- ── 2026 · Orden PJC/297/2026, art. 33.2.a) ─────────────────────────────
    ('DESEMPLEO_EMP_INDEFINIDA',    5.50, DATE '2026-01-01', cast(null as date)),
    ('DESEMPLEO_TRAB_INDEFINIDA',   1.55, DATE '2026-01-01', cast(null as date)),
    ('DESEMPLEO_EMP_DETERMINADA',   6.70, DATE '2026-01-01', cast(null as date)),
    ('DESEMPLEO_TRAB_DETERMINADA',  1.60, DATE '2026-01-01', cast(null as date))
) as v(contingencia, tipo, desde, hasta);
