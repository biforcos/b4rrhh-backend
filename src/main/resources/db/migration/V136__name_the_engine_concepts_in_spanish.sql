-- =========================================================
-- V136__name_the_engine_concepts_in_spanish.sql
-- Donde vive el nombre de un concepto del motor (backend#109)
-- =========================================================
--
-- El recibo ensena SALARIO_BASE donde va un nombre. Y no es que el literal sea
-- feo: es que no hay literal. Lo que la linea guarda es el concept_mnemonic del
-- motor copiado tal cual, y un mnemonico es un IDENTIFICADOR — es lo que las
-- reglas referencian para encontrar un concepto. Se esta ensenando la clave
-- interna en el sitio donde va un nombre.
--
-- ---------------------------------------------------------
-- Por que aqui y no en rulesystem
-- ---------------------------------------------------------
-- Por organizacion —los conceptos no viven en rulesystem— y porque no podrian:
-- rulesystem.rule_entity_translation tiene por clave rule_entity_id, y un
-- concepto del motor no es un rule_entity; cuelga de payroll_object, en otro
-- esquema. La maquinaria que parecia reusable no alcanza la cosa.
--
-- ---------------------------------------------------------
-- Por que language_code si solo hay espanol
-- ---------------------------------------------------------
-- Porque el sitio se deja hecho y usarlo es otra conversacion. La clave es
-- (concepto, idioma) desde el primer dia: anadir el idioma despues obligaria a
-- reescribir la clave de una tabla ya poblada. Hoy solo se siembra 'es' y el
-- motor resuelve con un idioma constante declarado en un sitio.
--
-- ---------------------------------------------------------
-- 1. La tabla
-- ---------------------------------------------------------
-- Cuelga del CONCEPTO y no del objeto: un nombre es de un concepto. El cascade
-- acompana al de V78 — borrar un concepto se lleva sus nombres, que sin el
-- quedarian apuntando a nada.
create table payroll_engine.payroll_concept_label (
    id            bigint generated always as identity primary key,
    object_id     bigint       not null,
    language_code varchar(5)   not null,
    label         varchar(200) not null,
    created_at    timestamp    not null default now(),
    updated_at    timestamp    not null default now(),
    constraint fk_payroll_concept_label_concept
        foreign key (object_id)
        references payroll_engine.payroll_concept(object_id)
        on delete cascade,
    constraint uk_payroll_concept_label
        unique (object_id, language_code)
);

comment on table payroll_engine.payroll_concept_label is
    'El nombre de un concepto del motor, por idioma. El mnemonico es el identificador que usan las reglas; esto es lo que el documento dice (backend#109).';

-- ---------------------------------------------------------
-- 2. Los nombres de los 38 conceptos de ESP
-- ---------------------------------------------------------
-- Los 15 que llegan al folio llevan el nombre del modelo oficial: «Total
-- devengado», «Total a deducir», «Liquido total a percibir» no son una
-- traduccion libre del mnemonico, son como se llaman en el recibo de salarios.
--
-- Los 23 que no llegan al folio tambien tienen nombre, y no es de adorno: son
-- los que el grafo y la pestana «Calculo» ensenan cuando alguien pregunta de
-- donde sale un numero.
insert into payroll_engine.payroll_concept_label (object_id, language_code, label)
select o.id, 'es', v.label
from (values
    -- Devengos
    ('101',                'Salario base'),
    ('102',                'Horas extraordinarias'),
    -- Deducciones del trabajador
    ('700',                'Contingencias comunes'),
    ('701',                'Formacion profesional'),
    ('702',                'Mecanismo de equidad intergeneracional'),
    ('703',                'Desempleo'),
    ('800',                'Retencion IRPF'),
    -- Aportacion empresarial
    ('720',                'Contingencias comunes (aportacion empresarial)'),
    ('721',                'Desempleo (aportacion empresarial)'),
    ('722',                'Formacion profesional (aportacion empresarial)'),
    ('723',                'FOGASA (aportacion empresarial)'),
    ('724',                'Mecanismo de equidad intergeneracional (aportacion empresarial)'),
    -- Totales y liquido, con el nombre del modelo oficial
    ('970',                'Total devengado'),
    ('980',                'Total a deducir'),
    ('990',                'Liquido total a percibir'),
    -- Bases
    ('B01',                'Base cotizable'),
    ('B_CC',               'Base de cotizacion por contingencias comunes'),
    ('B_CC_MAX',           'Base de cotizacion limitada al tope maximo'),
    -- Dias y jornada
    ('D01',                'Dias de devengo'),
    ('D02',                'Dias del mes de nomina'),
    ('D03',                'Dias reales del mes'),
    ('J01',                'Coeficiente de jornada'),
    -- La cantidad que entra desde fuera
    ('H01',                'Horas extraordinarias realizadas'),
    -- Precios
    ('P01',                'Precio del dia'),
    ('P02',                'Precio del dia a jornada completa'),
    ('P03',                'Precio de la hora extraordinaria'),
    -- Tipos de cotizacion y retencion
    ('P_SS_CC',            'Tipo de contingencias comunes (trabajador)'),
    ('P_FP_TRAB',          'Tipo de formacion profesional (trabajador)'),
    ('P_MEI_TRAB',         'Tipo de MEI (trabajador)'),
    ('P_SS_DESEMPLEO',     'Tipo de desempleo (trabajador)'),
    ('P_IRPF',             'Tipo de retencion de IRPF'),
    ('P_SS_CC_EMP',        'Tipo de contingencias comunes (empresa)'),
    ('P_SS_DESEMPLEO_EMP', 'Tipo de desempleo (empresa)'),
    ('P_SS_FP_EMP',        'Tipo de formacion profesional (empresa)'),
    ('P_SS_FOGASA_EMP',    'Tipo de FOGASA (empresa)'),
    ('P_SS_MEI_EMP',       'Tipo de MEI (empresa)'),
    -- Topes
    ('P_TOPE_MAX',         'Tope maximo de cotizacion'),
    ('P_TOPE_MIN',         'Tope minimo de cotizacion')
) as v(codigo, label)
join payroll_engine.payroll_object o
  on o.rule_system_code = 'ESP'
 and o.object_type_code = 'CONCEPT'
 and o.object_code = v.codigo
join payroll_engine.payroll_concept c
  on c.object_id = o.id
where not exists (
    select 1
      from payroll_engine.payroll_concept_label l
     where l.object_id = o.id
       and l.language_code = 'es'
);
