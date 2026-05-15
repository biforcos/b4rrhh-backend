-- =========================================================
-- V100__create_employee_absence_table.sql
-- Create employee.employee_absence table for absence tracking
-- =========================================================

create table employee.employee_absence (
    id                bigint generated always as identity primary key,
    employee_id       bigint not null references employee.employee(id) on delete cascade,
    absence_type_code varchar(50) not null,
    start_date        date not null,
    start_time        integer not null default 0,
    end_date          date,
    end_time          integer,
    created_at        timestamp not null,
    updated_at        timestamp not null,
    constraint uk_employee_absence_business_key
        unique (employee_id, absence_type_code, start_date, start_time),
    constraint chk_absence_start_time check (start_time >= 0 and start_time <= 1439),
    constraint chk_absence_end_time   check (end_time is null or (end_time >= 0 and end_time <= 1439)),
    constraint chk_absence_end_date   check (end_date is null or end_date >= start_date)
);

create index idx_employee_absence_employee_start_date
    on employee.employee_absence (employee_id, start_date);
