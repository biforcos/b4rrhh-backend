-- =========================================================
-- La huella de las tablas de negocio (backend#156, camino 5 de workspace#20)
-- =========================================================
--
--   Un md5 por tabla. Dos bases con la misma huella tienen los mismos empleados,
--   la misma historia y los mismos recibos, celda a celda.
--
-- Es el oraculo del camino 5 (ADR-077): cada paso termina recalculando la semilla
-- entera con el backend nuevo y comparando esta huella con la de la semilla de
-- referencia. «Cero recibos se mueven» es que las catorce salen iguales.
--
-- Eran once. backend#159 anadio direccion, documentos y contactos: son lo que leen
-- los catalogos que el backend#158 subio a COM e INT (paises, tipos de documento,
-- de contacto y de direccion), y la huella no los miraba. Por eso el fichero se
-- llamaba huella-de-las-once-tablas.sql.
--
-- ---------------------------------------------------------
-- Como se usa
-- ---------------------------------------------------------
--   docker exec -i b4rrhh-postgres psql -U b4rrhh -d b4rrhh_semilla -At \
--       < docs/consultas/huella-de-las-tablas-de-negocio.sql
--
-- Y lo mismo contra la otra base; un diff de las dos salidas vacio es el criterio.
--
-- ---------------------------------------------------------
-- Que se deja fuera, y por que
-- ---------------------------------------------------------
-- Lo que cambia de una corrida a otra sin que cambie nada de negocio:
--   - los id surrogados, y las FK por id (employee_id, payroll_id): se sustituyen
--     por la clave de negocio de la fila a la que apuntan;
--   - las marcas de tiempo (created_at, updated_at, calculated_at) y el run_id.
-- Todo lo demas entra, incluidos los nulos (concat_ws los saltaria; por eso cada
-- columna pasa por coalesce a un literal que no puede ser un valor).
--
-- Las filas se ordenan por su propio texto, asi que el orden de insercion tampoco
-- cuenta.
-- =========================================================

with
emp as (
    select id, rule_system_code || '/' || employee_type_code || '/' || employee_number as k
      from employee.employee
),
nom as (
    select id, rule_system_code || '/' || employee_type_code || '/' || employee_number
               || '/' || presence_number || '/' || payroll_period_code || '/' || payroll_type_code as k
      from payroll.payroll
),
filas(tabla, fila) as (
    select 'employee.employee', concat_ws('|', rule_system_code, employee_type_code, employee_number,
           first_name, last_name_1, coalesce(last_name_2, '∅'), coalesce(preferred_name, '∅'),
           coalesce(photo_url, '∅'))
      from employee.employee
    union all
    select 'employee.address', concat_ws('|', emp.k, address_number, address_type_code, street, city,
           country_code, coalesce(postal_code, '∅'), coalesce(region_code, '∅'),
           start_date, coalesce(end_date::text, '∅'))
      from employee.address x join emp on emp.id = x.employee_id
    union all
    select 'employee.identifier', concat_ws('|', emp.k, identifier_type_code, identifier_value,
           coalesce(issuing_country_code, '∅'), coalesce(expiration_date::text, '∅'), is_primary)
      from employee.identifier x join emp on emp.id = x.employee_id
    union all
    select 'employee.contact', concat_ws('|', emp.k, contact_type_code, contact_value)
      from employee.contact x join emp on emp.id = x.employee_id
    union all
    select 'employee.presence', concat_ws('|', emp.k, presence_number, company_code, entry_reason_code,
           coalesce(exit_reason_code, '∅'), start_date, coalesce(end_date::text, '∅'))
      from employee.presence x join emp on emp.id = x.employee_id
    union all
    select 'employee.contract', concat_ws('|', emp.k, contract_code, coalesce(contract_subtype_code, '∅'),
           start_date, coalesce(end_date::text, '∅'))
      from employee.contract x join emp on emp.id = x.employee_id
    union all
    select 'employee.labor_classification', concat_ws('|', emp.k, agreement_code, agreement_category_code,
           start_date, coalesce(end_date::text, '∅'))
      from employee.labor_classification x join emp on emp.id = x.employee_id
    union all
    select 'employee.working_time', concat_ws('|', emp.k, working_time_number, start_date,
           coalesce(end_date::text, '∅'), working_time_percentage, coalesce(weekly_hours::text, '∅'),
           coalesce(daily_hours::text, '∅'), coalesce(monthly_hours::text, '∅'))
      from employee.working_time x join emp on emp.id = x.employee_id
    union all
    select 'employee.employee_absence', concat_ws('|', emp.k, absence_type_code, start_date,
           coalesce(start_time::text, '∅'), coalesce(end_date::text, '∅'), coalesce(end_time::text, '∅'),
           coalesce(benefit_entitled::text, '∅'))
      from employee.employee_absence x join emp on emp.id = x.employee_id
    union all
    select 'employee.employee_payroll_input', concat_ws('|', rule_system_code, employee_type_code,
           employee_number, concept_code, period, quantity)
      from employee.employee_payroll_input
    union all
    select 'payroll.payroll', concat_ws('|', nom.k, status, coalesce(status_reason_code, '∅'),
           calculation_engine_code, calculation_engine_version)
      from payroll.payroll x join nom on nom.id = x.id
    union all
    select 'payroll.payroll_concept', concat_ws('|', nom.k, line_number, concept_code,
           coalesce(concept_label, '∅'), amount, coalesce(quantity::text, '∅'), coalesce(rate::text, '∅'),
           coalesce(concept_nature_code, '∅'), coalesce(origin_period_code, '∅'),
           coalesce(display_order::text, '∅'), coalesce(merged_step_count::text, '∅'),
           coalesce(concept_mnemonic, '∅'), coalesce(payslip_section_code, '∅'),
           coalesce(payslip_subsection_code, '∅'))
      from payroll.payroll_concept x join nom on nom.id = x.payroll_id
    union all
    select 'payroll.payroll_segment', concat_ws('|', nom.k, segment_start)
      from payroll.payroll_segment x join nom on nom.id = x.payroll_id
    union all
    select 'payroll.payroll_warning', concat_ws('|', nom.k, warning_code, severity_code,
           coalesce(message, '∅'), coalesce(details_json::text, '∅'))
      from payroll.payroll_warning x join nom on nom.id = x.payroll_id
)
select tabla, count(*) as filas, md5(string_agg(fila, E'\n' order by fila)) as huella
  from filas
 group by tabla
 order by tabla;
