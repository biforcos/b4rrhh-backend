-- ADR-053 §3 (backend#33): una extensión es algo poseído por la raíz y siempre cae en
-- cascada. Sin excepciones, y por eso no hay columna que lo configure.
--
-- El issue esperaba que sólo work_center_contact estuviera sin cascada, pero al mirar el
-- esquema resultó que NINGUNA de las nueve claves ajenas a rule_entity(id) la tenía: los
-- cuatro perfiles y las dos relaciones estaban igual. La única con cascada era
-- rule_entity_translation (V105, backend#26). La guardia 4 lo afirma para todas, así que
-- aquí se arreglan todas: se suelta la restricción original y se vuelve a crear con
-- on delete cascade, como hizo la V105.

alter table rulesystem.company_profile
    drop constraint fk_company_profile_company_rule_entity;
alter table rulesystem.company_profile
    add constraint fk_company_profile_company_rule_entity
    foreign key (company_rule_entity_id) references rulesystem.rule_entity(id)
    on delete cascade;

alter table rulesystem.work_center_profile
    drop constraint fk_work_center_profile_rule_entity;
alter table rulesystem.work_center_profile
    add constraint fk_work_center_profile_rule_entity
    foreign key (work_center_rule_entity_id) references rulesystem.rule_entity(id)
    on delete cascade;

alter table rulesystem.work_center_contact
    drop constraint fk_work_center_contact_rule_entity;
alter table rulesystem.work_center_contact
    add constraint fk_work_center_contact_rule_entity
    foreign key (work_center_rule_entity_id) references rulesystem.rule_entity(id)
    on delete cascade;

alter table rulesystem.agreement_profile
    drop constraint fk_agreement_profile_rule_entity;
alter table rulesystem.agreement_profile
    add constraint fk_agreement_profile_rule_entity
    foreign key (agreement_rule_entity_id) references rulesystem.rule_entity(id)
    on delete cascade;

alter table rulesystem.agreement_category_profile
    drop constraint fk_acp_category;
alter table rulesystem.agreement_category_profile
    add constraint fk_acp_category
    foreign key (agreement_category_rule_entity_id) references rulesystem.rule_entity(id)
    on delete cascade;

alter table rulesystem.agreement_category_relation
    drop constraint fk_agreement_category_relation_agreement_entity;
alter table rulesystem.agreement_category_relation
    add constraint fk_agreement_category_relation_agreement_entity
    foreign key (agreement_rule_entity_id) references rulesystem.rule_entity(id)
    on delete cascade;

alter table rulesystem.agreement_category_relation
    drop constraint fk_agreement_category_relation_category_entity;
alter table rulesystem.agreement_category_relation
    add constraint fk_agreement_category_relation_category_entity
    foreign key (category_rule_entity_id) references rulesystem.rule_entity(id)
    on delete cascade;

alter table rulesystem.contract_subtype_relation
    drop constraint fk_contract_subtype_relation_contract_entity;
alter table rulesystem.contract_subtype_relation
    add constraint fk_contract_subtype_relation_contract_entity
    foreign key (contract_rule_entity_id) references rulesystem.rule_entity(id)
    on delete cascade;

alter table rulesystem.contract_subtype_relation
    drop constraint fk_contract_subtype_relation_subtype_entity;
alter table rulesystem.contract_subtype_relation
    add constraint fk_contract_subtype_relation_subtype_entity
    foreign key (subtype_rule_entity_id) references rulesystem.rule_entity(id)
    on delete cascade;
