-- backend#26: una traducción es un satélite del código, no un uso de él. Si el código se
-- borra, sus traducciones se van con él; sin cascada, borrar un código traducido rompía con
-- una violación de integridad que no mencionaba las traducciones por ninguna parte.
--
-- La restricción a soltar es la que Postgres generó para el `references` en línea de V104:
-- <tabla>_<columna>_fkey.

alter table rulesystem.rule_entity_translation
    drop constraint rule_entity_translation_rule_entity_id_fkey;

alter table rulesystem.rule_entity_translation
    add constraint fk_rule_entity_translation_rule_entity
    foreign key (rule_entity_id) references rulesystem.rule_entity(id)
    on delete cascade;
