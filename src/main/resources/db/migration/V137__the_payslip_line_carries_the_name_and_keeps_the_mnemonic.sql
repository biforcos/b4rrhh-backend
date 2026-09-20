-- =========================================================
-- V137__the_payslip_line_carries_the_name_and_keeps_the_mnemonic.sql
-- Dos campos, dos trabajos (backend#109)
-- =========================================================
--
-- La linea del recibo tenia un solo hueco para dos cosas, y lo que habia
-- dentro era la equivocada: concept_label guardaba el concept_mnemonic del
-- motor. Poner ahi el nombre de verdad sin esta columna no arreglaria el
-- defecto, lo daria la vuelta — cambiaria un identificador por un nombre y
-- dejaria a la linea sin la clave que el grafo y las reglas usan.
--
-- ---------------------------------------------------------
-- Por que el relleno dice la verdad
-- ---------------------------------------------------------
-- concept_mnemonic se rellena DESDE concept_label en las lineas que ya
-- existen, y no es un apano: lo que esas lineas tienen guardado en el hueco
-- del literal ES su mnemonico, comprobado en la semilla. Copiarlo a la
-- columna que le corresponde no inventa nada.
--
-- Y concept_label se queda como esta, diciendo SALARIO_BASE. Tambien a
-- proposito: un recibo es lo que el motor calculo, y esos se calcularon
-- diciendo eso. El literal nuevo llega recalculando o resembrando, no
-- reescribiendo hacia atras lo que un documento dijo (ADR-059, ADR-062).
--
-- ---------------------------------------------------------
-- 1. La columna
-- ---------------------------------------------------------
alter table payroll.payroll_concept
    add column concept_mnemonic varchar(50);

update payroll.payroll_concept
   set concept_mnemonic = concept_label
 where concept_mnemonic is null;

-- Ni una linea sin mnemonico, ni ahora ni despues: lo que el grafo y las
-- reglas usan no puede faltar en la linea que lo explica.
alter table payroll.payroll_concept
    alter column concept_mnemonic set not null;

comment on column payroll.payroll_concept.concept_mnemonic is
    'El identificador del concepto en el motor, congelado con la linea. Es lo que las reglas y el grafo referencian; el nombre esta en concept_label (backend#109).';

comment on column payroll.payroll_concept.concept_label is
    'Como se llamaba el concepto cuando se calculo esta linea. Se congela aqui y no se resuelve al leer: el recibo es un documento, no una vista del catalogo (backend#109).';
