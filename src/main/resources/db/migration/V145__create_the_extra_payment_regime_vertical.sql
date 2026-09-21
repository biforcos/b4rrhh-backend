-- =========================================================
-- V145__create_the_extra_payment_regime_vertical.sql
-- La vertical de regimen de pagas extras (backend#118, paso 4 de workspace#9)
-- =========================================================
--
-- Si al empleado se le prorratean las pagas extras, y desde cuando.
--
-- ---------------------------------------------------------
-- Por que una vertical y no una columna en contract o employee
-- ---------------------------------------------------------
-- Porque el regimen cambia, y cuando cambia hay que poder explicar el recibo de
-- marzo. Sin vigencia no se puede cambiar de regimen sin reescribir la historia:
-- la columna diria «prorrateado» y el recibo de marzo, que se pago sin
-- prorratear, dejaria de poder explicarse.
--
-- Con vigencia, ademas, el cambio a mitad de mes parte el periodo como lo parte
-- la jornada (ADR-068), y la prorrata de cada tramo entra por la puerta que le
-- toca (backend#119).
--
-- ---------------------------------------------------------
-- La misma forma que employee.working_time
-- ---------------------------------------------------------
-- Numero funcional por empleado, vigencia con fin abierto, y un solo dato. Las
-- mismas restricciones: una ocurrencia de una serie temporal se identifica por
-- el dia en que empieza (ADR-057), asi que dos del mismo empleado no pueden
-- empezar el mismo dia y la base lo dice tambien, haga lo que haga la
-- aplicacion (backend#58, V116).
--
-- prorated no admite nulos: un empleado esta en un regimen o en el otro, y «no
-- se sabe» no es un regimen del que se pueda calcular una nomina.
create table employee.extra_payment_regime (
    id                          bigint generated always as identity primary key,
    employee_id                 bigint    not null,
    extra_payment_regime_number integer   not null,
    start_date                  date      not null,
    end_date                    date,
    prorated                    boolean   not null,
    created_at                  timestamp not null default now(),
    updated_at                  timestamp not null default now()
);

comment on table employee.extra_payment_regime is
    'Si al empleado se le prorratean las pagas extras, y desde cuando. Serie temporal de tipo A sobre la presencia (ADR-057, backend#118).';

alter table employee.extra_payment_regime
    add constraint fk_extra_payment_regime_employee
    foreign key (employee_id)
    references employee.employee(id);

alter table employee.extra_payment_regime
    add constraint uk_extra_payment_regime_number
    unique (employee_id, extra_payment_regime_number);

alter table employee.extra_payment_regime
    add constraint uk_extra_payment_regime_employee_start_date
    unique (employee_id, start_date);

alter table employee.extra_payment_regime
    add constraint chk_extra_payment_regime_dates
    check (end_date is null or start_date <= end_date);

-- ---------------------------------------------------------
-- Lo que esta migracion NO hace
-- ---------------------------------------------------------
-- No rellena nada. Los empleados que ya estan en la base se quedan sin fila, y
-- eso NO es un hueco de la serie: la serie empieza cuando hay una ocurrencia, y
-- la cobertura obligatoria se juzga sobre lo que hay. Quien los quiera con
-- regimen los siembra de nuevo (workforce-loader#13) o se lo da por el API.
--
-- Tampoco decide ningun recibo: quien lee esta vertical para calcular es el
-- backend#119.
