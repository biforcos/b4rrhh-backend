-- backend#88: POST /rule-entities creaba raices sin su extension required, y desde el #34 y
-- el #35 eso las deja ilegibles. La decision es la opcion 1: el endpoint generico rechaza los
-- tipos que declaran extensiones required, porque «este tipo se da de alta por su endpoint
-- propio».
--
-- Para que el 400 pueda decir CUAL es ese endpoint sin que nadie lo recuerde, el tipo lo
-- declara. Un mapa tipo -> ruta escrito en Java seria la opcion 3 disfrazada: una lista de
-- «acuerdate de actualizarme» (ADR-057, backend#59), y un tipo nuevo quedaria fuera hasta que
-- alguien la tocara.
--
-- Criterio de admision del ADR-053 §5 —que test falla cuando esta mal—:
-- RuleEntityTypeOwnEndpointGuardTest comprueba que cada ruta declarada es un POST que el
-- backend sirve de verdad. Una ruta renombrada o mal escrita sale ahi, no en produccion.
--
-- Nula a proposito donde no hay endpoint de alta: AGREEMENT, AGREEMENT_CATEGORY y
-- EMPLOYEE_ADDRESS_TYPE declaran perfil obligatorio y hoy solo se siembran por migracion.
-- Un null dice «no se sabe por donde» y el mensaje lo dice asi; inventar una ruta plausible
-- seria mentir con mas aplomo.
alter table rulesystem.rule_entity_type
    add column api_collection_path varchar(100);

alter table rulesystem.rule_entity_type
    add constraint chk_rule_entity_type_api_collection_path
    check (api_collection_path is null or api_collection_path like '/%');

comment on column rulesystem.rule_entity_type.api_collection_path is
    'Coleccion propia del API por la que se da de alta una raiz de este tipo (/work-centers). '
    'Nula si no hay ninguna todavia. La lee el rechazo de POST /rule-entities (backend#88).';

update rulesystem.rule_entity_type set api_collection_path = '/companies'    where code = 'COMPANY';
update rulesystem.rule_entity_type set api_collection_path = '/work-centers' where code = 'WORK_CENTER';
