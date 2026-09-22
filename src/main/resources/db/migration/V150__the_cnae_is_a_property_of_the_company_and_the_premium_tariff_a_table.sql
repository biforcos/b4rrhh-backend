-- =========================================================
-- V150__the_cnae_is_a_property_of_the_company_and_the_premium_tariff_a_table.sql
-- La cuota de accidentes de trabajo (backend#122)
-- =========================================================
--
-- No habia cuota empresarial por accidentes de trabajo y enfermedad profesional,
-- y no la habia porque faltaba el dato: `rulesystem.company_profile` tenia una
-- columna `epigrafe_at_code` a null en las cuatro empresas de la semilla, y no
-- habia ninguna tabla de tarifa de primas detras. Es la deuda 4 del ADR-048,
-- aplazada explicitamente desde la V88.
--
-- Ahora si la hay, porque el backend#121 dejo la base sobre la que se cotiza:
-- la de contingencias profesionales.
--
-- ---------------------------------------------------------
-- 1. El CNAE es una propiedad de la empresa, y se llama CNAE
-- ---------------------------------------------------------
-- `epigrafe_at_code` se renombra a `cnae_code`, sin mantener el nombre viejo.
-- El «epigrafe» es la tarifa de primas anterior a 2007 y hoy no significa nada:
-- lo que la Seguridad Social usa para cotizar por accidentes de trabajo desde
-- entonces es el codigo CNAE de la actividad economica.
--
-- Sin alias y sin columna de compatibilidad a proposito: la columna estaba a
-- null en las cuatro filas que hay, asi que no hay nada que migrar y no hay
-- ningun cliente leyendola. Dejar el nombre viejo vivo «por si acaso» seria
-- crear la divergencia que el backend#80 acaba de cerrar en el contrato.
alter table rulesystem.company_profile
    rename column epigrafe_at_code to cnae_code;

comment on column rulesystem.company_profile.cnae_code is
    'La actividad economica de la empresa, en CNAE. De aqui sale el tipo de la cuota de accidentes de trabajo y enfermedad profesional, buscando en payroll_engine.ss_tarifa_primas_at la entrada mas especifica que lo cubra (backend#122).';

-- Las cuatro empresas de la semilla: comercio al por menor en establecimientos
-- no especializados con predominio en productos distintos de los alimenticios,
-- que es el CNAE de los grandes almacenes y el convenio que la demo usa
-- («Convenio colectivo del sector de grandes almacenes»).
--
-- Se siembra aqui y no a mano en la base. El loader no crea empresas -solo da de
-- alta empleados en las que ya existen-, asi que la unica via por la que este
-- dato llega a una base nueva es la migracion que crea las empresas, y esa es
-- la V51.
update rulesystem.company_profile
   set cnae_code  = '4719',
       updated_at = current_timestamp
 where cnae_code is null;

-- ---------------------------------------------------------
-- 2. La tarifa de primas, como tabla y con vigencia
-- ---------------------------------------------------------
-- Con la forma de `ss_cotizacion_tipos`, que es el hermano que ya existe: lo que
-- manda en una fecha se declara, no se compila (backend#105).
--
-- LAS CITAS, que son dos porque la tarifa cambio de sitio:
--
--   Hasta el 31 de diciembre de 2025: disposicion adicional CUARTA de la Ley
--   42/2006, de 28 de diciembre, de Presupuestos Generales del Estado para 2007,
--   en su redaccion vigente. Es la que el issue nombra.
--
--   Desde el 1 de enero de 2026: disposicion adicional SEXAGESIMA PRIMERA del
--   texto refundido de la Ley General de la Seguridad Social, en la redaccion
--   dada por la disposicion final primera del Real Decreto-ley 16/2025, de 23 de
--   diciembre (BOE de 24 de diciembre de 2025). La anterior quedo derogada al
--   aprobarse la CNAE-2025 y trasladarse la tarifa a la LGSS.
--
-- Se siembran las dos filas y no solo la vigente, y no es por completismo: el
-- periodo que calcula la demo es 202609 —septiembre de 2026, la segunda— y los
-- tests de la suite calculan 202504 —abril de 2025, la primera—. Sembrar solo
-- una dejaria a la otra sin tarifa, y una corrida sin tarifa falla. Ademas es lo
-- que la tabla existe para poder decir: que cambie la norma no es un cambio de
-- codigo.
--
-- El tipo de la actividad 47 NO se movio entre las dos: 0,95 de IT y 0,70 de
-- IMS en las dos. Lo que cambia es el texto de la actividad, porque la CNAE-2025
-- renumero las excepciones. Que los numeros coincidan es una casualidad util:
-- la resiembra del deploy#20 no depende de cual de las dos filas se lea.
--
-- La fecha de inicio de la primera fila es 2025-01-01 y no la de entrada en
-- vigor real de aquella tarifa (2019): es la que usan todas las demas filas de
-- catalogo de este sistema de reglas —los tipos de la V88, los topes—, y aqui se
-- sigue esa convencion en vez de estrenar otra.
--
-- LA FORMA. La tarifa no lista todos los CNAE: lista entradas de dos, tres o
-- cuatro digitos y **la mas especifica gana**. «47 Comercio al por menor
-- (excepto 4773, 4781, 4782 y 4783)» es exactamente eso, y por eso la busqueda
-- es por prefijo mas largo y no por igualdad. Un CNAE de cuatro digitos como el
-- 4719 encuentra su tipo en la entrada de dos.
--
-- No hace falta la tarifa entera para la demo, y no se siembra entera: entra la
-- entrada que la semilla usa. La tabla puede tener todas las demas sin tocar ni
-- una linea de codigo, que es lo que se pedia.
create table payroll_engine.ss_tarifa_primas_at (
    id               bigserial     primary key,
    rule_system_code varchar(10)   not null,
    cnae_code        varchar(10)   not null,
    activity_name    varchar(300)  not null,
    tipo_it          numeric(6,4)  not null,
    tipo_ims         numeric(6,4)  not null,
    valid_from       date          not null,
    valid_to         date,
    constraint uq_ss_tarifa_primas_at unique (rule_system_code, cnae_code, valid_from)
);

comment on table payroll_engine.ss_tarifa_primas_at is
    'La tarifa de primas para la cotizacion por accidentes de trabajo y enfermedades profesionales: un tipo de IT y otro de IMS por actividad economica y por fecha. Las entradas son de dos, tres o cuatro digitos de CNAE y gana la mas especifica que cubra al CNAE de la empresa (backend#122).';

comment on column payroll_engine.ss_tarifa_primas_at.tipo_it is
    'En tanto por ciento: 0.9500 es el 0,95 %. Incapacidad temporal.';

comment on column payroll_engine.ss_tarifa_primas_at.tipo_ims is
    'En tanto por ciento. Incapacidad permanente, muerte y supervivencia.';

comment on column payroll_engine.ss_tarifa_primas_at.valid_to is
    'Nulo mientras siga vigente. Una tarifa nueva se declara cerrando la fila vigente y anadiendo otra, sin tocar codigo.';

insert into payroll_engine.ss_tarifa_primas_at
    (rule_system_code, cnae_code, activity_name, tipo_it, tipo_ims, valid_from, valid_to)
values
    -- Disposicion adicional cuarta de la Ley 42/2006, vigente hasta el 31/12/2025
    ('ESP', '47', 'Comercio al por menor, excepto de vehiculos de motor y motocicletas (excepto 473)',
     0.95, 0.70, DATE '2025-01-01', DATE '2025-12-31'),
    -- Disposicion adicional sexagesima primera de la LGSS, desde el 01/01/2026
    ('ESP', '47', 'Comercio al por menor (excepto 4773, 4781, 4782 y 4783)',
     0.95, 0.70, DATE '2026-01-01', null);

-- ---------------------------------------------------------
-- 3. El concepto
-- ---------------------------------------------------------
-- 727, detras del 726 y delante del total. Como las otras cinco de empresa:
-- INFORMATIONAL, suma en el 725 y no descuenta del liquido.
--
-- El tipo es uno solo y es la SUMA de los dos de la tarifa. La alternativa era
-- dos lineas -una de IT y otra de IMS- y no es lo que el modelo oficial imprime:
-- el recibo ensena una linea de accidentes de trabajo. Los dos tipos se guardan
-- por separado en la tabla porque la norma los declara por separado y porque la
-- cobertura de IT se puede tener concertada con una mutua distinta que la de
-- IMS; lo que se suma es lo que se paga.
insert into payroll_engine.payroll_object (rule_system_code, object_type_code, object_code)
select 'ESP', 'CONCEPT', v.codigo
from (values ('P_AT_EP'), ('727')) as v(codigo)
where not exists (
    select 1 from payroll_engine.payroll_object o
     where o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
);

insert into payroll_engine.payroll_concept (
    object_id, concept_mnemonic, calculation_type, functional_nature,
    payslip_order_code, execution_scope, rounding_scale, rounding_mode
)
select o.id, v.mnemonico, v.tipo, v.naturaleza, v.orden, 'PERIOD', 2, 'HALF_UP'
from (values
        ('P_AT_EP', 'TIPO_AT_EP_EMPRESARIO', 'ENGINE_PROVIDED', 'TECHNICAL',     cast(null as varchar)),
        ('727',     'AT_EP_EMPRESARIO',      'PERCENTAGE',      'INFORMATIONAL', '727')
     ) as v(codigo, mnemonico, tipo, naturaleza, orden)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
where not exists (
    select 1 from payroll_engine.payroll_concept c where c.object_id = o.id
);

-- Sobre la base de contingencias PROFESIONALES, que es la que el modelo oficial
-- llama «profesionales y recaudacion conjunta» precisamente porque esta es una
-- de ellas (backend#121).
insert into payroll_engine.payroll_concept_operand (target_object_id, operand_role, source_object_id)
select destino.id, v.rol, origen.id
from (values ('727', 'BASE', 'B_CP'), ('727', 'PERCENTAGE', 'P_AT_EP')) as v(destino, rol, origen)
join payroll_engine.payroll_object destino
  on destino.rule_system_code = 'ESP' and destino.object_type_code = 'CONCEPT' and destino.object_code = v.destino
join payroll_engine.payroll_object origen
  on origen.rule_system_code = 'ESP' and origen.object_type_code = 'CONCEPT' and origen.object_code = v.origen
where not exists (
    select 1 from payroll_engine.payroll_concept_operand x
     where x.target_object_id = destino.id and x.operand_role = v.rol
);

insert into payroll_engine.payroll_concept_feed_relation (
    source_object_id, target_object_id, feed_mode, feed_value, invert_sign, effective_from, effective_to
)
select origen.id, destino.id, 'FEED_BY_SOURCE', cast(null as numeric), false, DATE '2025-01-01', cast(null as date)
from payroll_engine.payroll_object origen, payroll_engine.payroll_object destino
where origen.rule_system_code = 'ESP' and origen.object_type_code = 'CONCEPT' and origen.object_code = '727'
  and destino.rule_system_code = 'ESP' and destino.object_type_code = 'CONCEPT' and destino.object_code = '725'
  and not exists (
    select 1 from payroll_engine.payroll_concept_feed_relation x
     where x.source_object_id = origen.id and x.target_object_id = destino.id
);

insert into payroll_engine.payroll_concept_label (object_id, language_code, label)
select o.id, 'es', v.label
from (values
        ('727',     'Accidentes de trabajo y enfermedades profesionales'),
        ('P_AT_EP', 'Tipo de accidentes de trabajo y enfermedades profesionales')
     ) as v(codigo, label)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
where not exists (
    select 1 from payroll_engine.payroll_concept_label l
     where l.object_id = o.id and l.language_code = 'es'
);

-- El tipo entra solo, como operando. El 727 no.
insert into payroll_engine.concept_assignment
    (rule_system_code, concept_code, company_code, agreement_code, employee_type_code,
     valid_from, valid_to, priority)
select 'ESP', '727', null, '99002405011982', null, DATE '2025-01-01', null, 727
where not exists (
    select 1 from payroll_engine.concept_assignment a
     where a.rule_system_code = 'ESP' and a.concept_code = '727'
);
