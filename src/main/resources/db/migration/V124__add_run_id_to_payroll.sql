-- =========================================================
-- V124__add_run_id_to_payroll.sql
-- El recibo dice que ejecucion lo produjo (ADR-059, backend#62)
-- =========================================================
--
-- payroll.payroll es de la V53 y payroll.calculation_run de la V55: el recibo
-- es anterior a la ejecucion y nunca se le anadio la referencia.
-- calculation_claim y calculation_run_message si la tienen; el resultado, no.
--
-- La columna queda nullable, y no es por los recibos que ya hay: esos se
-- rellenan aqui abajo y en la semilla de la demo salen todos. Es porque siguen
-- vivos dos caminos que crean recibo sin ejecucion registrada —el endpoint
-- temporal POST /payrolls/calculate y el recalculo puntual de un recibo—.
-- Mientras existan, run_id null significa «recibo sin ejecucion conocida», que
-- es exactamente lo que son. Cuando se retiren, la columna puede pasar a
-- not null sin tocar datos.

alter table payroll.payroll
    add column run_id bigint;

-- Sin on delete: la ejecucion que produjo un recibo no se borra mientras el
-- recibo exista. Ni cascade —borrar la ejecucion no puede borrar el resultado—
-- ni set null, que perderia justo la procedencia que esta columna guarda.
alter table payroll.payroll
    add constraint fk_payroll_run
    foreign key (run_id)
    references payroll.calculation_run(id);

-- Para «dame los recibos de esta ejecucion», que es la pregunta del
-- frontend#42 y la que necesita el lanzamiento asincrono del backend#75.
create index idx_payroll_run_id
    on payroll.payroll (run_id);

-- Relleno de los existentes. La ejecucion de un recibo es la que comparte
-- contexto y cuya ventana [started_at, finished_at] contiene su calculated_at.
-- Se coge la mas reciente de las que encajan, para que el resultado no dependa
-- del orden de la tabla si dos ventanas llegaran a solaparse.
--
-- En la semilla de la demo esto no deja ninguno a null: 871 recibos, 870 dentro
-- de la ventana de la ejecucion 1 (873 candidatos, 871 calculados) y uno solo
-- —EMP001000, presencia 2— dentro de la de la ejecucion 2, que lo recalculo.
update payroll.payroll p
set run_id = (
    select r.id
    from payroll.calculation_run r
    where r.rule_system_code = p.rule_system_code
      and r.payroll_period_code = p.payroll_period_code
      and r.payroll_type_code = p.payroll_type_code
      and r.started_at is not null
      and p.calculated_at >= r.started_at
      and (r.finished_at is null or p.calculated_at <= r.finished_at)
    order by r.started_at desc
    limit 1
)
where p.run_id is null;
