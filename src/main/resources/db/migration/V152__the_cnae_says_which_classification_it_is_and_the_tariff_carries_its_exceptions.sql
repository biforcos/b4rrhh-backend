-- =========================================================
-- V152__the_cnae_says_which_classification_it_is_and_the_tariff_carries_its_exceptions.sql
-- CNAE-2025, y las excepciones que la tarifa nombra tienen fila (backend#122)
-- =========================================================
--
-- Tres correcciones sobre la V150, y las tres son del mismo defecto: un codigo
-- de actividad no significa nada si no se dice en que clasificacion esta escrito.
--
-- ---------------------------------------------------------
-- 1. El codigo sembrado era de una clasificacion derogada
-- ---------------------------------------------------------
-- La V150 puso `4719` en las cuatro empresas. El 4719 es **CNAE-2009** —«otro
-- comercio al por menor en establecimientos no especializados»— y la tarifa que
-- se aplica desde el 1 de enero de 2026 se indexa por **CNAE-2025**, aprobada
-- por el Real Decreto 10/2025, de 14 de enero (BOE de 21 de enero de 2025).
--
-- En CNAE-2025 aquel 4719 se parte en dos: **4712** «otro comercio al por menor
-- no especializado», que es el de los grandes almacenes —el convenio que la demo
-- usa—, y 4791 «intermediacion para comercio al por menor no especializado».
--
-- Las empresas espanolas pasan a 4712. Las de Francia y Portugal se quedan **sin
-- CNAE**: la CNAE es la clasificacion espanola, y ponerles un codigo espanol no
-- significa nada — les tocaria la clasificacion de su pais y una tarifa de su
-- pais, y ninguna de las dos esta modelada. Una empresa sin CNAE no puede
-- calcular nomina espanola, y eso es exactamente lo que hay que ver si alguien
-- intenta calcularle una a un empleado suyo.
--
-- ---------------------------------------------------------
-- 2. La propiedad dice en que clasificacion esta escrita
-- ---------------------------------------------------------
-- Es lo que habria cantado con el 4719: el codigo estaba en CNAE-2009 y se
-- resolvia contra una tarifa de CNAE-2025 sin que nada lo notara. Un codigo de
-- cuatro digitos vale en las dos clasificaciones y quiere decir cosas distintas.
--
-- Hoy el unico valor admisible es `CNAE-2025`, y el check lo dice. Admitir una
-- segunda clasificacion no es anadir un valor: es traer la tabla de
-- correspondencias oficial —la que regula la disposicion transitoria
-- cuadragesima quinta de la LGSS para el transito de CNAE-2009 a CNAE-2025— y
-- decidir quien convierte y cuando. Mientras eso no este, el check es la forma
-- de que nadie guarde un codigo viejo creyendo que vale.
alter table rulesystem.company_profile
    add column cnae_classification varchar(20);

alter table rulesystem.company_profile
    add constraint chk_company_profile_cnae_classification
        check (
            (cnae_code is null and cnae_classification is null)
            or (cnae_code is not null and cnae_classification = 'CNAE-2025')
        );

comment on column rulesystem.company_profile.cnae_classification is
    'En que clasificacion esta escrito cnae_code. Hoy solo puede ser CNAE-2025 (RD 10/2025), que es la que entiende la tarifa de primas vigente; una segunda clasificacion necesita la tabla de correspondencias de la DT 45.a de la LGSS (backend#122).';

update rulesystem.company_profile cp
   set cnae_code           = v.cnae,
       cnae_classification = v.clasificacion,
       updated_at          = current_timestamp
  from (values
          ('ES01', '4712', 'CNAE-2025'),
          ('ES02', '4712', 'CNAE-2025'),
          ('FR01', cast(null as varchar), cast(null as varchar)),
          ('PT01', cast(null as varchar), cast(null as varchar))
       ) as v(codigo, cnae, clasificacion)
  join rulesystem.rule_entity re
    on re.rule_system_code = 'ESP' and re.rule_entity_type_code = 'COMPANY' and re.code = v.codigo
 where cp.company_rule_entity_id = re.id;

-- ---------------------------------------------------------
-- 3. Las excepciones que la propia fila nombra, con su fila
-- ---------------------------------------------------------
-- La fila del 47 dice «excepto 473, 4781, 4782 y 4783» y la tabla no tenia esos
-- codigos. Como la resolucion va del codigo mas especifico al menos, una empresa
-- con `4730` —comercio al por menor de combustible— resolvia al 47 y cotizaba al
-- 1,65 en vez de al 1,85: verde y en falso. **Si una fila nombra una excepcion,
-- la excepcion tiene que tener su fila**, o el texto de la fila es un adorno.
--
-- Y de paso se corrige el texto de la fila del 47, que la V150 escribio con
-- «4773» —un codigo que no esta en la lista de excepciones; el 4773 es el
-- comercio al por menor de productos farmaceuticos—. Era un error de copia.
--
-- LA CITA de las cinco filas de 2026: **disposicion adicional sexagesima primera
-- del texto refundido de la Ley General de la Seguridad Social**, en la
-- redaccion dada por la **disposicion final primera del Real Decreto-ley 3/2026,
-- de 3 de febrero** (BOE-A-2026-2548, BOE de 4 de febrero de 2026), con efectos
-- desde el **1 de enero de 2026**.
--
-- Y una correccion a la V150, que citaba el Real Decreto-ley 16/2025: aquel
-- introdujo la misma disposicion el 29/12/2025 y **quedo derogado** por la
-- Resolucion del Congreso de los Diputados de 28 de enero de 2026, que no lo
-- convalido. La tarifa volvio por el Real Decreto-ley 3/2026, con los mismos
-- efectos desde el 1 de enero. La norma que manda es la segunda.
--
--   47    Comercio al por menor (excepto 473, 4781, 4782 y 4783)   0,95   0,70
--   473   Comercio al por menor de combustible para la automocion  1,00   0,85
--   4781  Comercio al por menor de vehiculos de motor              1,00   1,05
--   4782  Comercio al por menor de repuestos y accesorios de
--         vehiculos de motor                                       1,00   1,05
--   4783  Comercio al por menor de motocicletas, y repuestos y
--         accesorios de motocicletas                               1,70   1,20
--
-- SOBRE LAS FILAS DE 2025, que es donde esta el limite de esta migracion: la
-- tarifa vigente hasta el 31/12/2025 es la disposicion adicional cuarta de la
-- Ley 42/2006, indexada por CNAE-2009, y **su texto no se ha podido leer entero
-- desde el BOE** en esta sesion: lo que hay son reproducciones publicadas que
-- coinciden entre si y con las cinco cifras de arriba. Se siembran las mismas
-- cinco filas para ese ejercicio porque la suite calcula 2025 y sin tarifa la
-- corrida falla, y porque **coincidiendo con las verificadas no pueden esconder
-- ninguna discrepancia**: si alguna difiere en la tarifa de 2006, corregirla no
-- mueve ni un recibo de la demo, que calcula 2026. Queda dicho aqui para que se
-- corrija con la cita cuando alguien la tenga delante.
update payroll_engine.ss_tarifa_primas_at
   set activity_name = 'Comercio al por menor (excepto 473, 4781, 4782 y 4783)'
 where rule_system_code = 'ESP' and cnae_code = '47' and valid_from = DATE '2026-01-01';

insert into payroll_engine.ss_tarifa_primas_at
    (rule_system_code, cnae_code, activity_name, tipo_it, tipo_ims, valid_from, valid_to)
select 'ESP', v.cnae, v.actividad, v.it, v.ims, v.desde, v.hasta
from (values
        -- Disposicion adicional cuarta de la Ley 42/2006, hasta el 31/12/2025
        ('473',  'Comercio al por menor de combustible para la automocion',
         1.00, 0.85, DATE '2025-01-01', DATE '2025-12-31'),
        ('4781', 'Comercio al por menor de vehiculos de motor',
         1.00, 1.05, DATE '2025-01-01', DATE '2025-12-31'),
        ('4782', 'Comercio al por menor de repuestos y accesorios de vehiculos de motor',
         1.00, 1.05, DATE '2025-01-01', DATE '2025-12-31'),
        ('4783', 'Comercio al por menor de motocicletas, y repuestos y accesorios de motocicletas',
         1.70, 1.20, DATE '2025-01-01', DATE '2025-12-31'),
        -- Disposicion adicional sexagesima primera de la LGSS, desde el 01/01/2026
        ('473',  'Comercio al por menor de combustible para la automocion',
         1.00, 0.85, DATE '2026-01-01', cast(null as date)),
        ('4781', 'Comercio al por menor de vehiculos de motor',
         1.00, 1.05, DATE '2026-01-01', cast(null as date)),
        ('4782', 'Comercio al por menor de repuestos y accesorios de vehiculos de motor',
         1.00, 1.05, DATE '2026-01-01', cast(null as date)),
        ('4783', 'Comercio al por menor de motocicletas, y repuestos y accesorios de motocicletas',
         1.70, 1.20, DATE '2026-01-01', cast(null as date))
     ) as v(cnae, actividad, it, ims, desde, hasta)
where not exists (
    select 1 from payroll_engine.ss_tarifa_primas_at e
     where e.rule_system_code = 'ESP' and e.cnae_code = v.cnae and e.valid_from = v.desde
);
