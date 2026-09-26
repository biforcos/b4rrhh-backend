-- =========================================================
-- V158__bind_the_absence_type_to_its_catalog.sql
-- El tipo de ausencia, atado a su catalogo (b4rrhh/frontend#84)
-- =========================================================
--
-- `employee.employee_absence` existe desde la V100 y sus tipos desde la V102,
-- pero el tipo NO estaba atado al catalogo en `resource_field_catalog_binding`.
-- Eso no se notaba porque nadie preguntaba: la ficha del empleado no tiene -no
-- tenia- seccion de ausencias, asi que nadie habia pedido nunca «dame las
-- opciones del tipo de ausencia».
--
-- Sin esta fila, la pantalla del frontend#84 tendria que traerse los siete tipos
-- por su cuenta, y entonces el desplegable de ausencias seria el unico de la
-- ficha que no sale del catalogo. La atadura es UNA FILA y lo evita: el servicio
-- generico de catalogos del frontend ya sabe resolver un DIRECT.
--
-- Del mismo tiron queda cubierta la fecha de vigencia: un tipo de ausencia que se
-- retire deja de ofrecerse sin tocar la pantalla, porque el que filtra por fecha
-- es el catalogo.
insert into rulesystem.resource_field_catalog_binding (
    resource_code,
    field_code,
    rule_entity_type_code,
    catalog_kind,
    depends_on_field_code,
    custom_resolver_code,
    active
)
values ('employee.absence', 'absenceTypeCode', 'EMPLOYEE_ABSENCE_TYPE', 'DIRECT', null, null, true)
on conflict (resource_code, field_code) do nothing;
