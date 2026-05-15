-- =========================================================
-- V102__seed_employee_absence_type_esp.sql
-- Seed ESP absence types for employee absence catalog
-- =========================================================

insert into rulesystem.rule_entity (
    rule_system_code,
    rule_entity_type_code,
    code,
    name,
    description,
    active,
    start_date
)
values
    ('ESP', 'EMPLOYEE_ABSENCE_TYPE', 'VACATION',            'Vacaciones',                                        'Employee vacation/annual leave',                                                true, DATE '1900-01-01'),
    ('ESP', 'EMPLOYEE_ABSENCE_TYPE', 'IT_COMMON',           'IT Contingencia Común',                             'Sick leave - common illness',                                                   true, DATE '1900-01-01'),
    ('ESP', 'EMPLOYEE_ABSENCE_TYPE', 'IT_WORK_ACCIDENT',    'IT Accidente de Trabajo / Enfermedad Profesional',  'Sick leave - work accident or occupational disease',                            true, DATE '1900-01-01'),
    ('ESP', 'EMPLOYEE_ABSENCE_TYPE', 'PARENTAL_LEAVE',      'Permiso de nacimiento / adopción',                  'Parental leave for birth or adoption',                                           true, DATE '1900-01-01'),
    ('ESP', 'EMPLOYEE_ABSENCE_TYPE', 'PAID_PERSONAL_LEAVE', 'Permiso retribuido',                                'Paid personal leave',                                                            true, DATE '1900-01-01'),
    ('ESP', 'EMPLOYEE_ABSENCE_TYPE', 'FORCE_MAJEURE',       'Permiso por fuerza mayor',                          'Force majeure leave',                                                            true, DATE '1900-01-01'),
    ('ESP', 'EMPLOYEE_ABSENCE_TYPE', 'UNPAID_LEAVE',        'Excedencia / Permiso no retribuido',                'Unpaid leave / sabbatical',                                                     true, DATE '1900-01-01')
on conflict (rule_system_code, rule_entity_type_code, code) do update
set
    name = excluded.name,
    description = excluded.description,
    active = excluded.active,
    start_date = excluded.start_date,
    updated_at = now();
