-- =========================================================
-- V103__drop_payroll_object_activation_table.sql
-- Drop payroll.payroll_object_activation
--
-- ADR-043 proposed this table as the per-context activation mechanism for
-- payroll concepts. ADR-045 replaced it with payroll_engine.concept_assignment
-- plus the execution plan, and nothing has read it since: the only readers
-- were the transitional CalculateBaseSalaryService / CalculateAgreementPlusService,
-- retired in issue #8.
--
-- The migrations that create and seed it (V62, V65, V68, V71, V72, V77, V88,
-- V91) are not edited: they are already applied. The table gets created,
-- filled and dropped here.
-- =========================================================

drop table if exists payroll.payroll_object_activation;
