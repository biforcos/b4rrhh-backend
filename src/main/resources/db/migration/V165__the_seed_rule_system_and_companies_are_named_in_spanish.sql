-- backend#152: el nombre de ESP era «Spain Personnel Administration», y sale en el title del
-- «Ámbito» del menú lateral de todas las pantallas. Francia y Portugal ya se llamaban
-- «Francia» y «Portugal». Es un dato de semilla (V49), no una traducción pendiente.
update rulesystem.rule_system
   set name = 'Administración de personal · España',
       updated_at = now()
 where code = 'ESP';

-- Las empresas de semilla son nombres propios (PROPER_NOUN, ADR-052 §2): no llevan
-- traducción, y lo que estaba en inglés era el nombre mismo. Decidido en el issue: se
-- renombran si nada depende del nombre. Comprobado en los cinco repos: el nombre sólo
-- aparece en fixtures de test que lo escriben ellos mismos, y ni el loader ni la
-- aplicación lo leen para decidir nada; eligen por código. La razón social del perfil
-- («B4RRHH Spain Company 01, S.L.») es otra cosa —el nombre legal que sale en el recibo—
-- y no se toca.
update rulesystem.rule_entity
   set name = v.name,
       updated_at = now()
  from (values
        ('ESP', 'ES01', 'Empresa España 01'),
        ('ESP', 'ES02', 'Empresa España 02'),
        ('FRA', 'FR01', 'Empresa Francia 01'),
        ('PRT', 'PT01', 'Empresa Portugal 01')
       ) as v(rule_system_code, code, name)
 where rule_entity.rule_entity_type_code = 'COMPANY'
   and rule_entity.rule_system_code = v.rule_system_code
   and rule_entity.code = v.code;
