-- =========================================================
-- V142__declare_the_cotizacion_rates_table_as_the_only_source.sql
-- Los tipos de cotizacion mandan desde aqui (backend#105)
-- =========================================================
--
-- La tabla estaba sembrada desde la V88 con nueve tipos y sus vigencias, y no
-- la leia nadie: los mismos nueve numeros vivian duplicados como constantes en
-- Java, que es de donde salian de verdad.
--
-- Datos inertes en el esquema es una forma que ya conociamos —el
-- result_composition_mode de la V90 que retiro la V120—, pero esto era peor:
-- no estaba solo inerte, tenia un gemelo vivo, y los dos podian divergir sin
-- que nada se enterara.
--
-- Gana la tabla. Y no por elegancia: la tabla YA tiene vigencias, asi que
-- leerla es lo unico que hace cierto que un tipo cambie a mitad de ano —lo que
-- en Espana pasa por ley cada ano— sin tocar el codigo.
--
-- Esta migracion no mueve ni un numero: los nueve son los mismos, y una
-- resiembra da el mismo recibo. Lo que cambia es de donde sale cada uno, y eso
-- vive en el codigo (SsCotizacionRateCalculator) y aqui.
--
-- Lo que NO entra en esta tabla es el IRPF, y esta dicho en el javadoc de
-- IrpfWithholdingRateCalculator: una retencion por tramos y situacion personal
-- no es un tipo por contingencia y por fecha. Va en
-- employee.employee_tax_information con el calculo que la lea, y eso es otro
-- issue.
comment on table payroll_engine.ss_cotizacion_tipos is
    'Los tipos de cotizacion por contingencia y por fecha, y la UNICA fuente: el motor los lee de aqui desde el backend#105. Antes estaban ademas como constantes en Java y las dos copias podian divergir. El IRPF no esta aqui a proposito.';

comment on column payroll_engine.ss_cotizacion_tipos.rate is
    'En tanto por ciento: 4.7000 es el 4,70 %.';

comment on column payroll_engine.ss_cotizacion_tipos.valid_to is
    'Nulo mientras siga vigente. Un cambio de tipos a mitad de ano se declara cerrando la fila vigente y anadiendo otra, sin tocar codigo.';
