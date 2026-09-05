-- =========================================================
-- V120__drop_result_composition_mode_and_persist_to_concepts.sql
-- Retirar result_composition_mode y persist_to_concepts de payroll_concept (ADR-058, backend#60)
-- =========================================================
--
-- Ninguna de las dos columnas gobernaba nada, y desde backend#64 tampoco
-- documentan una intencion:
--
--   result_composition_mode  El motor compone siempre por suma. Una tasa
--                            SEGMENT no puede ser operando de un concepto
--                            PERIOD (backend#63), asi que nunca hay que
--                            componerla; una magnitud SEGMENT solo se compone
--                            para el recibo o para un feed, y las dos son
--                            suma. La composicion deja de ser una eleccion
--                            del concepto (V56 la creo, V90 la puso en
--                            ACCUMULATE en los topes sin efecto alguno).
--
--   persist_to_concepts      Existia para no llenar la tabla de resultados
--                            (V80). Son 36 filas por nomina, unas 31.000
--                            para los mil empleados sembrados; y para poder
--                            explicar un importe hay que persistirlo todo.

alter table payroll_engine.payroll_concept
    drop column result_composition_mode,
    drop column persist_to_concepts;
