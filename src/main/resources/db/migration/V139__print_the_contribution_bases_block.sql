-- =========================================================
-- V139__print_the_contribution_bases_block.sql
-- El bloque de bases de cotizacion deja de salir vacio (backend#111)
-- =========================================================
--
-- La V138 declaro las cinco secciones del recibo oficial, y una es
-- «Determinacion de las bases de cotizacion». El folio ya sabe colocar un
-- bloque ahi —comprobado en el b4rrhh/frontend#76 moviendo el 980 a esa
-- seccion y viendolo aparecer— pero el bloque salia vacio: ningun concepto de
-- naturaleza BASE lleva payslip_order_code, asi que se calculan, se guardan y
-- no se imprimen.
--
-- El recibo ensenaba cuatro bloques de cinco, y el que faltaba es el que un
-- tecnico de nominas mira primero para comprobar la cotizacion.
--
-- ---------------------------------------------------------
-- Cuales se imprimen, y por que esos dos
-- ---------------------------------------------------------
-- **No todos los BASE van al papel**, y esa es la decision que este fichero
-- deja escrita. Hay seis conceptos BASE en ESP y solo dos son una base del
-- recibo oficial; los otros cuatro son andamiaje del motor:
--
--   B_CC   Base de cotizacion por contingencias comunes.  SE IMPRIME.
--          Es la base contra la que cotizan los cuatro conceptos del
--          trabajador (700 a 703) y los cinco de la empresa (720 a 724).
--          En el modelo oficial es la primera linea del recuadro.
--
--   B01    Base cotizable, y la base sujeta a retencion del IRPF. SE IMPRIME.
--          Es el operando BASE del 800, o sea el numero sobre el que se aplica
--          el tipo de retencion. En el modelo oficial es la ultima linea del
--          recuadro.
--
--   B_CC_MAX  NO se imprime. Es B01 limitado al tope maximo, un paso
--             intermedio hacia B_CC: el motor lo necesita y el papel no. Entre
--             B01 y B_CC no hay nada que un tecnico tenga que ver.
--
--   P01, P02, P03  NO se imprimen. Son PRECIOS —precio del dia, precio del dia
--                  a jornada completa, precio de la hora extraordinaria— y no
--                  bases de cotizacion. Llevan naturaleza BASE porque el motor
--                  los usa como operando BASE de un calculo, que es otra cosa
--                  que compartir nombre. Imprimirlos meteria tres tarifas en
--                  el recuadro de las bases.
--
-- Lo que esta linea defiende es que «no tiene orden de recibo» siga
-- significando «no va al papel» y no «se me olvido». Darselo a los seis
-- hubiera sido sustituir «no sale ninguno» por «salen los que no deben», que
-- es mas dificil de ver.
--
-- ---------------------------------------------------------
-- Los numeros de orden
-- ---------------------------------------------------------
-- La decena 4xx estaba libre y no significa nada todavia en este catalogo
-- —1xx devengos, 7xx deducciones del trabajador, 72x aportacion empresarial,
-- 8xx IRPF, 9xx totales—, asi que es la del recuadro de bases.
--
-- Estos numeros **no deciden en que bloque sale la linea**: eso lo dice la
-- seccion declarada en la V138. Solo ordenan las dos lineas dentro del
-- recuadro, y van en el orden del modelo oficial: primero la de cotizacion,
-- despues la sujeta a retencion.
update payroll_engine.payroll_concept c
   set payslip_order_code = v.orden
  from (values ('B_CC', '410'), ('B01', '430')) as v(codigo, orden)
  join payroll_engine.payroll_object o
    on o.rule_system_code = 'ESP'
   and o.object_type_code = 'CONCEPT'
   and o.object_code = v.codigo
 where c.object_id = o.id;
