-- =========================================================
-- V153__the_bases_and_the_limits_of_each_year_with_their_own_dates.sql
-- Los topes de 2024, 2025 y 2026, cada uno con su vigencia y su cita (backend#123)
-- =========================================================
--
-- `ss_cotizacion_topes` mezclaba dos ejercicios: las bases minimas por grupo
-- eran las de 2024 y el tope maximo el de 2025, todo con `valid_from`
-- 2025-01-01. La demo calcula `202609`. Ninguna de las cifras era la del
-- ejercicio que se calcula, y la vigencia decia lo contrario de lo que era.
--
-- Las filas viejas **se borran** en vez de cerrarse. Cerrarlas seria declarar
-- que estuvieron vigentes en 2025, y no lo estuvieron: no son las de ningun
-- ejercicio. Lo que se guarda de ellas es esta explicacion.
--
-- Cada fila de aqui abajo sale de una Orden de cotizacion y se puede comprobar
-- con ella delante:
--
--   2024  Orden PJC/51/2024, de 29 de enero (BOE de 30 de enero de 2024),
--         art. 2 (topes) y art. 3 (bases por grupo), en la redaccion dada por
--         la Orden PJC/281/2024, de 27 de marzo (BOE de 28 de marzo de 2024).
--   2025  Orden PJC/178/2025, de 25 de febrero (BOE de 26 de febrero de 2025),
--         arts. 2 y 3.
--   2026  Orden PJC/297/2026, de 30 de marzo (BOE-A-2026-7296, BOE de 31 de
--         marzo de 2026), arts. 2 y 3.
--
-- EL TOPE MINIMO DE PROFESIONALES es, en las tres, el salario minimo
-- interprofesional incrementado en un sexto (art. 2.2 de cada Orden). En 2025 y
-- en 2026 la propia Orden escribe ademas un suelo -1.381,20 y 1.424,40-; en
-- 2024 no lo escribe, y la cifra sale de la cuenta: el Real Decreto 145/2024,
-- de 6 de febrero, art. 1, fija el SMI en 1.134,00 euros/mes, y 1.134,00 mas su
-- sexto son 1.323,00.
--
-- Coincide con la base minima de los grupos 4 a 7 de su ejercicio, y esa
-- coincidencia es de la norma y no de esta tabla: en los grupos 1 a 3 las dos
-- cifras se separan, que es lo que el backend#121 dejo escrito.
--
-- La cifra diaria del tope minimo es la mensual entre treinta, y coincide con la
-- base minima diaria de los grupos 8 a 11 del mismo ejercicio: 44,10, 46,04 y
-- 47,48.
--
-- ---------------------------------------------------------
-- 1. Fuera lo que no era de ningun ano
-- ---------------------------------------------------------
delete from payroll_engine.ss_cotizacion_topes where rule_system_code = 'ESP';

-- ---------------------------------------------------------
-- 2. Los tres ejercicios
-- ---------------------------------------------------------
insert into payroll_engine.ss_cotizacion_topes
    (rule_system_code, grupo_code, period_type, contingency_code, base_min, base_max, valid_from, valid_to)
select 'ESP', v.grupo, v.periodo, v.contingencia, v.minimo, v.maximo, v.desde, v.hasta
from (values
    -- ── 2024 · Orden PJC/51/2024, arts. 2 y 3 (redaccion de la PJC/281/2024) ──
    ('01', 'MENSUAL', 'COMUNES',       1847.40, 4720.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('02', 'MENSUAL', 'COMUNES',       1532.10, 4720.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('03', 'MENSUAL', 'COMUNES',       1332.90, 4720.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('04', 'MENSUAL', 'COMUNES',       1323.00, 4720.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('05', 'MENSUAL', 'COMUNES',       1323.00, 4720.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('06', 'MENSUAL', 'COMUNES',       1323.00, 4720.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('07', 'MENSUAL', 'COMUNES',       1323.00, 4720.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('08', 'DIARIO',  'COMUNES',         44.10,  157.35, DATE '2024-01-01', DATE '2024-12-31'),
    ('09', 'DIARIO',  'COMUNES',         44.10,  157.35, DATE '2024-01-01', DATE '2024-12-31'),
    ('10', 'DIARIO',  'COMUNES',         44.10,  157.35, DATE '2024-01-01', DATE '2024-12-31'),
    ('11', 'DIARIO',  'COMUNES',         44.10,  157.35, DATE '2024-01-01', DATE '2024-12-31'),
    ('01', 'MENSUAL', 'PROFESIONALES', 1323.00, 4720.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('02', 'MENSUAL', 'PROFESIONALES', 1323.00, 4720.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('03', 'MENSUAL', 'PROFESIONALES', 1323.00, 4720.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('04', 'MENSUAL', 'PROFESIONALES', 1323.00, 4720.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('05', 'MENSUAL', 'PROFESIONALES', 1323.00, 4720.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('06', 'MENSUAL', 'PROFESIONALES', 1323.00, 4720.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('07', 'MENSUAL', 'PROFESIONALES', 1323.00, 4720.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('08', 'DIARIO',  'PROFESIONALES',   44.10,  157.35, DATE '2024-01-01', DATE '2024-12-31'),
    ('09', 'DIARIO',  'PROFESIONALES',   44.10,  157.35, DATE '2024-01-01', DATE '2024-12-31'),
    ('10', 'DIARIO',  'PROFESIONALES',   44.10,  157.35, DATE '2024-01-01', DATE '2024-12-31'),
    ('11', 'DIARIO',  'PROFESIONALES',   44.10,  157.35, DATE '2024-01-01', DATE '2024-12-31'),

    -- ── 2025 · Orden PJC/178/2025, arts. 2 y 3 ───────────────────────────────
    ('01', 'MENSUAL', 'COMUNES',       1929.00, 4909.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('02', 'MENSUAL', 'COMUNES',       1599.60, 4909.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('03', 'MENSUAL', 'COMUNES',       1391.70, 4909.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('04', 'MENSUAL', 'COMUNES',       1381.20, 4909.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('05', 'MENSUAL', 'COMUNES',       1381.20, 4909.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('06', 'MENSUAL', 'COMUNES',       1381.20, 4909.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('07', 'MENSUAL', 'COMUNES',       1381.20, 4909.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('08', 'DIARIO',  'COMUNES',         46.04,  163.65, DATE '2025-01-01', DATE '2025-12-31'),
    ('09', 'DIARIO',  'COMUNES',         46.04,  163.65, DATE '2025-01-01', DATE '2025-12-31'),
    ('10', 'DIARIO',  'COMUNES',         46.04,  163.65, DATE '2025-01-01', DATE '2025-12-31'),
    ('11', 'DIARIO',  'COMUNES',         46.04,  163.65, DATE '2025-01-01', DATE '2025-12-31'),
    ('01', 'MENSUAL', 'PROFESIONALES', 1381.20, 4909.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('02', 'MENSUAL', 'PROFESIONALES', 1381.20, 4909.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('03', 'MENSUAL', 'PROFESIONALES', 1381.20, 4909.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('04', 'MENSUAL', 'PROFESIONALES', 1381.20, 4909.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('05', 'MENSUAL', 'PROFESIONALES', 1381.20, 4909.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('06', 'MENSUAL', 'PROFESIONALES', 1381.20, 4909.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('07', 'MENSUAL', 'PROFESIONALES', 1381.20, 4909.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('08', 'DIARIO',  'PROFESIONALES',   46.04,  163.65, DATE '2025-01-01', DATE '2025-12-31'),
    ('09', 'DIARIO',  'PROFESIONALES',   46.04,  163.65, DATE '2025-01-01', DATE '2025-12-31'),
    ('10', 'DIARIO',  'PROFESIONALES',   46.04,  163.65, DATE '2025-01-01', DATE '2025-12-31'),
    ('11', 'DIARIO',  'PROFESIONALES',   46.04,  163.65, DATE '2025-01-01', DATE '2025-12-31'),

    -- ── 2026 · Orden PJC/297/2026, arts. 2 y 3 ───────────────────────────────
    ('01', 'MENSUAL', 'COMUNES',       1989.30, 5101.20, DATE '2026-01-01', cast(null as date)),
    ('02', 'MENSUAL', 'COMUNES',       1649.70, 5101.20, DATE '2026-01-01', cast(null as date)),
    ('03', 'MENSUAL', 'COMUNES',       1435.20, 5101.20, DATE '2026-01-01', cast(null as date)),
    ('04', 'MENSUAL', 'COMUNES',       1424.40, 5101.20, DATE '2026-01-01', cast(null as date)),
    ('05', 'MENSUAL', 'COMUNES',       1424.40, 5101.20, DATE '2026-01-01', cast(null as date)),
    ('06', 'MENSUAL', 'COMUNES',       1424.40, 5101.20, DATE '2026-01-01', cast(null as date)),
    ('07', 'MENSUAL', 'COMUNES',       1424.40, 5101.20, DATE '2026-01-01', cast(null as date)),
    ('08', 'DIARIO',  'COMUNES',         47.48,  170.04, DATE '2026-01-01', cast(null as date)),
    ('09', 'DIARIO',  'COMUNES',         47.48,  170.04, DATE '2026-01-01', cast(null as date)),
    ('10', 'DIARIO',  'COMUNES',         47.48,  170.04, DATE '2026-01-01', cast(null as date)),
    ('11', 'DIARIO',  'COMUNES',         47.48,  170.04, DATE '2026-01-01', cast(null as date)),
    ('01', 'MENSUAL', 'PROFESIONALES', 1424.40, 5101.20, DATE '2026-01-01', cast(null as date)),
    ('02', 'MENSUAL', 'PROFESIONALES', 1424.40, 5101.20, DATE '2026-01-01', cast(null as date)),
    ('03', 'MENSUAL', 'PROFESIONALES', 1424.40, 5101.20, DATE '2026-01-01', cast(null as date)),
    ('04', 'MENSUAL', 'PROFESIONALES', 1424.40, 5101.20, DATE '2026-01-01', cast(null as date)),
    ('05', 'MENSUAL', 'PROFESIONALES', 1424.40, 5101.20, DATE '2026-01-01', cast(null as date)),
    ('06', 'MENSUAL', 'PROFESIONALES', 1424.40, 5101.20, DATE '2026-01-01', cast(null as date)),
    ('07', 'MENSUAL', 'PROFESIONALES', 1424.40, 5101.20, DATE '2026-01-01', cast(null as date)),
    ('08', 'DIARIO',  'PROFESIONALES',   47.48,  170.04, DATE '2026-01-01', cast(null as date)),
    ('09', 'DIARIO',  'PROFESIONALES',   47.48,  170.04, DATE '2026-01-01', cast(null as date)),
    ('10', 'DIARIO',  'PROFESIONALES',   47.48,  170.04, DATE '2026-01-01', cast(null as date)),
    ('11', 'DIARIO',  'PROFESIONALES',   47.48,  170.04, DATE '2026-01-01', cast(null as date))
) as v(grupo, periodo, contingencia, minimo, maximo, desde, hasta);

-- ---------------------------------------------------------
-- 3. Los tipos, tambien por ejercicio
-- ---------------------------------------------------------
-- Misma enfermedad y mismo remedio: `ss_cotizacion_tipos` tenia once filas
-- todas con `valid_from` 2025-01-01, y el MEI que llevaban -0,58 + 0,11- no es
-- el de ningun ejercicio: el 0,58 es la parte del empleador de 2024 y el 0,11
-- no aparece en ninguna Orden. Se borran y se siembran los tres ejercicios.
--
-- Y con ellos entra la correccion que el issue pedia y que no es de ano:
-- **`DESEMPLEO_EMP` = 7,05 era el tipo TOTAL**, no la cuota de la empresa. En
-- la contratacion indefinida la empresa paga 5,50 y la persona trabajadora
-- 1,55; los 7,05 son la suma. El `721` llevaba cobrandole a la empresa el total
-- desde la V88. Que el tipo dependa ademas del tipo de contrato -8,30 en la
-- temporal, 6,70 + 1,60- es el backend#124, y necesita una condicion por
-- segmento que aqui no se monta.
--
-- CITA DE CADA FILA, por ejercicio:
--
--   2024  Orden PJC/51/2024, de 29 de enero (BOE de 30 de enero de 2024):
--         art. 4.a) contingencias comunes 28,30 (23,60 + 4,70); art. 4.c) MEI
--         0,70 (0,58 + 0,12); art. 5, parrafo tercero, horas extraordinarias
--         que no son de fuerza mayor 28,30 (23,60 + 4,70); art. 31.2.a).1.o
--         desempleo en contratacion indefinida 7,05 (5,50 + 1,55); art. 31.2
--         FOGASA 0,20 a cargo de la empresa y formacion profesional 0,70
--         (0,60 + 0,10).
--   2025  Orden PJC/178/2025, de 25 de febrero (BOE de 26 de febrero de 2025):
--         art. 4.a); art. 16 MEI 0,80 (0,67 + 0,13); art. 5, parrafo tercero;
--         art. 33.2.a).1.o; art. 33.2.b) FOGASA; art. 33.2.c) formacion
--         profesional.
--   2026  Orden PJC/297/2026, de 30 de marzo (BOE-A-2026-7296, BOE de 31 de
--         marzo de 2026): art. 4.a); art. 16 MEI 0,90 (0,75 + 0,15); art. 5,
--         parrafo tercero; art. 33.2.a).1.o; art. 33.2.b); art. 33.2.c).
--
-- Los tipos de comunes y de horas extraordinarias no cambian en los tres anos.
-- Se siembran igualmente los tres, con su vigencia: una fila abierta desde 2024
-- diria que nadie ha comprobado 2025 ni 2026, y aqui lo que se declara es que
-- se han comprobado los tres.
delete from payroll_engine.ss_cotizacion_tipos where rule_system_code = 'ESP';

insert into payroll_engine.ss_cotizacion_tipos
    (rule_system_code, contingency_code, rate, valid_from, valid_to)
select 'ESP', v.contingencia, v.tipo, v.desde, v.hasta
from (values
    -- ── 2024 · Orden PJC/51/2024 ─────────────────────────────────────────────
    ('CC_EMP',           23.60, DATE '2024-01-01', DATE '2024-12-31'),
    ('CC_TRAB',           4.70, DATE '2024-01-01', DATE '2024-12-31'),
    ('MEI_EMP',           0.58, DATE '2024-01-01', DATE '2024-12-31'),
    ('MEI_TRAB',          0.12, DATE '2024-01-01', DATE '2024-12-31'),
    ('HORAS_EXTRA_EMP',  23.60, DATE '2024-01-01', DATE '2024-12-31'),
    ('HORAS_EXTRA_TRAB',  4.70, DATE '2024-01-01', DATE '2024-12-31'),
    ('DESEMPLEO_EMP',     5.50, DATE '2024-01-01', DATE '2024-12-31'),
    ('DESEMPLEO_TRAB',    1.55, DATE '2024-01-01', DATE '2024-12-31'),
    ('FOGASA_EMP',        0.20, DATE '2024-01-01', DATE '2024-12-31'),
    ('FP_EMP',            0.60, DATE '2024-01-01', DATE '2024-12-31'),
    ('FP_TRAB',           0.10, DATE '2024-01-01', DATE '2024-12-31'),

    -- ── 2025 · Orden PJC/178/2025 ────────────────────────────────────────────
    ('CC_EMP',           23.60, DATE '2025-01-01', DATE '2025-12-31'),
    ('CC_TRAB',           4.70, DATE '2025-01-01', DATE '2025-12-31'),
    ('MEI_EMP',           0.67, DATE '2025-01-01', DATE '2025-12-31'),
    ('MEI_TRAB',          0.13, DATE '2025-01-01', DATE '2025-12-31'),
    ('HORAS_EXTRA_EMP',  23.60, DATE '2025-01-01', DATE '2025-12-31'),
    ('HORAS_EXTRA_TRAB',  4.70, DATE '2025-01-01', DATE '2025-12-31'),
    ('DESEMPLEO_EMP',     5.50, DATE '2025-01-01', DATE '2025-12-31'),
    ('DESEMPLEO_TRAB',    1.55, DATE '2025-01-01', DATE '2025-12-31'),
    ('FOGASA_EMP',        0.20, DATE '2025-01-01', DATE '2025-12-31'),
    ('FP_EMP',            0.60, DATE '2025-01-01', DATE '2025-12-31'),
    ('FP_TRAB',           0.10, DATE '2025-01-01', DATE '2025-12-31'),

    -- ── 2026 · Orden PJC/297/2026 ────────────────────────────────────────────
    ('CC_EMP',           23.60, DATE '2026-01-01', cast(null as date)),
    ('CC_TRAB',           4.70, DATE '2026-01-01', cast(null as date)),
    ('MEI_EMP',           0.75, DATE '2026-01-01', cast(null as date)),
    ('MEI_TRAB',          0.15, DATE '2026-01-01', cast(null as date)),
    ('HORAS_EXTRA_EMP',  23.60, DATE '2026-01-01', cast(null as date)),
    ('HORAS_EXTRA_TRAB',  4.70, DATE '2026-01-01', cast(null as date)),
    ('DESEMPLEO_EMP',     5.50, DATE '2026-01-01', cast(null as date)),
    ('DESEMPLEO_TRAB',    1.55, DATE '2026-01-01', cast(null as date)),
    ('FOGASA_EMP',        0.20, DATE '2026-01-01', cast(null as date)),
    ('FP_EMP',            0.60, DATE '2026-01-01', cast(null as date)),
    ('FP_TRAB',           0.10, DATE '2026-01-01', cast(null as date))
) as v(contingencia, tipo, desde, hasta);

-- ---------------------------------------------------------
-- 4. Lo que esta migracion NO hace, y por que
-- ---------------------------------------------------------
-- **El tipo de desempleo se siembra como si todo el mundo fuera indefinido.**
-- La Orden da dos: 7,05 (5,50 + 1,55) para la contratacion indefinida y 8,30
-- (6,70 + 1,60) para la de duracion determinada, y ademas manda la indefinida a
-- unos cuantos supuestos de contrato temporal -formacion, relevo, sustitucion,
-- discapacidad del 33 por ciento- (art. 33.2.a).1.o y 2.o de la Orden de 2026 y
-- sus equivalentes de 2024 y 2025).
--
-- Elegir entre las dos no es un dato con vigencia: es una condicion por
-- segmento, y el motor no sabe hoy leer el tipo de contrato para escoger fila.
-- Eso es el backend#124, y hasta entonces la demo -donde todos los contratos
-- son indefinidos- sale bien y cualquier temporal saldria barato. Queda escrito
-- aqui para que no parezca una decision.
--
-- **Tampoco se siembran los tipos de los sistemas especiales** -agrario (art.
-- 34), empleados de hogar (art. 35)- ni los de incapacidad temporal: la demo no
-- los usa, y sembrar filas que nadie lee es lo que dejo la tabla como estaba.
