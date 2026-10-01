-- backend#159, paso 5 del camino 5 (workspace#20, ADR-077): la ley de nomina cuelga de la capa 4.
--
-- Cinco tablas del motor son ley: lo que nadie elige y es igual para cualquier empresa espanola.
-- Hasta aqui colgaban de rule_system_code, asi que una segunda reglamentacion con la misma ley y
-- otro esquema de empresa (ESP_2 = COM, INT, ESP, NOM_ESP, NOM_ESP2_EMP) tendria que copiarlas.
-- Ahora cuelgan de la capa de nivel 4 que monta la reglamentacion: ESP -> NOM_ESP. Las comparte
-- cualquier reglamentacion que monte NOM_ESP, y la que monte otra capa 4 no ve nada de esta.
--
-- Por tabla:
--   1. rule_system_code pasa a layer_code, ancho de capa (20);
--   2. cada fila va a la capa que su reglamentacion monta en el nivel 4 (rule_system_layer). Si
--      alguna se queda sin capa, la migracion falla;
--   3. layer_level, fijo a 4, y la FK compuesta (layer_code, layer_level) -> layer(code, level):
--      que la capa es de nivel 4 lo dice el esquema y no un trigger, igual que en
--      rule_system_layer (V166).
-- Los nombres de las claves unicas y de los indices no cambian: siguen siendo las mismas claves.
--
-- Lo que no es de aqui y se queda en la reglamentacion (inventario en el primer comentario de
-- backend#159): las tablas del convenio (payroll_table_row, payroll_object_binding), que el
-- convenio no es capa; el grafo y el folio (payroll_object, concept_assignment, payslip_*), que es
-- la puerta que no se cruza todavia; y los hechos de empleados y nominas.

-- ---------------------------------------------------------------------------------------------
-- payroll_engine.ss_cotizacion_tipos: tipos de cotizacion por contingencia
-- ---------------------------------------------------------------------------------------------
alter table payroll_engine.ss_cotizacion_tipos rename column rule_system_code to layer_code;
alter table payroll_engine.ss_cotizacion_tipos alter column layer_code type varchar(20);
update payroll_engine.ss_cotizacion_tipos t
   set layer_code = rsl.layer_code
  from rulesystem.rule_system_layer rsl
 where rsl.rule_system_code = t.layer_code
   and rsl.level = 4;
alter table payroll_engine.ss_cotizacion_tipos
    add column layer_level smallint not null default 4 constraint chk_ss_cotizacion_tipos_layer_level check (layer_level = 4);
alter table payroll_engine.ss_cotizacion_tipos
    add constraint fk_ss_cotizacion_tipos_law_layer
    foreign key (layer_code, layer_level) references rulesystem.layer(code, level);

-- ---------------------------------------------------------------------------------------------
-- payroll_engine.ss_cotizacion_topes: bases minima y maxima por grupo
-- ---------------------------------------------------------------------------------------------
alter table payroll_engine.ss_cotizacion_topes rename column rule_system_code to layer_code;
alter table payroll_engine.ss_cotizacion_topes alter column layer_code type varchar(20);
update payroll_engine.ss_cotizacion_topes t
   set layer_code = rsl.layer_code
  from rulesystem.rule_system_layer rsl
 where rsl.rule_system_code = t.layer_code
   and rsl.level = 4;
alter table payroll_engine.ss_cotizacion_topes
    add column layer_level smallint not null default 4 constraint chk_ss_cotizacion_topes_layer_level check (layer_level = 4);
alter table payroll_engine.ss_cotizacion_topes
    add constraint fk_ss_cotizacion_topes_law_layer
    foreign key (layer_code, layer_level) references rulesystem.layer(code, level);

-- ---------------------------------------------------------------------------------------------
-- payroll_engine.ss_tarifa_primas_at: tarifa de primas de AT y EP por CNAE
-- ---------------------------------------------------------------------------------------------
alter table payroll_engine.ss_tarifa_primas_at rename column rule_system_code to layer_code;
alter table payroll_engine.ss_tarifa_primas_at alter column layer_code type varchar(20);
update payroll_engine.ss_tarifa_primas_at t
   set layer_code = rsl.layer_code
  from rulesystem.rule_system_layer rsl
 where rsl.rule_system_code = t.layer_code
   and rsl.level = 4;
alter table payroll_engine.ss_tarifa_primas_at
    add column layer_level smallint not null default 4 constraint chk_ss_tarifa_primas_at_layer_level check (layer_level = 4);
alter table payroll_engine.ss_tarifa_primas_at
    add constraint fk_ss_tarifa_primas_at_law_layer
    foreign key (layer_code, layer_level) references rulesystem.layer(code, level);

-- ---------------------------------------------------------------------------------------------
-- payroll_engine.ss_desempleo_modalidad_contrato: modalidad de desempleo de cada contrato
-- ---------------------------------------------------------------------------------------------
alter table payroll_engine.ss_desempleo_modalidad_contrato rename column rule_system_code to layer_code;
alter table payroll_engine.ss_desempleo_modalidad_contrato alter column layer_code type varchar(20);
update payroll_engine.ss_desempleo_modalidad_contrato t
   set layer_code = rsl.layer_code
  from rulesystem.rule_system_layer rsl
 where rsl.rule_system_code = t.layer_code
   and rsl.level = 4;
alter table payroll_engine.ss_desempleo_modalidad_contrato
    add column layer_level smallint not null default 4 constraint chk_ss_desempleo_modalidad_contrato_layer_level check (layer_level = 4);
alter table payroll_engine.ss_desempleo_modalidad_contrato
    add constraint fk_ss_desempleo_modalidad_contrato_law_layer
    foreign key (layer_code, layer_level) references rulesystem.layer(code, level);

-- ---------------------------------------------------------------------------------------------
-- payroll_engine.it_prestacion_tramo: tramos de la prestacion de IT
-- ---------------------------------------------------------------------------------------------
alter table payroll_engine.it_prestacion_tramo rename column rule_system_code to layer_code;
alter table payroll_engine.it_prestacion_tramo alter column layer_code type varchar(20);
update payroll_engine.it_prestacion_tramo t
   set layer_code = rsl.layer_code
  from rulesystem.rule_system_layer rsl
 where rsl.rule_system_code = t.layer_code
   and rsl.level = 4;
alter table payroll_engine.it_prestacion_tramo
    add column layer_level smallint not null default 4 constraint chk_it_prestacion_tramo_layer_level check (layer_level = 4);
alter table payroll_engine.it_prestacion_tramo
    add constraint fk_it_prestacion_tramo_law_layer
    foreign key (layer_code, layer_level) references rulesystem.layer(code, level);
