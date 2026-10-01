package com.b4rrhh.employee.labor_classification.infrastructure.persistence;

import com.b4rrhh.employee.labor_classification.application.port.AgreementCategoryRelationLookupPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AgreementCategoryRelationLookupAdapter implements AgreementCategoryRelationLookupPort {

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);
    private static final String BASE_QUERY = """
                select case when count(*) > 0 then true else false end
                from rulesystem.agreement_category_relation r
                join rulesystem.rule_system rs
                  on rs.id = r.rule_system_id
                join rulesystem.rule_entity agr
                  on agr.id = r.agreement_rule_entity_id
                join rulesystem.rule_entity cat
                  on cat.id = r.category_rule_entity_id
                where upper(trim(rs.code)) = :ruleSystemCode
                  and upper(trim(agr.code)) = :agreementCode
                  and upper(trim(cat.code)) = :agreementCategoryCode
                  and agr.rule_entity_type_code = 'AGREEMENT'
                  and cat.rule_entity_type_code = 'AGREEMENT_CATEGORY'
                  and agr.layer_code = rs.code
                  and cat.layer_code = rs.code
                  and agr.active = true
                  and cat.active = true
                  and r.is_active = true
            """;
    private static final String TEMPORAL_FILTERS = """
                  and agr.start_date <= :referenceDate
                  and :referenceDate <= coalesce(agr.end_date, :maxDate)
                  and cat.start_date <= :referenceDate
                  and :referenceDate <= coalesce(cat.end_date, :maxDate)
                  and r.start_date <= :referenceDate
                  and :referenceDate <= coalesce(r.end_date, :maxDate)
            """;

    private final EntityManager entityManager;

    public AgreementCategoryRelationLookupAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsActiveRelation(
            String ruleSystemCode,
            String agreementCode,
            String agreementCategoryCode,
            LocalDate referenceDate
    ) {
        String sql = referenceDate == null ? BASE_QUERY : BASE_QUERY + TEMPORAL_FILTERS;

        var query = entityManager.createNativeQuery(sql)
                .setParameter("ruleSystemCode", ruleSystemCode)
                .setParameter("agreementCode", agreementCode)
                .setParameter("agreementCategoryCode", agreementCategoryCode);

        if (referenceDate != null) {
            query.setParameter("referenceDate", referenceDate);
            query.setParameter("maxDate", MAX_DATE);
        }

        Object result = query.getSingleResult();

        if (result instanceof Boolean boolResult) {
            return boolResult;
        }
        if (result instanceof Number numberResult) {
            return numberResult.intValue() > 0;
        }

        return Boolean.parseBoolean(String.valueOf(result));
    }
}
