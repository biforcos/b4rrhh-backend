-- =========================================================
-- V138__declare_the_payslip_sections_of_the_official_model.sql
-- Las agrupaciones del recibo, declaradas y no deducidas (backend#109)
-- =========================================================
--
-- La maquinaria de colocacion ya estaba: payslip_order_code en el concepto del
-- motor y display_order en la linea, que hoy valen los dos el codigo del
-- concepto (101, 700, 970...). Lo que no estaba en ninguna parte eran las
-- AGRUPACIONES del modelo oficial —devengos, deducciones, bases, liquido—:
-- estaban implicitas en que 1xx < 7xx < 9xx y en unos cuantos condicionales
-- repartidos por la pantalla.
--
-- Un rango numerico no es una declaracion. Funciona hasta que alguien numera
-- un devengo en el 750 porque le tocaba ahi por orden, y entonces se rompe en
-- silencio y dos anos despues. El PDF del paso 2 necesita los bloques, y
-- deducirlos de un rango es exactamente como se rompen.
--
-- ---------------------------------------------------------
-- Por que cuelgan de la naturaleza y no del concepto
-- ---------------------------------------------------------
-- Porque functional_nature ya dice lo que un concepto ES —EARNING, DEDUCTION,
-- BASE, NET_PAY...— y la seccion dice donde van las cosas de esa clase. Atarlo
-- al concepto obligaria a repetir la decision 38 veces y a acordarse la 39.
--
-- TECHNICAL no tiene seccion, y eso tampoco es un olvido: un concepto tecnico
-- no llega al folio (no lleva payslip_order_code). Si algun dia llegara uno,
-- su linea se quedaria sin seccion y se veria, que es mejor que colocarlo por
-- defecto en un bloque donde no pinta nada.
--
-- ---------------------------------------------------------
-- 1. Las secciones
-- ---------------------------------------------------------
create table payroll_engine.payslip_section (
    rule_system_code varchar(10)  not null,
    section_code     varchar(30)  not null,
    section_label    varchar(200) not null,
    display_order    integer      not null,
    created_at       timestamp    not null default now(),
    updated_at       timestamp    not null default now(),
    constraint pk_payslip_section primary key (rule_system_code, section_code),
    constraint uk_payslip_section_display_order unique (rule_system_code, display_order)
);

comment on table payroll_engine.payslip_section is
    'Los bloques del modelo oficial de recibo de salarios, y en que orden van. Declarados, no deducidos del rango del codigo de concepto (backend#109).';

-- ---------------------------------------------------------
-- 2. Que naturaleza va en que seccion
-- ---------------------------------------------------------
-- La clave primaria es (sistema de reglas, naturaleza): una naturaleza esta en
-- UNA seccion. Sin eso, un EARNING podria acabar declarado en dos bloques y la
-- pregunta «donde va esta linea» tendria dos respuestas.
create table payroll_engine.payslip_section_nature (
    rule_system_code  varchar(10) not null,
    functional_nature varchar(30) not null,
    section_code      varchar(30) not null,
    constraint pk_payslip_section_nature primary key (rule_system_code, functional_nature),
    constraint fk_payslip_section_nature_section
        foreign key (rule_system_code, section_code)
        references payroll_engine.payslip_section (rule_system_code, section_code)
        on delete cascade
);

-- ---------------------------------------------------------
-- 3. El modelo oficial espanol
-- ---------------------------------------------------------
-- Los cinco bloques del recibo de salarios, en el orden en el que se imprimen.
insert into payroll_engine.payslip_section (rule_system_code, section_code, section_label, display_order)
values
    ('ESP', 'DEVENGOS',               'Devengos',                                 10),
    ('ESP', 'DEDUCCIONES',            'Deducciones',                              20),
    ('ESP', 'LIQUIDO',                'Liquido total a percibir',                 30),
    ('ESP', 'BASES',                  'Determinacion de las bases de cotizacion', 40),
    ('ESP', 'APORTACION_EMPRESARIAL', 'Aportacion empresarial',                   50);

-- El total de un bloque vive en su bloque: TOTAL_EARNING es la ultima linea de
-- devengos, no una seccion aparte.
insert into payroll_engine.payslip_section_nature (rule_system_code, functional_nature, section_code)
values
    ('ESP', 'EARNING',          'DEVENGOS'),
    ('ESP', 'TOTAL_EARNING',    'DEVENGOS'),
    ('ESP', 'DEDUCTION',        'DEDUCCIONES'),
    ('ESP', 'TOTAL_DEDUCTION',  'DEDUCCIONES'),
    ('ESP', 'NET_PAY',          'LIQUIDO'),
    ('ESP', 'BASE',             'BASES'),
    ('ESP', 'INFORMATIONAL',    'APORTACION_EMPRESARIAL');

-- ---------------------------------------------------------
-- 4. La seccion viaja con la linea
-- ---------------------------------------------------------
-- Como display_order y como la naturaleza: el bloque en el que se imprimio una
-- linea es parte del documento, y el PDF tiene que poder leer el recibo sin
-- preguntarle nada al catalogo.
--
-- Se rellena en las lineas que ya existen, y aqui no se inventa nada: la
-- seccion se DERIVA de concept_nature_code, que ya venia congelado en cada
-- linea. Derivar de lo congelado no es reescribir un documento; es terminar de
-- escribir lo que ya decia.
alter table payroll.payroll_concept
    add column payslip_section_code varchar(30);

update payroll.payroll_concept c
   set payslip_section_code = n.section_code
  from payroll.payroll p, payroll_engine.payslip_section_nature n
 where p.id = c.payroll_id
   and n.rule_system_code = p.rule_system_code
   and n.functional_nature = c.concept_nature_code;

comment on column payroll.payroll_concept.payslip_section_code is
    'El bloque del modelo oficial en el que se imprimio esta linea, congelado con ella. Nulo si la naturaleza del concepto no tenia seccion declarada, que es una ausencia que hay que ver (backend#109).';
