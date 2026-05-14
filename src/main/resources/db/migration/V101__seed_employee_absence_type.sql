-- =========================================================
-- V101__seed_employee_absence_type.sql
-- Seed rule_entity_type for employee absence
-- =========================================================

insert into rulesystem.rule_entity_type (
    code,
    name,
    active
)
values
    ('EMPLOYEE_ABSENCE_TYPE', 'Employee Absence Type', true)
on conflict (code) do update
set
    name = excluded.name,
    active = excluded.active,
    updated_at = now();
