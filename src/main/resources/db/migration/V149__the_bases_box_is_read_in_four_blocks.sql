-- =========================================================
-- V149__the_bases_box_is_read_in_four_blocks.sql
-- El recuadro de bases, en los cuatro bloques del modelo oficial (backend#121)
-- =========================================================
--
-- La V148 dejo las tres bases calculadas y sus diez lineas con orden de folio.
-- Lo que falta es que el papel las agrupe: el modelo oficial no tiene diez
-- lineas seguidas bajo «Determinacion de las bases de cotizacion», tiene cuatro
-- bloques numerados y cada uno se lee de arriba abajo.
--
--   1. Contingencias comunes
--      Remuneracion mensual                          1.850,10
--      Prorrata de pagas extraordinarias               616,70
--      Base de cotizacion                            2.466,80
--      Base tras topes                               2.466,80
--   2. Contingencias profesionales y recaudacion conjunta
--      Base de contingencias comunes                 2.466,80
--      Horas extraordinarias                           138,78
--      Base de cotizacion                            2.605,58
--      Base tras topes                               2.605,58
--   3. Horas extraordinarias
--      Base                                            138,78
--   4. Base sujeta a retencion del IRPF
--      Base                                          1.988,88
--
-- ---------------------------------------------------------
-- Por que una subseccion y no cuatro secciones
-- ---------------------------------------------------------
-- Porque el recuadro es UNO. «Determinacion de las bases de cotizacion» es un
-- bloque del modelo oficial con cuatro partes dentro, y partirlo en cuatro
-- bloques hermanos haria desaparecer su titulo del papel: el recibo pasaria de
-- cinco bloques a ocho y ninguno se llamaria ya como el modelo.
--
-- Y porque el sitio de la agrupacion no cambia: sigue estando declarado en el
-- catalogo y no escrito dentro del dibujo. Un renderizador que supiera que hay
-- cuatro bloques de bases seria el mismo error que la V138 retiro con las cinco
-- secciones, un nivel mas abajo.
--
-- ---------------------------------------------------------
-- Por que cuelga del concepto y la seccion de la naturaleza
-- ---------------------------------------------------------
-- No es una incoherencia: son dos preguntas distintas.
--
--   La SECCION dice de que CLASE es la linea —un devengo, una deduccion, una
--   base— y eso ya lo dice functional_nature. Atarla al concepto obligaria a
--   repetir la misma decision en los 65 conceptos del catalogo (V138).
--
--   La SUBSECCION dice A QUE BASE pertenece la linea, y eso NO se deduce de la
--   naturaleza: los diez son BASE y van en cuatro bloques distintos. Es una
--   propiedad del concepto, y por eso vive en el concepto.
--
-- Nula es el caso normal: una linea sin subseccion se imprime en su bloque, sin
-- nada por encima, que es como se imprimen hoy los devengos y las deducciones.
--
-- ---------------------------------------------------------
-- Lo que esta migracion NO hace
-- ---------------------------------------------------------
-- No rellena la subseccion de las lineas ya calculadas. La V138 si derivo la
-- seccion de lo que cada linea traia congelado —la naturaleza— y eso era
-- terminar de escribir lo que el documento ya decia. Aqui no hay de donde
-- derivarla: habria que ir al catalogo de HOY a preguntar por el concepto, y un
-- recibo viejo no tiene por que haberse calculado con el catalogo de hoy. Las
-- lineas anteriores se imprimen como antes —seguidas, dentro del recuadro— y
-- las que salgan de la proxima corrida, en sus cuatro bloques.

-- ---------------------------------------------------------
-- 1. Las subsecciones
-- ---------------------------------------------------------
create table payroll_engine.payslip_subsection (
    rule_system_code varchar(10)  not null,
    section_code     varchar(30)  not null,
    subsection_code  varchar(30)  not null,
    subsection_label varchar(200) not null,
    display_order    integer      not null,
    created_at       timestamp    not null default now(),
    updated_at       timestamp    not null default now(),
    constraint pk_payslip_subsection primary key (rule_system_code, subsection_code),
    constraint fk_payslip_subsection_section
        foreign key (rule_system_code, section_code)
        references payroll_engine.payslip_section (rule_system_code, section_code)
        on delete cascade,
    constraint uk_payslip_subsection_display_order
        unique (rule_system_code, section_code, display_order)
);

comment on table payroll_engine.payslip_subsection is
    'Las partes de un bloque del recibo: los cuatro apartados numerados del recuadro de bases del modelo oficial. Declaradas, no dibujadas (backend#121).';

-- La clave primaria es (sistema, subseccion) y no incluye la seccion a
-- proposito: una subseccion esta en UN bloque, igual que una naturaleza esta en
-- UNA seccion (V138). Si el codigo pudiera repetirse en dos bloques, la
-- pregunta «donde va esta linea» volveria a tener dos respuestas.

insert into payroll_engine.payslip_subsection
    (rule_system_code, section_code, subsection_code, subsection_label, display_order)
values
    ('ESP', 'BASES', 'BASE_CC',    '1. Contingencias comunes',                             10),
    ('ESP', 'BASES', 'BASE_CP',    '2. Contingencias profesionales y recaudacion conjunta', 20),
    ('ESP', 'BASES', 'BASE_HE',    '3. Horas extraordinarias',                             30),
    ('ESP', 'BASES', 'BASE_IRPF',  '4. Base sujeta a retencion del IRPF',                  40);

-- ---------------------------------------------------------
-- 2. Que concepto va en que parte
-- ---------------------------------------------------------
alter table payroll_engine.payroll_concept
    add column payslip_subsection_code varchar(30);

-- Sin clave ajena, y no por descuido: payroll_concept no lleva el sistema de
-- reglas —lo lleva payroll_object— y la clave de payslip_subsection es
-- (sistema, subseccion). Una ajena solo por el codigo de subseccion pediria
-- hacerlo unico entre sistemas de reglas, que es exactamente lo que no es. Es la
-- misma situacion que payslip_order_code, que tampoco la tiene.

comment on column payroll_engine.payroll_concept.payslip_subsection_code is
    'En que parte de su bloque se imprime la linea de este concepto. Nulo es el caso normal: se imprime en el bloque, sin apartado (backend#121).';

update payroll_engine.payroll_concept c
   set payslip_subsection_code = v.subseccion,
       updated_at              = current_timestamp
  from (values
          ('B03',  'BASE_CC'),   ('B04',  'BASE_CC'),
          ('B01',  'BASE_CC'),   ('B_CC', 'BASE_CC'),
          ('B05',  'BASE_CP'),   ('B06',  'BASE_CP'),
          ('B07',  'BASE_CP'),   ('B_CP', 'BASE_CP'),
          ('B08',  'BASE_HE'),
          ('B09',  'BASE_IRPF')
       ) as v(codigo, subseccion)
  join payroll_engine.payroll_object o
    on o.rule_system_code = 'ESP' and o.object_type_code = 'CONCEPT' and o.object_code = v.codigo
 where c.object_id = o.id;

-- ---------------------------------------------------------
-- 3. La subseccion viaja con la linea
-- ---------------------------------------------------------
-- Como la seccion y como el nombre del concepto: el sitio en el que se imprimio
-- una linea es parte del documento, y las dos salidas tienen que poder leer el
-- recibo sin preguntarle nada al catalogo (ADR-062).
alter table payroll.payroll_concept
    add column payslip_subsection_code varchar(30);

comment on column payroll.payroll_concept.payslip_subsection_code is
    'El apartado del bloque en el que se imprimio esta linea, congelado con ella. Nulo en las lineas que no van en un apartado, y en las calculadas antes del backend#121.';
