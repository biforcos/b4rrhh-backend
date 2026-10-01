package com.b4rrhh.rulesystem.translation.infrastructure.persistence;

import com.b4rrhh.rulesystem.translation.application.port.RuleEntityTranslationCoverageReadPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El informe de cobertura cuenta filas de {@code rule_entity}, no resuelve por reglamentación: una
 * entidad de {@code INT} es una fila aunque la monten tres. Por eso lee la tabla sin pasar por el
 * puerto y es una excepción con nombre del candado ({@code NobodyReadsRuleEntityOutsideItsPortTest},
 * backend#157); cada fila dice de qué capa es.
 */
@Component
public class RuleEntityTranslationCoverageReadAdapter implements RuleEntityTranslationCoverageReadPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<String, Long> countRuleEntitiesByType() {
        List<Object[]> rows = entityManager.createQuery("""
                select re.ruleEntityTypeCode, count(re)
                  from RuleEntityEntity re
                 group by re.ruleEntityTypeCode
                 order by re.ruleEntityTypeCode
                """, Object[].class).getResultList();

        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public List<UntranslatedRuleEntity> findUntranslated(String languageCode) {
        return entityManager.createQuery("""
                select new com.b4rrhh.rulesystem.translation.application.port.RuleEntityTranslationCoverageReadPort$UntranslatedRuleEntity(
                       re.ruleEntityTypeCode, re.layerCode, cast(ret.level as Integer), re.code, re.name)
                  from RuleEntityEntity re, RuleEntityTypeEntity ret
                 where ret.code = re.ruleEntityTypeCode
                   and not exists (
                       select t.ruleEntityId
                         from RuleEntityTranslationEntity t
                        where t.ruleEntityId = re.id
                          and t.languageCode = :languageCode
                 )
                 order by re.ruleEntityTypeCode, re.layerCode, re.code
                """, UntranslatedRuleEntity.class)
                .setParameter("languageCode", languageCode)
                .getResultList();
    }
}
