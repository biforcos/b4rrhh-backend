-- =========================================================
-- V116__make_working_time_start_date_unique_per_employee.sql
-- Two working times of the same employee cannot start on the same day.
--
-- An occurrence of a temporal series is identified by the day it starts
-- (ADR-057): two of them starting on the same day are not representable in
-- the model, so the database says so too, whatever the application does
-- (backend#58). The non-unique index on the same columns becomes redundant
-- and is replaced by the unique constraint, which indexes them as well.
-- =========================================================

drop index if exists employee.idx_working_time_employee_start_date;

alter table employee.working_time
    add constraint uk_working_time_employee_start_date
    unique (employee_id, start_date);
