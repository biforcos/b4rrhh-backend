-- V179__a_presence_date_is_rectified_and_leaves_a_trail.sql
--
-- backend#172. La fecha de alta o de cese de una presencia se puede rectificar: el alta tecleada el
-- 01/10 que debía ser el 29/09. La rectificación mueve la presencia y lo que empezó (o acabó) con ella,
-- y deja rastro aquí: quién, cuándo, de qué fecha a cuál y qué movió. No hay otro histórico de la
-- presencia, que sólo guarda cómo es ahora.

create table employee.presence_rectification (
    id              bigint generated always as identity primary key,
    employee_id     bigint       not null references employee.employee (id) on delete cascade,
    presence_number integer      not null,
    field           varchar(10)  not null,
    old_date        date         not null,
    new_date        date         not null,
    rectified_by    varchar(100) not null,
    rectified_at    timestamp    not null default now(),
    moved           text         not null,
    constraint ck_presence_rectification_field check (field in ('START_DATE', 'END_DATE')),
    constraint ck_presence_rectification_changes check (old_date <> new_date)
);

create index idx_presence_rectification_employee on employee.presence_rectification (employee_id, presence_number);

comment on table employee.presence_rectification is
    'Cada rectificación de la fecha de alta o de cese de una presencia (backend#172): quién, cuándo, de qué fecha a cuál y qué movió con ella.';
comment on column employee.presence_rectification.moved is
    'Lo que se movió con la presencia, en palabras: «El contrato 100 del 01/10/2026 al 29/09/2026; …».';
